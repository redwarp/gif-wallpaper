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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.redwarp.gifwallpaper.R
import kotlin.math.max

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
                    // .background(MaterialTheme.colors.surface)
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
