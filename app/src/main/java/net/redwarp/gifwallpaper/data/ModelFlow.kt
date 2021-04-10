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
import android.graphics.Color
import androidx.annotation.ColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType

class ModelFlow private constructor(val context: Context) {
    private val _scaleTypeFlow =
        MutableSharedFlow<ScaleType>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _rotationFlow =
        MutableSharedFlow<Rotation>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _translationFlow =
        MutableSharedFlow<Translation>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _backgroundColorFlow =
        MutableSharedFlow<Int>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _translationEventFlow = MutableSharedFlow<TranslationEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _postTranslateData =
        MutableSharedFlow<Translation>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val scaleTypeFlow: Flow<ScaleType> get() = _scaleTypeFlow.distinctUntilChanged()
    val rotationFlow: Flow<Rotation> get() = _rotationFlow.distinctUntilChanged()
    val translationFlow: Flow<Translation> get() = _translationFlow.distinctUntilChanged()
    val translationEventFlow: Flow<TranslationEvent> get() = _translationEventFlow.distinctUntilChanged()
    val backgroundColorFlow: Flow<Int> get() = _backgroundColorFlow.distinctUntilChanged()

    init {
        GlobalScope.launch {
            loadInitialData(context)
            rotationFlow.drop(1).onEach { rotation ->
                storeCurrentRotation(context, rotation)
            }.launchIn(this)
            scaleTypeFlow.drop(1).onEach { scaleType ->
                storeCurrentScaleType(context, scaleType)
            }.launchIn(this)
            translationFlow.drop(1).onEach { translation ->
                storeTranslation(context, translation.x, translation.y)
            }.launchIn(this)
            backgroundColorFlow.drop(1).onEach { color ->
                storeBackgroundColor(context, color)
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

    fun setTranslation(translation: Translation) {
        _translationFlow.tryEmit(translation)
    }

    fun resetTranslate() {
        setTranslation(Translation(0f, 0f))
        _translationEventFlow.tryEmit(TranslationEvent.Reset)
    }

    fun postTranslate(translateX: Float, translateY: Float) {
        runBlocking {
            _postTranslateData.tryEmit(Translation(translateX, translateY))
            val translation =
                get(context).translationFlow.firstOrNull()?.let { previous ->
                    Translation(previous.x + translateX, previous.y + translateY)
                } ?: Translation(translateX, translateY)
            get(context).setTranslation(translation)
        }

        _translationEventFlow.tryEmit(TranslationEvent.PostTranslate(translateX, translateY))
    }

    fun setBackgroundColor(@ColorInt color: Int) {
        _backgroundColorFlow.tryEmit(color)
    }

    private suspend fun loadInitialData(context: Context) = withContext(Dispatchers.Default) {
        _scaleTypeFlow.tryEmit(loadCurrentScaleType(context))
        _rotationFlow.tryEmit(loadCurrentRotation(context))
        _translationFlow.tryEmit(loadTranslation(context).let { Translation(it.first, it.second) })
        _backgroundColorFlow.tryEmit(loadBackgroundColor(context))
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
        return sharedPreferences.getInt(KEY_WALLPAPER_BACKGROUND_COLOR, Color.RED)
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // Suppressed because it's the application context.
        private lateinit var instance: ModelFlow

        fun get(context: Context): ModelFlow {
            instance = if (Companion::instance.isInitialized) {
                instance
            } else {
                ModelFlow(context.applicationContext)
            }

            return instance
        }
    }
}

data class Translation(val x: Float, val y: Float)
