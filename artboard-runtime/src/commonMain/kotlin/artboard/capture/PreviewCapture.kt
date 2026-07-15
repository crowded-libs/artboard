package artboard.capture

import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import artboard.model.PreviewFrame
import artboard.model.PreviewKind
import kotlin.math.floor
import kotlin.math.sqrt

internal const val MAX_FALLBACK_CAPTURE_PIXELS: Long = 12_000_000L

internal data class PreviewCaptureSpec(
    val pixelSize: IntSize,
    val opaque: Boolean,
    val fileName: String,
)

internal enum class PreviewCaptureState {
    Idle,
    Capturing,
    Complete,
    Failed,
}

internal expect val previewImageDownloadsSupported: Boolean

internal expect suspend fun downloadPreviewImage(
    image: ImageBitmap,
    fileName: String,
    opaque: Boolean,
)

internal fun previewCaptureSpec(
    frame: PreviewFrame,
    logicalWidth: Int,
    logicalHeight: Int,
    darkTheme: Boolean,
    localeTag: String?,
): PreviewCaptureSpec {
    val pixelSize = resolveCapturePixelSize(
        kind = frame.kind,
        logicalWidth = logicalWidth,
        logicalHeight = logicalHeight,
    )
    return PreviewCaptureSpec(
        pixelSize = pixelSize,
        opaque = frame.kind == PreviewKind.Screen,
        fileName = previewCaptureFileName(
            frame = frame,
            darkTheme = darkTheme,
            localeTag = localeTag,
            pixelSize = pixelSize,
        ),
    )
}

internal fun resolveCapturePixelSize(
    kind: PreviewKind,
    logicalWidth: Int,
    logicalHeight: Int,
): IntSize {
    require(logicalWidth > 0 && logicalHeight > 0) {
        "Preview capture dimensions must be positive"
    }

    if (kind == PreviewKind.Screen) {
        nativeScreenPixelSize(logicalWidth, logicalHeight)?.let { return it }
    }

    return capCaptureSize(
        width = logicalWidth * 2L,
        height = logicalHeight * 2L,
    )
}

internal fun previewCaptureFileName(
    frame: PreviewFrame,
    darkTheme: Boolean,
    localeTag: String?,
    pixelSize: IntSize,
): String {
    val functionName = frame.sourceFqName
        ?.substringAfterLast('.')
        ?.takeIf(String::isNotBlank)
        ?: frame.id.substringBefore("::").substringAfterLast('.').ifBlank { "preview" }
    val segments = listOf(
        functionName,
        frame.name,
        if (darkTheme) "dark" else "light",
        localeTag ?: "system",
        "${pixelSize.width}x${pixelSize.height}",
    )
    return segments.joinToString("-") { it.fileNameSegment() }
        .take(180)
        .trim('-')
        .ifBlank { "artboard-preview" } + ".png"
}

internal suspend fun capturePreviewLayer(
    sourceLayer: GraphicsLayer,
    graphicsContext: GraphicsContext,
    density: Density,
    layoutDirection: LayoutDirection,
    spec: PreviewCaptureSpec,
): Result<Unit> = runCatching {
    val sourceSize = sourceLayer.size
    check(sourceSize.width > 0 && sourceSize.height > 0) {
        "Preview has not completed its first draw"
    }

    val outputLayer = graphicsContext.createGraphicsLayer()
    try {
        val scaleX = spec.pixelSize.width.toFloat() / sourceSize.width
        val scaleY = spec.pixelSize.height.toFloat() / sourceSize.height
        outputLayer.record(
            density = density,
            layoutDirection = layoutDirection,
            size = spec.pixelSize,
        ) {
            scale(scaleX = scaleX, scaleY = scaleY, pivot = Offset.Zero) {
                drawLayer(sourceLayer)
            }
        }
        downloadPreviewImage(
            image = outputLayer.toImageBitmap(),
            fileName = spec.fileName,
            opaque = spec.opaque,
        )
    } finally {
        graphicsContext.releaseGraphicsLayer(outputLayer)
    }
}

private fun nativeScreenPixelSize(width: Int, height: Int): IntSize? {
    nativePortraitScreenPixelSizes[width to height]?.let { return it }
    return nativePortraitScreenPixelSizes[height to width]?.let {
        IntSize(width = it.height, height = it.width)
    }
}

private val nativePortraitScreenPixelSizes = mapOf(
    (402 to 874) to IntSize(1206, 2622),
    (440 to 956) to IntSize(1320, 2868),
    (375 to 667) to IntSize(750, 1334),
    (412 to 923) to IntSize(1080, 2424),
    (384 to 832) to IntSize(1440, 3120),
    (834 to 1194) to IntSize(1668, 2388),
)

private fun capCaptureSize(width: Long, height: Long): IntSize {
    val pixels = width * height
    if (pixels <= MAX_FALLBACK_CAPTURE_PIXELS) {
        return IntSize(width.toInt(), height.toInt())
    }

    val factor = sqrt(MAX_FALLBACK_CAPTURE_PIXELS.toDouble() / pixels.toDouble())
    return IntSize(
        width = floor(width * factor).toInt().coerceAtLeast(1),
        height = floor(height * factor).toInt().coerceAtLeast(1),
    )
}

private fun String.fileNameSegment(): String = buildString {
    var pendingSeparator = false
    for (character in this@fileNameSegment.lowercase()) {
        if (character.isLetterOrDigit()) {
            if (pendingSeparator && isNotEmpty()) append('-')
            append(character)
            pendingSeparator = false
        } else {
            pendingSeparator = true
        }
    }
}.ifBlank { "preview" }
