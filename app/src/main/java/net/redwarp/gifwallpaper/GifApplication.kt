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

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.redwarp.gifwallpaper.data.FlowBasedModel
import net.redwarp.gifwallpaper.data.WallpaperSettings

// See https://medium.com/androiddevelopers/coroutines-patterns-for-work-that-shouldnt-be-cancelled-e26c40f142ad
// and https://developer.android.com/kotlin/coroutines/coroutines-best-practices#create-coroutines-data-layer
class GifApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var _appSettings: DataStoreAppSettings
    private lateinit var _flowBasedModel: FlowBasedModel
    val appSettings: AppSettings get() = _appSettings
    val model get() = _flowBasedModel

    override fun onCreate() {
        super.onCreate()

        val appSettings = DataStoreAppSettings(this, ioScope)
        _appSettings = appSettings

        val wallpaperSettings = WallpaperSettings(this, ioScope)
        _flowBasedModel = FlowBasedModel(this, appScope, wallpaperSettings, appSettings)

        instance = this
    }

    companion object {
        private lateinit var instance: GifApplication

        val app: GifApplication get() = instance
    }
}
