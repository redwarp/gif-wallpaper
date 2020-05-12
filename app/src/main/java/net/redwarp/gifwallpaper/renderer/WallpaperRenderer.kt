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
    backgroundColor: Int = Color.BLACK
) : Renderer {

    private val canvasRect = RectF(0f, 0f, 1f, 1f)
    private val gifRect = RectF(0f, 0f, 0f, 0f)
    private val handler: Handler =
        DrawHandler(this)
    private var matrixAnimator: ValueAnimator? = null

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
            transformMatrix(scaleType)
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

        handler.removeMessages(MESSAGE_DRAW)
    }

    private fun computeMatrix(
        matrix: Matrix,
        scaleType: ScaleType,
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
    }

    override fun invalidate() {
        matrixAnimator?.cancel()
        handler.removeMessages(MESSAGE_DRAW)
        computeMatrix(matrix, scaleType, canvasRect, gifRect)

        gif.drawable.callback = drawableCallback
        gif.animatable.start()

        handler.sendMessage(getDrawMessage())
    }

    override fun onCreate(surfaceHolder: SurfaceHolder) {
        holder = surfaceHolder
        invalidate()
    }

    override fun onDestroy() {
        holder = null
        onPause()
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

    private fun getDrawMessage(): Message {
        return Message.obtain(
            handler,
            MESSAGE_DRAW
        )
    }

    private fun transformMatrix(scaleType: ScaleType) {
        val targetMatrix = Matrix().also {
            computeMatrix(it, scaleType, canvasRect, gifRect)
        }
        val sourceMatrix = Matrix(matrix)

        matrixAnimator?.cancel()
        matrixAnimator = ValueAnimator.ofObject(
            MatrixEvaluator(matrix),
            sourceMatrix,
            targetMatrix
        ).apply {
            addUpdateListener { _ ->
                draw()
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
            // Nothing to do.
            handler.removeMessages(MESSAGE_DRAW)
        }

        override fun invalidateDrawable(who: Drawable) {
            handler.sendMessage(getDrawMessage())
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            handler.removeMessages(MESSAGE_DRAW)
            handler.sendMessageAtTime(getDrawMessage(), `when`)
        }
    }

    private class DrawHandler(private val wallpaperRenderer: WallpaperRenderer) :
        Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_DRAW) {
                wallpaperRenderer.draw()
            }
        }
    }

    enum class ScaleType {
        FIT_CENTER, FIT_END, FIT_START, FIT_XY, CENTER, CENTER_CROP;
    }
}
