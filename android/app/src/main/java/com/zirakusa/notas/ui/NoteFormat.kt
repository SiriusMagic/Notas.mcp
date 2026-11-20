package com.zirakusa.notas.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.zirakusa.notas.data.TextEdits

/**
 * Shows the plain-text marks of a note as formatting, without changing a single character position:
 * headings bigger, **bold**, *italic*, __underline__, ~~strike~~, ☐/☑ as round boxes (done lines crossed out) and tables
 * in monospace. The marks themselves are drawn faint (or invisible), so what is stored stays plain text.
 */
class NoteFormat(private val muted: Color, private val accent: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(format(text.text), OffsetMapping.Identity)

    fun format(raw: String): AnnotatedString {
        val shown = raw.replace(TextEdits.UNCHECKED, "○ ").replace(TextEdits.CHECKED, "◉ ")
        return buildAnnotatedString {
            append(shown)
            val hidden = SpanStyle(color = Color.Transparent, fontSize = 1.sp)
            val faint = SpanStyle(color = muted.copy(alpha = 0.5f))
            var start = 0
            for (line in raw.split('\n')) {
                val end = start + line.length
                val prefix = TextEdits.prefixOf(line)
                when {
                    prefix in TextEdits.headings -> {
                        val level = TextEdits.headings.indexOf(prefix)
                        addStyle(hidden, start, start + prefix.length)
                        addStyle(SpanStyle(fontSize = listOf(26, 23, 20, 18)[level].sp, fontWeight = FontWeight.Bold), start + prefix.length, end)
                    }
                    prefix == TextEdits.UNCHECKED -> addStyle(SpanStyle(color = muted, fontWeight = FontWeight.Bold), start, start + 1)
                    prefix == TextEdits.CHECKED -> {
                        addStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold), start, start + 1)
                        addStyle(SpanStyle(color = muted, textDecoration = TextDecoration.LineThrough), start + 2, end)
                    }
                    line.startsWith("|") -> {
                        addStyle(SpanStyle(fontFamily = FontFamily.Monospace), start, end)
                        line.forEachIndexed { i, c -> if (c == '|') addStyle(faint, start + i, start + i + 1) }
                    }
                }
                start = end + 1
            }
            for ((mark, style) in marks) {
                val pattern = if (mark == "*") "(?<!\\*)\\*(?=[^\\s*])(.+?)(?<=[^\\s*])\\*(?!\\*)"
                else Regex.escape(mark) + "(?=\\S)(.+?)(?<=\\S)" + Regex.escape(mark)
                Regex(pattern).findAll(raw).forEach { m ->
                    addStyle(faint, m.range.first, m.range.first + mark.length)
                    addStyle(faint, m.range.last + 1 - mark.length, m.range.last + 1)
                    addStyle(style, m.range.first + mark.length, m.range.last + 1 - mark.length)
                }
            }
        }
    }

    private val marks = listOf(
        "**" to SpanStyle(fontWeight = FontWeight.Bold),
        "~~" to SpanStyle(textDecoration = TextDecoration.LineThrough),
        "__" to SpanStyle(textDecoration = TextDecoration.Underline),
        "*" to SpanStyle(fontStyle = FontStyle.Italic),
    )
}
