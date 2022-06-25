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

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dev.sasikanth.colorsheet.ColorSheet
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.GifApplication
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.ColorScheme
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.WallpaperStatus
import net.redwarp.gifwallpaper.renderer.DrawableMapper
import net.redwarp.gifwallpaper.renderer.Rotation
import net.redwarp.gifwallpaper.renderer.ScaleType
import net.redwarp.gifwallpaper.renderer.rememberGifDrawablePainter

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SetupActivity : AppCompatActivity() {

    private var currentScale = 0
    private var currentRotation = 0
    private val flowBasedModel: FlowBasedModel get() = GifApplication.app.model

    private var currentColor: Int? = null

    private val getGif =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launchWhenCreated {
                flowBasedModel.loadNewGif(this@SetupActivity, uri)
                flowBasedModel.resetTranslate()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                SetupUi(flowBasedModel = flowBasedModel)
            }
        }
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
                colorInfo.palette.getColorForTarget(it, android.graphics.Color.BLACK)
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
        }.cornerRadius(0).show(supportFragmentManager)
    }

    private fun rotate() {
        currentRotation = (currentRotation + 1) % Rotation.values().size
        lifecycleScope.launchWhenCreated {
            flowBasedModel.setRotation(Rotation.values()[currentRotation])
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
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        TopAppBar(
            title = {
                Text(text = "Hello")
            },
            actions = {
                val items = mutableListOf<OverflowAction>()
                items.add(
                    OverflowAction(stringResource(id = R.string.clear_gif)) {
                        scope.launch {
                            flowBasedModel.clearGif()
                        }
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    items.add(
                        OverflowAction(stringResource(id = R.string.settings)) {
                            startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    )
                }
                items.add(
                    OverflowAction(stringResource(id = R.string.about)) {
                        startActivity(TextActivity.getIntent(context, "about.md"))
                    }
                )

                items.add(
                    OverflowAction(stringResource(id = R.string.privacy)) {
                        startActivity(TextActivity.getIntent(context, "privacy.md"))
                    }
                )

                OverflowMenu(items = items)
            },

            backgroundColor = Color.Transparent,
            contentColor = Color.Transparent,
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

        Scaffold {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = rememberGifDrawablePainter(drawable = drawable),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds
                )
                TransparentTopBar()
                ActionBar(
                    flowBasedModel = flowBasedModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
