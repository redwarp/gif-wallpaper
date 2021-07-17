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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.noties.markwon.Markwon
import net.redwarp.gifwallpaper.databinding.FragmentTextBinding
import java.io.InputStream

private const val KEY_MARKDOWN_FILENAME = "markdown_filename"

class TextFragment : Fragment() {
    private lateinit var binding: FragmentTextBinding

    override fun onResume() {
        super.onResume()
        // setToolbarPosition(ToolbarPosition.TopOf)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // val topPadding = binding.textView.paddingTop
        // binding.textView.setOnApplyWindowInsetsListener { textView, insets ->
        //     textView.updatePadding(
        //         top = insets.systemWindowInsetCompatTop + topPadding,
        //         bottom = insets.systemWindowInsetCompatBottom + topPadding
        //     )
        //     insets
        // }

        // val nightModeFlags = resources.configuration.uiMode and
        //     Configuration.UI_MODE_NIGHT_MASK
        // when (nightModeFlags) {
        //     Configuration.UI_MODE_NIGHT_YES -> setStatusBarColor(true)
        //     else -> setStatusBarColor(false)
        // }

        arguments?.getString(KEY_MARKDOWN_FILENAME)?.let {
            val content = loadMarkdownFile(it) ?: return@let

            Markwon.create(requireContext()).setMarkdown(binding.textView, content)
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
        fun newInstance(markdownFileName: String) = TextFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_MARKDOWN_FILENAME, markdownFileName)
            }
        }
    }
}
