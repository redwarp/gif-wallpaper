/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import io.noties.markwon.Markwon
import java.io.InputStream
import kotlinx.android.synthetic.main.activity_text.*

private const val KEY_MARKDOWN_FILENAME = "markdown_filename"

class TextActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_text)
        textView.setOnApplyWindowInsetsListener { v, insets ->
            textView.y = insets.systemWindowInsetTop.toFloat()
            insets
        }
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

            statusBarColor = Color.TRANSPARENT
        }

        val nightModeFlags = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> setStatusBarColor(true)
            else -> setStatusBarColor(false)
        }

        intent.getStringExtra(KEY_MARKDOWN_FILENAME)?.let {
            val content = loadMarkdownFile(it) ?: return@let

            Markwon.create(this).setMarkdown(textView, content)
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

    private fun setStatusBarColor(isDark: Boolean) {
        window?.apply {
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
        fun getIntent(context: Context, markdownFileName: String): Intent {
            return Intent(context, TextActivity::class.java).apply {
                putExtra(KEY_MARKDOWN_FILENAME, markdownFileName)
            }
        }
    }
}
