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
package net.redwarp.gifwallpaper.renderer

import android.content.Context
import android.graphics.drawable.Drawable
import app.redwarp.gif.android.GifDrawable
import app.redwarp.gif.decoder.descriptors.params.LoopCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.TranslationEvent
import net.redwarp.gifwallpaper.data.WallpaperStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface DrawableProvider {
    val drawables: Flow<Drawable>
}

class DrawableMapper private constructor(
    context: Context,
    flowBasedModel: FlowBasedModel,
    scope: CoroutineScope,
    unsetText: String,
    isService: Boolean,
) : DrawableProvider {
    private val _drawableFlow: MutableSharedFlow<Drawable> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val drawables: Flow<Drawable> get() = _drawableFlow

    init {
        val animated = !isService
        collectDrawables(scope, flowBasedModel, context, unsetText, isService)
        scope.launch {
            flowBasedModel.backgroundColorFlow.collect { color ->
                _drawableFlow.onWrappedGif {
                    it.setBackgroundColor(color)
                }
            }
        }
        scope.launch {
            flowBasedModel.rotationFlow.collect { rotation ->
                _drawableFlow.onWrappedGif { it.setRotation(rotation, animated) }
            }
        }
        scope.launch {
            flowBasedModel.scaleTypeFlow.collect { scaleType ->
                _drawableFlow.onWrappedGif { it.setScaledType(scaleType, animated) }
            }
        }

        if (isService) {
            scope.launch {
                flowBasedModel.shouldPlay.collect { shouldPlay ->
                    _drawableFlow.onWrappedGif {
                        it.shouldPlay = shouldPlay
                    }
                }
            }
            scope.launch {
                flowBasedModel.translationFlow.collect { translation ->
                    _drawableFlow.onWrappedGif {
                        it.setTranslate(translation.x, translation.y, animated)
                    }
                }
            }
        } else {
            scope.launch {
                flowBasedModel.translationEventFlow.collect { event ->
                    _drawableFlow.onWrappedGif { drawable ->
                        when (event) {
                            is TranslationEvent.PostTranslate -> {
                                drawable.postTranslate(
                                    event.translateX,
                                    event.translateY
                                )
                            }
                            TranslationEvent.Reset -> {
                                drawable.resetTranslation(animated)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun collectDrawables(
        scope: CoroutineScope,
        flowBasedModel: FlowBasedModel,
        context: Context,
        unsetText: String,
        isService: Boolean
    ) {
        scope.launch {
            flowBasedModel.wallpaperStatusFlow.map { status ->
                when (status) {
                    WallpaperStatus.Loading ->
                        TextDrawable(context, context.getString(R.string.loading))
                    WallpaperStatus.NotSet -> TextDrawable(context, unsetText)
                    is WallpaperStatus.Wallpaper -> {
                        val gif = GifDrawable(status.gifDescriptor)
                        gif.loopCount = LoopCount.Infinite
                        val shouldPlay = !isService || flowBasedModel.shouldPlay.first()
                        val scaleType = flowBasedModel.scaleTypeFlow.first()
                        val rotation = flowBasedModel.rotationFlow.first()
                        val backgroundColor = flowBasedModel.backgroundColorFlow.first()
                        val translation = flowBasedModel.translationFlow.first()
                        val wrapper = GifWrapperDrawable(
                            gif,
                            backgroundColor,
                            scaleType,
                            rotation,
                            translation.x to translation.y,
                            shouldPlay
                        )
                        wrapper
                    }
                }
            }.collect { _drawableFlow.emit(it) }
        }
    }

    companion object {
        fun previewMapper(
            context: Context,
            flowBasedModel: FlowBasedModel,
            scope: CoroutineScope,
        ): DrawableMapper {
            return DrawableMapper(
                context = context,
                flowBasedModel = flowBasedModel,
                scope = scope,
                unsetText = context.getString(R.string.click_the_open_gif_button),
                isService = false,
            )
        }

        fun serviceMapper(
            context: Context,
            flowBasedModel: FlowBasedModel,
            scope: CoroutineScope,
        ): DrawableMapper {
            return DrawableMapper(
                context = context,
                flowBasedModel = flowBasedModel,
                scope = scope,
                unsetText = context.getString(R.string.open_app),
                isService = true,
            )
        }
    }
}

@OptIn(ExperimentalContracts::class)
private suspend inline fun Flow<Drawable>.onWrappedGif(block: (GifWrapperDrawable) -> Unit) {
    contract { callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }
    (firstOrNull() as? GifWrapperDrawable)?.let { block(it) }
}
