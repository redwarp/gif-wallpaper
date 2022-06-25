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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import net.redwarp.gifwallpaper.R

@Composable
fun NoColorChoice() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_no_color),
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
fun ColorChoice(color: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.rgbToColor())
    ) {
    }
}

@Composable
fun ColorPicker(colors: List<Int>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
            Text(
                text = stringResource(id = R.string.pick_a_color),
                style = MaterialTheme.typography.subtitle1
            )
        }
        Divider()
        FlowRow(
            mainAxisSpacing = 16.dp,
            modifier = Modifier.padding(ButtonDefaults.ContentPadding)
        ) {
            NoColorChoice()
            for (color in colors) {
                ColorChoice(color = color)
            }
        }
    }
}

@Preview
@Composable
fun ColorPickerPreview() {
    AppTheme {
        ColorPicker(colors = listOf(0xffffff, 0xff0000, 0x00ff00, 0x0000ff, 0x000000))
    }
}

fun Int.rgbToColor(): Color {
    return Color(0xff000000 or this.toLong())
}
