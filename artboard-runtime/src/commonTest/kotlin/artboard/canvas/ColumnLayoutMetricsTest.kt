package artboard.canvas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Proves [columnLayoutMetrics] matches Figma COLUMNS stretch grids
 * (Count / Gutter / Offset) so Artboard overlays are exact, not approximate.
 */
class ColumnLayoutMetricsTest {

    @Test
    fun figmaMixFitFrame375_6col_8gutter_24offset() {
        // Figma product frames: 375 wide, COLUMNS count=6, gutter=8, offset=24
        val m = columnLayoutMetrics(
            widthPx = 375f,
            columns = 6,
            marginPx = 24f,
            gutterPx = 8f,
        )
        assertNotNull(m)
        assertEquals(24f, m.outerLeftPx)
        assertEquals(351f, m.outerRightPx) // 375 - 24
        // usable = 375 - 48 = 327; gutters = 5*8 = 40; col = (327-40)/6
        assertEquals((327f - 40f) / 6f, m.columnWidthPx, absoluteTolerance = 0.0001f)
    }

    @Test
    fun artboardPreview402_sameMarginGutter_columnsFlex() {
        // MixFit @Preview widthDp=402 keeps margin 24; columns stretch.
        val m = columnLayoutMetrics(
            widthPx = 402f,
            columns = 6,
            marginPx = 24f,
            gutterPx = 8f,
        )
        assertNotNull(m)
        assertEquals(24f, m.outerLeftPx)
        assertEquals(378f, m.outerRightPx)
        // usable = 354; gutters = 40; col = (354-40)/6
        assertEquals((354f - 40f) / 6f, m.columnWidthPx, absoluteTolerance = 0.0001f)
    }

    @Test
    fun oldHardcoded16MarginMisalignsWithFigma24Content() {
        // What Artboard used to draw: margin 16 while product content sits at 24.
        val overlay = columnLayoutMetrics(402f, 6, marginPx = 16f, gutterPx = 8f)!!
        val contentLeft = 24f
        assertEquals(16f, overlay.outerLeftPx)
        // Content starts 8dp inside the first column — the “guides look wrong” bug.
        assertEquals(8f, contentLeft - overlay.outerLeftPx)
    }

    @Test
    fun tooNarrowReturnsNull() {
        assertNull(columnLayoutMetrics(widthPx = 20f, columns = 6, marginPx = 24f, gutterPx = 8f))
    }
}
