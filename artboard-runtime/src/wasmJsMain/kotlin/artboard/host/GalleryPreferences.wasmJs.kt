package artboard.host

import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop

private var pendingNamespace: String? = null
private var pendingPrefs: GalleryPreferences? = null
private var unloadHookInstalled = false

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
    notePendingGalleryPreferences(namespace, prefs)
    writeToLocalStorage(namespace, prefs)
}

@OptIn(ExperimentalWasmJsInterop::class)
internal actual fun notePendingGalleryPreferences(namespace: String, prefs: GalleryPreferences) {
    pendingNamespace = namespace
    pendingPrefs = prefs
    ensureUnloadHook()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun ensureUnloadHook() {
    if (unloadHookInstalled) return
    unloadHookInstalled = true
    val listener: (Event) -> Unit = {
        val ns = pendingNamespace
        val prefs = pendingPrefs
        if (ns != null && prefs != null) {
            writeToLocalStorage(ns, prefs)
        }
    }
    // pagehide fires on hard reload / tab close; more reliable than Compose dispose alone.
    window.addEventListener("pagehide", listener)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun writeToLocalStorage(namespace: String, prefs: GalleryPreferences) {
    try {
        window.localStorage.setItem(storageKey(namespace), prefs.toStorageJson())
    } catch (_: Throwable) {
        // Quota / private mode — ignore; session still works without persistence.
    }
}

private fun storageKey(namespace: String): String =
    GalleryPreferences.STORAGE_KEY_PREFIX + namespace
