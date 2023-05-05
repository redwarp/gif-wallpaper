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
package net.redwarp.gifwallpaper

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AppSettings {
    val powerSavingSettingFlow: Flow<Boolean>
    val thermalThrottleSettingFlow: Flow<Boolean>
    val infiniteLoopSettingFlow: Flow<Boolean>
    val isThermalThrottleSupported: Boolean
    suspend fun setPowerSaving(enabled: Boolean)
    suspend fun setThermalThrottle(enabled: Boolean)
    suspend fun setInfiniteLoop(enabled: Boolean)
}

class DataStoreAppSettings(private val context: Context, ioScope: CoroutineScope) : AppSettings {
    private val powerSavingKey = booleanPreferencesKey("power_saving")
    private val thermalThrottleKey = booleanPreferencesKey("thermal_throttle")
    private val infiniteLoopKey = booleanPreferencesKey("infinite_loop")
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        "app_settings",
        scope = ioScope,
    )

    override val powerSavingSettingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[powerSavingKey] ?: context.resources.getBoolean(R.bool.power_saving_enabled)
    }
    override val thermalThrottleSettingFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[thermalThrottleKey]
                ?: context.resources.getBoolean(R.bool.thermal_throttle_enabled)
        }
    override val isThermalThrottleSupported: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    override val infiniteLoopSettingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[infiniteLoopKey] ?: context.resources.getBoolean(R.bool.infinite_loop_enabled)
    }

    override suspend fun setPowerSaving(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[powerSavingKey] = enabled
        }
    }

    override suspend fun setThermalThrottle(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[thermalThrottleKey] = enabled
        }
    }

    override suspend fun setInfiniteLoop(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[infiniteLoopKey] = enabled
        }
    }
}
