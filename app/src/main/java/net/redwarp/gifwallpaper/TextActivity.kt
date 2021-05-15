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

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.noties.markwon.Markwon
import net.redwarp.gifwallpaper.databinding.ActivityTextBinding
import net.redwarp.gifwallpaper.util.setStatusBarColor
import net.redwarp.gifwallpaper.util.systemWindowInsetCompatTop
import java.io.InputStream

private const val KEY_MARKDOWN_FILENAME = "markdown_filename"

class TextActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.textView.setOnApplyWindowInsetsListener { _, insets ->
            binding.textView.y = insets.systemWindowInsetCompatTop.toFloat()
            insets
        }

        val nightModeFlags = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> setStatusBarColor(true)
            else -> setStatusBarColor(false)
        }

        intent.getStringExtra(KEY_MARKDOWN_FILENAME)?.let {
            val content = loadMarkdownFile(it) ?: return@let

            Markwon.create(this).setMarkdown(binding.textView, content)
        }
    }

    private fun loadMarkdownFile(markdownFileName: String): String? {
        val url: InputStream? =
            this::class.java.classLoader?.getResourceAsStream(markdownFileName)
        val content: String?
        if (url != null) {
            content = url.reader().readText()
            url.close()
        } else {
            content = null
        }
        return content
    }

    companion object {
        fun getIntent(context: Context, markdownFileName: String): Intent {
            return Intent(context, TextActivity::class.java).apply {
                putExtra(KEY_MARKDOWN_FILENAME, markdownFileName)
            }
        }
    }
}
