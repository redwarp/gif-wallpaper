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
package net.redwarp.gifwallpaper.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType
import java.io.File

private const val SHARED_PREF_NAME = "wallpaper_pref"
private const val KEY_WALLPAPER_BACKGROUND_COLOR = "wallpaper_background_color"
private const val KEY_WALLPAPER_ROTATION = "wallpaper_rotation"
private const val KEY_WALLPAPER_TRANSLATE_X = "wallpaper_translate_x"
private const val KEY_WALLPAPER_TRANSLATE_Y = "wallpaper_translate_y"
private const val KEY_WALLPAPER_SCALE_TYPE = "wallpaper_scale_type"
private const val KEY_WALLPAPER_URI = "wallpaper_uri"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "wallpaper_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context,
                SHARED_PREF_NAME,
                setOf(
                    KEY_WALLPAPER_ROTATION,
                    KEY_WALLPAPER_BACKGROUND_COLOR,
                    KEY_WALLPAPER_TRANSLATE_X,
                    KEY_WALLPAPER_TRANSLATE_Y,
                    KEY_WALLPAPER_SCALE_TYPE,
                    KEY_WALLPAPER_URI
                )
            )
        )
    }
)

@Suppress("PrivatePropertyName")
class WallpaperSettings private constructor(context: Context) {
    private val context = context.applicationContext
    private val WALLPAPER_ROTATION = intPreferencesKey(KEY_WALLPAPER_ROTATION)
    private val WALLPAPER_BACKGROUND_COLOR = intPreferencesKey(KEY_WALLPAPER_BACKGROUND_COLOR)
    private val WALLPAPER_TRANSLATE_X = floatPreferencesKey(KEY_WALLPAPER_TRANSLATE_X)
    private val WALLPAPER_TRANSLATE_Y = floatPreferencesKey(KEY_WALLPAPER_TRANSLATE_Y)
    private val WALLPAPER_SCALE_TYPE = intPreferencesKey(KEY_WALLPAPER_SCALE_TYPE)
    private val WALLPAPER_URI = stringPreferencesKey(KEY_WALLPAPER_URI)

    val rotationFlow: Flow<Rotation>
        get() = context.dataStore.data.mapNotNull { preferences ->
            preferences[WALLPAPER_ROTATION]?.let { rotationOrdinal ->
                Rotation.values()[rotationOrdinal]
            }
        }.distinctUntilChanged()

    val backgroundColorFlow
        get() = context.dataStore.data.mapNotNull { preferences ->
            preferences[WALLPAPER_BACKGROUND_COLOR]
        }.distinctUntilChanged()

    val translationFlow: Flow<Translation>
        get() = context.dataStore.data.mapNotNull { preferences ->
            val x = preferences[WALLPAPER_TRANSLATE_X]
            val y = preferences[WALLPAPER_TRANSLATE_Y]

            when {
                x != null && y != null -> Translation(x, y)
                else -> null
            }
        }.distinctUntilChanged()

    val scaleTypeFlow: Flow<ScaleType>
        get() = context.dataStore.data.mapNotNull { preferences ->
            preferences[WALLPAPER_SCALE_TYPE]?.let { scaleTypeOrdinal ->
                ScaleType.values()[scaleTypeOrdinal]
            }
        }.distinctUntilChanged()

    suspend fun setRotation(rotation: Rotation) {
        context.dataStore.edit { settings ->
            settings[WALLPAPER_ROTATION] = rotation.ordinal
        }
    }

    suspend fun setBackgroundColor(@ColorInt color: Int) {
        context.dataStore.edit { settings ->
            settings[WALLPAPER_BACKGROUND_COLOR] = color
        }
    }

    suspend fun setTranslation(translation: Translation) {
        context.dataStore.edit { settings ->
            settings[WALLPAPER_TRANSLATE_X] = translation.x
            settings[WALLPAPER_TRANSLATE_Y] = translation.y
        }
    }

    suspend fun postTranslation(translation: Translation) {
        context.dataStore.edit { settings ->
            val currentX = settings[WALLPAPER_TRANSLATE_X] ?: 0.0f
            val currentY = settings[WALLPAPER_TRANSLATE_Y] ?: 0.0f

            settings[WALLPAPER_TRANSLATE_X] = currentX + translation.x
            settings[WALLPAPER_TRANSLATE_Y] = currentY + translation.y
        }
    }

    suspend fun setScaleType(scaleType: ScaleType) {
        context.dataStore.edit { settings ->
            settings[WALLPAPER_SCALE_TYPE] = scaleType.ordinal
        }
    }

    suspend fun getWallpaperFile(): File? = context.dataStore.data.map { preferences ->
        val urlString = preferences[WALLPAPER_URI]
        urlString?.let {
            val path = Uri.parse(it).path
            if (path != null) {
                File(path)
            } else {
                null
            }
        }
    }.firstOrNull()

    suspend fun setWallpaperFile(file: File?) {
        context.dataStore.edit { preferences ->
            val string = file?.let { Uri.fromFile(it).toString() }
            if (string != null) {
                preferences[WALLPAPER_URI] = string
            } else {
                preferences.remove(WALLPAPER_URI)
            }
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // Suppressed because it's the application context.
        private lateinit var instance: WallpaperSettings

        fun get(context: Context): WallpaperSettings {
            instance = if (Companion::instance.isInitialized) {
                instance
            } else {
                WallpaperSettings(context.applicationContext)
            }
            return instance
        }
    }
}
