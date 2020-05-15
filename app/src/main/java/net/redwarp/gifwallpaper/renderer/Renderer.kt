/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.renderer

import android.os.Looper
import android.view.SurfaceHolder

interface Renderer {
    fun onCreate(surfaceHolder: SurfaceHolder, looper: Looper)
    fun onDestroy()
    fun invalidate()
    fun setSize(width: Float, height: Float)
    fun onResume()
    fun onPause()
}
