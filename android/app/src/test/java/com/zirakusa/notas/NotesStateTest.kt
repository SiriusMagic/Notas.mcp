package com.zirakusa.notas

import androidx.test.core.app.ApplicationProvider
import com.zirakusa.notas.data.Layout
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.Section
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.ui.Draft
import com.zirakusa.notas.ui.NotesState
import com.zirakusa.notas.ui.TasksState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** NotesState over the real SQLite store (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class NotesStateTest {
    private var now = 1_000_000L
    private val store = SqlNoteStore(ApplicationProvider.getApplicationContext())
    private fun state() = NotesState(store, { now })

    @Test fun writeSaveAndReopen() {
        val s = state()
        s.newNote()
        s.editing!!.change(Draft("Compras", "Cajetines\nSpray negro"))
        s.closeEditor()
        assertNull(s.editing)
        val saved = s.visible.single()
        assertEquals("Compras", saved.title)
        assertEquals(now, saved.updatedAt)
        assertEquals(listOf("Compras"), state().notes.map { it.title })   // survives a restart
    }

    @Test fun emptyNoteIsNotKeptAndClearingDeletes() {
        val s = state()
        s.newNote(); s.closeEditor()
        assertTrue(s.notes.isEmpty())
        s.newNote(); s.editing!!.change(Draft("", "x")); s.closeEditor()
        s.open(s.notes.single()); s.editing!!.change(Draft("", "")); s.closeEditor()
        assertTrue(s.notes.isEmpty())
    }

    @Test fun trashRestoreAndPurgeAfterThirtyDays() {
        val s = state()
        s.newNote(); s.editing!!.change(Draft("a", "")); s.closeEditor()
        s.open(s.notes.single()); s.trashOpen()
        assertEquals(0, s.count(Section.All)); assertEquals(1, s.count(Section.Trash))
        s.restore(s.notes.single())
        assertEquals(1, s.count(Section.All))
        s.open(s.notes.single()); s.trashOpen()
        now += 30L * 24 * 60 * 60 * 1000
        assertTrue(state().notes.isEmpty())
    }

    @Test fun foldersMoveAndDelete() {
        val s = state()
        s.addFolder("  Obra  ")
        val folder = s.folders.single()
        assertEquals("Obra", folder.name)
        assertEquals(Section.InFolder(folder.id), s.section)
        s.newNote(); s.editing!!.change(Draft("Taladro", "")); s.closeEditor()   // new note goes into the open folder
        assertEquals(folder.id, s.notes.single().folderId)
        s.deleteFolder(folder.id)
        assertEquals(Section.Uncategorized, s.section)
        assertNull(s.notes.single().folderId)
        s.deleteFolder(12345)                                                    // not found: nothing happens
        s.addFolder("   ")                                                       // bad name: ignored
        assertTrue(s.folders.isEmpty())
    }

    @Test fun layoutIsRemembered() {
        state().changeLayout(Layout.LIST)
        assertEquals(Layout.LIST, state().layout)
    }

    @Test fun saveOpenKeepsEditorAndAssignsId() {
        val s = state()
        s.newNote(); s.editing!!.change(Draft("", "borrador"))
        s.saveOpen()
        assertTrue(s.editing!!.note.id != 0L)
        s.editing!!.change(Draft("", "borrador final")); s.closeEditor()
        assertEquals(listOf(Note::body.get(s.notes.single())), listOf("borrador final"))
    }

    private fun add(s: NotesState, title: String) { s.newNote(); s.editing!!.change(Draft(title, "")); s.closeEditor() }

    @Test fun selectionPinsLocksMovesAndTrashes() {
        val s = state()
        add(s, "a"); now += 1; add(s, "b")
        val a = s.notes.first { it.title == "a" }
        s.toggleSelect(a); assertTrue(s.selecting)
        s.pinSelected()
        assertEquals("a", s.visible.first().title)                     // pinned goes first although older
        assertTrue(!s.selecting)
        s.toggleSelect(s.visible.first()); s.lockSelected()
        assertEquals(1, s.count(Section.Locked))
        assertEquals(false, s.pick(Section.Locked))                    // still locked: asks the phone first
        s.unlock(); assertEquals(Section.Locked, s.section)
        s.lockAgain(); assertEquals(Section.Uncategorized, s.section)
        s.selectAll(); s.trashSelected()
        assertEquals(1, s.count(Section.Trash))
        s.pick(Section.Trash); s.selectAll(); s.deleteSelectedForever()
        assertEquals(0, s.count(Section.Trash))
    }

    @Test fun tasksAddTickEditAndDelete() {
        val t = TasksState(store, { now })
        t.startNew(); t.done("  Comprar spray  ")
        t.startNew(); t.done("   ")                                     // empty: not added
        val task = t.tasks.single()
        assertEquals("Comprar spray", task.text)
        t.toggle(task); assertTrue(t.tasks.single().done)
        t.edit(t.tasks.single()); t.done("Comprar spray negro")
        assertEquals("Comprar spray negro", TasksState(store).tasks.single().text)
        assertTrue(t.visible("negro").isNotEmpty()); assertTrue(t.visible("taladro").isEmpty())
        t.delete(t.tasks.single()); assertTrue(t.tasks.isEmpty())
        t.delete(task)                                                  // not found: nothing happens
    }

    private class FakeAlarms : com.zirakusa.notas.ui.ReminderScheduler {
        val set = mutableMapOf<String, Long>()
        override fun set(kind: String, id: Long, at: Long, text: String) { set["$kind$id"] = at }
        override fun cancel(kind: String, id: Long) { set.remove("$kind$id") }
    }

    @Test fun photoKeepsAnEmptyNoteAndMenuActionsSave() {
        val alarms = FakeAlarms()
        val s = NotesState(store, { now }, alarms)
        s.newNote()
        val file = java.io.File(s.attachmentDir, "p.jpg").apply { writeText("x") }
        s.attach(com.zirakusa.notas.data.Attachment.Kind.PHOTO, file.name)
        val id = s.editing!!.note.id
        assertTrue(id != 0L)
        s.changeOpen { it.copy(pinned = true, color = 3) }
        s.remindOpen(now + 60_000)
        assertEquals(now + 60_000, alarms.set["note$id"])
        s.closeEditor()
        val saved = s.notes.single()
        assertTrue(saved.pinned); assertEquals(3, saved.color)       // kept although it has no text: it has a photo
        s.open(saved); s.detach(s.editing!!.attachments.single())
        assertTrue(!file.exists())
        s.trashOpen()
        assertNull(alarms.set["note$id"])                             // a deleted note does not ring
        assertEquals(false, s.openById(id))                          // in the trash: a shortcut does not open it
        assertEquals(false, s.openById(4242))                        // not found
    }

    @Test fun editorToolsAndListsWorkThroughEditing() {
        val s = state()
        s.newNote()
        val e = s.editing!!
        e.changeBody(androidx.compose.ui.text.input.TextFieldValue("Cable", androidx.compose.ui.text.TextRange(5)))
        e.tool(com.zirakusa.notas.data.TextEdits::cycleCheck)
        assertEquals("☐ Cable", e.body.text)
        e.changeBody(androidx.compose.ui.text.input.TextFieldValue("☐ Cable\n", androidx.compose.ui.text.TextRange(8)))
        assertEquals("☐ Cable\n☐ ", e.body.text)
        e.undo()
        assertEquals("☐ Cable", e.body.text)
    }

    @Test fun taskReminders() {
        val alarms = FakeAlarms()
        val t = TasksState(store, { now }, alarms)
        t.startNew(); t.done("Llamar", now + 5_000)
        val task = t.tasks.single()
        assertEquals(now + 5_000, alarms.set["task${task.id}"])
        t.toggle(task); assertNull(alarms.set["task${task.id}"])     // done: no reminder
        t.edit(t.tasks.single()); t.done("Llamar", now - 1)          // past time: dropped
        assertNull(t.tasks.single().remindAt)
    }

    @Test fun themeIsGlobalAndKept() {
        val s = state()
        assertEquals(com.zirakusa.notas.ui.theme.ThemeMode.DARK, s.themeMode)
        s.changeTheme(com.zirakusa.notas.ui.theme.ThemeMode.LIGHT); s.changeAccent(com.zirakusa.notas.ui.theme.Accent.AZUL)
        add(s, "a")
        s.open(s.notes.single()); s.changeOpen { it.copy(color = 4) }; s.closeEditor()
        val again = state()
        assertEquals(com.zirakusa.notas.ui.theme.ThemeMode.LIGHT, again.themeMode)
        assertEquals(com.zirakusa.notas.ui.theme.Accent.AZUL, again.accent)
        assertEquals(4, again.notes.single().color)                 // the note keeps its own background
    }
}
