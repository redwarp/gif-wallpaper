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
