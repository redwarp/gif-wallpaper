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

import android.animation.FloatArrayEvaluator
import android.animation.TypeEvaluator
import android.graphics.Matrix

/**
 * This class is used to interpolate between two matrices and animate the transition.
 **/
class MatrixEvaluator(reuseMatrix: Matrix) : TypeEvaluator<Matrix> {
    private val startData = FloatArray(9)
    private val endData = FloatArray(9)
    private val workMatrix = reuseMatrix
    private val floatArrayEvaluator = FloatArrayEvaluator(FloatArray(9))

    override fun evaluate(fraction: Float, startValue: Matrix, endValue: Matrix): Matrix {
        startValue.getValues(startData)
        endValue.getValues(endData)

        workMatrix.setValues(
            floatArrayEvaluator.evaluate(fraction, startData, endData),
        )

        return workMatrix
    }
}
