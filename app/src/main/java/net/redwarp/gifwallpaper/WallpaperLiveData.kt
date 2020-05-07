package net.redwarp.gifwallpaper

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val SHARED_PREF_NAME = "wallpaper_pref"
private const val KEY_WALLPAPER_URI = "wallpaper_uri"

class WallpaperLiveData private constructor(private val context: Context) :
    LiveData<WallpaperStatus>() {
    private var currentWallpaper: WallpaperStatus.Wallpaper2? = null

    init {
        postValue(WallpaperStatus.Loading)
        loadInitialValue()
    }

    override fun setValue(value: WallpaperStatus?) {
        super.setValue(value)
        if (currentWallpaper != value && value is WallpaperStatus.Wallpaper2) {
            currentWallpaper = value
            storeCurrentWallpaperUri(context, value.uri)
        }
    }

    fun loadNewGif(uri: Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            postValue(WallpaperStatus.Loading)
            val copiedUri = FileUtils.copyFileLocally(context, uri)
            if (copiedUri == null) {
                postValue(WallpaperStatus.NotSet)
                return@launch
            }

            postValue(WallpaperStatus.Wallpaper2(copiedUri, GifDrawer.ScaleType.FIT_CENTER))
        }
    }

    private fun loadInitialValue() {
        val uri: Uri? = loadCurrentWallpaperUri(context)
        if (uri == null) {
            postValue(WallpaperStatus.NotSet)
        } else {
            postValue(WallpaperStatus.Wallpaper2(uri, GifDrawer.ScaleType.FIT_CENTER))
        }
    }

    private fun loadCurrentWallpaperUri(context: Context): Uri? {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val urlString = sharedPreferences.getString(KEY_WALLPAPER_URI, null)
        return urlString?.let { Uri.parse(it) }
    }

    private fun storeCurrentWallpaperUri(context: Context, uri: Uri) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply()
    }

    companion object {
        private lateinit var instance: WallpaperLiveData

        fun get(context: Context): WallpaperLiveData {
            instance = if (::instance.isInitialized) {
                instance
            } else {
                WallpaperLiveData(context.applicationContext)
            }
            return instance
        }
    }
}

sealed class WallpaperStatus {
    object NotSet : WallpaperStatus()
    object Loading : WallpaperStatus()
    data class Wallpaper2(val uri: Uri, val scaleType: GifDrawer.ScaleType) : WallpaperStatus()
}
