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
package net.redwarp.gifwallpaper.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.redwarp.gifwallpaper.GifApplication
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.ColorScheme
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.WallpaperStatus
import net.redwarp.gifwallpaper.renderer.DrawableMapper
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType
import net.redwarp.gifwallpaper.renderer.rememberGifDrawablePainter
import net.redwarp.gifwallpaper.util.isDark
import net.redwarp.gifwallpaper.util.setStatusBarColor

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
@Keep
class SetupFragment : Fragment() {
    private var currentScale = 0
    private var currentRotation = 0
    private lateinit var flowBasedModel: FlowBasedModel

    private var currentColor: Int? = null
    private lateinit var detector: GestureDetectorCompat
    private val getGif =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launchWhenCreated {
                flowBasedModel.loadNewGif(requireContext(), uri)
                flowBasedModel.resetTranslate()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                AppTheme {
                    SetupUi(flowBasedModel = GifApplication.app.model)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // _binding = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // binding.buttonContainer.setOnApplyWindowInsetsListener { _, insets ->
        //     binding.buttonContainer.updatePadding(bottom = insets.systemWindowInsetCompatBottom)
        //     insets
        // }

        flowBasedModel = GifApplication.app.model

        lifecycleScope.launchWhenStarted {
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
        }

        detector = GestureDetectorCompat(requireContext(), MyGestureListener())
        // binding.touchArea.setOnTouchListener { _, event ->
        //     detector.onTouchEvent(event)
        // }
        // ViewCompat.setOnApplyWindowInsetsListener(binding.imageView) { _, inset ->
        //     val systemGestureInsets = inset.getInsets(WindowInsetsCompat.Type.systemGestures())
        //
        //     (binding.touchArea.layoutParams as? FrameLayout.LayoutParams)?.setMargins(
        //         systemGestureInsets.left,
        //         systemGestureInsets.top,
        //         systemGestureInsets.right,
        //         systemGestureInsets.bottom,
        //     )
        //
        //     inset
        // }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.extras, menu)
        menu.findItem(R.id.settings).isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_gif -> {
                lifecycleScope.launchWhenCreated {
                    flowBasedModel.clearGif()
                }
                return true
            }
            R.id.settings -> {
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
                return true
            }
            R.id.about -> {
                startActivity(TextActivity.getIntent(requireContext(), "about.md"))
                return true
            }
            R.id.privacy -> {
                startActivity(TextActivity.getIntent(requireContext(), "privacy.md"))
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun adjustTheme(backgroundColor: Int) {
        setStatusBarColor(backgroundColor.isDark())
        val overflowColor = if (backgroundColor.isDark()) Color.WHITE else Color.BLACK

        val toolbar: Toolbar? = null
        // val toolbar: Toolbar? = (activity as? SetupActivity)?.toolbar
        val icon = toolbar?.overflowIcon?.let {
            DrawableCompat.wrap(it).also { wrapped -> wrapped.setTint(overflowColor) }
        }

        toolbar?.overflowIcon = icon
    }

    private fun pickDocument() {
        getGif.launch("image/gif")
    }

    private fun changeScale() {
        currentScale = (currentScale + 1) % ScaleType.values().size
        lifecycleScope.launchWhenCreated {
            flowBasedModel.setScaleType(ScaleType.values()[currentScale])
        }
    }

    private fun changeColor(colorInfo: ColorScheme) {
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

    @Composable
    fun ActionBar(flowBasedModel: FlowBasedModel, modifier: Modifier = Modifier) {
        val colorInfo by flowBasedModel.colorInfoFlow.map { it as? ColorScheme }
            .collectAsState(
                initial = null
            )
        val wallpaperStatus by flowBasedModel.wallpaperStatusFlow.collectAsState(initial = WallpaperStatus.NotSet)

        ActionRow(modifier = modifier) {
            ActionButton(
                icon = R.drawable.ic_collections,
                text = stringResource(id = R.string.open_gif)
            ) {
                pickDocument()
            }

            ActionButton(
                icon = R.drawable.ic_transform,
                text = stringResource(id = R.string.change_scale),
                enabled = wallpaperStatus is WallpaperStatus.Wallpaper

            ) {
                changeScale()
            }

            ActionButton(
                icon = R.drawable.ic_rotate_90_degrees_cw,
                text = stringResource(id = R.string.rotate),
                enabled = wallpaperStatus is WallpaperStatus.Wallpaper
            ) {
                rotate()
            }

            ActionButton(
                icon = R.drawable.ic_color_lens,
                text = stringResource(id = R.string.change_color),
                enabled = colorInfo != null
            ) {
                colorInfo?.let(::changeColor)
            }
        }
    }

    @Composable
    fun TransparentTopBar(modifier: Modifier = Modifier) {
        TopAppBar(
            title = {
                Text(text = "Hello")
            },
            actions = {
                // OverflowMenu {
                //     DropdownMenuItem(onClick = { /*TODO*/ }) {
                //         Text(text = stringResource(id = R.string.clear_gif))
                //     }
                //
                //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //         DropdownMenuItem(onClick = { /*TODO*/ }) {
                //             Text(text = stringResource(id = R.string.settings))
                //         }
                //     }
                //     DropdownMenuItem(onClick = { /*TODO*/ }) {
                //         Text(text = stringResource(id = R.string.about))
                //     }
                //
                //     DropdownMenuItem(onClick = { /*TODO*/ }) {
                //         Text(text = stringResource(id = R.string.privacy))
                //     }
                // }
            },

            backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = androidx.compose.ui.graphics.Color.Transparent,
            elevation = 0.dp,
            modifier = modifier
        )
    }

    @Preview
    @Composable
    fun TopBarPreview() {
        TransparentTopBar()
    }

    @Composable
    fun SetupUi(flowBasedModel: FlowBasedModel) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val drawableOwner by remember {
            mutableStateOf(
                DrawableMapper.previewMapper(
                    context = context,
                    flowBasedModel = flowBasedModel,
                    scope = scope,
                )
            )
        }
        val drawable by drawableOwner.drawables.collectAsState(null)

        Scaffold(topBar = {
            TransparentTopBar()
        }) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = rememberGifDrawablePainter(drawable = drawable),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
                ActionBar(
                    flowBasedModel = flowBasedModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
