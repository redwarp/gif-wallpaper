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
package net.redwarp.gifwallpaper.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.renderer.GifWrapperDrawable
import net.redwarp.gifwallpaper.ui.rgbToColor
import net.redwarp.gifwallpaper.ui.setup.ColorPalette
import net.redwarp.gifwallpaper.ui.setup.SetupModel

class FakeSetupModel(private val context: Context) : SetupModel {
    override val displayDarkIcons: Flow<Boolean>
        get() = flowOf(true)
    override val colorFlow: Flow<ColorPalette>
        get() = flowOf(
            ColorPalette(
                Color.Green,
                listOf(
                    0xf07880.rgbToColor(),
                    0xe82838.rgbToColor(),
                    0x000000.rgbToColor(),
                    0xe8e8e8.rgbToColor(),
                    0x000000.rgbToColor(),
                    0x000000.rgbToColor(),
                    0xffffff.rgbToColor()
                )
            )
        )
    override val backgroundColorFlow: Flow<Color>
        get() = flowOf(0xffffff.rgbToColor())
    override val hasColorFlow: Flow<Boolean>
        get() = flowOf(true)
    override val drawables: Flow<Drawable>
        get() {
            return flowOf(
                GifWrapperDrawable(
                    drawable =
                    requireNotNull(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.rocket
                        )
                    ),
                    backgroundColor = 0xffffffff.toInt()
                )
            )
        }
    override val isWallpaperSet: Flow<Boolean>
        get() = flowOf(true)
    override val hasSettings: Boolean = true

    override suspend fun setBackgroundColor(color: Color) {
    }

    override suspend fun resetTranslate() {
    }

    override suspend fun postTranslate(translateX: Float, translateY: Float) {
    }

    override suspend fun loadNewGif(context: Context, uri: Uri) {
    }

    override suspend fun clearGif() {
    }

    override suspend fun setNextScale() {
    }

    override suspend fun setNextRotation() {
    }
}
