package artboard.sample.light

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import artboard.sample.light.resources.Res
import artboard.sample.light.resources.receipt_total
import org.jetbrains.compose.resources.stringResource

/**
 * Sample UI for the snapshot ("light mode") consumer contract.
 *
 * This module declares **only** a `jvm()` target — no `wasmJs`. It exists to prove
 * that Artboard binds to a target the consumer already has, and that previews whose
 * dependency graph is not Wasm-safe still get a gallery.
 */

@Composable
fun ReceiptCard(
    merchant: String,
    lines: List<String>,
    total: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(merchant, style = MaterialTheme.typography.titleMedium)
            lines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium)
            }
            Text(total, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun LedgerScreen(
    title: String,
    entries: List<String>,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            entries.forEach { entry ->
                Text(entry, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** Consumer-owned theme; Artboard only publishes the light/dark signal. */
@Composable
private fun LightSampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

/**
 * Uses a localized string and therefore renders differently per locale, which is how
 * the snapshot renderers' locale axis is verified end to end.
 */
@Preview(name = "Receipt · itemized", group = "Cards")
@Composable
fun ReceiptCardPreview() = LightSampleTheme {
    ReceiptCard(
        merchant = "Corner Roasters",
        lines = listOf("Cortado    3.75", "Croissant  4.25"),
        total = "${stringResource(Res.string.receipt_total)}      8.00",
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(name = "Receipt · empty", group = "Cards")
@Composable
fun ReceiptCardEmptyPreview() = LightSampleTheme {
    ReceiptCard(
        merchant = "Corner Roasters",
        lines = listOf("No items yet."),
        total = "Total      0.00",
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(name = "Wide ledger", group = "Cards", widthDp = 480)
@Composable
fun WideReceiptPreview() = LightSampleTheme {
    ReceiptCard(
        merchant = "Wide layout check",
        lines = listOf("Declared widthDp, default heightDp."),
        total = "—",
        modifier = Modifier.padding(16.dp),
    )
}

/** Named `*ScreenPreview` so the board places it in the Screens zone. */
@Preview(name = "Ledger", group = "App", widthDp = 360, heightDp = 640)
@Composable
fun LedgerScreenPreview() = LightSampleTheme {
    LedgerScreen(
        title = "Ledger",
        entries = listOf("Opened  08:15", "Closed  17:40", "Net     +128.50"),
    )
}
