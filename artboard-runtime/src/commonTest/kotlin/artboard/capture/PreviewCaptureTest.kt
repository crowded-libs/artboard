package artboard.capture

import androidx.compose.ui.unit.IntSize
import artboard.model.PreviewFrame
import artboard.model.PreviewKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreviewCaptureTest {
    @Test
    fun knownScreenSizesUseNativePixels() {
        assertEquals(
            IntSize(1206, 2622),
            resolveCapturePixelSize(PreviewKind.Screen, 402, 874),
        )
        assertEquals(
            IntSize(2622, 1206),
            resolveCapturePixelSize(PreviewKind.Screen, 874, 402),
        )
        assertEquals(
            IntSize(1440, 3120),
            resolveCapturePixelSize(PreviewKind.Screen, 384, 832),
        )
    }

    @Test
    fun unknownScreensAndComponentsUseTwoTimesLogicalSize() {
        assertEquals(
            IntSize(720, 1280),
            resolveCapturePixelSize(PreviewKind.Screen, 360, 640),
        )
        assertEquals(
            IntSize(640, 560),
            resolveCapturePixelSize(PreviewKind.Component, 320, 280),
        )
    }

    @Test
    fun largeFallbackCapturesAreCappedWithoutChangingAspectRatio() {
        val size = resolveCapturePixelSize(PreviewKind.Component, 2000, 3000)

        assertTrue(size.width.toLong() * size.height <= MAX_FALLBACK_CAPTURE_PIXELS)
        assertEquals(2.0 / 3.0, size.width.toDouble() / size.height, absoluteTolerance = 0.001)
    }

    @Test
    fun captureSpecUsesKindSpecificAlphaAndDeterministicName() {
        val screen = PreviewFrame(
            id = "example.HomeScreenPreview::Home Screen",
            name = "Home Screen",
            kind = PreviewKind.Screen,
            sourceFqName = "example.HomeScreenPreview",
            content = {},
        )
        val component = screen.copy(kind = PreviewKind.Component)

        val screenSpec = previewCaptureSpec(screen, 402, 874, darkTheme = true, localeTag = "en-US")
        val componentSpec = previewCaptureSpec(component, 320, 280, darkTheme = false, localeTag = null)

        assertTrue(screenSpec.opaque)
        assertEquals("homescreenpreview-home-screen-dark-en-us-1206x2622.png", screenSpec.fileName)
        assertFalse(componentSpec.opaque)
        assertEquals("homescreenpreview-home-screen-light-system-640x560.png", componentSpec.fileName)
    }
}
