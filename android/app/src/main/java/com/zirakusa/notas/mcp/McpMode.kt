package com.zirakusa.notas.mcp

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.zirakusa.notas.BuildConfig
import com.zirakusa.notas.data.Folder
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NoteStore
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.data.Task

/**
 * The public relay may run for MCP or for redirection. Only redirection serves the current Arka token.
 * The phone grants local access separately through the signature-protected provider.
 */
object McpMode {
    var remote by mutableStateOf(false); private set
    var enabled by mutableStateOf(false); private set
    var localMcp by mutableStateOf(false); private set
    val active get() = enabled || remote
    var state by mutableStateOf(RelayLink.State.OFFLINE); private set
    /** Called on the main thread after an AI changed notes or tasks (the screens reload, the widgets redraw). */
    var onChanged: () -> Unit = {}

    private var prefs: android.content.SharedPreferences? = null
    private var link: RelayLink? = null
    private var device = ""
    private var relaySecret = ""
    private var token = Token("")
    internal var relayUrl: String = BuildConfig.RELAY_URL

    fun load(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences("mcp", Context.MODE_PRIVATE)
        prefs = p
        remote = p.getBoolean("redirect", true)
        enabled = p.getBoolean("enabled", false)
        localMcp = p.getBoolean("localMcp", false)
        token = Token(SecretBox.open(p.getString("arka", "") ?: "").ifBlank {
            (Legacy.redirectSecret(SecretBox.open(p.getString("tokens", "") ?: "")) ?: Token.secret())
                .also { p.edit { putString("arka", SecretBox.seal(it)); remove("tokens") } }
        })
        device = p.getString("device", null) ?: Token.id().repeat(2).also { p.edit { putString("device", it) } }
        relaySecret = SecretBox.open(p.getString("relay", "") ?: "").ifBlank {
            Token.secret().also { p.edit { putString("relay", SecretBox.seal(it)) } }
        }
        data = StoreData(SqlNoteStore(app)) { Handler(Looper.getMainLooper()).post { onChanged() } }
        apply()
    }

    fun enable(on: Boolean) { enabled = on; prefs?.edit { putBoolean("enabled", on) }; apply() }
    fun allowLocal(on: Boolean) { localMcp = on; prefs?.edit { putBoolean("localMcp", on) } }

    fun turnRemote(on: Boolean) { remote = on; prefs?.edit { putBoolean("redirect", on) }; apply() }

    fun hubLink() = "${relayUrl.trimEnd('/')}/d/$device/mcp?token=${token.secret}"

    private var data: NotesData? = null

    /** One JSON-RPC request from Lucía on this phone, through the same handler as the relay. Null before [load]. */
    fun local(body: String): String? {
        val d = data ?: return null
        return kotlinx.coroutines.runBlocking {
            McpHandler({ listOf(token) }, d, BuildConfig.VERSION_NAME).handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(), body).body
        }
    }

    private fun apply() {
        val d = data ?: return
        if (active && relayUrl.isNotBlank()) {
            if (link == null) link = RelayLink(relayUrl, device, relaySecret,
                McpHandler({ if (remote) listOf(token) else emptyList() }, d, BuildConfig.VERSION_NAME)) { state = it }.also { it.start() }
        } else { link?.stop(); link = null; state = RelayLink.State.OFFLINE }
    }
}

/** The real notes for the tools: the phone's database; every change is announced through [changed]. */
class StoreData(private val store: NoteStore, private val changed: () -> Unit = {}) : NotesData {
    override fun notes(): List<Note> = store.notes()
    override fun folders(): List<Folder> = store.folders()
    override fun tasks(): List<Task> = store.tasks()
    override fun saveNote(note: Note) = store.save(note).also { changed() }
    override fun addFolder(name: String) = store.addFolder(name).also { changed() }
    override fun saveTask(task: Task) = store.saveTask(task).also { changed() }
    override fun deleteTask(id: Long) { store.deleteTask(id); changed() }
}
