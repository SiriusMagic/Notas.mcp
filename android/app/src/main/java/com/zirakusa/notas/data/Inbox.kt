package com.zirakusa.notas.data

/**
 * Notes other family apps hand to Notas (Lucía's counts in Gateway Hub): a new note, never a change to one that exists,
 * in the folder they name (created if missing). Pure apart from the [NoteStore].
 */
object Inbox {
    const val TITLE_MAX = 200
    const val BODY_MAX = 20_000
    const val FOLDER_MAX = 60

    /** The new note's id, or null when there is nothing to keep. */
    fun add(store: NoteStore, title: String?, body: String?, folder: String?, now: Long): Long? {
        val t = title.orEmpty().replace('\n', ' ').trim().take(TITLE_MAX)
        val b = body.orEmpty().take(BODY_MAX)
        if (NotesRules.isEmpty(t, b)) return null
        val name = folder.orEmpty().replace('\n', ' ').trim().take(FOLDER_MAX)
        val folderId = if (name.isEmpty()) null
            else store.folders().firstOrNull { it.name.equals(name, ignoreCase = true) }?.id ?: store.addFolder(name)
        return store.save(Note(title = t, body = b, folderId = folderId, createdAt = now, updatedAt = now))
    }
}
