@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package artboard.capture

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlin.io.encoding.Base64
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal actual val previewImageDownloadsSupported: Boolean = true

internal actual suspend fun downloadPreviewImage(
    image: ImageBitmap,
    fileName: String,
    opaque: Boolean,
) {
    val bitmap = image.asSkiaBitmap()
    if (opaque) {
        check(bitmap.setAlphaType(ColorAlphaType.OPAQUE)) {
            "Unable to mark the preview capture as opaque"
        }
    }

    val skiaImage = Image.makeFromBitmap(bitmap)
    val encoded = try {
        val data = checkNotNull(skiaImage.encodeToData(EncodedImageFormat.PNG)) {
            "Unable to encode the preview capture as PNG"
        }
        try {
            data.bytes
        } finally {
            data.close()
        }
    } finally {
        skiaImage.close()
    }

    triggerBrowserDownload(
        fileName = fileName,
        base64Png = Base64.Default.encode(encoded),
    )
}

@Suppress("UNUSED_PARAMETER")
private fun triggerBrowserDownload(fileName: String, base64Png: String): Unit =
    js(
        """{
            const link = document.createElement('a');
            link.href = 'data:image/png;base64,' + base64Png;
            link.download = fileName;
            link.style.display = 'none';
            document.body.appendChild(link);
            link.click();
            link.remove();
        }""",
    )
