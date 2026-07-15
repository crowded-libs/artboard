package com.crowdedlibs.cafe.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.crowdedlibs.cafe.resources.Res
import com.crowdedlibs.cafe.resources.settings_about
import com.crowdedlibs.cafe.resources.settings_eyebrow
import com.crowdedlibs.cafe.resources.settings_notifications
import com.crowdedlibs.cafe.resources.settings_order_updates
import com.crowdedlibs.cafe.resources.settings_order_updates_detail
import com.crowdedlibs.cafe.resources.settings_profile
import com.crowdedlibs.cafe.resources.settings_profile_detail
import com.crowdedlibs.cafe.resources.settings_profile_name
import com.crowdedlibs.cafe.resources.settings_promotions
import com.crowdedlibs.cafe.resources.settings_promotions_detail
import com.crowdedlibs.cafe.resources.settings_title
import com.crowdedlibs.cafe.resources.settings_version
import com.crowdedlibs.cafe.ui.CafeAppFrame
import com.crowdedlibs.cafe.ui.CafeTab
import com.crowdedlibs.cafe.ui.components.CafeSwitch
import com.crowdedlibs.cafe.ui.components.DashedRule
import com.crowdedlibs.cafe.ui.components.Monogram
import com.crowdedlibs.cafe.ui.components.Text
import com.crowdedlibs.cafe.ui.components.PaperCard
import com.crowdedlibs.cafe.ui.components.ScreenHeader
import com.crowdedlibs.cafe.ui.theme.CafeTheme
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.stringResource

/** Settings tab: the regular's loyalty card, then notification toggles. */
@Composable
fun SettingsScreen(
    orderUpdates: Boolean,
    promotions: Boolean,
    onOrderUpdatesChange: (Boolean) -> Unit,
    onPromotionsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Column(
        modifier.fillMaxSize().background(colors.paper).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader(
            eyebrow = stringResource(Res.string.settings_eyebrow),
            title = stringResource(Res.string.settings_title),
        )

        // The loyalty punch-card, on the chalkboard.
        PaperCard(Modifier.fillMaxWidth(), color = colors.board, border = false) {
            Row(
                Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Monogram(stringResource(Res.string.settings_profile_name), size = 52.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(Res.string.settings_profile).uppercase(), style = type.label.copy(color = colors.stamp))
                    Text(stringResource(Res.string.settings_profile_name), style = type.heading.copy(color = colors.chalk))
                    Text(stringResource(Res.string.settings_profile_detail), style = type.mono.copy(color = colors.chalk.copy(alpha = 0.7f)))
                }
            }
        }

        Text(stringResource(Res.string.settings_notifications).uppercase(), style = type.label.copy(color = colors.inkSoft))
        PaperCard(Modifier.fillMaxWidth()) {
            Column {
                ToggleRow(
                    title = stringResource(Res.string.settings_order_updates),
                    detail = stringResource(Res.string.settings_order_updates_detail),
                    checked = orderUpdates,
                    onCheckedChange = onOrderUpdatesChange,
                )
                DashedRule(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                ToggleRow(
                    title = stringResource(Res.string.settings_promotions),
                    detail = stringResource(Res.string.settings_promotions_detail),
                    checked = promotions,
                    onCheckedChange = onPromotionsChange,
                )
            }
        }

        Text(stringResource(Res.string.settings_about).uppercase(), style = type.label.copy(color = colors.inkSoft))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text(stringResource(Res.string.settings_version), style = type.mono.copy(color = colors.inkSoft))
            DashedRule(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp))
            Text("1.0", style = type.mono.copy(color = colors.ink))
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = CafeTheme.colors
    val type = CafeTheme.type
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = type.serifItem.copy(color = colors.ink))
            Text(detail, style = type.bodySmall.copy(color = colors.inkSoft))
        }
        CafeSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(name = "Settings", group = "Account", widthDp = 402, heightDp = 874)
@Composable
fun SettingsScreenPreview() {
    CafeTheme(darkTheme = isSystemInDarkTheme()) {
        CafeAppFrame(selectedTab = CafeTab.Settings, cartCount = 0) { padding ->
            SettingsScreen(
                orderUpdates = true,
                promotions = false,
                onOrderUpdatesChange = {},
                onPromotionsChange = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}
