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
package net.redwarp.gifwallpaper

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isWallpaperSet(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_launcher)
    }

    override fun onResume() {
        super.onResume()
        if (isWallpaperSet(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        }
    }

    private fun isWallpaperSet(context: Context): Boolean {
        return true

        // val wallpaperManager = WallpaperManager.getInstance(context)
        // return wallpaperManager.wallpaperInfo?.let {
        //     it.packageName == context.packageName
        // } ?: false
    }
}
