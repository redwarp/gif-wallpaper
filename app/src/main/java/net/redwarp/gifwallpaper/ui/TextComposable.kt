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

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import io.noties.markwon.Markwon
import java.io.InputStream

@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val markwon = remember {
        Markwon.create(context)
    }

    AndroidView(modifier = modifier, factory = { ctx ->
        TextView(ctx)
    }, update = {
            markwon.setMarkdown(it, text)
        })
}

@Composable
fun MarkdownUi(title: String, fileName: String, navController: NavController) {
    val context = LocalContext.current

    val text = remember {
        context.loadMarkdownFile(fileName) ?: ""
    }

    MarkdownPage(title = title, markdownText = text, navController = navController)
}

@Composable
fun MarkdownPage(title: String, markdownText: String, navController: NavController) {
    Scaffold(topBar = {
        BasicTopBar(title = title, navController = navController)
    }) {
        MarkdownText(
            text = markdownText,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun MarkdownUiPreview() {
    val context = LocalContext.current
    val navController = NavController(context)
    AppTheme {
        MarkdownPage(
            title = "About",
            markdownText = """
            # About
            
            So, this is how the page will look like?

            ## Check the sources

            Sources are available on [github](https://github.com/redwarp/gif-wallpaper).

            Head there if you have feature requests or so.
            """.trimIndent(),
            navController = navController
        )
    }
}

private fun Context.loadMarkdownFile(markdownFileName: String): String? {
    val url: InputStream? = this::class.java.classLoader?.getResourceAsStream(markdownFileName)
    val content: String?
    if (url != null) {
        content = url.reader().readText()
        url.close()
    } else {
        content = null
    }
    return content
}
