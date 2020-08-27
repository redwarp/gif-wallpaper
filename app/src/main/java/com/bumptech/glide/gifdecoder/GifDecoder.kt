/*
 * Copyright 2014 Google, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *          conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *          of conditions and the following disclaimer in the documentation and/or other materials
 *          provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY GOOGLE, INC. ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL GOOGLE, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Google, Inc.
 */
package com.bumptech.glide.gifdecoder

import android.graphics.Bitmap
import androidx.annotation.IntDef
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Shared interface for GIF decoders.
 */
interface GifDecoder {
    /** Android Lint annotation for status codes that can be used with a GIF decoder.  */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [STATUS_OK, STATUS_FORMAT_ERROR, STATUS_OPEN_ERROR, STATUS_PARTIAL_DECODE])
    annotation class GifDecodeStatus

    /**
     * An interface that can be used to provide reused [android.graphics.Bitmap]s to avoid GCs
     * from constantly allocating [android.graphics.Bitmap]s for every frame.
     */
    interface BitmapProvider {
        /**
         * Returns an [Bitmap] with exactly the given dimensions and config.
         *
         * @param width The width in pixels of the desired [android.graphics.Bitmap].
         * @param height The height in pixels of the desired [android.graphics.Bitmap].
         * @param config The [android.graphics.Bitmap.Config] of the desired [               ].
         */
        fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap

        /**
         * Releases the given Bitmap back to the pool.
         */
        fun release(bitmap: Bitmap)

        /**
         * Returns a byte array used for decoding and generating the frame bitmap.
         *
         * @param size the size of the byte array to obtain
         */
        fun obtainByteArray(size: Int): ByteArray

        /**
         * Releases the given byte array back to the pool.
         */
        fun release(bytes: ByteArray)

        /**
         * Returns an int array used for decoding/generating the frame bitmaps.
         */
        fun obtainIntArray(size: Int): IntArray

        /**
         * Release the given array back to the pool.
         */
        fun release(array: IntArray)

        /**
         * Free up all memory.
         */
        fun flush()
    }

    val width: Int
    val height: Int
    val data: ByteBuffer

    /**
     * Returns the current status of the decoder.
     *
     *
     *  Status will update per frame to allow the caller to tell whether or not the current frame
     * was decoded successfully and/or completely. Format and open failures persist across frames.
     *
     */
    @get:GifDecodeStatus
    val status: Int

    /**
     * Move the animation frame counter forward.
     */
    fun advance()

    /**
     * Gets display duration for specified frame.
     *
     * @param n int index of frame.
     * @return delay in milliseconds.
     */
    fun getDelay(n: Int): Int

    /**
     * Gets display duration for the upcoming frame in ms.
     */
    val nextDelay: Int

    /**
     * Gets the number of frames read from file.
     *
     * @return frame count.
     */
    val frameCount: Int

    /**
     * Gets the current index of the animation frame, or -1 if animation hasn't not yet started.
     *
     * @return frame index.
     */
    val currentFrameIndex: Int

    /**
     * Resets the frame pointer to before the 0th frame, as if we'd never used this decoder to
     * decode any frames.
     */
    fun resetFrameIndex()

    /**
     * Gets the "Netscape" loop count, if any.
     * A count of 0 ([GifHeader.NETSCAPE_LOOP_COUNT_FOREVER]) means repeat indefinitely.
     * It must not be a negative value.
     * <br></br>
     * Use [.getTotalIterationCount]
     * to know how many times the animation sequence should be displayed.
     *
     * @return loop count if one was specified,
     * else -1 ([GifHeader.NETSCAPE_LOOP_COUNT_DOES_NOT_EXIST]).
     */
    val netscapeLoopCount: Int

    /**
     * Gets the total count
     * which represents how many times the animation sequence should be displayed.
     * A count of 0 ([.TOTAL_ITERATION_COUNT_FOREVER]) means repeat indefinitely.
     * It must not be a negative value.
     *
     *
     * The total count is calculated as follows by using [.getNetscapeLoopCount].
     * This behavior is the same as most web browsers.
     * <table border='1'>
     * <tr class='tableSubHeadingColor'><th>`getNetscapeLoopCount()`</th>
     * <th>The total count</th></tr>
     * <tr><td>[GifHeader.NETSCAPE_LOOP_COUNT_FOREVER]</td>
     * <td>[.TOTAL_ITERATION_COUNT_FOREVER]</td></tr>
     * <tr><td>[GifHeader.NETSCAPE_LOOP_COUNT_DOES_NOT_EXIST]</td>
     * <td>`1`</td></tr>
     * <tr><td>`n (n > 0)`</td>
     * <td>`n + 1`</td></tr>
     </table> *
     *
     *
     * @see [Discussion about
     * the iteration count of animated GIFs
     * @return total iteration count calculated from "Netscape" loop count.
     ](https://bugs.chromium.org/p/chromium/issues/detail?id=592735.c5) */
    val totalIterationCount: Int

    /**
     * Returns an estimated byte size for this decoder based on the data provided to [ ][.setData], as well as internal buffers.
     */
    val byteSize: Int

    /**
     * Get the next frame in the animation sequence.
     *
     * @return Bitmap representation of frame.
     */
    val nextFrame: Bitmap?

    /**
     * Reads GIF image from stream.
     *
     * @param `is` containing GIF file.
     * @return read status code (0 = no errors).
     */
    @GifDecodeStatus
    fun read(inputStream: InputStream, contentLength: Int): Int
    fun clear()
    fun setData(header: GifHeader, data: ByteArray)
    fun setData(header: GifHeader, buffer: ByteBuffer)
    fun setData(header: GifHeader, buffer: ByteBuffer, sampleSize: Int)

    /**
     * Reads GIF image from byte array.
     *
     * @param data containing GIF file.
     * @return read status code (0 = no errors).
     */
    @GifDecodeStatus
    fun read(data: ByteArray): Int

    /**
     * Sets the default [android.graphics.Bitmap.Config] to use when decoding frames of a GIF.
     *
     *
     * Valid options are [android.graphics.Bitmap.Config.ARGB_8888] and
     * [android.graphics.Bitmap.Config.RGB_565].
     * [android.graphics.Bitmap.Config.ARGB_8888] will produce higher quality frames, but will
     * also use 2x the memory of [android.graphics.Bitmap.Config.RGB_565].
     *
     *
     * Defaults to [android.graphics.Bitmap.Config.ARGB_8888]
     *
     *
     * This value is not a guarantee. For example if set to
     * [android.graphics.Bitmap.Config.RGB_565] and the GIF contains transparent pixels,
     * [android.graphics.Bitmap.Config.ARGB_8888] will be used anyway to support the
     * transparency.
     */
    fun setDefaultBitmapConfig(format: Bitmap.Config)

    companion object {
        /** File read status: No errors.  */
        const val STATUS_OK = 0

        /** File read status: Error decoding file (may be partially decoded).  */
        const val STATUS_FORMAT_ERROR = 1

        /** File read status: Unable to open source.  */
        const val STATUS_OPEN_ERROR = 2

        /** Unable to fully decode the current frame.  */
        const val STATUS_PARTIAL_DECODE = 3

        /** The total iteration count which means repeat forever.  */
        const val TOTAL_ITERATION_COUNT_FOREVER = 0
    }
}
