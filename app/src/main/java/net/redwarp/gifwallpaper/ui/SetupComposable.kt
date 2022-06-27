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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.ColorScheme
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.WallpaperStatus
import net.redwarp.gifwallpaper.renderer.DrawableMapper
import net.redwarp.gifwallpaper.renderer.rememberGifDrawablePainter
import kotlin.math.max

@Composable
fun ActionBar(
    flowBasedModel: FlowBasedModel,
    modifier: Modifier = Modifier,
    onChangeColorClick: () -> Unit
) {
    val hasColor by flowBasedModel.hasColorFlow.collectAsState(initial = false)

    val wallpaperStatus by flowBasedModel.wallpaperStatusFlow.collectAsState(initial = WallpaperStatus.NotSet)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickGif = rememberGifPicker { uri ->
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
            pickGif.launch("image/gif")
        }

        ActionButton(
            icon = R.drawable.ic_transform,
            text = stringResource(id = R.string.change_scale),
            enabled = wallpaperStatus is WallpaperStatus.Wallpaper

        ) {
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
            enabled = hasColor,
            onClick = onChangeColorClick
        )
    }
}

@Composable
fun SetupUi(flowBasedModel: FlowBasedModel, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val drawableOwner = remember {
        DrawableMapper.previewMapper(
            context = context,
            flowBasedModel = flowBasedModel,
            scope = scope,
        )
    }
    val drawable by drawableOwner.drawables.collectAsState(null)

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )

    var colorInfo: Pair<Color, List<Color>> by remember {
        mutableStateOf(Color.Black to emptyList())
    }
    LaunchedEffect("colorInfo") {
        flowBasedModel.colorInfoFlow.map { colorInfo ->
            if (colorInfo is ColorScheme) {
                colorInfo.defaultColor.rgbToColor() to colorInfo.palette.targets.map {
                    colorInfo.palette.getColorForTarget(it, android.graphics.Color.BLACK)
                        .rgbToColor()
                }.distinct()
            } else Color.Black to emptyList()
        }.collect {
            colorInfo = it
        }
    }

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
            TransparentTopBar(flowBasedModel, navController)
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
fun TransparentTopBar(
    flowBasedModel: FlowBasedModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

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
                        // context.startActivity(Intent(context, SettingsActivity::class.java))
                        navController.navigate("settings")
                    }
                )
            }
            items.add(
                OverflowAction(stringResource(id = R.string.about)) {
                    navController.navigate("about")
                }
            )

            items.add(
                OverflowAction(stringResource(id = R.string.privacy)) {
                    navController.navigate("privacy")
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
fun rememberGifPicker(onUri: (Uri) -> Unit): ManagedActivityResultLauncher<String, Uri?> {
    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let(onUri)
        }
    )
    return result
}

@Composable
fun VerticalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
    val colors = MaterialTheme.colors.onSurface
    CompositionLocalProvider(
        LocalContentAlpha provides contentAlpha,
        LocalContentColor provides colors
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.button) {
            Layout(
                content = content,
                modifier = modifier
                    .clickable(
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = true)
                    )
                    .padding(PaddingValues(8.dp))
            ) { measurables, constraints ->
                val placeables = measurables.map { measurable ->
                    measurable.measure(constraints)
                }
                val height = max(placeables.sumOf(Placeable::height), constraints.minHeight)
                val width = max(placeables.maxOf(Placeable::width), constraints.minWidth)

                layout(width, height) {
                    var yPosition = 0

                    placeables.forEach { placeable ->
                        placeable.placeRelative(x = (width - placeable.width) / 2, y = yPosition)

                        yPosition += placeable.height
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    VerticalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
        )
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.wrapContentHeight(Alignment.CenterVertically)
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ActionButtonPreviewNight() {
    AppTheme {
        ActionButton(
            icon = R.drawable.ic_collections,
            text = stringResource(id = R.string.open_gif)
        ) {
        }
    }
}

@Composable
fun ActionRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = modifier.background(MaterialTheme.colors.surface)
    ) { measurables, constraints ->
        val childWidth = constraints.maxWidth / measurables.size
        val childHeight = measurables.maxOf { it.minIntrinsicHeight(childWidth) }
        val childConstraint = constraints.copy(
            maxWidth = childWidth,
            minWidth = childWidth,
            minHeight = childHeight,
        )
        val placeables = measurables.map { measurable ->
            measurable.measure(childConstraint)
        }

        val height = placeables.maxOf(Placeable::height)

        layout(constraints.maxWidth, height) {
            var xPosition = 0

            placeables.forEach { placeable ->
                placeable.placeRelative(x = xPosition, y = 0)

                xPosition += placeable.width
            }
        }
    }
}

@Preview
@Composable
fun ActionBarPreview() {
    AppTheme {
        ActionRow(modifier = Modifier) {
            ActionButton(
                icon = R.drawable.ic_collections,
                text = stringResource(id = R.string.open_gif)
            ) {
            }

            ActionButton(
                icon = R.drawable.ic_transform,
                text = stringResource(id = R.string.change_scale)
            ) {
            }

            ActionButton(
                icon = R.drawable.ic_rotate_90_degrees_cw,
                text = stringResource(id = R.string.rotate)
            ) {
            }

            ActionButton(
                icon = R.drawable.ic_color_lens,
                text = stringResource(id = R.string.change_color)
            ) {
            }
        }
    }
}

@Composable
fun OverflowMenu(items: List<OverflowAction>) {
    var showMenu by remember {
        mutableStateOf(false)
    }

    IconButton(onClick = { showMenu = !showMenu }) {
        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "More")
    }
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        for (item in items) {
            DropdownMenuItem(onClick = {
                showMenu = false
                item.onClick()
            }) {
                Text(text = item.text)
            }
        }
    }
}

data class OverflowAction(val text: String, val onClick: () -> Unit)
