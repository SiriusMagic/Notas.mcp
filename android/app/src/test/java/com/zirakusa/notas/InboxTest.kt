package com.zirakusa.notas

import androidx.test.core.app.ApplicationProvider
import com.zirakusa.notas.data.Inbox
import com.zirakusa.notas.data.SqlNoteStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Notes handed over by family apps (Lucía's counts): always a new note, in its folder, made once. */
@RunWith(RobolectricTestRunner::class)
class InboxTest {
    private val store = SqlNoteStore(ApplicationProvider.getApplicationContext(), null)
    @After fun close() = store.close()

    @Test fun addsNotesInOneFolder() {
        val a = Inbox.add(store, "Conteo casa Pérez", "| Tomacorriente | 4 |", "Conteos", 10)!!
        val b = Inbox.add(store, "Conteo\nlocal", "| Foco | 2 |", "conteos", 20)!!
        assertEquals(1, store.folders().size)
        val notes = store.notes().associateBy { it.id }
        assertEquals("Conteo casa Pérez", notes.getValue(a).title)
        assertEquals("Conteo local", notes.getValue(b).title)
        assertEquals(store.folders().single().id, notes.getValue(b).folderId)
        assertEquals(20L, notes.getValue(b).createdAt)
    }

    @Test fun emptyOrNoFolder() {
        assertNull(Inbox.add(store, " ", "", "Conteos", 1))
        assertEquals(0, store.folders().size)
        val id = Inbox.add(store, null, "x".repeat(Inbox.BODY_MAX + 5), null, 1)!!
        val n = store.notes().single { it.id == id }
        assertNull(n.folderId); assertEquals(Inbox.BODY_MAX, n.body.length)
    }
}
