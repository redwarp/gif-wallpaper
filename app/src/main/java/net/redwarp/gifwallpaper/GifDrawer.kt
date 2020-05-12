/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper

import android.animation.ValueAnimator
import android.content.Context
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.data.WallpaperStatus
import net.redwarp.gifwallpaper.utils.MatrixEvaluator
import net.redwarp.gifwallpaper.utils.setCenterCropRectInRect
import net.redwarp.gifwallpaper.utils.setCenterRectInRect

private const val MESSAGE_DRAW = 1

class GifDrawer(private val context: Context, private val holder: SurfaceHolder) :
    SurfaceHolder.Callback2, LifecycleObserver {
    private var scaleType: ScaleType = ScaleType.FIT_CENTER

    private val canvasRect = RectF(0f, 0f, 1f, 1f)
    private val gifRect = RectF(0f, 0f, 0f, 0f)
    private val emptyPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private var isCreated = false
    private val handler: Handler = DrawHandler(this)
    private var matrixAnimator: ValueAnimator? = null

    var gif: Gif? = null
        set(value) {
            field?.cleanup()

            if (value == null) {
                gifRect.set(0f, 0f, 0f, 0f)
            } else {
                gifRect.right = value.drawable.intrinsicWidth.toFloat()
                gifRect.bottom = value.drawable.intrinsicHeight.toFloat()
            }
            field = value
            invalidate()
        }

    private val backgroundPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
    }

    private val matrix = Matrix()

    init {
        holder.addCallback(this)
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        invalidate()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        canvasRect.right = width.toFloat()
        canvasRect.bottom = height.toFloat()

        surfaceRedrawNeeded(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isCreated = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isCreated = true
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

    fun setWallpaperStatus(wallpaperStatus: WallpaperStatus) {
        when (wallpaperStatus) {
            WallpaperStatus.NotSet -> {
                gif = null
            }
            is WallpaperStatus.Wallpaper -> {
                CoroutineScope(Dispatchers.Main).launch {
                    gif = Gif.loadGif(context, wallpaperStatus.uri)
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        invalidate()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        matrixAnimator?.cancel()
        gif?.let { gif ->
            gif.drawable.callback = null
            gif.animatable.stop()
        }
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
            else -> Unit
        }
    }

    private fun invalidate() {
        matrixAnimator?.cancel()
        handler.removeMessages(MESSAGE_DRAW)
        computeMatrix(matrix, scaleType, canvasRect, gifRect)

        gif?.drawable?.callback = drawableCallback
        gif?.animatable?.start()

        handler.sendMessage(getDrawMessage(gif))
    }

    private fun draw(gif: Gif?) {
        if (!isCreated || gif != this.gif) {
            return
        }

        val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.lockHardwareCanvas()
        } else {
            holder.lockCanvas()
        }

        draw(canvas, gif)

        holder.unlockCanvasAndPost(canvas)
    }

    private fun draw(canvas: Canvas, gif: Gif?) {
        if (gif == null) {
            drawEmptySurface(canvas)
        } else {
            drawGif(canvas, gif)
        }
    }

    private fun drawGif(canvas: Canvas, gif: Gif) {
        canvas.clipRect(canvasRect)
        canvas.drawRect(canvasRect, backgroundPaint)

        canvas.withMatrix(matrix) {
            gif.drawable.draw(this)
        }
    }

    private fun drawEmptySurface(canvas: Canvas) {
        canvas.drawRect(canvasRect, emptyPaint)
    }

    private fun getDrawMessage(gif: Gif?): Message {
        return Message.obtain(handler, MESSAGE_DRAW, gif)
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
                draw(gif)
            }
            interpolator = AccelerateDecelerateInterpolator()
            duration = 500
            doOnStart {
                gif?.drawable?.callback = null
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
            handler.sendMessage(getDrawMessage(gif))
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            handler.sendMessageAtTime(getDrawMessage(gif), `when`)
        }
    }

    private class DrawHandler(private val gifDrawer: GifDrawer) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_DRAW) {
                val gif = msg.obj as? Gif?

                gifDrawer.draw(gif)
            }
        }
    }

    enum class ScaleType {
        FIT_CENTER, FIT_END, FIT_START, FIT_XY, CENTER, CENTER_CROP;
    }
}
