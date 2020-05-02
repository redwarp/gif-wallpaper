package net.redwarp.gifwallpaper

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtils {
    fun copyFileLocally(context: Context, uri: Uri): Uri? {
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
            return null
        } else {
            return Uri.fromFile(file)
        }
    }
}
