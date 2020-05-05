package net.redwarp.gifwallpaper

import android.animation.FloatArrayEvaluator
import android.animation.TypeEvaluator
import android.graphics.Matrix

class MatrixEvaluator(reuseMatrix: Matrix?) : TypeEvaluator<Matrix> {
    private val startData = FloatArray(9)
    private val endData = FloatArray(9)
    private val workMatrix = reuseMatrix
    private val floatArrayEvaluator = FloatArrayEvaluator(FloatArray(9))

    override fun evaluate(fraction: Float, startValue: Matrix, endValue: Matrix): Matrix {
        startValue.getValues(startData)
        endValue.getValues(endData)

        val matrix = workMatrix ?: Matrix()
        matrix.setValues(
            floatArrayEvaluator.evaluate(fraction, startData, endData)
        )

        return matrix
    }
}

