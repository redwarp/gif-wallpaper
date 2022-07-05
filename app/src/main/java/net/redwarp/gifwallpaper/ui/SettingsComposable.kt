/* Copyright 2020 Benoit Vermont
 * Copyright 2020 GifWallpaper Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.redwarp.gifwallpaper.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.insets.statusBarsPadding
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.AppSettings
import net.redwarp.gifwallpaper.R

@Composable
fun Setting(title: String, summary: String, checked: Boolean, onCheckedChanged: (Boolean) -> Unit) {
    Row {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.h6)
            Text(text = summary, style = MaterialTheme.typography.body1)
        }
        Spacer(modifier = Modifier.size(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChanged)
    }
}

@Composable
@Preview
fun SettingPreview() {
    AppTheme {
        Setting(
            title = "Battery Saver",
            summary = "Pause GIF when Battery Saver is enabled",
            checked = false,
            onCheckedChanged = {}
        )
    }
}

@Composable
fun SettingUi(navController: NavController, appSettings: AppSettings) {
    val isPowerSaving by appSettings.powerSavingSettingFlow.collectAsState(initial = false)
    val isThermalThrottle by appSettings.thermalThrottleSettingFlow.collectAsState(initial = false)

    UpdateStatusBarColors()

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            BasicTopBar(
                modifier = Modifier.statusBarsPadding(),
                title = stringResource(id = R.string.settings),
                navController = navController
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Setting(
                title = stringResource(id = R.string.battery_saver),
                summary = stringResource(id = R.string.pause_blayback_battery_saver),
                checked = isPowerSaving,
                onCheckedChanged = { enabled ->
                    scope.launch {
                        appSettings.setPowerSaving(enabled)
                    }
                }
            )

            if (appSettings.isThermalThrottleSupported) {
                Setting(
                    title = stringResource(id = R.string.thermal_throttle),
                    summary = stringResource(id = R.string.pause_playback_running_hot),
                    checked = isThermalThrottle,
                    onCheckedChanged = { enabled ->
                        scope.launch {
                            appSettings.setThermalThrottle(enabled)
                        }
                    }
                )
            }
        }
    }
}
