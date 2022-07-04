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

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

fun ComposeContentTestRule.takeScreenshot(fileName: String, locale: String = "en-US") {
    onRoot().captureToImage().asAndroidBitmap().save(fileName, locale)
}

fun Bitmap.save(fileName: String, locale: String) {
    val screenshotDirectory = requireNotNull(
        InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("screenshots")
    )
    val languageDirectory = File(screenshotDirectory, locale).apply {
        mkdirs()
    }

    val file = File(languageDirectory, "$fileName.png")
    FileOutputStream(file).use { out ->
        compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

fun createScreenshotComposeRule() = createAndroidComposeRule<ScreenshotActivity>()
