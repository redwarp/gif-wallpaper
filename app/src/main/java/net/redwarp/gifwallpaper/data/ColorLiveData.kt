package net.redwarp.gifwallpaper.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.Gif

class ColorLiveData(val context: Context, wallpaperLiveData: LiveData<WallpaperStatus>) :
    MediatorLiveData<ColorInfo>() {
    init {
        addSource(wallpaperLiveData) { status ->
            if (status is WallpaperStatus.Wallpaper) {
                extractColorScheme(status)
            }
        }
    }

    private fun extractColorScheme(wallpaper: WallpaperStatus.Wallpaper) {
        postValue(Calculating)
        CoroutineScope(Dispatchers.IO).launch {
            val gif = Gif.loadGif(context, wallpaper.uri) ?: return@launch

            val defaultColor = calculateDefaultBackgroundColor(gif.drawable)
            val palette = calculatePalette(gif.drawable)

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
        return color
    }
}
