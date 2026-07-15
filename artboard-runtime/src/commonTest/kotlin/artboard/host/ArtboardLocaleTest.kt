package artboard.host

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
}
