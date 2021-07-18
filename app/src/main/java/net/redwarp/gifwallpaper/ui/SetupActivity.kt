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

import android.app.WallpaperManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.databinding.ActivitySetupBinding
import net.redwarp.gifwallpaper.util.isDarkMode
import net.redwarp.gifwallpaper.util.setStatusBarColor

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class SetupActivity : AppCompatActivity() {
    lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController: NavController
    lateinit var bindings: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindings = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(bindings.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.launcher, R.id.setup),
            bindings.drawerLayout
        )
        bindings.toolbar.setupWithNavController(
            navController,
            appBarConfiguration
        )
        bindings.navigationView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.launcher) {
                bindings.toolbar.isVisible = false
                bindings.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                bindings.toolbar.isVisible = true
                bindings.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            }

            // val layoutParams =
            //     bindings.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
            // if (destination.id == R.id.setup) {
            //     layoutParams.behavior = null
            // } else {
            //     layoutParams.behavior = AppBarLayout.ScrollingViewBehavior()
            // }
        }
        bindings.appBarLayout.outlineProvider = null

        setStatusBarColor(isDarkMode)
    }

    override fun onResume() {
        super.onResume()

        if (isWallpaperSet(this) && navController.currentDestination?.id == R.id.launcher) {
            navController.navigate(
                R.id.setup,
                null,
                NavOptions.Builder().setPopUpTo(R.id.launcher, true)
                    .build()
            )
        } else if (!isWallpaperSet(this) && navController.currentDestination?.id != R.id.launcher) {
            navController.navigate(
                R.id.launcher,
                null,
                NavOptions.Builder().setPopUpTo(R.id.setup, true)
                    .build()
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun isWallpaperSet(context: Context): Boolean {
        return true

        val wallpaperManager = WallpaperManager.getInstance(context)
        return wallpaperManager.wallpaperInfo?.let {
            it.packageName == context.packageName
        } ?: false
    }
}
