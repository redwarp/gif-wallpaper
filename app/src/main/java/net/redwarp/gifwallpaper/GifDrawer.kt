package net.redwarp.gifwallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.SurfaceHolder
import androidx.core.graphics.withSave
import kotlin.properties.Delegates

private const val MESSAGE_DRAW = 1

class GifDrawer(private val holder: SurfaceHolder) : SurfaceHolder.Callback2 {

    private var dimensions = Rect(0, 0, 1, 1)
    private val emptyPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private var isCreated = false
    private val handler: Handler = DrawHandler(this)

    var gif: Gif? by Delegates.observable<Gif?>(null) { property, oldValue, newValue ->
        oldValue?.cleanup()

        newValue?.drawable?.callback = drawableCallback
        newValue?.animatable?.start()
        invalidate()
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        invalidate()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        dimensions.right = width
        dimensions.bottom = height

        surfaceRedrawNeeded(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isCreated = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isCreated = true
        handler.removeMessages(MESSAGE_DRAW)
    }

    private fun invalidate() {
        handler.removeMessages(MESSAGE_DRAW)
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
        canvas.drawRect(dimensions, gif.backgroundPaint)

        canvas.withSave {
            // Adjust size and position so that
            // the image looks good on your screen
            val scale =
                width.toFloat() / gif.drawable.intrinsicWidth.toFloat()
            val offset =
                (height / scale - gif.drawable.intrinsicHeight) * 0.5f

            scale(scale, scale)
            translate(0f, offset)
            gif.drawable.draw(this)
        }
    }

    private fun drawEmptySurface(canvas: Canvas) {
        canvas.drawRect(dimensions, emptyPaint)
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
