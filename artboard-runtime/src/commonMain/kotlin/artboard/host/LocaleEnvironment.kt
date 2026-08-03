package artboard.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalInspectionMode
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

/**
 * Publishes the gallery's light/dark choice the way the platform expresses it.
 *
 * Skiko targets carry `LocalSystemTheme`, which does not exist on Android — there the
 * signal is `Configuration.uiMode`. Both routes end at the consumer's own
 * `isSystemInDarkTheme()`, so preview bodies stay Artboard-free either way.
 */
@Composable
internal expect fun PlatformSystemTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
)

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

/**
 * Environment for one preview frame body (not gallery chrome).
 *
 * Matches IDE `@Preview` host semantics:
 * - [LocalSystemTheme] for gallery light/dark
 * - [LocalInspectionMode] = true so consumer preview branches (sample images,
 *   placeholders, skipped side effects) behave as they do in the IDE
 * - Locale layout direction via [PreviewContentLocale]
 */
@Composable
fun PreviewFrameEnvironment(
    isDark: Boolean,
    localeTag: String?,
    content: @Composable () -> Unit,
) {
    PlatformSystemTheme(isDark = isDark) {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            PreviewContentLocale(localeTag = localeTag, content = content)
        }
    }
}

/**
 * Locale requested for the current preview body, or null when following the platform.
 *
 * Published for the same reason `LocalSystemTheme` is: a frame body may need to know
 * which variant it is being rendered as. Artboard's snapshot viewer uses it to pick
 * the matching pre-rendered image.
 */
val LocalArtboardPreviewLocale: ProvidableCompositionLocal<String?> =
    compositionLocalOf { null }

/** Applies resource direction around one preview body while keeping the shell LTR. */
@Composable
fun PreviewContentLocale(
    localeTag: String?,
    content: @Composable () -> Unit,
) {
    val effectiveTag = localeTag ?: currentSystemLocaleTag()
    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirectionForLocale(effectiveTag),
        LocalArtboardPreviewLocale provides localeTag,
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
