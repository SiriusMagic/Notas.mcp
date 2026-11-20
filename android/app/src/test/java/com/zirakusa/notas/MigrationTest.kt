package com.zirakusa.notas

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.zirakusa.notas.data.Attachment
import com.zirakusa.notas.data.SqlNoteStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Notes written with Notas 0.1.0 (database version 1) stay after updating, and get the new columns. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun version1KeepsNotesAndGainsAttachments() {
        val path = context.getDatabasePath("v1.db").apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)")
            db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, folder_id INTEGER, locked INTEGER NOT NULL DEFAULT 0, pinned INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER)")
            db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            db.execSQL("INSERT INTO notes (title, body, created_at, updated_at) VALUES ('Hh', 'Cajetines', 1, 2)")
            db.execSQL("INSERT INTO tasks (text, created_at, updated_at) VALUES ('Cable 8', 1, 1)")
            db.version = 1
        }
        val store = SqlNoteStore(context, "v1.db")
        val note = store.notes().single()
        assertEquals("Cajetines", note.body); assertEquals(0, note.color); assertNull(note.remindAt)
        assertNull(store.tasks().single().remindAt)
        val file = File(store.attachmentDir, "a.jpg").apply { writeText("x") }
        store.addAttachment(Attachment(noteId = note.id, kind = Attachment.Kind.PHOTO, file = file.name))
        assertEquals(1, store.attachments(note.id).size)
        store.deleteForever(listOf(note.id))
        assertTrue(store.attachments(note.id).isEmpty())
        assertTrue(!file.exists())                                   // the photo goes with the note
        store.deleteAttachment(999)                                  // not found: nothing happens
    }
}
