package com.zirakusa.notas.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zirakusa.notas.data.Attachment
import com.zirakusa.notas.data.Folder
import com.zirakusa.notas.data.Layout
import com.zirakusa.notas.data.Note
import com.zirakusa.notas.data.NoteStore
import com.zirakusa.notas.data.NotesRules
import com.zirakusa.notas.data.Section

enum class Tab { NOTES, TASKS }

/** Single owner of what the app shows. Screens only paint it and call these functions. */
/** What the phone does with reminders (AlarmManager in the app, nothing in tests). */
interface ReminderScheduler {
    fun set(kind: String, id: Long, at: Long, text: String) = Unit
    fun cancel(kind: String, id: Long) = Unit
}

class NotesState(
    private val store: NoteStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val reminders: ReminderScheduler = object : ReminderScheduler {},
) {
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
    fun trashSelected() { val now = clock(); chosen().forEach { reminders.cancel(NOTE, it.id) }; changeSelected { it.copy(deletedAt = now, remindAt = null) } }
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

    fun open(note: Note) {
        editing = Editing(note, UndoHistory(Draft(note.title, note.body)), if (note.id == 0L) emptyList() else store.attachments(note.id))
    }

    /** Opens the note with [id] (a home-screen shortcut or a reminder). False when it does not exist or is locked. */
    fun openById(id: Long): Boolean {
        val note = notes.firstOrNull { it.id == id && it.deletedAt == null } ?: return false
        if (note.locked && !unlocked) return false
        open(note); return true
    }

    /** Leaves the editor: keeps the note if it changed, and drops a new one left empty. */
    fun closeEditor() {
        val e = editing ?: return
        editing = null
        if (e.readOnly) return
        if (isEmpty(e)) {
            if (e.note.id != 0L) { reminders.cancel(NOTE, e.note.id); store.deleteForever(listOf(e.note.id)) }
            reload()
        } else persist(e)
    }

    /** Keeps what is written without leaving the editor (✓, or the app goes to the background). */
    fun saveOpen() {
        val e = editing ?: return
        if (e.readOnly || isEmpty(e)) return
        e.note = persist(e)
    }

    private fun isEmpty(e: Editing) = NotesRules.isEmpty(e.draft.title, e.draft.body) && e.attachments.isEmpty()

    private fun persist(e: Editing): Note {
        val (title, body) = e.draft
        if (title == e.note.title && body == e.note.body && e.note.id != 0L) return e.note
        val now = clock()
        val note = e.note.copy(title = title, body = body, updatedAt = now, createdAt = e.note.createdAt.takeIf { it > 0 } ?: now)
        val saved = note.copy(id = store.save(note))
        reload()
        return saved
    }

    /** Changes the open note's settings (pin, lock, background, reminder, folder) and keeps them at once. */
    fun changeOpen(change: (Note) -> Note) {
        val e = editing ?: return
        if (e.readOnly) return
        val next = change(e.note)
        e.note = if (next.id != 0L) { store.save(next); reload(); next } else next
        if (next.remindAt != null && next.id != 0L) reminders.set(NOTE, next.id, next.remindAt, NotesRules.displayTitle(next.copy(title = e.draft.title, body = e.draft.body)))
        else if (next.id != 0L) reminders.cancel(NOTE, next.id)
    }

    /** «Recordatorio»: [at] null removes it. A new note is saved first so the reminder can open it. */
    fun remindOpen(at: Long?) {
        val e = editing ?: return
        if (e.note.id == 0L) e.note = persist(e)
        changeOpen { it.copy(remindAt = at) }
    }

    /** Keeps a photo or drawing (already in [NoteStore.attachmentDir]) with the open note, saving a new note first. */
    fun attach(kind: Attachment.Kind, file: String) {
        val e = editing ?: return
        if (e.readOnly) return
        if (e.note.id == 0L) e.note = persist(e)
        store.addAttachment(Attachment(noteId = e.note.id, kind = kind, file = file, createdAt = clock()))
        e.attachments = store.attachments(e.note.id)
    }

    fun detach(a: Attachment) {
        val e = editing ?: return
        store.deleteAttachment(a.id)
        e.attachments = store.attachments(e.note.id)
    }

    val attachmentDir get() = store.attachmentDir

    /** Sends the open note to «Recientemente eliminados» (kept 30 days). */
    fun trashOpen() {
        val e = editing ?: return
        editing = null
        val (title, body) = e.draft
        if (e.note.id != 0L) {
            reminders.cancel(NOTE, e.note.id)
            store.save(e.note.copy(title = title, body = body, updatedAt = clock(), deletedAt = clock(), remindAt = null))
        }
        reload()
    }

    fun restore(note: Note) { store.save(note.copy(deletedAt = null)); editing = null; reload() }

    fun deleteForever(note: Note) { store.deleteForever(listOf(note.id)); editing = null; reload() }

    fun emptyTrash() { store.deleteForever(notes.filter { it.deletedAt != null }.map { it.id }); reload() }

    /** Moves the open note to [folderId] (null = «Sin categoría»). */
    fun moveOpen(folderId: Long?) = changeOpen { it.copy(folderId = folderId, locked = false) }

    /** Reminders still to come, to set again after the phone restarts. */
    fun upcomingReminders(): List<Note> = store.notes().filter { it.deletedAt == null && (it.remindAt ?: 0) > clock() }

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

    companion object {
        private const val LAYOUT = "layout"
        const val NOTE = "note"
    }
}
