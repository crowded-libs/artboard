package artboard.capture

import androidx.compose.ui.graphics.ImageBitmap

internal actual val previewImageDownloadsSupported: Boolean = false

internal actual suspend fun downloadPreviewImage(
    image: ImageBitmap,
    fileName: String,
    opaque: Boolean,
) {
    error("Preview image downloads require the Wasm browser gallery")
}
