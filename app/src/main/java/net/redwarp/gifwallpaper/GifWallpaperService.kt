/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper

import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import net.redwarp.gifwallpaper.data.Model
import net.redwarp.gifwallpaper.renderer.RenderCallback
import net.redwarp.gifwallpaper.renderer.Renderer
import net.redwarp.gifwallpaper.renderer.RendererMapper

class GifWallpaperService : WallpaperService() {
    private lateinit var rendererMapper: RendererMapper

    override fun onCreateEngine(): Engine {
        return GifEngine()
    }

    inner class GifEngine : Engine(), LifecycleOwner {
        private var renderCallback: RenderCallback? = null
        private val handlerThread = HandlerThread("WallpaperLooper")
        private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            handlerThread.start()

            renderCallback =
                RenderCallback(surfaceHolder, handlerThread.looper).also(lifecycle::addObserver)
            rendererMapper = RendererMapper(
                model = Model.get(this@GifWallpaperService),
                surfaceHolder = surfaceHolder,
                animated = false
            ).apply {
                observe(
                    this@GifEngine,
                    Observer { renderer: Renderer ->
                        renderCallback?.renderer = renderer
                    })
            }

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
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            }
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }
    }
}
