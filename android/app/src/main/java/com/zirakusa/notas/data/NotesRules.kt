package com.zirakusa.notas.data

import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Pure rules of the notes (no Android): which notes a section shows, search, titles, dates and the trash. */
object NotesRules {
    /** Notes stay this long in «Recientemente eliminados» before they are removed for good. */
    const val TRASH_DAYS = 30L
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun inSection(note: Note, section: Section): Boolean = when (section) {
        Section.Trash -> note.deletedAt != null
        Section.Locked -> note.deletedAt == null && note.locked
        Section.All -> note.deletedAt == null && !note.locked
        Section.Uncategorized -> note.deletedAt == null && !note.locked && note.folderId == null
        is Section.InFolder -> note.deletedAt == null && !note.locked && note.folderId == section.id
    }

    /** The notes of [section] that match [query]: pinned first, then newest (by last edit, or by deletion in the trash). */
    fun visible(notes: List<Note>, section: Section, query: String = ""): List<Note> =
        notes.filter { inSection(it, section) && matches(it.title + " " + it.body, query) }.sortedWith(
            if (section == Section.Trash) compareByDescending { it.deletedAt ?: 0 }
            else compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt }
        )

    /** Tasks that match [query]: pending first (newest first), then the done ones (last done first). */
    fun visibleTasks(tasks: List<Task>, query: String = ""): List<Task> =
        tasks.filter { matches(it.text, query) }.sortedWith(compareBy<Task> { it.done }.thenByDescending { if (it.done) it.updatedAt else it.createdAt })

    /** Every word of [query] is in [text], ignoring case and accents. */
    fun matches(text: String, query: String): Boolean {
        val folded = fold(text)
        return fold(query).split(' ').filter { it.isNotBlank() }.all { it in folded }
    }

    fun count(notes: List<Note>, section: Section) = notes.count { inSection(it, section) }

    /** Title shown on the card: the title, or else the first line of the text (like the phone's notes). */
    fun displayTitle(note: Note): String =
        note.title.trim().ifEmpty { note.body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty() }

    /** The text under the title on the card, without blank lines. */
    fun preview(note: Note): String = note.body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

    /** A note with neither title nor text is not kept. */
    fun isEmpty(title: String, body: String) = title.isBlank() && body.isBlank()

    fun expired(note: Note, now: Long) = note.deletedAt != null && now - note.deletedAt >= TRASH_DAYS * DAY_MS

    /** Days left before a note in the trash is removed (at least 0). */
    fun daysLeft(note: Note, now: Long): Long =
        note.deletedAt?.let { maxOf(0, TRASH_DAYS - (now - it) / DAY_MS) } ?: TRASH_DAYS

    /** «14:05» today, «11/18» this year, «2025/11/18» before. */
    fun shortDate(millis: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val date = Instant.ofEpochMilli(millis).atZone(zone)
        val today = Instant.ofEpochMilli(now).atZone(zone)
        val pattern = when {
            date.toLocalDate() == today.toLocalDate() -> "HH:mm"
            date.year == today.year -> "MM/dd"
            else -> "yyyy/MM/dd"
        }
        return date.format(DateTimeFormatter.ofPattern(pattern))
    }

    /** Lower case and without accents, so «cafe» finds «Café» and «guayaquil» finds «Guayaquil». */
    fun fold(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
}
