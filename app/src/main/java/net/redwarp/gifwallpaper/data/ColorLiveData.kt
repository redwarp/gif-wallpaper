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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.alpha
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.drawable.GifDrawable

class ColorLiveData(val context: Context, wallpaperLiveData: LiveData<WallpaperStatus>) :
    MediatorLiveData<ColorInfo>() {
    init {
        postValue(NotSet)
        addSource(wallpaperLiveData) { status ->
            if (status is WallpaperStatus.Wallpaper) {
                extractColorScheme(status)
            } else {
                postValue(NotSet)
            }
        }
    }

    private fun extractColorScheme(wallpaper: WallpaperStatus.Wallpaper) {
        postValue(NotSet)
        CoroutineScope(Dispatchers.IO).launch {
            val gif = GifDrawable.getGifDrawable(context, wallpaper.uri) ?: return@launch

            val defaultColor = calculateDefaultBackgroundColor(gif)
            val palette = calculatePalette(gif)

            postValue(ColorScheme(defaultColor, palette))
        }
    }

    private fun calculatePalette(drawable: Drawable): Palette {
        val sample = Bitmap.createBitmap(
            drawable.intrinsicWidth / 2,
            drawable.intrinsicHeight / 2,
            Bitmap.Config.ARGB_8888
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
