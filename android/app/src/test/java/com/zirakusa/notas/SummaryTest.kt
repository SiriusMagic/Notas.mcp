package com.zirakusa.notas

import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Notas' card in Inicio: pending tasks first, then the latest notes; locked notes and the trash never appear. */
class SummaryTest {
    @Test fun pendingTasksThenLatestNotes() {
        val tasks = listOf(Task(1, "Comprar pan", createdAt = 1), Task(2, "Pagar luz\nantes del 30", createdAt = 2), Task(3, "Hecha", done = true, createdAt = 3))
        val notes = listOf(Note(1, "Ideas", "…", updatedAt = 5), Note(2, "Secreto", "x", locked = true, updatedAt = 9),
            Note(3, "Borrada", "x", deletedAt = 7, updatedAt = 8), Note(4, "", "Lista de la compra\nleche", updatedAt = 6), Note(5, "", "", updatedAt = 10))
        val lines = SummaryLines.lines(tasks, notes)
        assertEquals("☐ Pagar luz", lines[0]); assertEquals("☐ Comprar pan", lines[1])
        assertEquals(4, lines.size); assertTrue(lines.none { "Secreto" in it || "Borrada" in it || "Hecha" in it })
        assertTrue(SummaryLines.lines(emptyList(), emptyList()).isEmpty())
        assertEquals(SummaryLines.MAX, SummaryLines.lines(List(9) { Task(it.toLong() + 1, "t$it") }, emptyList()).size)
    }
}
