/* Copyright 2020 Redwarp
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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@Suppress("unused")
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val dataStore = AppSettingsPreferencesDataStore(requireContext())
        preferenceManager?.preferenceDataStore = dataStore

        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}

class AppSettingsPreferencesDataStore(context: Context) : PreferenceDataStore() {
    private val appSettings = AppSettings.get(context)

    override fun getBoolean(key: String, defValue: Boolean): Boolean = runBlocking {
        appSettings.getBoolean(key, defValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        runBlocking {
            appSettings.putBoolean(key, value)
        }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("app_settings")

class AppSettings(context: Context) {
    private val context = context.applicationContext
    private val powerSavingKey = booleanPreferencesKey("power_saving")

    val powerSavingSettingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[powerSavingKey] ?: false
    }

    suspend fun getBoolean(key: String, defValue: Boolean): Boolean {
        val preferenceKey = booleanPreferencesKey(key)

        return context.dataStore.data.map { preferences ->
            preferences[preferenceKey]
        }.firstOrNull() ?: defValue
    }

    suspend fun putBoolean(key: String, value: Boolean) {
        val preferenceKey = booleanPreferencesKey(key)

        context.dataStore.edit { preferences ->
            preferences[preferenceKey] = value
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // Suppressed because it's the application context.
        private lateinit var instance: AppSettings

        fun get(context: Context): AppSettings {
            instance = if (Companion::instance.isInitialized) {
                instance
            } else {
                AppSettings(context.applicationContext)
            }
            return instance
        }
    }
}
