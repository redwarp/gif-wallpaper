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
package net.redwarp.gifwallpaper.drawable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifBitmapProvider
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyGifDrawable(
    private val gifDecoder: StandardGifDecoder,
    private val bitmapProvider: GifBitmapProvider
) : Drawable(), Animatable {

    private var currentFrame: Bitmap?

    init {
        gifDecoder.resetFrameIndex()
        gifDecoder.advance()
        currentFrame = gifDecoder.nextFrame
    }

    private var nextFrame: Bitmap? = null
    private var isRunning: Boolean = false
    private var loopJob: Job? = null
    private val width = gifDecoder.width
    private val height = gifDecoder.height
    private val bitmapPaint = Paint().apply {
        isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        currentFrame?.let { bitmap ->
            canvas.drawBitmap(bitmap, null, bounds, bitmapPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
    }

    override fun getIntrinsicWidth(): Int = width

    override fun getIntrinsicHeight(): Int = height

    override fun isRunning(): Boolean {
        return isRunning
    }

    override fun start() {
        if (isRunning) return // already running

        isRunning = true

        invalidateSelf()

        loopJob = CoroutineScope(Dispatchers.IO).launch {
            while (isRunning) {
                val frameDelay = gifDecoder.nextDelay.toLong()
                Log.d("MyGifDrawable", "delay: $frameDelay")
                nextFrame = gifDecoder.nextFrame
                delay(frameDelay)
                currentFrame?.let(bitmapProvider::release)
                currentFrame = nextFrame
                invalidateSelf()
                gifDecoder.advance()
            }
        }
    }

    override fun stop() {
        isRunning = false
        loopJob?.cancel()
    }

    fun recycle() {
        currentFrame?.let(bitmapProvider::release)
        nextFrame?.let(bitmapProvider::release)
    }

    companion object {
        fun decode(context: Context, byteBuffer: ByteBuffer): MyGifDrawable {
            val bitmapProvider = GifBitmapProvider(Glide.get(context).bitmapPool)
            val gifDecoder = StandardGifDecoder(bitmapProvider)

            gifDecoder.read(byteBuffer.array())

            return MyGifDrawable(gifDecoder, bitmapProvider)
        }
    }
}
