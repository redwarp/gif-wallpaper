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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder

/**
 * Simple class do draw a [Drawable] on a [SurfaceHolder]
 */
class SurfaceDrawableRenderer(
    private val holder: SurfaceHolder,
    looper: Looper,
    drawable: Drawable? = null
) : SurfaceHolder.Callback2, Drawable.Callback {
    private var width: Int = 0
    private var height: Int = 0
    private var isCreated = false
    private var isVisible = true
    private var hasDimension = false
    private val handler: Handler = Handler(looper)

    init {
        holder.addCallback(this)
    }

    var drawable: Drawable? = drawable
        set(value) {
            synchronized(this) {
                field?.callback = null

                field = value
                if (value != null) {
                    value.setBounds(0, 0, width, height)

                    if (isCreated && isVisible) {
                        value.callback = this
                        drawOnSurface()
                    }
                }
            }
        }

    @Synchronized
    fun visibilityChanged(isVisible: Boolean) {
        this.isVisible = isVisible

        if (isVisible && isCreated) {
            drawable?.callback = this
            drawOnSurface()
        } else {
            drawable?.callback = null
        }
    }

    @Synchronized
    override fun surfaceCreated(holder: SurfaceHolder) {
        isCreated = true

        if (isVisible) drawable?.callback = this
    }

    @Synchronized
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        this.width = width
        this.height = height

        drawable?.setBounds(0, 0, width, height)

        hasDimension = true

        drawOnSurface()
    }

    @Synchronized
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        drawable?.callback = null
        isCreated = false
        hasDimension = false
    }

    @Synchronized
    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        drawOnSurface()
    }

    override fun surfaceRedrawNeededAsync(holder: SurfaceHolder, drawingFinished: Runnable) {
        synchronized(this) {
            drawOnSurface()
        }
        drawingFinished.run()
    }

    @Synchronized
    private fun drawOnSurface() {
        val drawable = drawable
        if (isCreated && drawable != null && hasDimension) {
            val canvas: Canvas? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }

            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                draw(canvas, drawable)

                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    @Synchronized
    private fun draw(canvas: Canvas, drawable: Drawable) {
        drawable.draw(canvas)
    }

    override fun invalidateDrawable(who: Drawable) {
        drawOnSurface()
    }

    private val drawRunnable = { drawOnSurface() }
    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        handler.postAtTime(drawRunnable, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        handler.removeCallbacks(drawRunnable)
    }
}
