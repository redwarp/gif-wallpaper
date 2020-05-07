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

fun Matrix.setCenterCropRectInRect(source: RectF, destination: RectF) {
    setCenterRectInRect(source, destination)
    val sourceRatio = source.width() / source.height()
    val destinationRatio = destination.width() / destination.height()

    val scale = if (sourceRatio > destinationRatio) {
        destination.height() / source.height()
    } else {
        destination.width() / source.width()
    }
    postScale(scale, scale, destination.width() * .5f, destination.height() * 0.5f)
}