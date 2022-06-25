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
@file:OptIn(ExperimentalMaterialApi::class)

package net.redwarp.gifwallpaper.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.GifApplication
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.ColorScheme
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.WallpaperStatus
import net.redwarp.gifwallpaper.renderer.DrawableMapper
import net.redwarp.gifwallpaper.renderer.rememberGifDrawablePainter

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                SetupUi(flowBasedModel = GifApplication.app.model)
            }
        }
    }
}

@Composable
fun ActionBar(
    flowBasedModel: FlowBasedModel,
    modifier: Modifier = Modifier,
    onChangeColorClick: () -> Unit
) {
    val colorInfo by flowBasedModel.colorInfoFlow.map { it as? ColorScheme }
        .collectAsState(
            initial = null
        )
    val wallpaperStatus by flowBasedModel.wallpaperStatusFlow.collectAsState(initial = WallpaperStatus.NotSet)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickGif = PickGif { uri ->
        scope.launch {
            flowBasedModel.loadNewGif(context, uri)
            flowBasedModel.resetTranslate()
        }
    }

    ActionRow(modifier = modifier) {
        ActionButton(
            icon = R.drawable.ic_collections,
            text = stringResource(id = R.string.open_gif)
        ) {
            // pickDocument()
            pickGif.launch("image/gif")
        }

        ActionButton(
            icon = R.drawable.ic_transform,
            text = stringResource(id = R.string.change_scale),
            enabled = wallpaperStatus is WallpaperStatus.Wallpaper

        ) {
            // changeScale()
            scope.launch {
                flowBasedModel.setNextScale()
            }
        }

        ActionButton(
            icon = R.drawable.ic_rotate_90_degrees_cw,
            text = stringResource(id = R.string.rotate),
            enabled = wallpaperStatus is WallpaperStatus.Wallpaper
        ) {
            scope.launch {
                flowBasedModel.setNextRotation()
            }
        }

        ActionButton(
            icon = R.drawable.ic_color_lens,
            text = stringResource(id = R.string.change_color),
            enabled = colorInfo != null,
            onClick = onChangeColorClick
        )
    }
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

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )
    val colorInfo: Pair<Color, List<Color>> by flowBasedModel.colorInfoFlow.map { colorInfo ->
        if (colorInfo is ColorScheme) {
            colorInfo.defaultColor.rgbToColor() to colorInfo.palette.targets.map {
                colorInfo.palette.getColorForTarget(it, android.graphics.Color.BLACK)
                    .rgbToColor()
            }.distinct()
        } else Color.Black to emptyList()
    }.collectAsState(
        initial = Color.Black to emptyList()
    )

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            ColorPicker(
                defaultColor = colorInfo.first,
                colors = colorInfo.second,
                onColorPicked = { color ->
                    scope.launch {
                        flowBasedModel.setBackgroundColor(color.toArgb())
                        sheetState.hide()
                    }
                },
                onCloseClick = {
                    scope.launch {
                        sheetState.hide()
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            scope.launch {
                                flowBasedModel.resetTranslate()
                            }
                        })
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consumeAllChanges()
                            scope.launch {
                                flowBasedModel.postTranslate(dragAmount.x, dragAmount.y)
                            }
                        }
                    },
                painter = rememberGifDrawablePainter(drawable = drawable),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )
            TransparentTopBar(flowBasedModel)
            ActionBar(
                flowBasedModel = flowBasedModel,
                modifier = Modifier.align(Alignment.BottomCenter),
                onChangeColorClick = {
                    scope.launch {
                        sheetState.show()
                    }
                }
            )
        }
    }
}

@Composable
fun TransparentTopBar(flowBasedModel: FlowBasedModel, modifier: Modifier = Modifier) {
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
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                )
            }
            items.add(
                OverflowAction(stringResource(id = R.string.about)) {
                    context.startActivity(TextActivity.getIntent(context, "about.md"))
                }
            )

            items.add(
                OverflowAction(stringResource(id = R.string.privacy)) {
                    context.startActivity(TextActivity.getIntent(context, "privacy.md"))
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

@Composable
fun PickGif(onUri: (Uri) -> Unit): ManagedActivityResultLauncher<String, Uri?> {
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let(onUri)
        }
    )
    return result
}
