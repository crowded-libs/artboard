@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package artboard.capture

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.io.encoding.Base64

internal actual val previewImageDownloadsSupported: Boolean = true

/*
 * Encodes with the browser's canvas rather than Skia's `Image.encodeToData`: the
 * consumer's Compose version picks the Skiko klib this links against, and
 * `encodeToData` changed its signature in Skiko 0.150 (Compose 1.12), which left
 * only a partial-linkage stub that throws at runtime. `ImageBitmap.readPixels`
 * is Compose's stable common API.
 */
internal actual suspend fun downloadPreviewImage(
    image: ImageBitmap,
    fileName: String,
    opaque: Boolean,
) {
    val argb = IntArray(image.width * image.height)
    image.readPixels(argb)
    val rgba = ByteArray(argb.size * 4)
    argb.forEachIndexed { pixel, color ->
        val offset = pixel * 4
        rgba[offset] = (color shr 16).toByte()
        rgba[offset + 1] = (color shr 8).toByte()
        rgba[offset + 2] = color.toByte()
        rgba[offset + 3] = if (opaque) 0xFF.toByte() else (color ushr 24).toByte()
    }

    triggerBrowserDownload(
        fileName = fileName,
        width = image.width,
        height = image.height,
        base64Rgba = Base64.Default.encode(rgba),
    )
}

@Suppress("UNUSED_PARAMETER")
private fun triggerBrowserDownload(
    fileName: String,
    width: Int,
    height: Int,
    base64Rgba: String,
): Unit =
    js(
        """{
            const binary = atob(base64Rgba);
            const pixels = new Uint8ClampedArray(binary.length);
            for (let i = 0; i < binary.length; i++) pixels[i] = binary.charCodeAt(i);
            const canvas = document.createElement('canvas');
            canvas.width = width;
            canvas.height = height;
            canvas.getContext('2d').putImageData(new ImageData(pixels, width, height), 0, 0);
            const link = document.createElement('a');
            link.href = canvas.toDataURL('image/png');
            link.download = fileName;
            link.style.display = 'none';
            document.body.appendChild(link);
            link.click();
            link.remove();
        }""",
    )
