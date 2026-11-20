package com.zirakusa.notas

import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class NotesRulesTest {
    private val day = 24L * 60 * 60 * 1000
    private val notes = listOf(
        Note(1, "Guayaquil", "Yuanshi", updatedAt = 30),
        Note(2, "", "Cierra\nTaladro", folderId = 7, updatedAt = 20),
        Note(3, "Clave", "secreta", locked = true, updatedAt = 10),
        Note(4, "Vieja", "", updatedAt = 5, deletedAt = 100),
        Note(5, "Café", "", updatedAt = 40),
    )

    @Test fun sectionsAndCounts() {
        assertEquals(listOf(5L, 1, 2), NotesRules.visible(notes, Section.All).map { it.id })
        assertEquals(listOf(5L, 1), NotesRules.visible(notes, Section.Uncategorized).map { it.id })
        assertEquals(listOf(2L), NotesRules.visible(notes, Section.InFolder(7)).map { it.id })
        assertEquals(listOf(3L), NotesRules.visible(notes, Section.Locked).map { it.id })
        assertEquals(listOf(4L), NotesRules.visible(notes, Section.Trash).map { it.id })
        assertEquals(0, NotesRules.count(notes, Section.InFolder(99)))   // folder not found: empty, no error
    }

    @Test fun searchIgnoresCaseAndAccentsAndNeedsEveryWord() {
        assertEquals(listOf(5L), NotesRules.visible(notes, Section.All, "cafe").map { it.id })
        assertEquals(listOf(2L), NotesRules.visible(notes, Section.All, "TALADRO cierra").map { it.id })
        assertTrue(NotesRules.visible(notes, Section.All, "taladro guayaquil").isEmpty())
        assertTrue(NotesRules.visible(notes, Section.All, "secreta").isEmpty())   // locked notes are not searched from «Todas»
    }

    @Test fun titleFallsBackToFirstLine() {
        assertEquals("Cierra", NotesRules.displayTitle(notes[1]))
        assertEquals("Guayaquil", NotesRules.displayTitle(notes[0]))
        assertEquals("a\nb", NotesRules.preview(Note(body = "\n a \n\n b")))
        assertTrue(NotesRules.isEmpty(" ", "\n"))
        assertFalse(NotesRules.isEmpty("", "x"))
    }

    @Test fun trashKeepsThirtyDays() {
        val n = Note(deletedAt = 0)
        assertFalse(NotesRules.expired(n, 29 * day))
        assertTrue(NotesRules.expired(n, 30 * day))
        assertEquals(28, NotesRules.daysLeft(n, 2 * day))
        assertFalse(NotesRules.expired(Note(), Long.MAX_VALUE))
    }

    @Test fun shortDates() {
        val now = ZonedDateTime.of(2026, 11, 20, 10, 44, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("08:05", NotesRules.shortDate(now - (2 * 60 + 39) * 60_000L, now, ZoneOffset.UTC))
        assertEquals("11/18", NotesRules.shortDate(now - 2 * day, now, ZoneOffset.UTC))
        assertEquals("2025/11/20", NotesRules.shortDate(now - 365 * day, now, ZoneOffset.UTC))
    }
}
