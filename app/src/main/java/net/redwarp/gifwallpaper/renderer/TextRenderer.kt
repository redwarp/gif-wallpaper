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

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Choreographer
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import net.redwarp.gifwallpaper.R
import kotlin.math.max

class TextRenderer(
    context: Context,
    private var holder: SurfaceHolder?,
    private val text: String
) : Renderer {
    private val emptyPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorPrimaryDark)
        style = Paint.Style.FILL
    }
    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.SANS_SERIF
        textSize = context.resources.getDimension(R.dimen.text_renderer_font_size)
    }
    private val canvasRect = RectF(0f, 0f, 1f, 1f)
    private var choreographer: Choreographer? = null
    private var staticLayout: StaticLayout? = null
    private val textPadding = context.resources.getDimension(R.dimen.text_renderer_padding)

    override fun invalidate() {
        choreographer?.postFrameCallback {
            draw()
        }
    }

    override fun setSize(width: Float, height: Float) {
        canvasRect.right = width
        canvasRect.bottom = height
        val textWidth = max(0, (width - 2f * textPadding).toInt())

        staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            buildStaticLayout23(textWidth)
        } else {
            buildStaticLayout21(textWidth)
        }
        invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun buildStaticLayout23(textWidth: Int) =
        StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun buildStaticLayout21(textWidth: Int) =
        StaticLayout(text, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true)

    private fun draw() {
        val staticLayout = staticLayout ?: return

        holder?.let { holder ->
            val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                holder.lockHardwareCanvas()
            } else {
                holder.lockCanvas()
            }

            canvas.drawRect(canvasRect, emptyPaint)

            canvas.save()
            canvas.translate(
                canvasRect.centerX(),
                canvasRect.centerY() - staticLayout.height.toFloat() / 2f
            )

            staticLayout.draw(canvas)
            canvas.restore()

            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onResume() = Unit

    override fun onPause() = Unit

    override fun onDestroy() {
        holder = null
    }

    override fun onCreate(surfaceHolder: SurfaceHolder, looper: Looper) {
        holder = surfaceHolder
        choreographer = Choreographer.getInstance()
        invalidate()
    }
}
