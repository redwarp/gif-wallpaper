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

import android.os.Looper
import android.view.SurfaceHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class RenderCallback(private val surfaceHolder: SurfaceHolder, private val looper: Looper) :
    SurfaceHolder.Callback2, LifecycleObserver {
    private val size: Size = Size(0f, 0f)
    private var isCreated = false

    var renderer: Renderer? = null
        set(value) {
            field?.onDestroy()
            field = value
            value?.let { renderer ->
                renderer.setSize(size.width, size.height)
                renderer.invalidate()
                if (isCreated) renderer.onCreate(surfaceHolder, looper)
            }
        }

    init {
        surfaceHolder.addCallback(this)
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        renderer?.invalidate()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        size.width = width.toFloat()
        size.height = height.toFloat()
        renderer?.setSize(width.toFloat(), height.toFloat())
        renderer?.invalidate()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isCreated = false
        renderer?.onDestroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isCreated = true
        renderer?.onCreate(holder, looper)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        renderer?.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        renderer?.onPause()
    }

    private data class Size(var width: Float, var height: Float)
}
