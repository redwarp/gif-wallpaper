package net.redwarp.gifwallpaper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_setup.*

const val PICK_GIF_FILE = 2
const val TAG = "GifWallpaper"

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SetupActivity : AppCompatActivity() {
    private var gifDrawer: GifDrawer? = null
    private var currentScale = 0
    private lateinit var wallpaperLiveData: WallpaperLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_setup)

        open_gif_button.setOnClickListener {
            pickDocument()
        }
        change_scale_button.setOnClickListener {
            changeScale()
        }

        gifDrawer = GifDrawer(surface_view.holder).also(lifecycle::addObserver)

        wallpaperLiveData = WallpaperLiveData.get(this)
        wallpaperLiveData.observe(this, Observer { status ->
            gifDrawer?.setWallpaperStatus(status)
        })
    }

    private fun pickDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/gif"
        }

        startActivityForResult(intent, PICK_GIF_FILE)
    }

    private fun changeScale() {
        currentScale = (currentScale + 1) % 4
        val scaleType = when (currentScale) {
            0 -> GifDrawer.ScaleType.FIT_CENTER
            1 -> GifDrawer.ScaleType.FIT_END
            2 -> GifDrawer.ScaleType.FIT_START
            else -> GifDrawer.ScaleType.FIT_XY
        }

        gifDrawer?.scaleType = scaleType
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_GIF_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                wallpaperLiveData.loadNewGif(uri)
            }
        }
    }
}
