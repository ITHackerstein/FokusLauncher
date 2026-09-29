package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.home.HomeWidgetAlignment
import com.lu4p.fokuslauncher.ui.util.clickableNoRippleWithSystemSound

const val HOME_NOTE_MAX_LENGTH = 2000
private const val HOME_NOTE_MAX_VISIBLE_LINES = 10

/**
 * Read-only home note, rendered with [renderNoteMarkdown]. Tapping it calls [onClick], which
 * opens [HomeNoteEditDialog]. Shows a muted placeholder when [text] is blank so the widget stays
 * tappable.
 */
@Composable
fun NoteWidget(
        text: String,
        modifier: Modifier = Modifier,
        alignment: HomeWidgetAlignment = HomeWidgetAlignment.START,
        outlined: Boolean = false,
        onClick: () -> Unit = {},
) {
    val isEmpty = text.isBlank()
    val baseColor = MaterialTheme.colorScheme.onBackground
    val color = if (isEmpty) baseColor.copy(alpha = 0.38f) else baseColor
    val style = MaterialTheme.typography.bodyLarge
    val placeholder = stringResource(R.string.home_note_placeholder)
    val displayText =
            remember(text, placeholder) {
                if (isEmpty) AnnotatedString(placeholder) else renderNoteMarkdown(text.trimEnd())
            }
    val (boxAlignment, textAlign) =
            when (alignment) {
                HomeWidgetAlignment.START -> Alignment.CenterStart to TextAlign.Start
                HomeWidgetAlignment.CENTER -> Alignment.Center to TextAlign.Center
                HomeWidgetAlignment.END -> Alignment.CenterEnd to TextAlign.End
            }

    Box(contentAlignment = boxAlignment, modifier = modifier) {
        val textModifier =
                Modifier.clickableNoRippleWithSystemSound(onClick = onClick).testTag("note_widget")
        if (outlined) {
            OutlinedText(
                    text = displayText,
                    style = style,
                    color = color,
                    maxLines = HOME_NOTE_MAX_VISIBLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    modifier = textModifier,
            )
        } else {
            Text(
                    text = displayText,
                    style = style,
                    color = color,
                    maxLines = HOME_NOTE_MAX_VISIBLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    modifier = textModifier,
            )
        }
    }
}

/** Multi-line editor for the home note. Changes are only persisted via [onSave]. */
@Composable
fun HomeNoteEditDialog(
        initialText: String,
        onDismiss: () -> Unit,
        onSave: (String) -> Unit,
) {
    var text by rememberSaveable(initialText) { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }

    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.home_note_edit_title)) },
            text = {
                OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(HOME_NOTE_MAX_LENGTH) },
                        placeholder = { Text(stringResource(R.string.home_note_edit_hint)) },
                        minLines = 4,
                        maxLines = 12,
                        modifier =
                                Modifier.fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .testTag("note_edit_field"),
                )
            },
            confirmButton = {
                FokusTextButton(
                        onClick = { onSave(text) },
                        modifier = Modifier.testTag("note_edit_save"),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                FokusTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
