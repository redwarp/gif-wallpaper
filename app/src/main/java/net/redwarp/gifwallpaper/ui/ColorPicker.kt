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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.redwarp.gifwallpaper.R

private val choiceSize = 48.dp
private val choicePadding = 4.dp

@Composable
fun NoColorChoice(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(choiceSize)
            .padding(choicePadding)
            .clip(CircleShape)
            .background(MaterialTheme.colors.onSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_color_lens_off),
            contentDescription = null,
            tint = MaterialTheme.colors.surface
        )
    }
}

@Composable
fun ColorChoice(color: Color, onClick: () -> Unit) {
    var modifier = Modifier
        .size(choiceSize)
        .padding(choicePadding)
        .clip(CircleShape)
        .background(color)
        .clickable(onClick = onClick)
    if (color == MaterialTheme.colors.surface) {
        modifier = modifier
            .border(width = 1.dp, color = MaterialTheme.colors.onSurface, shape = CircleShape)
    }
    Box(
        modifier = modifier
    ) {
    }
}

@Composable
fun EvenFlow(modifier: Modifier = Modifier, spacing: Dp = 0.dp, content: @Composable () -> Unit) {
    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = { measurables, constraints ->
            val spacingInPx = spacing.roundToPx()
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }

            val sequences = mutableListOf<List<Placeable>>()
            val currentSequence = mutableListOf<Placeable>()

            var usedWidth = 0
            var lineFull = false

            fun canAddToCurrent(placeable: Placeable): Boolean =
                currentSequence.isEmpty() || (usedWidth + placeable.width + spacingInPx) < constraints.maxWidth

            fun newSequence() {
                sequences.add(currentSequence.toList())
                lineFull = true

                currentSequence.clear()
                usedWidth = 0
            }

            for (placeable in placeables) {
                if (!canAddToCurrent(placeable)) {
                    newSequence()
                }
                if (currentSequence.isNotEmpty()) {
                    usedWidth += spacingInPx
                }
                usedWidth += placeable.width
                currentSequence.add(placeable)
            }
            if (currentSequence.isNotEmpty()) {
                sequences.add(currentSequence.toList())
            }

            val evenSpacing = if (lineFull) {
                sequences.firstOrNull()?.let { line ->
                    val summedWidth = line.sumOf(Placeable::width)

                    (constraints.maxWidth - summedWidth) / (line.size - 1)
                } ?: 0
            } else {
                spacingInPx
            }

            val totalHeight = sequences.sumOf { line ->
                line.maxOf(Placeable::height)
            } + kotlin.math.max(0, sequences.size - 1) * evenSpacing

            layout(constraints.maxWidth, totalHeight) {
                var yOffset = 0
                var xOffset: Int
                for (line in sequences) {
                    xOffset = 0

                    for (placeable in line) {
                        placeable.placeRelative(xOffset, yOffset)

                        xOffset += placeable.width + evenSpacing
                    }
                    yOffset += evenSpacing + line.maxOf(Placeable::height)
                }
            }
        }
    )
}

@Composable
fun ColorPicker(
    modifier: Modifier = Modifier,
    defaultColor: Color,
    colors: List<Color>,
    onColorPicked: (Color) -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
            Text(
                text = stringResource(id = R.string.pick_a_color),
                style = MaterialTheme.typography.h6
            )
        }
        Divider()
        EvenFlow(spacing = 8.dp, modifier = Modifier.padding(16.dp)) {
            NoColorChoice {
                onColorPicked(defaultColor)
            }
            for (color in colors) {
                ColorChoice(color = color) {
                    onColorPicked(color)
                }
            }
        }
    }
}

@Preview
@Composable
fun ColorPickerPreview() {
    AppTheme {
        ColorPicker(
            defaultColor = Color.White,
            colors = listOf(
                0xffffff,
                0xff0000,
                0x00ff00,
                0x0000ff,
                0x000000,
                0x1587af,
                0x4578f3
            ).map(Int::rgbToColor)
        ) {
        }
    }
}

fun Int.rgbToColor(): Color {
    return Color(0xff000000 or this.toLong())
}
