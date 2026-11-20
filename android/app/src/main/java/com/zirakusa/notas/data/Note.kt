package com.zirakusa.notas.data

/** One note. [deletedAt] set = it is in «Recientemente eliminados»; [locked] = only shown in «Bloqueado». */
data class Note(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val folderId: Long? = null,
    val locked: Boolean = false,
    val pinned: Boolean = false,
    /** «Fondo»: index in NoteColors (0 = the normal dark grey). */
    val color: Int = 0,
    /** «Recordatorio»: when the phone notifies about this note, or null. */
    val remindAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

data class Folder(val id: Long = 0, val name: String)

/** One line of «Tareas». */
data class Task(
    val id: Long = 0, val text: String, val done: Boolean = false, val createdAt: Long = 0, val updatedAt: Long = 0,
    val remindAt: Long? = null,
)

/** A photo (camera or gallery) or a drawing kept with a note, as a file in the app's private folder. */
data class Attachment(val id: Long = 0, val noteId: Long, val kind: Kind, val file: String, val createdAt: Long = 0) {
    enum class Kind { PHOTO, DRAWING }
}

/** What the list shows: the fixed folders of the drawer or one folder made by the user. */
sealed interface Section {
    data object All : Section
    data object Uncategorized : Section
    data object Locked : Section
    data object Trash : Section
    data class InFolder(val id: Long) : Section
}

/** «Vista de lista», «Vista de tarjeta» and «Vista en cuadrícula». */
enum class Layout { LIST, CARD, GRID }
