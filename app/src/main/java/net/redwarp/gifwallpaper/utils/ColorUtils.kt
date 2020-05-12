/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils

fun Int.isDark(): Boolean {
    return ColorUtils.calculateLuminance(this) < 0.5
}

@SuppressLint("Recycle")
@ColorInt
fun Context.themeColor(
    @AttrRes themeAttrId: Int
): Int {
    return obtainStyledAttributes(
        intArrayOf(themeAttrId)
    ).use {
        it.getColor(0, Color.MAGENTA)
    }
}
