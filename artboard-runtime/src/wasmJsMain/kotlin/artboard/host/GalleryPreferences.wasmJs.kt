package artboard.host

import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun loadGalleryPreferences(namespace: String): GalleryPreferences? {
    return try {
        val raw = window.localStorage.getItem(storageKey(namespace)) ?: return null
        parseGalleryPreferencesJson(raw)
    } catch (_: Throwable) {
        null
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun saveGalleryPreferences(namespace: String, prefs: GalleryPreferences) {
    try {
        window.localStorage.setItem(storageKey(namespace), prefs.toStorageJson())
    } catch (_: Throwable) {
        // Quota / private mode — ignore; session still works without persistence.
    }
}

private fun storageKey(namespace: String): String =
    GalleryPreferences.STORAGE_KEY_PREFIX + namespace
