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
package net.redwarp.gifwallpaper.renderer

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.lifecycle.MediatorLiveData
import app.redwarp.gif.android.GifDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.Model
import net.redwarp.gifwallpaper.data.ModelFlow
import net.redwarp.gifwallpaper.data.TranslationEvent
import net.redwarp.gifwallpaper.data.WallpaperStatus

class DrawableMapper constructor(
    model: Model,
    modelFlow: ModelFlow,
    animated: Boolean,
    unsetText: String,
    isService: Boolean
) : MediatorLiveData<Drawable>() {

    init {

        addSource(model.wallpaperStatus) { status ->
            CoroutineScope(Dispatchers.Main).launch {
                when (status) {
                    WallpaperStatus.NotSet -> postValue(
                        TextDrawable(model.context, unsetText)
                    )
                    WallpaperStatus.Loading -> postValue(
                        TextDrawable(
                            model.context,
                            model.context.getString(
                                R.string.loading
                            )
                        )
                    )
                    is WallpaperStatus.Wallpaper -> {
                        val gif = GifDrawable(status.gifDescriptor).apply {
                            start()
                        }
                        val scaleType =
                            model.scaleTypeData.value ?: ScaleType.FIT_CENTER
                        val rotation = modelFlow.rotationFlow.first()
                        val backgroundColor = model.backgroundColorData.value ?: Color.BLACK
                        val translation =
                            modelFlow.translationFlow.first()
                        val wrapper = GifWrapperDrawable(
                            gif,
                            scaleType,
                            rotation,
                            translation.x to translation.y
                        )
                        wrapper.setBackgroundColor(backgroundColor)
                        postValue(wrapper)
                    }
                }
            }
        }
        addSource(model.backgroundColorData) { backgroundColor ->
            (value as? GifWrapperDrawable)?.setBackgroundColor(backgroundColor)
        }
        addSource(model.scaleTypeData) { scaleType ->
            (value as? GifWrapperDrawable)?.setScaledType(scaleType, animated)
        }
        CoroutineScope(Dispatchers.Main).launch {
            modelFlow.rotationFlow.collect { rotation ->
                (value as? GifWrapperDrawable)?.setRotation(rotation, animated)
            }
        }

        if (isService) {
            CoroutineScope(Dispatchers.Main).launch {
                modelFlow.translationFlow.collect { translation ->
                    (value as? GifWrapperDrawable)?.setTranslate(
                        translation.x,
                        translation.y,
                        animated
                    )
                }
            }
        } else {
            addSource(model.translationEvents) { event ->
                when (event) {
                    is TranslationEvent.PostTranslate -> {
                        (value as? GifWrapperDrawable)?.postTranslate(
                            event.translateX,
                            event.translateY
                        )
                    }
                    TranslationEvent.Reset -> {
                        (value as? GifWrapperDrawable)?.resetTranslation(animated)
                    }
                }
            }
        }
    }
}
