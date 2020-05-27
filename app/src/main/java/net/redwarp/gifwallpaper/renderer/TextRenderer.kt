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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.SurfaceHolder
import kotlin.math.max
import net.redwarp.gifwallpaper.R

class TextRenderer(
    context: Context,
    private var holder: SurfaceHolder?,
    private val text: String
) : Renderer {
    private val emptyPaint = Paint().apply {
        color = context.getColor(R.color.colorPrimaryDark)
        style = Paint.Style.FILL
    }
    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.SANS_SERIF
        textSize = context.resources.getDimension(R.dimen.title)
    }
    private val canvasRect = RectF(0f, 0f, 1f, 1f)
    private var handler: Handler? = null
    private var staticLayout: StaticLayout? = null
    private val textPadding = context.resources.getDimension(R.dimen.text_renderer_padding)

    override fun invalidate() {
        handler?.post {
            draw()
        }
    }

    override fun setSize(width: Float, height: Float) {
        canvasRect.right = width
        canvasRect.bottom = height
        staticLayout = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            textPaint,
            max(0, (width - 2f * textPadding).toInt())
        ).setAlignment(
            Layout.Alignment.ALIGN_NORMAL
        ).build()
    }

    private fun draw() {
        holder?.let { holder ->
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            }

            canvas.drawRect(canvasRect, emptyPaint)

            canvas.save()
            canvas.translate(canvasRect.centerX(), canvasRect.centerY())
            staticLayout?.draw(canvas)
            canvas.restore()

            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onDestroy() {
        holder = null
        handler = null
    }

    override fun onCreate(surfaceHolder: SurfaceHolder, looper: Looper) {
        holder = surfaceHolder
        handler = Handler(looper)
        invalidate()
    }
}
