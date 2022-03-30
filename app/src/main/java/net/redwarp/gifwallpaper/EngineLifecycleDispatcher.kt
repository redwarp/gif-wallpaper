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

import android.os.Handler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Helper class to dispatch lifecycle events for our wallpaper engine.
 * Adapted from [androidx.lifecycle.ServiceLifecycleDispatcher].
 */
class EngineLifecycleDispatcher(provider: LifecycleOwner) {
    private val registry: LifecycleRegistry = LifecycleRegistry(provider)
    @Suppress("DEPRECATION")
    private val handler: Handler = Handler()
    private var lastDispatchRunnable: DispatchRunnable? = null

    private fun postDispatchRunnable(event: Lifecycle.Event) {
        lastDispatchRunnable?.run()
        lastDispatchRunnable = DispatchRunnable(registry, event).also {
            handler.postAtFrontOfQueue(it)
        }
    }

    fun onResume() {
        postDispatchRunnable(Lifecycle.Event.ON_RESUME)
    }

    fun onCreate() {
        postDispatchRunnable(Lifecycle.Event.ON_CREATE)
    }

    fun onStop() {
        postDispatchRunnable(Lifecycle.Event.ON_STOP)
    }

    fun onDestroy() {
        postDispatchRunnable(Lifecycle.Event.ON_DESTROY)
    }

    val lifecycle: Lifecycle
        get() = registry

    internal class DispatchRunnable(
        private val registry: LifecycleRegistry,
        private val event: Lifecycle.Event
    ) : Runnable {
        private var wasExecuted = false
        override fun run() {
            if (!wasExecuted) {
                registry.handleLifecycleEvent(event)
                wasExecuted = true
            }
        }
    }
}
