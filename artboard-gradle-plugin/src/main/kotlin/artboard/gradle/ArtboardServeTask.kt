package artboard.gradle

import com.sun.net.httpserver.HttpServer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.net.BindException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.CountDownLatch

/**
 * Serves this module's synchronized gallery through the build's shared
 * [ArtboardGalleryServer], without touching the consumer's product tasks.
 *
 * The first run task to register starts the server and blocks until the build is
 * cancelled; run tasks from other modules in the same build mount their gallery on
 * that server and return.
 */
@DisableCachingByDefault(because = "Runs a long-lived local development server")
abstract class ArtboardServeTask : DefaultTask() {
    @get:Internal
    abstract val contentDirectory: DirectoryProperty

    @get:Internal
    abstract val nodeModulesDirectory: DirectoryProperty

    @get:Input
    abstract val preferredPort: Property<Int>

    @get:Input
    abstract val bindAddress: Property<String>

    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val galleryTitle: Property<String>

    @get:Input
    abstract val galleryKind: Property<GalleryKind>

    /** Set when this build runs tasks one at a time and the request may span modules. */
    @get:Input
    abstract val serialAcrossModules: Property<Boolean>

    @get:Internal
    abstract val galleryServer: Property<ArtboardGalleryServer>

    init {
        preferredPort.convention(8080)
        bindAddress.convention(LOOPBACK_ADDRESS)
    }

    @TaskAction
    fun serve() {
        val root = contentDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        val nodeModules = nodeModulesDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        // Finder/iCloud often leaves "file 2.ext" junk under build/; strip it instead of failing.
        val purged = ConflictCopies.purge(root)
        if (purged.isNotEmpty()) {
            logger.lifecycle(
                buildString {
                    appendLine("Artboard removed ${purged.size} conflict-copy path(s) from gallery output:")
                    purged.take(MAX_PURGE_LOG).forEach { appendLine("  $it") }
                    if (purged.size > MAX_PURGE_LOG) {
                        appendLine("  … and ${purged.size - MAX_PURGE_LOG} more")
                    }
                }.trimEnd(),
            )
        }
        val registration = galleryServer.get().register(
            ArtboardGalleryServer.Gallery(
                mount = MountedGallery(projectPath.get(), galleryTitle.get(), galleryKind.get()),
                root = root,
                nodeModules = nodeModules,
            ),
        )
        val port = registration.port
        if (!registration.owner) {
            logger.lifecycle("Artboard gallery ${projectPath.get()} added → http://127.0.0.1:$port/")
            return
        }
        if (serialAcrossModules.get()) logger.warn(SERIAL_WARNING)
        logger.lifecycle("Artboard gallery → http://127.0.0.1:$port/")
        if (bindAddress.get() == ALL_INTERFACES_ADDRESS) {
            val lanUrls = artboardLanUrls(port)
            if (lanUrls.isEmpty()) {
                logger.lifecycle("Artboard gallery (LAN) → no private IPv4 address detected")
            } else {
                lanUrls.forEach { logger.lifecycle("Artboard gallery (LAN) → $it") }
            }
            logger.lifecycle("Warning: Artboard is visible to other devices on your local network.")
        }
        logger.lifecycle("Press Ctrl-C to stop.")
        try {
            CountDownLatch(1).await()
        } finally {
            galleryServer.get().close()
        }
    }

    private companion object {
        const val MAX_PURGE_LOG = 12
    }
}

/**
 * Serving blocks until Ctrl-C, so in a build that runs tasks one at a time every other
 * module's run task waits behind the first and never joins its gallery.
 */
internal const val SERIAL_WARNING =
    "Warning: this build runs tasks one at a time, so other modules' Artboard galleries " +
        "cannot join this one. Run with --parallel (or enable the configuration cache) " +
        "to browse every module in one gallery."

/**
 * True when tasks run serially and the request names a run task without a project path,
 * which Gradle expands to every module that has one.
 */
internal fun runsSeriallyAcrossModules(parallel: Boolean, requestedTasks: List<String>): Boolean =
    !parallel && requestedTasks.any { it == "artboardRun" || it == "artboardRunLan" }

internal const val LOOPBACK_ADDRESS = "127.0.0.1"
internal const val ALL_INTERFACES_ADDRESS = "0.0.0.0"

internal fun firstAvailableServer(startPort: Int, bindAddress: String): HttpServer {
    for (port in startPort..startPort + PORT_FALLBACK_COUNT) {
        try {
            return HttpServer.create(InetSocketAddress(bindAddress, port), 0)
        } catch (_: BindException) {
            // Try the next port on the same interface.
        }
    }
    error("No available Artboard port in $startPort..${startPort + PORT_FALLBACK_COUNT}")
}

internal fun artboardLanUrls(
    port: Int,
    addresses: List<InetAddress> = localNetworkAddresses(),
): List<String> = addresses
    .filterIsInstance<Inet4Address>()
    .filter { it.isSiteLocalAddress && !it.isLoopbackAddress }
    .map(InetAddress::getHostAddress)
    .distinct()
    .sorted()
    .map { "http://$it:$port/" }

private fun localNetworkAddresses(): List<InetAddress> = runCatching {
    val interfaces = NetworkInterface.getNetworkInterfaces()
        ?.let(Collections::list)
        .orEmpty()
    interfaces
        .filter { it.isUp && !it.isLoopback }
        .flatMap { Collections.list(it.inetAddresses) }
}.getOrDefault(emptyList())

private const val PORT_FALLBACK_COUNT = 20

internal fun browserImportMap(
    contentRoot: Path,
    nodeModules: Path,
    mountPrefix: String = "",
): Map<String, String> {
    return resolveBrowserModules(contentRoot, nodeModules).resolutions.associate { resolution ->
        resolution.specifier to
            "$mountPrefix/node_modules/${resolution.packageName}/${resolution.entryPath}"
    }
}

internal data class BrowserModuleResolution(
    val specifier: String,
    val packageName: String,
    val packageDirectory: Path,
    val entryPath: String,
)

internal data class BrowserModuleResolutionResult(
    val resolutions: List<BrowserModuleResolution>,
    val unresolvedSpecifiers: List<String>,
)

internal fun resolveBrowserModules(
    contentRoot: Path,
    nodeModules: Path,
): BrowserModuleResolutionResult {
    val pending = ArrayDeque(bareModuleSpecifiers(contentRoot).sorted())
    val visited = mutableSetOf<String>()
    val scannedEntries = mutableSetOf<Path>()
    val resolutions = mutableListOf<BrowserModuleResolution>()
    val unresolved = mutableSetOf<String>()

    while (pending.isNotEmpty()) {
        val specifier = pending.removeFirst()
        if (!visited.add(specifier)) continue
        val resolution = resolveBrowserModule(specifier, nodeModules)
        if (resolution == null) {
            unresolved += specifier
            continue
        }
        resolutions += resolution
        val entryFile = resolution.packageDirectory.resolve(resolution.entryPath).normalize()
        scanModuleGraph(entryFile, resolution.packageDirectory, scannedEntries)
            .filter(::isBareModuleSpecifier)
            .filterNot(visited::contains)
            .forEach(pending::addLast)
    }

    return BrowserModuleResolutionResult(
        resolutions = resolutions.sortedBy { it.specifier },
        unresolvedSpecifiers = unresolved.sorted(),
    )
}

private fun resolveBrowserModule(
    specifier: String,
    nodeModules: Path,
): BrowserModuleResolution? {
    if (!Files.isDirectory(nodeModules)) return null
    val packageName = packageName(specifier)
    val linkedPackageDirectory = nodeModules.resolve(packageName).normalize()
    if (!linkedPackageDirectory.startsWith(nodeModules) || !Files.isDirectory(linkedPackageDirectory)) {
        return null
    }
    val packageDirectory = linkedPackageDirectory.toRealPath()
    val subpath = specifier.removePrefix(packageName).removePrefix("/")
    val entryFile = if (subpath.isNotBlank()) {
        resolveModuleFile(packageDirectory.resolve(subpath), packageDirectory)
    } else {
        val packageJson = packageDirectory.resolve("package.json")
        val metadata = packageJson.takeIf(Files::isRegularFile)?.let(Files::readString).orEmpty()
        val entry = listOf("module", "browser", "main")
            .firstNotNullOfOrNull { key -> jsonStringField(metadata, key) }
            ?: "index.js"
        resolveModuleFile(packageDirectory.resolve(entry.removePrefix("./")), packageDirectory)
    } ?: return null

    return BrowserModuleResolution(
        specifier = specifier,
        packageName = packageName,
        packageDirectory = packageDirectory,
        entryPath = packageDirectory.relativize(entryFile).toString().replace('\\', '/'),
    )
}

private fun scanModuleGraph(
    entryFile: Path,
    packageDirectory: Path,
    scannedFiles: MutableSet<Path>,
): Set<String> {
    val pending = ArrayDeque<Path>().apply { add(entryFile) }
    val specifiers = mutableSetOf<String>()
    while (pending.isNotEmpty()) {
        val file = pending.removeFirst()
        if (!scannedFiles.add(file)) continue
        moduleSpecifiers(file).forEach { specifier ->
            specifiers += specifier
            if (specifier.startsWith(".")) {
                resolveModuleFile(file.parent.resolve(specifier), packageDirectory)
                    ?.takeIf { it !in scannedFiles }
                    ?.let(pending::addLast)
            }
        }
    }
    return specifiers
}

private fun bareModuleSpecifiers(contentRoot: Path): Set<String> = buildSet {
    if (!Files.isDirectory(contentRoot)) return@buildSet
    Files.walk(contentRoot).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.extensionIsJavaScript() }
            .forEach { file ->
                moduleSpecifiers(file).filter(::isBareModuleSpecifier).forEach(::add)
            }
    }
}

private fun moduleSpecifiers(file: Path): Sequence<String> =
    MODULE_IMPORT.findAll(Files.readString(file)).map { it.groupValues[1] }

private fun resolveModuleFile(candidate: Path, packageDirectory: Path): Path? {
    val cleanCandidate = candidate.toString().substringBefore('?').substringBefore('#')
    val base = Path.of(cleanCandidate).normalize()
    if (!base.startsWith(packageDirectory)) return null
    return listOf(
        base,
        Path.of("$base.js"),
        Path.of("$base.mjs"),
        base.resolve("index.js"),
        base.resolve("index.mjs"),
    ).firstOrNull(Files::isRegularFile)
}

private fun packageName(specifier: String): String = if (specifier.startsWith("@")) {
    specifier.split('/').take(2).joinToString("/")
} else {
    specifier.substringBefore('/')
}

private fun isBareModuleSpecifier(specifier: String): Boolean =
    !specifier.startsWith(".") && !specifier.startsWith("/") && !specifier.contains(":")

private fun Path.extensionIsJavaScript(): Boolean =
    fileName.toString().substringAfterLast('.', "") in setOf("js", "mjs")

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

private val MODULE_IMPORT = Regex(
    pattern =
        """^\s*(?:import(?:\s+[^'"\n;]+?\s+from)?|export\s+(?:\*[^'"\n]*|\{[^}\n]*})\s+from)\s*['"]([^'"]+)['"]""",
    option = RegexOption.MULTILINE,
)
