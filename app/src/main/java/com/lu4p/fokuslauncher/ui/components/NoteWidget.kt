package com.lu4p.fokuslauncher.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.home.HomeWidgetAlignment
import com.lu4p.fokuslauncher.ui.util.combinedClickableWithSystemSound

const val HOME_NOTE_MAX_LENGTH = 2000
private const val HOME_NOTE_MAX_VISIBLE_LINES = 10

/**
 * Read-only home note, rendered with [renderNoteMarkdown]. Tapping a task line calls
 * [onToggleTask] with its source line index; tapping anywhere else, or long-pressing, calls
 * [onClick], which opens [HomeNoteEditDialog]. Shows a muted placeholder when [text] is blank so
 * the widget stays tappable.
 */
@Composable
fun NoteWidget(
        text: String,
        modifier: Modifier = Modifier,
        alignment: HomeWidgetAlignment = HomeWidgetAlignment.START,
        outlined: Boolean = false,
        onClick: () -> Unit = {},
        onToggleTask: (lineIndex: Int) -> Unit = {},
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

    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var elementSize by remember { mutableStateOf(IntSize.Zero) }
    val lastDown = remember { mutableStateOf(Offset.Unspecified) }
    val taskLines = remember(text) { if (isEmpty) emptyList() else noteTaskLines(text) }

    fun taskLineAt(position: Offset): Int? {
        val layout = textLayout ?: return null
        if (taskLines.isEmpty() || !position.isSpecified) return null
        // With a photo backdrop the text is centered in a padded pill; elsewhere this is zero.
        val inset =
                Offset(
                        (elementSize.width - layout.size.width) / 2f,
                        (elementSize.height - layout.size.height) / 2f,
                )
        val offset = layout.getOffsetForPosition(position - inset)
        val line = noteSourceLineAt(layout.layoutInput.text.text, offset)
        return line.takeIf { it in taskLines }
    }

    val renderedLines = displayText.text.split('\n')
    val toggleActions =
            taskLines.map { line ->
                val label =
                        renderedLines.getOrNull(line).orEmpty().trim().removePrefix("☐ ").removePrefix("☑ ")
                CustomAccessibilityAction(stringResource(R.string.home_note_toggle_task, label)) {
                    onToggleTask(line)
                    true
                }
            }
    val editLabel = stringResource(R.string.home_note_edit_action)

    Box(contentAlignment = boxAlignment, modifier = modifier) {
        val textModifier =
                Modifier.onSizeChanged { elementSize = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                lastDown.value =
                                        awaitFirstDown(
                                                        requireUnconsumed = false,
                                                        pass = PointerEventPass.Initial,
                                                )
                                                .position
                            }
                        }
                        .combinedClickableWithSystemSound(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClickLabel = editLabel,
                                onLongClickLabel = editLabel,
                                onLongClick = onClick,
                                onClick = {
                                    val line = taskLineAt(lastDown.value)
                                    if (line != null) onToggleTask(line) else onClick()
                                },
                        )
                        .then(
                                if (toggleActions.isEmpty()) Modifier
                                else Modifier.semantics { customActions = toggleActions }
                        )
                        .testTag("note_widget")
        if (outlined) {
            OutlinedText(
                    text = displayText,
                    style = style,
                    color = color,
                    maxLines = HOME_NOTE_MAX_VISIBLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = textAlign,
                    onTextLayout = { textLayout = it },
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
                    onTextLayout = { textLayout = it },
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
    var value by
            rememberSaveable(initialText, stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
            }
    val focusRequester = remember { FocusRequester() }

    FokusAlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                            stringResource(R.string.home_note_edit_title),
                            modifier = Modifier.weight(1f),
                    )
                    FokusIconButton(
                            onClick = {
                                val (text, cursor) =
                                        insertNoteTaskLine(value.text, value.selection.max)
                                if (text.length <= HOME_NOTE_MAX_LENGTH) {
                                    value = TextFieldValue(text, TextRange(cursor))
                                }
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier.testTag("note_add_task"),
                    ) {
                        LauncherIcon(
                                Icons.Default.Add,
                                stringResource(R.string.home_note_add_task),
                                tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            text = {
                OutlinedTextField(
                        value = value,
                        onValueChange = {
                            value =
                                    if (it.text.length <= HOME_NOTE_MAX_LENGTH) it
                                    else it.copy(text = it.text.take(HOME_NOTE_MAX_LENGTH))
                        },
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
                        onClick = { onSave(value.text) },
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
