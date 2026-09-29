package com.lu4p.fokuslauncher.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMarkdownTest {

    /** Text covered by each span, so assertions don't depend on span ordering or offsets. */
    private fun AnnotatedString.spanTexts(): List<Pair<String, SpanStyle>> =
            spanStyles.map { text.substring(it.start, it.end) to it.item }

    @Test
    fun plain_text_is_unchanged() {
        val rendered = renderNoteMarkdown("milk\neggs")
        assertEquals("milk\neggs", rendered.text)
        assertTrue(rendered.spanStyles.isEmpty())
    }

    @Test
    fun bullets_render_as_dots_and_keep_indent() {
        assertEquals(
                "• milk\n• eggs\n  • free range",
                renderNoteMarkdown("- milk\n* eggs\n  + free range").text,
        )
    }

    @Test
    fun tasks_render_as_checkboxes_and_strike_done_items() {
        val rendered = renderNoteMarkdown("- [ ] call mum\n- [x] buy milk\n* [X] pay rent")
        assertEquals("☐ call mum\n☑ buy milk\n☑ pay rent", rendered.text)
        val struck =
                rendered.spanTexts()
                        .filter { it.second.textDecoration == TextDecoration.LineThrough }
                        .map { it.first }
        assertEquals(listOf("buy milk", "pay rent"), struck)
    }

    @Test
    fun headings_are_bold_and_drop_hashes() {
        val rendered = renderNoteMarkdown("# Today\n### Later")
        assertEquals("Today\nLater", rendered.text)
        val bold =
                rendered.spanTexts()
                        .filter { it.second.fontWeight == FontWeight.Bold }
                        .map { it.first }
        assertEquals(listOf("Today", "Later"), bold)
    }

    @Test
    fun hashtag_without_space_is_not_a_heading() {
        assertEquals("#groceries", renderNoteMarkdown("#groceries").text)
    }

    @Test
    fun inline_emphasis_is_styled_and_markers_removed() {
        val rendered = renderNoteMarkdown("**bold** *italic* ~~gone~~")
        assertEquals("bold italic gone", rendered.text)
        val spans = rendered.spanTexts().toMap()
        assertEquals(FontWeight.Bold, spans.getValue("bold").fontWeight)
        assertEquals(FontStyle.Italic, spans.getValue("italic").fontStyle)
        assertEquals(TextDecoration.LineThrough, spans.getValue("gone").textDecoration)
    }

    @Test
    fun bold_nested_inside_italic() {
        val rendered = renderNoteMarkdown("*a **b** c*")
        assertEquals("a b c", rendered.text)
        val spans = rendered.spanTexts()
        assertTrue(spans.any { it.first == "a b c" && it.second.fontStyle == FontStyle.Italic })
        assertTrue(spans.any { it.first == "b" && it.second.fontWeight == FontWeight.Bold })
    }

    @Test
    fun inline_emphasis_works_inside_list_items() {
        val rendered = renderNoteMarkdown("- [ ] **urgent** call")
        assertEquals("☐ urgent call", rendered.text)
        assertEquals(FontWeight.Bold, rendered.spanTexts().toMap().getValue("urgent").fontWeight)
    }

    @Test
    fun unmatched_or_spaced_markers_stay_literal() {
        assertEquals("2 * 3 * 4", renderNoteMarkdown("2 * 3 * 4").text)
        assertEquals("a ** b", renderNoteMarkdown("a ** b").text)
        assertEquals("*open", renderNoteMarkdown("*open").text)
        assertEquals("~~open", renderNoteMarkdown("~~open").text)
        assertTrue(renderNoteMarkdown("2 * 3 * 4").spanStyles.isEmpty())
    }

    @Test
    fun backslash_escapes_markers() {
        val rendered = renderNoteMarkdown("\\*not italic\\* and \\- not a bullet")
        assertEquals("*not italic* and - not a bullet", rendered.text)
        assertTrue(rendered.spanStyles.isEmpty())
        assertEquals("- literal", renderNoteMarkdown("\\- literal").text)
    }

    @Test
    fun spans_never_set_a_color() {
        val rendered = renderNoteMarkdown("# H\n- [x] **b** *i* ~~s~~")
        assertTrue(rendered.spanStyles.isNotEmpty())
        rendered.spanStyles.forEach { assertEquals(Color.Unspecified, it.item.color) }
    }
}
