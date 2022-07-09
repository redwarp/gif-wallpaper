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

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.google.accompanist.insets.ProvideWindowInsets
import net.redwarp.gifwallpaper.R
import net.redwarp.gifwallpaper.ui.AppTheme
import net.redwarp.gifwallpaper.ui.LauncherUi
import net.redwarp.gifwallpaper.ui.SettingUi
import net.redwarp.gifwallpaper.ui.SetupUi
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.locale.LocaleTestRule

class ScreenshotTest {
    @get:Rule
    val composeRule = createScreenshotComposeRule()

    @get:Rule
    val localeTestRule = LocaleTestRule()

    @Test
    fun launcher() {
        composeRule.setContent {
            TestTheme {
                LauncherUi {}
            }
        }
        composeRule.takeScreenshot("1_launcher")
    }

    @Test
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            TestTheme {
                SetupUi(
                    setupModel = FakeSetupModel(context), navController = rememberNavController()
                )
            }
        }

        composeRule.takeScreenshot("2_setup")
    }

    @Test
    fun setup_color_picker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            TestTheme {
                SetupUi(
                    setupModel = FakeSetupModel(context), navController = rememberNavController()
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.change_color)).performClick()

        composeRule.takeScreenshot("3_color_picker")
    }

    @Test
    fun settings() {
        composeRule.setContent {
            TestTheme {
                SettingUi(navController = rememberNavController(), appSettings = FakeAppSettings())
            }
        }

        composeRule.takeScreenshot("4_settings")
    }
}

@Composable
@Suppress("TestFunctionName")
fun TestTheme(content: @Composable () -> Unit) {
    ProvideWindowInsets {
        AppTheme(darkTheme = true, content = content)
    }
}
