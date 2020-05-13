/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.android.synthetic.main.activity_setup.*
import kotlinx.android.synthetic.main.fragment_setup.*
import net.redwarp.gifwallpaper.data.ColorScheme
import net.redwarp.gifwallpaper.data.Model
import net.redwarp.gifwallpaper.renderer.RenderCallback
import net.redwarp.gifwallpaper.renderer.Renderer
import net.redwarp.gifwallpaper.renderer.RendererMapper
import net.redwarp.gifwallpaper.renderer.WallpaperRenderer
import net.redwarp.gifwallpaper.util.isDark
import net.redwarp.gifwallpaper.util.themeColor

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SetupFragment : Fragment() {
    private var renderCallback: RenderCallback? = null
    private var currentScale = 0
    private lateinit var model: Model
    private var colorInfo: ColorScheme? = null
        set(value) {
            field = value
            change_color_button.isEnabled = value != null
        }
    private var currentColor: Int? = null

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
        change_color_button.setOnClickListener {
            changeColor()
        }

        renderCallback =
            RenderCallback(surface_view.holder, Looper.getMainLooper()).also(lifecycle::addObserver)

        model = Model.get(requireContext())
        RendererMapper(
            model = model,
            surfaceHolder = surface_view.holder,
            animated = true
        ).observe(
            viewLifecycleOwner,
            Observer { renderer: Renderer ->
                renderCallback?.renderer = renderer
            })

        model.colorInfoData.observe(viewLifecycleOwner, Observer { colorStatus ->
            colorInfo = colorStatus as? ColorScheme
            change_color_button.isEnabled = colorStatus is ColorScheme
        })
        model.backgroundColorData.observe(viewLifecycleOwner, Observer {
            currentColor = it
            adjustTheme(it)
        })
        model.scaleTypeData.observe(viewLifecycleOwner, Observer {
            currentScale = it.ordinal
        })
    }

    private fun adjustTheme(backgroundColor: Int) {
        // This is disgusting and should go.
        val darkColor: Int = requireContext().themeColor(android.R.attr.textColorPrimary)
        val lightColor: Int = requireContext().themeColor(android.R.attr.textColorPrimaryInverse)

        activity?.window?.apply {
            if (backgroundColor.isDark()) {
                decorView.systemUiVisibility =
                    decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                val icon = activity?.toolbar?.overflowIcon?.let {
                    DrawableCompat.wrap(it).also { it.setTint(lightColor) }
                }

                activity?.toolbar?.overflowIcon = icon
            } else {
                decorView.systemUiVisibility =
                    decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                val icon = activity?.toolbar?.overflowIcon?.let {
                    DrawableCompat.wrap(it).also { it.setTint(darkColor) }
                }

                activity?.toolbar?.overflowIcon = icon
            }
        }
    }

    private fun pickDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/gif"
        }

        startActivityForResult(intent, PICK_GIF_FILE)
    }

    private fun changeScale() {
        currentScale = (currentScale + 1) % WallpaperRenderer.ScaleType.values().size
        model.setScaleType(WallpaperRenderer.ScaleType.values()[currentScale])
    }

    private fun changeColor() {
        colorInfo?.let { colorInfo ->
            val colors =
                colorInfo.palette.targets.map {
                    colorInfo.palette.getColorForTarget(it, Color.BLACK)
                }.distinct().toIntArray()

            ColorSheet().colorPicker(
                colors, currentColor, noColorOption = true
            ) {
                if (it == ColorSheet.NO_COLOR) {
                    currentColor = null
                    model.setBackgroundColor(colorInfo.defaultColor)
                } else {
                    currentColor = it
                    model.setBackgroundColor(it)
                }
            }.cornerRadius(0).show(parentFragmentManager)
        }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_gif -> {
                model.clearGif()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
