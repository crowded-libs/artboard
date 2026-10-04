@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package artboard.host

internal actual fun announceChromeTheme(darkTheme: Boolean) = postChromeTheme(if (darkTheme) "dark" else "light")

// Same-origin only: the shell and every mounted gallery share the dev server or export root.
@Suppress("UNUSED_PARAMETER")
private fun postChromeTheme(theme: String): Unit =
    js(
        """{
            if (window.parent !== window) {
                window.parent.postMessage({ artboardChromeTheme: theme }, window.location.origin);
            }
        }""",
    )
