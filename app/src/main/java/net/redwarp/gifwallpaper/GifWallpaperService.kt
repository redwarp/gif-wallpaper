package net.redwarp.gifwallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer

class GifWallpaperService : WallpaperService(), LifecycleOwner {
    private lateinit var lifecycleRegistry: LifecycleRegistry

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
        var gifDrawer: GifDrawer? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            gifDrawer =
                GifDrawer(this@GifWallpaperService, surfaceHolder).also(lifecycle::addObserver)

            WallpaperLiveData.get(this@GifWallpaperService)
                .observe(this@GifWallpaperService, Observer { status ->
                    gifDrawer?.setWallpaperStatus(status)
                })
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
