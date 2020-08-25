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

import androidx.annotation.ColorInt
import androidx.annotation.IntDef

/**
 * Inner model class housing metadata for each frame.
 *
 * @see [GIF 89a Specification](https://www.w3.org/Graphics/GIF/spec-gif89a.txt)
 */
class GifFrame {
    /**
     *
     * **GIF89a**:
     * *Indicates the way in which the graphic is to be treated after being displayed.*
     * Disposal methods 0-3 are defined, 4-7 are reserved for future use.
     *
     * @see [DISPOSAL_UNSPECIFIED]
     * @see [DISPOSAL_NONE]
     * @see [DISPOSAL_BACKGROUND]
     * @see [DISPOSAL_PREVIOUS]
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [DISPOSAL_UNSPECIFIED, DISPOSAL_NONE, DISPOSAL_BACKGROUND, DISPOSAL_PREVIOUS])
    private annotation class GifDisposalMethod

    @JvmField
    var ix = 0

    @JvmField
    var iy = 0

    @JvmField
    var iw = 0

    @JvmField
    var ih = 0

    /**
     * Control Flag.
     */
    @JvmField
    var interlace = false

    /**
     * Control Flag.
     */
    @JvmField
    var transparency = false

    /**
     * Disposal Method.
     */
    @JvmField
    @GifDisposalMethod
    var dispose = 0

    /**
     * Transparency Index.
     */
    @JvmField
    var transIndex = 0

    /**
     * Delay, in milliseconds, to next frame.
     */
    @JvmField
    var delay = 0

    /**
     * Index in the raw buffer where we need to start reading to decode.
     */
    @JvmField
    var bufferFrameStart = 0

    /**
     * Local Color Table.
     */
    @JvmField
    @ColorInt
    var lct: IntArray? = null

    companion object {
        /**
         * GIF Disposal Method meaning take no action.
         *
         * **GIF89a**: *No disposal specified.
         * The decoder is not required to take any action.*
         */
        const val DISPOSAL_UNSPECIFIED = 0

        /**
         * GIF Disposal Method meaning leave canvas from previous frame.
         *
         * **GIF89a**: *Do not dispose.
         * The graphic is to be left in place.*
         */
        const val DISPOSAL_NONE = 1

        /**
         * GIF Disposal Method meaning clear canvas to background color.
         *
         * **GIF89a**: *Restore to background color.
         * The area used by the graphic must be restored to the background color.*
         */
        const val DISPOSAL_BACKGROUND = 2

        /**
         * GIF Disposal Method meaning clear canvas to frame before last.
         *
         * **GIF89a**: *Restore to previous.
         * The decoder is required to restore the area overwritten by the graphic
         * with what was there prior to rendering the graphic.*
         */
        const val DISPOSAL_PREVIOUS = 3
    }
}
