package artboard.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import artboard.host.ArtboardResourceLocaleProvider
import artboard.host.LocalStudioColors
import artboard.host.Studio
import artboard.host.StudioText
import artboard.registry.ArtboardRegistry
import kotlin.math.exp
import kotlin.math.ln
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A one-shot request to move the camera to [frameId]. */
data class ArtboardFocusRequest(
    val frameId: String,
    val token: Long,
)

/**
 * Spatial board: auto-laid-out frames under a pan/zoom camera.
 *
 * Navigation (Figma-like):
 * - Trackpad / wheel scroll → pan
 * - Ctrl/⌘ + scroll → zoom toward pointer
 * - Mouse or one-finger drag → pan
 * - Two-finger gesture → pan and pinch-zoom
 *
 * Board canvas always shows a full-viewport graph-paper grid.
 * [showScreenLayoutGrid] toggles the column/margin/gutter overlay on screen frames only.
 */
@Composable
fun ArtboardSurface(
    registry: ArtboardRegistry,
    modifier: Modifier = Modifier,
    query: String = "",
    kindFilter: KindFilter = KindFilter.All,
    selectedFrameId: String? = null,
    onFrameSelected: (String?) -> Unit = {},
    /** Column layout grid on [PreviewKind.Screen] frames (not the board canvas). */
    showScreenLayoutGrid: Boolean = true,
    /** Screen layout grid column count (design-system overlay). */
    layoutGridColumns: Int = 4,
    /** Screen layout grid gutter width in dp. */
    layoutGridGutterDp: Int = 16,
    /** Device viewport applied to every Screen frame; null keeps declared sizes. */
    screenSize: ScreenDeviceSize? = null,
    camera: BoardCamera,
    onCameraChange: (BoardCamera) -> Unit,
    fitToken: Int = 0,
    focusRequest: ArtboardFocusRequest? = null,
    onFocusRequestConsumed: (Long) -> Unit = {},
    onViewportSizeChange: (Size) -> Unit = {},
    /** Gallery preview locale — applied only inside frame bodies (not chrome). */
    previewLocaleTag: String? = null,
) {
    val frames = registry.frames
    val layout = remember(frames, query, kindFilter, screenSize) {
        layoutBoard(
            frames = frames,
            query = query,
            kindFilter = kindFilter,
            selectedGroups = emptySet(),
            screenSize = screenSize,
        )
    }
    val density = LocalDensity.current
    val densityValue = density.density

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val cameraState = rememberUpdatedState(camera)
    val onCameraRef = rememberUpdatedState(onCameraChange)

    val scope = rememberCoroutineScope()
    // A user gesture cancels any in-flight camera move; the gesture wins.
    val cameraAnimJob = remember { mutableStateOf<Job?>(null) }
    fun flyTo(target: BoardCamera) {
        cameraAnimJob.value?.cancel()
        cameraAnimJob.value = scope.launch {
            animateCamera(cameraState.value, target) { onCameraRef.value(it) }
        }
    }
    fun cancelCameraFlight() {
        cameraAnimJob.value?.cancel()
    }

    var initialCameraApplied by remember { mutableStateOf(false) }
    LaunchedEffect(viewportSize.width, viewportSize.height, layout.bounds, densityValue) {
        if (!initialCameraApplied && viewportSize.width > 0 && viewportSize.height > 0 && !layout.bounds.isEmpty) {
            initialCameraApplied = true
            if (focusRequest == null) {
                flyTo(
                    BoardCamera.fit(
                        worldBoundsDp = layout.bounds,
                        viewportSizePx = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat()),
                        density = densityValue,
                    ),
                )
            }
        }
    }

    val fitRequests = remember { FitRequestTracker(fitToken) }
    LaunchedEffect(fitToken, viewportSize.width, viewportSize.height, layout.bounds, densityValue) {
        val ready = viewportSize.width > 0 && viewportSize.height > 0 && !layout.bounds.isEmpty
        if (fitRequests.consumeIfReady(fitToken, ready)) {
            flyTo(
                BoardCamera.fit(
                    worldBoundsDp = layout.bounds,
                    viewportSizePx = Size(
                        viewportSize.width.toFloat(),
                        viewportSize.height.toFloat(),
                    ),
                    density = densityValue,
                ),
            )
        }
    }

    LaunchedEffect(focusRequest?.token, layout, viewportSize, densityValue) {
        val request = focusRequest ?: return@LaunchedEffect
        val id = request.frameId
        val placed = layout.placed.find { it.frame.id == id } ?: return@LaunchedEffect
        if (viewportSize.width > 0 && viewportSize.height > 0) {
            flyTo(
                BoardCamera.fitPlaced(
                    placed = placed,
                    viewportSizePx = Size(
                        viewportSize.width.toFloat(),
                        viewportSize.height.toFloat(),
                    ),
                    density = densityValue,
                ),
            )
            onFocusRequestConsumed(request.token)
        }
    }

    val colors = LocalStudioColors.current
    val canvasBg = colors.canvas
    val gridMinor = colors.gridMinor
    val gridMajor = colors.gridMajor

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            // Panned/zoomed frames must slide under the toolbar chrome, never
            // paint over it.
            .clipToBounds()
            .onSizeChanged {
                viewportSize = it
                onViewportSizeChange(Size(it.width.toFloat(), it.height.toFloat()))
            }
            .pointerInput(densityValue) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        handleCanvasScroll(
                            event = event,
                            density = densityValue,
                            camera = cameraState.value,
                            onCameraChange = {
                                cancelCameraFlight()
                                onCameraRef.value(it)
                            },
                        )
                    }
                }
            }
            .pointerInput(densityValue) {
                detectCanvasTransforms(
                    density = densityValue,
                    camera = { cameraState.value },
                    onCameraChange = {
                        cancelCameraFlight()
                        onCameraRef.value(it)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onFrameSelected(null) })
            },
    ) {
        // Full-panel graph paper — always on; spans the entire board surface.
        DesignSystemGrid(
            modifier = Modifier.fillMaxSize(),
            minorStep = 8.dp,
            majorEvery = 8,
            minorColor = gridMinor,
            majorColor = gridMajor,
            scale = camera.scale,
        )

        if (layout.placed.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (frames.isEmpty()) {
                    EmptyArtboardMessage()
                } else {
                    StudioText(
                        text = if (query.isNotBlank()) {
                            "No frames match \"$query\" — clear the search or switch the kind filter."
                        } else {
                            "No frames match the current filter — switch it back to All."
                        },
                        style = Studio.type.body,
                        color = colors.inkSoft,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@Box
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = camera.offsetX
                    translationY = camera.offsetY
                    scaleX = camera.scale
                    scaleY = camera.scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
                // The board is an infinite canvas: without unbounded measurement,
                // Modifier.height() on frames silently coerces into the viewport
                // constraints, squashing any frame taller than the window.
                .wrapContentSize(align = Alignment.TopStart, unbounded = true),
        ) {
            layout.zoneHeaders.forEach { header ->
                StudioText(
                    text = header.title,
                    style = Studio.type.zoneHeader,
                    color = colors.ink,
                    modifier = Modifier.offset(header.x.dp, header.y.dp),
                )
            }

            layout.groupHeaders.forEach { header ->
                StudioText(
                    text = header.title,
                    style = Studio.type.groupHeader,
                    color = colors.inkSoft,
                    modifier = Modifier.offset(header.x.dp, header.y.dp),
                )
            }

            ArtboardResourceLocaleProvider(localeTag = previewLocaleTag) {
                layout.placed.forEach { placed ->
                    FrameChrome(
                        placed = placed,
                        selected = placed.frame.id == selectedFrameId,
                        previewLocaleTag = previewLocaleTag,
                        showScreenLayoutGrid = showScreenLayoutGrid,
                        layoutGridColumns = layoutGridColumns,
                        layoutGridGutterDp = layoutGridGutterDp,
                        onClick = { onFrameSelected(placed.frame.id) },
                        onDoubleClick = {
                            onFrameSelected(placed.frame.id)
                            if (viewportSize.width > 0) {
                                flyTo(
                                    BoardCamera.fitPlaced(
                                        placed = placed,
                                        viewportSizePx = Size(
                                            viewportSize.width.toFloat(),
                                            viewportSize.height.toFloat(),
                                        ),
                                        density = densityValue,
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.offset(placed.x.dp, placed.y.dp),
                    )
                }
            }
        }
    }
}

internal class FitRequestTracker(initialToken: Int) {
    private var consumedToken = initialToken

    fun consumeIfReady(token: Int, ready: Boolean): Boolean {
        if (!ready || token == consumedToken) return false
        consumedToken = token
        return true
    }
}

@Composable
private fun EmptyArtboardMessage() {
    val colors = LocalStudioColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StudioText(
            text = "Artboard",
            style = Studio.type.zoneHeader,
            color = colors.ink,
        )
        StudioText(
            text = "No previews yet — annotate a composable with @Preview and rebuild.",
            style = Studio.type.body,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Glides the camera from [from] to [to]: offsets interpolate linearly, scale in
 * log space so zooming reads at constant perceptual speed.
 */
private suspend fun animateCamera(
    from: BoardCamera,
    to: BoardCamera,
    onFrame: (BoardCamera) -> Unit,
) {
    if (from == to) return
    val progress = Animatable(0f)
    val lnFrom = ln(from.scale)
    val lnTo = ln(to.scale)
    progress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
    ) {
        val t = value
        onFrame(
            BoardCamera(
                offsetX = from.offsetX + (to.offsetX - from.offsetX) * t,
                offsetY = from.offsetY + (to.offsetY - from.offsetY) * t,
                scale = exp(lnFrom + (lnTo - lnFrom) * t),
            ),
        )
    }
}
