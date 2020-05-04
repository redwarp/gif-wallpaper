package net.redwarp.gifwallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import kotlin.properties.Delegates

class GifDrawer(private val holder: SurfaceHolder) : SurfaceHolder.Callback2 {

    private var dimensions = Rect(0, 0, 1, 1)
    private val emptyPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    private var isCreated = false
    private val handler: Handler = Handler(Looper.getMainLooper())

    var gif by Delegates.observable<Gif?>(null) { property, oldValue, newValue ->
        oldValue?.cleanup()

        invalidate()
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        draw()
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
    }

    private fun invalidate() {
        handler.post { draw() }
    }

    private fun draw() {
        if (!isCreated) {
            return
        }

        val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.lockHardwareCanvas()
        } else {
            holder.lockCanvas()
        }

        draw(canvas)

        holder.unlockCanvasAndPost(canvas)
    }

    private fun draw(canvas: Canvas) {
        val gif = gif
        if (gif == null) {
            drawEmptySurface(canvas)
        } else {
            drawGif(canvas, gif)
        }
    }

    private fun drawGif(canvas: Canvas, gif: Gif) {
        canvas.drawARGB(255, 255, 0, 0)
    }

    private fun drawEmptySurface(canvas: Canvas) {
        canvas.drawRect(dimensions, emptyPaint)
    }
}
