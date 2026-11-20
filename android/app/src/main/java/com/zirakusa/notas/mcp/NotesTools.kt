package com.zirakusa.notas.mcp

import com.zirakusa.notas.data.Folder
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Section
import com.zirakusa.notas.data.Task
import com.zirakusa.notas.data.TextEdits
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** What the tools read and change; the app gives the real notes (and refreshes screen and widgets), tests a fresh store. */
interface NotesData {
    fun notes(): List<Note>
    fun folders(): List<Folder>
    fun tasks(): List<Task>
    fun saveNote(note: Note): Long
    fun addFolder(name: String): Long
    fun saveTask(task: Task): Long
    fun deleteTask(id: Long)
    fun now(): Long = System.currentTimeMillis()
}

/** An expected failure the AI can read (structuredContent.error.code), never a crash. */
class ToolFailure(val code: String, message: String) : Exception(message)

/**
 * The notes' MCP tools. Locked notes and the trash are invisible here: asking for one answers NOT_FOUND, exactly as for
 * a note that does not exist. READ tokens list, search and read; WRITE tokens also create, edit, move, pin and delete
 * (to the trash, as on the phone). Arguments are checked one by one; nothing is ever run as code.
 */
object NotesTools {
    class Tool(val name: String, val description: String, val write: Boolean, val schema: JSONObject)

    const val TITLE_MAX = 200
    const val BODY_MAX = 100_000
    const val QUERY_MAX = 200
    const val TASK_MAX = 500

    private fun schema(vararg props: Pair<String, JSONObject>, required: List<String> = emptyList()) = JSONObject()
        .put("type", "object").put("properties", JSONObject().apply { props.forEach { (k, v) -> put(k, v) } })
        .put("required", JSONArray(required)).put("additionalProperties", false)
    private fun str(d: String) = JSONObject().put("type", "string").put("description", d)
    private fun int(d: String) = JSONObject().put("type", "integer").put("description", d)
    private fun bool(d: String) = JSONObject().put("type", "boolean").put("description", d)
    private fun intOrNull(d: String) = JSONObject().put("type", JSONArray(listOf("integer", "null"))).put("description", d)

    private val ID = "id" to int("Note id (from notes_list or notes_search).")
    private val LIMIT = "limit" to int("How many, 1–100 (default 20).")
    private val OFFSET = "offset" to int("Skip this many (default 0), to page.")

    val tools = listOf(
        Tool("notes_list", "List notes, pinned first then newest. Optional folder_id (null = «Sin categoría»; omit for all).", false,
            schema("folder_id" to intOrNull("Folder id, or null for notes without folder."), LIMIT, OFFSET)),
        Tool("notes_search", "Find notes whose title or text has every word of the query (ignores case and accents).", false,
            schema("query" to str("Words to look for."), LIMIT, OFFSET, required = listOf("query"))),
        Tool("notes_get", "Read one note whole: title, text, folder, pinned, dates and reminder.", false, schema(ID, required = listOf("id"))),
        Tool("notes_folders", "List the folders with how many notes each has.", false, schema()),
        Tool("tasks_list", "List the tasks: pending first, then the done ones.", false,
            schema("include_done" to bool("Also list done tasks (default true)."))),
        Tool("notes_create", "Create a note. Give a title, a text or both. Marks: '# ' heading, **bold**, '☐ ' checkbox, '• ' bullet.", true,
            schema("title" to str("Title (up to $TITLE_MAX characters)."), "body" to str("Text (up to $BODY_MAX characters)."),
                "folder_id" to intOrNull("Folder id; omit or null for «Sin categoría»."))),
        Tool("notes_update", "Change a note's title or text, or add text at its end (append).", true,
            schema(ID, "title" to str("New title."), "body" to str("New whole text."), "append" to str("Text to add on a new line at the end."),
                required = listOf("id"))),
        Tool("notes_move", "Move a note to a folder (null = «Sin categoría»).", true,
            schema(ID, "folder_id" to intOrNull("Folder id or null."), required = listOf("id", "folder_id"))),
        Tool("notes_pin", "Pin a note (it shows first) or unpin it.", true, schema(ID, "pinned" to bool("true to pin."), required = listOf("id", "pinned"))),
        Tool("notes_delete", "Send a note to «Recientemente eliminados» (the user can restore it for 30 days).", true, schema(ID, required = listOf("id"))),
        Tool("folders_create", "Create a folder.", true, schema("name" to str("Folder name (up to 60 characters)."), required = listOf("name"))),
        Tool("tasks_create", "Add a task.", true, schema("text" to str("What to do (up to $TASK_MAX characters)."), required = listOf("text"))),
        Tool("tasks_complete", "Mark a task done, or pending again with done=false.", true,
            schema("id" to int("Task id."), "done" to bool("Default true."), required = listOf("id"))),
        Tool("tasks_delete", "Delete a task.", true, schema("id" to int("Task id."), required = listOf("id"))),
    )

    fun available(access: Access) = tools.filter { access == Access.WRITE || !it.write }

    fun call(name: String, args: JSONObject, access: Access, data: NotesData): JSONObject {
        val tool = tools.firstOrNull { it.name == name } ?: throw ToolFailure("UNKNOWN_TOOL", "No tool called «$name».")
        if (tool.write && access != Access.WRITE) throw ToolFailure("READ_ONLY", "This token is read-only.")
        val allowed = tool.schema.getJSONObject("properties").keys().asSequence().toSet()
        args.keys().forEach { if (it !in allowed) throw ToolFailure("BAD_ARGUMENT", "Unknown argument «$it».") }
        return when (name) {
            "notes_list" -> page(visible(data).filter { n -> !args.has("folder_id") || n.folderId == optId(args, "folder_id") }, args)
            "notes_search" -> page(NotesRules.visible(data.notes(), Section.All, text(args, "query", QUERY_MAX, required = true)!!), args)
            "notes_get" -> full(note(data, args), data.folders())
            "notes_folders" -> JSONObject().put("folders", JSONArray(data.folders().map { f ->
                JSONObject().put("id", f.id).put("name", f.name).put("notes", NotesRules.count(data.notes(), Section.InFolder(f.id)))
            })).put("uncategorized", NotesRules.count(data.notes(), Section.Uncategorized))
            "tasks_list" -> {
                val done = if (args.has("include_done")) flag(args, "include_done") else true
                JSONObject().put("tasks", JSONArray(NotesRules.visibleTasks(data.tasks()).filter { done || !it.done }.map(::task)))
            }
            "notes_create" -> {
                val title = text(args, "title", TITLE_MAX).orEmpty().replace("\n", " ")
                val body = text(args, "body", BODY_MAX).orEmpty()
                if (NotesRules.isEmpty(title, body)) throw ToolFailure("BAD_ARGUMENT", "Give a title or a text.")
                val now = data.now()
                val id = data.saveNote(Note(title = title, body = body, folderId = folder(data, args), createdAt = now, updatedAt = now))
                full(data.notes().first { it.id == id }, data.folders())
            }
            "notes_update" -> {
                val n = note(data, args)
                val title = text(args, "title", TITLE_MAX)?.replace("\n", " ")
                val body = text(args, "body", BODY_MAX)
                val append = text(args, "append", BODY_MAX)
                if (title == null && body == null && append == null) throw ToolFailure("BAD_ARGUMENT", "Give title, body or append.")
                var newBody = body ?: n.body
                if (append != null) newBody = if (newBody.isEmpty()) append else newBody.trimEnd('\n') + "\n" + append
                if (newBody.length > BODY_MAX) throw ToolFailure("TOO_LONG", "The text would pass $BODY_MAX characters.")
                val next = n.copy(title = title ?: n.title, body = newBody, updatedAt = data.now())
                if (NotesRules.isEmpty(next.title, next.body)) throw ToolFailure("BAD_ARGUMENT", "A note cannot be left empty; use notes_delete.")
                data.saveNote(next); full(next, data.folders())
            }
            "notes_move" -> { val n = note(data, args).copy(folderId = folder(data, args), updatedAt = data.now()); data.saveNote(n); full(n, data.folders()) }
            "notes_pin" -> { val n = note(data, args).copy(pinned = flag(args, "pinned")); data.saveNote(n); full(n, data.folders()) }
            "notes_delete" -> {
                val n = note(data, args)
                data.saveNote(n.copy(deletedAt = data.now(), remindAt = null))
                JSONObject().put("deleted", n.id).put("restorable_days", NotesRules.TRASH_DAYS)
            }
            "folders_create" -> {
                val name = text(args, "name", 60, required = true)!!.trim()
                if (name.isEmpty()) throw ToolFailure("BAD_ARGUMENT", "The name is empty.")
                JSONObject().put("id", data.addFolder(name)).put("name", name)
            }
            "tasks_create" -> {
                val text = text(args, "text", TASK_MAX, required = true)!!.replace('\n', ' ').trim()
                if (text.isEmpty()) throw ToolFailure("BAD_ARGUMENT", "The text is empty.")
                val now = data.now()
                val id = data.saveTask(Task(text = text, createdAt = now, updatedAt = now))
                task(data.tasks().first { it.id == id })
            }
            "tasks_complete" -> {
                val t = taskOf(data, args)
                val done = if (args.has("done")) flag(args, "done") else true
                val next = t.copy(done = done, updatedAt = data.now()); data.saveTask(next); task(next)
            }
            "tasks_delete" -> { val t = taskOf(data, args); data.deleteTask(t.id); JSONObject().put("deleted", t.id) }
            else -> throw ToolFailure("UNKNOWN_TOOL", "No tool called «$name».")
        }
    }

    /** The notes an AI may see: not locked, not in the trash. */
    private fun visible(data: NotesData) = NotesRules.visible(data.notes(), Section.All)

    private fun note(data: NotesData, args: JSONObject): Note {
        val id = id(args, "id")
        return visible(data).firstOrNull { it.id == id } ?: throw ToolFailure("NOT_FOUND", "No note with id $id.")
    }

    private fun taskOf(data: NotesData, args: JSONObject): Task {
        val id = id(args, "id")
        return data.tasks().firstOrNull { it.id == id } ?: throw ToolFailure("NOT_FOUND", "No task with id $id.")
    }

    private fun folder(data: NotesData, args: JSONObject): Long? {
        val id = optId(args, "folder_id") ?: return null
        if (data.folders().none { it.id == id }) throw ToolFailure("FOLDER_NOT_FOUND", "No folder with id $id.")
        return id
    }

    private fun id(args: JSONObject, key: String): Long {
        if (!args.has(key) || args.isNull(key)) throw ToolFailure("BAD_ARGUMENT", "«$key» is required.")
        return optId(args, key)!!
    }

    private fun optId(args: JSONObject, key: String): Long? {
        if (!args.has(key) || args.isNull(key)) return null
        val v = args.get(key)
        val id = when (v) { is Int -> v.toLong(); is Long -> v; is String -> v.toLongOrNull(); else -> null }
        if (id == null || id <= 0) throw ToolFailure("BAD_ARGUMENT", "«$key» must be a whole number above 0.")
        return id
    }

    private fun flag(args: JSONObject, key: String): Boolean =
        args.opt(key) as? Boolean ?: throw ToolFailure("BAD_ARGUMENT", "«$key» must be true or false.")

    private fun text(args: JSONObject, key: String, max: Int, required: Boolean = false): String? {
        if (!args.has(key) || args.isNull(key)) { if (required) throw ToolFailure("BAD_ARGUMENT", "«$key» is required."); return null }
        val v = args.get(key) as? String ?: throw ToolFailure("BAD_ARGUMENT", "«$key» must be text.")
        if (v.length > max) throw ToolFailure("TOO_LONG", "«$key» passes $max characters.")
        return v
    }

    private fun page(list: List<Note>, args: JSONObject): JSONObject {
        val limit = if (args.has("limit")) optId(args, "limit")!!.toInt().coerceAtMost(100) else 20
        val offset = if (args.has("offset")) (args.opt("offset") as? Int)?.takeIf { it >= 0 } ?: throw ToolFailure("BAD_ARGUMENT", "«offset» must be 0 or more.") else 0
        return JSONObject().put("total", list.size).put("notes", JSONArray(list.drop(offset).take(limit).map { n ->
            JSONObject().put("id", n.id).put("title", NotesRules.displayTitle(n)).put("preview", TextEdits.plain(NotesRules.preview(n)).take(200))
                .put("folder_id", n.folderId ?: JSONObject.NULL).put("pinned", n.pinned).put("updated_at", iso(n.updatedAt))
        }))
    }

    private fun full(n: Note, folders: List<Folder>) = JSONObject().put("id", n.id).put("title", n.title).put("body", n.body)
        .put("folder_id", n.folderId ?: JSONObject.NULL).put("folder", folders.firstOrNull { it.id == n.folderId }?.name ?: "Sin categoría")
        .put("pinned", n.pinned).put("created_at", iso(n.createdAt)).put("updated_at", iso(n.updatedAt))
        .put("reminder", n.remindAt?.let(::iso) ?: JSONObject.NULL)

    private fun task(t: Task) = JSONObject().put("id", t.id).put("text", t.text).put("done", t.done)
        .put("reminder", t.remindAt?.let(::iso) ?: JSONObject.NULL)

    private fun iso(millis: Long) = Instant.ofEpochMilli(millis).toString()
}
