package artboard.snapshot

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import artboard.host.PreviewFrameEnvironment
import artboard.registry.ArtboardRegistry
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.math.roundToInt

/** Inputs for one snapshot run. */
data class SnapshotOptions(
    val outputDirectory: File,
    val title: String = "Artboard",
    val themes: List<SnapshotTheme> = SnapshotTheme.entries.toList(),
    /** Locale axis; defaults to the platform locale only. */
    val locales: List<SnapshotLocale> = listOf(SnapshotLocale.Default),
    /** Device pixel ratio baked into the images; also the scene [Density]. */
    val scale: Float = DEFAULT_SCALE,
) {
    companion object {
        const val DEFAULT_SCALE: Float = 2f
    }
}

/** Outcome of a snapshot run. */
data class SnapshotResult(
    val frameCount: Int,
    val imageCount: Int,
    val failures: List<SnapshotFailure>,
    val manifestFile: File,
)

/**
 * Renders every frame in an [ArtboardRegistry] to PNG files plus a manifest.
 *
 * This is the "light mode" path: it produces the images that Artboard's prebuilt
 * viewer browses, so a consumer can get a gallery without declaring a `wasmJs`
 * target of their own.
 *
 * Sizing and file naming come from [snapshotRenderSpecs] so this output is
 * interchangeable with the Android renderer's. Rendering is deliberately
 * single-threaded: [ImageComposeScene] does not support concurrent use.
 */
object ArtboardSnapshotRenderer {

    /** Renders [registry] into [SnapshotOptions.outputDirectory]. */
    fun render(registry: ArtboardRegistry, options: SnapshotOptions): SnapshotResult {
        // Scoped to the directory this renderer owns, so a re-run cannot leave
        // images behind for previews that were deleted or renamed.
        val imagesDirectory = File(options.outputDirectory, SNAPSHOT_IMAGES_DIRECTORY)
        imagesDirectory.deleteRecursively()
        imagesDirectory.mkdirs()

        val failures = mutableListOf<SnapshotFailure>()
        val entries = mutableListOf<SnapshotManifestFrame>()
        var imageCount = 0

        for (spec in snapshotRenderSpecs(registry.frames)) {
            val images = linkedMapOf<String, Map<String, String>>()

            for (locale in options.locales) {
                val themed = linkedMapOf<String, String>()
                for (theme in options.themes) {
                    val relativePath = snapshotImagePath(spec.slug, locale.id, theme.id)
                    runCatching {
                        renderFrame(spec = spec, theme = theme, locale = locale, options = options)
                    }.fold(
                        onSuccess = { bytes ->
                            File(options.outputDirectory, relativePath).writeBytes(bytes)
                            themed[theme.id] = relativePath
                            imageCount++
                        },
                        onFailure = { error ->
                            failures += SnapshotFailure(
                                frameId = spec.frame.id,
                                locale = locale.id,
                                theme = theme.id,
                                reason = error.describe(),
                            )
                        },
                    )
                }
                if (themed.isNotEmpty()) images[locale.id] = themed
            }

            entries += SnapshotManifestFrame(
                frame = spec.frame,
                widthDp = spec.size.widthDp,
                heightDp = spec.size.heightDp,
                images = images,
            )
        }

        val manifestFile = File(options.outputDirectory, SNAPSHOT_MANIFEST_FILE_NAME)
        manifestFile.writeText(
            buildSnapshotManifest(
                title = options.title,
                scale = options.scale,
                locales = options.locales,
                frames = entries,
                failures = failures,
            ),
        )

        return SnapshotResult(
            frameCount = registry.frames.size,
            imageCount = imageCount,
            failures = failures,
            manifestFile = manifestFile,
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun renderFrame(
        spec: SnapshotRenderSpec,
        theme: SnapshotTheme,
        locale: SnapshotLocale,
        options: SnapshotOptions,
    ): ByteArray {
        val pixelWidth = (spec.size.widthDp * options.scale).roundToInt().coerceAtLeast(1)
        val pixelHeight = (spec.size.heightDp * options.scale).roundToInt().coerceAtLeast(1)

        // Wraps the whole scene lifetime, not just composition: resource loads are
        // async and settle over several frames, so the locale has to still be in
        // effect when they resolve.
        return withSnapshotLocale(locale.tag) {
            // The scene is created empty and the content set afterwards so that a
            // preview which throws during composition still releases the Skia surface.
            val scene = ImageComposeScene(
                width = pixelWidth,
                height = pixelHeight,
                density = Density(options.scale),
            )
            try {
                scene.setContent {
                    PreviewFrameEnvironment(isDark = theme.isDark, localeTag = locale.tag) {
                        spec.frame.content()
                    }
                }
                // Async work (resource loads, LaunchedEffect) is not complete after the
                // first frame. Settle before capturing, but never spin forever on a
                // preview that animates continuously.
                var settled = 0
                while (scene.hasInvalidations() && settled < SETTLE_LIMIT) {
                    scene.render()
                    settled++
                }
                val image = scene.render()
                image.encodeToData(EncodedImageFormat.PNG)?.bytes
                    ?: error("Skia returned no PNG data for ${spec.frame.id}")
            } finally {
                scene.close()
            }
        }
    }

    private fun Throwable.describe(): String {
        val text = message?.takeIf(String::isNotBlank) ?: this::class.java.simpleName
        return "${this::class.java.simpleName}: $text".take(MAX_REASON_LENGTH)
    }

    private const val SETTLE_LIMIT = 64
    private const val MAX_REASON_LENGTH = 400
}
