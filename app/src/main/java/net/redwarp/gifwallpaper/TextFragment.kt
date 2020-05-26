/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.noties.markwon.Markwon
import java.io.InputStream
import kotlinx.android.synthetic.main.fragment_text.*

private const val KEY_MARKDOWN_FILENAME = "markdown_filename"

class TextFragment : Fragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)

        val nightModeFlags = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> setStatusBarColor(true)
            else -> setStatusBarColor(false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireArguments().getString(KEY_MARKDOWN_FILENAME)?.let {
            val content = loadMarkdownFile(it) ?: return@let

            Markwon.create(view.context).setMarkdown(textView, content)
        }
        textView.setOnApplyWindowInsetsListener { v, insets ->
            textView.y = insets.systemWindowInsetTop.toFloat()
            insets
        }
    }

    private fun loadMarkdownFile(markdownFileName: String): String? {
        val url: InputStream? =
            TextFragment::class.java.classLoader?.getResourceAsStream(markdownFileName)
        val content: String?
        if (url != null) {
            content = url.reader().readText()
            url.close()
        } else {
            content = null
        }
        return content
    }

    private fun setStatusBarColor(isDark: Boolean) {
        activity?.window?.apply {
            if (isDark) {
                decorView.systemUiVisibility =
                    decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                decorView.systemUiVisibility =
                    decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    companion object {
        fun newInstance(markdownFileName: String): TextFragment {
            return TextFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_MARKDOWN_FILENAME, markdownFileName)
                }
            }
        }
    }
}
