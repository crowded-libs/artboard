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

    @Test
    fun contentIntersectsViewportWhenBoardOverlapsWindow() {
        // Board 0..400dp at scale 1, density 1 → screen 0..400; viewport 500×500.
        val camera = BoardCamera(offsetX = 0f, offsetY = 0f, scale = 1f)
        assertTrue(
            camera.contentIntersectsViewport(
                worldBoundsDp = Rect(0f, 0f, 400f, 300f),
                viewportSizePx = Size(500f, 500f),
                density = 1f,
            ),
        )
    }

    @Test
    fun contentIntersectsViewportFalseWhenPannedOffScreen() {
        // Board rendered at x = offsetX; push entirely past the right edge.
        val camera = BoardCamera(offsetX = 800f, offsetY = 0f, scale = 1f)
        assertTrue(
            !camera.contentIntersectsViewport(
                worldBoundsDp = Rect(0f, 0f, 400f, 300f),
                viewportSizePx = Size(500f, 500f),
                density = 1f,
            ),
        )
    }

    @Test
    fun contentIntersectsViewportFalseWhenZoomedPastAndOffsetAway() {
        // Tiny board far off-screen after a large pan.
        val camera = BoardCamera(offsetX = -10_000f, offsetY = -10_000f, scale = 2f)
        assertTrue(
            !camera.contentIntersectsViewport(
                worldBoundsDp = Rect(0f, 0f, 100f, 100f),
                viewportSizePx = Size(800f, 600f),
                density = 2f,
            ),
        )
    }

    @Test
    fun contentIntersectsViewportTrueWhenOnlyCornerClipsIn() {
        // Board mostly above/left of origin; bottom-right corner still in view.
        val camera = BoardCamera(offsetX = -350f, offsetY = -250f, scale = 1f)
        assertTrue(
            camera.contentIntersectsViewport(
                worldBoundsDp = Rect(0f, 0f, 400f, 300f),
                viewportSizePx = Size(500f, 500f),
                density = 1f,
            ),
        )
    }

    @Test
    fun contentIntersectsViewportRejectsEmptyInputs() {
        val camera = BoardCamera()
        assertTrue(
            !camera.contentIntersectsViewport(
                worldBoundsDp = Rect.Zero,
                viewportSizePx = Size(500f, 500f),
                density = 1f,
            ),
        )
        assertTrue(
            !camera.contentIntersectsViewport(
                worldBoundsDp = Rect(0f, 0f, 100f, 100f),
                viewportSizePx = Size.Zero,
                density = 1f,
            ),
        )
    }

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.001f, "expected $expected, got $actual")
    }
}
