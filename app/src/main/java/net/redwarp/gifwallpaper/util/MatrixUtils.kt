/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.util

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

fun Matrix.setCenterInsideRectInRect(source: RectF, destination: RectF) {
    setCenterRectInRect(source, destination)
    if (source.width() <= destination.width() && source.height() <= destination.height()) {
        return
    }

    val sourceRatio = source.width() / source.height()
    val destinationRatio = destination.width() / destination.height()

    val scale = if (sourceRatio > destinationRatio) {
        destination.width() / source.width()
    } else {
        destination.height() / source.height()
    }
    postScale(scale, scale, destination.width() * .5f, destination.height() * 0.5f)
}
