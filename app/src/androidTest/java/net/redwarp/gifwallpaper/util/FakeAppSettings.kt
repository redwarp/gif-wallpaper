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
package net.redwarp.gifwallpaper.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.redwarp.gifwallpaper.AppSettings

class FakeAppSettings : AppSettings {
    override val powerSavingSettingFlow: Flow<Boolean>
        get() = flowOf(true)
    override val thermalThrottleSettingFlow: Flow<Boolean>
        get() = flowOf(false)
    override val isThermalThrottleSupported: Boolean = true

    override suspend fun setPowerSaving(enabled: Boolean) = Unit

    override suspend fun setThermalThrottle(enabled: Boolean) = Unit
}
