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
