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

import android.graphics.drawable.Drawable
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.MediatorLiveData
import app.redwarp.gif.android.GifDrawable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.Model
import net.redwarp.gifwallpaper.data.ModelFlow
import net.redwarp.gifwallpaper.data.TranslationEvent
import net.redwarp.gifwallpaper.data.WallpaperStatus

class DrawableMapper(
    model: Model,
    modelFlow: ModelFlow,
    animated: Boolean,
    unsetText: String,
    isService: Boolean,
    lifecycleScope: LifecycleCoroutineScope
) : MediatorLiveData<Drawable>() {

    init {
        addSource(model.wallpaperStatus) { status ->
            lifecycleScope.launchWhenStarted {
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
                        val scaleType = modelFlow.scaleTypeFlow.first()
                        val rotation = modelFlow.rotationFlow.first()
                        val backgroundColor = modelFlow.backgroundColorFlow.first()
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

        lifecycleScope.launchWhenStarted {
            modelFlow.backgroundColorFlow.onEach { backgroundColor ->
                (value as? GifWrapperDrawable)?.setBackgroundColor(backgroundColor)
            }.launchIn(this)
            modelFlow.scaleTypeFlow.onEach { scaleType ->
                (value as? GifWrapperDrawable)?.setScaledType(scaleType, animated)
            }.launchIn(this)
            modelFlow.rotationFlow.onEach { rotation ->
                (value as? GifWrapperDrawable)?.setRotation(rotation, animated)
            }.launchIn(this)

            if (isService) {
                modelFlow.translationFlow.onEach { translation ->
                    (value as? GifWrapperDrawable)?.setTranslate(
                        translation.x,
                        translation.y,
                        animated
                    )
                }.launchIn(this)
            } else {
                modelFlow.translationEventFlow.onEach { event ->
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
                }.launchIn(this)
            }
        }
    }
}
