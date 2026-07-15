package artboard.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.max
import kotlin.math.min

/**
 * Viewport camera over world space.
 *
 * World layout units are **dp**. [offsetX]/[offsetY] are **viewport pixels**.
 * [scale] multiplies the rendered world (after density conversion).
 *
 * Screen position of world point (wxDp, wyDp):
 *   sx = wxDp * density * scale + offsetX
 */
data class BoardCamera(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
) {
    fun zoomToward(
        focalViewport: Offset,
        factor: Float,
        density: Float,
        minScale: Float = MIN_SCALE,
        maxScale: Float = MAX_SCALE,
    ): BoardCamera {
        val d = density.coerceAtLeast(0.01f)
        val newScale = (scale * factor).coerceIn(minScale, maxScale)
        if (newScale == scale) return this
        val worldXDp = (focalViewport.x - offsetX) / (scale * d)
        val worldYDp = (focalViewport.y - offsetY) / (scale * d)
        return BoardCamera(
            offsetX = focalViewport.x - worldXDp * d * newScale,
            offsetY = focalViewport.y - worldYDp * d * newScale,
            scale = newScale,
        )
    }

    fun pan(deltaViewport: Offset): BoardCamera =
        copy(offsetX = offsetX + deltaViewport.x, offsetY = offsetY + deltaViewport.y)

    companion object {
        const val MIN_SCALE = 0.03f
        const val MAX_SCALE = 3f

        /**
         * Fit [worldBoundsDp] into the viewport.
         *
         * Both dimensions are respected so the complete board remains visible.
         */
        fun fit(
            worldBoundsDp: Rect,
            viewportSizePx: Size,
            density: Float,
            paddingPx: Float = 48f,
            maxScale: Float = 1f,
        ): BoardCamera {
            if (worldBoundsDp.isEmpty ||
                viewportSizePx.width <= 0f ||
                viewportSizePx.height <= 0f
            ) {
                return BoardCamera()
            }
            val d = density.coerceAtLeast(0.01f)
            val worldW = max(1f, worldBoundsDp.width * d)
            val worldH = max(1f, worldBoundsDp.height * d)
            val availW = max(1f, viewportSizePx.width - paddingPx * 2)
            val availH = max(1f, viewportSizePx.height - paddingPx * 2)
            val rawScale = min(availW / worldW, availH / worldH)
            val scale = rawScale.coerceIn(MIN_SCALE, maxScale.coerceAtMost(MAX_SCALE))
            val renderedWidth = worldW * scale
            val renderedHeight = worldH * scale
            return BoardCamera(
                offsetX = (viewportSizePx.width - renderedWidth) / 2f -
                    worldBoundsDp.left * d * scale,
                offsetY = (viewportSizePx.height - renderedHeight) / 2f -
                    worldBoundsDp.top * d * scale,
                scale = scale,
            )
        }

        fun fitPlaced(
            placed: PlacedFrame,
            viewportSizePx: Size,
            density: Float,
            paddingPx: Float = 56f,
        ): BoardCamera = fit(
            worldBoundsDp = placed.bounds,
            viewportSizePx = viewportSizePx,
            density = density,
            paddingPx = paddingPx,
            maxScale = MAX_SCALE,
        )
    }
}
