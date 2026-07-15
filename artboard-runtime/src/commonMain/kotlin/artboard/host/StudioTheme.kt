package artboard.host

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import artboard.resources.FreeSans
import artboard.resources.IBMPlexMono_Regular
import artboard.resources.Res
import artboard.resources.SpaceGrotesk_Medium
import artboard.resources.SpaceGrotesk_Regular
import org.jetbrains.compose.resources.Font

/**
 * The Artboard host's own design system: **"Drafting Studio."**
 *
 * Artboard is an instrument — a light table you lay Compose previews out on — so
 * its chrome is built from a drafting studio's materials, not a UI kit's: warm
 * paper (light) or graphite (dark) surfaces, a single drafting-ink cobalt for
 * every live control, Space Grotesk for the instrument panel, IBM Plex Mono for
 * every readout, and print/registration marks as the selection language.
 *
 * The host depends on **no** Material library. Colours and type are addressed
 * through [LocalStudioColors] / [LocalStudioType] — the studio's own vocabulary —
 * exactly as a consumer would address their own design system. Frame bodies are
 * never themed by the host; they inherit only the light/dark signal (see
 * `FrameChrome`), so the gallery imposes nothing on the previews it hangs.
 */
@Immutable
data class StudioColors(
    /** The board itself — the deepest surface, the table you pin frames to. */
    val canvas: Color,
    /** Toolbar / instrument-panel surface. */
    val surface: Color,
    /** Raised popovers (menus) that float above [surface]. */
    val surfaceRaised: Color,
    /** Primary ink for text and glyphs. */
    val ink: Color,
    /** Secondary ink — readouts, metadata, captions. */
    val inkSoft: Color,
    /** Faint ink — placeholders, disabled text. */
    val inkFaint: Color,
    /** Hairline rules and idle control borders. */
    val line: Color,
    /** Stronger outline — screen-frame borders, emphasized rules. */
    val lineStrong: Color,
    /** Drafting-ink cobalt: the single accent, used for every live control. */
    val accent: Color,
    /** Accent as text/ink on [surface] (darkened in light for contrast). */
    val accentInk: Color,
    /** A wash of [accent] behind ink — active pills, selected chips. */
    val accentWash: Color,
    /** Ink drawn on [accent] fills. */
    val onAccent: Color,
    /** Graph-paper minor gridline. */
    val gridMinor: Color,
    /** Graph-paper major gridline (every 8th). */
    val gridMajor: Color,
    /** Idle frame border. */
    val frameBorder: Color,
    /** Frame border on hover. */
    val frameBorderHover: Color,
    /** Frame border + registration ticks on selection. */
    val frameBorderSelected: Color,
    /** Non-photo blue: the column/margin/gutter layout overlay. */
    val columnOverlay: Color,
    /** Ambient/spot shadow colour cast under frames. */
    val shadow: Color,
    /** Screen kind-badge fill / ink. */
    val screenBadge: Color,
    val onScreenBadge: Color,
    /** Component kind-badge fill / ink. */
    val componentBadge: Color,
    val onComponentBadge: Color,
    val isDark: Boolean,
)

@Immutable
data class StudioType(
    /** Space Grotesk Medium — the app title in the toolbar. */
    val title: TextStyle,
    /** Space Grotesk Medium — board zone titles (Screens / Components). */
    val zoneHeader: TextStyle,
    /** Space Grotesk Medium — group headers on the board. */
    val groupHeader: TextStyle,
    /** Space Grotesk Medium — frame names above each preview. */
    val frameName: TextStyle,
    /** Space Grotesk — control labels (buttons, chips, menu items). */
    val label: TextStyle,
    /** Space Grotesk Medium, tracked out — kind badges and eyebrows. */
    val badge: TextStyle,
    /** FreeSans — running body copy for empty states and messages. */
    val body: TextStyle,
    /** IBM Plex Mono — every numeric readout: zoom, counts, IDs, dimensions. */
    val mono: TextStyle,
    /** FreeSans — geometric symbol glyphs (chevrons) Space Grotesk lacks. */
    val symbol: TextStyle,
)

val LocalStudioColors = staticCompositionLocalOf<StudioColors> {
    error("StudioColors not provided — wrap chrome in StudioTheme { }")
}
val LocalStudioType = staticCompositionLocalOf<StudioType> {
    error("StudioType not provided — wrap chrome in StudioTheme { }")
}

/** Terse accessor, mirroring the shape consumers know from `MaterialTheme`. */
object Studio {
    val colors: StudioColors
        @Composable get() = LocalStudioColors.current
    val type: StudioType
        @Composable get() = LocalStudioType.current
}

// ── Palettes ──────────────────────────────────────────────────────────────────

/** Light: a drafting table — warm paper, drafting-ink cobalt, non-photo blue. */
private val LightColors = run {
    val ink = Color(0xFF262421)
    val accent = Color(0xFF2946C8)
    StudioColors(
        canvas = Color(0xFFE9E6E0),
        surface = Color(0xFFF6F5F1),
        surfaceRaised = Color(0xFFFCFBF7),
        ink = ink,
        inkSoft = Color(0xFF5C574E),
        inkFaint = Color(0xFF9A9488),
        line = Color(0xFFD6D2C9),
        lineStrong = Color(0xFFA9A399),
        accent = accent,
        accentInk = Color(0xFF16247A),
        accentWash = Color(0xFFDDE3FA),
        onAccent = Color(0xFFFFFFFF),
        gridMinor = ink.copy(alpha = 0.05f),
        gridMajor = ink.copy(alpha = 0.10f),
        frameBorder = Color(0xFFD6D2C9),
        frameBorderHover = accent.copy(alpha = 0.45f),
        frameBorderSelected = accent,
        // Non-photo blue: the pencil shade process cameras couldn't see — a
        // layout aid that reads as a guide, never as content.
        columnOverlay = Color(0xFF62A8DC).copy(alpha = 0.30f),
        shadow = Color(0xFF3A3226),
        screenBadge = Color(0xFFDDE3FA),
        onScreenBadge = Color(0xFF16247A),
        componentBadge = Color(0xFFE6E2D9),
        onComponentBadge = Color(0xFF423E36),
        isDark = false,
    )
}

/** Dark: graphite — deep warm charcoal, never pure black, chalk-warm ink. */
private val DarkColors = run {
    val paper = Color(0xFFE9E6DF)
    val accent = Color(0xFF93A8F7)
    StudioColors(
        canvas = Color(0xFF1B1A18),
        surface = Color(0xFF262422),
        surfaceRaised = Color(0xFF302D2A),
        ink = paper,
        inkSoft = Color(0xFFA39D92),
        inkFaint = Color(0xFF6E685E),
        line = Color(0xFF3A3733),
        lineStrong = Color(0xFF55514A),
        accent = accent,
        accentInk = accent,
        accentWash = Color(0xFF2E3C86),
        onAccent = Color(0xFF101B52),
        gridMinor = paper.copy(alpha = 0.045f),
        gridMajor = paper.copy(alpha = 0.09f),
        frameBorder = Color(0xFF3A3733),
        frameBorderHover = accent.copy(alpha = 0.45f),
        frameBorderSelected = accent,
        columnOverlay = Color(0xFF6FB3E8).copy(alpha = 0.32f),
        shadow = Color(0xFF000000),
        screenBadge = Color(0xFF2E3C86),
        onScreenBadge = Color(0xFFD8DFFC),
        componentBadge = Color(0xFF38342E),
        onComponentBadge = Color(0xFFD9D4C9),
        isDark = true,
    )
}

// ── Typography ────────────────────────────────────────────────────────────────
//
// Compose resource fonts resolve asynchronously, so the FontFamily must be
// rebuilt every composition (calling Font() again) or the first frame renders in
// the fallback face and never invalidates. StudioType is therefore constructed
// inside StudioTheme's composition, not held as a top-level constant.

@Composable
private fun studioType(): StudioType {
    val grotesk = FontFamily(
        Font(Res.font.SpaceGrotesk_Regular, weight = FontWeight.Normal),
        Font(Res.font.SpaceGrotesk_Medium, weight = FontWeight.Medium),
    )
    val mono = FontFamily(Font(Res.font.IBMPlexMono_Regular, weight = FontWeight.Normal))
    val sans = FontFamily(Font(Res.font.FreeSans, weight = FontWeight.Normal))
    return StudioType(
        title = TextStyle(
            fontFamily = grotesk, fontWeight = FontWeight.Medium,
            fontSize = 15.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
        ),
        zoneHeader = TextStyle(
            fontFamily = grotesk, fontWeight = FontWeight.Medium,
            fontSize = 18.sp, lineHeight = 22.sp, letterSpacing = 0.sp,
        ),
        groupHeader = TextStyle(
            fontFamily = grotesk, fontWeight = FontWeight.Medium,
            fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
        ),
        frameName = TextStyle(
            fontFamily = grotesk, fontWeight = FontWeight.Medium,
            fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp,
        ),
        label = TextStyle(
            fontFamily = grotesk, fontWeight = FontWeight.Medium,
            fontSize = 12.5.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp,
        ),
        badge = TextStyle(
            fontFamily = grotesk, fontWeight = FontWeight.Medium,
            fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.9.sp,
        ),
        body = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Normal,
            fontSize = 15.sp, lineHeight = 22.sp,
        ),
        mono = TextStyle(
            fontFamily = mono, fontWeight = FontWeight.Normal,
            fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 0.sp,
        ),
        symbol = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
        ),
    )
}

/**
 * Host theme for the gallery chrome. Provides [StudioColors] + [StudioType] to
 * the toolbar, board canvas, and frame chrome. Applies to host chrome only —
 * frame bodies are left to the consumer's own theme.
 */
@Composable
internal fun StudioTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStudioColors provides if (darkTheme) DarkColors else LightColors,
        LocalStudioType provides studioType(),
        content = content,
    )
}

/**
 * Foundation text helper for the chrome — the studio's `Text`. Folds [color] and
 * [textAlign] into [style] and draws with [BasicText] so no Material `Text` (and
 * thus no Material dependency) is pulled in.
 */
@Composable
internal fun StudioText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign ?: TextAlign.Unspecified),
        maxLines = maxLines,
        overflow = overflow,
    )
}
