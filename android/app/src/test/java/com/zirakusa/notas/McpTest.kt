package com.zirakusa.notas

import androidx.test.core.app.ApplicationProvider
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.SqlNoteStore
import com.zirakusa.notas.data.Task
import com.zirakusa.notas.mcp.Access
import com.zirakusa.notas.mcp.HubLink
import com.zirakusa.notas.mcp.Kind
import com.zirakusa.notas.mcp.McpHandler
import com.zirakusa.notas.mcp.StoreData
import com.zirakusa.notas.mcp.Token
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The notes' MCP: tools list, good calls, bad arguments, not found, locked notes hidden, read-only, hub link. */
@RunWith(RobolectricTestRunner::class)
class McpTest {
    private val store = SqlNoteStore(ApplicationProvider.getApplicationContext(), null)
    private var changes = 0
    private val data = StoreData(store) { changes++ }
    private val reader = Token("r", "Claude", "not_" + "r".repeat(40), Access.READ, Kind.HUB, "Compras")
    private val writer = Token("w", "ChatGPT", "not_" + "w".repeat(40), Access.WRITE)
    private val paused = Token("p", "Viejo", "not_" + "p".repeat(40), Access.WRITE, paused = true)
    private val handler = McpHandler({ listOf(reader, writer, paused) }, data, "0.3.0")

    private val folder = store.addFolder("Obra")
    private val hh = store.save(Note(title = "Hh", body = "Cajetines\nSpray **negro**", folderId = folder, createdAt = 1, updatedAt = 10))
    private val guayaquil = store.save(Note(title = "Guayaquil", body = "Yuanshi", createdAt = 2, updatedAt = 20))
    private val secret = store.save(Note(title = "Clave", body = "1234", locked = true, createdAt = 3, updatedAt = 30))
    private val trashed = store.save(Note(title = "Vieja", body = "x", createdAt = 4, updatedAt = 40, deletedAt = 50))
    private val task = store.saveTask(Task(text = "Comprar cable 8", createdAt = 1, updatedAt = 1))

    private fun rpc(token: Token, method: String, params: JSONObject = JSONObject()) = runBlocking {
        JSONObject(handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${token.secret}"), emptyMap(),
            JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()).body).getJSONObject("result")
    }
    private fun tool(token: Token, name: String, args: JSONObject) = rpc(token, "tools/call", JSONObject().put("name", name).put("arguments", args))
    private fun ok(token: Token, name: String, args: JSONObject = JSONObject()) =
        tool(token, name, args).also { assertFalse(it.toString(), it.optBoolean("isError")) }.getJSONObject("structuredContent")
    private fun code(token: Token, name: String, args: JSONObject = JSONObject()) = tool(token, name, args).also { assertTrue(it.optBoolean("isError")) }
        .getJSONObject("structuredContent").getJSONObject("error").getString("code")

    @Test fun toolsFollowTheAccess() {
        val read = rpc(reader, "tools/list").getJSONArray("tools").toString()
        assertTrue(read.contains("notes_search")); assertTrue(read.contains("tasks_list")); assertFalse(read.contains("notes_create"))
        val write = rpc(writer, "tools/list").getJSONArray("tools").toString()
        assertTrue(write.contains("notes_create")); assertTrue(write.contains("tasks_delete"))
        val init = rpc(reader, "initialize")
        assertEquals("notas", init.getJSONObject("serverInfo").getString("name"))
        assertEquals("hub", init.getJSONObject("_meta").getJSONObject(McpHandler.TOKEN_META).getString("kind"))
    }

    @Test fun readListSearchAndGet() {
        val list = ok(reader, "notes_list")
        assertEquals(2, list.getInt("total"))                                    // the locked note and the trash are not there
        assertEquals("Guayaquil", list.getJSONArray("notes").getJSONObject(0).getString("title"))
        assertEquals(1, ok(reader, "notes_list", JSONObject().put("folder_id", folder)).getInt("total"))
        assertEquals(1, ok(reader, "notes_list", JSONObject().put("folder_id", JSONObject.NULL)).getInt("total"))
        val found = ok(reader, "notes_search", JSONObject().put("query", "SPRAY cajetines")).getJSONArray("notes")
        assertEquals(hh, found.getJSONObject(0).getLong("id"))
        assertTrue(found.getJSONObject(0).getString("preview").contains("Spray negro"))   // marks taken out
        assertEquals(0, ok(reader, "notes_search", JSONObject().put("query", "1234")).getInt("total"))
        val note = ok(reader, "notes_get", JSONObject().put("id", hh))
        assertEquals("Obra", note.getString("folder")); assertTrue(note.getString("body").contains("**negro**"))
        assertEquals(1, ok(reader, "notes_folders").getJSONArray("folders").getJSONObject(0).getInt("notes"))
        assertEquals("Comprar cable 8", ok(reader, "tasks_list").getJSONArray("tasks").getJSONObject(0).getString("text"))
    }

    @Test fun notFoundAndLockedLookTheSame() {
        assertEquals("NOT_FOUND", code(reader, "notes_get", JSONObject().put("id", 9999)))
        assertEquals("NOT_FOUND", code(reader, "notes_get", JSONObject().put("id", secret)))
        assertEquals("NOT_FOUND", code(reader, "notes_get", JSONObject().put("id", trashed)))
        assertEquals("NOT_FOUND", code(writer, "notes_update", JSONObject().put("id", secret).put("body", "robado")))
        assertEquals("1234", store.notes().first { it.id == secret }.body)
        assertEquals("NOT_FOUND", code(writer, "tasks_complete", JSONObject().put("id", 777)))
        assertEquals("FOLDER_NOT_FOUND", code(writer, "notes_create", JSONObject().put("title", "x").put("folder_id", 555)))
    }

    @Test fun badArgumentsAreRefused() {
        assertEquals("BAD_ARGUMENT", code(reader, "notes_get", JSONObject()))
        assertEquals("BAD_ARGUMENT", code(reader, "notes_get", JSONObject().put("id", "abc")))
        assertEquals("BAD_ARGUMENT", code(reader, "notes_get", JSONObject().put("id", -1)))
        assertEquals("BAD_ARGUMENT", code(reader, "notes_list", JSONObject().put("sql", "DROP TABLE notes")))
        assertEquals("BAD_ARGUMENT", code(writer, "notes_create", JSONObject()))
        assertEquals("BAD_ARGUMENT", code(writer, "notes_pin", JSONObject().put("id", hh).put("pinned", "yes")))
        assertEquals("TOO_LONG", code(reader, "notes_search", JSONObject().put("query", "x".repeat(201))))
        assertEquals("UNKNOWN_TOOL", code(reader, "notes_eval", JSONObject()))
        assertEquals("READ_ONLY", code(reader, "notes_delete", JSONObject().put("id", hh)))
        assertEquals(0, changes)
    }

    @Test fun writeChangesTheNotes() {
        val created = ok(writer, "notes_create", JSONObject().put("title", "Lista").put("body", "☐ Cable 12").put("folder_id", folder))
        val id = created.getLong("id")
        ok(writer, "notes_update", JSONObject().put("id", id).put("append", "☐ Caja de breakers"))
        assertEquals("☐ Cable 12\n☐ Caja de breakers", store.notes().first { it.id == id }.body)
        ok(writer, "notes_move", JSONObject().put("id", id).put("folder_id", JSONObject.NULL))
        ok(writer, "notes_pin", JSONObject().put("id", id).put("pinned", true))
        assertEquals(id, ok(reader, "notes_list").getJSONArray("notes").getJSONObject(0).getLong("id"))   // pinned first
        assertEquals(30, ok(writer, "notes_delete", JSONObject().put("id", id)).getInt("restorable_days"))
        assertTrue(store.notes().first { it.id == id }.deletedAt != null)                             // in the trash, restorable
        ok(writer, "folders_create", JSONObject().put("name", "Casa"))
        val t = ok(writer, "tasks_create", JSONObject().put("text", "Llamar al doctor")).getLong("id")
        assertTrue(ok(writer, "tasks_complete", JSONObject().put("id", t)).getBoolean("done"))
        ok(writer, "tasks_delete", JSONObject().put("id", task))
        assertEquals(listOf("Llamar al doctor"), store.tasks().map { it.text })
        assertTrue(changes >= 8)
    }

    @Test fun tokensAndHubLink() = runBlocking {
        assertEquals(401, handler.handle("POST", "/mcp", mapOf("Authorization" to "Bearer ${paused.secret}"), emptyMap(), "{}").status)
        assertEquals(401, handler.handle("POST", "/mcp", emptyMap(), emptyMap(), "{}").status)
        assertEquals(404, handler.handle("POST", "/other", emptyMap(), emptyMap(), "{}").status)
        assertEquals("NOT_SAME_DEVELOPER", HubLink.answer(false).getString("error"))
        assertEquals("REDIRECT_OFF", HubLink.answer(true).getString("error"))
    }
}
