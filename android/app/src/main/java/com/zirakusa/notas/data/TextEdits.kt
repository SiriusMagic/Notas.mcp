package com.zirakusa.notas.data

/**
 * The editor's text tools as pure functions over the text and its selection (no Android, tested in TextEditsTest).
 * Notes stay plain text with a few marks, so they can be searched, shared and read by an AI as they are:
 * `# `…`#### ` headings H1–H4, `**bold**`, `*italic*`, `__underline__`, `~~strike~~`, `• ` list, `1. ` numbered list,
 * `☐ ` / `☑ ` checklist and `| a | b |` table rows.
 */
object TextEdits {
    data class Edit(val text: String, val start: Int, val end: Int = start)

    const val UNCHECKED = "☐ "
    const val CHECKED = "☑ "
    const val BULLET = "• "
    private val numbered = Regex("^(\\d{1,4})\\. ")
    val headings = listOf("# ", "## ", "### ", "#### ")
    private val fixedPrefixes = headings + listOf(BULLET, UNCHECKED, CHECKED)

    /** The list or heading mark at the start of [line], or "". */
    fun prefixOf(line: String): String =
        numbered.find(line)?.value ?: fixedPrefixes.sortedByDescending { it.length }.firstOrNull { line.startsWith(it) }.orEmpty()

    /** Puts [prefix] on the cursor's line, replacing any other mark; the same mark again removes it. */
    fun togglePrefix(e: Edit, prefix: String): Edit = onLine(e) { line ->
        val old = prefixOf(line)
        val same = if (prefix == "1. ") old.firstOrNull()?.isDigit() == true else old == prefix
        when {
            same -> "" to old.length
            prefix == "1. " -> numberFor(e) to old.length
            else -> prefix to old.length
        }
    }

    /** Checklist button: no box → ☐ → ☑ → no box. */
    fun cycleCheck(e: Edit): Edit = onLine(e) { line ->
        when (val old = prefixOf(line)) {
            UNCHECKED -> CHECKED to old.length
            CHECKED -> "" to old.length
            else -> UNCHECKED to old.length
        }
    }

    /** Bold / italic: wraps the selection (or unwraps it); with nothing selected leaves the cursor between the marks. */
    fun wrap(e: Edit, mark: String): Edit {
        val (s, t) = e.start.coerceAtMost(e.end) to e.end.coerceAtLeast(e.start)
        val text = e.text
        if (s >= mark.length && t + mark.length <= text.length &&
            text.substring(s - mark.length, s) == mark && text.substring(t, t + mark.length) == mark) {
            return Edit(text.removeRange(t, t + mark.length).removeRange(s - mark.length, s), s - mark.length, t - mark.length)
        }
        val out = text.substring(0, s) + mark + text.substring(s, t) + mark + text.substring(t)
        return Edit(out, s + mark.length, t + mark.length)
    }

    /** Replaces the selection with [insert] and leaves the cursor after it. */
    fun insert(e: Edit, insert: String): Edit {
        val (s, t) = e.start.coerceAtMost(e.end) to e.end.coerceAtLeast(e.start)
        return Edit(e.text.substring(0, s) + insert + e.text.substring(t), s + insert.length)
    }

    /** A table of [columns]×[rows] on its own lines, the cursor in the first cell. */
    fun table(e: Edit, columns: Int = 3, rows: Int = 3): Edit {
        val s = e.start.coerceAtMost(e.end)
        val before = if (s > 0 && e.text[s - 1] != '\n') "\n" else ""
        val row = "|" + "  |".repeat(columns)
        val block = before + List(rows) { row }.joinToString("\n") + "\n"
        val inserted = insert(e, block)
        return inserted.copy(start = s + before.length + 2, end = s + before.length + 2)
    }

    /**
     * Enter on a list line continues the list (☑ continues as ☐, numbers go up); Enter on a line that is only the mark
     * ends the list. Returns [new] unchanged for any other change.
     */
    fun continueList(old: Edit, new: Edit): Edit {
        val at = new.start
        if (new.text.length != old.text.length + 1 || at == 0 || new.text[at - 1] != '\n' ||
            new.text.removeRange(at - 1, at) != old.text) return new
        val lineStart = new.text.lastIndexOf('\n', at - 2) + 1
        val line = new.text.substring(lineStart, at - 1)
        val prefix = prefixOf(line)
        if (prefix.isEmpty() || prefix.startsWith("#")) return new
        if (line == prefix) return Edit(new.text.removeRange(lineStart, at), lineStart)       // empty item: list ends
        val next = when (prefix) {
            CHECKED -> UNCHECKED
            BULLET, UNCHECKED -> prefix
            else -> "${prefix.dropLast(2).toInt() + 1}. "
        }
        return Edit(new.text.substring(0, at) + next + new.text.substring(at), at + next.length)
    }

    /**
     * A tap on a ☐/☑ box (the cursor jumps to just before or after the box, text unchanged) ticks it and leaves the
     * cursor at the end of that line. Null when [new] is not such a tap.
     */
    fun tapOnBox(old: Edit, new: Edit): Edit? {
        if (new.text != old.text || new.start != new.end || new.start == old.start) return null
        val lineStart = lineStartOf(new.text, new.start)
        if (new.start - lineStart > 1) return null
        val prefix = prefixOf(new.text.substring(lineStart))
        if (prefix != UNCHECKED && prefix != CHECKED) return null
        val text = new.text.replaceRange(lineStart, lineStart + 2, if (prefix == UNCHECKED) CHECKED else UNCHECKED)
        val lineEnd = text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
        return Edit(text, lineEnd)
    }

    /** «Cuerpo»: takes the heading mark off the cursor's line. */
    fun body(e: Edit): Edit = onLine(e) { line -> val old = prefixOf(line); (if (old in headings) "" else old) to old.length }

    /** Heading level (1–4) of the cursor's line, or 0 for body text. */
    fun headingAt(e: Edit): Int {
        val start = lineStartOf(e.text, e.start)
        return headings.indexOf(prefixOf(e.text.substring(start))).let { it + 1 }
    }

    /** Text for cards, sharing and AI: without heading and style marks, boxes as ○ / ✓. */
    fun plain(text: String): String = text.lineSequence().joinToString("\n") { line ->
        val prefix = prefixOf(line)
        val rest = if (prefix in headings) line.removePrefix(prefix) else line
        rest.replace("**", "").replace("~~", "").replace("__", "").replace(UNCHECKED, "○ ").replace(CHECKED, "✓ ")
    }

    /** «1. », or one more than the number on the line above. */
    private fun numberFor(e: Edit): String {
        val lineStart = lineStartOf(e.text, e.start)
        if (lineStart == 0) return "1. "
        val prevStart = e.text.lastIndexOf('\n', lineStart - 2) + 1
        val prev = numbered.find(e.text.substring(prevStart, lineStart - 1))?.groupValues?.get(1)?.toInt() ?: 0
        return "${prev + 1}. "
    }

    /** Changes the start of the cursor's line: [change] gets the line and returns (new mark, length of the old mark). */
    private fun onLine(e: Edit, change: (String) -> Pair<String, Int>): Edit {
        val lineStart = lineStartOf(e.text, e.start)
        val lineEnd = e.text.indexOf('\n', lineStart).let { if (it < 0) e.text.length else it }
        val (mark, oldLength) = change(e.text.substring(lineStart, lineEnd))
        val text = e.text.substring(0, lineStart) + mark + e.text.substring(lineStart + oldLength)
        val shift = mark.length - oldLength
        fun move(p: Int) = if (p < lineStart + oldLength) (lineStart + mark.length).coerceAtLeast(lineStart) else p + shift
        return Edit(text, move(e.start), move(e.end))
    }

    private fun lineStartOf(text: String, cursor: Int): Int {
        val c = cursor.coerceIn(0, text.length)
        return if (c == 0) 0 else text.lastIndexOf('\n', c - 1) + 1
    }
}
