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

import android.content.Context
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator

object AnimUtils {
    private var fastOutLinearInInterpolator: Interpolator? = null
    private var linearOutSlowInInterpolator: Interpolator? = null

    fun getFastOutLinearInInterpolator(context: Context): Interpolator {
        fastOutLinearInInterpolator?.let { return it }

        AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in).let {
            fastOutLinearInInterpolator = it
            return it
        }
    }

    fun getLinearOutSlowInInterpolator(context: Context): Interpolator {
        linearOutSlowInInterpolator?.let { return it }

        AnimationUtils.loadInterpolator(context, android.R.interpolator.linear_out_slow_in).let {
            linearOutSlowInInterpolator = it
            return it
        }
    }
}
