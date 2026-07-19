package artboard.canvas

import artboard.model.PreviewFrame
import artboard.model.PreviewKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardLayoutTest {
    private val frames = listOf(
        frame("demo.MenuScreenPreview::Empty", "Empty", "Menu", PreviewKind.Screen, 390, 844),
        frame("demo.MenuCardPreview::Featured", "Featured", "Menu", PreviewKind.Component),
        frame("demo.CartScreenPreview::Filled", "Filled", "Cart", PreviewKind.Screen),
    )

    @Test
    fun layoutFiltersByKindGroupAndQuery() {
        val result = layoutBoard(
            frames = frames,
            query = "empty",
            kindFilter = KindFilter.Screens,
            selectedGroups = setOf("Menu"),
        )

        assertEquals(listOf("demo.MenuScreenPreview::Empty"), result.placed.map { it.frame.id })
        assertEquals(listOf("Screens"), result.zoneHeaders.map { it.title })
        assertTrue(!result.bounds.isEmpty)
    }

    @Test
    fun deviceOverrideChangesScreensButNotComponents() {
        val result = layoutBoard(frames, screenSize = ScreenDeviceSize(402, 874))
        val byId = result.placed.associateBy { it.frame.id }

        assertEquals(402f, byId.getValue("demo.MenuScreenPreview::Empty").width)
        assertEquals(874f, byId.getValue("demo.CartScreenPreview::Filled").height)
        assertEquals(
            BoardLayoutDefaults.COMPONENT_DEFAULT_W,
            byId.getValue("demo.MenuCardPreview::Featured").width,
        )
    }

    @Test
    fun emptyResultsHaveNoHeadersOrBounds() {
        val result = layoutBoard(frames, query = "missing")
        assertTrue(result.placed.isEmpty())
        assertTrue(result.zoneHeaders.isEmpty())
        assertEquals(androidx.compose.ui.geometry.Rect.Zero, result.bounds)
    }

    @Test
    fun screensPackAboutFivePerRowByDefault() {
        val many = (1..8).map { i ->
            frame(
                id = "demo.Screen::$i",
                name = "Screen $i",
                group = "App",
                kind = PreviewKind.Screen,
                width = BoardLayoutDefaults.SCREEN_DEFAULT_W.toInt(),
                height = BoardLayoutDefaults.SCREEN_DEFAULT_H.toInt(),
            )
        }
        val result = layoutBoard(many)
        val ys = result.placed.map { it.y }.distinct().sorted()
        // First row should hold five; remaining three wrap to a second row.
        assertEquals(2, ys.size)
        assertEquals(5, result.placed.count { it.y == ys[0] })
        assertEquals(3, result.placed.count { it.y == ys[1] })
    }

    @Test
    fun screensPerRowIsCountBasedRegardlessOfFrameWidth() {
        val many = (1..6).map { i ->
            frame(
                id = "demo.Phone::$i",
                name = "Phone $i",
                group = "App",
                kind = PreviewKind.Screen,
            )
        }
        val result = layoutBoard(many, screenSize = ScreenDeviceSize(402, 874))
        val firstRowY = result.placed.minOf { it.y }
        assertEquals(5, result.placed.count { it.y == firstRowY })
    }

    @Test
    fun screensPerRowThreePacksTightly() {
        val many = (1..7).map { i ->
            frame(
                id = "demo.Screen::$i",
                name = "Screen $i",
                group = "App",
                kind = PreviewKind.Screen,
            )
        }
        val result = layoutBoard(many, screensPerRow = 3)
        val ys = result.placed.map { it.y }.distinct().sorted()
        assertEquals(3, ys.size)
        assertEquals(3, result.placed.count { it.y == ys[0] })
        assertEquals(3, result.placed.count { it.y == ys[1] })
        assertEquals(1, result.placed.count { it.y == ys[2] })
    }

    @Test
    fun screensPerRowSixKeepsSixOnFirstRow() {
        val many = (1..8).map { i ->
            frame(
                id = "demo.Screen::$i",
                name = "Screen $i",
                group = "App",
                kind = PreviewKind.Screen,
            )
        }
        val result = layoutBoard(many, screensPerRow = 6)
        val firstRowY = result.placed.minOf { it.y }
        assertEquals(6, result.placed.count { it.y == firstRowY })
        assertEquals(2, result.placed.count { it.y != firstRowY })
    }

    private fun frame(
        id: String,
        name: String,
        group: String,
        kind: PreviewKind,
        width: Int? = null,
        height: Int? = null,
    ) = PreviewFrame(
        id = id,
        name = name,
        group = group,
        kind = kind,
        widthDp = width,
        heightDp = height,
        sourceFqName = id.substringBefore("::"),
        content = {},
    )
}
