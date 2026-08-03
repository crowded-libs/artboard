package artboard.host

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Android has no `LocalSystemTheme`; dark mode is carried by `Configuration.uiMode`,
 * which is what `isSystemInDarkTheme()` reads on this platform.
 */
@Composable
internal actual fun PlatformSystemTheme(
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val current = LocalConfiguration.current
    val themed = remember(current, isDark) {
        Configuration(current).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
    }
    CompositionLocalProvider(LocalConfiguration provides themed, content = content)
}
