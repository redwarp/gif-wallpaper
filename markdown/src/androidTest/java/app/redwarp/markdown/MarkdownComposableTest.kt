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
package app.redwarp.markdown

import androidx.compose.material.Text
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.commonmark.node.Document
import org.commonmark.parser.Parser
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class MarkdownComposableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun quoteSoftLineBreak() {
        val markdown = """
            > Well
            > this
            > is
            > awkward.
        """.trimIndent()

        val parser = Parser.builder().build()
        val document = parser.parse(markdown) as Document

        composeTestRule.setContent {
            MDDocument(document = document)
        }

        composeTestRule.onNodeWithText("Well this is awkward.").assertExists()
    }

    @Test
    fun quoteHardLineBreak() {
        val markdown = """
            > Break after this!
            > 
            > Did it work?
        """.trimIndent()

        val parser = Parser.builder().build()
        val document = parser.parse(markdown) as Document

        composeTestRule.setContent {
            MDDocument(document = document)
        }

        composeTestRule.onNode(
            hasText("Break after this!", true)
                .and(hasText("Did it work?", true))
        ).assertExists()
    }

    @Test
    fun blockCode() {
        val markdown = """
            ```
            This is code!
            ```
        """.trimIndent()

        val parser = Parser.builder().build()
        val document = parser.parse(markdown) as Document

        composeTestRule.setContent {
            MDDocument(document = document)
        }

        composeTestRule.onNodeWithText("This is code!").assertExists()
    }

    @Test
    @Ignore("Just a way to run debug code EZ")
    fun wholeShenanigans() {
        val markdown = """
        # Hello
        
        ## Second title
        This is some paragraph with `inline code`, *italic* and **bold**
        
        > This is a quoted block, but well this is not perfect,
        > we can work on that!
        >
        > There should be a new line.
        
        > Hello
        
        ```
        This is some code
        ```
        
        A list?
        - Okay!
        - Well
          1. Not sure. Let's have a super long text, so we can see how wrapping looks like.
            Well it does not look nice. We can do better I'm sure!
          2. About that
        """.trimIndent()

        val parser = Parser.builder().build()
        val document = parser.parse(markdown) as Document

        composeTestRule.setContent {
            MDDocument(document = document)
        }

        composeTestRule.onNode(
            hasText("Break after this!", true)
                .and(hasText("Did it work?", true))
        ).assertExists()
    }

    @Test
    fun mdList() {
        composeTestRule.setContent {
            MDList {
                MDListRow("1.") {
                    Text(text = "Hello")
                }
                MDListRow("-") {
                    Text(text = "What is this?")
                }
            }
        }

        composeTestRule.onNodeWithText("1.").assertExists()
        composeTestRule.onNode(hasText("-")).assertExists()
    }
}
