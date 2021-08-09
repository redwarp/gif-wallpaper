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
        @Synchronized set(value) {
            field?.callback = null

            field = value
            if (value != null) {
                value.setBounds(0, 0, width, height)

                if (isCreated && isVisible) {
                    value.callback = this
                    drawOnSurface(value)
                }
            }
        }
        @Synchronized get

    @Synchronized
    fun visibilityChanged(isVisible: Boolean) {
        this.isVisible = isVisible

        if (isVisible && isCreated) {
            drawable?.callback = this
            drawable?.let(::drawOnSurface)
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

        drawable?.let(::drawOnSurface)
    }

    @Synchronized
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        drawable?.callback = null
        isCreated = false
        hasDimension = false
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        drawable?.let(::drawOnSurface)
    }

    override fun surfaceRedrawNeededAsync(holder: SurfaceHolder, drawingFinished: Runnable) {
        drawable?.let(::drawOnSurface)
        drawingFinished.run()
    }

    @Synchronized
    private fun drawOnSurface(drawable: Drawable) {
        if (isCreated && hasDimension) {
            val canvas: Canvas? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }

            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawable.draw(canvas)

                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    override fun invalidateDrawable(who: Drawable) {
        drawOnSurface(who)
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        handler.postAtTime(what, who, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        handler.removeCallbacks(what, who)
    }
}
