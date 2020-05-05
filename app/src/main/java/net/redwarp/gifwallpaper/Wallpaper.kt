package net.redwarp.gifwallpaper

import android.content.Context
import android.net.Uri

// TODO nuke this
data class Wallpaper(val uri: Uri) {
    companion object {
        private const val SHARED_PREF_NAME = "wallpaper_pref"
        private const val KEY_WALLPAPER_URI = "wallpaper_uri"

        fun getWallpaper(context: Context): Wallpaper? {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            val urlString = sharedPreferences.getString(KEY_WALLPAPER_URI, null)
            return urlString?.let { Wallpaper(Uri.parse(it)) }
        }

        fun setWallpaper(context: Context, wallpaper: Wallpaper) {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
            sharedPreferences.edit().putString(KEY_WALLPAPER_URI, wallpaper.uri.toString()).apply()
        }
    }
}
