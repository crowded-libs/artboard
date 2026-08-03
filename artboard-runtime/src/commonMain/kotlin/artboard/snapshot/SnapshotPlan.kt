package artboard.snapshot

import artboard.canvas.BoardLayoutDefaults
import artboard.model.PreviewFrame
import artboard.model.PreviewKind

/** A light/dark variant rendered for every frame. */
enum class SnapshotTheme(val id: String, val isDark: Boolean) {
    Light(id = "light", isDark = false),
    Dark(id = "dark", isDark = true),
}

/** A frame variant that could not be rendered. Surfaced instead of silently dropped. */
data class SnapshotFailure(
    val frameId: String,
    val locale: String,
    val theme: String,
    val reason: String,
)

/** Resolved frame box in world dp. */
data class FrameSizeDp(val widthDp: Float, val heightDp: Float)

/** One frame's rendering instructions, shared by every snapshot renderer. */
data class SnapshotRenderSpec(
    val frame: PreviewFrame,
    val slug: String,
    val size: FrameSizeDp,
)

/**
 * Builds the render plan for a set of frames.
 *
 * Platform-agnostic on purpose: the JVM renderer (Skia, offscreen) and the Android
 * renderer (Robolectric, real Android views) must agree on sizes and file names so
 * their output is interchangeable in the viewer.
 */
fun snapshotRenderSpecs(frames: List<PreviewFrame>): List<SnapshotRenderSpec> {
    val used = mutableSetOf<String>()
    return frames.map { frame ->
        SnapshotRenderSpec(
            frame = frame,
            slug = uniqueSnapshotSlug(frame, used),
            size = resolveFrameSizeDp(frame),
        )
    }
}

/**
 * Mirrors `layoutBoard`'s per-frame sizing so snapshots match the live board.
 *
 * Width and height fall back independently, exactly as the board does — a preview
 * that declares only `widthDp` keeps the default height.
 */
fun resolveFrameSizeDp(frame: PreviewFrame): FrameSizeDp {
    val isScreen = frame.kind == PreviewKind.Screen
    val defaultWidth =
        if (isScreen) BoardLayoutDefaults.SCREEN_DEFAULT_W else BoardLayoutDefaults.COMPONENT_DEFAULT_W
    val defaultHeight =
        if (isScreen) BoardLayoutDefaults.SCREEN_DEFAULT_H else BoardLayoutDefaults.COMPONENT_DEFAULT_H
    return FrameSizeDp(
        widthDp = frame.widthDp?.toFloat()?.takeIf { it > 0f } ?: defaultWidth,
        heightDp = frame.heightDp?.toFloat()?.takeIf { it > 0f } ?: defaultHeight,
    )
}

/**
 * One locale a snapshot is rendered for.
 *
 * [id] is the manifest key and file-name segment; [tag] is what reaches
 * `PreviewFrameEnvironment`. The platform default renders with a null [tag] under the
 * reserved id [DEFAULT_ID], so a project with no localized resources still produces a
 * complete manifest.
 */
data class SnapshotLocale(val id: String, val tag: String?) {
    companion object {
        const val DEFAULT_ID: String = "default"

        /** The platform-default locale. */
        val Default: SnapshotLocale = SnapshotLocale(DEFAULT_ID, null)

        /** Builds the render axis from discovered resource language tags. */
        fun axisOf(languageTags: List<String>): List<SnapshotLocale> =
            listOf(Default) + languageTags
                .filter { it.isNotBlank() }
                .distinct()
                .map { SnapshotLocale(id = it, tag = it) }
    }
}

/** Path of one rendered variant, relative to the manifest. */
fun snapshotImagePath(slug: String, localeId: String, themeId: String): String =
    "$SNAPSHOT_IMAGES_DIRECTORY/$slug-$localeId-$themeId.png"

/**
 * Total images a run will write, for reporting before the work starts.
 *
 * Locales multiply an already-multiplied axis, so a project can go from tens to
 * thousands of files by adding translations; renderers log this and warn past
 * [SNAPSHOT_IMAGE_WARN_THRESHOLD].
 */
fun snapshotImageCount(frames: Int, locales: Int, themes: Int): Int =
    frames * locales * themes

const val SNAPSHOT_IMAGE_WARN_THRESHOLD: Int = 200

/**
 * Derives a file-safe name from the frame id.
 *
 * The id (FQCN plus preview name) is unique where the function name alone is not, so
 * two same-named previews in different packages cannot collide.
 */
fun uniqueSnapshotSlug(frame: PreviewFrame, used: MutableSet<String>): String {
    val base = frame.id.slugifySnapshotName().take(MAX_SLUG_LENGTH).ifBlank { "preview" }
    if (used.add(base)) return base
    var suffix = 2
    while (!used.add("$base-$suffix")) suffix++
    return "$base-$suffix"
}

internal fun String.slugifySnapshotName(): String = buildString {
    var pendingSeparator = false
    for (character in this@slugifySnapshotName.lowercase()) {
        if (character.isLetterOrDigit()) {
            if (pendingSeparator && isNotEmpty()) append('-')
            append(character)
            pendingSeparator = false
        } else {
            pendingSeparator = true
        }
    }
}

const val SNAPSHOT_IMAGES_DIRECTORY: String = "images"
const val SNAPSHOT_MANIFEST_FILE_NAME: String = "manifest.json"

private const val MAX_SLUG_LENGTH = 120
