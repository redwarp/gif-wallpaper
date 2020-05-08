package net.redwarp.gifwallpaper

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Gif private constructor(gif: Any) {
    private constructor(animatedImageDrawable: AnimatedImageDrawable) : this(animatedImageDrawable as Any)
    private constructor(gifDrawable: GifDrawable) : this(gifDrawable as Any)

    val animatable: Animatable = gif as Animatable
    val drawable: Drawable = gif as Drawable

    init {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    }

    fun cleanup() {
        drawable.callback = null
        animatable.stop()
    }

    companion object {
        suspend fun loadGif(context: Context, uri: Uri): Gif? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Gif(getAnimatedImageDrawable(context, uri) ?: return null)
            } else {
                Gif(getGifDrawable(context, uri) ?: return null)
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private suspend fun getAnimatedImageDrawable(
            context: Context,
            uri: Uri
        ): AnimatedImageDrawable? {
            return withContext(Dispatchers.IO) {
                ImageDecoder.decodeDrawable(
                    ImageDecoder.createSource(context.contentResolver, uri)
                ) as? AnimatedImageDrawable
            }
        }

        private suspend fun getGifDrawable(context: Context, uri: Uri): GifDrawable? {
            return withContext(Dispatchers.IO) {
                try {
                    Glide.with(context).asGif()
                        .load(uri).submit().get()
                } catch (exception: Exception) {
                    null
                }
            }
        }
    }
}
