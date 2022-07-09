/* Copyright 2020 Benoit Vermont
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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.ui.typography
import kotlin.math.max

class TextDrawable(context: Context, private val text: String) : Drawable() {
    private val emptyPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.colorPrimaryDark)
        style = Paint.Style.FILL
    }
    private val textPaint = TextPaint().apply {
        val density = Density(context)
        val fontSize = with(density) {
            typography.body1.fontSize.toPx()
        }
        val spacing = with(density) {
            typography.body1.letterSpacing.toPx() / fontSize
        }

        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT
        textSize = fontSize
        letterSpacing = spacing
    }
    private val canvasRect = RectF(0f, 0f, 1f, 1f)
    private val textPadding = context.resources.getDimension(R.dimen.text_renderer_padding)
    private var staticLayout: StaticLayout? = null

    override fun draw(canvas: Canvas) {
        val staticLayout = staticLayout ?: return

        canvas.drawRect(canvasRect, emptyPaint)

        canvas.save()
        canvas.translate(
            canvasRect.centerX(),
            canvasRect.centerY() - staticLayout.height.toFloat() / 2f
        )

        staticLayout.draw(canvas)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) = Unit // Don't intend to use that

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit // Don't intend to use that

    @Deprecated("Deprecated in Java", ReplaceWith(""))
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)

        canvasRect.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

        val textWidth = max(0, (canvasRect.width() - 2f * textPadding).toInt())
        staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            buildStaticLayout23(textWidth)
        } else {
            buildStaticLayout21(textWidth)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun buildStaticLayout23(textWidth: Int) =
        StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

    @Suppress("DEPRECATION")
    private fun buildStaticLayout21(textWidth: Int) =
        StaticLayout(text, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true)
}
