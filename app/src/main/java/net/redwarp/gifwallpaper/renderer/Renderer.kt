/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.renderer

import android.view.SurfaceHolder

interface Renderer {
    fun onCreate(surfaceHolder: SurfaceHolder)
    fun onDestroy()
    fun invalidate()
    fun setSize(width: Float, height: Float)
    fun onResume()
    fun onPause()
}
