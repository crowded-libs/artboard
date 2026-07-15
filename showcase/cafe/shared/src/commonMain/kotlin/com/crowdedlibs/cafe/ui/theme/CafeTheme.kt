package com.crowdedlibs.cafe.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.amiri_bold
import com.crowdedlibs.cafe.resources.amiri_regular
import com.crowdedlibs.cafe.resources.fraunces_600
import com.crowdedlibs.cafe.resources.fraunces_900
import com.crowdedlibs.cafe.resources.instrument_sans_400
import com.crowdedlibs.cafe.resources.instrument_sans_500
import com.crowdedlibs.cafe.resources.instrument_sans_600
import com.crowdedlibs.cafe.resources.space_mono_400
import com.crowdedlibs.cafe.resources.space_mono_700
import com.crowdedlibs.cafe.resources.tajawal_400
import com.crowdedlibs.cafe.resources.tajawal_500
import com.crowdedlibs.cafe.resources.tajawal_700
import org.jetbrains.compose.resources.Font as ResourceFont

/**
 * The Crowded Café design system: "Board & Ticket".
 *
 * The café has two visual artifacts — a hand-lettered chalkboard and the
 * thermal receipt the roaster staples to a bag of beans. The whole identity
 * is a translation of those two things: chalkboard pine-green, cream ticket
 * stock, a single roaster's-stamp orange, a characterful display serif for
 * the board and a monospace for every printed number.
 *
 * Colours and type are addressed through [LocalCafeColors] / [LocalCafeType],
 * the café's own vocabulary. The app depends on no Material library at all —
 * text, toggles, and fields are built from Compose foundation, so nothing here
 * can inherit a seeded default.
 */
@Immutable
data class CafeColors(
    /** The page — warm cream ticket stock. */
    val paper: Color,
    /** Raised ticket surfaces (cards, receipts) — a shade brighter than paper. */
    val ticket: Color,
    /** The chalkboard: deep pine green used for hero panels and the counter bar. */
    val board: Color,
    /** Chalk text drawn on [board]. */
    val chalk: Color,
    /** Primary ink for text on [paper]/[ticket]. */
    val ink: Color,
    /** Secondary ink — captions, metadata, descriptions. */
    val inkSoft: Color,
    /** The single hot accent: a roaster's-stamp red-orange. Use sparingly. */
    val stamp: Color,
    /** A wash of [stamp] for stamp fills that sit behind ink. */
    val stampWash: Color,
    /** Hairline rule / border colour on paper. */
    val line: Color,
    /** "Ready / done" affirmative — a steeped brew green. */
    val brew: Color,
    val isDark: Boolean,
)

@Immutable
data class CafeType(
    /** Fraunces 900 — the hand-lettered board headline. Used big, used once. */
    val board: TextStyle,
    /** Fraunces 600 — screen and section headings. */
    val heading: TextStyle,
    /** Fraunces 600 — item names on cards. */
    val serifItem: TextStyle,
    /** Instrument Sans — running body copy. */
    val body: TextStyle,
    /** Instrument Sans — smaller body / descriptions. */
    val bodySmall: TextStyle,
    /** Instrument Sans 600, tracked out — eyebrows and section labels. */
    val label: TextStyle,
    /** Space Mono — printed metadata (sizes, tags, timestamps). */
    val mono: TextStyle,
    /** Space Mono 700 — prices, totals, codes. The receipt's numbers. */
    val price: TextStyle,
    /** Space Mono 700, oversized — the pickup code hero. */
    val code: TextStyle,
)

val LocalCafeColors: ProvidableCompositionLocal<CafeColors> =
    staticCompositionLocalOf { error("CafeColors not provided; wrap in CafeTheme { }") }
val LocalCafeType: ProvidableCompositionLocal<CafeType> =
    staticCompositionLocalOf { error("CafeType not provided; wrap in CafeTheme { }") }

/** Reduced-motion switch, threaded as a design token (Compose has no CSS media query). */
val LocalReduceMotion: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/** Terse accessor object, mirroring `MaterialTheme`. */
object CafeTheme {
    val colors: CafeColors
        @Composable get() = LocalCafeColors.current
    val type: CafeType
        @Composable get() = LocalCafeType.current
    val reduceMotion: Boolean
        @Composable get() = LocalReduceMotion.current
}

// ── Palettes ────────────────────────────────────────────────────────────────

/** Daytime: a printed ticket on the counter, sun on cream paper. */
private val DayColors = CafeColors(
    paper = Color(0xFFF1E7D5),
    ticket = Color(0xFFFBF6EC),
    board = Color(0xFF13271F),
    chalk = Color(0xFFF2ECDD),
    ink = Color(0xFF23201B),
    inkSoft = Color(0xFF716855),
    stamp = Color(0xFFCE4B23),
    stampWash = Color(0xFFF0D8C8),
    line = Color(0xFFD8C9B0),
    brew = Color(0xFF2E6A4E),
    isDark = false,
)

/** After hours: the chalkboard glows, chalk goes warm, the stamp burns brighter. */
private val NightColors = CafeColors(
    paper = Color(0xFF10190F),
    ticket = Color(0xFF19241A),
    board = Color(0xFF0A120C),
    chalk = Color(0xFFF2ECDD),
    ink = Color(0xFFEDE3D0),
    inkSoft = Color(0xFF9A927F),
    stamp = Color(0xFFEE6B3F),
    stampWash = Color(0xFF3A2418),
    line = Color(0xFF2C3A2C),
    brew = Color(0xFF6FB58C),
    isDark = true,
)

// ── Typography ────────────────────────────────────────────────────────────────

@Composable
private fun frauncesFamily() = FontFamily(
    ResourceFont(Res.font.fraunces_600, FontWeight.SemiBold, FontStyle.Normal),
    ResourceFont(Res.font.fraunces_900, FontWeight.Black, FontStyle.Normal),
)

@Composable
private fun instrumentFamily() = FontFamily(
    ResourceFont(Res.font.instrument_sans_400, FontWeight.Normal, FontStyle.Normal),
    ResourceFont(Res.font.instrument_sans_500, FontWeight.Medium, FontStyle.Normal),
    ResourceFont(Res.font.instrument_sans_600, FontWeight.SemiBold, FontStyle.Normal),
)

@Composable
private fun spaceMonoFamily() = FontFamily(
    ResourceFont(Res.font.space_mono_400, FontWeight.Normal, FontStyle.Normal),
    ResourceFont(Res.font.space_mono_700, FontWeight.Bold, FontStyle.Normal),
)

/**
 * Arabic display face — Amiri, a classical Naskh with the same editorial,
 * high-contrast character as Fraunces. Registered at the display roles' weights
 * (SemiBold headings, Black board). Skiko does not fall back across faces by
 * script, so Arabic gets its own faces rather than a fallback chain.
 */
@Composable
private fun amiriFamily() = FontFamily(
    ResourceFont(Res.font.amiri_regular, FontWeight.SemiBold, FontStyle.Normal),
    ResourceFont(Res.font.amiri_bold, FontWeight.Black, FontStyle.Normal),
)

/** Arabic sans/mono face — Tajawal, a modern geometric Arabic+Latin sans. */
@Composable
private fun tajawalFamily() = FontFamily(
    ResourceFont(Res.font.tajawal_400, FontWeight.Normal, FontStyle.Normal),
    ResourceFont(Res.font.tajawal_500, FontWeight.Medium, FontStyle.Normal),
    ResourceFont(Res.font.tajawal_700, FontWeight.Bold, FontStyle.Normal),
)

private val tightHeights = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

@Composable
private fun cafeType(): CafeType {
    // Skiko has no per-script font fallback, so the whole type system swaps to
    // Arabic-capable faces when the frame runs RTL (the Artboard host provides
    // LayoutDirection.Rtl for `ar`; the product app inherits it from the device
    // locale). Amiri carries the display serif's editorial contrast; Tajawal
    // carries the sans/mono roles.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val display = if (rtl) amiriFamily() else frauncesFamily()
    val sans = if (rtl) tajawalFamily() else instrumentFamily()
    val mono = if (rtl) tajawalFamily() else spaceMonoFamily()
    // Arabic is cursive: per-glyph tracking breaks letterform joining, and the
    // Latin display's negative tracking has no meaning in Naskh — drop both in RTL.
    fun track(latin: Double) = (if (rtl) 0.0 else latin).sp
    // Naskh sits tall with low descenders; give the display roles more leading in RTL.
    fun lh(latin: Int, arabic: Int) = (if (rtl) arabic else latin).sp
    return CafeType(
        board = TextStyle(
            fontFamily = display, fontWeight = FontWeight.Black,
            fontSize = 40.sp, lineHeight = lh(42, 56), letterSpacing = track(-1.0),
            lineHeightStyle = tightHeights,
        ),
        heading = TextStyle(
            fontFamily = display, fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp, lineHeight = lh(30, 40), letterSpacing = track(-0.4),
        ),
        serifItem = TextStyle(
            fontFamily = display, fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp, lineHeight = lh(22, 30), letterSpacing = track(-0.2),
        ),
        body = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Normal,
            fontSize = 15.sp, lineHeight = lh(22, 26),
        ),
        bodySmall = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.Normal,
            fontSize = 13.sp, lineHeight = lh(18, 22),
        ),
        label = TextStyle(
            fontFamily = sans, fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp, lineHeight = lh(14, 18), letterSpacing = track(1.4),
        ),
        mono = TextStyle(
            fontFamily = mono, fontWeight = FontWeight.Normal,
            fontSize = 12.sp, lineHeight = lh(16, 20), letterSpacing = track(0.2),
        ),
        price = TextStyle(
            fontFamily = mono, fontWeight = FontWeight.Bold,
            fontSize = 15.sp, lineHeight = lh(18, 22),
        ),
        code = TextStyle(
            fontFamily = mono, fontWeight = FontWeight.Bold,
            fontSize = 44.sp, lineHeight = lh(46, 58), letterSpacing = track(4.0),
        ),
    )
}

/**
 * Root theme. Applied by the app entrypoints **and** by every `@Preview`, so the
 * Artboard gallery shows the real café — the host otherwise pins previews to
 * baseline Material3. Locale still comes from the host; only palette, type, and
 * motion are ours. Just provides the café [CompositionLocal]s — no Material.
 */
@Composable
fun CafeTheme(
    darkTheme: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCafeColors provides if (darkTheme) NightColors else DayColors,
        LocalCafeType provides cafeType(),
        LocalReduceMotion provides reduceMotion,
        content = content,
    )
}
