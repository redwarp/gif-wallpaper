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
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Simple class do draw a [Drawable] on a [SurfaceHolder]
 */
class SurfaceDrawableRenderer(
    holder: SurfaceHolder,
    looper: Looper,
    drawable: Drawable? = null
) : SurfaceHolder.Callback2, Drawable.Callback {
    private var width: Int = 0
    private var height: Int = 0
    private var isVisible = false
    private var hasDimension = false
    private var surface: Surface? = null
    private val handler: Handler = Handler(looper)

    init {
        drawable?.setVisible(false, false)
        holder.addCallback(this)
    }

    var drawable: Drawable? = drawable
        @Synchronized set(value) {
            field?.callback = null

            field = value
            if (value != null) {
                value.setBounds(0, 0, width, height)

                value.callback = this
                value.setVisible(isVisible, false)

                if (isVisible) {
                    bothNotNull(surface, value) { surface, drawable ->
                        drawOnSurface(surface, drawable)
                    }
                }
            }
        }
        @Synchronized get

    @Synchronized
    fun visibilityChanged(isVisible: Boolean) {
        this.isVisible = isVisible
        drawable?.setVisible(isVisible, false)

        if (isVisible) {
            bothNotNull(surface, drawable) { surface, drawable ->
                drawOnSurface(surface, drawable)
            }
        }
    }

    @Synchronized
    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = holder.surface

        drawable?.callback = this
        drawable?.setVisible(isVisible, true)
        Log.d("GifWallpaper", "Surface created.")
    }

    @Synchronized
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        this.width = width
        this.height = height

        drawable?.setBounds(0, 0, width, height)

        hasDimension = true

        bothNotNull(surface, drawable) { surface, drawable ->
            drawOnSurface(surface, drawable)
        }
    }

    @Synchronized
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        drawable?.callback = null
        drawable?.setVisible(isVisible, false)
        surface?.release()
        surface = null
        hasDimension = false
        Log.d("GifWallpaper", "Surface destroyed.")
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        bothNotNull(surface, drawable) { surface, drawable ->
            drawOnSurface(surface, drawable)
        }
    }

    override fun surfaceRedrawNeededAsync(holder: SurfaceHolder, drawingFinished: Runnable) {
        bothNotNull(surface, drawable) { surface, drawable ->
            drawOnSurface(surface, drawable)
        }
        drawingFinished.run()
    }

    @Synchronized
    private fun drawOnSurface(surface: Surface, drawable: Drawable) {
        if (hasDimension) {
            val canvas: Canvas? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    surface.lockHardwareCanvas()
                } else {
                    surface.lockCanvas(null)
                }

            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawable.draw(canvas)

                surface.unlockCanvasAndPost(canvas)
            }
        }
    }

    override fun invalidateDrawable(who: Drawable) {
        surface?.let { drawOnSurface(it, who) }
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        handler.postAtTime(what, who, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        handler.removeCallbacks(what, who)
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <A, B, R> bothNotNull(left: A?, right: B?, block: (left: A, right: B) -> R) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        if (left != null && right != null) {
            block(left, right)
        }
    }
}
