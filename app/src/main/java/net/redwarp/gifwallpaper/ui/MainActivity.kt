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
package net.redwarp.gifwallpaper.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.redwarp.gifwallpaper.GifApplication
import net.redwarp.gifwallpaper.GifWallpaperService
import net.redwarp.gifwallpaper.R

const val EXTRA_PREVIEW_MODE = "android.service.wallpaper.PREVIEW_MODE"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val context = LocalContext.current
                val isWallpaperSet by checkOnResume {
                    isWallpaperSet(context)
                }
                val isPreview = remember {
                    isPreviewMode()
                }

                if (isWallpaperSet || isPreview) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "setup") {
                        composable("setup") {
                            SetupUi(
                                flowBasedModel = GifApplication.app.model,
                                navController = navController
                            )
                        }
                        composable("privacy") {
                            MarkdownUi(
                                fileName = "privacy.md",
                                title = stringResource(id = R.string.privacy),
                                navController = navController
                            )
                        }
                        composable("about") {
                            MarkdownUi(
                                fileName = "about.md",
                                title = stringResource(id = R.string.about),
                                navController = navController
                            )
                        }
                        composable("settings") {
                            SettingUi(
                                appSettings = GifApplication.app.appSettings,
                                navController = navController
                            )
                        }
                    }
                } else {
                    LauncherUi {
                        activateWallpaper(context)
                    }
                }
            }
        }
    }

    private fun isWallpaperSet(context: Context): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(context)
        return wallpaperManager.wallpaperInfo?.let {
            it.packageName == context.packageName
        } ?: false
    }
}

fun activateWallpaper(context: Context) {
    try {
        context.startActivity(
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, GifWallpaperService::class.java)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(
                Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_wallpaper_chooser, Toast.LENGTH_LONG).show()
        }
    }
}

fun Context.isPreviewMode(): Boolean {
    return if (this is Activity) {
        this.intent.getBooleanExtra(EXTRA_PREVIEW_MODE, false)
    } else {
        false
    }
}
