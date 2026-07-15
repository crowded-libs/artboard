package artboard.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Applies and restores the platform resource-locale override around [content]. */
@Composable
internal expect fun PlatformLocaleOverride(
    localeTag: String?,
    content: @Composable () -> Unit,
)

/** Current platform language tag used when the gallery locale is System. */
internal expect fun currentSystemLocaleTag(): String

/** Board-wide resource locale override for Compose Multiplatform strings. */
@Composable
fun ArtboardResourceLocaleProvider(
    localeTag: String?,
    content: @Composable () -> Unit,
) {
    PlatformLocaleOverride(localeTag) {
        key(localeTag) { content() }
    }
}

/** Applies resource direction around one preview body while keeping the shell LTR. */
@Composable
fun PreviewContentLocale(
    localeTag: String?,
    content: @Composable () -> Unit,
) {
    val effectiveTag = localeTag ?: currentSystemLocaleTag()
    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirectionForLocale(effectiveTag),
    ) {
        key(localeTag) { content() }
    }
}

/** Returns the layout direction for a BCP-47 language tag. */
fun layoutDirectionForLocale(localeTag: String?): LayoutDirection {
    val language = localeTag
        ?.replace('_', '-')
        ?.substringBefore('-')
        ?.lowercase()
        .orEmpty()
    return if (language in RTL_LANGUAGES) LayoutDirection.Rtl else LayoutDirection.Ltr
}

private val RTL_LANGUAGES = setOf("ar", "he", "fa", "ur", "iw")
