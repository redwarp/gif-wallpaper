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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.Keep
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.redwarp.gifwallpaper.data.ColorScheme
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.WallpaperStatus
import net.redwarp.gifwallpaper.databinding.FragmentSetupBinding
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType
import net.redwarp.gifwallpaper.renderer.SurfaceDrawableRenderer
import net.redwarp.gifwallpaper.renderer.drawableFlow
import net.redwarp.gifwallpaper.util.ToolbarPosition
import net.redwarp.gifwallpaper.util.setToolbarPosition
import net.redwarp.gifwallpaper.util.systemWindowInsetCompatBottom

const val PICK_GIF_FILE = 2

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@Keep
class SetupFragment : Fragment() {
    private var currentScale = 0
    private var currentRotation = 0
    private lateinit var flowBasedModel: FlowBasedModel

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private var colorInfo: ColorScheme? = null
        set(value) {
            field = value
            binding.changeColorButton.isEnabled = value != null
        }
    private var currentColor: Int? = null
    private lateinit var detector: GestureDetectorCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.openGifButton.setOnClickListener {
            pickDocument()
        }
        binding.changeScaleButton.setOnClickListener {
            changeScale()
        }
        binding.changeColorButton.setOnClickListener {
            changeColor()
        }
        binding.rotateButton.setOnClickListener {
            rotate()
        }
        binding.clearGifButton.setOnClickListener {
            clearGif()
        }
        binding.buttonContainer.setOnApplyWindowInsetsListener { _, insets ->
            binding.buttonContainer.updatePadding(bottom = insets.systemWindowInsetCompatBottom)
            insets
        }

        val renderer = SurfaceDrawableRenderer(binding.surfaceView.holder, Looper.getMainLooper())

        flowBasedModel = FlowBasedModel.get(requireContext())

        lifecycleScope.launchWhenStarted {
            flowBasedModel.colorInfoFlow.onEach { colorInfo ->
                this@SetupFragment.colorInfo = colorInfo as? ColorScheme
            }.launchIn(this)
            flowBasedModel.backgroundColorFlow.onEach { backgroundColor ->
                currentColor = backgroundColor
                adjustTheme(backgroundColor)
            }.launchIn(this)
            flowBasedModel.scaleTypeFlow.onEach { scaleType ->
                currentScale = scaleType.ordinal
            }.launchIn(this)
            flowBasedModel.rotationFlow.onEach { rotation ->
                currentRotation = rotation.ordinal
            }.launchIn(this)
            flowBasedModel.wallpaperStatusFlow.onEach {
                val isWallpaperSet = it is WallpaperStatus.Wallpaper
                binding.changeScaleButton.isEnabled = isWallpaperSet
                binding.rotateButton.isEnabled = isWallpaperSet
                binding.clearGifButton.isEnabled = isWallpaperSet
            }.launchIn(this)

            drawableFlow(
                this@SetupFragment.requireContext(),
                flowBasedModel,
                getString(R.string.click_the_open_gif_button),
                animated = true,
                isService = false
            ).onEach { drawable ->
                renderer.drawable = drawable
            }.launchIn(this)
        }

        detector = GestureDetectorCompat(requireContext(), MyGestureListener())
        binding.touchArea.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.surfaceView) { _, inset ->
            (binding.touchArea.layoutParams as? FrameLayout.LayoutParams)?.setMargins(
                inset.systemGestureInsets.left,
                inset.systemGestureInsets.top,
                inset.systemGestureInsets.right,
                inset.systemGestureInsets.bottom,
            )

            inset
        }
    }

    override fun onResume() {
        super.onResume()
        setToolbarPosition(ToolbarPosition.Overlay)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_GIF_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                lifecycleScope.launchWhenCreated {
                    flowBasedModel.loadNewGif(requireContext(), uri)
                    flowBasedModel.resetTranslate()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.extras, menu)
        menu.findItem(R.id.settings).isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                // startActivity(Intent(requireContext(), SettingsActivity::class.java))
                parentFragmentManager.commit {
                    replace<SettingsFragment>(R.id.nav_host_fragment)
                    setReorderingAllowed(true)
                    addToBackStack(null)
                }
                return true
            }
            R.id.about -> {
                showTextFragment("about.md")
                return true
            }
            R.id.privacy -> {
                showTextFragment("privacy.md")
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun clearGif() {
        lifecycleScope.launchWhenCreated {
            flowBasedModel.clearGif()
        }
    }

    private fun showTextFragment(markdownFileName: String) {
        // startActivity(TextActivity.getIntent(requireContext(), markdownFileName))
        val fragment = TextFragment.newInstance(markdownFileName)
        parentFragmentManager.commit {
            replace(R.id.nav_host_fragment, fragment)
            setReorderingAllowed(true)
            addToBackStack(null)
        }
    }

    private fun adjustTheme(backgroundColor: Int) {
        // setStatusBarColor(backgroundColor.isDark())
        // val overflowColor = if (backgroundColor.isDark()) Color.WHITE else Color.BLACK
        //
        // val toolbar: Toolbar? = (activity as? SetupActivity)?.toolbar
        // val icon = toolbar?.overflowIcon?.let {
        //     DrawableCompat.wrap(it).also { wrapped -> wrapped.setTint(overflowColor) }
        // }
        //
        // toolbar?.overflowIcon = icon
    }

    private fun pickDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/gif"
        }

        startActivityForResult(intent, PICK_GIF_FILE)
    }

    private fun changeScale() {
        currentScale = (currentScale + 1) % ScaleType.values().size
        lifecycleScope.launchWhenCreated {
            flowBasedModel.setScaleType(ScaleType.values()[currentScale])
        }
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
                lifecycleScope.launchWhenCreated {
                    if (it == ColorSheet.NO_COLOR) {
                        currentColor = null
                        flowBasedModel.setBackgroundColor(colorInfo.defaultColor)
                    } else {
                        currentColor = it
                        flowBasedModel.setBackgroundColor(it)
                    }
                }
            }.cornerRadius(0).show(parentFragmentManager)
        }
    }

    private fun rotate() {
        currentRotation = (currentRotation + 1) % Rotation.values().size
        lifecycleScope.launchWhenCreated {
            flowBasedModel.setRotation(Rotation.values()[currentRotation])
        }
    }

    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            lifecycleScope.launchWhenCreated {
                flowBasedModel.postTranslate(-distanceX, -distanceY)
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            lifecycleScope.launchWhenCreated {
                flowBasedModel.resetTranslate()
            }
            return true
        }
    }
}
