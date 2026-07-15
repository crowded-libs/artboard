package artboard.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardCameraTest {
    @Test
    fun fitUsesBothViewportDimensionsAndCentersTheBoard() {
        val camera = BoardCamera.fit(
            worldBoundsDp = Rect(0f, 0f, 1_000f, 500f),
            viewportSizePx = Size(500f, 500f),
            density = 1f,
            paddingPx = 0f,
        )

        assertClose(0.5f, camera.scale)
        assertClose(0f, camera.offsetX)
        assertClose(125f, camera.offsetY)
    }

    @Test
    fun zoomTowardKeepsTheFocalWorldPointStationary() {
        val original = BoardCamera(offsetX = 20f, offsetY = -10f, scale = 0.5f)
        val focal = Offset(200f, 160f)
        val density = 2f
        val worldX = (focal.x - original.offsetX) / (original.scale * density)
        val worldY = (focal.y - original.offsetY) / (original.scale * density)

        val zoomed = original.zoomToward(focal, factor = 1.8f, density = density)

        assertClose(focal.x, worldX * density * zoomed.scale + zoomed.offsetX)
        assertClose(focal.y, worldY * density * zoomed.scale + zoomed.offsetY)
    }

    @Test
    fun invalidFitInputReturnsDefaultCamera() {
        assertEquals(
            BoardCamera(),
            BoardCamera.fit(Rect.Zero, Size(500f, 500f), density = 1f),
        )
        assertTrue(BoardCamera.MIN_SCALE < 0.05f)
    }

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.001f, "expected $expected, got $actual")
    }
}
