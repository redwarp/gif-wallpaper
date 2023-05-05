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
package net.redwarp.gifwallpaper.data

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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.redwarp.gifwallpaper.AppSettings
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType

/**
 * Arbitrary delay to avoid over-requesting colors refresh.
 */
private const val REFRESH_DELAY = 200L

class FlowBasedModel(
    context: Context,
    appScope: CoroutineScope,
    private val wallpaperSettings: WallpaperSettings,
    appSettings: AppSettings,
) {
    private val _wallpaperStatusFlow = MutableSharedFlow<WallpaperStatus>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _translationEventFlow = MutableSharedFlow<TranslationEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _updateFlow =
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _colorInfoFlow = MutableStateFlow<ColorInfo>(NotSet)

    private var isColorSet = true

    val scaleTypeFlow: Flow<ScaleType> get() = wallpaperSettings.scaleTypeFlow
    val rotationFlow: Flow<Rotation> get() = wallpaperSettings.rotationFlow
    val translationFlow: Flow<Translation> get() = wallpaperSettings.translationFlow
    val translationEventFlow: Flow<TranslationEvent> get() = _translationEventFlow.distinctUntilChanged()
    val backgroundColorFlow: Flow<Int> get() = wallpaperSettings.backgroundColorFlow
    val wallpaperStatusFlow: Flow<WallpaperStatus> get() = _wallpaperStatusFlow
    val colorInfoFlow: Flow<ColorInfo> get() = _colorInfoFlow
    val shouldPlay: Flow<Boolean> = combine(
        appSettings.powerSavingSettingFlow,
        powerSaveFlow(context),
        appSettings.thermalThrottleSettingFlow,
        thermalThrottleFlow(context),
    ) { powerSavingSetting, powerSave, thermalThrottleSetting, thermalThrottle ->
        val powerSavingOn = powerSavingSetting && powerSave
        val thermalThrottlingOn = thermalThrottleSetting && thermalThrottle

        !(powerSavingOn || thermalThrottlingOn)
    }
    val infiniteLoopFlow: Flow<Boolean> = appSettings.infiniteLoopSettingFlow

    @OptIn(FlowPreview::class)
    val updateFlow: Flow<Unit>
        get() = _updateFlow.debounce(REFRESH_DELAY)

    init {
        appScope.launch {
            loadInitialData()
            rotationFlow.onEach {
                _updateFlow.emit(Unit)
            }.launchIn(this)
            scaleTypeFlow.onEach {
                _updateFlow.emit(Unit)
            }.launchIn(this)
            translationFlow.onEach {
                _updateFlow.emit(Unit)
            }.launchIn(this)
            backgroundColorFlow.onEach {
                _updateFlow.emit(Unit)
            }.launchIn(this)
            wallpaperStatusFlow.onEach { status ->
                if (status is WallpaperStatus.NotSet) isColorSet = false

                if (status is WallpaperStatus.Wallpaper) {
                    _colorInfoFlow.emitAll(extractColorScheme(status))
                } else {
                    _colorInfoFlow.emit(NotSet)
                }
                _updateFlow.emit(Unit)
            }.launchIn(this)
            colorInfoFlow.onEach { colorInfo ->
                if (!isColorSet && colorInfo is ColorScheme) {
                    wallpaperSettings.setBackgroundColor(colorInfo.defaultColor)
                }
            }.launchIn(this)
        }
    }

    private suspend fun setScaleType(scaleType: ScaleType) {
        resetTranslate()
        wallpaperSettings.setScaleType(scaleType)
    }

    suspend fun setNextScale() {
        val currentScale = scaleTypeFlow.first()
        val nextScale = ScaleType.values()[(currentScale.ordinal + 1) % ScaleType.values().size]
        setScaleType(nextScale)
    }

    private suspend fun setRotation(rotation: Rotation) {
        resetTranslate()
        wallpaperSettings.setRotation(rotation)
    }

    suspend fun setNextRotation() {
        val currentRotation = rotationFlow.first()
        val nextRotation = Rotation.values()[(currentRotation.ordinal + 1) % Rotation.values().size]
        setRotation(nextRotation)
    }

    suspend fun resetTranslate() {
        wallpaperSettings.resetTranslation()
        _translationEventFlow.emit(TranslationEvent.Reset)
    }

    suspend fun postTranslate(translateX: Float, translateY: Float) {
        wallpaperSettings.postTranslation(Translation(translateX, translateY))
        _translationEventFlow.emit(TranslationEvent.PostTranslate(translateX, translateY))
    }

    suspend fun setBackgroundColor(@ColorInt color: Int) {
        wallpaperSettings.setBackgroundColor(color)
    }

    suspend fun loadNewGif(context: Context, uri: Uri) {
        isColorSet = false
        _wallpaperStatusFlow.emitAll(GifLoader.loadNewGif(context, wallpaperSettings, uri))
    }

    suspend fun clearGif() {
        isColorSet = false
        _wallpaperStatusFlow.emit(WallpaperStatus.NotSet)
        GifLoader.clearGif(wallpaperSettings)
        setBackgroundColor(Color.BLACK)
        setScaleType(ScaleType.FIT_CENTER)
        setRotation(Rotation.NORTH)
    }

    private suspend fun loadInitialData() = withContext(Dispatchers.Default) {
        _wallpaperStatusFlow.emit(GifLoader.loadInitialValue(wallpaperSettings))
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
            Bitmap.Config.ARGB_8888,
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
}
