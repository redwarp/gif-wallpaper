/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtils {
    suspend fun copyFileLocally(context: Context, uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
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
}
