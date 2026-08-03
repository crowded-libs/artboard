@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package artboard.viewer

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.io.encoding.Base64
import kotlin.js.Promise
import kotlinx.coroutines.await
import org.jetbrains.skia.Image as SkiaImage

/**
 * Decoded snapshot images, keyed by manifest-relative path.
 *
 * Loading is owned here rather than by each tile: the board disposes and re-composes
 * frame bodies as the camera moves, which cancels any in-flight per-tile effect and
 * leaves the tile stuck loading forever. Hoisting the work out of the composition
 * makes tiles pure functions of this state, and images appear progressively as they
 * arrive.
 */
internal object SnapshotImageStore {
    val images = mutableStateMapOf<String, ImageBitmap>()
    val errors = mutableStateMapOf<String, String>()

    /**
     * Fetches and decodes every image in [manifest], publishing each as it lands.
     *
     * Sequential on purpose — decoding is CPU-bound on Wasm's single thread, so
     * racing the fetches would only delay the first tile from appearing.
     */
    suspend fun loadAll(manifest: SnapshotManifest) {
        val paths = manifest.frames
            .flatMap { frame -> frame.images.values.flatMap { byTheme -> byTheme.values } }
            .distinct()

        for (path in paths) {
            if (images.containsKey(path) || errors.containsKey(path)) continue
            runCatching { decode(path) }
                .fold(
                    onSuccess = { images[path] = it },
                    onFailure = { errors[path] = it.message ?: "Failed to load $path" },
                )
        }
    }

    private suspend fun decode(path: String): ImageBitmap {
        val base64 = fetchBase64(path).await<JsString>().toString()
        val bytes = Base64.Default.decode(base64)
        return SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    }
}

/**
 * Fetches a binary asset as base64.
 *
 * Round-tripping through base64 keeps this off typed-array interop, matching the
 * approach already used for preview PNG downloads.
 */
@Suppress("UNUSED_PARAMETER")
private fun fetchBase64(url: String): Promise<JsString> =
    js(
        """
        fetch(url).then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status + ' while loading ' + url);
            }
            return response.arrayBuffer();
        }).then(function (buffer) {
            var bytes = new Uint8Array(buffer);
            var binary = '';
            var chunk = 0x8000;
            for (var i = 0; i < bytes.length; i += chunk) {
                binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
            }
            return btoa(binary);
        })
        """,
    )
