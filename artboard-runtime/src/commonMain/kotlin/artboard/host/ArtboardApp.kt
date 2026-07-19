package artboard.host

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import artboard.canvas.ArtboardSurface
import artboard.canvas.BoardCamera
import artboard.canvas.ArtboardFocusRequest
import artboard.canvas.BoardLayoutDefaults
import artboard.canvas.KindFilter
import artboard.canvas.ScreenDeviceSize
import artboard.registry.ArtboardRegistry

/**
 * Top-level Artboard shell: the "Drafting Studio" chrome (see [StudioTheme])
 * wrapped around the spatial [ArtboardSurface]. Built entirely on Compose
 * foundation — no Material — so the host imposes nothing on the previews it hangs.
 *
 * View preferences (grid, screens-per-row, theme, device, camera) are restored
 * from platform storage on Wasm so a hard refresh keeps your last layout.
 *
 * @param supportedLocales languages offered in the locale control — pass only tags
 *   that exist in the project's `composeResources/values*` folders (plus System).
 *   When empty or only [ArtboardLocale.System], the locale control is hidden.
 */
@Composable
fun ArtboardApp(
    registry: ArtboardRegistry,
    modifier: Modifier = Modifier,
    title: String = "Artboard",
    initialDarkTheme: Boolean = false,
    selectedFrameId: String? = null,
    focusRequest: ArtboardFocusRequest? = null,
    onFocusRequestConsumed: (Long) -> Unit = {},
    onSelectedFrameIdChange: (String?) -> Unit = {},
    supportedLocales: List<ArtboardLocale> = listOf(ArtboardLocale.System),
) {
    val prefsNamespace = remember(title) { galleryPreferencesNamespace(title) }
    val restored = remember(prefsNamespace) { loadGalleryPreferences(prefsNamespace) }
    val skipInitialFit = restored?.camera != null

    var darkTheme by rememberSaveable {
        mutableStateOf(restored?.darkTheme ?: initialDarkTheme)
    }
    var query by rememberSaveable { mutableStateOf("") }
    var kindFilterName by rememberSaveable { mutableStateOf(KindFilter.All.name) }
    val kindFilter = KindFilter.entries.find { it.name == kindFilterName } ?: KindFilter.All
    /** Screen layout grid overlay (columns/gutters) — off by default; board graph paper is always on. */
    var showScreenLayoutGrid by rememberSaveable {
        mutableStateOf(restored?.showScreenLayoutGrid ?: false)
    }
    var layoutGridColumns by rememberSaveable {
        mutableIntStateOf(restored?.layoutGridColumns ?: GalleryPreferences.DEFAULT_LAYOUT_GRID_COLUMNS)
    }
    var layoutGridGutterDp by rememberSaveable {
        mutableIntStateOf(restored?.layoutGridGutterDp ?: GalleryPreferences.DEFAULT_LAYOUT_GRID_GUTTER_DP)
    }
    var screensPerRow by rememberSaveable {
        mutableIntStateOf(restored?.screensPerRow ?: BoardLayoutDefaults.SCREEN_DEFAULT_PER_ROW)
    }
    var localeTag by rememberSaveable { mutableStateOf<String?>(null) }
    // Device viewport for Screen frames, saved as "WxH"; empty = declared sizes.
    var deviceSpec by rememberSaveable {
        mutableStateOf(restored?.deviceSpec ?: "")
    }
    val screenDeviceSize = remember(deviceSpec) { parseDeviceSpec(deviceSpec) }
    var camera by remember { mutableStateOf(restored?.camera ?: BoardCamera()) }
    var viewportSize by remember { mutableStateOf(Size.Zero) }
    var fitToken by remember { mutableIntStateOf(0) }

    // Persist chrome + camera across browser reloads (debounced for pan/zoom).
    LaunchedEffect(
        prefsNamespace,
        showScreenLayoutGrid,
        layoutGridColumns,
        layoutGridGutterDp,
        screensPerRow,
        darkTheme,
        deviceSpec,
        camera.offsetX,
        camera.offsetY,
        camera.scale,
    ) {
        delay(200)
        saveGalleryPreferences(
            prefsNamespace,
            GalleryPreferences(
                showScreenLayoutGrid = showScreenLayoutGrid,
                layoutGridColumns = layoutGridColumns,
                layoutGridGutterDp = layoutGridGutterDp,
                screensPerRow = screensPerRow,
                darkTheme = darkTheme,
                deviceSpec = deviceSpec,
                camera = camera,
            ),
        )
    }

    val locales = remember(supportedLocales) {
        listOf(ArtboardLocale.System) + supportedLocales
            .filter { it.tag != null }
            .distinctBy { it.tag }
    }
    val showLocaleControl = locales.any { it.tag != null }

    // Drop selection if the project no longer lists that language.
    LaunchedEffect(locales) {
        if (localeTag != null && locales.none { it.tag == localeTag }) {
            localeTag = null
        }
    }

    val frames = registry.frames

    // Shell is always LTR + shell typography. Preview locale is resource-only here;
    // RTL/fonts for content apply inside each frame.
    StudioTheme(darkTheme = darkTheme) {
        val density = LocalDensity.current.density
        fun zoomAtCenter(factor: Float) {
            val focal = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
            camera = camera.zoomToward(focal, factor, density)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Studio.colors.canvas),
            ) {
                ArtboardToolbar(
                    title = title,
                    frameCount = frames.size,
                    selectedFrameLabel = selectedFrameId?.substringAfterLast("::"),
                    query = query,
                    onQueryChange = { query = it },
                    kindFilter = kindFilter,
                    onKindFilter = { kindFilterName = it.name },
                    locales = locales,
                    showLocaleControl = showLocaleControl,
                    localeTag = localeTag,
                    onLocaleTag = { localeTag = it },
                    deviceSize = screenDeviceSize,
                    onDeviceSize = { size ->
                        deviceSpec = size?.let { "${it.widthDp}x${it.heightDp}" } ?: ""
                    },
                    screensPerRow = screensPerRow,
                    onScreensPerRowChange = { screensPerRow = it },
                    scale = camera.scale,
                    onZoomOut = { zoomAtCenter(1f / 1.2f) },
                    onZoomIn = { zoomAtCenter(1.2f) },
                    onZoom100 = { zoomAtCenter(1f / camera.scale) },
                    onFit = { fitToken++ },
                    showScreenLayoutGrid = showScreenLayoutGrid,
                    onShowScreenLayoutGridChange = { showScreenLayoutGrid = it },
                    layoutGridColumns = layoutGridColumns,
                    onLayoutGridColumnsChange = { layoutGridColumns = it },
                    layoutGridGutterDp = layoutGridGutterDp,
                    onLayoutGridGutterDpChange = { layoutGridGutterDp = it },
                    darkTheme = darkTheme,
                    onToggleTheme = { darkTheme = !darkTheme },
                )

                ArtboardSurface(
                    registry = registry,
                    query = query,
                    kindFilter = kindFilter,
                    selectedFrameId = selectedFrameId,
                    onFrameSelected = onSelectedFrameIdChange,
                    showScreenLayoutGrid = showScreenLayoutGrid,
                    layoutGridColumns = layoutGridColumns,
                    layoutGridGutterDp = layoutGridGutterDp,
                    screenSize = screenDeviceSize,
                    screensPerRow = screensPerRow,
                    camera = camera,
                    onCameraChange = { camera = it },
                    fitToken = fitToken,
                    focusRequest = focusRequest,
                    onFocusRequestConsumed = onFocusRequestConsumed,
                    onViewportSizeChange = { viewportSize = it },
                    previewLocaleTag = localeTag,
                    skipInitialFit = skipInitialFit,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ArtboardToolbar(
    title: String,
    frameCount: Int,
    selectedFrameLabel: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    kindFilter: KindFilter,
    onKindFilter: (KindFilter) -> Unit,
    locales: List<ArtboardLocale>,
    showLocaleControl: Boolean,
    localeTag: String?,
    onLocaleTag: (String?) -> Unit,
    deviceSize: ScreenDeviceSize?,
    onDeviceSize: (ScreenDeviceSize?) -> Unit,
    screensPerRow: Int,
    onScreensPerRowChange: (Int) -> Unit,
    scale: Float,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onZoom100: () -> Unit,
    onFit: () -> Unit,
    showScreenLayoutGrid: Boolean,
    onShowScreenLayoutGridChange: (Boolean) -> Unit,
    layoutGridColumns: Int,
    onLayoutGridColumnsChange: (Int) -> Unit,
    layoutGridGutterDp: Int,
    onLayoutGridGutterDpChange: (Int) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val colors = LocalStudioColors.current
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        // Row 1 — identity + view controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Studio mark: a filled cobalt registration tick, the board's signature.
            StudioMark()
            Spacer(Modifier.width(9.dp))
            StudioText(
                text = title,
                style = Studio.type.title,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(10.dp))
            StudioText(
                text = buildString {
                    append(frameCount)
                    append(" frames")
                    if (selectedFrameLabel != null) {
                        append("  ·  ")
                        append(selectedFrameLabel)
                    }
                },
                style = Studio.type.mono,
                color = colors.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            ZoomCluster(
                scale = scale,
                onZoomOut = onZoomOut,
                onZoomIn = onZoomIn,
                onZoom100 = onZoom100,
                onFit = onFit,
            )
            Spacer(Modifier.width(6.dp))
            GridConfigMenu(
                visible = showScreenLayoutGrid,
                onVisibleChange = onShowScreenLayoutGridChange,
                columns = layoutGridColumns,
                onColumnsChange = onLayoutGridColumnsChange,
                gutterDp = layoutGridGutterDp,
                onGutterDpChange = onLayoutGridGutterDpChange,
            )
            Spacer(Modifier.width(6.dp))
            ThemeToggleButton(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
            )
        }

        StudioDivider()

        // Row 2 — kind · search · locale (groups live on the board, not as filters)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KindSegmented(
                selected = kindFilter,
                onSelect = onKindFilter,
            )

            ToolbarSearchField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.width(220.dp),
            )

            if (showLocaleControl) {
                LocaleDropdown(
                    locales = locales,
                    selectedTag = localeTag,
                    onSelect = onLocaleTag,
                )
            }

            DeviceDropdown(
                selected = deviceSize,
                onSelect = onDeviceSize,
            )

            ScreensPerRowMenu(
                screensPerRow = screensPerRow,
                onScreensPerRowChange = onScreensPerRowChange,
            )
        }

        StudioDivider(strong = true)
    }
}

// ── Shared chrome primitives ──────────────────────────────────────────────────

/** The board's mark: a filled L-registration tick in drafting-ink cobalt. */
@Composable
private fun StudioMark() {
    val accent = LocalStudioColors.current.accent
    Canvas(Modifier.width(14.dp).height(14.dp)) {
        val s = 2.2.dp.toPx()
        val len = size.minDimension
        drawLine(accent, Offset(0f, s / 2f), Offset(len * 0.72f, s / 2f), strokeWidth = s)
        drawLine(accent, Offset(s / 2f, 0f), Offset(s / 2f, len * 0.72f), strokeWidth = s)
        drawLine(accent, Offset(len, len - s / 2f), Offset(len * 0.35f, len - s / 2f), strokeWidth = s)
        drawLine(accent, Offset(len - s / 2f, len), Offset(len - s / 2f, len * 0.35f), strokeWidth = s)
    }
}

/** Hairline separator between toolbar rows / under the toolbar. */
@Composable
private fun StudioDivider(strong: Boolean = false) {
    val colors = LocalStudioColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (strong) colors.line else colors.line.copy(alpha = 0.6f)),
    )
}

/**
 * A quiet chrome text button: no fill at rest, a faint ink wash on hover, hand
 * cursor. The studio's replacement for Material `TextButton`.
 */
@Composable
private fun StudioButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
    enabled: Boolean = true,
) {
    val colors = LocalStudioColors.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (hovered && enabled) colors.ink.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = tween(120),
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .then(
                if (enabled) {
                    Modifier
                        .hoverable(interaction)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClick = onClick,
                        )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        StudioText(
            text = label,
            style = Studio.type.label,
            color = when {
                !enabled -> colors.inkFaint
                color != null -> color
                else -> colors.ink
            },
            maxLines = 1,
        )
    }
}

/**
 * A popover anchored under [anchor]. Foundation [Popup] replaces Material
 * `DropdownMenu`, which is unavailable without the Material dependency (and, in
 * its exposed form, unreliable on Wasm anyway).
 */
@Composable
private fun StudioMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    anchor: @Composable () -> Unit,
    menu: @Composable () -> Unit,
) {
    val colors = LocalStudioColors.current
    val density = LocalDensity.current
    var anchorHeight by remember { mutableIntStateOf(0) }
    Box(Modifier.onSizeChanged { anchorHeight = it.height }) {
        anchor()
        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, anchorHeight + with(density) { 4.dp.roundToPx() }),
                onDismissRequest = onDismiss,
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(10.dp), ambientColor = colors.shadow, spotColor = colors.shadow)
                        .background(colors.surfaceRaised, RoundedCornerShape(10.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(10.dp))
                        .widthIn(min = 176.dp)
                        .padding(vertical = 6.dp),
                ) {
                    menu()
                }
            }
        }
    }
}

/** One selectable row inside a [StudioMenu]. */
@Composable
private fun StudioMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalStudioColors.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .background(if (hovered) colors.ink.copy(alpha = 0.06f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

// ── Toolbar controls ──────────────────────────────────────────────────────────

@Composable
private fun ZoomCluster(
    scale: Float,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onZoom100: () -> Unit,
    onFit: () -> Unit,
) {
    val colors = LocalStudioColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.line, RoundedCornerShape(8.dp))
            .padding(horizontal = 2.dp),
    ) {
        StudioButton("−", onClick = onZoomOut)
        StudioText(
            text = "${(scale * 100).toInt()}%",
            style = Studio.type.mono,
            color = colors.inkSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 44.dp)
                .padding(horizontal = 4.dp),
            maxLines = 1,
        )
        StudioButton("+", onClick = onZoomIn)
        StudioButton("100%", onClick = onZoom100)
        StudioButton("Fit", onClick = onFit)
    }
}

/**
 * Theme switch drawn as a half-shaded disc (no glyph dependency): the unfilled
 * half hints at the surface you would switch to.
 */
@Composable
private fun ThemeToggleButton(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
) {
    val colors = LocalStudioColors.current
    val description = if (darkTheme) "Switch to light theme" else "Switch to dark theme"
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg by animateColorAsState(
        targetValue = if (hovered) colors.ink.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = tween(120),
    )
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onClick = onToggleTheme,
            )
            .semantics {
                contentDescription = description
                stateDescription = if (darkTheme) "Dark" else "Light"
            }
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.width(15.dp).height(15.dp)) {
            val stroke = 1.25.dp.toPx()
            val radius = size.minDimension / 2f - stroke
            drawCircle(color = colors.inkSoft, radius = radius, style = Stroke(width = stroke))
            drawArc(
                color = colors.inkSoft,
                startAngle = if (darkTheme) 90f else -90f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(stroke, stroke),
                size = Size(radius * 2f, radius * 2f),
            )
        }
    }
}

@Composable
private fun KindSegmented(
    selected: KindFilter,
    onSelect: (KindFilter) -> Unit,
) {
    val colors = LocalStudioColors.current
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.line, RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        KindFilter.entries.forEach { filter ->
            val isOn = selected == filter
            val label = when (filter) {
                KindFilter.All -> "All"
                KindFilter.Screens -> "Screens"
                KindFilter.Components -> "Components"
            }
            val pillColor by animateColorAsState(
                targetValue = if (isOn) colors.accentWash else Color.Transparent,
                animationSpec = tween(140),
            )
            val labelColor by animateColorAsState(
                targetValue = if (isOn) colors.accentInk else colors.inkSoft,
                animationSpec = tween(140),
            )
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(pillColor)
                    .hoverable(interaction)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = isOn
                    }
                    .clickable(interactionSource = interaction, indication = null) { onSelect(filter) }
                    .padding(horizontal = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                StudioText(text = label, style = Studio.type.label, color = labelColor, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ToolbarSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalStudioColors.current
    val shape = RoundedCornerShape(8.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (focused) colors.accent else colors.line,
        animationSpec = tween(140),
    )
    val glassColor = colors.inkSoft.copy(alpha = 0.75f)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interaction,
        textStyle = Studio.type.label.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier
            .semantics { contentDescription = "Search preview frames" }
            .height(32.dp)
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Magnifier drawn inline — no glyph dependency.
                Canvas(Modifier.width(12.dp).height(12.dp)) {
                    val stroke = 1.25.dp.toPx()
                    val r = size.minDimension * 0.32f
                    val c = Offset(r + stroke, r + stroke)
                    drawCircle(color = glassColor, radius = r, center = c, style = Stroke(stroke))
                    drawLine(
                        color = glassColor,
                        start = Offset(c.x + r * 0.71f, c.y + r * 0.71f),
                        end = Offset(size.width - stroke, size.height - stroke),
                        strokeWidth = stroke,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        StudioText(
                            text = "Search frames…",
                            style = Studio.type.label,
                            color = colors.inkFaint,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            }
        },
    )
}

/**
 * Compact control for how many screen frames pack on each board row.
 */
@Composable
private fun ScreensPerRowMenu(
    screensPerRow: Int,
    onScreensPerRowChange: (Int) -> Unit,
) {
    val colors = LocalStudioColors.current
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    StudioMenu(
        expanded = expanded,
        onDismiss = { expanded = false },
        anchor = {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(shape)
                    .border(1.dp, colors.line, shape)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { expanded = true }
                    .padding(start = 10.dp, end = 8.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Screens per row"
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StudioText(
                    text = "Row",
                    style = Studio.type.label,
                    color = colors.ink,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                StudioText(
                    text = screensPerRow.toString(),
                    style = Studio.type.mono,
                    color = colors.accentInk,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
                StudioText(
                    text = if (expanded) "▴" else "▾",
                    style = Studio.type.symbol,
                    color = colors.inkSoft,
                )
            }
        },
        menu = {
            MenuSectionLabel("Screens per row")
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SCREENS_PER_ROW_OPTIONS.forEach { n ->
                    GridOptionChip(
                        label = n.toString(),
                        selected = screensPerRow == n,
                        onClick = {
                            onScreensPerRowChange(n)
                            expanded = false
                        },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        },
    )
}

/**
 * One chrome control with two hit targets (no visual split):
 * - **Grid** (left) toggles the screen layout overlay
 * - **▾** (right) opens column/gutter configuration only
 */
@Composable
private fun GridConfigMenu(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    columns: Int,
    onColumnsChange: (Int) -> Unit,
    gutterDp: Int,
    onGutterDpChange: (Int) -> Unit,
) {
    val colors = LocalStudioColors.current
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (visible) colors.accent.copy(alpha = 0.55f) else colors.line
    val labelColor = if (visible) colors.accentInk else colors.ink

    StudioMenu(
        expanded = expanded,
        onDismiss = { expanded = false },
        anchor = {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(shape)
                    .border(1.dp, borderColor, shape)
                    .background(if (visible) colors.accentWash.copy(alpha = 0.4f) else Color.Transparent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StudioText(
                    text = "Grid",
                    style = Studio.type.label,
                    color = labelColor,
                    maxLines = 1,
                    modifier = Modifier
                        .semantics {
                            role = Role.Switch
                            stateDescription = if (visible) "On" else "Off"
                        }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onVisibleChange(!visible) }
                        .padding(start = 10.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
                )
                StudioText(
                    text = if (expanded) "▴" else "▾",
                    style = Studio.type.symbol,
                    color = colors.inkSoft,
                    modifier = Modifier
                        .semantics {
                            role = Role.Button
                            contentDescription = "Configure layout grid"
                        }
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { expanded = true }
                        .padding(start = 2.dp, end = 9.dp, top = 7.dp, bottom = 7.dp),
                )
            }
        },
        menu = {
            MenuSectionLabel("Columns")
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GRID_COLUMN_OPTIONS.forEach { n ->
                    GridOptionChip(label = n.toString(), selected = columns == n, onClick = { onColumnsChange(n) })
                }
            }
            MenuSectionLabel("Gutter")
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GRID_GUTTER_OPTIONS_DP.forEach { g ->
                    GridOptionChip(label = "${g}dp", selected = gutterDp == g, onClick = { onGutterDpChange(g) })
                }
            }
            Spacer(Modifier.height(4.dp))
        },
    )
}

@Composable
private fun MenuSectionLabel(text: String) {
    StudioText(
        text = text,
        style = Studio.type.badge,
        color = LocalStudioColors.current.inkSoft,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
private fun GridOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalStudioColors.current
    val shape = RoundedCornerShape(6.dp)
    val bg = if (selected) colors.accentWash else colors.ink.copy(alpha = 0.05f)
    val fg = if (selected) colors.accentInk else colors.ink
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .pointerHoverIcon(PointerIcon.Hand)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        StudioText(text = label, style = Studio.type.mono, color = fg, maxLines = 1)
    }
}

private val GRID_COLUMN_OPTIONS = listOf(2, 3, 4, 6, 8, 12)
private val GRID_GUTTER_OPTIONS_DP = listOf(8, 12, 16, 24)
private val SCREENS_PER_ROW_OPTIONS = listOf(3, 4, 5, 6, 8)

/**
 * Locale control — options come from project resource languages only (see
 * [ArtboardLocale]).
 */
@Composable
private fun LocaleDropdown(
    locales: List<ArtboardLocale>,
    selectedTag: String?,
    onSelect: (String?) -> Unit,
) {
    val colors = LocalStudioColors.current
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = locales.find { it.tag == selectedTag }?.label
        ?: locales.firstOrNull { it.tag == null }?.label
        ?: "System"

    StudioMenu(
        expanded = expanded,
        onDismiss = { expanded = false },
        anchor = {
            DropdownAnchorRow(
                label = selectedLabel,
                expanded = expanded,
                onClick = { expanded = true },
            )
        },
        menu = {
            locales.forEach { option ->
                val isOn = option.tag == selectedTag
                StudioMenuItem(onClick = { onSelect(option.tag); expanded = false }) {
                    StudioText(
                        text = option.label,
                        style = Studio.type.label,
                        color = if (isOn) colors.accentInk else colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (isOn) MenuTick()
                }
            }
        },
    )
}

/** Shared anchor row for the locale/device dropdowns: label + chevron in a shell. */
@Composable
private fun DropdownAnchorRow(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalStudioColors.current
    Row(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.line, RoundedCornerShape(8.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .semantics {
                role = Role.Button
                contentDescription = label
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StudioText(
            text = label,
            style = Studio.type.label,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        StudioText(
            text = if (expanded) "▴" else "▾",
            style = Studio.type.symbol,
            color = colors.inkSoft,
        )
    }
}

@Composable
private fun MenuTick() {
    StudioText(text = "✓", style = Studio.type.label, color = LocalStudioColors.current.accent)
}

/** Parses a "WxH" device spec; blank or malformed → null (declared sizes). */
internal fun parseDeviceSpec(spec: String): ScreenDeviceSize? {
    val parts = spec.split('x')
    if (parts.size != 2) return null
    val w = parts[0].toIntOrNull() ?: return null
    val h = parts[1].toIntOrNull() ?: return null
    if (w !in 100..2000 || h !in 100..3000) return null
    return ScreenDeviceSize(w, h)
}

/**
 * Device viewport for Screen frames: named presets plus a custom W×H entry.
 * "Declared" restores each preview's own `widthDp`/`heightDp`.
 */
@Composable
private fun DeviceDropdown(
    selected: ScreenDeviceSize?,
    onSelect: (ScreenDeviceSize?) -> Unit,
) {
    val colors = LocalStudioColors.current
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = when {
        selected == null -> "Device · declared"
        else -> artboardDevicePresets.find { it.size == selected }?.label
            ?: "${selected.widthDp} × ${selected.heightDp}"
    }

    StudioMenu(
        expanded = expanded,
        onDismiss = { expanded = false },
        anchor = {
            DropdownAnchorRow(
                label = selectedLabel,
                expanded = expanded,
                onClick = { expanded = true },
            )
        },
        menu = {
            StudioMenuItem(onClick = { onSelect(null); expanded = false }) {
                StudioText(
                    text = "Declared sizes",
                    style = Studio.type.label,
                    color = if (selected == null) colors.accentInk else colors.ink,
                    modifier = Modifier.weight(1f),
                )
                if (selected == null) MenuTick()
            }
            artboardDevicePresets.forEach { device ->
                val isOn = device.size == selected
                StudioMenuItem(onClick = { onSelect(device.size); expanded = false }) {
                    StudioText(
                        text = device.label,
                        style = Studio.type.label,
                        color = if (isOn) colors.accentInk else colors.ink,
                    )
                    StudioText(
                        text = "${device.size.widthDp}×${device.size.heightDp}",
                        style = Studio.type.mono,
                        color = colors.inkSoft,
                        modifier = Modifier.weight(1f),
                    )
                    if (isOn) MenuTick()
                }
            }
            Box(Modifier.padding(vertical = 4.dp)) { StudioDivider() }
            CustomDeviceRow(
                initial = selected,
                onApply = { onSelect(it); expanded = false },
            )
        },
    )
}

/** Manual W×H entry inside the device menu. */
@Composable
private fun CustomDeviceRow(
    initial: ScreenDeviceSize?,
    onApply: (ScreenDeviceSize) -> Unit,
) {
    val colors = LocalStudioColors.current
    var widthText by remember(initial) { mutableStateOf(initial?.widthDp?.toString() ?: "") }
    var heightText by remember(initial) { mutableStateOf(initial?.heightDp?.toString() ?: "") }
    val parsed = parseDeviceSpec("${widthText.trim()}x${heightText.trim()}")

    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DimensionField(value = widthText, onValueChange = { widthText = it }, placeholder = "W")
        StudioText("×", style = Studio.type.label, color = colors.inkSoft)
        DimensionField(value = heightText, onValueChange = { heightText = it }, placeholder = "H")
        StudioButton(
            label = "Apply",
            onClick = { parsed?.let(onApply) },
            enabled = parsed != null,
            color = colors.accentInk,
        )
    }
}

@Composable
private fun DimensionField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    val colors = LocalStudioColors.current
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .width(58.dp)
            .height(30.dp)
            .clip(shape)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            StudioText(placeholder, style = Studio.type.mono, color = colors.inkFaint)
        }
        BasicTextField(
            value = value,
            onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(4)) },
            singleLine = true,
            textStyle = Studio.type.mono.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.accent),
        )
    }
}
