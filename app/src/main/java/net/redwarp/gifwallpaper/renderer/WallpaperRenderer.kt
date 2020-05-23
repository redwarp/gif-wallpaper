/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.renderer

import android.animation.ValueAnimator
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
import net.redwarp.gifwallpaper.Gif
import net.redwarp.gifwallpaper.util.MatrixEvaluator
import net.redwarp.gifwallpaper.util.setCenterCropRectInRect
import net.redwarp.gifwallpaper.util.setCenterRectInRect

private const val MESSAGE_DRAW = 1

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
        invalidate()
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
        val coordinates = floatArrayOf(gifRect.centerX(), gifRect.centerY())
        matrix.mapPoints(coordinates)
        matrix.postRotate(rotation.angle, coordinates[0], coordinates[1])

        if (scaleType == ScaleType.FIT_CENTER && (rotation == Rotation.EAST || rotation == Rotation.WEST)) {
            val scale = gifRect.width() / gifRect.height()
            matrix.postScale(scale, scale, coordinates[0], coordinates[1])
        }
    }

    override fun invalidate() {
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
        invalidate()
    }

    override fun onDestroy() {
        onPause()
        holder = null
    }

    private fun draw() {
        holder?.let { holder ->
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            }

            draw(canvas, gif)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun draw(canvas: Canvas, gif: Gif) {
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

        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_DRAW) {
                wallpaperRenderer.draw()
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
