package artboard.host

internal actual fun loadGalleryPreferences(namespace: String): GalleryPreferences? = null

internal actual fun saveGalleryPreferences(namespace: String, prefs: GalleryPreferences) {
    // JVM hosts (unit tests) do not persist gallery chrome.
}
