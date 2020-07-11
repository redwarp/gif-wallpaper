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

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.SurfaceHolder
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.graphics.withMatrix
import kotlin.math.max
import net.redwarp.gifwallpaper.Gif
import net.redwarp.gifwallpaper.util.MatrixEvaluator
import net.redwarp.gifwallpaper.util.setCenterCropRectInRect
import net.redwarp.gifwallpaper.util.setCenterRectInRect

private const val MESSAGE_DRAW = 1
private const val MESSAGE_INVALIDATE = 2

class WallpaperRenderer(
    private var holder: SurfaceHolder?,
    private val gif: Gif,
    private var scaleType: ScaleType = ScaleType.FIT_CENTER,
    private var rotation: Rotation = Rotation.NORTH,
    backgroundColor: Int = Color.BLACK
) : Renderer {

    private val canvasRect = RectF(0f, 0f, 1f, 1f)
    private val gifRect = RectF(0f, 0f, 0f, 0f)
    private var matrixAnimator: ValueAnimator? = null
    private var handler: DrawHandler? = null
    private val workArray = FloatArray(2)
    private var isRecycled = false

    init {
        gifRect.right = gif.drawable.intrinsicWidth.toFloat()
        gifRect.bottom = gif.drawable.intrinsicHeight.toFloat()
    }

    private val backgroundPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = backgroundColor
    }

    private val matrix = Matrix()

    override fun setSize(width: Float, height: Float) {
        canvasRect.right = width
        canvasRect.bottom = height
        invalidate()
    }

    fun setScaleType(scaleType: ScaleType, animated: Boolean) {
        this.scaleType = scaleType
        if (animated) {
            transformMatrix(scaleType, rotation)
        } else {
            invalidate()
        }
    }

    fun setRotation(rotation: Rotation, animated: Boolean) {
        this.rotation = rotation
        if (animated) {
            transformMatrix(scaleType, rotation)
        } else {
            invalidate()
        }
    }

    fun setBackgroundColor(backgroundColor: Int) {
        backgroundPaint.color = backgroundColor
        invalidate()
    }

    override fun onResume() {
        handler?.postInvalidate()
    }

    override fun onPause() {
        matrixAnimator?.cancel()

        gif.drawable.callback = null
        gif.animatable.stop()

        handler?.cancelDraw()
    }

    private fun computeMatrix(
        matrix: Matrix,
        scaleType: ScaleType,
        rotation: Rotation,
        canvasRect: RectF,
        gifRect: RectF
    ) {
        when (scaleType) {
            ScaleType.FIT_CENTER ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.CENTER)
            ScaleType.FIT_END ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.END)
            ScaleType.FIT_START ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.START)
            ScaleType.FIT_XY ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.FILL)
            ScaleType.CENTER ->
                matrix.setCenterRectInRect(gifRect, canvasRect)
            ScaleType.CENTER_CROP ->
                matrix.setCenterCropRectInRect(gifRect, canvasRect)
        }
        workArray[0] = gifRect.centerX()
        workArray[1] = gifRect.centerY()
        matrix.mapPoints(workArray)
        matrix.postRotate(rotation.angle, workArray[0], workArray[1])

        if (rotation == Rotation.EAST || rotation == Rotation.WEST) {
            when (scaleType) {
                ScaleType.FIT_CENTER -> {
                    val scale = gifRect.width() / gifRect.height()
                    matrix.postScale(scale, scale, workArray[0], workArray[1])
                }
                ScaleType.FIT_XY -> {
                    val scale = canvasRect.width() / canvasRect.height()
                    matrix.postScale(scale, 1f / scale, workArray[0], workArray[1])
                }
                else -> Unit
            }
        }
    }

    override fun invalidate() {
        if (isRecycled) return

        matrixAnimator?.cancel()
        handler?.cancelDraw()
        computeMatrix(matrix, scaleType, rotation, canvasRect, gifRect)

        gif.drawable.callback = drawableCallback
        gif.animatable.start()

        handler?.requestDraw()
    }

    override fun onCreate(surfaceHolder: SurfaceHolder, looper: Looper) {
        holder = surfaceHolder
        handler = DrawHandler(looper, this)
        handler?.postInvalidate()
    }

    override fun onDestroy() {
        onPause()
        holder = null
    }

    fun recycle() {
        isRecycled = true
        gif.recycle()
    }

    fun createMiniature(): Bitmap {
        val miniCanvasRect =
            RectF(0f, 0f, max(1f, canvasRect.right / 4f), max(1f, canvasRect.bottom / 4f))

        val bitmap = Bitmap.createBitmap(
            miniCanvasRect.width().toInt(),
            miniCanvasRect.height().toInt(),
            Bitmap.Config.ARGB_8888
        )
        val matrix = Matrix()
        computeMatrix(matrix, scaleType, rotation, miniCanvasRect, gifRect)

        val canvas = Canvas(bitmap)
        draw(canvas, miniCanvasRect, matrix, gif)

        return bitmap
    }

    private fun draw() {
        if (isRecycled) return
        holder?.let { holder ->
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            }

            draw(canvas, canvasRect, matrix, gif)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun draw(canvas: Canvas, canvasRect: RectF, matrix: Matrix, gif: Gif) {
        canvas.clipRect(canvasRect)
        canvas.drawRect(canvasRect, backgroundPaint)

        canvas.withMatrix(matrix) {
            gif.drawable.draw(this)
        }
    }

    private fun transformMatrix(scaleType: ScaleType, rotation: Rotation) {
        val targetMatrix = Matrix().also {
            computeMatrix(it, scaleType, rotation, canvasRect, gifRect)
        }
        val sourceMatrix = Matrix(matrix)

        matrixAnimator?.cancel()
        matrixAnimator = ValueAnimator.ofObject(
            MatrixEvaluator(matrix),
            sourceMatrix,
            targetMatrix
        ).apply {
            addUpdateListener { _ ->
                handler?.requestDraw()
            }
            interpolator = AccelerateDecelerateInterpolator()
            duration = 500
            doOnStart {
                gif.drawable.callback = null
            }
            doOnEnd {
                invalidate()
            }
            start()
        }
    }

    private val drawableCallback = object : Drawable.Callback {
        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            handler?.cancelDraw()
        }

        override fun invalidateDrawable(who: Drawable) {
            handler?.requestDraw()
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            handler?.requestDrawAtTime(`when`)
        }
    }

    private class DrawHandler(looper: Looper, private val wallpaperRenderer: WallpaperRenderer) :
        Handler(looper) {

        fun cancelDraw() {
            removeMessages(MESSAGE_INVALIDATE)
            removeMessages(MESSAGE_DRAW)
        }

        fun requestDraw() {
            if (hasMessages(MESSAGE_DRAW)) return
            sendMessage(Message.obtain(this, MESSAGE_DRAW))
        }

        fun requestDrawAtTime(time: Long) {
            removeMessages(MESSAGE_DRAW)
            sendMessageAtTime(Message.obtain(this, MESSAGE_DRAW), time)
        }

        fun postInvalidate() {
            if (hasMessages(MESSAGE_INVALIDATE)) return
            sendMessage(Message.obtain(this, MESSAGE_INVALIDATE))
        }

        override fun handleMessage(msg: Message) {
            if (wallpaperRenderer.isRecycled) return
            when (msg.what) {
                MESSAGE_DRAW -> wallpaperRenderer.draw()
                MESSAGE_INVALIDATE -> wallpaperRenderer.invalidate()
            }
        }
    }

    enum class ScaleType {
        FIT_CENTER, FIT_END, FIT_START, FIT_XY, CENTER, CENTER_CROP;
    }

    enum class Rotation(val angle: Float) {
        NORTH(0f), EAST(90f), SOUTH(180f), WEST(270f)
    }
}
