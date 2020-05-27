/* Licensed under Apache-2.0 */
package net.redwarp.gifwallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Shader
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.nickbutcher.TileDrawable
import kotlinx.android.synthetic.main.fragment_launcher.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LauncherFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_launcher, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_first.setOnClickListener {
            activateWallpaper(requireContext())
        }
        ContextCompat.getDrawable(view.context, R.drawable.pattern_stripes)?.let {
            background.setImageDrawable(TileDrawable(it, Shader.TileMode.REPEAT))
        }
    }

    private fun activateWallpaper(context: Context) {
        val intent = Intent().apply {
            action = WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(
                    context.packageName,
                    GifWallpaperService::class.qualifiedName ?: return
                )
            )
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(intent)
    }
}
