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

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private val mutableWallpaperActive = MutableStateFlow(false)
val wallpaperActive = mutableWallpaperActive.asStateFlow()

class WallpaperObserver : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        mutableWallpaperActive.value = true
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mutableWallpaperActive.value = false
    }
}
