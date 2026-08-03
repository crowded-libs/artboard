package artboard.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.Locale

internal actual fun currentSystemLocaleTag(): String = Locale.getDefault().toLanguageTag()

@Composable
internal actual fun PlatformLocaleOverride(
    localeTag: String?,
    content: @Composable () -> Unit,
) {
    val ready = remember(localeTag) { mutableStateOf(false) }
    DisposableEffect(localeTag) {
        val previous = Locale.getDefault()
        val replacement = localeTag
            ?.replace('_', '-')
            ?.takeIf(String::isNotBlank)
            ?.let(Locale::forLanguageTag)
            ?: previous
        Locale.setDefault(replacement)
        ready.value = true
        onDispose {
            Locale.setDefault(previous)
            ready.value = false
        }
    }
    if (ready.value) content()
}
