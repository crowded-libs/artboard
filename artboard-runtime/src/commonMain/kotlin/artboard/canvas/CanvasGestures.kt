package artboard.canvas

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import kotlin.math.abs
import kotlin.math.exp

/**
 * Primary-button drag pans the canvas (Figma hand-tool-like free drag).
 */
internal suspend fun PointerInputScope.detectCanvasDragPan(
    camera: () -> BoardCamera,
    onCameraChange: (BoardCamera) -> Unit,
) {
    detectDragGestures { change, dragAmount ->
        change.consume()
        if (dragAmount != Offset.Zero) {
            onCameraChange(camera().pan(dragAmount))
        }
    }
}

/**
 * Process a scroll event:
 * - default → **pan** (trackpad two-finger / mouse wheel)
 * - Ctrl / ⌘ held → **zoom** toward pointer
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
