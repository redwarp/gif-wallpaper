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

class GifWallpaperService : WallpaperService(), LifecycleOwner {
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var rendererMapper: RendererMapper

    override fun onCreate() {
        super.onCreate()

        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun onCreateEngine(): Engine {
        return GifEngine()
    }

    inner class GifEngine : Engine() {
        private var renderCallback: RenderCallback? = null
        private val handlerThread = HandlerThread("WallpaperLooper")

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            handlerThread.start()

            renderCallback =
                RenderCallback(surfaceHolder, handlerThread.looper).also(lifecycle::addObserver)
            rendererMapper = RendererMapper(
                model = Model.get(this@GifWallpaperService),
                surfaceHolder = surfaceHolder,
                animated = false
            ).also {
                it.observe(
                    this@GifWallpaperService,
                    Observer { renderer: Renderer ->
                        renderCallback?.renderer = renderer
                    })
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handlerThread.quit()
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                lifecycleRegistry.currentState = Lifecycle.State.RESUMED
            } else {
                lifecycleRegistry.currentState = Lifecycle.State.CREATED
            }
        }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}
