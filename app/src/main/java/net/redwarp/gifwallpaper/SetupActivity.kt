/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_setup.*

const val TAG = "GifWallpaper"

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_setup)
        setupActionBar()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        toolbar.setOnApplyWindowInsetsListener { v, insets ->
            toolbar.y = insets.systemWindowInsetTop.toFloat()
            insets
        }
        supportActionBar?.title = null
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

            statusBarColor = Color.TRANSPARENT
        }
    }
}
