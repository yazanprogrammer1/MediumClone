package com.example.mediumclone.ui.screens.article

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownRenderer(
    content: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {}
) {
    val lines = content.lines()
    var inCodeBlock = false
    var codeBlockContent = ""
    var currentParagraph = StringBuilder()



    SelectionContainer {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            lines.forEach { line ->
                // Code Block Handling
                if (line.trim().startsWith("```")) {
                    // Flush text before code block
                    if (currentParagraph.isNotBlank()) {
                        Paragraph(currentParagraph.toString().trim(), onLinkClick)
                        currentParagraph.clear()
                    }
                    if (inCodeBlock) {
                        // End of code block
                        CodeBlock(codeBlockContent)
                        codeBlockContent = ""
                        inCodeBlock = false
                    } else {
                        // Start of code block
                        inCodeBlock = true
                    }
                    return@forEach
                }

                if (inCodeBlock) {
                    codeBlockContent += line + "\n"
                    return@forEach
                }

                // Standard Blocks
                val isHeader = line.startsWith("# ")
                val isQuote = line.startsWith("> ")
                val isList = line.trim().startsWith("- ") || line.trim().startsWith("* ")
                val isImage = line.trim().startsWith("![") && line.contains("](")
                val isBlank = line.isBlank()

                if (isHeader || isQuote || isList || isImage || isBlank) {
                    // Flush accumulated paragraph text
                    if (currentParagraph.isNotBlank()) {
                        Paragraph(currentParagraph.toString().trim(), onLinkClick)
                        currentParagraph.clear()
                    }
                    
                    when {
                        isHeader -> Header(line.removePrefix("# "))
                        isQuote -> Quote(line.removePrefix("> "), onLinkClick)
                        isList -> BulletList(line.trim().substring(2), onLinkClick)
                        isImage -> MarkdownImage(line)
                        // Blank lines just flush
                    }
                } else {
                    if (currentParagraph.isNotEmpty()) {
                        currentParagraph.append("\n")
                    }
                    currentParagraph.append(line)
                }
            }
            
            // Flush remaining content
            if (currentParagraph.isNotBlank()) {
                Paragraph(currentParagraph.toString().trim(), onLinkClick)
                currentParagraph.clear()
            }
            
            // If code block wasn't closed properly
            if (inCodeBlock && codeBlockContent.isNotEmpty()) {
                CodeBlock(codeBlockContent)
            }
        }
    }
}

@Composable
private fun MarkdownImage(line: String) {
    // Parse ![Alt](Url)
    val regex = Regex("!\\[(.*?)\\]\\((.*?)\\)")
    val match = regex.find(line)
    val imageUrl = match?.groupValues?.get(2)
    
    if (imageUrl != null) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            coil.compose.AsyncImage(
                model = imageUrl,
                contentDescription = match.groupValues.get(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(), // Adjust as needed
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
            )
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun Paragraph(text: String, onLinkClick: (String) -> Unit) {
    val styledText = parseInlineStyles(text)
    ClickableText(
        text = styledText,
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            styledText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                }
        }
    )
}

@Composable
private fun Quote(text: String, onLinkClick: (String) -> Unit) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        val styledText = parseInlineStyles(text)
        ClickableText(
            text = styledText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            onClick = { offset ->
                styledText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onLinkClick(annotation.item)
                    }
            }
        )
    }
}

@Composable
private fun BulletList(text: String, onLinkClick: (String) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 8.dp)
        )
        Paragraph(text, onLinkClick)
    }
}

@Composable
private fun CodeBlock(content: String) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            kotlinx.coroutines.delay(2000)
            isCopied = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = content.trim(),
            modifier = Modifier
                .padding(16.dp)
                .padding(top = 24.dp) // Space for copy button
                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        // Premium Copy Button
        IconButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(content.trim()))
                isCopied = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy code",
                tint = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun parseInlineStyles(text: String): androidx.compose.ui.text.AnnotatedString {
    val colorScheme = MaterialTheme.colorScheme
    return buildAnnotatedString {
        var remaining = text
        
        while (remaining.isNotEmpty()) {
            // Find next occurrences of our tokens
            // Find next occurrences of our tokens
            val boldMatch = Regex("(?s)\\*\\*(.*?)\\*\\*").find(remaining)
            val italicMatch = Regex("(?s)(?<!\\*)\\*([^*]+)\\*(?!\\*)").find(remaining)
            // Fix: Use strictly non-greedy and explicit delimiters for links to avoid capturing wrong groups
            val linkMatch = Regex("(?s)\\[([^\\[\\]]*?)\\]\\(([^()]*?)\\)").find(remaining)
            
            // Determine which comes first
            val matches = listOfNotNull(boldMatch, italicMatch, linkMatch)
            if (matches.isEmpty()) {
                append(remaining)
                break
            }
            
            val firstMatch = matches.minByOrNull { it.range.first }!!
            
            // Append text before match
            if (firstMatch.range.first > 0) {
                 append(remaining.substring(0, firstMatch.range.first))
            }
            
            // Process match
            when (firstMatch) {
                boldMatch -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(firstMatch.groupValues[1])
                    }
                }
                italicMatch -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(firstMatch.groupValues[1])
                    }
                }
                linkMatch -> {
                    val linkText = firstMatch.groupValues[1]
                    val linkUrl = firstMatch.groupValues[2]
                    
                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(SpanStyle(color = colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                        append(linkText)
                    }
                    pop()
                }
            }
            
            // Advance
            remaining = remaining.substring(firstMatch.range.last + 1)
        }
    }
}
