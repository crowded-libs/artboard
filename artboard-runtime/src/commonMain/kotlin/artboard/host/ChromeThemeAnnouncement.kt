package artboard.host

/**
 * Tells an embedding page which theme the gallery chrome shows, so a multi-module
 * shell can match it. A no-op outside the browser and when the gallery is not embedded.
 */
internal expect fun announceChromeTheme(darkTheme: Boolean)
