package net.redwarp.gifwallpaper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_setup.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val PICK_GIF_FILE = 2
const val TAG = "GifWallpaper"

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SetupActivity : AppCompatActivity() {
    private var gifDrawer: GifDrawer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_setup)

        dummy_button.setOnClickListener {
            pickDocument()
        }

        gifDrawer = GifDrawer(surface_view.holder)
    }

    private fun pickDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/gif"
        }

        startActivityForResult(intent, PICK_GIF_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_GIF_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                val outputUri = FileUtils.copyFileLocally(this, uri)
                Log.d(TAG, outputUri.toString())

                CoroutineScope(Dispatchers.Main).launch {
                    gifDrawer?.gif = Gif.loadGif(this@SetupActivity, uri)
                }
            }
        }
    }
}
