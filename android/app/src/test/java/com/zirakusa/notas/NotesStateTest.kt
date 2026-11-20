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
    private fun state() = NotesState(store) { now }

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
        val t = TasksState(store) { now }
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
}
