package artboard.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Domain-free sample UI for dogfooding Artboard.
 *
 * Naming convention for kinds:
 * - `*ScreenPreview` → Screens zone
 * - other `*Preview` → Components zone
 *
 * It intentionally avoids product targets and resources to keep the plugin
 * feedback loop small; the café showcase covers those integration paths.
 */

@Composable
fun WelcomeCard(
    title: String,
    body: String,
    primary: String,
    secondary: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {}) { Text(primary) }
            OutlinedButton(onClick = {}) { Text(secondary) }
        }
    }
}

@Composable
fun StatusChipRow(
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Statuses", style = MaterialTheme.typography.titleMedium)
        labels.forEach { label ->
            Text("• $label", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun HomeScreen(
    title: String,
    subtitle: String,
    action: String,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text(action)
            }
        }
    }
}

/**
 * The sample's own theme — the reference for how a consumer follows the gallery.
 *
 * Artboard imposes no theme on preview bodies; it only publishes the light/dark
 * signal through `LocalSystemTheme`, which `isSystemInDarkTheme()` reads. A
 * consumer keys their own `colorScheme` off that boolean and their previews track
 * the gallery's theme toggle with no Artboard dependency at all. Any design
 * system that honours this convention works the same way.
 */
@Composable
private fun SampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

@Preview(name = "Welcome · default", group = "Cards")
@Composable
fun WelcomeCardPreview() = SampleTheme {
    WelcomeCard(
        title = "Welcome to Artboard",
        body = "Compose previews, arranged on one spatial board.",
        primary = "Open preview",
        secondary = "Copy link",
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(name = "Welcome · empty copy", group = "Cards")
@Composable
fun WelcomeCardEmptyPreview() = SampleTheme {
    WelcomeCard(
        title = "Empty state",
        body = "No items yet. Previews should cover empty, loading, and error.",
        primary = "Primary",
        secondary = "Secondary",
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(name = "Statuses", group = "Lists", widthDp = 320, heightDp = 240)
@Composable
fun StatusChipRowPreview() = SampleTheme {
    StatusChipRow(labels = listOf("Idle", "Loading", "Ready", "Error"))
}

/** Named *ScreenPreview so the board places it in the Screens zone. */
@Preview(name = "Home Screen", group = "App", widthDp = 360, heightDp = 640)
@Composable
fun HomeScreenPreview() = SampleTheme {
    HomeScreen(
        title = "Home",
        subtitle = "This Wasm-only project exercises the consumer plugin contract.",
        action = "Continue",
    )
}
