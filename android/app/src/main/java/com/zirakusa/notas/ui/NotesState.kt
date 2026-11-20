package com.zirakusa.notas.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.notas.data.Folder
import com.zirakusa.notas.data.Layout
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NoteStore
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Section

enum class Tab { NOTES, TASKS }

/** The note open in the editor; [history] holds its title and text. */
class Editing(val note: Note, val history: UndoHistory) {
    var draft by mutableStateOf(history.current)
        private set
    val readOnly get() = note.deletedAt != null

    fun change(next: Draft) { if (!readOnly) { history.record(next); draft = history.current } }
    fun undo() { draft = history.undo() }
    fun redo() { draft = history.redo() }
}

/** Single owner of what the app shows. Screens only paint it and call these functions. */
class NotesState(private val store: NoteStore, private val clock: () -> Long = System::currentTimeMillis) {
    var notes by mutableStateOf(emptyList<Note>()); private set
    var folders by mutableStateOf(emptyList<Folder>()); private set
    var section by mutableStateOf<Section>(Section.Uncategorized); private set
    /** «Bloqueado» was opened with the phone's lock; closes again when the app goes to the background. */
    var unlocked by mutableStateOf(false); private set
    /** Notes chosen with a long press; not empty = selection mode. */
    var selected by mutableStateOf(emptySet<Long>()); private set
    var query by mutableStateOf("")
    var tab by mutableStateOf(Tab.NOTES)
    var layout by mutableStateOf(store.setting(LAYOUT)?.let { runCatching { Layout.valueOf(it) }.getOrNull() } ?: Layout.GRID)
        private set
    var editing by mutableStateOf<Editing?>(null); private set

    init {
        // Notes that stayed 30 days in the trash are removed when the app opens.
        val now = clock()
        store.notes().filter { NotesRules.expired(it, now) }.map { it.id }.takeIf { it.isNotEmpty() }?.let(store::deleteForever)
        reload()
    }

    val visible get() = NotesRules.visible(notes, section, query)
    val selecting get() = selected.isNotEmpty()

    /** Shows [next]. «Bloqueado» only after [unlock] (the phone's lock said yes). Returns false when it is still locked. */
    fun pick(next: Section): Boolean {
        if (next == Section.Locked && !unlocked) return false
        section = next; selected = emptySet(); query = ""
        return true
    }

    fun unlock() { unlocked = true; pick(Section.Locked) }

    /** The app went to the background: keeps what is written and locks «Bloqueado» again. */
    fun lockAgain() {
        saveOpen()
        if (!unlocked) return
        unlocked = false
        if (editing?.note?.locked == true) closeEditor()
        if (section == Section.Locked) pick(Section.Uncategorized)
    }

    fun toggleSelect(note: Note) { selected = if (note.id in selected) selected - note.id else selected + note.id }
    fun selectAll() { selected = if (selected.size == visible.size) emptySet() else visible.map { it.id }.toSet() }
    fun clearSelection() { selected = emptySet() }

    /** Pins the chosen notes, or unpins them when all of them are pinned already. */
    fun pinSelected() { val pin = !chosen().all { it.pinned }; changeSelected { it.copy(pinned = pin) } }
    /** Sends the chosen notes to «Bloqueado», or takes them out when looking at «Bloqueado». */
    fun lockSelected() { val lock = section != Section.Locked; changeSelected { it.copy(locked = lock) } }
    fun moveSelected(folderId: Long?) = changeSelected { it.copy(folderId = folderId, locked = false) }
    fun trashSelected() { val now = clock(); changeSelected { it.copy(deletedAt = now) } }
    fun restoreSelected() = changeSelected { it.copy(deletedAt = null) }
    fun deleteSelectedForever() { store.deleteForever(selected); selected = emptySet(); reload() }

    private fun chosen() = notes.filter { it.id in selected }
    private fun changeSelected(change: (Note) -> Note) { chosen().forEach { store.save(change(it)) }; selected = emptySet(); reload() }
    fun count(s: Section) = NotesRules.count(notes, s)

    fun sectionTitle(fixed: (Section) -> String): String =
        (section as? Section.InFolder)?.let { s -> folders.firstOrNull { it.id == s.id }?.name } ?: fixed(section)

    fun changeLayout(next: Layout) { layout = next; store.putSetting(LAYOUT, next.name) }

    fun newNote() {
        val folder = (section as? Section.InFolder)?.id
        open(Note(folderId = folder, locked = section == Section.Locked))
    }

    fun open(note: Note) { editing = Editing(note, UndoHistory(Draft(note.title, note.body))) }

    /** Leaves the editor: keeps the note if it changed, and drops a new one left empty. */
    fun closeEditor() {
        val e = editing ?: return
        editing = null
        if (e.readOnly) return
        if (NotesRules.isEmpty(e.draft.title, e.draft.body)) {
            if (e.note.id != 0L) store.deleteForever(listOf(e.note.id))
            reload()
        } else persist(e)
    }

    /** Keeps what is written without leaving the editor (the app goes to the background). */
    fun saveOpen() {
        val e = editing ?: return
        if (e.readOnly || NotesRules.isEmpty(e.draft.title, e.draft.body)) return
        editing = Editing(persist(e), e.history)
    }

    private fun persist(e: Editing): Note {
        val (title, body) = e.draft
        if (title == e.note.title && body == e.note.body && e.note.id != 0L) return e.note
        val now = clock()
        val note = e.note.copy(title = title, body = body, updatedAt = now, createdAt = e.note.createdAt.takeIf { it > 0 } ?: now)
        val saved = note.copy(id = store.save(note))
        reload()
        return saved
    }

    /** Sends the open note to «Recientemente eliminados» (kept 30 days). */
    fun trashOpen() {
        val e = editing ?: return
        editing = null
        val (title, body) = e.draft
        if (e.note.id != 0L) store.save(e.note.copy(title = title, body = body, updatedAt = clock(), deletedAt = clock()))
        reload()
    }

    fun restore(note: Note) { store.save(note.copy(deletedAt = null)); editing = null; reload() }

    fun deleteForever(note: Note) { store.deleteForever(listOf(note.id)); editing = null; reload() }

    fun emptyTrash() { store.deleteForever(notes.filter { it.deletedAt != null }.map { it.id }); reload() }

    /** Moves the open note to [folderId] (null = «Sin categoría»). */
    fun moveOpen(folderId: Long?) {
        val e = editing ?: return
        val moved = e.note.copy(folderId = folderId)
        editing = Editing(moved, e.history)
        if (e.note.id != 0L) { store.save(moved); reload() }
    }

    fun addFolder(name: String) {
        val clean = name.trim().take(60)
        if (clean.isEmpty()) return
        val id = store.addFolder(clean)
        reload()
        pick(Section.InFolder(id))
    }

    fun deleteFolder(id: Long) {
        store.deleteFolder(id)
        if (section == Section.InFolder(id)) pick(Section.Uncategorized)
        reload()
    }

    private fun reload() { notes = store.notes(); folders = store.folders() }

    private companion object { const val LAYOUT = "layout" }
}
