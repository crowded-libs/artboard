package artboard.canvas

import androidx.compose.ui.geometry.Rect
import artboard.model.PreviewFrame
import artboard.model.PreviewKind

/** How the chrome filters kinds. */
enum class KindFilter {
    All,
    Screens,
    Components,
}

/**
 * Placed frame in **world dp** coordinates.
 * [x]/[y] are the top-left of the full chrome block (labels + surface).
 */
data class PlacedFrame(
    val frame: PreviewFrame,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val labelHeight: Float,
) {
    val totalHeight: Float get() = labelHeight + height
    val bounds: Rect
        get() = Rect(x, y, x + width, y + totalHeight)
}

data class ZoneHeader(
    val kind: PreviewKind,
    val title: String,
    val x: Float,
    val y: Float,
)

data class GroupHeader(
    val title: String,
    val x: Float,
    val y: Float,
)

data class BoardLayoutResult(
    val placed: List<PlacedFrame>,
    val zoneHeaders: List<ZoneHeader>,
    val groupHeaders: List<GroupHeader>,
    val bounds: Rect,
)

/**
 * Layout constants in **world dp** (must match visual sizes used with `.dp` modifiers).
 */
object BoardLayoutDefaults {
    const val OUTER_PADDING = 48f
    const val ZONE_GAP = 72f
    /** Room for "Screens" / "Components" headline + breathing room. */
    const val ZONE_HEADER_HEIGHT = 44f
    /** Room for group title + gap before frames. */
    const val GROUP_HEADER_HEIGHT = 36f
    /** Name row + id row under FrameChrome. */
    const val LABEL_HEIGHT = 44f

    const val SCREEN_DEFAULT_W = 360f
    const val SCREEN_DEFAULT_H = 640f
    const val SCREEN_GAP_X = 56f
    const val SCREEN_GAP_Y = 56f
    const val SCREEN_GROUP_GAP = 48f
    /**
     * Default number of screen frames per row before wrapping.
     * Count-based packing (see [layoutBoard] `screensPerRow`).
     */
    const val SCREEN_DEFAULT_PER_ROW = 5

    const val COMPONENT_DEFAULT_W = 320f
    const val COMPONENT_DEFAULT_H = 280f
    const val COMPONENT_GAP_X = 48f
    const val COMPONENT_GAP_Y = 56f
    const val COMPONENT_GROUP_GAP = 48f
    /** Max world-dp width of a component row; ~5 default-width cards. */
    const val COMPONENT_MAX_ROW = 2000f
}

/**
 * Auto-layout: Screens zone above Components zone; groups flow L→R with wrap.
 * All units are world **dp**.
 */
/**
 * Optional device viewport applied to every [PreviewKind.Screen] frame,
 * overriding per-preview `widthDp`/`heightDp` so the whole Screens zone
 * renders at one real device size (e.g. iPhone 17 = 402×874).
 */
data class ScreenDeviceSize(val widthDp: Int, val heightDp: Int)

fun layoutBoard(
    frames: List<PreviewFrame>,
    query: String = "",
    kindFilter: KindFilter = KindFilter.All,
    selectedGroups: Set<String> = emptySet(),
    screenSize: ScreenDeviceSize? = null,
    /** Max screen frames per row before wrapping. Components still use max-row-dp packing. */
    screensPerRow: Int = BoardLayoutDefaults.SCREEN_DEFAULT_PER_ROW,
): BoardLayoutResult {
    val q = query.trim()
    val screenRowLimit = screensPerRow.coerceAtLeast(1)
    val filtered = frames.filter { frame ->
        when (kindFilter) {
            KindFilter.All -> true
            KindFilter.Screens -> frame.kind == PreviewKind.Screen
            KindFilter.Components -> frame.kind == PreviewKind.Component
        } &&
            (selectedGroups.isEmpty() || (frame.group ?: "Ungrouped") in selectedGroups) &&
            (q.isEmpty() || frame.matchesQuery(q))
    }

    if (filtered.isEmpty()) {
        return BoardLayoutResult(
            placed = emptyList(),
            zoneHeaders = emptyList(),
            groupHeaders = emptyList(),
            bounds = Rect.Zero,
        )
    }

    val placed = mutableListOf<PlacedFrame>()
    val zoneHeaders = mutableListOf<ZoneHeader>()
    val groupHeaders = mutableListOf<GroupHeader>()

    var cursorY = BoardLayoutDefaults.OUTER_PADDING
    val outerX = BoardLayoutDefaults.OUTER_PADDING

    val zones = listOf(PreviewKind.Screen, PreviewKind.Component)
    var firstZone = true

    for (kind in zones) {
        val zoneFrames = filtered.filter { it.kind == kind }
        if (zoneFrames.isEmpty()) continue

        if (!firstZone) {
            cursorY += BoardLayoutDefaults.ZONE_GAP
        }
        firstZone = false

        zoneHeaders += ZoneHeader(
            kind = kind,
            title = if (kind == PreviewKind.Screen) "Screens" else "Components",
            x = outerX,
            y = cursorY,
        )
        cursorY += BoardLayoutDefaults.ZONE_HEADER_HEIGHT

        val metrics = kind.metrics()
        val groups = zoneFrames
            .groupBy { it.group ?: "Ungrouped" }
            .toList()
            .sortedWith(
                compareBy<Pair<String, List<PreviewFrame>>> { (name, _) ->
                    if (name == "Ungrouped") 1 else 0
                }.thenBy { it.first.lowercase() },
            )

        var firstGroup = true
        for ((groupName, groupFrames) in groups) {
            if (!firstGroup) {
                cursorY += metrics.groupGap
            }
            firstGroup = false

            groupHeaders += GroupHeader(
                title = groupName,
                x = outerX,
                y = cursorY,
            )
            cursorY += BoardLayoutDefaults.GROUP_HEADER_HEIGHT

            var rowX = outerX
            var rowY = cursorY
            var rowHeight = 0f
            var framesInRow = 0
            val sorted = groupFrames.sortedBy { it.id }

            for (frame in sorted) {
                val deviceOverride = screenSize.takeIf { frame.kind == PreviewKind.Screen }
                val w = deviceOverride?.widthDp?.toFloat()
                    ?: frame.widthDp?.toFloat()
                    ?: metrics.defaultW
                val h = deviceOverride?.heightDp?.toFloat()
                    ?: frame.heightDp?.toFloat()
                    ?: metrics.defaultH
                val blockH = BoardLayoutDefaults.LABEL_HEIGHT + h

                val shouldWrap = when (kind) {
                    PreviewKind.Screen -> framesInRow >= screenRowLimit
                    PreviewKind.Component ->
                        rowX > outerX && rowX + w > outerX + metrics.maxRowDp
                }
                if (shouldWrap) {
                    rowX = outerX
                    rowY += rowHeight + metrics.gapY
                    rowHeight = 0f
                    framesInRow = 0
                }

                placed += PlacedFrame(
                    frame = frame,
                    x = rowX,
                    y = rowY,
                    width = w,
                    height = h,
                    labelHeight = BoardLayoutDefaults.LABEL_HEIGHT,
                )
                rowX += w + metrics.gapX
                rowHeight = maxOf(rowHeight, blockH)
                framesInRow++
            }
            cursorY = rowY + rowHeight
        }
    }

    val bounds = if (placed.isEmpty()) {
        Rect.Zero
    } else {
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        for (p in placed) {
            val b = p.bounds
            left = minOf(left, b.left)
            top = minOf(top, b.top)
            right = maxOf(right, b.right)
            bottom = maxOf(bottom, b.bottom)
        }
        for (h in zoneHeaders) {
            left = minOf(left, h.x)
            top = minOf(top, h.y)
        }
        // Tight content bounds for fit (small pad only).
        val pad = 24f
        Rect(left - pad, top - pad, right + pad, bottom + pad)
    }

    return BoardLayoutResult(
        placed = placed,
        zoneHeaders = zoneHeaders,
        groupHeaders = groupHeaders,
        bounds = bounds,
    )
}

private data class KindMetrics(
    val defaultW: Float,
    val defaultH: Float,
    val gapX: Float,
    val gapY: Float,
    val groupGap: Float,
    /** Used for component width-based wrapping only; screens use [screensPerRow]. */
    val maxRowDp: Float,
)

private fun PreviewKind.metrics(): KindMetrics = when (this) {
    PreviewKind.Screen -> KindMetrics(
        defaultW = BoardLayoutDefaults.SCREEN_DEFAULT_W,
        defaultH = BoardLayoutDefaults.SCREEN_DEFAULT_H,
        gapX = BoardLayoutDefaults.SCREEN_GAP_X,
        gapY = BoardLayoutDefaults.SCREEN_GAP_Y,
        groupGap = BoardLayoutDefaults.SCREEN_GROUP_GAP,
        maxRowDp = Float.POSITIVE_INFINITY,
    )
    PreviewKind.Component -> KindMetrics(
        defaultW = BoardLayoutDefaults.COMPONENT_DEFAULT_W,
        defaultH = BoardLayoutDefaults.COMPONENT_DEFAULT_H,
        gapX = BoardLayoutDefaults.COMPONENT_GAP_X,
        gapY = BoardLayoutDefaults.COMPONENT_GAP_Y,
        groupGap = BoardLayoutDefaults.COMPONENT_GROUP_GAP,
        maxRowDp = BoardLayoutDefaults.COMPONENT_MAX_ROW,
    )
}

private fun PreviewFrame.matchesQuery(q: String): Boolean {
    val n = q.lowercase()
    return name.lowercase().contains(n) ||
        id.lowercase().contains(n) ||
        (sourceFqName?.lowercase()?.contains(n) == true) ||
        (group?.lowercase()?.contains(n) == true)
}
