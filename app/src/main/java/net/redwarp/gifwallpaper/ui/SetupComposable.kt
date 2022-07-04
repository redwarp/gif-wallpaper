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
import androidx.compose.material.AppBarDefaults
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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.renderer.rememberGifDrawablePainter
import net.redwarp.gifwallpaper.ui.setup.ColorPalette
import net.redwarp.gifwallpaper.ui.setup.SetupModel
import net.redwarp.gifwallpaper.util.isDark
import kotlin.math.max

@Composable
fun ActionBar(
    setupModel: SetupModel,
    modifier: Modifier = Modifier,
    onChangeColorClick: () -> Unit
) {
    val hasColor by setupModel.hasColorFlow.collectAsState(initial = false)

    val isWallpaperSet by setupModel.isWallpaperSet.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pickGif = rememberGifPicker { uri ->
        scope.launch {
            setupModel.loadNewGif(context, uri)
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
            enabled = isWallpaperSet

        ) {
            scope.launch {
                setupModel.setNextScale()
            }
        }

        ActionButton(
            icon = R.drawable.ic_rotate_90_degrees_cw,
            text = stringResource(id = R.string.rotate),
            enabled = isWallpaperSet
        ) {
            scope.launch {
                setupModel.setNextRotation()
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
fun SetupUi(
    flowBasedModel: FlowBasedModel,
    setupModel: SetupModel,
    navController: NavController
) {
    val scope = rememberCoroutineScope()

    val darkIcons by setupModel.displayDarkIcons.collectAsState(initial = false)

    UpdateStatusBarColors(darkIcons = darkIcons)

    val drawable by setupModel.drawables.collectAsState(null)

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden
    )

    val colorInfo by setupModel.colorFlow.collectAsState(
        initial = ColorPalette(
            Color.Black,
            emptyList()
        )
    )

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            ColorPicker(
                modifier = Modifier.navigationBarsPadding(),
                defaultColor = colorInfo.defaultColor,
                colors = colorInfo.colors,
                onColorPicked = { color ->
                    scope.launch {
                        setupModel.setBackgroundColor(color)
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
                                setupModel.resetTranslate()
                            }
                        })
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consumeAllChanges()
                            scope.launch {
                                setupModel.postTranslate(dragAmount.x, dragAmount.y)
                            }
                        }
                    },
                painter = rememberGifDrawablePainter(drawable = drawable),
                contentDescription = null,
                contentScale = ContentScale.FillBounds
            )

            ActionMenu(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding(),
                setupModel = setupModel,
                navController = navController,
                tint = if (darkIcons) {
                    if (MaterialTheme.colors.isLight) {
                        MaterialTheme.colors.onSurface
                    } else {
                        MaterialTheme.colors.surface
                    }
                } else {
                    if (MaterialTheme.colors.isLight) {
                        MaterialTheme.colors.surface
                    } else {
                        MaterialTheme.colors.onSurface
                    }
                }
            )
            ActionBar(
                setupModel = setupModel,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding(),
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
fun ActionMenu(
    setupModel: SetupModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    val scope = rememberCoroutineScope()
    val items = mutableListOf<OverflowAction>()
    items.add(
        OverflowAction(stringResource(id = R.string.clear_gif)) {
            scope.launch {
                setupModel.clearGif()
            }
        }
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        items.add(
            OverflowAction(stringResource(id = R.string.settings)) {
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

    OverflowMenu(modifier = modifier, items = items, tint = tint)
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
    Surface(
        modifier = modifier.background(MaterialTheme.colors.surface),
        elevation = AppBarDefaults.BottomAppBarElevation
    ) {
        Layout(
            content = content,
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
fun OverflowMenu(
    modifier: Modifier = Modifier,
    items: List<OverflowAction>,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
) {
    var showMenu by remember {
        mutableStateOf(false)
    }
    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = !showMenu }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "More",
                tint = tint
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
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
}

data class OverflowAction(val text: String, val onClick: () -> Unit)

private val FlowBasedModel.displayDarkIcons: Flow<Boolean> get() = backgroundColorFlow.map { !it.isDark() }
