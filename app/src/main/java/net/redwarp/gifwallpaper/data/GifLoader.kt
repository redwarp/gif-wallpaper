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
package net.redwarp.gifwallpaper.data

import android.content.Context
import android.net.Uri
import app.redwarp.gif.decoder.Parser
import app.redwarp.gif.decoder.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.redwarp.gifwallpaper.util.FileUtils
import java.io.File

private const val FILE_SIZE_THRESHOLD = 5 * 1024 * 1024
internal const val KEY_WALLPAPER_URI = "wallpaper_uri"

object GifLoader {

    suspend fun loadInitialValue(context: Context): WallpaperStatus {
        val file: File? = loadCurrentWallpaperFile(context)
        return if (file == null) {
            WallpaperStatus.NotSet
        } else {
            loadGifDescriptor(file)
        }
    }

    suspend fun loadNewGif(context: Context, uri: Uri) =
        withContext(Dispatchers.IO) {
            flow {
                emit(WallpaperStatus.Loading)
                val copiedFile = FileUtils.copyFileLocally(context, uri)
                if (copiedFile == null) {
                    emit(WallpaperStatus.NotSet)
                } else {
                    emit(loadGifDescriptor(copiedFile))
                }
                val localFile: File? = loadCurrentWallpaperFile(context)

                localFile?.let(this@GifLoader::cleanupOldUri)

                storeCurrentWallpaperFile(context, copiedFile)
            }
        }

    fun clearGif(context: Context) {
        val localFile: File? = loadCurrentWallpaperFile(context)
        localFile?.let(this::cleanupOldUri)
        storeCurrentWallpaperFile(context, null)
    }

    private fun cleanupOldUri(file: File) {
        runCatching {
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun loadCurrentWallpaperFile(context: Context): File? {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val urlString = sharedPreferences.getString(KEY_WALLPAPER_URI, null)
        return urlString?.let {
            val path = Uri.parse(it).path
            if (path != null) {
                File(path)
            } else {
                null
            }
        }
    }

    private suspend fun loadGifDescriptor(file: File): WallpaperStatus =
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val result = if (file.length() > FILE_SIZE_THRESHOLD) {
                    Parser.parse(file)
                } else {
                    Parser.parse(file.inputStream())
                }
                when (result) {
                    is Result.Success -> {
                        WallpaperStatus.Wallpaper(file, result.value)
                    }
                    else -> {
                        WallpaperStatus.NotSet
                    }
                }
            }.getOrDefault(WallpaperStatus.NotSet)
        }

    private fun storeCurrentWallpaperFile(context: Context, file: File?) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

        if (file != null) {
            val uri = Uri.fromFile(file)
            sharedPreferences.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply()
        } else {
            sharedPreferences.edit().remove(KEY_WALLPAPER_URI).apply()
        }
    }
}
