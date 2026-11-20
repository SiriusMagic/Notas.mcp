package com.zirakusa.notas.ui

/** What the editor can undo: the title and the text together. */
data class Draft(val title: String, val body: String)

/**
 * Undo and redo of the editor (the ↶ ↷ buttons). Typing inside one word is a single step, so undo
 * goes back word by word instead of letter by letter. Pure Kotlin, tested in UndoHistoryTest.
 */
class UndoHistory(initial: Draft, private val limit: Int = 200) {
    private val past = ArrayDeque<Draft>()
    private val future = ArrayDeque<Draft>()
    var current: Draft = initial
        private set
    private var typingWord = false

    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()

    fun record(next: Draft) {
        if (next == current) return
        val added = typedChar(current, next)
        val inWord = added != null && !added.isWhitespace()
        if (!(inWord && typingWord)) {
            past.addLast(current)
            if (past.size > limit) past.removeFirst()
        }
        typingWord = inWord
        current = next
        future.clear()
    }

    fun undo(): Draft {
        val previous = past.removeLastOrNull() ?: return current
        future.addLast(current); current = previous; typingWord = false
        return current
    }

    fun redo(): Draft {
        val next = future.removeLastOrNull() ?: return current
        past.addLast(current); current = next; typingWord = false
        return current
    }

    /** The one character added at the end of the title or the text, or null for any other change. */
    private fun typedChar(old: Draft, new: Draft): Char? = when {
        old.title == new.title && new.body.length == old.body.length + 1 && new.body.startsWith(old.body) -> new.body.last()
        old.body == new.body && new.title.length == old.title.length + 1 && new.title.startsWith(old.title) -> new.title.last()
        else -> null
    }
}
