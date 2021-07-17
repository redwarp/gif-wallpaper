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
package net.redwarp.gifwallpaper.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import net.redwarp.gifwallpaper.R

fun FragmentActivity.setStatusBarColor(isDark: Boolean) {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            setStatusBarColor30(isDark)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            setStatusBarColor23(isDark)
        }
        else -> {
            setStatusBarColor21()
        }
    }
}

fun Fragment.setStatusBarColor(isDark: Boolean) {
    activity?.setStatusBarColor(isDark)
}

fun FragmentActivity.setStatusBarColor21() {
    window?.apply {
        statusBarColor = context.themeColor(R.attr.colorPrimaryDark)
    }
}

@Suppress("DEPRECATION")
@RequiresApi(Build.VERSION_CODES.M)
fun FragmentActivity.setStatusBarColor23(isDark: Boolean) {
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

@RequiresApi(Build.VERSION_CODES.R)
fun FragmentActivity.setStatusBarColor30(isDark: Boolean) {
    window?.apply {
        if (isDark) {
            insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }
}

val Context.isDarkMode: Boolean
    get() {
        val nightModeFlags = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        return when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

fun Fragment.setToolbarPosition(toolbarPosition: ToolbarPosition) {
    val activity = requireActivity()
    val containerView = activity.findViewById<FragmentContainerView>(R.id.nav_host_fragment)
    val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)
    when (toolbarPosition) {
        ToolbarPosition.Overlay -> containerView.updatePadding(top = 0)
        ToolbarPosition.TopOf -> containerView.updatePadding(top = toolbar.bottom)
        ToolbarPosition.Invisible -> containerView.updatePadding(top = 0)
    }
    containerView.updatePadding(top = toolbar.bottom)
}

enum class ToolbarPosition {
    Overlay, TopOf, Invisible
}
