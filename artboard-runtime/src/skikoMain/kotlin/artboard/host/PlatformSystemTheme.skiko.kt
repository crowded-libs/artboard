package artboard.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import org.jetbrains.skiko.SystemTheme as SkikoSystemTheme

/*
 * `LocalSystemTheme` holds `androidx.compose.ui.SystemTheme` through Compose 1.11 and
 * `org.jetbrains.skiko.SystemTheme` from Compose 1.12. The consumer's Compose version
 * decides which one is linked, so the local is read and provided untyped and the
 * value matches whichever enum the current default carries. Providing the wrong one
 * is an illegal cast on Wasm and a silent light theme on the JVM.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(InternalComposeUiApi::class)
@Composable
internal actual fun PlatformSystemTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val local = LocalSystemTheme as ProvidableCompositionLocal<Any?>
    val theme: Any = if (local.current is SystemTheme) {
        if (isDark) SystemTheme.Dark else SystemTheme.Light
    } else {
        if (isDark) SkikoSystemTheme.DARK else SkikoSystemTheme.LIGHT
    }
    CompositionLocalProvider(local provides theme, content = content)
}
