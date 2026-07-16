package artboard.canvas

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanvasGesturesTest {
    @Test
    fun panMovesTheCameraWithThePointer() {
        val transformed = BoardCamera(offsetX = 10f, offsetY = 20f, scale = 0.5f)
            .applyGestureTransform(
                centroid = Offset(100f, 100f),
                pan = Offset(24f, -18f),
                zoom = 1f,
                density = 2f,
            )

        assertClose(34f, transformed.offsetX)
        assertClose(2f, transformed.offsetY)
        assertClose(0.5f, transformed.scale)
    }

    @Test
    fun simultaneousPanAndZoomMovesTheOldCentroidToTheNewCentroid() {
        val original = BoardCamera(offsetX = 20f, offsetY = -10f, scale = 0.5f)
        val oldCentroid = Offset(200f, 160f)
        val pan = Offset(30f, -12f)
        val density = 2f
        val worldX = (oldCentroid.x - original.offsetX) / (original.scale * density)
        val worldY = (oldCentroid.y - original.offsetY) / (original.scale * density)

        val transformed = original.applyGestureTransform(
            centroid = oldCentroid,
            pan = pan,
            zoom = 1.8f,
            density = density,
        )

        assertClose(
            oldCentroid.x + pan.x,
            worldX * density * transformed.scale + transformed.offsetX,
        )
        assertClose(
            oldCentroid.y + pan.y,
            worldY * density * transformed.scale + transformed.offsetY,
        )
    }

    @Test
    fun clampedZoomKeepsTheCentroidStableBeforeApplyingPan() {
        val original = BoardCamera(offsetX = 15f, offsetY = 25f, scale = 2.9f)
        val centroid = Offset(140f, 210f)
        val pan = Offset(-8f, 11f)
        val density = 1.5f
        val worldX = (centroid.x - original.offsetX) / (original.scale * density)
        val worldY = (centroid.y - original.offsetY) / (original.scale * density)

        val transformed = original.applyGestureTransform(
            centroid = centroid,
            pan = pan,
            zoom = 2f,
            density = density,
        )

        assertEquals(BoardCamera.MAX_SCALE, transformed.scale)
        assertClose(
            centroid.x + pan.x,
            worldX * density * transformed.scale + transformed.offsetX,
        )
        assertClose(
            centroid.y + pan.y,
            worldY * density * transformed.scale + transformed.offsetY,
        )
    }

    @Test
    fun zeroPanAndUnitZoomAreANoOp() {
        val original = BoardCamera(offsetX = 12f, offsetY = 34f, scale = 0.75f)

        assertEquals(
            original,
            original.applyGestureTransform(
                centroid = Offset(100f, 100f),
                pan = Offset.Zero,
                zoom = 1f,
                density = 2f,
            ),
        )
    }

    @Test
    fun invalidTransformInputIsIgnored() {
        val original = BoardCamera(offsetX = 12f, offsetY = 34f, scale = 0.75f)

        assertEquals(
            original,
            original.applyGestureTransform(
                centroid = Offset.Unspecified,
                pan = Offset(5f, 5f),
                zoom = Float.NaN,
                density = 2f,
            ),
        )
    }

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.001f, "expected $expected, got $actual")
    }
}
