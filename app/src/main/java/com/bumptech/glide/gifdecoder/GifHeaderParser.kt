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

import android.util.Log
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import kotlin.math.pow

/**
 * A class responsible for creating [com.bumptech.glide.gifdecoder.GifHeader]s from data
 * representing animated GIFs.
 *
 * @see [GIF 89a Specification](https://www.w3.org/Graphics/GIF/spec-gif89a.txt)
 */
class GifHeaderParser(data: ByteArray) {
    // Raw data read working array.
    private val block = ByteArray(MAX_BLOCK_SIZE)
    private var rawData: ByteBuffer?
    private var header = GifHeader()
    private var blockSize = 0

    private fun reset() {
        rawData = null
        Arrays.fill(block, 0.toByte())
        header = GifHeader()
        blockSize = 0
    }

    fun parseHeader(): GifHeader {
        checkNotNull(rawData) { "You must call setData() before parseHeader()" }
        if (err()) {
            return header
        }
        readHeader()
        if (!err()) {
            readContents()
            if (header.numFrames < 0) {
                header.status = GifDecoder.STATUS_FORMAT_ERROR
            }
        }
        return header
    } /* maxFrames */

    /**
     * Determines if the GIF is animated by trying to read in the first 2 frames
     * This method re-parses the data even if the header has already been read.
     */
    val isAnimated: Boolean
        get() {
            readHeader()
            if (!err()) {
                readContents(2 /* maxFrames */)
            }
            return header.numFrames > 1
        }
    /**
     * Main file parser. Reads GIF content blocks. Stops after reading maxFrames
     */
    /**
     * Main file parser. Reads GIF content blocks.
     */
    private fun readContents(maxFrames: Int = Int.MAX_VALUE) {
        // Read GIF file content blocks.
        var done = false
        while (!(done || err() || header.numFrames > maxFrames)) {
            when (read()) {
                IMAGE_SEPARATOR -> {
                    // The Graphic Control Extension is optional, but will always come first if it exists.
                    // If one did exist, there will be a non-null current frame which we should use.
                    // However if one did not exist, the current frame will be null
                    // and we must create it here. See issue #134.
                    readBitmap(
                        header.currentFrame ?: run {
                            GifFrame().also(header::currentFrame::set)
                        }
                    )
                }
                EXTENSION_INTRODUCER -> {
                    when (read()) {
                        LABEL_GRAPHIC_CONTROL_EXTENSION -> {
                            // Start a new frame.
                            header.currentFrame = GifFrame()
                            readGraphicControlExt(header.currentFrame!!)
                        }
                        LABEL_APPLICATION_EXTENSION -> {
                            readBlock()
                            val app = StringBuilder()
                            var i = 0
                            while (i < 11) {
                                app.append(block[i].toChar())
                                i++
                            }
                            if (app.toString() == "NETSCAPE2.0") {
                                readNetscapeExt()
                            } else {
                                // Don't care.
                                skip()
                            }
                        }
                        LABEL_COMMENT_EXTENSION -> skip()
                        LABEL_PLAIN_TEXT_EXTENSION -> skip()
                        else -> // Uninteresting extension.
                            skip()
                    }
                }
                TRAILER -> // This block is a single-field block indicating the end of the GIF Data Stream.
                    done = true
                0x00 -> header.status = GifDecoder.STATUS_FORMAT_ERROR
                else -> header.status = GifDecoder.STATUS_FORMAT_ERROR
            }
        }
    }

    /**
     * Reads Graphic Control Extension values.
     */
    private fun readGraphicControlExt(currentFrame: GifFrame) {
        // Block size.
        read()
        /*
         * Graphic Control Extension packed field:
         *      7 6 5 4 3 2 1 0
         *     +---------------+
         *  1  |     |     | | |
         *
         * Reserved                    3 Bits
         * Disposal Method             3 Bits
         * User Input Flag             1 Bit
         * Transparent Color Flag      1 Bit
         */
        val packed = read()
        // Disposal method.
        currentFrame.dispose = packed and GCE_MASK_DISPOSAL_METHOD shr GCE_DISPOSAL_METHOD_SHIFT
        if (currentFrame.dispose == GifFrame.DISPOSAL_UNSPECIFIED) {
            // Elect to keep old image if discretionary.
            currentFrame.dispose = GifFrame.DISPOSAL_NONE
        }
        currentFrame.transparency = packed and GCE_MASK_TRANSPARENT_COLOR_FLAG != 0
        // Delay in milliseconds.
        var delayInHundredthsOfASecond = readShort()
        // TODO: consider allowing -1 to indicate show forever.
        if (delayInHundredthsOfASecond < MIN_FRAME_DELAY) {
            delayInHundredthsOfASecond = DEFAULT_FRAME_DELAY
        }
        currentFrame.delay = delayInHundredthsOfASecond * 10
        // Transparent color index
        currentFrame.transIndex = read()
        // Block terminator
        read()
    }

    /**
     * Reads next frame image.
     */
    private fun readBitmap(inFrame: GifFrame) {
        // (sub)image position & size.
        inFrame.ix = readShort()
        inFrame.iy = readShort()
        inFrame.iw = readShort()
        inFrame.ih = readShort()

        /*
         * Image Descriptor packed field:
         *     7 6 5 4 3 2 1 0
         *    +---------------+
         * 9  | | | |   |     |
         *
         * Local Color Table Flag     1 Bit
         * Interlace Flag             1 Bit
         * Sort Flag                  1 Bit
         * Reserved                   2 Bits
         * Size of Local Color Table  3 Bits
         */
        val packed = read()
        val lctFlag = packed and DESCRIPTOR_MASK_LCT_FLAG != 0
        val lctSize = 2.0.pow((packed and DESCRIPTOR_MASK_LCT_SIZE) + 1.toDouble()).toInt()
        inFrame.interlace = packed and DESCRIPTOR_MASK_INTERLACE_FLAG != 0
        if (lctFlag) {
            inFrame.lct = readColorTable(lctSize)
        } else {
            // No local color table.
            inFrame.lct = null
        }

        // Save this as the decoding position pointer.
        inFrame.bufferFrameStart = rawData!!.position()

        // False decode pixel data to advance buffer.
        skipImageData()
        if (err()) {
            return
        }
        header.numFrames++
        // Add image to frame.
        header.frames.add(inFrame)
    }

    /**
     * Reads Netscape extension to obtain iteration count.
     */
    private fun readNetscapeExt() {
        do {
            readBlock()
            if (block[0] == 1.toByte()) {
                // Loop count sub-block.
                val b1 = block[1].toInt() and MASK_INT_LOWEST_BYTE
                val b2 = block[2].toInt() and MASK_INT_LOWEST_BYTE
                header.loopCount = b2 shl 8 or b1
            }
        } while (blockSize > 0 && !err())
    }

    /**
     * Reads GIF file header information.
     */
    private fun readHeader() {
        val id = StringBuilder()
        for (i in 0..5) {
            id.append(read().toChar())
        }
        if (!id.toString().startsWith("GIF")) {
            header.status = GifDecoder.STATUS_FORMAT_ERROR
            return
        }
        readLSD()
        if (header.gctFlag && !err()) {
            header.gct = readColorTable(header.gctSize)
            header.bgColor = header.gct!![header.bgIndex]
        }
    }

    /**
     * Reads Logical Screen Descriptor.
     */
    private fun readLSD() {
        // Logical screen size.
        header.width = readShort()
        header.height = readShort()
        /*
         * Logical Screen Descriptor packed field:
         *      7 6 5 4 3 2 1 0
         *     +---------------+
         *  4  | |     | |     |
         *
         * Global Color Table Flag     1 Bit
         * Color Resolution            3 Bits
         * Sort Flag                   1 Bit
         * Size of Global Color Table  3 Bits
         */
        val packed = read()
        header.gctFlag = packed and LSD_MASK_GCT_FLAG != 0
        header.gctSize = 2.0.pow((packed and LSD_MASK_GCT_SIZE) + 1.toDouble())
            .toInt()
        // Background color index.
        header.bgIndex = read()
        // Pixel aspect ratio
        header.pixelAspect = read()
    }

    /**
     * Reads color table as 256 RGB integer values.
     *
     * @param nColors int number of colors to read.
     * @return int array containing 256 colors (packed ARGB with full alpha).
     */
    private fun readColorTable(nColors: Int): IntArray? {
        val nBytes = 3 * nColors
        var tab: IntArray? = null
        val c = ByteArray(nBytes)
        try {
            rawData!![c]

            // TODO: what bounds checks are we avoiding if we know the number of colors?
            // Max size to avoid bounds checks.
            tab = IntArray(MAX_BLOCK_SIZE)
            var i = 0
            var j = 0
            while (i < nColors) {
                val r = c[j++].toInt() and MASK_INT_LOWEST_BYTE
                val g = c[j++].toInt() and MASK_INT_LOWEST_BYTE
                val b = c[j++].toInt() and MASK_INT_LOWEST_BYTE
                tab[i++] = -0x1000000 or (r shl 16) or (g shl 8) or b
            }
        } catch (e: BufferUnderflowException) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Format Error Reading Color Table", e)
            }
            header.status = GifDecoder.STATUS_FORMAT_ERROR
        }
        return tab
    }

    /**
     * Skips LZW image data for a single frame to advance buffer.
     */
    private fun skipImageData() {
        // lzwMinCodeSize
        read()
        // data sub-blocks
        skip()
    }

    /**
     * Skips variable length blocks up to and including next zero length block.
     */
    private fun skip() {
        var blockSize: Int
        do {
            blockSize = read()
            val newPosition = Math.min(rawData!!.position() + blockSize, rawData!!.limit())
            rawData!!.position(newPosition)
        } while (blockSize > 0)
    }

    /**
     * Reads next variable length block from input.
     */
    private fun readBlock() {
        blockSize = read()
        var n = 0
        if (blockSize > 0) {
            var count = 0
            try {
                while (n < blockSize) {
                    count = blockSize - n
                    rawData!![block, n, count]
                    n += count
                }
            } catch (e: Exception) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(
                        TAG,
                        "Error Reading Block n: $n count: $count blockSize: $blockSize", e
                    )
                }
                header.status = GifDecoder.STATUS_FORMAT_ERROR
            }
        }
    }

    /**
     * Reads a single byte from the input stream.
     */
    private fun read(): Int {
        var currByte = 0
        try {
            currByte = rawData!!.get().toInt() and MASK_INT_LOWEST_BYTE
        } catch (e: Exception) {
            header.status = GifDecoder.STATUS_FORMAT_ERROR
        }
        return currByte
    }

    /**
     * Reads next 16-bit value, LSB first.
     */
    private fun readShort(): Int {
        // Read 16-bit value.
        return rawData!!.short.toInt()
    }

    private fun err(): Boolean {
        return header.status != GifDecoder.STATUS_OK
    }

    companion object {
        private const val TAG = "GifHeaderParser"
        private const val MASK_INT_LOWEST_BYTE = 0x000000FF

        /**
         * Identifies the beginning of an Image Descriptor.
         */
        private const val IMAGE_SEPARATOR = 0x2C

        /**
         * Identifies the beginning of an extension block.
         */
        private const val EXTENSION_INTRODUCER = 0x21

        /**
         * This block is a single-field block indicating the end of the GIF Data Stream.
         */
        private const val TRAILER = 0x3B

        // Possible labels that identify the current extension block.
        private const val LABEL_GRAPHIC_CONTROL_EXTENSION = 0xF9
        private const val LABEL_APPLICATION_EXTENSION = 0xFF
        private const val LABEL_COMMENT_EXTENSION = 0xFE
        private const val LABEL_PLAIN_TEXT_EXTENSION = 0x01
        // Graphic Control Extension packed field masks
        /**
         * Mask (bits 4-2) to extract Disposal Method of the current frame.
         *
         * @see GifFrame.GifDisposalMethod possible values
         */
        private const val GCE_MASK_DISPOSAL_METHOD = 28

        /**
         * Shift so the Disposal Method extracted from the packed value is on the least significant bit.
         */
        private const val GCE_DISPOSAL_METHOD_SHIFT = 2

        /**
         * Mask (bit 0) to extract Transparent Color Flag of the current frame.
         *
         * **GIF89a**: *Indicates whether a transparency index is given
         * in the Transparent Index field.*
         * Possible values are:
         *  * 0 - Transparent Index is not given.
         *  * 1 - Transparent Index is given.
         *
         */
        private const val GCE_MASK_TRANSPARENT_COLOR_FLAG = 1
        // Image Descriptor packed field masks (describing Local Color Table)
        /**
         * Mask (bit 7) to extract Local Color Table Flag of the current image.
         *
         * **GIF89a**: *Indicates the presence of a Local Color Table
         * immediately following this Image Descriptor.*
         */
        private const val DESCRIPTOR_MASK_LCT_FLAG = 128

        /**
         * Mask (bit 6) to extract Interlace Flag of the current image.
         *
         * **GIF89a**: *Indicates if the image is interlaced.
         * An image is interlaced in a four-pass interlace pattern.*
         * Possible values are:
         *  * 0 - Image is not interlaced.
         *  * 1 - Image is interlaced.
         *
         */
        private const val DESCRIPTOR_MASK_INTERLACE_FLAG = 64

        /**
         * Mask (bits 2-0) to extract Size of the Local Color Table of the current image.
         *
         * **GIF89a**: *If the Local Color Table Flag is set to 1, the value in this
         * field is used to calculate the number of bytes contained in the Local Color Table.
         * To determine that actual size of the color table, raise 2 to [the value of the field + 1].
         * This value should be 0 if there is no Local Color Table specified.*
         */
        private const val DESCRIPTOR_MASK_LCT_SIZE = 7
        // Logical Screen Descriptor packed field masks (describing Global Color Table)
        /**
         * Mask (bit 7) to extract Global Color Table Flag of the current image.
         *
         * **GIF89a**: *Indicates the presence of a Global Color Table
         * immediately following this Image Descriptor.*
         * Possible values are:
         *  * 0 - No Global Color Table follows, the Background Color Index field is meaningless.
         *  * 1 - A Global Color Table will immediately follow,
         * the Background Color Index field is meaningful.
         *
         */
        private const val LSD_MASK_GCT_FLAG = 128

        /**
         * Mask (bits 2-0) to extract Size of the Global Color Table of the current image.
         *
         * **GIF89a**: *If the Global Color Table Flag is set to 1, the value in this
         * field is used to calculate the number of bytes contained in the Global Color Table.
         * To determine that actual size of the color table, raise 2 to [the value of the field + 1].
         * Even if there is no Global Color Table specified, set this field according to the above
         * formula so that decoders can choose the best graphics mode to display the stream in.*
         */
        private const val LSD_MASK_GCT_SIZE = 7

        /**
         * The minimum frame delay in hundredths of a second.
         */
        const val MIN_FRAME_DELAY = 2

        /**
         * The default frame delay in hundredths of a second.
         * This is used for GIFs with frame delays less than the minimum.
         */
        const val DEFAULT_FRAME_DELAY = 10
        private const val MAX_BLOCK_SIZE = 256
    }

    init {
        reset()
        rawData = ByteBuffer.wrap(data).asReadOnlyBuffer().also {
            it.position(0)
            it.order(ByteOrder.LITTLE_ENDIAN)
        }
    }
}
