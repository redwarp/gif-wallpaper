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
package net.redwarp.gifwallpaper.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtils {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun copyFileLocally(context: Context, uri: Uri): Uri? = withContext(Dispatchers.IO) {
        val localDir = context.filesDir
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        val localFileName = UUID.randomUUID().toString().let {
            if (extension != "") {
                "$it.$extension"
            } else {
                it
            }
        }

        val file = File(localDir, localFileName).apply {
            createNewFile()
            setWritable(true, true)
        }

        var copiedLength = 0L
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                copiedLength = inputStream.copyTo(outputStream)
            }
        }

        if (copiedLength == 0L) {
            file.delete()
            null
        } else {
            Uri.fromFile(file)
        }
    }
}
