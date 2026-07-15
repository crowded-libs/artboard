package artboard.canvas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import artboard.host.LocalStudioColors
import artboard.host.PreviewContentLocale
import artboard.host.Studio
import artboard.host.StudioText
import artboard.model.PreviewKind

@OptIn(ExperimentalFoundationApi::class, InternalComposeUiApi::class)
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
    layoutGridColumns: Int = 4,
    /** Gutter width in dp between columns. */
    layoutGridGutterDp: Int = 16,
) {
    val frame = placed.frame
    val isScreen = frame.kind == PreviewKind.Screen
    val corner = if (isScreen) 12.dp else 8.dp
    val colors = LocalStudioColors.current

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
            // The host imposes no theme on frame bodies. It publishes the standard
            // LocalSystemTheme signal so isSystemInDarkTheme() works without an
            // Artboard dependency in consumer source.
            CompositionLocalProvider(
                LocalSystemTheme provides if (colors.isDark) SystemTheme.Dark else SystemTheme.Light,
            ) {
                PreviewContentLocale(localeTag = previewLocaleTag) {
                    Box(Modifier.fillMaxSize()) {
                        frame.content()
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
