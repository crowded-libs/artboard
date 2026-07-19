package artboard.host

import artboard.canvas.BoardCamera
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
class GalleryPreferencesTest {
    @Test
    fun namespaceSlugifiesTitle() {
        assertEquals("crowded-café", galleryPreferencesNamespace("Crowded Café"))
        assertEquals("design-system", galleryPreferencesNamespace("Design System"))
        assertEquals("my-gallery", galleryPreferencesNamespace("My Gallery!!!"))
        assertEquals("default", galleryPreferencesNamespace("   "))
    }

    @Test
    fun roundTripWithoutCamera() {
        val prefs = GalleryPreferences(
            showScreenLayoutGrid = true,
            layoutGridColumns = 6,
            layoutGridGutterDp = 8,
            screensPerRow = 4,
            darkTheme = true,
            deviceSpec = "402x874",
            camera = null,
        )
        val parsed = parseGalleryPreferencesJson(prefs.toStorageJson())
        assertEquals(prefs, parsed)
    }

    @Test
    fun roundTripWithCamera() {
        val prefs = GalleryPreferences(
            showScreenLayoutGrid = false,
            layoutGridColumns = 12,
            layoutGridGutterDp = 16,
            screensPerRow = 6,
            darkTheme = false,
            deviceSpec = "",
            camera = BoardCamera(offsetX = 12.5f, offsetY = -40f, scale = 0.75f),
        )
        val parsed = parseGalleryPreferencesJson(prefs.toStorageJson())
        assertNotNull(parsed)
        assertEquals(prefs.showScreenLayoutGrid, parsed.showScreenLayoutGrid)
        assertEquals(prefs.layoutGridColumns, parsed.layoutGridColumns)
        assertEquals(prefs.layoutGridGutterDp, parsed.layoutGridGutterDp)
        assertEquals(prefs.screensPerRow, parsed.screensPerRow)
        assertNotNull(parsed.camera)
        assertEquals(12.5f, parsed.camera!!.offsetX)
        assertEquals(-40f, parsed.camera!!.offsetY)
        assertEquals(0.75f, parsed.camera!!.scale)
    }

    @Test
    fun escapesDeviceSpecQuotes() {
        val prefs = GalleryPreferences(deviceSpec = "a\"b")
        val parsed = parseGalleryPreferencesJson(prefs.toStorageJson())
        assertEquals("a\"b", parsed?.deviceSpec)
    }

    @Test
    fun invalidJsonReturnsNull() {
        assertNull(parseGalleryPreferencesJson(""))
        assertNull(parseGalleryPreferencesJson("{not json"))
        assertNull(parseGalleryPreferencesJson("{\"layoutGridColumns\":6}"))
    }

    @Test
    fun clampsOutOfRangeValues() {
        val json = """
            {"v":1,"showScreenLayoutGrid":true,"layoutGridColumns":99,
            "layoutGridGutterDp":-1,"screensPerRow":0,"darkTheme":false,"deviceSpec":""}
        """.trimIndent()
        val parsed = parseGalleryPreferencesJson(json)
        assertNotNull(parsed)
        assertEquals(24, parsed.layoutGridColumns)
        assertEquals(0, parsed.layoutGridGutterDp)
        assertEquals(1, parsed.screensPerRow)
    }
}
