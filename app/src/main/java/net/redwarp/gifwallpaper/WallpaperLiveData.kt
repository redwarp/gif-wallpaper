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
    private var currentWallpaper: WallpaperStatus.Wallpaper? = null

    init {
        postValue(WallpaperStatus.Loading)
        loadInitialValue()
    }

    override fun setValue(value: WallpaperStatus?) {
        super.setValue(value)
        if (currentWallpaper != value && value is WallpaperStatus.Wallpaper) {
            currentWallpaper = value
            storeCurrentWallpaperuri(context, value.uri)
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

            val gif = Gif.loadGif(context, copiedUri)
            if (gif == null) {
                postValue(WallpaperStatus.NotSet)
                return@launch
            }

            postValue(
                WallpaperStatus.Wallpaper(
                    gif,
                    GifDrawer.ScaleType.FIT_CENTER,
                    copiedUri
                )
            )
        }
    }

    private fun loadInitialValue() {
        val uri: Uri? = loadCurrentWallpaperUri(context)
        if (uri == null) {
            postValue(WallpaperStatus.NotSet)
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                val gif = Gif.loadGif(context, uri)
                if (gif == null) postValue(WallpaperStatus.NotSet)
                else {
                    val initialWallpaper =
                        WallpaperStatus.Wallpaper(
                            gif,
                            GifDrawer.ScaleType.FIT_CENTER,
                            uri
                        )
                    currentWallpaper = initialWallpaper
                    postValue(initialWallpaper)
                }
            }
        }
    }

    fun loadCurrentWallpaperUri(context: Context): Uri? {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val urlString = sharedPreferences.getString(KEY_WALLPAPER_URI, null)
        return urlString?.let { Uri.parse(it) }
    }

    fun storeCurrentWallpaperuri(context: Context, uri: Uri) {
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
    data class Wallpaper(val gif: Gif, val scaleType: GifDrawer.ScaleType, val uri: Uri) :
        WallpaperStatus()
}
