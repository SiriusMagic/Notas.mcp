package com.zirakusa.notas.data

/** One note. [deletedAt] set = it is in «Recientemente eliminados»; [locked] = only shown in «Bloqueado». */
data class Note(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val folderId: Long? = null,
    val locked: Boolean = false,
    val pinned: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

data class Folder(val id: Long = 0, val name: String)

/** One line of «Tareas». */
data class Task(val id: Long = 0, val text: String, val done: Boolean = false, val createdAt: Long = 0, val updatedAt: Long = 0)

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
