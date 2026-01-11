package com.example.mediumclone.ui.screens.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp

class MarkdownTransformation(
    private val colorScheme: androidx.compose.material3.ColorScheme
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = buildAnnotatedStringWithMarkdown(text.text, colorScheme),
            offsetMapping = OffsetMapping.Identity
        )
    }

    private fun buildAnnotatedStringWithMarkdown(text: String, colorScheme: androidx.compose.material3.ColorScheme): AnnotatedString {
        val builder = AnnotatedString.Builder(text)

        // Bold (**text**)
        val boldRegex = Regex("(?s)\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(text).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Italic (*text*)
        // Match *text* but ensure it's not part of **text**
        // valid: *italic*
        // invalid: **bold** (the inner part shouldn't match as italic if it wraps the whole thing)
        // This is tricky with simple regex.
        // Better approach: Match *text* where * is not preceded or followed by another *
        
        val italicRegex = Regex("(?s)(?<!\\*)\\*([^*]+)\\*(?!\\*)")
        italicRegex.findAll(text).forEach { match ->
             builder.addStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Links [text](url)
        val linkRegex = Regex("(?s)\\[([^\\[\\]]*?)\\]\\(([^()]*?)\\)")
        linkRegex.findAll(text).forEach { match ->
            // Style the [text] part
            val textStart = match.range.first
            val textEnd = match.groups[1]?.range?.last?.plus(1) ?: match.range.last
            // We color the whole thing or just the text?
            // "Live Preview" usually keeps raw text but colors it.
            // Let's color the whole block to indicate it's a link structure
            builder.addStyle(
                style = SpanStyle(
                    color = colorScheme.primary,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Headers (# Text)
        val headerRegex = Regex("(?m)^#\\s+(.*)$")
        headerRegex.findAll(text).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Quote (> Text)
        val quoteRegex = Regex("(?m)^>\\s+(.*)$")
        quoteRegex.findAll(text).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    fontStyle = FontStyle.Italic,
                    color = colorScheme.secondary,
                    background = colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Inline Code (`text`)
        val codeRegex = Regex("`([^`]+)`")
        codeRegex.findAll(text).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = colorScheme.surfaceVariant,
                    color = colorScheme.onSurfaceVariant
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
        
         // Code Block (```...```)
        val codeBlockRegex = Regex("(?s)```(.*?)```")
        codeBlockRegex.findAll(text).forEach { match ->
            builder.addStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = colorScheme.inverseOnSurface.copy(alpha = 0.5f), // Darker bg
                    color = colorScheme.onSurface
                ),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        return builder.toAnnotatedString()
    }
}
