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

import android.content.Context
import android.os.Bundle
import androidx.annotation.Keep
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.runBlocking
import net.redwarp.gifwallpaper.AppSettings
import net.redwarp.gifwallpaper.R

@Keep
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val dataStore = AppSettingsPreferencesDataStore(requireContext())
        preferenceManager?.preferenceDataStore = dataStore

        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        // setToolbarPosition(ToolbarPosition.TopOf)
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
