/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper.data

import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import net.redwarp.gifwallpaper.renderer.WallpaperRenderer

internal const val SHARED_PREF_NAME = "wallpaper_pref"
internal const val KEY_WALLPAPER_URI = "wallpaper_uri"
internal const val KEY_WALLPAPER_SCALE_TYPE = "wallpaper_scale_type"
internal const val KEY_WALLPAPER_BACKGROUND_COLOR = "wallpaper_background_color"

class Model private constructor(val context: Context) {
    private val _wallpaperStatus =
        WallpaperLiveData(context)
    private val _scaleTypeData =
        ScaleTypeData(context)
    private val _backgroundColorData = MediatorLiveData<Int>()

    val wallpaperStatus: LiveData<WallpaperStatus> get() = _wallpaperStatus
    val scaleTypeData: LiveData<WallpaperRenderer.ScaleType> get() = _scaleTypeData
    val colorInfoData: LiveData<ColorInfo> = ColorLiveData(context, wallpaperStatus)
    val backgroundColorData: LiveData<Int> get() = _backgroundColorData

    private var isColorSet = true

    init {
        _backgroundColorData.addSource(colorInfoData) { colorInfo ->
            if (!isColorSet) {
                when (colorInfo) {
                    is ColorScheme -> {
                        _backgroundColorData.postValue(colorInfo.defaultColor)
                        isColorSet = true
                    }
                }
            }
        }
        _backgroundColorData.value = loadBackgroundColor(context)
        _backgroundColorData.observeForever { backgroundColor ->
            storeBackgroundColor(context, backgroundColor)
        }
        wallpaperStatus.observeForever {
            if (it is WallpaperStatus.NotSet) isColorSet = false
        }
    }

    private fun storeBackgroundColor(context: Context, backgroundColor: Int) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(KEY_WALLPAPER_BACKGROUND_COLOR, backgroundColor).apply()
    }

    private fun loadBackgroundColor(context: Context): Int {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_WALLPAPER_BACKGROUND_COLOR, Color.RED)
    }

    fun loadNewGif(uri: Uri) {
        isColorSet = false
        _wallpaperStatus.loadNewGif(uri)
    }

    fun clearGif() {
        isColorSet = false
        _wallpaperStatus.clearGif()
    }

    fun setScaleType(scaleType: WallpaperRenderer.ScaleType) {
        _scaleTypeData.setScaleType(scaleType)
    }

    fun setBackgroundColor(@ColorInt color: Int) {
        _backgroundColorData.postValue(color)
    }

    private class ScaleTypeData(private val context: Context) : LiveData<WallpaperRenderer.ScaleType>() {
        init {
            loadInitialValue()
        }

        private fun loadInitialValue() {
            postValue(loadCurrentScaleType(context))
        }

        private fun loadCurrentScaleType(context: Context): WallpaperRenderer.ScaleType {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val scaleTypeOrdinal = sharedPreferences.getInt(KEY_WALLPAPER_SCALE_TYPE, 0)
            return WallpaperRenderer.ScaleType.values()[scaleTypeOrdinal]
        }

        private fun storeCurrentScaleType(context: Context, scaleType: WallpaperRenderer.ScaleType) {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit().putInt(KEY_WALLPAPER_SCALE_TYPE, scaleType.ordinal).apply()
        }

        fun setScaleType(scaleType: WallpaperRenderer.ScaleType) {
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
