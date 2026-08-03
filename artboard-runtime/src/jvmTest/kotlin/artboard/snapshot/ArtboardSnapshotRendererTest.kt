package artboard.snapshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import artboard.canvas.BoardLayoutDefaults
import artboard.model.PreviewFrame
import artboard.model.PreviewKind
import artboard.registry.ArtboardRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArtboardSnapshotRendererTest {

    private val outputDirectory: File = File.createTempFile("artboard-snapshot", "").let { file ->
        file.delete()
        file.mkdirs()
        file
    }

    @AfterTest
    fun cleanUp() {
        outputDirectory.deleteRecursively()
    }

    @Test
    fun rendersEveryFrameInBothThemesAndWritesManifest() {
        val result = ArtboardSnapshotRenderer.render(
            registry = registryOf(
                frame(id = "com.example.CardPreview::default", name = "default"),
                frame(
                    id = "com.example.HomeScreenPreview::default",
                    name = "default",
                    kind = PreviewKind.Screen,
                    group = "Home",
                ),
            ),
            options = SnapshotOptions(outputDirectory = outputDirectory, title = "Test board"),
        )

        assertEquals(2, result.frameCount)
        assertEquals(4, result.imageCount, "expected one image per frame per theme")
        assertTrue(result.failures.isEmpty(), "unexpected failures: ${result.failures}")

        val images = File(outputDirectory, SNAPSHOT_IMAGES_DIRECTORY)
            .listFiles()
            .orEmpty()
            .map { it.name }
            .sorted()
        assertEquals(4, images.size)
        assertTrue(images.all { it.endsWith(".png") })
        assertTrue(images.any { it.contains("-light") } && images.any { it.contains("-dark") })

        val manifest = readManifest()
        assertEquals(2, manifest["schemaVersion"]?.jsonPrimitive?.content?.toInt())
        assertEquals("Test board", manifest["title"]?.jsonPrimitive?.content)

        val frames = manifest["frames"]!!.jsonArray
        assertEquals(2, frames.size)
        val screen = frames.map { it.jsonObject }.single { it.string("kind") == "Screen" }
        assertEquals("Home", screen.string("group"))
        assertEquals(BoardLayoutDefaults.SCREEN_DEFAULT_W.toInt(), screen.int("widthDp"))
        assertEquals(BoardLayoutDefaults.SCREEN_DEFAULT_H.toInt(), screen.int("heightDp"))
        assertEquals(2, screen["images"]!!.jsonObject.getValue("default").jsonObject.size)
    }

    @Test
    fun declaredPreviewSizeWinsOverBoardDefault() {
        ArtboardSnapshotRenderer.render(
            registry = registryOf(
                frame(id = "com.example.WidePreview::default", name = "default", widthDp = 500),
            ),
            options = SnapshotOptions(outputDirectory = outputDirectory),
        )

        val frame = readManifest()["frames"]!!.jsonArray.single().jsonObject
        assertEquals(500, frame.int("widthDp"))
        // Height falls back independently, exactly as the live board does.
        assertEquals(BoardLayoutDefaults.COMPONENT_DEFAULT_H.toInt(), frame.int("heightDp"))
    }

    @Test
    fun renderedImageContainsThePixelsThePreviewDrew() {
        ArtboardSnapshotRenderer.render(
            registry = registryOf(frame(id = "com.example.CardPreview::default", name = "default")),
            options = SnapshotOptions(
                outputDirectory = outputDirectory,
                themes = listOf(SnapshotTheme.Light),
                scale = 1f,
            ),
        )

        val png = File(outputDirectory, SNAPSHOT_IMAGES_DIRECTORY)
            .listFiles()
            .orEmpty()
            .single()
        val image = ImageIO.read(png)

        assertEquals(BoardLayoutDefaults.COMPONENT_DEFAULT_W.toInt(), image.width)
        assertEquals(BoardLayoutDefaults.COMPONENT_DEFAULT_H.toInt(), image.height)
        assertEquals(
            Color.Magenta.toArgb(),
            image.getRGB(image.width / 2, image.height / 2),
            "preview body was not rasterized into the snapshot",
        )
    }

    @Test
    fun failingPreviewIsReportedRatherThanHidden() {
        val result = ArtboardSnapshotRenderer.render(
            registry = registryOf(
                PreviewFrame(
                    id = "com.example.BrokenPreview::default",
                    name = "default",
                    sourceFqName = "com.example.BrokenPreview",
                    content = { error("preview exploded") },
                ),
            ),
            options = SnapshotOptions(
                outputDirectory = outputDirectory,
                themes = listOf(SnapshotTheme.Light),
            ),
        )

        assertEquals(0, result.imageCount)
        assertEquals(1, result.failures.size)
        assertContains(result.failures.single().reason, "preview exploded")

        val manifest = readManifest()
        val failed = manifest["failed"]!!.jsonArray
        assertEquals(1, failed.size)
        assertEquals("com.example.BrokenPreview::default", failed.single().jsonObject.string("id"))
        // The frame is still catalogued, just without images.
        assertTrue(manifest["frames"]!!.jsonArray.single().jsonObject["images"]!!.jsonObject.isEmpty())
    }

    @Test
    fun sameFunctionNameInDifferentPackagesDoesNotCollide() {
        val used = mutableSetOf<String>()
        val first = uniqueSnapshotSlug(
            frame(id = "com.a.CardPreview::default", name = "default"),
            used,
        )
        val second = uniqueSnapshotSlug(
            frame(id = "com.b.CardPreview::default", name = "default"),
            used,
        )
        assertTrue(first != second, "distinct frame ids produced the same slug: $first")
    }

    private fun readManifest(): JsonObject =
        Json.parseToJsonElement(
            File(outputDirectory, SNAPSHOT_MANIFEST_FILE_NAME).readText(),
        ).jsonObject

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.takeIf { it.isString }?.content

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.content?.toInt()

    private fun registryOf(vararg frames: PreviewFrame): ArtboardRegistry =
        object : ArtboardRegistry {
            override val frames: List<PreviewFrame> = frames.toList()
        }

    private fun frame(
        id: String,
        name: String,
        kind: PreviewKind = PreviewKind.Component,
        group: String? = null,
        widthDp: Int? = null,
        heightDp: Int? = null,
    ): PreviewFrame = PreviewFrame(
        id = id,
        name = name,
        group = group,
        kind = kind,
        widthDp = widthDp,
        heightDp = heightDp,
        sourceFqName = id.substringBefore("::"),
        content = { Box(Modifier.fillMaxSize().background(Color.Magenta)) },
    )
}
