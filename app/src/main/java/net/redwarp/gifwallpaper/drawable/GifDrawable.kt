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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.animation.AnimationUtils
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GifDrawable private constructor(
    private val gifDecoder: StandardGifDecoder,
    private val bitmapProvider: SimpleBitmapProvider
) : Drawable(), Animatable {

    private var currentFrame: Bitmap?

    init {
        gifDecoder.resetFrameIndex()
        gifDecoder.advance()
        currentFrame = gifDecoder.nextFrame
        gifDecoder.advance()
    }

    private var nextFrame: Bitmap? = null
    private var isRunning: Boolean = false
    private var isRecycled: Boolean = false
    private var loopJob: Job? = null
    private val width = gifDecoder.width
    private val height = gifDecoder.height
    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }

    override fun draw(canvas: Canvas) {
        if (isRecycled) return
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
        if (isRecycled) return

        isRunning = true

        delayedRunnable.run()
    }

    private fun prepareNextFrame() {
        val frameDelay = gifDecoder.nextDelay.toLong()
        val elapsedTime = measureElapsedRealtime {
            nextFrame = gifDecoder.nextFrame
        }
        val delay = (frameDelay - elapsedTime).coerceIn(0L, frameDelay)
        gifDecoder.advance()
        scheduleSelf(delayedRunnable, AnimationUtils.currentAnimationTimeMillis() + delay)
    }

    private val delayedRunnable = Runnable {
        currentFrame?.let(bitmapProvider::release)
        currentFrame = nextFrame

        invalidateSelf()

        if (isRunning && !isRecycled) {
            CoroutineScope(Dispatchers.Default).launch {
                prepareNextFrame()
            }
        }
    }

    override fun stop() {
        isRunning = false
        unscheduleSelf(delayedRunnable)
        loopJob?.cancel()
    }

    @Synchronized
    fun recycle() {
        isRecycled = true

        stop()
        currentFrame?.let(bitmapProvider::release)
        nextFrame?.let(bitmapProvider::release)
        bitmapProvider.flush()
    }

    private inline fun measureElapsedRealtime(crossinline block: () -> Unit): Long {
        val startTime = SystemClock.elapsedRealtime()
        block()
        return SystemClock.elapsedRealtime() - startTime
    }

    companion object {
        fun decode(byteArray: ByteArray): GifDrawable {
            val bitmapProvider = SimpleBitmapProvider()
            val gifDecoder = StandardGifDecoder(bitmapProvider).apply {
                read(byteArray)
            }

            return GifDrawable(gifDecoder, bitmapProvider)
        }
    }
}
