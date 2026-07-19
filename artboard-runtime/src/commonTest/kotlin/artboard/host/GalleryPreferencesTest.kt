package artboard.host

import artboard.canvas.BoardCamera
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            layoutGridMarginDp = 24,
            layoutGridGutterDp = 8,
            screensPerRow = 4,
            darkTheme = true,
            deviceSpec = "402x874",
            camera = null,
            layoutBoundsWidthDp = 1200f,
            layoutBoundsHeightDp = 800f,
        )
        val parsed = parseGalleryPreferencesJson(prefs.toStorageJson())
        assertEquals(prefs, parsed)
    }

    @Test
    fun roundTripWithCamera() {
        val prefs = GalleryPreferences(
            showScreenLayoutGrid = false,
            layoutGridColumns = 12,
            layoutGridMarginDp = 16,
            layoutGridGutterDp = 16,
            screensPerRow = 6,
            darkTheme = false,
            deviceSpec = "",
            camera = BoardCamera(offsetX = 12.5f, offsetY = -40f, scale = 0.75f),
            layoutBoundsWidthDp = 2400f,
            layoutBoundsHeightDp = 1600f,
        )
        val parsed = parseGalleryPreferencesJson(prefs.toStorageJson())
        assertNotNull(parsed)
        assertEquals(prefs.showScreenLayoutGrid, parsed.showScreenLayoutGrid)
        assertEquals(prefs.layoutGridColumns, parsed.layoutGridColumns)
        assertEquals(prefs.layoutGridMarginDp, parsed.layoutGridMarginDp)
        assertEquals(prefs.layoutGridGutterDp, parsed.layoutGridGutterDp)
        assertEquals(prefs.screensPerRow, parsed.screensPerRow)
        assertNotNull(parsed.camera)
        assertEquals(12.5f, parsed.camera!!.offsetX)
        assertEquals(-40f, parsed.camera!!.offsetY)
        assertEquals(0.75f, parsed.camera!!.scale)
        assertEquals(2400f, parsed.layoutBoundsWidthDp)
        assertEquals(1600f, parsed.layoutBoundsHeightDp)
    }

    @Test
    fun legacyJsonWithoutMarginDefaultsToTwentyFour() {
        val json = """
            {"v":1,"showScreenLayoutGrid":true,"layoutGridColumns":6,
            "layoutGridGutterDp":8,"screensPerRow":4,"darkTheme":false,"deviceSpec":""}
        """.trimIndent()
        val parsed = parseGalleryPreferencesJson(json)
        assertNotNull(parsed)
        assertEquals(24, parsed.layoutGridMarginDp)
        assertNull(parsed.layoutBoundsWidthDp)
        assertNull(parsed.layoutBoundsHeightDp)
    }

    @Test
    fun rejectsUnknownSchemaVersion() {
        val json = """
            {"v":2,"showScreenLayoutGrid":true,"layoutGridColumns":6,
            "layoutGridGutterDp":8,"screensPerRow":4,"darkTheme":false,"deviceSpec":""}
        """.trimIndent()
        assertNull(parseGalleryPreferencesJson(json))
    }

    @Test
    fun acceptsMissingSchemaVersionAsV1() {
        val json = """
            {"showScreenLayoutGrid":false,"layoutGridColumns":6,
            "layoutGridGutterDp":8,"screensPerRow":5,"darkTheme":true,"deviceSpec":""}
        """.trimIndent()
        val parsed = parseGalleryPreferencesJson(json)
        assertNotNull(parsed)
        assertEquals(true, parsed.darkTheme)
        assertEquals(5, parsed.screensPerRow)
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
            "layoutGridMarginDp":200,"layoutGridGutterDp":-1,"screensPerRow":0,
            "darkTheme":false,"deviceSpec":""}
        """.trimIndent()
        val parsed = parseGalleryPreferencesJson(json)
        assertNotNull(parsed)
        assertEquals(24, parsed.layoutGridColumns)
        assertEquals(96, parsed.layoutGridMarginDp)
        assertEquals(0, parsed.layoutGridGutterDp)
        assertEquals(1, parsed.screensPerRow)
    }

    @Test
    fun layoutBoundsCompatibleRequiresSavedDims() {
        assertFalse(layoutBoundsCompatible(100f, 200f, savedWidthDp = null, savedHeightDp = null))
        assertFalse(layoutBoundsCompatible(100f, 200f, savedWidthDp = 100f, savedHeightDp = null))
        assertTrue(layoutBoundsCompatible(100f, 200f, savedWidthDp = 100f, savedHeightDp = 200f))
        assertTrue(layoutBoundsCompatible(100.5f, 200f, savedWidthDp = 100f, savedHeightDp = 200f))
        assertFalse(layoutBoundsCompatible(150f, 200f, savedWidthDp = 100f, savedHeightDp = 200f))
    }
}
