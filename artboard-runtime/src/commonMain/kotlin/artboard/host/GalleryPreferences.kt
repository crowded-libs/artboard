package artboard.host

import artboard.canvas.BoardCamera
import artboard.canvas.BoardLayoutDefaults

/**
 * Gallery chrome + camera state that should survive browser refresh.
 *
 * Stored as JSON via [loadGalleryPreferences] / [saveGalleryPreferences]
 * (Wasm `localStorage`; JVM is a no-op).
 */
data class GalleryPreferences(
    val showScreenLayoutGrid: Boolean = false,
    val layoutGridColumns: Int = DEFAULT_LAYOUT_GRID_COLUMNS,
    val layoutGridGutterDp: Int = DEFAULT_LAYOUT_GRID_GUTTER_DP,
    val screensPerRow: Int = BoardLayoutDefaults.SCREEN_DEFAULT_PER_ROW,
    val darkTheme: Boolean = false,
    val deviceSpec: String = "",
    val camera: BoardCamera? = null,
) {
    companion object {
        const val DEFAULT_LAYOUT_GRID_COLUMNS = 6
        const val DEFAULT_LAYOUT_GRID_GUTTER_DP = 8

        internal const val SCHEMA_VERSION = 1
        internal const val STORAGE_KEY_PREFIX = "artboard.gallery.v1."
    }
}

/** Stable localStorage namespace from a gallery title. */
internal fun galleryPreferencesNamespace(title: String): String {
    val slug = title
        .lowercase()
        .map { c -> if (c.isLetterOrDigit()) c else '-' }
        .joinToString("")
        .trim('-')
        .replace(Regex("-+"), "-")
        .take(64)
    return slug.ifBlank { "default" }
}

internal fun GalleryPreferences.toStorageJson(): String = buildString {
    append('{')
    append("\"v\":").append(GalleryPreferences.SCHEMA_VERSION)
    append(",\"showScreenLayoutGrid\":").append(showScreenLayoutGrid)
    append(",\"layoutGridColumns\":").append(layoutGridColumns)
    append(",\"layoutGridGutterDp\":").append(layoutGridGutterDp)
    append(",\"screensPerRow\":").append(screensPerRow)
    append(",\"darkTheme\":").append(darkTheme)
    append(",\"deviceSpec\":\"").append(deviceSpec.jsonEscape()).append('"')
    camera?.let { cam ->
        append(",\"camera\":{")
        append("\"offsetX\":").append(cam.offsetX.toStorageNumber())
        append(",\"offsetY\":").append(cam.offsetY.toStorageNumber())
        append(",\"scale\":").append(cam.scale.toStorageNumber())
        append('}')
    }
    append('}')
}

/**
 * Minimal JSON parser for our fixed schema. Avoids a multiplatform JSON dependency.
 * Returns null when the payload is missing, unreadable, or structurally invalid.
 */
internal fun parseGalleryPreferencesJson(raw: String): GalleryPreferences? {
    val text = raw.trim()
    if (text.isEmpty() || text[0] != '{') return null

    val showGrid = text.jsonBool("showScreenLayoutGrid") ?: return null
    val columns = text.jsonInt("layoutGridColumns") ?: return null
    val gutter = text.jsonInt("layoutGridGutterDp") ?: return null
    val perRow = text.jsonInt("screensPerRow") ?: return null
    val dark = text.jsonBool("darkTheme") ?: false
    val device = text.jsonString("deviceSpec") ?: ""
    val camera = parseCameraObject(text)

    return GalleryPreferences(
        showScreenLayoutGrid = showGrid,
        layoutGridColumns = columns.coerceIn(1, 24),
        layoutGridGutterDp = gutter.coerceIn(0, 64),
        screensPerRow = perRow.coerceIn(1, 16),
        darkTheme = dark,
        deviceSpec = device,
        camera = camera,
    )
}

private fun parseCameraObject(json: String): BoardCamera? {
    val camStart = json.indexOf("\"camera\"")
    if (camStart < 0) return null
    val brace = json.indexOf('{', camStart)
    if (brace < 0) return null
    val end = json.indexOf('}', brace)
    if (end < 0) return null
    val body = json.substring(brace, end + 1)
    val ox = body.jsonFloat("offsetX") ?: return null
    val oy = body.jsonFloat("offsetY") ?: return null
    val scale = body.jsonFloat("scale") ?: return null
    if (!ox.isFinite() || !oy.isFinite() || !scale.isFinite() || scale <= 0f) return null
    return BoardCamera(
        offsetX = ox,
        offsetY = oy,
        scale = scale.coerceIn(BoardCamera.MIN_SCALE, BoardCamera.MAX_SCALE),
    )
}

private fun String.jsonEscape(): String = buildString(length) {
    for (c in this@jsonEscape) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

private fun Float.toStorageNumber(): String =
    if (this % 1f == 0f && this in -1e7f..1e7f) toInt().toString() else toString()

private fun String.jsonBool(key: String): Boolean? {
    val token = jsonRawValue(key) ?: return null
    return when (token) {
        "true" -> true
        "false" -> false
        else -> null
    }
}

private fun String.jsonInt(key: String): Int? =
    jsonRawValue(key)?.toIntOrNull()

private fun String.jsonFloat(key: String): Float? =
    jsonRawValue(key)?.toFloatOrNull()

private fun String.jsonString(key: String): String? {
    val marker = "\"$key\""
    val keyIdx = indexOf(marker)
    if (keyIdx < 0) return null
    val colon = indexOf(':', keyIdx + marker.length)
    if (colon < 0) return null
    var i = colon + 1
    while (i < length && this[i].isWhitespace()) i++
    if (i >= length || this[i] != '"') return null
    i++
    val out = StringBuilder()
    while (i < length) {
        val c = this[i]
        when {
            c == '\\' && i + 1 < length -> {
                when (val n = this[i + 1]) {
                    '"', '\\', '/' -> out.append(n)
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    else -> out.append(n)
                }
                i += 2
            }
            c == '"' -> return out.toString()
            else -> {
                out.append(c)
                i++
            }
        }
    }
    return null
}

/** Raw token after `"key":` — number, true/false, or quoted string (without quotes). */
private fun String.jsonRawValue(key: String): String? {
    val marker = "\"$key\""
    val keyIdx = indexOf(marker)
    if (keyIdx < 0) return null
    val colon = indexOf(':', keyIdx + marker.length)
    if (colon < 0) return null
    var i = colon + 1
    while (i < length && this[i].isWhitespace()) i++
    if (i >= length) return null
    return when (this[i]) {
        '"' -> {
            // Use jsonString path for full unescaping; raw returns inner content.
            jsonString(key)
        }
        else -> {
            val start = i
            while (i < length && this[i] !in ",}") i++
            substring(start, i).trim()
        }
    }
}

internal expect fun loadGalleryPreferences(namespace: String): GalleryPreferences?

internal expect fun saveGalleryPreferences(namespace: String, prefs: GalleryPreferences)
