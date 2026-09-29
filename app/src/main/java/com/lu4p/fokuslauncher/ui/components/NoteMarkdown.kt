package com.lu4p.fokuslauncher.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em

/**
 * Renders the small Markdown subset supported by the home note:
 * - `- item`, `* item`, `+ item`: `• item`
 * - `- [ ] task` / `- [x] task`: `☐ task` / `☑ task`
 * - `# Heading`, ..., `###### Heading`: bold and larger font for levels 1–2
 * - inline `**bold**`, `*italic*`, `~~strike~~`, and `\` escapes
 */
fun renderNoteMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, line ->
        if (index > 0) append('\n')
        appendLine(line)
    }
}

private val taskRegex = Regex("""^(\s*)[-*+] \[([ xX])] (.*)$""")
private val bulletRegex = Regex("""^(\s*)[-*+] (.*)$""")
private val headingRegex = Regex("""^(#{1,6}) +(.*)$""")

private fun AnnotatedString.Builder.appendLine(line: String) {
    taskRegex.matchEntire(line)?.let { match ->
        val (indent, mark, body) = match.destructured
        val done = mark != " "
        append(indent)
        append(if (done) "☑ " else "☐ ")
        if (done) {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { appendInline(body) }
        } else {
            appendInline(body)
        }
        return
    }
    bulletRegex.matchEntire(line)?.let { match ->
        val (indent, body) = match.destructured
        append(indent)
        append("• ")
        appendInline(body)
        return
    }
    headingRegex.matchEntire(line)?.let { match ->
        val (hashes, body) = match.destructured
        val size =
                when (hashes.length) {
                    1 -> 1.3.em
                    2 -> 1.15.em
                    else -> 1.em
                }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = size)) { appendInline(body) }
        return
    }
    appendInline(line)
}

private const val ESCAPABLE = "\\*~#-+[]_`"

/** Appends [text] with inline emphasis. Unmatched markers are kept as literal characters. */
private fun AnnotatedString.Builder.appendInline(text: String) {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\\' && i + 1 < text.length && text[i + 1] in ESCAPABLE) {
            append(text[i + 1])
            i += 2
            continue
        }
        if (text.startsWith("**", i)) {
            val end = findClosingDouble(text, i + 2, "**")
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInline(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        } else if (text.startsWith("~~", i)) {
            val end = findClosingDouble(text, i + 2, "~~")
            if (end != -1) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    appendInline(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        } else if (c == '*') {
            val end = findClosingItalic(text, i + 1)
            if (end != -1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInline(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        append(c)
        i++
    }
}

/** Content must be non-empty and not start or end with a space, so `2 ** 3 ** 4` stays literal. */
private fun findClosingDouble(text: String, from: Int, marker: String): Int {
    if (from >= text.length || text[from] == ' ') return -1
    var j = text.indexOf(marker, from + 1)
    while (j != -1) {
        if (text[j - 1] != ' ' && text[j - 1] != '\\') return j
        j = text.indexOf(marker, j + 1)
    }
    return -1
}

/** Finds a lone `*` that closes an italic run, skipping `**` pairs used for nested bold. */
private fun findClosingItalic(text: String, from: Int): Int {
    if (from >= text.length || text[from] == ' ' || text[from] == '*') return -1
    for (j in from + 1 until text.length) {
        if (text[j] != '*') continue
        val partOfDouble = text.getOrNull(j + 1) == '*' || text[j - 1] == '*'
        if (!partOfDouble && text[j - 1] != ' ' && text[j - 1] != '\\') return j
    }
    return -1
}
