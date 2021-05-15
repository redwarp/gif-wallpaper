/* Copyright 2020 Benoit Vermont
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

object GifLoader {

    suspend fun loadInitialValue(wallpaperSettings: WallpaperSettings): WallpaperStatus {
        val file: File? = wallpaperSettings.getWallpaperFile()
        return if (file == null) {
            WallpaperStatus.NotSet
        } else {
            loadGifDescriptor(file)
        }
    }

    suspend fun loadNewGif(context: Context, wallpaperSettings: WallpaperSettings, uri: Uri) =
        withContext(Dispatchers.IO) {
            flow {
                emit(WallpaperStatus.Loading)
                val copiedFile = FileUtils.copyFileLocally(context, uri)
                if (copiedFile == null) {
                    emit(WallpaperStatus.NotSet)
                } else {
                    emit(loadGifDescriptor(copiedFile))
                }
                val localFile: File? = wallpaperSettings.getWallpaperFile()

                localFile?.let(this@GifLoader::cleanupOldUri)

                wallpaperSettings.setWallpaperFile(copiedFile)
            }
        }

    suspend fun clearGif(wallpaperSettings: WallpaperSettings) {
        val localFile: File? = wallpaperSettings.getWallpaperFile()
        localFile?.let(this::cleanupOldUri)
        wallpaperSettings.setWallpaperFile(null)
    }

    private fun cleanupOldUri(file: File) {
        runCatching {
            if (file.exists()) {
                file.delete()
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
                        WallpaperStatus.Wallpaper(result.value)
                    }
                    else -> {
                        WallpaperStatus.NotSet
                    }
                }
            }.getOrDefault(WallpaperStatus.NotSet)
        }
}
