@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlin.js.ExperimentalWasmJsInterop::class,
)

package artboard.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import artboard.host.ArtboardApp
import artboard.host.ArtboardLocale
import artboard.host.artboardLocalesFromResourceTags
import artboard.model.PreviewFrame
import artboard.registry.ArtboardRegistry
import artboard.registry.EmptyArtboardRegistry
import artboard.registry.resolveFrameId
import kotlin.js.Promise
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.dom.events.Event

/**
 * Artboard's prebuilt snapshot gallery.
 *
 * Loads `manifest.json` next to itself and turns each entry into a [PreviewFrame]
 * whose body draws the pre-rendered image. The board itself is unmodified — it
 * cannot tell a snapshot frame from a live one.
 */
fun main() {
    val body = document.body ?: error("document.body is null")
    ComposeViewport(viewportContainer = body) {
        var registry by remember { mutableStateOf<ArtboardRegistry>(EmptyArtboardRegistry) }
        var title by remember { mutableStateOf("Artboard") }
        var supportedLocales by remember { mutableStateOf(listOf(ArtboardLocale.System)) }
        var loadError by remember { mutableStateOf<String?>(null) }
        var selectedFrameId by remember { mutableStateOf(readHashParam(FRAME_PARAM)) }
        var deepLinkLocaleTag by remember { mutableStateOf(readHashParam(LOCALE_PARAM)) }
        var deepLinkLocaleRevision by remember {
            mutableStateOf(if (readHashParam(LOCALE_PARAM) != null || hasLocaleHashKey()) 1L else 0L)
        }

        LaunchedEffect(Unit) {
            runCatching { parseSnapshotManifest(fetchText(MANIFEST_PATH).await<JsString>().toString()) }
                .onSuccess { manifest ->
                    title = manifest.title
                    registry = manifest.toRegistry()
                    // "default" is the platform locale, which the board already
                    // offers as ArtboardLocale.System.
                    supportedLocales = artboardLocalesFromResourceTags(
                        manifest.locales.filter { it != "default" },
                    )
                    // Publishes each image as it arrives, so the board fills in
                    // progressively instead of waiting for the whole set.
                    SnapshotImageStore.loadAll(manifest)
                }
                .onFailure { failure ->
                    loadError = failure.message ?: "Could not load $MANIFEST_PATH"
                }
        }

        // Keep deep links working exactly as they do in the live gallery.
        LaunchedEffect(Unit) {
            val listener: (Event) -> Unit = {
                selectedFrameId = readHashParam(FRAME_PARAM)
                deepLinkLocaleTag = readHashParam(LOCALE_PARAM)
                deepLinkLocaleRevision++
            }
            window.addEventListener("hashchange", listener)
        }

        val error = loadError
        if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BasicText(
                    text = "Artboard could not start.\n$error",
                    modifier = Modifier.padding(24.dp),
                    style = TextStyle(color = Color(0xFFB3261E), fontSize = 14.sp),
                )
            }
            return@ComposeViewport
        }

        ArtboardApp(
            registry = registry,
            title = title,
            supportedLocales = supportedLocales,
            selectedFrameId = registry.resolveFrameId(selectedFrameId),
            onSelectedFrameIdChange = { frameId ->
                selectedFrameId = frameId
                writeHash(frameId = frameId, localeTag = deepLinkLocaleTag)
            },
            deepLinkLocaleRevision = deepLinkLocaleRevision,
            deepLinkLocaleTag = deepLinkLocaleTag,
            onLocaleTagChange = { tag ->
                deepLinkLocaleTag = tag
                writeHash(frameId = selectedFrameId, localeTag = tag)
            },
        )
    }
}

private fun SnapshotManifest.toRegistry(): ArtboardRegistry {
    val manifest = this
    val previewFrames = frames.map { frame ->
        PreviewFrame(
            id = frame.id,
            name = frame.name,
            group = frame.group,
            kind = frame.kind,
            widthDp = frame.widthDp,
            heightDp = frame.heightDp,
            sourceFqName = frame.sourceFqName,
            content = { SnapshotTile(frame = frame, failureReason = manifest.failureFor(frame.id)) },
        )
    }
    return object : ArtboardRegistry {
        override val frames: List<PreviewFrame> = previewFrames
    }
}

private fun readHashParam(key: String): String? {
    val hash = window.location.hash.removePrefix("#")
    if (hash.isBlank()) return null
    if (key == FRAME_PARAM && !hash.contains('=')) {
        return decodeUriComponent(hash).takeIf(String::isNotBlank)
    }
    return hash.split('&')
        .firstOrNull { it.startsWith("$key=") }
        ?.substringAfter('=')
        ?.let(::decodeUriComponent)
        ?.takeIf(String::isNotBlank)
}

private fun hasLocaleHashKey(): Boolean {
    val hash = window.location.hash.removePrefix("#")
    if (hash.isBlank()) return false
    return hash.split('&').any { it == LOCALE_PARAM || it.startsWith("$LOCALE_PARAM=") }
}

private fun writeHash(frameId: String?, localeTag: String?) {
    val parts = buildList {
        if (!frameId.isNullOrBlank()) add("$FRAME_PARAM=${encodeUriComponent(frameId)}")
        if (!localeTag.isNullOrBlank()) add("$LOCALE_PARAM=${encodeUriComponent(localeTag)}")
    }
    window.location.hash = if (parts.isEmpty()) "" else parts.joinToString("&")
}

@Suppress("UNUSED_PARAMETER")
private fun fetchText(url: String): Promise<JsString> =
    js(
        """
        fetch(url).then(function (response) {
            if (!response.ok) {
                throw new Error('HTTP ' + response.status + ' while loading ' + url);
            }
            return response.text();
        })
        """,
    )

@Suppress("UNUSED_PARAMETER")
private fun encodeUriComponent(value: String): String = js("encodeURIComponent(value)")

@Suppress("UNUSED_PARAMETER")
private fun decodeUriComponent(value: String): String = js("decodeURIComponent(value)")

private const val MANIFEST_PATH = "manifest.json"
private const val FRAME_PARAM = "frame"
private const val LOCALE_PARAM = "locale"
