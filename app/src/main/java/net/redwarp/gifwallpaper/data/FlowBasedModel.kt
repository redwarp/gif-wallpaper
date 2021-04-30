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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType

internal const val SHARED_PREF_NAME = "wallpaper_pref"

/**
 * Arbitrary delay to avoid over-requesting colors refresh.
 */
private const val REFRESH_DELAY = 200L

class FlowBasedModel private constructor(context: Context) {
    private val settings = Settings(context)
    private val _wallpaperStatusFlow = MutableSharedFlow<WallpaperStatus>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _translationEventFlow = MutableSharedFlow<TranslationEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _updateFlow =
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _colorInfoFlow = MutableStateFlow<ColorInfo>(NotSet)

    private var isColorSet = false

    val scaleTypeFlow: Flow<ScaleType> get() = settings.scaleTypeFlow
    val rotationFlow: Flow<Rotation> get() = settings.rotationFlow
    val translationFlow: Flow<Translation> get() = settings.translationFlow
    val translationEventFlow: Flow<TranslationEvent> get() = _translationEventFlow.distinctUntilChanged()
    val backgroundColorFlow: Flow<Int> get() = settings.backgroundColorFlow
    val wallpaperStatusFlow: SharedFlow<WallpaperStatus> get() = _wallpaperStatusFlow
    val colorInfoFlow: Flow<ColorInfo> get() = _colorInfoFlow
    val shouldPlay: Flow<Boolean> =
        powerSaveFlow(context).combine(thermalThrottleFlow(context)) { powerSave, thermalThrottle ->
            !(powerSave || thermalThrottle)
        }

    @OptIn(FlowPreview::class)
    val updateFlow: Flow<Unit>
        get() = _updateFlow.debounce(REFRESH_DELAY)

    init {
        GlobalScope.launch {
            loadInitialData()
            rotationFlow.onEach {
                _updateFlow.tryEmit(Unit)
            }.launchIn(this)
            scaleTypeFlow.onEach {
                _updateFlow.tryEmit(Unit)
            }.launchIn(this)
            translationFlow.onEach {
                _updateFlow.tryEmit(Unit)
            }.launchIn(this)
            backgroundColorFlow.onEach {
                _updateFlow.tryEmit(Unit)
            }.launchIn(this)
            wallpaperStatusFlow.onEach { status ->
                if (status is WallpaperStatus.NotSet) isColorSet = false

                if (status is WallpaperStatus.Wallpaper) {
                    _colorInfoFlow.emitAll(extractColorScheme(status))
                } else {
                    _colorInfoFlow.tryEmit(NotSet)
                }
                _updateFlow.tryEmit(Unit)
            }.launchIn(this)
            colorInfoFlow.onEach { colorInfo ->
                if (!isColorSet && colorInfo is ColorScheme) {
                    settings.setBackgroundColor(colorInfo.defaultColor)
                }
            }.launchIn(this)
        }
    }

    suspend fun setScaleType(scaleType: ScaleType) {
        resetTranslate()
        settings.setScaleType(scaleType)
    }

    suspend fun setRotation(rotation: Rotation) {
        resetTranslate()
        settings.setRotation(rotation)
    }

    suspend fun resetTranslate() {
        settings.setTranslation(Translation(0f, 0f))
        _translationEventFlow.tryEmit(TranslationEvent.Reset)
    }

    suspend fun postTranslate(translateX: Float, translateY: Float) {
        settings.postTranslation(Translation(translateX, translateY))
        _translationEventFlow.tryEmit(TranslationEvent.PostTranslate(translateX, translateY))
    }

    suspend fun setBackgroundColor(@ColorInt color: Int) {
        settings.setBackgroundColor(color)
    }

    suspend fun loadNewGif(context: Context, uri: Uri) {
        isColorSet = false
        _wallpaperStatusFlow.emitAll(GifLoader.loadNewGif(context, settings, uri))
    }

    suspend fun clearGif() {
        isColorSet = false
        _wallpaperStatusFlow.tryEmit(WallpaperStatus.NotSet)
        GifLoader.clearGif(settings)
        setBackgroundColor(Color.BLACK)
        setScaleType(ScaleType.FIT_CENTER)
        setRotation(Rotation.NORTH)
    }

    private suspend fun loadInitialData() = withContext(Dispatchers.Default) {
        _wallpaperStatusFlow.tryEmit(GifLoader.loadInitialValue(settings))
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
