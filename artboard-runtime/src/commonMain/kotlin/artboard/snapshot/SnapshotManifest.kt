package artboard.snapshot

import artboard.model.PreviewFrame
import kotlin.math.roundToInt

/** One frame plus the images rendered for it. */
data class SnapshotManifestFrame(
    val frame: PreviewFrame,
    val widthDp: Float,
    val heightDp: Float,
    /**
     * Locale id to theme id to image path, relative to the manifest.
     *
     * Empty inner maps are dropped; an empty outer map means every variant failed.
     */
    val images: Map<String, Map<String, String>>,
)

/**
 * Serializes the snapshot manifest.
 *
 * Hand-rolled to match the existing `artboard-previews.json` writer in
 * `artboard-codegen` and to keep a serialization dependency out of the runtime.
 * The prebuilt viewer is the only reader and parses it with kotlinx-serialization.
 *
 * Lives in `commonMain` so the JVM and Android renderers emit byte-identical
 * manifests.
 */
fun buildSnapshotManifest(
    title: String,
    scale: Float,
    locales: List<SnapshotLocale>,
    frames: List<SnapshotManifestFrame>,
    failures: List<SnapshotFailure>,
): String = buildString {
    appendLine("{")
    appendLine("  \"schemaVersion\": $SNAPSHOT_SCHEMA_VERSION,")
    appendLine("  \"title\": ${title.jsonString()},")
    appendLine("  \"scale\": $scale,")
    val localeIds = locales.joinToString(", ") { it.id.jsonString() }
    appendLine("  \"locales\": [$localeIds],")

    appendLine("  \"frames\": [")
    frames.forEachIndexed { index, entry ->
        val frame = entry.frame
        appendLine("    {")
        appendLine("      \"id\": ${frame.id.jsonString()},")
        appendLine("      \"name\": ${frame.name.jsonString()},")
        appendLine("      \"group\": ${frame.group.jsonStringOrNull()},")
        appendLine("      \"kind\": ${frame.kind.name.jsonString()},")
        appendLine("      \"widthDp\": ${entry.widthDp.roundToInt()},")
        appendLine("      \"heightDp\": ${entry.heightDp.roundToInt()},")
        appendLine("      \"sourceFqName\": ${frame.sourceFqName.jsonStringOrNull()},")
        appendLine("      \"images\": {")
        entry.images.entries.forEachIndexed { localeIndex, (locale, themes) ->
            val localeSeparator = if (localeIndex == entry.images.size - 1) "" else ","
            appendLine("        ${locale.jsonString()}: {")
            themes.entries.forEachIndexed { themeIndex, (theme, path) ->
                val separator = if (themeIndex == themes.size - 1) "" else ","
                appendLine("          ${theme.jsonString()}: ${path.jsonString()}$separator")
            }
            appendLine("        }$localeSeparator")
        }
        appendLine("      }")
        appendLine("    }${if (index == frames.size - 1) "" else ","}")
    }
    appendLine("  ],")

    appendLine("  \"failed\": [")
    failures.forEachIndexed { index, failure ->
        appendLine("    {")
        appendLine("      \"id\": ${failure.frameId.jsonString()},")
        appendLine("      \"locale\": ${failure.locale.jsonString()},")
        appendLine("      \"theme\": ${failure.theme.jsonString()},")
        appendLine("      \"reason\": ${failure.reason.jsonString()}")
        appendLine("    }${if (index == failures.size - 1) "" else ","}")
    }
    appendLine("  ]")
    appendLine("}")
}

const val SNAPSHOT_SCHEMA_VERSION: Int = 2

private fun String?.jsonStringOrNull(): String = this?.jsonString() ?: "null"

private fun String.jsonString(): String = buildString {
    append('"')
    for (character in this@jsonString) {
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else ->
                if (character < ' ') {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
        }
    }
    append('"')
}
