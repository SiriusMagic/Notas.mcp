package com.zirakusa.notas

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.zirakusa.notas.data.Inbox
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.mcp.McpMode

/**
 * «add_note»: a family app (Lucía's counts) leaves a new note. Only apps signed with the same key reach it (the ACT
 * permission and a signature check); it can only add, never read, change or delete. Notas' screens reload if open.
 */
class InboxProvider : ContentProvider() {
    override fun onCreate() = true
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != "add_note" || extras == null) return null
        val context = context ?: return null
        val caller = callingPackage ?: return null
        if (context.packageManager.checkSignatures(context.packageName, caller) != PackageManager.SIGNATURE_MATCH) return null
        val store = SqlNoteStore(context)
        val id = try { Inbox.add(store, extras.getString("title"), extras.getString("body"), extras.getString("folder"), System.currentTimeMillis()) }
            finally { store.close() }
            ?: return null
        Handler(Looper.getMainLooper()).post { McpMode.onChanged() }
        return Bundle().apply { putLong("id", id) }
    }
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
