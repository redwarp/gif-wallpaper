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
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomappbar.BottomAppBar

class CustomBehavior : CoordinatorLayout.Behavior<View> {
    constructor() : super()
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return dependency is BottomAppBar
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        child.y = -parent.topOnScreen.toFloat()
        // child.y = -300f
        // child.x = 100f
        // ViewCompat.offsetTopAndBottom(child, -parent.topOnScreen)

        Log.d("CustomBehavior", "onDependentViewChanged $child")

        return true
    }

    override fun getInsetDodgeRect(parent: CoordinatorLayout, child: View, rect: Rect): Boolean {
        return super.getInsetDodgeRect(parent, child, rect)
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: View,
        layoutDirection: Int
    ): Boolean {
        val contentView = findContentView(child)
        return if (contentView == null) false
        else {
            Log.d("CustomBehavior", "onLayoutChild $child")
            child.layout(
                contentView.left,
                contentView.top,
                contentView.right,
                contentView.bottom
            )
            true
        }
    }

    private fun findContentView(child: View): View? {
        var parent: View? = child.parent as? View
        while (true) {
            if (parent == null) return null
            else if (parent.id == android.R.id.content) return parent

            parent = parent.parent as? View
        }
    }
}

private val workLocationArray = IntArray(2)

val View.topOnScreen: Int
    get() {
        getLocationOnScreen(workLocationArray)
        return workLocationArray[1]
    }
