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

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.SparseArray
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Colors
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import org.commonmark.node.Text as TextNode

private const val TAG_URL = "url"
private const val TAG_IMAGE_URL = "imageUrl"
private val BLOCK_PADDING = 8.dp

/**
 * Based on https://www.hellsoft.se/rendering-markdown-with-jetpack-compose/
 */
@Composable
fun MDDocument(document: Document, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .then(modifier)
    ) {
        MDBlockChildren(document)
    }
}

@Composable
fun MDDocument(markdown: String, modifier: Modifier = Modifier) {
    val document = remember {
        val parser = Parser.builder().build()
        parser.parse(markdown) as Document
    }

    MDDocument(document = document, modifier = modifier)
}

@Composable
fun MDHeading(heading: Heading, modifier: Modifier = Modifier) {
    val style = when (heading.level) {
        1 -> MaterialTheme.typography.h1
        2 -> MaterialTheme.typography.h2
        3 -> MaterialTheme.typography.h3
        4 -> MaterialTheme.typography.h4
        5 -> MaterialTheme.typography.h5
        6 -> MaterialTheme.typography.h6
        else -> {
            // Invalid header...
            MDBlockChildren(heading)
            return
        }
    }

    val padding = if (heading.parent is Document) BLOCK_PADDING else 0.dp
    Box(modifier = modifier.padding(bottom = padding)) {
        val text = buildAnnotatedString {
            appendMarkdownChildren(heading, MaterialTheme.colors)
        }
        MarkdownText(text, style)
    }
}

@Composable
fun MDParagraph(paragraph: Paragraph, modifier: Modifier = Modifier) {
    if (paragraph.firstChild is Image && paragraph.firstChild == paragraph.lastChild) {
        // Paragraph with single image
        MDImage(paragraph.firstChild as Image, modifier)
    } else {
        val padding = if (paragraph.parent is Document) BLOCK_PADDING else 0.dp
        Box(modifier = modifier.padding(bottom = padding)) {
            val styledText = buildAnnotatedString {
                pushStyle(MaterialTheme.typography.body1.toSpanStyle())
                appendMarkdownChildren(paragraph, MaterialTheme.colors)
                pop()
            }
            MarkdownText(styledText, MaterialTheme.typography.body1)
        }
    }
}

@Composable
fun MDImage(image: Image, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AsyncImage(model = image.destination, contentDescription = image.title)
    }
}

@Composable
fun MDBulletList(bulletList: BulletList, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalListLevel provides LocalListLevel.current.next()) {
        val marker = when (LocalListLevel.current.level) {
            1 -> "•"
            2 -> "◦"
            3 -> "▪"
            else -> "▫"
        }
        MDListItems(bulletList, modifier = modifier) {
            MDListRow("$marker ") {
                val text = buildAnnotatedString {
                    pushStyle(MaterialTheme.typography.body1.toSpanStyle())
                    appendMarkdownChildren(it, MaterialTheme.colors)
                    pop()
                }
                MarkdownText(text, MaterialTheme.typography.body1, modifier)
            }
        }
    }
}

@Composable
fun MDOrderedList(orderedList: OrderedList, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalListLevel provides LocalListLevel.current.next()) {
        var number = orderedList.startNumber
        val delimiter = orderedList.delimiter
        MDListItems(orderedList, modifier) {
            MDListRow("${number++}$delimiter ") {
                val text = buildAnnotatedString {
                    pushStyle(MaterialTheme.typography.body1.toSpanStyle())
                    appendMarkdownChildren(it, MaterialTheme.colors)
                    pop()
                }
                MarkdownText(text, MaterialTheme.typography.body1, modifier)
            }
        }
    }
}

@Composable
fun MDListItems(
    listBlock: ListBlock,
    modifier: Modifier = Modifier,
    item: @Composable (node: Node) -> Unit
) {
    val bottom = if (listBlock.parent is Document) BLOCK_PADDING else 0.dp
    val start = if (listBlock.parent is Document) 0.dp else 8.dp
    MDList(modifier = modifier.padding(start = start, bottom = bottom)) {
        for (listItem in listBlock.children()) {
            for (child in listItem.children()) {
                when (child) {
                    is BulletList -> MDBulletList(child, modifier)
                    is OrderedList -> MDOrderedList(child, modifier)
                    else -> item(child)
                }
            }
        }
    }
}

@Composable
fun MDBlockQuote(blockQuote: BlockQuote, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colors.primary
    val padding = if (blockQuote.parent is Document) BLOCK_PADDING else 0.dp
    Box(
        modifier = modifier
            .padding(bottom = padding)
            .padding(start = 8.dp, end = 8.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background.copy(alpha = 0.1f))
            .drawBehind {
                drawLine(
                    color = color,
                    strokeWidth = 4.dp.toPx(),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height)
                )
            }
            .padding(8.dp)
    ) {
        val text = buildAnnotatedString {
            pushStyle(
                MaterialTheme.typography.body1.toSpanStyle()
                    .plus(SpanStyle(fontStyle = FontStyle.Italic))
            )
            appendMarkdownChildren(blockQuote, MaterialTheme.colors)
            pop()
        }
        Text(text, modifier)
    }
}

@Composable
fun MDFencedCodeBlock(fencedCodeBlock: FencedCodeBlock, modifier: Modifier = Modifier) {
    val padding = if (fencedCodeBlock.parent is Document) BLOCK_PADDING else 0.dp
    Box(
        modifier = modifier
            .padding(bottom = padding)
            .background(MaterialTheme.colors.background)
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = fencedCodeBlock.literal.trim('\n'),
            style = TextStyle(fontFamily = FontFamily.Monospace),
            modifier = modifier
        )
    }
}

@Composable
fun MDIndentedCodeBlock(indentedCodeBlock: IndentedCodeBlock, modifier: Modifier = Modifier) {
    val padding = if (indentedCodeBlock.parent is Document) BLOCK_PADDING else 0.dp
    Box(
        modifier = modifier
            .padding(bottom = padding)
            .background(MaterialTheme.colors.background)
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = indentedCodeBlock.literal.trim('\n'),
            style = TextStyle(fontFamily = FontFamily.Monospace),
            modifier = modifier
        )
    }
}

@Composable
fun MDThematicBreak(modifier: Modifier = Modifier) {
    Divider(modifier = modifier.padding(PaddingValues(horizontal = 0.dp, vertical = 8.dp)))
}

@Composable
fun MDBlockChildren(parent: Node) {
    for (child in parent.children()) {
        when (child) {
            is BlockQuote -> MDBlockQuote(child)
            is ThematicBreak -> MDThematicBreak()
            is Heading -> MDHeading(child)
            is Paragraph -> MDParagraph(child)
            is FencedCodeBlock -> MDFencedCodeBlock(child)
            is IndentedCodeBlock -> MDIndentedCodeBlock(child)
            is Image -> MDImage(child)
            is BulletList -> MDBulletList(child)
            is OrderedList -> MDOrderedList(child)
        }
    }
}

fun Builder.appendMarkdownChildren(
    parent: Node,
    colors: Colors
) {
    for (child in parent.children()) {
        when (child) {
            is Paragraph -> {
                pushStyle(ParagraphStyle())
                appendMarkdownChildren(child, colors)
                pop()
            }
            is TextNode -> append(child.literal)
            is Image -> appendInlineContent(TAG_IMAGE_URL, child.destination)
            is Emphasis -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendMarkdownChildren(child, colors)
                pop()
            }
            is StrongEmphasis -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendMarkdownChildren(child, colors)
                pop()
            }
            is Code -> {
                pushStyle(TextStyle(fontFamily = FontFamily.Monospace).toSpanStyle())
                append(child.literal)
                pop()
            }
            is HardLineBreak -> {
                append("\n")
            }
            is SoftLineBreak -> {
                append(" ")
            }
            is Link -> {
                val underline = SpanStyle(colors.primary, textDecoration = TextDecoration.Underline)
                pushStyle(underline)
                pushStringAnnotation(TAG_URL, child.destination)
                appendMarkdownChildren(child, colors)
                pop()
                pop()
            }
        }
    }
}

@Composable
fun MarkdownText(text: AnnotatedString, style: TextStyle, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                layoutResult.value?.let { layoutResult ->
                    val position = layoutResult.getOffsetForPosition(offset)
                    text.getStringAnnotations(position, position)
                        .firstOrNull()
                        ?.let { sa ->
                            if (sa.tag == TAG_URL) {
                                uriHandler.openUri(sa.item)
                            }
                        }
                }
            }
        },
        style = style,
        inlineContent = mapOf(
            TAG_IMAGE_URL to InlineTextContent(
                Placeholder(style.fontSize, style.fontSize, PlaceholderVerticalAlign.Bottom)
            ) {
                AsyncImage(model = it, contentDescription = null, alignment = Alignment.Center)
            }
        ),
        onTextLayout = { layoutResult.value = it }
    )
}

private sealed class MDListPlaceable {
    class WithSeparator(val separator: Placeable, val block: Placeable) : MDListPlaceable()
    class Solo(val block: Placeable) : MDListPlaceable()
}

private enum class LayoutId {
    Delimiter
}

@Composable
fun MDList(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = { measurables, constraints ->
            var height = 0
            var alignment = 0

            val placeables = mutableListOf<MDListPlaceable>()

            val separators = SparseArray<Placeable>()
            measurables.mapIndexedNotNull { index, measurable ->
                if (measurable.layoutId == LayoutId.Delimiter) {
                    index to measurable
                } else {
                    null
                }
            }.forEach { (index, separator) ->
                val placeable = separator.measure(constraints)
                alignment = kotlin.math.max(placeable.width, alignment)
                separators[index] = placeable
            }

            var index = 0
            while (index < measurables.size) {
                val measurable = measurables[index]
                if (measurable.layoutId == LayoutId.Delimiter && index + 1 < measurables.size) {
                    val nextMeasurable = measurables[index + 1]
                    val separatorPlaceable = separators[index]
                    val blockPlaceable =
                        nextMeasurable.measure(constraints.copy(maxWidth = constraints.maxWidth - alignment))
                    height += blockPlaceable.height
                    alignment = kotlin.math.max(separatorPlaceable.width, alignment)

                    placeables.add(
                        MDListPlaceable.WithSeparator(
                            separatorPlaceable,
                            blockPlaceable
                        )
                    )

                    index += 2
                } else if (measurable.layoutId == LayoutId.Delimiter) {
                    // Weird, the last item is a delimiter, how did that happen?
                    index += 1
                } else {
                    val placeable = measurable.measure(constraints)
                    height += placeable.height

                    placeables.add(MDListPlaceable.Solo(placeable))

                    index += 1
                }
            }

            layout(constraints.maxWidth, height) {
                var y = 0
                for (placeable in placeables) {
                    y += when (placeable) {
                        is MDListPlaceable.WithSeparator -> {
                            placeable.separator.placeRelative(0, y)
                            placeable.block.placeRelative(alignment, y)
                            placeable.block.height
                        }
                        is MDListPlaceable.Solo -> {
                            placeable.block.placeRelative(0, y)
                            placeable.block.height
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun MDListRow(separator: String, content: @Composable BoxScope.() -> Unit) {
    Text(
        text = separator,
        modifier = Modifier.layoutId(LayoutId.Delimiter),
        style = MaterialTheme.typography.body1
    )
    Box {
        content()
    }
}

fun Node.children(): NodeIterator = NodeIterator(this)

class NodeIterator(node: Node) : Iterator<Node> {
    var child: Node? = node.firstChild

    override fun hasNext(): Boolean = child != null

    override fun next(): Node {
        val next = requireNotNull(child)
        child = next.next
        return next
    }
}

@Preview
@Composable
fun MDListPreview() {
    MDList(modifier = Modifier) {
        MDListRow("1.") {
            Text(text = "Hello, let's see how this performs with a long text that wraps on itself.")
        }
        MDListRow("-") {
            Text("What is this?")
        }
        Text(text = "Hello")
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(uiMode = UI_MODE_NIGHT_NO)
@Composable
fun MarkdownPreview() {
    val markdownText = """
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
        
            And this is an indented
            code block
        
        I'm not sure what is going to happen here.
        
        ---
        
        A list?
        - Okay!
        - Well
          1. Not sure. Let's have a super long text, so we can see how wrapping looks like.
            Well it does not look nice. We can do better I'm sure!
          2. About that
        - Test
          - Multiple
            - Levels
              - Where does it ends?
            - Hard to say
    """.trimIndent()

    MaterialTheme(colors = if (isSystemInDarkTheme()) darkColors() else lightColors()) {
        Surface {
            MDDocument(markdown = markdownText)
        }
    }
}

private data class ListLevel(val level: Int) {
    fun next(): ListLevel = copy(level = level + 1)
}

private val LocalListLevel = compositionLocalOf { ListLevel(0) }
