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
package net.redwarp.gifwallpaper.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import kotlin.math.sqrt

private const val MAX_BITMAP_SIZE = 112
private const val MAX_WALLPAPER_EXTRACTION_AREA = MAX_BITMAP_SIZE * MAX_BITMAP_SIZE

fun Drawable.createMiniature(): Bitmap? {
    if (bounds.width() < 1 || bounds.height() < 1) return null
    val copy = constantState?.newDrawable() ?: return null

    val miniBounds = calculateOptimalBounds(bounds.width(), bounds.height())

    copy.bounds = miniBounds

    val bitmap =
        Bitmap.createBitmap(miniBounds.width(), miniBounds.height(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    copy.draw(canvas)

    return bitmap
}

/**
 * Copied from [android.app.WallpaperColors], compute an optimal size for the miniature
 * being created.
 */
private fun calculateOptimalBounds(width: Int, height: Int): Rect {
    // Calculate how big the bitmap needs to be.
    // This avoids unnecessary processing and allocation inside Palette.
    val requestedArea = width * height
    var scale = 1.0
    if (requestedArea > MAX_WALLPAPER_EXTRACTION_AREA) {
        scale = sqrt(MAX_WALLPAPER_EXTRACTION_AREA / requestedArea.toDouble())
    }
    var newWidth = (width * scale).toInt()
    var newHeight = (height * scale).toInt()
    // Dealing with edge cases of the drawable being too wide or too tall.
    // Width or height would end up being 0, in this case we'll set it to 1.
    if (newWidth == 0) {
        newWidth = 1
    }
    if (newHeight == 0) {
        newHeight = 1
    }
    return Rect(0, 0, newWidth, newHeight)
}
