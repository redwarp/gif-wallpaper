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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.core.graphics.alpha
import androidx.palette.graphics.Palette
import app.redwarp.gif.android.GifDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType

internal const val SHARED_PREF_NAME = "wallpaper_pref"
internal const val KEY_WALLPAPER_SCALE_TYPE = "wallpaper_scale_type"
internal const val KEY_WALLPAPER_BACKGROUND_COLOR = "wallpaper_background_color"
internal const val KEY_WALLPAPER_ROTATION = "wallpaper_rotation"
internal const val KEY_WALLPAPER_TRANSLATE_X = "wallpaper_translate_x"
internal const val KEY_WALLPAPER_TRANSLATE_Y = "wallpaper_translate_y"

class FlowBasedModel private constructor(context: Context) {
    private val _scaleTypeFlow =
        MutableSharedFlow<ScaleType>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _rotationFlow =
        MutableSharedFlow<Rotation>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _translationFlow =
        MutableSharedFlow<Translation>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _backgroundColorFlow =
        MutableSharedFlow<Int>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _wallpaperStatusFlow = MutableSharedFlow<WallpaperStatus>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _translationEventFlow = MutableSharedFlow<TranslationEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _postTranslateData =
        MutableSharedFlow<Translation>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _colorInfoFlow =
        MutableSharedFlow<ColorInfo>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var isColorSet = false

    val scaleTypeFlow: Flow<ScaleType> get() = _scaleTypeFlow.distinctUntilChanged()
    val rotationFlow: Flow<Rotation> get() = _rotationFlow.distinctUntilChanged()
    val translationFlow: Flow<Translation> get() = _translationFlow.distinctUntilChanged()
    val translationEventFlow: Flow<TranslationEvent> get() = _translationEventFlow.distinctUntilChanged()
    val backgroundColorFlow: Flow<Int> get() = _backgroundColorFlow.distinctUntilChanged()
    val wallpaperStatusFlow: SharedFlow<WallpaperStatus> get() = _wallpaperStatusFlow
    val colorInfoFlow: Flow<ColorInfo> get() = _colorInfoFlow.distinctUntilChanged()

    init {
        GlobalScope.launch {
            val applicationContext = context.applicationContext
            loadInitialData(applicationContext)
            rotationFlow.drop(1).onEach { rotation ->
                storeCurrentRotation(applicationContext, rotation)
            }.launchIn(this)
            scaleTypeFlow.drop(1).onEach { scaleType ->
                storeCurrentScaleType(applicationContext, scaleType)
            }.launchIn(this)
            translationFlow.drop(1).onEach { translation ->
                storeTranslation(applicationContext, translation.x, translation.y)
            }.launchIn(this)
            backgroundColorFlow.drop(1).onEach { color ->
                storeBackgroundColor(applicationContext, color)
            }.launchIn(this)
            wallpaperStatusFlow.onEach { status ->
                if (status is WallpaperStatus.NotSet) isColorSet = false

                if (status is WallpaperStatus.Wallpaper) {
                    _colorInfoFlow.emitAll(extractColorScheme(status))
                } else {
                    _colorInfoFlow.tryEmit(NotSet)
                }
            }.launchIn(this)
            colorInfoFlow.onEach { colorInfo ->
                if (!isColorSet && colorInfo is ColorScheme) {
                    _backgroundColorFlow.tryEmit(colorInfo.defaultColor)
                }
            }.launchIn(this)
        }
    }

    fun setScaleType(scaleType: ScaleType) {
        resetTranslate()
        _scaleTypeFlow.tryEmit(scaleType)
    }

    fun setRotation(rotation: Rotation) {
        resetTranslate()
        _rotationFlow.tryEmit(rotation)
    }

    private fun setTranslation(translation: Translation) {
        _translationFlow.tryEmit(translation)
    }

    fun resetTranslate() {
        setTranslation(Translation(0f, 0f))
        _translationEventFlow.tryEmit(TranslationEvent.Reset)
    }

    fun postTranslate(translateX: Float, translateY: Float) {
        runBlocking {
            _postTranslateData.tryEmit(Translation(translateX, translateY))
            val translation = translationFlow.first().let { previous ->
                Translation(previous.x + translateX, previous.y + translateY)
            }
            setTranslation(translation)
        }

        _translationEventFlow.tryEmit(TranslationEvent.PostTranslate(translateX, translateY))
    }

    fun setBackgroundColor(@ColorInt color: Int) {
        _backgroundColorFlow.tryEmit(color)
    }

    fun loadNewGif(context: Context, uri: Uri) {
        isColorSet = false
        CoroutineScope(Dispatchers.Main).launch {
            _wallpaperStatusFlow.emitAll(GifLoader.loadNewGif(context, uri))
        }
    }

    fun clearGif(context: Context) {
        isColorSet = false
        _wallpaperStatusFlow.tryEmit(WallpaperStatus.NotSet)
        GifLoader.clearGif(context)
        setBackgroundColor(Color.BLACK)
        setScaleType(ScaleType.FIT_CENTER)
        setRotation(Rotation.NORTH)
    }

    private suspend fun loadInitialData(context: Context) = withContext(Dispatchers.Default) {
        _scaleTypeFlow.tryEmit(loadCurrentScaleType(context))
        _rotationFlow.tryEmit(loadCurrentRotation(context))
        _translationFlow.tryEmit(loadTranslation(context).let { Translation(it.first, it.second) })
        _backgroundColorFlow.tryEmit(loadBackgroundColor(context))
        _wallpaperStatusFlow.tryEmit(GifLoader.loadInitialValue(context))
        _colorInfoFlow.tryEmit(NotSet)
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

    private fun storeBackgroundColor(context: Context, backgroundColor: Int) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(KEY_WALLPAPER_BACKGROUND_COLOR, backgroundColor).apply()
    }

    private fun loadBackgroundColor(context: Context): Int {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_WALLPAPER_BACKGROUND_COLOR, Color.BLACK)
    }

    private suspend fun extractColorScheme(wallpaper: WallpaperStatus.Wallpaper) =
        withContext(Dispatchers.Default) {
            flow {
                emit(NotSet)
                val gif = GifDrawable(wallpaper.gifDescriptor)

                val defaultColor = calculateDefaultBackgroundColor(gif)
                val palette = calculatePalette(gif)

                emit(ColorScheme(defaultColor, palette))
            }
        }

    private fun calculatePalette(drawable: Drawable): Palette {
        val sample = Bitmap.createBitmap(
            drawable.intrinsicWidth / 2,
            drawable.intrinsicHeight / 2,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(sample)
        canvas.scale(0.5f, 0.5f)
        drawable.draw(canvas)

        val palette = Palette.from(sample).generate()
        sample.recycle()

        return palette
    }

    private fun calculateDefaultBackgroundColor(drawable: Drawable): Int {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val sample = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sample)
        canvas.clipRect(0, 0, 1, 1)
        drawable.draw(canvas)
        val color = sample.getPixel(0, 0)
        sample.recycle()

        return if (color.alpha == 0) Color.WHITE else color
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // Suppressed because it's the application context.
        private lateinit var instance: FlowBasedModel

        fun get(context: Context): FlowBasedModel {
            instance = if (Companion::instance.isInitialized) {
                instance
            } else {
                FlowBasedModel(context.applicationContext)
            }

            return instance
        }
    }
}

data class Translation(val x: Float, val y: Float)
