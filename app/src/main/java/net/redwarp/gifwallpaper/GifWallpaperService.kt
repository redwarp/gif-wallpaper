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
package net.redwarp.gifwallpaper

import android.app.WallpaperColors
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.renderer.SurfaceDrawableRenderer
import net.redwarp.gifwallpaper.renderer.createMiniature
import net.redwarp.gifwallpaper.renderer.drawableFlow

class GifWallpaperService : WallpaperService(), LifecycleOwner {
    private val dispatcher = EngineLifecycleDispatcher(this)

    override fun onCreateEngine(): Engine {
        return GifEngine()
    }

    override fun onCreate() {
        super.onCreate()

        dispatcher.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()

        dispatcher.onDestroy()
    }

    override fun getLifecycle(): Lifecycle {
        return dispatcher.lifecycle
    }

    inner class GifEngine : Engine() {
        private var surfaceDrawableRenderer: SurfaceDrawableRenderer? = null

        private val handlerThread = HandlerThread("WallpaperLooper")
        private var handler: Handler? = null
        private var wallpaperColors: WallpaperColors? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                getColor(R.color.colorPrimaryDark).colorToWallpaperColor()
            } else {
                null
            }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            handlerThread.start()
            handler = Handler(handlerThread.looper)

            surfaceDrawableRenderer =
                SurfaceDrawableRenderer(surfaceHolder, handlerThread.looper)

            val modelFlow = FlowBasedModel.get(this@GifWallpaperService)

            lifecycleScope.launchWhenStarted {
                launch {
                    drawableFlow(
                        context = this@GifWallpaperService,
                        flowBasedModel = modelFlow,
                        unsetText = getString(R.string.open_app),
                        animated = false,
                        isService = true
                    ).collectLatest { drawable ->
                        surfaceDrawableRenderer?.drawable = drawable
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    launch {
                        modelFlow.updateFlow.collectLatest {
                            updateWallpaperColors()
                        }
                    }
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            surfaceDrawableRenderer = null
            handlerThread.quit()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                dispatcher.onResume()
            } else {
                dispatcher.onStop()
            }
            surfaceDrawableRenderer?.visibilityChanged(visible)
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors? {
            return wallpaperColors
        }
        @RequiresApi(Build.VERSION_CODES.O_MR1)
        private suspend fun updateWallpaperColors() {
            withContext(Dispatchers.Default) {
                wallpaperColors = surfaceDrawableRenderer?.drawable?.createMiniature()
                    ?.let(WallpaperColors::fromBitmap)
                    ?: getColor(R.color.colorPrimaryDark).colorToWallpaperColor()
                withContext(Dispatchers.Main) {
                    notifyColorsChanged()
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
