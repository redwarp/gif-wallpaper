package net.redwarp.gifwallpaper

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
import android.widget.ImageView
import androidx.core.graphics.withMatrix
import kotlin.properties.Delegates

private const val MESSAGE_DRAW = 1

class GifDrawer(private val holder: SurfaceHolder) : SurfaceHolder.Callback2 {
    var scaleType: ImageView.ScaleType = ImageView.ScaleType.FIT_CENTER

    private val canvasRect = RectF(0f, 0f, 1f, 1f)
    private val gifRect = RectF(0f, 0f, 0f, 0f)
    private val emptyPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private var isCreated = false
    private val handler: Handler = DrawHandler(this)

    var gif: Gif? by Delegates.observable(null as Gif?) { _, oldValue, newValue ->
        oldValue?.cleanup()

        newValue?.drawable?.callback = drawableCallback
        newValue?.animatable?.start()
        if (newValue == null) {
            gifRect.set(0f, 0f, 0f, 0f)
        } else {
            gifRect.right = newValue.drawable.intrinsicWidth.toFloat()
            gifRect.bottom = newValue.drawable.intrinsicHeight.toFloat()
        }
        invalidate()
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
        handler.removeMessages(MESSAGE_DRAW)
    }

    private fun computeMatrix(
        matrix: Matrix,
        scaleType: ImageView.ScaleType,
        canvasRect: RectF,
        gifRect: RectF
    ) {
        when (scaleType) {
            ImageView.ScaleType.FIT_CENTER ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.CENTER)
        }
    }

    private fun computeMatrix(matrix: Matrix, canvasRect: RectF, gifRect: RectF) {
        val canvasRatio = canvasRect.width() / canvasRect.height()
        val gifRatio = gifRect.width() / gifRect.height()


        with(matrix) {
            reset()
            if (gifRatio > canvasRatio) {
                val scale = canvasRect.width() / gifRect.width()
                val offset = (canvasRect.height() - gifRect.height() * scale) * 0.5f

                postScale(scale, scale)
                postTranslate(0f, offset)
            } else {

                val scale = canvasRect.height() / gifRect.height()
                val offset = (canvasRect.width() - gifRect.width() * scale) * 0.5f

                postScale(scale, scale)
                postTranslate(offset, 0f)
            }
        }
    }

    private fun invalidate() {
        handler.removeMessages(MESSAGE_DRAW)
        computeMatrix(matrix, scaleType, canvasRect, gifRect)
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
        canvas.drawRect(canvasRect, gif.backgroundPaint)

        canvas.withMatrix(matrix) {
            gif.drawable.draw(this)
        }
        // canvas.withSave {
        //     // Adjust size and position so that
        //     // the image looks good on your screen
        //     val scale =
        //         width.toFloat() / gif.drawable.intrinsicWidth.toFloat()
        //     val offset =
        //         (height / scale - gif.drawable.intrinsicHeight) * 0.5f
        //
        //     scale(scale, scale)
        //     translate(0f, offset)
        //     gif.drawable.draw(this)
        // }
    }

    private fun drawEmptySurface(canvas: Canvas) {
        canvas.drawRect(canvasRect, emptyPaint)
    }

    private fun getDrawMessage(gif: Gif?): Message {
        return Message.obtain(handler, MESSAGE_DRAW, gif)
    }

    private val drawableCallback = object : Drawable.Callback {
        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            // Nothing to do.
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
}
