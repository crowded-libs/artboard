package artboard.model

import androidx.compose.runtime.Composable

/**
 * One addressable preview on the artboard.
 *
 * [id] must be stable across recompiles (e.g. FQCN + preview name) so deep links,
 * AI tools, and exports can target frames without UI coordinates.
 *
 * [kind] is inferred from the function and `@Preview(name)` naming convention
 * (`*ScreenPreview` → Screen, else Component).
 */
data class PreviewFrame(
    val id: String,
    val name: String,
    val group: String? = null,
    val kind: PreviewKind = PreviewKind.Component,
    val widthDp: Int? = null,
    val heightDp: Int? = null,
    val sourceFqName: String? = null,
    val content: @Composable () -> Unit,
)
