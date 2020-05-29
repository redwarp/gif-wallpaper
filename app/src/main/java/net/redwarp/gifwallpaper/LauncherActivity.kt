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
package net.redwarp.gifwallpaper

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_launcher.*

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isWallpaperSet(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_launcher)
        setupActionBar()
    }

    override fun onResume() {
        super.onResume()
        if (isWallpaperSet(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        toolbar.setOnApplyWindowInsetsListener { v, insets ->
            toolbar.y = insets.systemWindowInsetTop.toFloat()
            insets
        }
        supportActionBar?.title = null
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

            statusBarColor = Color.TRANSPARENT
        }
    }

    private fun isWallpaperSet(context: Context): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(context)
        return wallpaperManager.wallpaperInfo?.let {
            it.packageName == context.packageName
        } ?: false
    }
}
