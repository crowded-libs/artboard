package artboard.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import artboard.host.LocalArtboardPreviewLocale

/**
 * Draws one pre-rendered preview.
 *
 * The image variant follows [LocalSystemTheme], which the board publishes to every
 * frame body — so the gallery's light/dark toggle switches snapshots with no extra
 * plumbing between the board and this tile.
 *
 * Frame chrome (selection marks, download, and the screen layout-grid overlay) is
 * owned by the board around this tile — snapshot images get the same Grid toggle
 * behavior as live previews.
 *
 * Stateless by design: images are owned by [SnapshotImageStore] so that camera
 * movement, which re-composes frame bodies, cannot interrupt loading.
 */
@OptIn(InternalComposeUiApi::class)
@Composable
internal fun SnapshotTile(frame: SnapshotFrame, failureReason: String?) {
    val themeId = if (LocalSystemTheme.current == SystemTheme.Dark) DARK else LIGHT
    // Both axes reach the tile through the board's own preview environment: theme via
    // LocalSystemTheme, locale via LocalArtboardPreviewLocale. Falling back keeps a
    // frame visible when only some variants rendered.
    val localeId = LocalArtboardPreviewLocale.current ?: SNAPSHOT_DEFAULT_LOCALE
    val byTheme = frame.images[localeId]
        ?: frame.images[SNAPSHOT_DEFAULT_LOCALE]
        ?: frame.images.values.firstOrNull()
    val path = byTheme?.get(themeId)
        ?: byTheme?.get(LIGHT)
        ?: byTheme?.values?.firstOrNull()

    val image = path?.let { SnapshotImageStore.images[it] }
    val error = path?.let { SnapshotImageStore.errors[it] }
        ?: failureReason.takeIf { path == null }
        ?: "No image was rendered for this preview.".takeIf { path == null }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            image != null ->
                Image(
                    bitmap = image,
                    contentDescription = frame.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                )

            error != null ->
                BasicText(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    style = TextStyle(
                        color = Color(0xFFB3261E),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    ),
                )

            // Still loading; the frame chrome already provides a surface.
            else -> Unit
        }
    }
}

private const val SNAPSHOT_DEFAULT_LOCALE = "default"
private const val LIGHT = "light"
private const val DARK = "dark"
