package com.zirakusa.notas.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

/** Where notes, folders, tasks, attachments and small settings live. The app uses [SqlNoteStore]. */
interface NoteStore {
    /** Private folder of the photos and drawings ([Attachment.file] is a name inside it). */
    val attachmentDir: File
    fun notes(): List<Note>
    fun folders(): List<Folder>
    /** Inserts when [Note.id] is 0, else updates. Returns the id. */
    fun save(note: Note): Long
    /** Removes the notes, their attachments and the attachment files. */
    fun deleteForever(ids: Collection<Long>)
    fun addFolder(name: String): Long
    fun renameFolder(id: Long, name: String)
    /** Removes the folder; its notes go back to «Sin categoría». */
    fun deleteFolder(id: Long)
    fun tasks(): List<Task>
    /** Inserts when [Task.id] is 0, else updates. Returns the id. */
    fun saveTask(task: Task): Long
    fun deleteTask(id: Long)
    fun attachments(noteId: Long): List<Attachment>
    fun addAttachment(attachment: Attachment): Long
    /** Removes one attachment and its file. */
    fun deleteAttachment(id: Long)
    fun setting(key: String): String?
    fun putSetting(key: String, value: String)
}

/** Local SQLite file (`notas.db`), always with parameters (never SQL built from text). */
class SqlNoteStore(context: Context, name: String? = "notas.db") : SQLiteOpenHelper(context, name, null, VERSION), NoteStore {
    override val attachmentDir = File(context.filesDir, "attachments").apply { mkdirs() }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)")
        db.execSQL(
            "CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, " +
                "folder_id INTEGER, locked INTEGER NOT NULL DEFAULT 0, pinned INTEGER NOT NULL DEFAULT 0, created_at " +
                "INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)"
        )
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        onUpgrade(db, 1, VERSION)
    }

    /** 1 → 2 (Notas 0.2.0): note colour and reminders, task reminders and the attachments table. */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE notes ADD COLUMN remind_at INTEGER")
            db.execSQL("ALTER TABLE tasks ADD COLUMN remind_at INTEGER")
            db.execSQL(
                "CREATE TABLE attachments (id INTEGER PRIMARY KEY AUTOINCREMENT, note_id INTEGER NOT NULL, kind TEXT NOT NULL, " +
                    "file TEXT NOT NULL, created_at INTEGER NOT NULL)"
            )
        }
    }

    override fun notes(): List<Note> = readableDatabase.query("notes", null, null, null, null, null, "updated_at DESC").use { c ->
        buildList { while (c.moveToNext()) add(c.toNote()) }
    }

    override fun folders(): List<Folder> = readableDatabase.query("folders", null, null, null, null, null, "name COLLATE NOCASE").use { c ->
        buildList { while (c.moveToNext()) add(Folder(c.getLong(0), c.getString(1))) }
    }

    override fun save(note: Note): Long {
        val values = ContentValues().apply {
            put("title", note.title); put("body", note.body); put("folder_id", note.folderId)
            put("locked", if (note.locked) 1 else 0); put("pinned", if (note.pinned) 1 else 0); put("color", note.color)
            put("remind_at", note.remindAt); put("created_at", note.createdAt); put("updated_at", note.updatedAt)
            put("deleted_at", note.deletedAt)
        }
        if (note.id == 0L) return writableDatabase.insertOrThrow("notes", null, values)
        writableDatabase.update("notes", values, "id = ?", arrayOf(note.id.toString()))
        return note.id
    }

    override fun deleteForever(ids: Collection<Long>) {
        val files = ids.flatMap { attachments(it) }.map { it.file }
        writableDatabase.run {
            beginTransaction()
            try {
                ids.forEach { val arg = arrayOf(it.toString()); delete("attachments", "note_id = ?", arg); delete("notes", "id = ?", arg) }
                setTransactionSuccessful()
            } finally { endTransaction() }
        }
        files.forEach { File(attachmentDir, it).delete() }
    }

    override fun addFolder(name: String): Long = writableDatabase.insertOrThrow("folders", null, ContentValues().apply { put("name", name) })

    override fun renameFolder(id: Long, name: String) {
        writableDatabase.update("folders", ContentValues().apply { put("name", name) }, "id = ?", arrayOf(id.toString()))
    }

    override fun deleteFolder(id: Long) = writableDatabase.run {
        beginTransaction()
        try {
            update("notes", ContentValues().apply { putNull("folder_id") }, "folder_id = ?", arrayOf(id.toString()))
            delete("folders", "id = ?", arrayOf(id.toString()))
            setTransactionSuccessful()
        } finally { endTransaction() }
    }

    override fun tasks(): List<Task> = readableDatabase.query("tasks", null, null, null, null, null, "created_at DESC").use { c ->
        buildList {
            while (c.moveToNext()) add(Task(c.long("id"), c.getString(c.getColumnIndexOrThrow("text")), c.long("done") == 1L,
                c.long("created_at"), c.long("updated_at"), c.longOrNull("remind_at")))
        }
    }

    override fun saveTask(task: Task): Long {
        val values = ContentValues().apply {
            put("text", task.text); put("done", if (task.done) 1 else 0); put("created_at", task.createdAt)
            put("updated_at", task.updatedAt); put("remind_at", task.remindAt)
        }
        if (task.id == 0L) return writableDatabase.insertOrThrow("tasks", null, values)
        writableDatabase.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
        return task.id
    }

    override fun deleteTask(id: Long) { writableDatabase.delete("tasks", "id = ?", arrayOf(id.toString())) }

    override fun attachments(noteId: Long): List<Attachment> =
        readableDatabase.query("attachments", null, "note_id = ?", arrayOf(noteId.toString()), null, null, "created_at, id").use { c ->
            buildList {
                while (c.moveToNext()) {
                    val kind = runCatching { Attachment.Kind.valueOf(c.getString(c.getColumnIndexOrThrow("kind"))) }.getOrDefault(Attachment.Kind.PHOTO)
                    add(Attachment(c.long("id"), c.long("note_id"), kind, c.getString(c.getColumnIndexOrThrow("file")), c.long("created_at")))
                }
            }
        }

    override fun addAttachment(attachment: Attachment): Long = writableDatabase.insertOrThrow("attachments", null, ContentValues().apply {
        put("note_id", attachment.noteId); put("kind", attachment.kind.name); put("file", attachment.file); put("created_at", attachment.createdAt)
    })

    override fun deleteAttachment(id: Long) {
        val file = readableDatabase.query("attachments", arrayOf("file"), "id = ?", arrayOf(id.toString()), null, null, null)
            .use { if (it.moveToFirst()) it.getString(0) else null } ?: return
        writableDatabase.delete("attachments", "id = ?", arrayOf(id.toString()))
        File(attachmentDir, file).delete()
    }

    override fun setting(key: String): String? =
        readableDatabase.query("settings", arrayOf("value"), "key = ?", arrayOf(key), null, null, null).use { if (it.moveToFirst()) it.getString(0) else null }

    override fun putSetting(key: String, value: String) {
        writableDatabase.insertWithOnConflict("settings", null, ContentValues().apply { put("key", key); put("value", value) }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun Cursor.long(column: String) = getLong(getColumnIndexOrThrow(column))
    private fun Cursor.longOrNull(column: String) = getColumnIndexOrThrow(column).let { if (isNull(it)) null else getLong(it) }

    private fun Cursor.toNote() = Note(
        id = long("id"), title = getString(getColumnIndexOrThrow("title")), body = getString(getColumnIndexOrThrow("body")),
        folderId = longOrNull("folder_id"), locked = long("locked") == 1L, pinned = long("pinned") == 1L,
        color = long("color").toInt(), remindAt = longOrNull("remind_at"), createdAt = long("created_at"),
        updatedAt = long("updated_at"), deletedAt = longOrNull("deleted_at"),
    )

    private companion object { const val VERSION = 2 }
}
