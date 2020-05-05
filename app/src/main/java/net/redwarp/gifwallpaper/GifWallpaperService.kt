package net.redwarp.gifwallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GifWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return GifEngine()
    }

    inner class GifEngine : Engine() {
        var gifDrawer: GifDrawer? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            gifDrawer = GifDrawer(surfaceHolder)

            updateWallpaper()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                updateWallpaper()
            } else {
                gifDrawer?.gif = null
            }
        }

        private fun updateWallpaper() {
            CoroutineScope(Dispatchers.Main).launch {
                val wallpaper = Wallpaper.getWallpaper(this@GifWallpaperService)
                if (wallpaper != null) {
                    gifDrawer?.gif = Gif.loadGif(this@GifWallpaperService, wallpaper.uri)
                }
            }
        }
    }
}
