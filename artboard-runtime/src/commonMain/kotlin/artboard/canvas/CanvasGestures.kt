package artboard.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.exp

/**
 * Directly manipulate the canvas with a mouse or touch pointers.
 *
 * A single pointer pans. Multiple pointers pan and pinch-zoom around their
 * shared focal point. Listening during the initial event pass lets board
 * navigation win over draggable preview content once touch slop is crossed,
 * while unconsumed taps still reach previews and frame chrome.
 */
internal suspend fun PointerInputScope.detectCanvasTransforms(
    density: Float,
    camera: () -> BoardCamera,
    onCameraChange: (BoardCamera) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val touchSlop = viewConfiguration.touchSlop
        var accumulatedZoom = 1f
        var accumulatedPan = Offset.Zero
        var pastTouchSlop = false
        var canceled = false

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    accumulatedZoom *= zoomChange
                    accumulatedPan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1f - accumulatedZoom) * centroidSize
                    val panMotion = accumulatedPan.getDistance()
                    pastTouchSlop = zoomMotion > touchSlop || panMotion > touchSlop
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (centroid.isFinite() &&
                        (zoomChange != 1f || panChange != Offset.Zero)
                    ) {
                        onCameraChange(
                            camera().applyGestureTransform(
                                centroid = centroid,
                                pan = panChange,
                                zoom = zoomChange,
                                density = density,
                            ),
                        )
                    }
                    event.changes.forEach { change ->
                        if (change.positionChanged()) change.consume()
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

/**
 * Applies one transform event whose [centroid] is measured before [pan].
 */
internal fun BoardCamera.applyGestureTransform(
    centroid: Offset,
    pan: Offset,
    zoom: Float,
    density: Float,
): BoardCamera {
    if (!centroid.isFinite() || !zoom.isFinite() || zoom <= 0f) return this
    return zoomToward(
        focalViewport = centroid,
        factor = zoom,
        density = density,
    ).pan(pan)
}

private fun Offset.isFinite(): Boolean = x.isFinite() && y.isFinite()

/**
 * Process a scroll event:
 * - default -> **pan** (trackpad two-finger / mouse wheel)
 * - Ctrl / Cmd held -> **zoom** toward pointer
 *
 * Returns true if consumed.
 */
internal fun handleCanvasScroll(
    event: PointerEvent,
    density: Float,
    camera: BoardCamera,
    onCameraChange: (BoardCamera) -> Unit,
): Boolean {
    if (event.type != PointerEventType.Scroll) return false
    val change = event.changes.firstOrNull() ?: return false
    val dx = change.scrollDelta.x
    val dy = change.scrollDelta.y
    if (dx == 0f && dy == 0f) return false

    val zoomModifier = event.keyboardModifiers.isCtrlPressed ||
        event.keyboardModifiers.isMetaPressed

    if (zoomModifier) {
        val scroll = if (abs(dy) >= abs(dx)) dy else dx
        // Smooth exponential zoom; scroll-up (negative dy) zooms in.
        val factor = exp(-scroll * 0.09f).coerceIn(0.82f, 1.22f)
        onCameraChange(camera.zoomToward(change.position, factor, density))
    } else {
        // Content follows fingers (natural). Slight gain for trackpad fluidity.
        val gain = 1.4f
        onCameraChange(camera.pan(Offset(-dx * gain, -dy * gain)))
    }
    change.consume()
    return true
}
