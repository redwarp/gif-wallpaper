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
package net.redwarp.gifwallpaper

import android.app.WallpaperColors
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.renderer.SurfaceDrawableRenderer
import net.redwarp.gifwallpaper.renderer.createMiniature
import net.redwarp.gifwallpaper.renderer.drawableFlow

private const val MESSAGE_REFRESH_WALLPAPER_COLORS = 1

/**
 * Arbitrary delay to avoid over-requesting colors refresh.
 */
private const val REFRESH_DELAY = 200L

class GifWallpaperService : WallpaperService() {
    private var drawableFlow: Flow<Drawable>? = null

    override fun onCreateEngine(): Engine {
        return GifEngine()
    }

    inner class GifEngine : Engine(), LifecycleOwner {
        private var surfaceDrawableRenderer: SurfaceDrawableRenderer? = null

        private val handlerThread = HandlerThread("WallpaperLooper")
        private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
        private var handler: Handler? = null
        private var wallpaperColors: WallpaperColors? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            handlerThread.start()
            handler = Handler(handlerThread.looper)

            surfaceDrawableRenderer =
                SurfaceDrawableRenderer(surfaceHolder, handlerThread.looper)

            val modelFlow = FlowBasedModel.get(this@GifWallpaperService)

            lifecycleScope.launchWhenStarted {
                Log.d("GifWallpaper", "Service resumed")
                drawableFlow(
                    context = this@GifWallpaperService,
                    flowBasedModel = modelFlow,
                    unsetText = getString(R.string.open_app),
                    animated = false,
                    isService = true
                ).also {
                    this@GifWallpaperService.drawableFlow = it
                }.onEach { drawable ->
                    surfaceDrawableRenderer?.drawable = drawable
                    requestWallpaperColorsComputation()
                }.launchIn(this)

                modelFlow.backgroundColorFlow.onEach {
                    requestWallpaperColorsComputation()
                }.launchIn(this)

                modelFlow.rotationFlow.onEach {
                    requestWallpaperColorsComputation()
                }.launchIn(this)

                modelFlow.scaleTypeFlow.onEach {
                    requestWallpaperColorsComputation()
                }.launchIn(this)
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
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
            surfaceDrawableRenderer?.visibilityChanged(visible)
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? {
            return wallpaperColors
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        val refreshWallpaperColorsRunnable: Runnable = Runnable {
            CoroutineScope(Dispatchers.Default).launch {
                wallpaperColors =
                    drawableFlow?.first()?.createMiniature()?.let(WallpaperColors::fromBitmap)
                    ?: getColor(R.color.colorPrimaryDark).colorToWallpaperColor()
                yield()
                notifyColorsChanged()
            }
        }

        private fun requestWallpaperColorsComputation() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                handler?.let { handler ->
                    // Delete previous message, they are obsolete.
                    handler.removeMessages(MESSAGE_REFRESH_WALLPAPER_COLORS)
                    val message = Message.obtain(null, refreshWallpaperColorsRunnable)
                    message.what = MESSAGE_REFRESH_WALLPAPER_COLORS
                    handler.sendMessageDelayed(message, REFRESH_DELAY)
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        private fun Int.colorToWallpaperColor(): WallpaperColors {
            val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(this)
            val wallpaperColors = WallpaperColors.fromBitmap(bitmap)
            bitmap.recycle()
            return wallpaperColors
        }
    }
}
