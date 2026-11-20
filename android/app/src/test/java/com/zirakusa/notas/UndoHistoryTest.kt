package com.zirakusa.notas

import com.zirakusa.notas.ui.Draft
import com.zirakusa.notas.ui.UndoHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoHistoryTest {
    private fun type(h: UndoHistory, text: String) = text.forEach { h.record(h.current.copy(body = h.current.body + it)) }

    @Test fun undoesWordByWordAndRedoes() {
        val h = UndoHistory(Draft("", ""))
        assertFalse(h.canUndo)
        type(h, "hola mundo")
        assertEquals("hola ", h.undo().body)
        assertEquals("hola", h.undo().body)
        assertEquals("", h.undo().body)
        assertEquals("", h.undo().body)            // nothing left: stays
        assertEquals("hola", h.redo().body)
        assertTrue(h.canRedo)
    }

    @Test fun newEditClearsRedoAndTitleCountsToo() {
        val h = UndoHistory(Draft("", "x"))
        h.record(Draft("T", "x"))
        h.undo()
        h.record(Draft("", "y"))
        assertFalse(h.canRedo)
        assertEquals(Draft("", "x"), h.undo())
    }

    @Test fun keepsAtMostTheLimit() {
        val h = UndoHistory(Draft("", ""), limit = 3)
        listOf("a ", "a b", "a b ", "a b c", "a b c ").forEach { h.record(Draft("", it)) }
        repeat(10) { h.undo() }
        assertFalse(h.canUndo)
        assertEquals("a b", h.current.body)
    }
}
