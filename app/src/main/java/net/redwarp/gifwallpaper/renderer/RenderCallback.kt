/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.renderer

import android.view.SurfaceHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class RenderCallback(surfaceHolder: SurfaceHolder) : SurfaceHolder.Callback2, LifecycleObserver {
    private val size: Size = Size(0f, 0f)
    var renderer: Renderer? = null
        set(value) {
            field?.onDestroy()
            field = value
            value?.let { renderer ->
                renderer.setSize(size.width, size.height)
                renderer.invalidate()
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
        renderer?.onDestroy()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderer?.onCreate(holder)
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
