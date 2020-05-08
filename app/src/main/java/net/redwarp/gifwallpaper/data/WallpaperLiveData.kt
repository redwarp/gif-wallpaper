package net.redwarp.gifwallpaper.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.utils.FileUtils
import java.io.File

internal class WallpaperLiveData(private val context: Context) :
    LiveData<WallpaperStatus>() {
    private var currentUri: Uri? = null
    private var localUri: Uri? = null

    init {
        postValue(WallpaperStatus.Loading)
        loadInitialValue()
    }

    fun loadNewGif(uri: Uri) {
        if (uri == currentUri) return

        CoroutineScope(Dispatchers.Main).launch {
            postValue(WallpaperStatus.Loading)
            val copiedUri =
                FileUtils.copyFileLocally(context, uri)
            if (copiedUri == null) {
                postValue(WallpaperStatus.NotSet)
            } else {
                postValue(
                    WallpaperStatus.Wallpaper(
                        copiedUri
                    )
                )
            }
            cleanupOldUri(localUri)
            currentUri = uri
            localUri = copiedUri
            storeCurrentWallpaperUri(context, copiedUri)
        }
    }

    fun clearGif() {
        postValue(WallpaperStatus.NotSet)
        cleanupOldUri(localUri)
        currentUri == null
        localUri = null
    }

    private fun loadInitialValue() {
        val uri: Uri? = loadCurrentWallpaperUri(context)
        if (uri == null) {
            postValue(WallpaperStatus.NotSet)
        } else {
            postValue(
                WallpaperStatus.Wallpaper(
                    uri
                )
            )
        }
    }

    private fun loadCurrentWallpaperUri(context: Context): Uri? {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val urlString = sharedPreferences.getString(KEY_WALLPAPER_URI, null)
        return urlString?.let { Uri.parse(it) }
    }

    private fun storeCurrentWallpaperUri(context: Context, uri: Uri?) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        if (uri != null) {
            sharedPreferences.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply()
        } else {
            sharedPreferences.edit().remove(KEY_WALLPAPER_URI).apply()
        }
    }

    private fun cleanupOldUri(uri: Uri?) {
        val path = uri?.path ?: return

        File(path).delete()
    }
}

sealed class WallpaperStatus {
    object NotSet : WallpaperStatus()
    object Loading : WallpaperStatus()
    data class Wallpaper(val uri: Uri) : WallpaperStatus()
}
