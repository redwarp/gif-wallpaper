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

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateDecelerateInterpolator
import net.redwarp.gifwallpaper.util.MatrixEvaluator
import net.redwarp.gifwallpaper.util.setCenterCropRectInRect
import net.redwarp.gifwallpaper.util.setCenterRectInRect

private const val ANIMATION_DURATION = 400L

class GifWrapperDrawable(
    drawable: Drawable,
    backgroundColor: Int = 0,
    scaleType: ScaleType = ScaleType.FIT_CENTER,
    rotation: Rotation = Rotation.NORTH,
    translation: Pair<Float, Float> = 0f to 0f,
    shouldPlay: Boolean = false
) : Drawable(), Animatable {
    private val state =
        GifWrapperState(drawable, backgroundColor, scaleType, rotation, translation, shouldPlay)

    private val paint = Paint().apply {
        color = state.backgroundColor
        style = Paint.Style.FILL
    }
    private val matrix = Matrix()
    private val workArray = FloatArray(2)
    private var matrixAnimator: ValueAnimator? = null
    private val animationInterpolator = AccelerateDecelerateInterpolator()

    private val chainingCallback: Callback = object : Callback {
        override fun invalidateDrawable(who: Drawable) {
            invalidateSelf()
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            scheduleSelf(what, `when`)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            unscheduleSelf(what)
        }
    }

    init {
        drawable.callback = chainingCallback
        updateMatrixAndInvalidate()
    }

    var shouldPlay: Boolean
        get() = state.shouldPlay
        set(value) {
            state.shouldPlay = value
            if (isRunning && !shouldPlay) {
                stop()
            } else if (!isRunning && shouldPlay) {
                start()
            }
        }

    fun setBackgroundColor(color: Int) {
        state.backgroundColor = color
        paint.color = color
        invalidateSelf()
    }

    fun setScaledType(scaleType: ScaleType, animated: Boolean) {
        this.state.scaleType = scaleType
        if (animated) {
            transformMatrix()
        } else {
            updateMatrixAndInvalidate()
        }
    }

    fun setRotation(rotation: Rotation, animated: Boolean) {
        this.state.rotation = rotation
        if (animated) {
            transformMatrix()
        } else {
            updateMatrixAndInvalidate()
        }
    }

    fun setTranslate(translateX: Float, translateY: Float, animated: Boolean) {
        this.state.translation = translateX to translateY

        if (animated) {
            transformMatrix()
        } else {
            updateMatrixAndInvalidate()
        }
    }

    fun postTranslate(translateX: Float, translateY: Float) {
        matrixAnimator?.cancel()

        state.translation =
            (state.translation.first + translateX) to (state.translation.second + translateY)
        matrix.postTranslate(translateX, translateY)
        invalidateSelf()
    }

    fun resetTranslation(animated: Boolean) {
        state.translation = 0f to 0f

        if (animated) {
            transformMatrix()
        } else {
            updateMatrixAndInvalidate()
        }
    }

    override fun start() {
        if (shouldPlay) {
            (state.drawable as? Animatable)?.start()
        }
    }

    override fun stop() {
        (state.drawable as? Animatable)?.stop()
    }

    override fun isRunning(): Boolean {
        return (state.drawable as? Animatable)?.isRunning ?: false
    }

    override fun draw(canvas: Canvas) {
        if (state.backgroundColor == Color.TRANSPARENT) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        } else {
            canvas.drawPaint(paint)
        }
        val checkpoint = canvas.save()
        canvas.concat(matrix)
        state.drawable.draw(canvas)
        canvas.restoreToCount(checkpoint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        state.drawable.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        state.drawable.colorFilter = colorFilter
    }

    @Deprecated("This method is no longer used in graphics optimizations", ReplaceWith(""))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int {
        return state.drawable.intrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return state.drawable.intrinsicHeight
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)

        updateMatrixAndInvalidate()
    }

    override fun getConstantState(): ConstantState = state

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        return state.drawable.setVisible(visible, restart)
    }

    private fun Rect.toRectF(): RectF =
        RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

    private fun computeMatrix(
        matrix: Matrix,
        scaleType: ScaleType,
        rotation: Rotation,
        canvasRect: RectF,
        gifRect: RectF,
        translation: Pair<Float, Float>
    ) {
        when (scaleType) {
            ScaleType.FIT_CENTER ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.CENTER)
            ScaleType.FIT_END ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.END)
            ScaleType.FIT_START ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.START)
            ScaleType.FIT_XY ->
                matrix.setRectToRect(gifRect, canvasRect, Matrix.ScaleToFit.FILL)
            ScaleType.CENTER ->
                matrix.setCenterRectInRect(gifRect, canvasRect)
            ScaleType.CENTER_CROP ->
                matrix.setCenterCropRectInRect(gifRect, canvasRect)
        }
        workArray[0] = gifRect.centerX()
        workArray[1] = gifRect.centerY()
        matrix.mapPoints(workArray)
        matrix.postRotate(rotation.angle, workArray[0], workArray[1])

        if (rotation == Rotation.EAST || rotation == Rotation.WEST) {
            when (scaleType) {
                ScaleType.FIT_CENTER -> {
                    val scale = gifRect.width() / gifRect.height()
                    matrix.postScale(scale, scale, workArray[0], workArray[1])
                }
                ScaleType.FIT_XY -> {
                    val scale = canvasRect.width() / canvasRect.height()
                    matrix.postScale(scale, 1f / scale, workArray[0], workArray[1])
                }
                else -> Unit
            }
        }
        val (translateX, translateY) = translation
        matrix.postTranslate(translateX, translateY)
    }

    private fun updateMatrixAndInvalidate() {
        matrixAnimator?.cancel()
        computeMatrix(
            matrix = matrix,
            scaleType = state.scaleType,
            rotation = state.rotation,
            canvasRect = bounds.toRectF(),
            gifRect = state.drawable.bounds.toRectF(),
            translation = state.translation
        )
        invalidateSelf()
    }

    private fun transformMatrix() {
        val targetMatrix = Matrix().also {
            computeMatrix(
                matrix = it,
                scaleType = state.scaleType,
                rotation = state.rotation,
                canvasRect = bounds.toRectF(),
                gifRect = state.drawable.bounds.toRectF(),
                translation = 0f to 0f
            )
        }
        val sourceMatrix = Matrix(matrix)

        matrixAnimator?.cancel()
        matrixAnimator = ValueAnimator.ofObject(
            MatrixEvaluator(matrix),
            sourceMatrix,
            targetMatrix
        ).apply {
            addUpdateListener { invalidateSelf() }
            interpolator = animationInterpolator
            duration = ANIMATION_DURATION
            start()
        }
    }

    private class GifWrapperState(
        val drawable: Drawable,
        var backgroundColor: Int,
        var scaleType: ScaleType = ScaleType.FIT_CENTER,
        var rotation: Rotation = Rotation.NORTH,
        var translation: Pair<Float, Float> = 0f to 0f,
        var shouldPlay: Boolean,
    ) : ConstantState() {
        init {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }

        override fun newDrawable(): Drawable {
            val copiedDrawable = drawable.constantState?.newDrawable() ?: drawable.mutate()

            return GifWrapperDrawable(
                copiedDrawable,
                backgroundColor,
                scaleType,
                rotation,
                translation,
                shouldPlay
            ).also {
                it.setBackgroundColor(backgroundColor)
            }
        }

        override fun getChangingConfigurations(): Int =
            drawable.constantState?.changingConfigurations ?: 0
    }
}
