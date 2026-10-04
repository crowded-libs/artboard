package artboard.gradle

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** How a module's gallery renders; shown on its tab when several modules share one gallery. */
enum class GalleryKind(val id: String) {
    Live("live"),
    Snapshot("snapshot"),
}

/** One module's gallery mounted under a multi-module index. */
internal data class MountedGallery(
    val projectPath: String,
    val title: String,
    val kind: GalleryKind,
) {
    /** URL-safe, collision-free for distinct Gradle paths: `:feature:a` → `feature-a`. */
    val slug: String = projectPath.trim(':').replace(':', '-').ifEmpty { "root" }

    /** Relative to the index page, so the gallery also works under a static host's subpath. */
    val url: String = "$MOUNT_DIRECTORY/$slug/"
}

internal const val MOUNT_DIRECTORY = "m"
internal const val GALLERIES_JSON = "artboard-galleries.json"
internal const val SHELL_ASSETS_DIRECTORY = "artboard-shell"

/** Fonts the shell loads, bundled from the runtime's Studio typography. */
internal val SHELL_ASSETS = listOf("SpaceGrotesk-Medium.ttf", "IBMPlexMono-Regular.ttf")

internal fun galleryIndexJson(buildName: String, galleries: List<MountedGallery>): String =
    galleries.sortedBy { it.projectPath }.joinToString(
        prefix = """{"build":${jsonString(buildName)},"galleries":[""",
        postfix = "]}",
        separator = ",",
    ) { gallery ->
        """{"path":${jsonString(gallery.projectPath)},"title":${jsonString(gallery.title)},""" +
            """"kind":${jsonString(gallery.kind.id)},"url":${jsonString(gallery.url)}}"""
    }

/** The tab shell. A static export marks it so the page stops polling for new modules. */
internal fun shellHtml(static: Boolean): String {
    val html = shellResource("index.html").use { it.readBytes().toString(Charsets.UTF_8) }
    return if (static) html.replaceFirst("<html ", "<html data-static ") else html
}

internal fun shellResource(name: String) = checkNotNull(
    ArtboardGalleryServer::class.java.getResourceAsStream("/artboard/shell/$name"),
) { "Artboard plugin is missing shell resource $name" }

/** Writes the shell page, its assets, and the module index into [directory]. */
internal fun writeShell(directory: Path, buildName: String, galleries: List<MountedGallery>) {
    Files.createDirectories(directory.resolve(SHELL_ASSETS_DIRECTORY))
    Files.writeString(directory.resolve("index.html"), shellHtml(static = true))
    Files.writeString(directory.resolve(GALLERIES_JSON), galleryIndexJson(buildName, galleries))
    SHELL_ASSETS.forEach { asset ->
        shellResource(asset).use { input ->
            Files.copy(input, directory.resolve(SHELL_ASSETS_DIRECTORY).resolve(asset), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

internal fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '<' -> append("\\u003c")
            else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
        }
    }
    append('"')
}

internal fun contentType(name: String): String = when (name.substringAfterLast('.', "")) {
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
