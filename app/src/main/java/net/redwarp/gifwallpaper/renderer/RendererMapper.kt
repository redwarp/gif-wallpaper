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
import android.view.SurfaceHolder
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.Gif
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.Model
import net.redwarp.gifwallpaper.data.WallpaperStatus

class RendererMapper(
    model: Model,
    surfaceHolder: SurfaceHolder,
    animated: Boolean,
    unsetText: String,
    isService: Boolean
) :
    MediatorLiveData<Renderer>() {
    init {
        addSource(model.wallpaperStatus) { status ->
            when (status) {
                WallpaperStatus.NotSet -> postValue(
                    TextRenderer(
                        model.context,
                        surfaceHolder,
                        unsetText
                    )
                )
                WallpaperStatus.Loading -> postValue(
                    TextRenderer(
                        model.context, surfaceHolder, model.context.getString(R.string.loading)
                    )
                )
                is WallpaperStatus.Wallpaper -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        val gif = Gif.loadGif(model.context, status.uri)
                        if (gif == null) {
                            postValue(
                                TextRenderer(
                                    model.context,
                                    surfaceHolder,
                                    unsetText
                                )
                            )
                            return@launch
                        }

                        val scaleType =
                            model.scaleTypeData.value ?: WallpaperRenderer.ScaleType.FIT_CENTER
                        val rotation = model.rotationData.value ?: WallpaperRenderer.Rotation.NORTH
                        val backgroundColor = model.backgroundColorData.value ?: Color.BLACK
                        val translation = model.translationData.value ?: (0f to 0f)
                        postValue(
                            WallpaperRenderer(
                                surfaceHolder,
                                gif,
                                scaleType,
                                rotation,
                                backgroundColor,
                                translation
                            )
                        )
                    }
                }
            }
        }
        addSource(model.scaleTypeData) { scaleType ->
            (value as? WallpaperRenderer)?.setScaleType(scaleType, animated)
        }
        addSource(model.backgroundColorData) { backgroundColor ->
            (value as? WallpaperRenderer)?.setBackgroundColor(backgroundColor)
        }
        addSource(model.rotationData) { rotation ->
            (value as? WallpaperRenderer)?.setRotation(rotation, animated)
        }
        if (isService) {
            addSource(model.translationData) { (translateX, translateY) ->
                (value as? WallpaperRenderer)?.setTranslate(translateX, translateY)
            }
        } else {
            addSource(model.postTranslationData) { (translateX, translateY) ->
                (value as? WallpaperRenderer)?.postTranslate(translateX, translateY)
            }
        }
    }

    override fun setValue(value: Renderer?) {
        val previousRenderer = getValue()
        super.setValue(value)

        (previousRenderer as? WallpaperRenderer)?.recycle()
    }
}
