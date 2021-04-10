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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
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

    val scaleTypeFlow: Flow<ScaleType> get() = _scaleTypeFlow.distinctUntilChanged()
    val rotationFlow: Flow<Rotation> get() = _rotationFlow.distinctUntilChanged()
    val translationFlow: Flow<Translation> get() = _translationFlow.distinctUntilChanged()
    val transformationsFlow = _scaleTypeFlow.zip(_rotationFlow) { scaleType, rotation ->
        Transformations(scaleType, rotation)
    }.distinctUntilChanged()

    init {
        CoroutineScope(GlobalScope.coroutineContext).launch {
            loadInitialData(context)
            rotationFlow.drop(1).collect { rotation ->
                storeCurrentRotation(context, rotation)
            }
            scaleTypeFlow.drop(1).collect { scaleType ->
                storeCurrentScaleType(context, scaleType)
            }
            translationFlow.drop(1).collect { translation ->
                storeTranslation(context, translation.x, translation.y)
            }
        }
    }

    fun setScaleType(scaleType: ScaleType) {
        _scaleTypeFlow.tryEmit(scaleType)
    }

    fun setRotation(rotation: Rotation) {
        _rotationFlow.tryEmit(rotation)
    }

    fun setTranslation(translation: Translation) {
        _translationFlow.tryEmit(translation)
    }

    private suspend fun loadInitialData(context: Context) = withContext(Dispatchers.Default) {
        _scaleTypeFlow.tryEmit(loadCurrentScaleType(context))
        _rotationFlow.tryEmit(loadCurrentRotation(context))
        _translationFlow.tryEmit(loadTranslation(context).let { Translation(it.first, it.second) })
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

data class Transformations(val scaleType: ScaleType, val rotation: Rotation)

data class Translation(val x: Float, val y: Float)
