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
import android.net.Uri
import android.os.SystemClock
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class GifDrawable private constructor(
    private val gifDecoder: StandardGifDecoder,
    private val bitmapProvider: SimpleBitmapProvider
) : Drawable(), Animatable {

    private var currentFrame: Bitmap?

    init {
        gifDecoder.resetFrameIndex()
        gifDecoder.advance()
        currentFrame = gifDecoder.nextFrame
    }

    private var isRunning: Boolean = false
    private var isRecycled: Boolean = false
    private var loopJob: Job? = null
    private val width = gifDecoder.width
    private val height = gifDecoder.height
    private val bitmapPaint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }

    init {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
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

        val previousJob = loopJob
        loopJob = CoroutineScope(Dispatchers.Default).launch {
            previousJob?.cancelAndJoin()
            animationLoop()
        }
    }

    override fun stop() {
        isRunning = false
        loopJob?.cancel()
    }

    @Synchronized
    fun recycle() {
        isRecycled = true

        stop()
        currentFrame?.let(bitmapProvider::release)
        bitmapProvider.flush()
    }

    private suspend fun animationLoop() {
        while (true) {
            coroutineContext.ensureActive()
            val frameDelay = gifDecoder.nextDelay.toLong()
            gifDecoder.advance()

            val startTime = SystemClock.elapsedRealtime()
            val nextFrame = gifDecoder.nextFrame
            val elapsedTime = SystemClock.elapsedRealtime() - startTime

            val delay = (frameDelay - elapsedTime).coerceIn(0L, frameDelay)

            coroutineContext.ensureActive()
            delay(delay)

            coroutineContext.ensureActive()
            currentFrame?.let(bitmapProvider::release)
            currentFrame = nextFrame
            invalidateSelf()
        }
    }

    companion object {
        private fun decode(byteArray: ByteArray): GifDrawable {
            val bitmapProvider = SimpleBitmapProvider()
            val gifDecoder = StandardGifDecoder(bitmapProvider).apply {
                read(byteArray)
            }

            return GifDrawable(gifDecoder, bitmapProvider)
        }

        fun getGifDrawable(context: Context, uri: Uri): GifDrawable? {
            return context.contentResolver.openInputStream(uri)?.use {
                return decode(it.readBytes())
            }
        }
    }
}
