package net.redwarp.gifwallpaper.data

import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette

sealed class ColorInfo
object Calculating : ColorInfo()
class ColorScheme(@ColorInt val defaultColor: Int, val palette: Palette) : ColorInfo()