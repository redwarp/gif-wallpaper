/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.renderer

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.view.SurfaceHolder
import net.redwarp.gifwallpaper.R

class LoadingRenderer(context: Context, private var holder: SurfaceHolder?) : Renderer {
    private val emptyPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.SANS_SERIF
        textSize = context.resources.getDimension(R.dimen.title)
    }
    private val promptText = context.getText(R.string.loading).toString()
    private val canvasRect = RectF(0f, 0f, 1f, 1f)
    private val handler = Handler(Looper.getMainLooper())

    override fun invalidate() {
        handler.post {
            draw()
        }
    }

    override fun setSize(width: Float, height: Float) {
        canvasRect.right = width
        canvasRect.bottom = height
    }

    private fun draw() {
        holder?.let { holder ->
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            }

            canvas.drawRect(canvasRect, emptyPaint)
            canvas.drawText(
                promptText,
                canvasRect.centerX(),
                canvasRect.centerY(),
                textPaint
            )

            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
        holder = null
    }

    override fun onCreate(surfaceHolder: SurfaceHolder) {
        holder = surfaceHolder
        invalidate()
    }
}
