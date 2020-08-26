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
package com.bumptech.glide.gifdecoder;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static com.bumptech.glide.gifdecoder.GifDecoder.STATUS_FORMAT_ERROR;
import static com.bumptech.glide.gifdecoder.GifFrame.DISPOSAL_NONE;
import static com.bumptech.glide.gifdecoder.GifFrame.DISPOSAL_UNSPECIFIED;

/**
 * A class responsible for creating {@link com.bumptech.glide.gifdecoder.GifHeader}s from data
 * representing animated GIFs.
 *
 * @see <a href="https://www.w3.org/Graphics/GIF/spec-gif89a.txt">GIF 89a Specification</a>
 */
public class GifHeaderParser {
    private static final String TAG = "GifHeaderParser";

    private static final int MASK_INT_LOWEST_BYTE = 0x000000FF;

    /**
     * Identifies the beginning of an Image Descriptor.
     */
    private static final int IMAGE_SEPARATOR = 0x2C;
    /**
     * Identifies the beginning of an extension block.
     */
    private static final int EXTENSION_INTRODUCER = 0x21;
    /**
     * This block is a single-field block indicating the end of the GIF Data Stream.
     */
    private static final int TRAILER = 0x3B;
    // Possible labels that identify the current extension block.
    private static final int LABEL_GRAPHIC_CONTROL_EXTENSION = 0xF9;
    private static final int LABEL_APPLICATION_EXTENSION = 0xFF;
    private static final int LABEL_COMMENT_EXTENSION = 0xFE;
    private static final int LABEL_PLAIN_TEXT_EXTENSION = 0x01;

    // Graphic Control Extension packed field masks

    /**
     * Mask (bits 4-2) to extract Disposal Method of the current frame.
     *
     * @see GifFrame.GifDisposalMethod possible values
     */
    private static final int GCE_MASK_DISPOSAL_METHOD = 0b00011100;
    /**
     * Shift so the Disposal Method extracted from the packed value is on the least significant bit.
     */
    private static final int GCE_DISPOSAL_METHOD_SHIFT = 2;
    /**
     * Mask (bit 0) to extract Transparent Color Flag of the current frame.
     * <p><b>GIF89a</b>: <i>Indicates whether a transparency index is given
     * in the Transparent Index field.</i></p>
     * Possible values are:<ul>
     * <li>0 - Transparent Index is not given.</li>
     * <li>1 - Transparent Index is given.</li>
     * </ul>
     */
    private static final int GCE_MASK_TRANSPARENT_COLOR_FLAG = 0b00000001;

    // Image Descriptor packed field masks (describing Local Color Table)

    /**
     * Mask (bit 7) to extract Local Color Table Flag of the current image.
     * <p><b>GIF89a</b>: <i>Indicates the presence of a Local Color Table
     * immediately following this Image Descriptor.</i></p>
     */
    private static final int DESCRIPTOR_MASK_LCT_FLAG = 0b10000000;
    /**
     * Mask (bit 6) to extract Interlace Flag of the current image.
     * <p><b>GIF89a</b>: <i>Indicates if the image is interlaced.
     * An image is interlaced in a four-pass interlace pattern.</i></p>
     * Possible values are:<ul>
     * <li>0 - Image is not interlaced.</li>
     * <li>1 - Image is interlaced.</li>
     * </ul>
     */
    private static final int DESCRIPTOR_MASK_INTERLACE_FLAG = 0b01000000;
    /**
     * Mask (bits 2-0) to extract Size of the Local Color Table of the current image.
     * <p><b>GIF89a</b>: <i>If the Local Color Table Flag is set to 1, the value in this
     * field is used to calculate the number of bytes contained in the Local Color Table.
     * To determine that actual size of the color table, raise 2 to [the value of the field + 1].
     * This value should be 0 if there is no Local Color Table specified.</i></p>
     */
    private static final int DESCRIPTOR_MASK_LCT_SIZE = 0b00000111;

    // Logical Screen Descriptor packed field masks (describing Global Color Table)

    /**
     * Mask (bit 7) to extract Global Color Table Flag of the current image.
     * <p><b>GIF89a</b>: <i>Indicates the presence of a Global Color Table
     * immediately following this Image Descriptor.</i></p>
     * Possible values are:<ul>
     * <li>0 - No Global Color Table follows, the Background Color Index field is meaningless.</li>
     * <li>1 - A Global Color Table will immediately follow,
     * the Background Color Index field is meaningful.</li>
     * </ul>
     */
    private static final int LSD_MASK_GCT_FLAG = 0b10000000;
    /**
     * Mask (bits 2-0) to extract Size of the Global Color Table of the current image.
     * <p><b>GIF89a</b>: <i>If the Global Color Table Flag is set to 1, the value in this
     * field is used to calculate the number of bytes contained in the Global Color Table.
     * To determine that actual size of the color table, raise 2 to [the value of the field + 1].
     * Even if there is no Global Color Table specified, set this field according to the above
     * formula so that decoders can choose the best graphics mode to display the stream in.</i></p>
     */
    private static final int LSD_MASK_GCT_SIZE = 0b00000111;

    /**
     * The minimum frame delay in hundredths of a second.
     */
    static final int MIN_FRAME_DELAY = 2;
    /**
     * The default frame delay in hundredths of a second.
     * This is used for GIFs with frame delays less than the minimum.
     */
    static final int DEFAULT_FRAME_DELAY = 10;

    private static final int MAX_BLOCK_SIZE = 256;
    // Raw data read working array.
    private final byte[] block = new byte[MAX_BLOCK_SIZE];

    private ByteBuffer rawData;
    private GifHeader header = new GifHeader();
    private int blockSize = 0;

    public GifHeaderParser setData(@NonNull byte[] data) {
        reset();
        rawData = ByteBuffer.wrap(data).asReadOnlyBuffer();
        rawData.position(0);
        rawData.order(ByteOrder.LITTLE_ENDIAN);
        return this;
    }

    public void clear() {
        rawData = null;
        header = null;
    }

    private void reset() {
        rawData = null;
        Arrays.fill(block, (byte) 0);
        header = new GifHeader();
        blockSize = 0;
    }

    @NonNull
    public GifHeader parseHeader() {
        if (rawData == null) {
            throw new IllegalStateException("You must call setData() before parseHeader()");
        }
        if (err()) {
            return header;
        }

        readHeader();
        if (!err()) {
            readContents();
            if (header.numFrames < 0) {
                header.status = STATUS_FORMAT_ERROR;
            }
        }

        return header;
    }

    /**
     * Determines if the GIF is animated by trying to read in the first 2 frames
     * This method re-parses the data even if the header has already been read.
     */
    public boolean isAnimated() {
        readHeader();
        if (!err()) {
            readContents(2 /* maxFrames */);
        }
        return header.numFrames > 1;
    }

    /**
     * Main file parser. Reads GIF content blocks.
     */
    private void readContents() {
        readContents(Integer.MAX_VALUE /* maxFrames */);
    }

    /**
     * Main file parser. Reads GIF content blocks. Stops after reading maxFrames
     */
    private void readContents(int maxFrames) {
        // Read GIF file content blocks.
        boolean done = false;
        while (!(done || err() || header.numFrames > maxFrames)) {
            int code = read();
            switch (code) {
                case IMAGE_SEPARATOR:
                    // The Graphic Control Extension is optional, but will always come first if it exists.
                    // If one did exist, there will be a non-null current frame which we should use.
                    // However if one did not exist, the current frame will be null
                    // and we must create it here. See issue #134.
                    if (header.currentFrame == null) {
                        header.currentFrame = new GifFrame();
                    }
                    readBitmap(header.currentFrame);
                    break;
                case EXTENSION_INTRODUCER:
                    int extensionLabel = read();
                    switch (extensionLabel) {
                        case LABEL_GRAPHIC_CONTROL_EXTENSION:
                            // Start a new frame.
                            header.currentFrame = new GifFrame();
                            readGraphicControlExt(header.currentFrame);
                            break;
                        case LABEL_APPLICATION_EXTENSION:
                            readBlock();
                            StringBuilder app = new StringBuilder();
                            for (int i = 0; i < 11; i++) {
                                app.append((char) block[i]);
                            }
                            if (app.toString().equals("NETSCAPE2.0")) {
                                readNetscapeExt();
                            } else {
                                // Don't care.
                                skip();
                            }
                            break;
                        case LABEL_COMMENT_EXTENSION:
                            skip();
                            break;
                        case LABEL_PLAIN_TEXT_EXTENSION:
                            skip();
                            break;
                        default:
                            // Uninteresting extension.
                            skip();
                    }
                    break;
                case TRAILER:
                    // This block is a single-field block indicating the end of the GIF Data Stream.
                    done = true;
                    break;
                // Bad byte, but keep going and see what happens
                case 0x00:
                default:
                    header.status = STATUS_FORMAT_ERROR;
            }
        }
    }

    /**
     * Reads Graphic Control Extension values.
     */
    private void readGraphicControlExt(@NonNull GifFrame currentFrame) {
        // Block size.
        read();
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
        int packed = read();
        // Disposal method.
        //noinspection WrongConstant field has to be extracted from packed value
        currentFrame.dispose = (packed & GCE_MASK_DISPOSAL_METHOD) >> GCE_DISPOSAL_METHOD_SHIFT;
        if (currentFrame.dispose == DISPOSAL_UNSPECIFIED) {
            // Elect to keep old image if discretionary.
            currentFrame.dispose = DISPOSAL_NONE;
        }
        currentFrame.transparency = (packed & GCE_MASK_TRANSPARENT_COLOR_FLAG) != 0;
        // Delay in milliseconds.
        int delayInHundredthsOfASecond = readShort();
        // TODO: consider allowing -1 to indicate show forever.
        if (delayInHundredthsOfASecond < MIN_FRAME_DELAY) {
            delayInHundredthsOfASecond = DEFAULT_FRAME_DELAY;
        }
        currentFrame.delay = delayInHundredthsOfASecond * 10;
        // Transparent color index
        currentFrame.transIndex = read();
        // Block terminator
        read();
    }

    /**
     * Reads next frame image.
     */
    private void readBitmap(GifFrame inFrame) {
        // (sub)image position & size.
        inFrame.ix = readShort();
        inFrame.iy = readShort();
        inFrame.iw = readShort();
        inFrame.ih = readShort();

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
        int packed = read();
        boolean lctFlag = (packed & DESCRIPTOR_MASK_LCT_FLAG) != 0;
        int lctSize = (int) Math.pow(2, (packed & DESCRIPTOR_MASK_LCT_SIZE) + 1);
        inFrame.interlace = (packed & DESCRIPTOR_MASK_INTERLACE_FLAG) != 0;
        if (lctFlag) {
            inFrame.lct = readColorTable(lctSize);
        } else {
            // No local color table.
            inFrame.lct = null;
        }

        // Save this as the decoding position pointer.
        inFrame.bufferFrameStart = rawData.position();

        // False decode pixel data to advance buffer.
        skipImageData();

        if (err()) {
            return;
        }

        header.numFrames++;
        // Add image to frame.
        header.frames.add(inFrame);
    }

    /**
     * Reads Netscape extension to obtain iteration count.
     */
    private void readNetscapeExt() {
        do {
            readBlock();
            if (block[0] == 1) {
                // Loop count sub-block.
                int b1 = ((int) block[1]) & MASK_INT_LOWEST_BYTE;
                int b2 = ((int) block[2]) & MASK_INT_LOWEST_BYTE;
                header.loopCount = (b2 << 8) | b1;
            }
        } while ((blockSize > 0) && !err());
    }


    /**
     * Reads GIF file header information.
     */
    private void readHeader() {
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            id.append((char) read());
        }
        if (!id.toString().startsWith("GIF")) {
            header.status = STATUS_FORMAT_ERROR;
            return;
        }
        readLSD();
        if (header.gctFlag && !err()) {
            header.gct = readColorTable(header.gctSize);
            header.bgColor = header.gct[header.bgIndex];
        }
    }

    /**
     * Reads Logical Screen Descriptor.
     */
    private void readLSD() {
        // Logical screen size.
        header.width = readShort();
        header.height = readShort();
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
        int packed = read();
        header.gctFlag = (packed & LSD_MASK_GCT_FLAG) != 0;
        header.gctSize = (int) Math.pow(2, (packed & LSD_MASK_GCT_SIZE) + 1);
        // Background color index.
        header.bgIndex = read();
        // Pixel aspect ratio
        header.pixelAspect = read();
    }

    /**
     * Reads color table as 256 RGB integer values.
     *
     * @param nColors int number of colors to read.
     * @return int array containing 256 colors (packed ARGB with full alpha).
     */
    @Nullable
    private int[] readColorTable(int nColors) {
        int nBytes = 3 * nColors;
        int[] tab = null;
        byte[] c = new byte[nBytes];

        try {
            rawData.get(c);

            // TODO: what bounds checks are we avoiding if we know the number of colors?
            // Max size to avoid bounds checks.
            tab = new int[MAX_BLOCK_SIZE];
            int i = 0;
            int j = 0;
            while (i < nColors) {
                int r = ((int) c[j++]) & MASK_INT_LOWEST_BYTE;
                int g = ((int) c[j++]) & MASK_INT_LOWEST_BYTE;
                int b = ((int) c[j++]) & MASK_INT_LOWEST_BYTE;
                tab[i++] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        } catch (BufferUnderflowException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Format Error Reading Color Table", e);
            }
            header.status = STATUS_FORMAT_ERROR;
        }

        return tab;
    }

    /**
     * Skips LZW image data for a single frame to advance buffer.
     */
    private void skipImageData() {
        // lzwMinCodeSize
        read();
        // data sub-blocks
        skip();
    }

    /**
     * Skips variable length blocks up to and including next zero length block.
     */
    private void skip() {
        int blockSize;
        do {
            blockSize = read();
            int newPosition = Math.min(rawData.position() + blockSize, rawData.limit());
            rawData.position(newPosition);
        } while (blockSize > 0);
    }

    /**
     * Reads next variable length block from input.
     */
    private void readBlock() {
        blockSize = read();
        int n = 0;
        if (blockSize > 0) {
            int count = 0;
            try {
                while (n < blockSize) {
                    count = blockSize - n;
                    rawData.get(block, n, count);

                    n += count;
                }
            } catch (Exception e) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG,
                            "Error Reading Block n: " + n + " count: " + count + " blockSize: " + blockSize, e);
                }
                header.status = STATUS_FORMAT_ERROR;
            }
        }
    }

    /**
     * Reads a single byte from the input stream.
     */
    private int read() {
        int currByte = 0;
        try {
            currByte = rawData.get() & MASK_INT_LOWEST_BYTE;
        } catch (Exception e) {
            header.status = STATUS_FORMAT_ERROR;
        }
        return currByte;
    }

    /**
     * Reads next 16-bit value, LSB first.
     */
    private int readShort() {
        // Read 16-bit value.
        return rawData.getShort();
    }

    private boolean err() {
        return header.status != GifDecoder.STATUS_OK;
    }
}
