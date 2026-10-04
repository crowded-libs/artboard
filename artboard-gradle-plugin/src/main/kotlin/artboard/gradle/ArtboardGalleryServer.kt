package artboard.gradle

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

/**
 * One gallery server shared by every module's run task in a build, per bind address.
 *
 * Each module registers its synchronized gallery directory and the first registration
 * starts the server. With one gallery, `/` serves it exactly as a single-module build
 * always has. With several, `/` serves a tab shell and each gallery is mounted at
 * `/m/<slug>/`; the shell polls [GALLERIES_JSON] so modules appear as they finish building.
 */
abstract class ArtboardGalleryServer :
    BuildService<ArtboardGalleryServer.Parameters>,
    AutoCloseable {

    interface Parameters : BuildServiceParameters {
        val bindAddress: Property<String>
        val preferredPort: Property<Int>
        val buildName: Property<String>
    }

    internal data class Gallery(
        val mount: MountedGallery,
        val root: Path,
        val nodeModules: Path,
    )

    /** Outcome of a registration: whether the caller now owns (and must block on) the server. */
    internal data class Registration(val port: Int, val owner: Boolean)

    private val galleries = LinkedHashMap<String, Gallery>()
    private var server: HttpServer? = null

    @Synchronized
    internal fun register(gallery: Gallery): Registration {
        galleries[gallery.mount.projectPath] = gallery
        server?.let { return Registration(it.address.port, owner = false) }
        val created = firstAvailableServer(parameters.preferredPort.get(), parameters.bindAddress.get())
        created.createContext("/") { exchange ->
            try {
                handle(exchange)
            } finally {
                exchange.close()
            }
        }
        created.executor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "artboard-http").apply { isDaemon = true }
        }
        created.start()
        server = created
        return Registration(created.address.port, owner = true)
    }

    @Synchronized
    private fun registered(): List<Gallery> = galleries.values.sortedBy { it.mount.projectPath }

    @Synchronized
    override fun close() {
        server?.stop(0)
        server = null
    }

    private fun handle(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET" && exchange.requestMethod != "HEAD") {
            return exchange.respond(405)
        }
        val path = exchange.requestURI.path.removePrefix("/")
        val current = registered()
        when {
            path == GALLERIES_JSON -> exchange.respondBytes(
                galleryIndexJson(parameters.buildName.get(), current.map(Gallery::mount)).toByteArray(),
                contentType(GALLERIES_JSON),
            )
            path.startsWith("$SHELL_ASSETS_DIRECTORY/") -> {
                val asset = path.removePrefix("$SHELL_ASSETS_DIRECTORY/")
                if (asset !in SHELL_ASSETS) return exchange.respond(404)
                exchange.respondBytes(shellResource(asset).use { it.readBytes() }, contentType(asset))
            }
            path.startsWith("$MOUNT_DIRECTORY/") -> {
                val slug = path.removePrefix("$MOUNT_DIRECTORY/").substringBefore('/')
                val gallery = current.firstOrNull { it.mount.slug == slug } ?: return exchange.respond(404)
                val rest = path.removePrefix("$MOUNT_DIRECTORY/$slug").removePrefix("/")
                if (rest.isEmpty() && !path.endsWith("/")) {
                    // Gallery scripts and resources are relative, so the mount needs its trailing slash.
                    exchange.responseHeaders.add("Location", "/$MOUNT_DIRECTORY/$slug/")
                    return exchange.respond(302)
                }
                serveGallery(exchange, gallery, rest, mountPrefix = "/$MOUNT_DIRECTORY/$slug")
            }
            current.size == 1 -> serveGallery(exchange, current.single(), path, mountPrefix = "")
            path.isEmpty() || path == "index.html" -> exchange.respondBytes(
                shellHtml(static = false).toByteArray(StandardCharsets.UTF_8),
                contentType("index.html"),
            )
            else -> exchange.respond(404)
        }
    }

    private fun serveGallery(exchange: HttpExchange, gallery: Gallery, path: String, mountPrefix: String) {
        val relative = path.ifBlank { "index.html" }
        val (base, localRelative) = if (relative.startsWith(NODE_MODULES_PREFIX)) {
            gallery.nodeModules to relative.removePrefix(NODE_MODULES_PREFIX)
        } else {
            gallery.root to relative
        }
        val requested = base.resolve(localRelative).normalize()
        if (!requested.startsWith(base) || !Files.isRegularFile(requested)) {
            return exchange.respond(404)
        }
        if (base == gallery.root && localRelative == "index.html") {
            val html = Files.readString(requested, StandardCharsets.UTF_8)
            val importMap = browserImportMap(gallery.root, gallery.nodeModules, mountPrefix)
            return exchange.respondBytes(
                injectImportMap(html, importMap).toByteArray(StandardCharsets.UTF_8),
                contentType("index.html"),
            )
        }
        exchange.responseHeaders.add("Content-Type", contentType(requested.fileName.toString()))
        exchange.responseHeaders.add("Cache-Control", "no-store")
        val head = exchange.requestMethod == "HEAD"
        exchange.sendResponseHeaders(200, if (head) -1 else Files.size(requested))
        if (!head) exchange.responseBody.use { output -> Files.copy(requested, output) }
    }

    private fun HttpExchange.respondBytes(bytes: ByteArray, type: String) {
        responseHeaders.add("Content-Type", type)
        responseHeaders.add("Cache-Control", "no-store")
        val head = requestMethod == "HEAD"
        sendResponseHeaders(200, if (head) -1 else bytes.size.toLong())
        if (!head) responseBody.use { it.write(bytes) }
    }

    private fun HttpExchange.respond(status: Int) {
        sendResponseHeaders(status, -1)
    }

    internal companion object {
        const val NODE_MODULES_PREFIX = "node_modules/"

        fun serviceName(bindAddress: String): String = "artboardGalleryServer@$bindAddress"
    }
}
