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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.redwarp.gifwallpaper.GifApplication
import net.redwarp.gifwallpaper.GifWallpaperService
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.renderer.DrawableMapper
import net.redwarp.gifwallpaper.ui.setup.SetupModelImpl
import net.redwarp.gifwallpaper.wallpaperActive

const val EXTRA_PREVIEW_MODE = "android.service.wallpaper.PREVIEW_MODE"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AppTheme {
                val context = LocalContext.current
                val isWallpaperSet by wallpaperActive.collectAsState()
                val isPreview = remember {
                    isPreviewMode()
                }

                if (isWallpaperSet || isPreview) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Routes.SETUP) {
                        composable(Routes.SETUP) {
                            val scope = rememberCoroutineScope()
                            val setupModel = remember {
                                val drawableProvider =
                                    DrawableMapper.previewMapper(
                                        context = context,
                                        flowBasedModel = GifApplication.app.model,
                                        scope = scope,
                                    )

                                SetupModelImpl(GifApplication.app.model, drawableProvider)
                            }

                            SetupUi(
                                setupModel = setupModel,
                                navController = navController
                            )
                        }
                        composable(Routes.ABOUT) {
                            MarkdownUi(
                                fileName = "about.md",
                                title = stringResource(id = R.string.about),
                                navController = navController
                            )
                        }
                        composable(Routes.SETTINGS) {
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

private fun Context.isPreviewMode(): Boolean {
    return if (this is Activity) {
        intent.getBooleanExtra(EXTRA_PREVIEW_MODE, false)
    } else {
        false
    }
}
