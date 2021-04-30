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

import android.content.Context
import android.graphics.drawable.Drawable
import app.redwarp.gif.android.GifDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.TranslationEvent
import net.redwarp.gifwallpaper.data.WallpaperStatus

fun CoroutineScope.drawableFlow(
    context: Context,
    flowBasedModel: FlowBasedModel,
    unsetText: String,
    animated: Boolean,
    isService: Boolean,
): Flow<Drawable> {
    val drawableFlow = flowBasedModel.wallpaperStatusFlow.map { status ->
        val wallpaper = when (status) {
            WallpaperStatus.NotSet -> TextDrawable(context, unsetText)

            WallpaperStatus.Loading ->
                TextDrawable(context, context.getString(R.string.loading))
            is WallpaperStatus.Wallpaper -> {
                val gif = GifDrawable(status.gifDescriptor)
                val shouldPlay = !isService || flowBasedModel.shouldPlay.first()
                if (shouldPlay) {
                    gif.start()
                } else {
                    gif.stop()
                }
                val scaleType = flowBasedModel.scaleTypeFlow.first()
                val rotation = flowBasedModel.rotationFlow.first()
                val backgroundColor = flowBasedModel.backgroundColorFlow.first()
                val translation = flowBasedModel.translationFlow.first()
                val wrapper = GifWrapperDrawable(
                    gif,
                    scaleType,
                    rotation,
                    translation.x to translation.y
                )
                wrapper.setBackgroundColor(backgroundColor)
                wrapper
            }
        }

        wallpaper
    }.shareIn(scope = this, started = SharingStarted.WhileSubscribed(), replay = 1)

    setupWallpaperUpdate(
        flowBasedModel = flowBasedModel,
        animated = animated,
        isService = isService,
        drawableFlow = drawableFlow
    )

    return drawableFlow
}

private fun CoroutineScope.setupWallpaperUpdate(
    flowBasedModel: FlowBasedModel,
    animated: Boolean,
    isService: Boolean,
    drawableFlow: Flow<Drawable>
) {
    flowBasedModel.backgroundColorFlow.onEach { backgroundColor ->
        val value = drawableFlow.first() as? GifWrapperDrawable
        value?.setBackgroundColor(backgroundColor)
    }.launchIn(this)
    flowBasedModel.scaleTypeFlow.onEach { scaleType ->
        val value = drawableFlow.first() as? GifWrapperDrawable
        value?.setScaledType(scaleType, animated)
    }.launchIn(this)
    flowBasedModel.rotationFlow.onEach { rotation ->
        val value = drawableFlow.first() as? GifWrapperDrawable
        value?.setRotation(rotation, animated)
    }.launchIn(this)

    if (isService) {
        flowBasedModel.shouldPlay.onEach { shouldPlay ->
            val value: GifWrapperDrawable =
                (drawableFlow.first() as? GifWrapperDrawable) ?: return@onEach
            if (shouldPlay) {
                value.start()
            } else {
                value.stop()
            }
        }.launchIn(this)
        flowBasedModel.translationFlow.onEach { translation ->
            val value = drawableFlow.first() as? GifWrapperDrawable
            value?.setTranslate(
                translation.x,
                translation.y,
                animated
            )
        }.launchIn(this)
    } else {
        flowBasedModel.translationEventFlow.onEach { event ->
            val drawable = drawableFlow.first() as? GifWrapperDrawable ?: return@onEach
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
        }.launchIn(this)
    }
}
