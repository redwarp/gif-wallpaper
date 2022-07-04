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
package net.redwarp.gifwallpaper.ui.setup

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.redwarp.gifwallpaper.data.ColorScheme
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.NotSet
import net.redwarp.gifwallpaper.data.WallpaperStatus
import net.redwarp.gifwallpaper.renderer.DrawableProvider
import net.redwarp.gifwallpaper.ui.rgbToColor
import net.redwarp.gifwallpaper.util.isDark

interface SetupModel {
    val displayDarkIcons: Flow<Boolean>
    val colorFlow: Flow<ColorPalette>
    val hasColorFlow: Flow<Boolean>
    val drawables: Flow<Drawable>
    val isWallpaperSet: Flow<Boolean>

    suspend fun setBackgroundColor(color: Color)
    suspend fun resetTranslate()
    suspend fun postTranslate(translateX: Float, translateY: Float)
    suspend fun loadNewGif(context: Context, uri: Uri)
    suspend fun clearGif()
    suspend fun setNextScale()
    suspend fun setNextRotation()
}

class SetupModelImpl(
    private val flowBasedModel: FlowBasedModel,
    private val drawableProvider: DrawableProvider
) : SetupModel {
    override val displayDarkIcons: Flow<Boolean>
        get() = flowBasedModel.backgroundColorFlow.map { !it.isDark() }

    override val colorFlow: Flow<ColorPalette>
        get() = flowBasedModel.colorInfoFlow.map { colorInfo ->
            when (colorInfo) {
                is ColorScheme -> {
                    colorInfo.toColorPalette()
                }
                NotSet -> ColorPalette(Color.Black, emptyList())
            }
        }

    override val hasColorFlow: Flow<Boolean> get() = flowBasedModel.colorInfoFlow.map { it is ColorScheme }

    override val drawables: Flow<Drawable>
        get() = drawableProvider.drawables
    override val isWallpaperSet: Flow<Boolean>
        get() = flowBasedModel.wallpaperStatusFlow.map { it is WallpaperStatus.Wallpaper }

    override suspend fun setBackgroundColor(color: Color) {
        flowBasedModel.setBackgroundColor(color.toArgb())
    }

    override suspend fun resetTranslate() {
        flowBasedModel.resetTranslate()
    }

    override suspend fun postTranslate(translateX: Float, translateY: Float) {
        flowBasedModel.postTranslate(translateX, translateY)
    }

    override suspend fun loadNewGif(context: Context, uri: Uri) {
        flowBasedModel.loadNewGif(context, uri)
        flowBasedModel.resetTranslate()
    }

    override suspend fun clearGif() {
        flowBasedModel.clearGif()
    }

    override suspend fun setNextScale() {
        flowBasedModel.setNextScale()
    }

    override suspend fun setNextRotation() {
        flowBasedModel.setNextRotation()
    }
}

data class ColorPalette(val defaultColor: Color, val colors: List<Color>)

private fun ColorScheme.toColorPalette(): ColorPalette =
    ColorPalette(
        this.defaultColor.rgbToColor(),
        palette.targets.map { target ->
            palette.getColorForTarget(target, android.graphics.Color.BLACK).rgbToColor()
        }
    )
