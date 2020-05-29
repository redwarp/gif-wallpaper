/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper

import android.app.WallpaperColors
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import net.redwarp.gifwallpaper.data.Model
import net.redwarp.gifwallpaper.renderer.RenderCallback
import net.redwarp.gifwallpaper.renderer.Renderer
import net.redwarp.gifwallpaper.renderer.RendererMapper
import net.redwarp.gifwallpaper.renderer.WallpaperRenderer

private const val MESSAGE_REFRESH_WALLPAPER_COLORS = 1

/**
 * Arbitrary delay to avoid over-requesting colors refresh.
 */
private const val REFRESH_DELAY = 30L

class GifWallpaperService : WallpaperService() {
    private lateinit var rendererMapper: RendererMapper

    override fun onCreateEngine(): Engine {
        return GifEngine()
    }

    inner class GifEngine : Engine(), LifecycleOwner {
        private var renderCallback: RenderCallback? = null
        private val handlerThread = HandlerThread("WallpaperLooper")
        private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
        private var handler: Handler? = null
        private var wallpaperColors: WallpaperColors? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            handlerThread.start()
            handler = Handler(handlerThread.looper)

            renderCallback =
                RenderCallback(surfaceHolder, handlerThread.looper).also(lifecycle::addObserver)
            val model = Model.get(this@GifWallpaperService)
            rendererMapper = RendererMapper(
                model = model,
                surfaceHolder = surfaceHolder,
                animated = false,
                unsetText = getString(R.string.open_app, getString(R.string.app_name))
            ).apply {
                observe(
                    this@GifEngine,
                    Observer { renderer: Renderer ->
                        renderCallback?.renderer = renderer
                        requestWallpaperColorsComputation()
                    })
            }
            model.backgroundColorData.observe(this, Observer {
                requestWallpaperColorsComputation()
            })
            model.scaleTypeData.observe(this, Observer {
                requestWallpaperColorsComputation()
            })
            model.rotationData.observe(this, Observer {
                requestWallpaperColorsComputation()
            })

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        override fun onDestroy() {
            super.onDestroy()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            handlerThread.quit()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            } else {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        val refreshWallpaperColorsRunnable: Runnable = Runnable {
            wallpaperColors = (renderCallback?.renderer as? WallpaperRenderer)?.run {
                val miniature = this.createMiniature()
                WallpaperColors.fromBitmap(miniature).also { miniature.recycle() }
            } ?: getColor(R.color.colorPrimaryDark).colorToWallpaperColor()
            notifyColorsChanged()
        }

        private fun requestWallpaperColorsComputation() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                handler?.let { handler ->
                    if (!handler.hasMessages(MESSAGE_REFRESH_WALLPAPER_COLORS)) {
                        val message = Message.obtain(null, refreshWallpaperColorsRunnable)
                        message.what = MESSAGE_REFRESH_WALLPAPER_COLORS
                        handler.sendMessageDelayed(message, REFRESH_DELAY)
                    }
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? {
            return wallpaperColors
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        fun Int.colorToWallpaperColor(): WallpaperColors {
            val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(this)
            val wallpaperColors = WallpaperColors.fromBitmap(bitmap)
            bitmap.recycle()
            return wallpaperColors
        }
    }
}
