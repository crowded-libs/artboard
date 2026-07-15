package artboard.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale
import kotlin.js.ExperimentalWasmJsInterop

private val LocalAppLocale = staticCompositionLocalOf { Locale.current }

@OptIn(ExperimentalWasmJsInterop::class)
private fun setCustomLocale(value: String?): Unit =
    js("{ globalThis.__customLocale = (value == null || value === '') ? null : value; }")

internal actual fun currentSystemLocaleTag(): String = Locale.current.toString()

@Composable
internal actual fun PlatformLocaleOverride(
    localeTag: String?,
    content: @Composable () -> Unit,
) {
    val normalized = localeTag?.replace('_', '-')?.takeIf(String::isNotBlank)
    setCustomLocale(normalized)
    CompositionLocalProvider(LocalAppLocale provides Locale.current) {
        key(normalized) { content() }
    }
}
