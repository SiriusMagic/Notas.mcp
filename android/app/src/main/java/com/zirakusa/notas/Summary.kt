package com.zirakusa.notas

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Section
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.data.Task

/**
 * The lines of Notas' card in Inicio: the pending tasks («☐ Comprar pan»), then the latest notes by title. Locked notes
 * and the trash never appear (as with MCP). Pure, with tests.
 */
object SummaryLines {
    const val MAX = 5
    fun lines(tasks: List<Task>, notes: List<Note>): List<String> {
        val pending = tasks.filter { !it.done }.sortedByDescending { it.createdAt }.map { "☐ " + it.text.lineSequence().first().take(80) }
        val latest = NotesRules.visible(notes, Section.All).filter { !NotesRules.isEmpty(it.title, it.body) }
            .sortedByDescending { it.updatedAt }.map { NotesRules.displayTitle(it).take(80) }
        return (pending + latest).take(MAX)
    }
}

/**
 * The family summary (see Inicio's FAMILIA.md): what Inicio shows in its «Hoy» card for Notas. Only apps signed by the
 * same developer may ask (Android's signature permission and the caller's signature checked here).
 */
class SummaryProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "summary") return null
        val context = context ?: return null
        val caller = callingPackage ?: return null
        if (context.packageManager.checkSignatures(context.packageName, caller) != PackageManager.SIGNATURE_MATCH) return null
        val store = SqlNoteStore(context)
        val lines = try { SummaryLines.lines(store.tasks(), store.notes()) } finally { store.close() }
        return Bundle().apply {
            putString("title", context.getString(R.string.summary_title))
            putStringArrayList("lines", ArrayList(lines))
            putString("empty", context.getString(R.string.summary_empty))
        }
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
