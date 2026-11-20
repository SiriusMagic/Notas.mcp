package com.zirakusa.notas.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.zirakusa.notas.data.Attachment
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.TextEdits

/**
 * The note open in the editor. [history] holds title and text for ↶ ↷; [body] is the text with its cursor.
 * [writing] = the keyboard is up (full toolbar, ✓); otherwise the note is being read (share, background, short toolbar).
 */
class Editing(note: Note, val history: UndoHistory, attachments: List<Attachment> = emptyList()) {
    var note by mutableStateOf(note)
    var attachments by mutableStateOf(attachments)
    var writing by mutableStateOf(false)
    var draft by mutableStateOf(history.current)
        private set
    var body by mutableStateOf(TextFieldValue(history.current.body))
        private set
    val readOnly get() = note.deletedAt != null
    private val cursor get() = TextEdits.Edit(body.text, body.selection.start, body.selection.end)

    /** Replaces title and text at once (one undo step). */
    fun change(next: Draft) {
        if (readOnly) return
        body = TextFieldValue(next.body, TextRange(next.body.length)); record(next)
    }

    fun changeTitle(title: String) = record(draft.copy(title = title.replace("\n", "")))

    /** Typing in the text: Enter continues lists, a tap on a box ticks it. */
    fun changeBody(next: TextFieldValue) {
        if (readOnly) return
        val new = TextEdits.Edit(next.text, next.selection.start, next.selection.end)
        TextEdits.tapOnBox(cursor, new)?.let { return apply(it) }
        val continued = TextEdits.continueList(cursor, new)
        if (continued == new) { body = next; record(draft.copy(body = next.text)) } else apply(continued)
    }

    /** Runs one of the toolbar's text tools on the text and cursor. */
    fun tool(edit: (TextEdits.Edit) -> TextEdits.Edit) { if (!readOnly) apply(edit(cursor)) }

    val heading get() = TextEdits.headingAt(cursor)

    fun undo() { draft = history.undo(); body = TextFieldValue(draft.body, TextRange(draft.body.length)) }
    fun redo() { draft = history.redo(); body = TextFieldValue(draft.body, TextRange(draft.body.length)) }

    private fun apply(e: TextEdits.Edit) {
        body = TextFieldValue(e.text, TextRange(e.start.coerceIn(0, e.text.length), e.end.coerceIn(0, e.text.length)))
        record(draft.copy(body = e.text))
    }

    private fun record(next: Draft) { history.record(next); draft = history.current }
}
