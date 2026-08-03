package artboard.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import artboard.host.PreviewFrameEnvironment
import org.jetbrains.skia.EncodedImageFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the offscreen-render APIs the snapshot ("light mode") renderer depends on.
 *
 * These are Compose Multiplatform APIs rather than Artboard's own, so this test
 * exists to fail loudly on a CMP upgrade that moves or removes them.
 */
class OffscreenRenderSpikeTest {

    @Test
    fun renderComposeSceneEncodesPng() {
        val image = renderComposeScene(width = 64, height = 48, density = Density(1f)) {
            Box(Modifier.fillMaxSize().background(Color.Red))
        }

        assertEquals(64, image.width)
        assertEquals(48, image.height)

        val png = image.encodeToData(EncodedImageFormat.PNG)
        assertTrue(png != null, "PNG encoding returned null")
        assertTrue(png.bytes.size > PNG_HEADER_SIZE, "PNG payload was empty")
    }

    @Test
    @OptIn(ExperimentalComposeUiApi::class)
    fun imageComposeSceneSettlesThenEncodes() {
        ImageComposeScene(width = 80, height = 80, density = Density(2f)) {
            PreviewFrameEnvironment(isDark = true, localeTag = "en") {
                Box(Modifier.fillMaxSize().background(Color.Blue))
            }
        }.use { scene ->
            var guard = 0
            while (scene.hasInvalidations() && guard < SETTLE_LIMIT) {
                scene.render()
                guard++
            }
            assertTrue(guard < SETTLE_LIMIT, "scene never settled")

            val png = scene.render().encodeToData(EncodedImageFormat.PNG)
            assertTrue(png != null && png.bytes.size > PNG_HEADER_SIZE)
        }
    }

    private companion object {
        const val PNG_HEADER_SIZE = 8
        const val SETTLE_LIMIT = 16
    }
}
