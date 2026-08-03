package artboard.viewer

import artboard.model.PreviewKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One preview described by a snapshot manifest. */
internal data class SnapshotFrame(
    val id: String,
    val name: String,
    val group: String?,
    val kind: PreviewKind,
    val widthDp: Int?,
    val heightDp: Int?,
    val sourceFqName: String?,
    /** Locale id to theme id to image path. Empty when every variant failed. */
    val images: Map<String, Map<String, String>>,
)

/** Why a frame variant has no image. */
internal data class SnapshotFailure(
    val frameId: String,
    val theme: String,
    val reason: String,
)

/** Parsed `manifest.json` produced by `artboardSnapshot`. */
internal data class SnapshotManifest(
    val title: String,
    /** Locale ids rendered, including the reserved "default". */
    val locales: List<String>,
    val frames: List<SnapshotFrame>,
    val failures: List<SnapshotFailure>,
) {
    /** First failure reason recorded for [frameId], if any. */
    fun failureFor(frameId: String): String? =
        failures.firstOrNull { it.frameId == frameId }?.reason
}

/**
 * Parses a snapshot manifest.
 *
 * Uses the JSON tree API rather than `@Serializable` so the viewer needs the
 * serialization runtime but not its compiler plugin.
 */
internal fun parseSnapshotManifest(json: String): SnapshotManifest {
    val root = Json.parseToJsonElement(json).jsonObject

    val schemaVersion = root.int("schemaVersion") ?: 0
    require(schemaVersion == SUPPORTED_SCHEMA_VERSION) {
        "Unsupported Artboard manifest schemaVersion $schemaVersion " +
            "(this viewer reads $SUPPORTED_SCHEMA_VERSION). Re-run artboardSnapshot " +
            "with a matching Artboard version."
    }

    val frames = root["frames"]?.jsonArray.orEmpty().map { element ->
        val frame = element.jsonObject
        SnapshotFrame(
            id = frame.string("id").orEmpty(),
            name = frame.string("name").orEmpty(),
            group = frame.string("group"),
            kind = if (frame.string("kind") == PreviewKind.Screen.name) {
                PreviewKind.Screen
            } else {
                PreviewKind.Component
            },
            widthDp = frame.int("widthDp"),
            heightDp = frame.int("heightDp"),
            sourceFqName = frame.string("sourceFqName"),
            images = frame["images"]?.jsonObject.orEmpty().mapValues { (_, byTheme) ->
                byTheme.jsonObject.mapValues { (_, path) -> path.jsonPrimitive.content }
            },
        )
    }

    val failures = root["failed"]?.jsonArray.orEmpty().map { element ->
        val failure = element.jsonObject
        SnapshotFailure(
            frameId = failure.string("id").orEmpty(),
            theme = failure.string("theme").orEmpty(),
            reason = failure.string("reason").orEmpty(),
        )
    }

    return SnapshotManifest(
        title = root.string("title") ?: "Artboard",
        locales = root["locales"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content },
        frames = frames,
        failures = failures,
    )
}

private const val SUPPORTED_SCHEMA_VERSION = 2

private fun JsonObject.string(key: String): String? =
    this[key]?.jsonPrimitive?.takeIf { it.isString }?.content

private fun JsonObject.int(key: String): Int? =
    this[key]?.jsonPrimitive?.content?.toIntOrNull()

private fun kotlinx.serialization.json.JsonArray?.orEmpty(): List<kotlinx.serialization.json.JsonElement> =
    this ?: emptyList()

private fun JsonObject?.orEmpty(): Map<String, kotlinx.serialization.json.JsonElement> =
    this ?: emptyMap()
