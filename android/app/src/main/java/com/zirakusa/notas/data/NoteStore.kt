package com.zirakusa.notas.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Where notes, folders and small settings live. The app uses [SqlNoteStore]; tests can use any other. */
interface NoteStore {
    fun notes(): List<Note>
    fun folders(): List<Folder>
    /** Inserts when [Note.id] is 0, else updates. Returns the id. */
    fun save(note: Note): Long
    fun deleteForever(ids: Collection<Long>)
    fun addFolder(name: String): Long
    fun renameFolder(id: Long, name: String)
    /** Removes the folder; its notes go back to «Sin categoría». */
    fun deleteFolder(id: Long)
    fun tasks(): List<Task>
    /** Inserts when [Task.id] is 0, else updates. Returns the id. */
    fun saveTask(task: Task): Long
    fun deleteTask(id: Long)
    fun setting(key: String): String?
    fun putSetting(key: String, value: String)
}

/** Local SQLite file (`notas.db`), always with parameters (never SQL built from text). */
class SqlNoteStore(context: Context) : SQLiteOpenHelper(context, "notas.db", null, 1), NoteStore {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)")
        db.execSQL(
            "CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, " +
                "folder_id INTEGER, locked INTEGER NOT NULL DEFAULT 0, pinned INTEGER NOT NULL DEFAULT 0, created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, deleted_at INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)"
        )
        db.execSQL("CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun notes(): List<Note> = readableDatabase.query("notes", null, null, null, null, null, "updated_at DESC").use { c ->
        buildList { while (c.moveToNext()) add(c.toNote()) }
    }

    override fun folders(): List<Folder> = readableDatabase.query("folders", null, null, null, null, null, "name COLLATE NOCASE").use { c ->
        buildList { while (c.moveToNext()) add(Folder(c.getLong(0), c.getString(1))) }
    }

    override fun save(note: Note): Long {
        val values = ContentValues().apply {
            put("title", note.title); put("body", note.body); put("folder_id", note.folderId)
            put("locked", if (note.locked) 1 else 0); put("pinned", if (note.pinned) 1 else 0); put("created_at", note.createdAt)
            put("updated_at", note.updatedAt); put("deleted_at", note.deletedAt)
        }
        if (note.id == 0L) return writableDatabase.insertOrThrow("notes", null, values)
        writableDatabase.update("notes", values, "id = ?", arrayOf(note.id.toString()))
        return note.id
    }

    override fun deleteForever(ids: Collection<Long>) = writableDatabase.run {
        beginTransaction()
        try { ids.forEach { delete("notes", "id = ?", arrayOf(it.toString())) }; setTransactionSuccessful() } finally { endTransaction() }
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
            while (c.moveToNext()) add(Task(c.getLong(c.getColumnIndexOrThrow("id")), c.getString(c.getColumnIndexOrThrow("text")),
                c.getInt(c.getColumnIndexOrThrow("done")) == 1, c.getLong(c.getColumnIndexOrThrow("created_at")), c.getLong(c.getColumnIndexOrThrow("updated_at"))))
        }
    }

    override fun saveTask(task: Task): Long {
        val values = ContentValues().apply {
            put("text", task.text); put("done", if (task.done) 1 else 0); put("created_at", task.createdAt); put("updated_at", task.updatedAt)
        }
        if (task.id == 0L) return writableDatabase.insertOrThrow("tasks", null, values)
        writableDatabase.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
        return task.id
    }

    override fun deleteTask(id: Long) { writableDatabase.delete("tasks", "id = ?", arrayOf(id.toString())) }

    override fun setting(key: String): String? =
        readableDatabase.query("settings", arrayOf("value"), "key = ?", arrayOf(key), null, null, null).use { if (it.moveToFirst()) it.getString(0) else null }

    override fun putSetting(key: String, value: String) {
        writableDatabase.insertWithOnConflict("settings", null, ContentValues().apply { put("key", key); put("value", value) }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun Cursor.toNote() = Note(
        id = getLong(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        body = getString(getColumnIndexOrThrow("body")),
        folderId = getColumnIndexOrThrow("folder_id").let { if (isNull(it)) null else getLong(it) },
        locked = getInt(getColumnIndexOrThrow("locked")) == 1,
        pinned = getInt(getColumnIndexOrThrow("pinned")) == 1,
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        deletedAt = getColumnIndexOrThrow("deleted_at").let { if (isNull(it)) null else getLong(it) },
    )
}
