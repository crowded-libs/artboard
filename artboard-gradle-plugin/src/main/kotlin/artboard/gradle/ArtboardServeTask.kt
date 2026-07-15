package artboard.gradle

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.net.BindException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/** Serves a synchronized Kotlin/Wasm executable without touching the consumer's product tasks. */
@DisableCachingByDefault(because = "Runs a long-lived local development server")
abstract class ArtboardServeTask : DefaultTask() {
    @get:Internal
    abstract val contentDirectory: DirectoryProperty

    @get:Internal
    abstract val nodeModulesDirectory: DirectoryProperty

    @get:Input
    abstract val preferredPort: Property<Int>

    init {
        preferredPort.convention(8080)
    }

    @TaskAction
    fun serve() {
        val root = contentDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        val nodeModules = nodeModulesDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        val conflictCopies = Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && CONFLICT_COPY.matches(it.fileName.toString()) }
                .map { root.relativize(it).toString() }
                .sorted()
                .toList()
        }
        check(conflictCopies.isEmpty()) {
            buildString {
                appendLine("Artboard cannot serve conflict-copy build output:")
                conflictCopies.forEach { appendLine("  $it") }
                append("Run the consumer build's clean task, then artboardRun again.")
            }
        }
        val server = firstAvailableServer(preferredPort.get())
        server.createContext("/") { exchange -> serveFile(exchange, root, nodeModules) }
        server.executor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "artboard-http").apply { isDaemon = true }
        }
        server.start()
        logger.lifecycle("Artboard gallery → http://127.0.0.1:${server.address.port}/")
        logger.lifecycle("Press Ctrl-C to stop.")
        try {
            CountDownLatch(1).await()
        } finally {
            server.stop(0)
        }
    }

    private fun firstAvailableServer(startPort: Int): HttpServer {
        for (port in startPort..startPort + 20) {
            try {
                return HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
            } catch (_: BindException) {
                // Try the next local port.
            }
        }
        error("No available Artboard port in $startPort..${startPort + 20}")
    }

    private fun serveFile(exchange: HttpExchange, root: Path, nodeModules: Path) {
        if (exchange.requestMethod != "GET" && exchange.requestMethod != "HEAD") {
            exchange.sendResponseHeaders(405, -1)
            exchange.close()
            return
        }

        val relative = exchange.requestURI.path.removePrefix("/").ifBlank { "index.html" }
        val (base, localRelative) = if (relative.startsWith(NODE_MODULES_PREFIX)) {
            nodeModules to relative.removePrefix(NODE_MODULES_PREFIX)
        } else {
            root to relative
        }
        val requested = base.resolve(localRelative).normalize()
        if (!requested.startsWith(base) || !Files.isRegularFile(requested)) {
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
            return
        }

        exchange.responseHeaders.add("Content-Type", contentType(requested.fileName.toString()))
        exchange.responseHeaders.add("Cache-Control", "no-store")
        val bytes = if (base == root && localRelative == "index.html") {
            val html = Files.readString(requested, StandardCharsets.UTF_8)
            injectImportMap(html, browserImportMap(root, nodeModules))
                .toByteArray(StandardCharsets.UTF_8)
        } else {
            null
        }
        val size = bytes?.size?.toLong() ?: Files.size(requested)
        exchange.sendResponseHeaders(200, if (exchange.requestMethod == "HEAD") -1 else size)
        if (exchange.requestMethod != "HEAD") {
            exchange.responseBody.use { output ->
                if (bytes != null) {
                    output.write(bytes)
                } else {
                    Files.newInputStream(requested).use { it.copyTo(output) }
                }
            }
        } else {
            exchange.close()
        }
    }

    private fun contentType(name: String): String = when (name.substringAfterLast('.', "")) {
        "html" -> "text/html; charset=utf-8"
        "mjs", "js" -> "text/javascript; charset=utf-8"
        "wasm" -> "application/wasm"
        "json", "map" -> "application/json; charset=utf-8"
        "css" -> "text/css; charset=utf-8"
        "ttf" -> "font/ttf"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "png" -> "image/png"
        "svg" -> "image/svg+xml"
        else -> "application/octet-stream"
    }

    private companion object {
        const val NODE_MODULES_PREFIX = "node_modules/"
        val CONFLICT_COPY = Regex(".+ [2-9][0-9]*(\\.[^.]+)?")
    }
}

internal fun browserImportMap(contentRoot: Path, nodeModules: Path): Map<String, String> {
    if (!Files.isDirectory(nodeModules)) return emptyMap()
    val specifiers = buildSet {
        Files.walk(contentRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".mjs") }
                .forEach { file ->
                    BARE_IMPORT.findAll(Files.readString(file))
                        .map { it.groupValues[1] }
                        .filter { !it.startsWith(".") && !it.startsWith("/") && !it.contains(":") }
                        .forEach(::add)
                }
        }
    }
    return specifiers.sorted().associateWithNotNull { specifier ->
        val packageName = if (specifier.startsWith("@")) {
            specifier.split('/').take(2).joinToString("/")
        } else {
            specifier.substringBefore('/')
        }
        val packageDir = nodeModules.resolve(packageName).normalize()
        val packageJson = packageDir.resolve("package.json")
        if (!packageDir.startsWith(nodeModules) || !Files.isRegularFile(packageJson)) {
            return@associateWithNotNull null
        }
        val metadata = Files.readString(packageJson)
        val entry = listOf("module", "browser", "main")
            .firstNotNullOfOrNull { key -> jsonStringField(metadata, key) }
            ?: "index.js"
        val entryFile = packageDir.resolve(entry).normalize()
        if (!entryFile.startsWith(packageDir) || !Files.isRegularFile(entryFile)) {
            null
        } else {
            "/node_modules/$packageName/${entry.replace('\\', '/').removePrefix("./")}" 
        }
    }
}

internal fun injectImportMap(html: String, imports: Map<String, String>): String {
    if (imports.isEmpty()) return html
    val mappings = imports.entries.joinToString(",\n") { (name, path) ->
        "      ${name.jsonQuote()}: ${path.jsonQuote()}"
    }
    val importMap =
        """
        |  <script type="importmap">
        |  {"imports": {
        |$mappings
        |  }}
        |  </script>
        """.trimMargin()
    return html.replace("</head>", "$importMap\n</head>")
}

private fun jsonStringField(json: String, name: String): String? =
    Regex("\\\"${Regex.escape(name)}\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        .find(json)
        ?.groupValues
        ?.get(1)

private fun String.jsonQuote(): String = buildString {
    append('"')
    for (character in this@jsonQuote) {
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
    append('"')
}

private inline fun <K, V : Any> Iterable<K>.associateWithNotNull(value: (K) -> V?): Map<K, V> =
    buildMap {
        for (key in this@associateWithNotNull) value(key)?.let { put(key, it) }
    }

private val BARE_IMPORT = Regex("(?:from\\s+|import\\s*)['\\\"]([^'\\\"]+)['\\\"]")
