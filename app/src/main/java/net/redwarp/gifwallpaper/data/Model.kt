package net.redwarp.gifwallpaper.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import net.redwarp.gifwallpaper.GifDrawer

internal const val SHARED_PREF_NAME = "wallpaper_pref"
internal const val KEY_WALLPAPER_URI = "wallpaper_uri"
internal const val KEY_WALLPAPER_SCALE_TYPE = "wallpaper_scale_type"

class Model private constructor(context: Context) {
    private val _wallpaperStatus =
        WallpaperLiveData(context)
    private val _scaleTypeData =
        ScaleTypeData(context)

    val wallpaperStatus: LiveData<WallpaperStatus> get() = _wallpaperStatus
    val scaleTypeData: LiveData<GifDrawer.ScaleType> get() = _scaleTypeData

    fun loadNewGif(uri: Uri) {
        _wallpaperStatus.loadNewGif(uri)
    }

    fun setScaleType(scaleType: GifDrawer.ScaleType) {
        _scaleTypeData.setScaleType(scaleType)
    }

    private class ScaleTypeData(private val context: Context) : LiveData<GifDrawer.ScaleType>() {
        init {
            loadInitialValue()
        }

        private fun loadInitialValue() {
            postValue(loadCurrentScaleType(context))
        }

        private fun loadCurrentScaleType(context: Context): GifDrawer.ScaleType {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val scaleTypeOrdinal = sharedPreferences.getInt(KEY_WALLPAPER_SCALE_TYPE, 0)
            return GifDrawer.ScaleType.values()[scaleTypeOrdinal]
        }

        private fun storeCurrentScaleType(context: Context, scaleType: GifDrawer.ScaleType) {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit().putInt(KEY_WALLPAPER_SCALE_TYPE, scaleType.ordinal).apply()
        }

        fun setScaleType(scaleType: GifDrawer.ScaleType) {
            storeCurrentScaleType(context, scaleType)
            postValue(scaleType)
        }
    }

    companion object {
        private lateinit var instance: Model

        fun get(context: Context): Model {
            instance =
                if (Companion::instance.isInitialized) instance else (Model(
                    context.applicationContext
                ))

            return instance
        }
    }
}
