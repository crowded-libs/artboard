package artboard.canvas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import artboard.capture.PreviewCaptureState
import artboard.capture.capturePreviewLayer
import artboard.capture.previewCaptureSpec
import artboard.capture.previewImageDownloadsSupported
import artboard.host.LocalStudioColors
import artboard.host.PreviewFrameEnvironment
import artboard.host.Studio
import artboard.host.StudioText
import artboard.model.PreviewKind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrameChrome(
    placed: PlacedFrame,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier,
    previewLocaleTag: String? = null,
    /**
     * When true and this frame is a [PreviewKind.Screen], paint a translucent
     * column/margin/gutter layout grid over the preview (design-system check).
     * Independent of the always-on board canvas graph paper.
     */
    showScreenLayoutGrid: Boolean = false,
    /** Column count for the screen layout grid overlay. */
    layoutGridColumns: Int = 6,
    /** Gutter width in dp between columns. */
    layoutGridGutterDp: Int = 8,
) {
    val frame = placed.frame
    val isScreen = frame.kind == PreviewKind.Screen
    val corner = if (isScreen) 12.dp else 8.dp
    val colors = LocalStudioColors.current
    val captureLayer = rememberGraphicsLayer()
    val graphicsContext = LocalGraphicsContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val captureScope = rememberCoroutineScope()
    var captureState by remember(frame.id) { mutableStateOf(PreviewCaptureState.Idle) }
    val captureSpec = remember(
        frame.id,
        frame.kind,
        frame.name,
        frame.sourceFqName,
        placed.width,
        placed.height,
        colors.isDark,
        previewLocaleTag,
    ) {
        previewCaptureSpec(
            frame = frame,
            logicalWidth = placed.width.roundToInt().coerceAtLeast(1),
            logicalHeight = placed.height.roundToInt().coerceAtLeast(1),
            darkTheme = colors.isDark,
            localeTag = previewLocaleTag,
        )
    }

    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> colors.frameBorderSelected
            hovered -> colors.frameBorderHover
            isScreen -> colors.lineStrong
            else -> colors.frameBorder
        },
        animationSpec = tween(140),
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(140),
    )
    val shadowElevation by animateDpAsState(
        targetValue = when {
            selected || hovered -> 6.dp
            isScreen -> 3.dp
            else -> 1.5.dp
        },
        animationSpec = tween(180),
    )
    // Registration ticks "draw in" on selection: 0f collapsed → 1f full arms.
    val tickProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
    )
    val tickColor = colors.frameBorderSelected

    // Frame chrome labels stay LTR with the board; only the surface body localizes.
    Column(
        modifier = modifier
            .width(placed.width.dp)
            .height((placed.labelHeight + placed.height).dp)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .semantics {
                role = Role.Button
                contentDescription = "${frame.name}, ${frame.kind.name.lowercase()} preview"
                this.selected = selected
            }
            // No ripple: the animated border and shadow are the interaction feedback.
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(placed.labelHeight.dp)
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StudioText(
                    text = frame.name,
                    style = Studio.type.frameName,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                KindBadge(kind = frame.kind)
                if (previewImageDownloadsSupported) {
                    PreviewDownloadButton(
                        state = captureState,
                        contentDescription = buildString {
                            append("Download ")
                            append(frame.name)
                            append(" as ")
                            append(captureSpec.pixelSize.width)
                            append(" by ")
                            append(captureSpec.pixelSize.height)
                            append(" PNG")
                        },
                        onClick = {
                            captureState = PreviewCaptureState.Capturing
                            captureScope.launch {
                                val result = capturePreviewLayer(
                                    sourceLayer = captureLayer,
                                    graphicsContext = graphicsContext,
                                    density = density,
                                    layoutDirection = layoutDirection,
                                    spec = captureSpec,
                                )
                                result.fold(
                                    onSuccess = {
                                        captureState = PreviewCaptureState.Complete
                                        delay(1_500)
                                        if (captureState == PreviewCaptureState.Complete) {
                                            captureState = PreviewCaptureState.Idle
                                        }
                                    },
                                    onFailure = { failure ->
                                        println("Artboard: preview download failed: ${failure.message}")
                                        captureState = PreviewCaptureState.Failed
                                    },
                                )
                            }
                        },
                    )
                }
            }
            StudioText(
                text = frame.id.substringAfterLast('.').ifEmpty { frame.id },
                style = Studio.type.mono,
                color = colors.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .width(placed.width.dp)
                .height(placed.height.dp)
                .drawBehind {
                    if (tickProgress > 0f) {
                        drawRegistrationTicks(
                            color = tickColor,
                            progress = tickProgress,
                            armLength = 10.dp.toPx(),
                            outset = 4.dp.toPx(),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                    }
                }
                .shadow(
                    elevation = shadowElevation,
                    shape = RoundedCornerShape(corner),
                    ambientColor = colors.shadow,
                    spotColor = colors.shadow,
                )
                // The frame is the consumer's canvas: a plain surface the preview
                // fills. It defaults to the host paper so a transparent preview
                // still reads, but the body paints over it.
                .background(colors.surfaceRaised, RoundedCornerShape(corner))
                .border(borderWidth, borderColor, RoundedCornerShape(corner))
                .clip(RoundedCornerShape(corner)),
        ) {
            // Preview bodies only: system theme + inspection mode (IDE @Preview parity).
            // Gallery chrome stays outside this provider.
            PreviewFrameEnvironment(
                isDark = colors.isDark,
                localeTag = previewLocaleTag,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                captureLayer.record {
                                    if (captureSpec.opaque) {
                                        drawRect(colors.surfaceRaised)
                                    }
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(captureLayer)
                            },
                    ) {
                        frame.content()
                    }
                    // Column/margin/gutter layout grid on screens only (design-system check).
                    if (showScreenLayoutGrid && isScreen) {
                        ColumnLayoutGrid(
                            modifier = Modifier.fillMaxSize(),
                            columns = layoutGridColumns,
                            margin = 16.dp,
                            gutter = layoutGridGutterDp.dp,
                            columnColor = colors.columnOverlay,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Four L-shaped registration marks just outside the frame corners — the
 * board's selection signature, echoing print/drafting crop marks.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRegistrationTicks(
    color: androidx.compose.ui.graphics.Color,
    progress: Float,
    armLength: Float,
    outset: Float,
    strokeWidth: Float,
) {
    val arm = armLength * progress
    if (arm <= 0f) return
    val left = -outset
    val top = -outset
    val right = size.width + outset
    val bottom = size.height + outset
    val stroke = Stroke(width = strokeWidth)

    fun tick(cx: Float, cy: Float, dx: Float, dy: Float) {
        drawLine(color, Offset(cx, cy), Offset(cx + arm * dx, cy), strokeWidth = stroke.width)
        drawLine(color, Offset(cx, cy), Offset(cx, cy + arm * dy), strokeWidth = stroke.width)
    }
    tick(left, top, dx = 1f, dy = 1f)
    tick(right, top, dx = -1f, dy = 1f)
    tick(left, bottom, dx = 1f, dy = -1f)
    tick(right, bottom, dx = -1f, dy = -1f)
}

@Composable
private fun KindBadge(kind: PreviewKind) {
    val colors = LocalStudioColors.current
    val label = if (kind == PreviewKind.Screen) "SCREEN" else "COMPONENT"
    val bg = if (kind == PreviewKind.Screen) colors.screenBadge else colors.componentBadge
    val fg = if (kind == PreviewKind.Screen) colors.onScreenBadge else colors.onComponentBadge
    StudioText(
        text = label,
        style = Studio.type.badge,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun PreviewDownloadButton(
    state: PreviewCaptureState,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = LocalStudioColors.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val enabled = state != PreviewCaptureState.Capturing
    val label = when (state) {
        PreviewCaptureState.Idle -> "PNG ↓"
        PreviewCaptureState.Capturing -> "PNG …"
        PreviewCaptureState.Complete -> "PNG ✓"
        PreviewCaptureState.Failed -> "PNG !"
    }
    val status = when (state) {
        PreviewCaptureState.Idle -> "Ready"
        PreviewCaptureState.Capturing -> "Creating image"
        PreviewCaptureState.Complete -> "Downloaded"
        PreviewCaptureState.Failed -> "Download failed; activate to retry"
    }

    StudioText(
        text = label,
        style = Studio.type.mono,
        color = when (state) {
            PreviewCaptureState.Complete -> colors.accentInk
            PreviewCaptureState.Failed -> colors.accent
            else -> colors.inkSoft
        },
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (hovered && enabled) colors.accentWash else colors.surface)
            .hoverable(interaction, enabled = enabled)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = status
            }
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}
