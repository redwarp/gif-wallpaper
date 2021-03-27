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
import androidx.lifecycle.LiveData
import app.redwarp.gif.decoder.Parser
import app.redwarp.gif.decoder.Result
import app.redwarp.gif.decoder.descriptors.GifDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.redwarp.gifwallpaper.util.FileUtils
import java.io.File

private const val FILE_SIZE_THRESHOLD = 5 * 1024 * 1024

internal class WallpaperLiveData(private val context: Context) :
    LiveData<WallpaperStatus>() {
    private var currentUri: Uri? = null
    private var localFile: File? = null

    init {
        postValue(WallpaperStatus.Loading)
        GlobalScope.launch {
            loadInitialValue()
        }
    }

    fun loadNewGif(uri: Uri) {
        if (uri == currentUri) return

        CoroutineScope(Dispatchers.IO).launch {
            postValue(WallpaperStatus.Loading)
            val copiedFile = FileUtils.copyFileLocally(context, uri)
            if (copiedFile == null) {
                postValue(WallpaperStatus.NotSet)
            } else {
                postValue(loadGifDescriptor(copiedFile))
            }
            localFile?.let(this@WallpaperLiveData::cleanupOldUri)
            currentUri = uri
            localFile = copiedFile
            storeCurrentWallpaperFile(context, copiedFile)
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

    fun clearGif() {
        postValue(WallpaperStatus.NotSet)
        localFile?.let(this::cleanupOldUri)
        storeCurrentWallpaperFile(context, null)
        currentUri = null
        localFile = null
    }

    private suspend fun loadInitialValue() {
        val uri: File? = loadCurrentWallpaperFile(context)
        localFile = uri
        if (uri == null) {
            postValue(WallpaperStatus.NotSet)
        } else {
            postValue(
                loadGifDescriptor(uri)
            )
        }
    }

    private fun loadCurrentWallpaperFile(context: Context): File? {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
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

    private fun cleanupOldUri(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }
}

sealed class WallpaperStatus {
    object NotSet : WallpaperStatus()
    object Loading : WallpaperStatus()
    data class Wallpaper(val gifDescriptor: GifDescriptor) : WallpaperStatus()
}
