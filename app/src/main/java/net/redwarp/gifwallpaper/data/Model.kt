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

import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType

internal const val SHARED_PREF_NAME = "wallpaper_pref"
internal const val KEY_WALLPAPER_URI = "wallpaper_uri"
internal const val KEY_WALLPAPER_SCALE_TYPE = "wallpaper_scale_type"
internal const val KEY_WALLPAPER_BACKGROUND_COLOR = "wallpaper_background_color"
internal const val KEY_WALLPAPER_ROTATION = "wallpaper_rotation"
internal const val KEY_WALLPAPER_TRANSLATE_X = "wallpaper_translate_x"
internal const val KEY_WALLPAPER_TRANSLATE_Y = "wallpaper_translate_y"

class Model private constructor(val context: Context) {
    private val _wallpaperStatus = WallpaperLiveData(context)

    // private val _scaleTypeData = ScaleTypeData(context)
    // private val _rotationData = RotationData(context)
    // private val _backgroundColorData = MediatorLiveData<Int>()
    private val _postTranslateData = MutableLiveData<Pair<Float, Float>>()

    // private val _translateData = MutableLiveData<Pair<Float, Float>>()
    // private val _translationEvents = MutableLiveData<TranslationEvent>()

    val wallpaperStatus: LiveData<WallpaperStatus> get() = _wallpaperStatus

    // val scaleTypeData: LiveData<ScaleType> get() = _scaleTypeData
    // val rotationData: LiveData<Rotation> get() = _rotationData
    val colorInfoData: LiveData<ColorInfo> = ColorLiveData(context, wallpaperStatus)
    // val backgroundColorData: LiveData<Int> get() = _backgroundColorData
    // val postTranslationData: LiveData<Pair<Float, Float>> get() = _postTranslateData

    // val translationData: LiveData<Pair<Float, Float>> get() = _translateData
    // val translationEvents: LiveData<TranslationEvent> get() = _translationEvents

    private var isColorSet = true

    init {
        colorInfoData.observeForever { colorInfo ->
            if (colorInfo is ColorScheme) {
                ModelFlow.get(context).setBackgroundColor(colorInfo.defaultColor)
            }
        }
        // _backgroundColorData.addSource(colorInfoData) { colorInfo ->
        //     if (!isColorSet) {
        //         if (colorInfo is ColorScheme) {
        //             _backgroundColorData.postValue(colorInfo.defaultColor)
        //             isColorSet = true
        //         }
        //     }
        // }
        // _backgroundColorData.value = loadBackgroundColor(context)
        // _backgroundColorData.observeForever { backgroundColor ->
        //     storeBackgroundColor(context, backgroundColor)
        // }
        // val loadTranslation = loadTranslation(context)
        // _translateData.value = loadTranslation
        // _translateData.observeForever { (translateX, translateY) ->
        //     storeTranslation(context, translateX, translateY)
        // }

        wallpaperStatus.observeForever {
            if (it is WallpaperStatus.NotSet) isColorSet = false
        }
    }

    private fun storeBackgroundColor(context: Context, backgroundColor: Int) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(KEY_WALLPAPER_BACKGROUND_COLOR, backgroundColor).apply()
    }

    private fun loadBackgroundColor(context: Context): Int {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_WALLPAPER_BACKGROUND_COLOR, Color.RED)
    }

    private fun loadTranslation(context: Context): Pair<Float, Float> {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getFloat(
            KEY_WALLPAPER_TRANSLATE_X,
            0f
        ) to sharedPreferences.getFloat(KEY_WALLPAPER_TRANSLATE_Y, 0f)
    }

    private fun storeTranslation(context: Context, translateX: Float, translateY: Float) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putFloat(KEY_WALLPAPER_TRANSLATE_X, translateX)
            .putFloat(KEY_WALLPAPER_TRANSLATE_Y, translateY)
            .apply()
    }

    fun loadNewGif(uri: Uri) {
        isColorSet = false
        _wallpaperStatus.loadNewGif(uri)
    }

    fun clearGif() {
        isColorSet = false
        _wallpaperStatus.clearGif()
        ModelFlow.get(context).setBackgroundColor(Color.BLACK)
        ModelFlow.get(context).setScaleType(ScaleType.FIT_CENTER)
        ModelFlow.get(context).setRotation(Rotation.NORTH)
    }

    // fun setScaleType(scaleType: ScaleType) {
    //     resetTranslate()
    //     ModelFlow.get(context).setScaleType(scaleType)
    // }

    // fun setRotation(rotation: Rotation) {
    //     resetTranslate()
    //     // _rotationData.setRotation(rotation)
    //
    //     ModelFlow.get(context).setRotation(rotation)
    // }

    // fun setBackgroundColor(@ColorInt color: Int) {
    //     _backgroundColorData.postValue(color)
    // }

    // fun postTranslate(translateX: Float, translateY: Float) {
    //     runBlocking {
    //         _postTranslateData.postValue(translateX to translateY)
    //         val translation =
    //             ModelFlow.get(context).translationFlow.firstOrNull()?.let { previous ->
    //                 Translation(previous.x + translateX, previous.y + translateY)
    //             } ?: Translation(translateX, translateY)
    //         ModelFlow.get(context).setTranslation(translation)
    //     }
    //
    //     _translationEvents.postValue(TranslationEvent.PostTranslate(translateX, translateY))
    // }

    // fun resetTranslate() {
    //     ModelFlow.get(context).setTranslation(Translation(0f, 0f))
    //     // _translateData.postValue(0f to 0f)
    //     _translationEvents.postValue(TranslationEvent.Reset)
    // }

    private class ScaleTypeData(private val context: Context) :
        LiveData<ScaleType>() {
        init {
            loadInitialValue()
        }

        private fun loadInitialValue() {
            postValue(loadCurrentScaleType(context))
        }

        private fun loadCurrentScaleType(context: Context): ScaleType {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val scaleTypeOrdinal = sharedPreferences.getInt(KEY_WALLPAPER_SCALE_TYPE, 0)
            return ScaleType.values()[scaleTypeOrdinal]
        }

        private fun storeCurrentScaleType(
            context: Context,
            scaleType: ScaleType
        ) {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit().putInt(KEY_WALLPAPER_SCALE_TYPE, scaleType.ordinal).apply()
        }

        fun setScaleType(scaleType: ScaleType) {
            // storeCurrentScaleType(context, scaleType)
            ModelFlow.get(context).setScaleType(scaleType)
            postValue(scaleType)
        }
    }

    private class RotationData(private val context: Context) :
        LiveData<Rotation>() {
        init {
            postValue(loadCurrentRotation(context))
        }

        private fun loadCurrentRotation(context: Context): Rotation {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val rotationOrdinal = sharedPreferences.getInt(KEY_WALLPAPER_ROTATION, 0)
            return Rotation.values()[rotationOrdinal]
        }

        private fun storeCurrentRotation(
            context: Context,
            rotation: Rotation
        ) {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit().putInt(KEY_WALLPAPER_ROTATION, rotation.ordinal).apply()
        }

        fun setRotation(rotation: Rotation) {
            // storeCurrentRotation(context, rotation)
            ModelFlow.get(context).setRotation(rotation)
            postValue(rotation)
        }
    }

    companion object {
        private lateinit var instance: Model

        fun get(context: Context): Model {
            instance =
                if (Companion::instance.isInitialized) instance else (
                    Model(
                        context.applicationContext
                    )
                    )

            return instance
        }
    }
}
