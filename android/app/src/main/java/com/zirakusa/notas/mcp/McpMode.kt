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
 * Ajustes › Modo MCP, as in Stone, Luma and Calculadora: «Activar MCP» with the user's tokens (LLM or Hub), or
 * «Redireccionar a Hub», where only the hub's own token is served and the hub manages who may do what. Off by default.
 * Runs while the app is alive.
 */
object McpMode {
    var enabled by mutableStateOf(false); private set
    var hubRedirect by mutableStateOf(false); private set
    var tokens by mutableStateOf(emptyList<Token>()); private set
    var state by mutableStateOf(RelayLink.State.OFFLINE); private set
    /** Called on the main thread after an AI changed notes or tasks (the screens reload, the widgets redraw). */
    var onChanged: () -> Unit = {}

    private var prefs: android.content.SharedPreferences? = null
    private var link: RelayLink? = null
    private var device = ""
    private var relaySecret = ""
    internal var relayUrl: String = BuildConfig.RELAY_URL
    val active get() = enabled || hubRedirect
    /** The tokens the user manages (the redirect token is never listed). */
    val listed get() = tokens.filter { it.kind != Kind.REDIRECT }

    fun load(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences("mcp", Context.MODE_PRIVATE)
        prefs = p
        enabled = p.getBoolean("enabled", false); hubRedirect = p.getBoolean("redirect", false)
        tokens = Token.parse(SecretBox.open(p.getString("tokens", "") ?: "").ifBlank { "[]" })
        device = p.getString("device", null) ?: Token.id().repeat(2).also { p.edit { putString("device", it) } }
        relaySecret = SecretBox.open(p.getString("relay", "") ?: "").ifBlank {
            Token.secret().also { p.edit { putString("relay", SecretBox.seal(it)) } }
        }
        data = StoreData(SqlNoteStore(app)) { Handler(Looper.getMainLooper()).post { onChanged() } }
        apply()
    }

    fun enable(on: Boolean) { enabled = on; prefs?.edit { putBoolean("enabled", on) }; apply() }
    fun redirect(on: Boolean) { hubRedirect = on; prefs?.edit { putBoolean("redirect", on) }; if (on) redirectToken(); apply() }

    fun create(name: String, access: Access, kind: Kind, usage: String, days: Int): Token {
        val now = System.currentTimeMillis()
        return Token(Token.id(), name.trim().take(40).ifEmpty { "Agente ${listed.size + 1}" }, Token.secret(), access, kind,
            usage.trim().take(Token.USAGE_MAX), now, if (days > 0) now + days * 86_400_000L else 0).also { save(tokens + it) }
    }
    fun setPaused(id: String, paused: Boolean) = save(tokens.map { if (it.id == id) it.copy(paused = paused) else it })
    fun revoke(id: String) = save(tokens.filterNot { it.id == id })

    /** The hub's token while redirecting: full write access, never expires, never listed. */
    fun redirectToken(): Token = tokens.firstOrNull { it.kind == Kind.REDIRECT }
        ?: Token(Token.id(), "Gateway Hub", Token.secret(), Access.WRITE, Kind.REDIRECT, "Redireccionar a Hub", System.currentTimeMillis())
            .also { save(tokens + it) }

    fun served(): List<Token> = tokens.filter { (it.kind == Kind.REDIRECT) == hubRedirect }
    fun linkFor(secret: String) = "${relayUrl.trimEnd('/')}/d/$device/mcp?token=$secret"
    fun hubLink() = linkFor(redirectToken().secret)

    private fun save(list: List<Token>) { tokens = list; prefs?.edit { putString("tokens", SecretBox.seal(Token.serialize(list))) } }

    private var data: NotesData? = null

    /** «Conectar todas» in Gateway Hub (the user's tap there): turns «Redireccionar a Hub» on here. */
    fun connectFromHub() { if (!hubRedirect) redirect(true) }

    /**
     * Lucía (Gateway Hub's voice on this phone) runs one tool without internet, with the hub's own token: the same checks
     * and rules as through the relay. Only while «Redireccionar a Hub» is on. [body] is a JSON-RPC request, so is the answer.
     */
    fun local(body: String): String? {
        val d = data ?: return null
        if (!hubRedirect) return null
        val token = redirectToken()
        return kotlinx.coroutines.runBlocking {
            McpHandler(::served, d, BuildConfig.VERSION_NAME, onUsed = ::touch).handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(), body).body
        }
    }

    private fun apply() {
        val d = data ?: return
        if (active && relayUrl.isNotBlank()) {
            if (link == null) link = RelayLink(relayUrl, device, relaySecret,
                McpHandler(::served, d, BuildConfig.VERSION_NAME, onUsed = ::touch)) { state = it }.also { it.start() }
        } else { link?.stop(); link = null; state = RelayLink.State.OFFLINE }
    }

    private fun touch(token: Token) {
        val now = System.currentTimeMillis()
        if (now - token.lastUsedAt > 60_000) save(tokens.map { if (it.id == token.id) it.copy(lastUsedAt = now) else it })
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
