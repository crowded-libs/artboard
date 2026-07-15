package artboard.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
private fun setCustomLocale(value: String?): Unit =
    js("{ globalThis.__customLocale = (value == null || value === '') ? null : value; }")

@OptIn(ExperimentalWasmJsInterop::class)
private fun getCustomLocale(): String? =
    js("globalThis.__customLocale == null ? null : String(globalThis.__customLocale)")

internal actual fun currentSystemLocaleTag(): String = Locale.current.toString()

@Composable
internal actual fun PlatformLocaleOverride(
    localeTag: String?,
    content: @Composable () -> Unit,
) {
    val normalized = localeTag?.replace('_', '-')?.takeIf(String::isNotBlank)
    val ready = remember(normalized) { mutableStateOf(false) }
    DisposableEffect(normalized) {
        val previous = getCustomLocale()
        setCustomLocale(normalized)
        ready.value = true
        onDispose {
            setCustomLocale(previous)
            ready.value = false
        }
    }
    if (ready.value) content()
}
