/* Copyright 2020 Redwarp
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
package net.redwarp.gifwallpaper.drawable

import android.graphics.Bitmap
import com.bumptech.glide.gifdecoder.GifDecoder
import java.util.LinkedList
import java.util.Queue

class SimpleBitmapProvider : GifDecoder.BitmapProvider {
    private val bitmaps = mutableMapOf<Int, Queue<Bitmap>>()
    private val byteArrays = mutableMapOf<Int, Queue<ByteArray>>()
    private val intArrays = mutableMapOf<Int, Queue<IntArray>>()

    override fun obtain(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        val key = calculateSize(width, height, config)

        return findBitmap(key)?.also { it.reconfigure(width, height, config) }
            ?: createBitmap(width, height, config)
    }

    override fun release(bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        synchronized(this) {
            val queue = bitmaps[bitmap.allocationByteCount]
                ?: LinkedList<Bitmap>().also { bitmaps[bitmap.allocationByteCount] = it }
            queue.offer(bitmap)
        }
    }

    @Synchronized
    override fun release(bytes: ByteArray) {
        byteArrays[bytes.size] ?: LinkedList<ByteArray>().also {
            byteArrays[bytes.size] = it
        }.offer(bytes)
    }

    @Synchronized
    override fun release(array: IntArray) {
        intArrays[array.size] ?: LinkedList<IntArray>().also {
            intArrays[array.size] = it
        }.offer(array)
    }

    @Synchronized
    override fun obtainByteArray(size: Int): ByteArray {
        return byteArrays[size]?.poll() ?: ByteArray(size)
    }

    @Synchronized
    override fun obtainIntArray(size: Int): IntArray {
        return intArrays[size]?.poll() ?: IntArray(size)
    }

    @Synchronized
    override fun flush() {
        intArrays.clear()
        byteArrays.clear()
        bitmaps.iterator().forEach { entry ->
            val queueIterator = entry.value.iterator()
            while (queueIterator.hasNext()) {
                queueIterator.next().recycle()
                queueIterator.remove()
            }
        }
        bitmaps.clear()
    }

    @Synchronized
    private fun findBitmap(cacheKey: Int): Bitmap? {
        return bitmaps[cacheKey]?.poll()
    }

    private fun createBitmap(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        return Bitmap.createBitmap(width, height, config)
    }

    private fun calculateSize(width: Int, height: Int, config: Bitmap.Config) =
        width * height * config.byteSize()
}

private fun Bitmap.Config.byteSize(): Int {
    return when (this) {
        Bitmap.Config.ALPHA_8 -> 1
        Bitmap.Config.ARGB_8888 -> 4
        Bitmap.Config.RGB_565 -> 2
        else -> throw UnsupportedOperationException("These bitmap formats are not supported")
    }
}
