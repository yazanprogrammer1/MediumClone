package com.example.mediumclone.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun EditorToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onAIClick: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                description = "Bold",
                onClick = {
                    onValueChange(applyFormatting(value, "**", "**"))
                }
            )
            
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                description = "Italic",
                onClick = {
                    onValueChange(applyFormatting(value, "*", "*"))
                }
            )
            
            ToolbarButton(
                icon = Icons.Default.Title,
                description = "Header 1",
                onClick = {
                    onValueChange(applyPrefix(value, "# "))
                }
            )

            ToolbarButton(
                icon = Icons.Default.FormatQuote,
                description = "Quote",
                onClick = {
                    onValueChange(applyPrefix(value, "> "))
                }
            )
            
            ToolbarButton(
                icon = Icons.Default.Code,
                description = "Code Block",
                onClick = {
                    onValueChange(applyFormatting(value, "```\n", "\n```"))
                }
            )
            
            ToolbarButton(
                icon = Icons.Default.Link,
                description = "Link",
                onClick = {
                    onValueChange(applyFormatting(value, "[", "](url)"))
                }
            )
            
            ToolbarButton(
                icon = Icons.Default.Image,
                description = "Insert Image",
                onClick = onImageClick
            )

            // AI Button
            ToolbarButton(
                icon = Icons.Default.AutoAwesome,
                description = "Generate with AI",
                onClick = onAIClick
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun applyFormatting(
    value: TextFieldValue,
    prefix: String,
    suffix: String
): TextFieldValue {
    val text = value.text
    val selection = value.selection
    
    val newText = StringBuilder(text)
        .insert(selection.max, suffix)
        .insert(selection.min, prefix)
        .toString()
        
    // Cursor position: if selection empty, place cursor between tags. Else, select formatted text.
    val newCursorPos = if (selection.collapsed) {
        TextRange(selection.min + prefix.length)
    } else {
        TextRange(selection.min, selection.max + prefix.length + suffix.length)
    }
    
    return value.copy(text = newText, selection = newCursorPos)
}

private fun applyPrefix(
    value: TextFieldValue,
    prefix: String
): TextFieldValue {
    val text = value.text
    val selection = value.selection
    
    // Find start of the current line
    var lineStart = 0
    if (selection.min > 0) {
        lineStart = text.lastIndexOf('\n', selection.min - 1) + 1
    }
    
    val newText = StringBuilder(text)
        .insert(lineStart, prefix)
        .toString()
        
    return value.copy(
        text = newText,
        selection = TextRange(selection.min + prefix.length, selection.max + prefix.length)
    )
}
