package artboard.gradle

import org.gradle.testfixtures.ProjectBuilder
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArtboardGalleryServerTest {
    private val workspace = createTempDirectory("artboard-gallery-server")
    private val server = ProjectBuilder.builder().build().gradle.sharedServices.registerIfAbsent(
        "test",
        ArtboardGalleryServer::class.java,
    ) { spec ->
        spec.parameters.bindAddress.set(LOOPBACK_ADDRESS)
        // Port 0 binds any free port, so tests never collide with a running gallery.
        spec.parameters.preferredPort.set(0)
        spec.parameters.buildName.set("umbrella")
    }.get()

    @AfterTest
    fun tearDown() {
        server.close()
        workspace.toFile().deleteRecursively()
    }

    @Test
    fun firstRegistrationOwnsTheServerAndLaterOnesJoinIt() {
        val first = server.register(liveGallery(":feature:a"))
        val second = server.register(snapshotGallery(":feature:b"))

        assertTrue(first.owner)
        assertFalse(second.owner)
        assertEquals(first.port, second.port)
    }

    @Test
    fun singleGalleryIsServedAtTheRootWithoutTheShell() {
        val port = server.register(liveGallery(":feature:a")).port

        val index = get(port, "/")
        assertEquals(200, index.status)
        assertContains(index.body, "gallery :feature:a")
        assertContains(index.body, "\"@js-joda/core\": \"/node_modules/@js-joda/core/dist/module.js\"")
        assertFalse("role=\"tablist\"" in index.body)
        assertEquals(200, get(port, "/m/feature-a/").status)
    }

    @Test
    fun severalGalleriesGetTheShellAndPrefixedMounts() {
        val port = server.register(snapshotGallery(":feature:b")).port
        server.register(liveGallery(":feature:a"))

        val shell = get(port, "/")
        assertContains(shell.body, "role=\"tablist\"")
        assertContains(shell.body, "<html lang=\"en\" data-theme=\"light\">")

        assertEquals(
            """{"build":"umbrella","galleries":[""" +
                """{"path":":feature:a","title":"a","kind":"live","url":"m/feature-a/"},""" +
                """{"path":":feature:b","title":"b","kind":"snapshot","url":"m/feature-b/"}]}""",
            get(port, "/$GALLERIES_JSON").body,
        )

        val mounted = get(port, "/m/feature-a/")
        assertContains(mounted.body, "gallery :feature:a")
        assertContains(
            mounted.body,
            "\"@js-joda/core\": \"/m/feature-a/node_modules/@js-joda/core/dist/module.js\"",
        )
        assertEquals(200, get(port, "/m/feature-a/node_modules/@js-joda/core/dist/module.js").status)
        assertContains(get(port, "/m/feature-b/").body, "gallery :feature:b")
    }

    @Test
    fun mountWithoutTrailingSlashRedirectsSoRelativeUrlsResolve() {
        val port = server.register(liveGallery(":feature:a")).port
        server.register(liveGallery(":feature:b"))

        val response = get(port, "/m/feature-a")
        assertEquals(302, response.status)
        assertEquals("/m/feature-a/", response.location)
    }

    @Test
    fun servesBundledShellFontsOnly() {
        val port = server.register(liveGallery(":feature:a")).port
        server.register(liveGallery(":feature:b"))

        val font = get(port, "/$SHELL_ASSETS_DIRECTORY/SpaceGrotesk-Medium.ttf")
        assertEquals(200, font.status)
        assertEquals("font/ttf", font.type)
        assertEquals(404, get(port, "/$SHELL_ASSETS_DIRECTORY/index.html").status)
    }

    @Test
    fun rejectsUnknownMountsAndPathTraversal() {
        val port = server.register(liveGallery(":feature:a")).port
        server.register(liveGallery(":feature:b"))

        assertEquals(404, get(port, "/m/feature-z/").status)
        assertEquals(404, get(port, "/m/feature-a/..%2F..%2Fsecret.txt").status)
        assertEquals(404, get(port, "/missing.js").status)
    }

    private fun liveGallery(path: String): ArtboardGalleryServer.Gallery {
        val root = galleryRoot(path)
        root.resolve("index.html").writeText(
            """<html><head></head><body>gallery $path<script type="module" src="app.mjs"></script></body></html>""",
        )
        root.resolve("app.mjs").writeText("import * as time from '@js-joda/core';")
        val modules = workspace.resolve("node_modules").createDirectories()
        val joda = modules.resolve("@js-joda/core").createDirectories()
        joda.resolve("package.json").writeText("""{"module":"dist/module.js"}""")
        joda.resolve("dist").createDirectories().resolve("module.js").writeText("export {};")
        workspace.resolve("secret.txt").writeText("secret")
        return ArtboardGalleryServer.Gallery(
            mount = MountedGallery(path, path.substringAfterLast(':'), GalleryKind.Live),
            root = root,
            nodeModules = modules,
        )
    }

    private fun snapshotGallery(path: String): ArtboardGalleryServer.Gallery {
        val root = galleryRoot(path)
        root.resolve("index.html").writeText("<html><head></head><body>gallery $path</body></html>")
        root.resolve("manifest.json").writeText("{}")
        return ArtboardGalleryServer.Gallery(
            mount = MountedGallery(path, path.substringAfterLast(':'), GalleryKind.Snapshot),
            root = root,
            nodeModules = workspace.resolve("absent"),
        )
    }

    private fun galleryRoot(path: String): Path =
        workspace.resolve("galleries").resolve(path.trim(':').replace(':', '-')).createDirectories()

    private data class Response(val status: Int, val body: String, val type: String?, val location: String?)

    private fun get(port: Int, path: String): Response {
        val connection = URI("http://127.0.0.1:$port$path").toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        return try {
            val status = connection.responseCode
            val body = if (status in 200..299) connection.inputStream.use { it.readBytes().decodeToString() } else ""
            Response(status, body, connection.contentType, connection.getHeaderField("Location"))
        } finally {
            connection.disconnect()
        }
    }
}
