package artboard.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme

@OptIn(InternalComposeUiApi::class)
@Composable
internal actual fun PlatformSystemTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSystemTheme provides if (isDark) SystemTheme.Dark else SystemTheme.Light,
        content = content,
    )
}
