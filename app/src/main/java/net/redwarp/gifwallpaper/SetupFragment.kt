package net.redwarp.gifwallpaper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_setup.*
import net.redwarp.gifwallpaper.data.Model

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SetupFragment : Fragment() {
    private var gifDrawer: GifDrawer? = null
    private var currentScale = 0
    private lateinit var model: Model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        open_gif_button.setOnClickListener {
            pickDocument()
        }
        change_scale_button.setOnClickListener {
            changeScale()
        }

        gifDrawer = GifDrawer(requireContext(), surface_view.holder).also(lifecycle::addObserver)

        model = Model.get(requireContext())
        model.wallpaperStatus.observe(this, Observer { status ->
            gifDrawer?.setWallpaperStatus(status)
        })
        model.scaleTypeData.observe(this, Observer { scaleType ->
            gifDrawer?.setScaleType(scaleType, animated = true)
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
        currentScale = (currentScale + 1) % GifDrawer.ScaleType.values().size
        model.setScaleType(GifDrawer.ScaleType.values()[currentScale])
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_GIF_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                model.loadNewGif(uri)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.extras, menu)
    }
}
