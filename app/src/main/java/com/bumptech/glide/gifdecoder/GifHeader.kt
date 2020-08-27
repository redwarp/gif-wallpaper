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
import com.bumptech.glide.gifdecoder.GifDecoder.GifDecodeStatus

/**
 * A header object containing the number of frames in an animated GIF image as well as basic
 * metadata like width and height that can be used to decode each individual frame of the GIF. Can
 * be shared by one or more [com.bumptech.glide.gifdecoder.GifDecoder]s to play the same
 * animated GIF in multiple views.
 *
 * @see [GIF 89a Specification](https://www.w3.org/Graphics/GIF/spec-gif89a.txt)
 */
class GifHeader {
    @JvmField
    @ColorInt
    var gct: IntArray? = null

    /**
     * Global status code of GIF data parsing.
     */
    @GifDecodeStatus
    var status = GifDecoder.STATUS_OK

    @JvmField
    var numFrames = 0

    @JvmField
    var currentFrame: GifFrame? = null

    @JvmField
    val frames: MutableList<GifFrame> = mutableListOf()

    /** Logical screen size: Full image width.  */
    @JvmField
    var width = 0

    /** Logical screen size: Full image height.  */
    @JvmField
    var height = 0

    // 1 : global color table flag.
    var gctFlag = false

    /**
     * Size of Global Color Table.
     * The value is already computed to be a regular number, this field doesn't store the exponent.
     */
    var gctSize = 0

    /** Background color index into the Global/Local color table.  */
    @JvmField
    var bgIndex = 0

    /**
     * Pixel aspect ratio.
     * Factor used to compute an approximation of the aspect ratio of the pixel in the original image.
     */
    var pixelAspect = 0

    @JvmField
    @ColorInt
    var bgColor = 0

    @JvmField
    var loopCount = NETSCAPE_LOOP_COUNT_DOES_NOT_EXIST

    companion object {
        /** The "Netscape" loop count which means loop forever.  */
        const val NETSCAPE_LOOP_COUNT_FOREVER = 0

        /** Indicates that this header has no "Netscape" loop count.  */
        const val NETSCAPE_LOOP_COUNT_DOES_NOT_EXIST = -1
    }
}
