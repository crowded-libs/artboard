package artboard.host

import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class ArtboardLocaleTest {
    @Test
    fun localeListNormalizesDeduplicatesAndKeepsSystemFirst() {
        val locales = artboardLocalesFromResourceTags(listOf("es-MX", "AR", "es", ""))

        assertEquals(
            listOf(
                ArtboardLocale.System,
                ArtboardLocale("ar", "Arabic"),
                ArtboardLocale("es", "Spanish"),
            ),
            locales,
        )
    }

    @Test
    fun localeDirectionSupportsLocalizedPreviewLayouts() {
        assertEquals(LayoutDirection.Rtl, layoutDirectionForLocale("ar"))
        assertEquals(LayoutDirection.Rtl, layoutDirectionForLocale("AR-eg"))
        assertEquals(LayoutDirection.Ltr, layoutDirectionForLocale("es"))
        assertEquals(LayoutDirection.Ltr, layoutDirectionForLocale("en-US"))
    }
}
