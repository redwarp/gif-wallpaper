package net.redwarp.gifwallpaper

import android.graphics.Matrix
import android.graphics.RectF

fun Matrix.setCenterRectInRect(source: RectF, destination: RectF) {
    reset()
    postTranslate(
        (destination.width() - source.width()) * 0.5f,
        (destination.height() - source.height()) * 0.5f
    )
}