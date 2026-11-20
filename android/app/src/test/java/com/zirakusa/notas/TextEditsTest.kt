package com.zirakusa.notas

import com.zirakusa.notas.data.TextEdits
import com.zirakusa.notas.data.TextEdits.Edit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextEditsTest {
    @Test fun headingsReplaceEachOtherAndToggleOff() {
        val h1 = TextEdits.togglePrefix(Edit("Compras\nCable", 3), "# ")
        assertEquals("# Compras\nCable", h1.text); assertEquals(5, h1.start)
        assertEquals(1, TextEdits.headingAt(h1))
        val h3 = TextEdits.togglePrefix(h1, "### ")
        assertEquals("### Compras\nCable", h3.text); assertEquals(3, TextEdits.headingAt(h3))
        assertEquals("Compras\nCable", TextEdits.body(h3).text)
        assertEquals("Compras\nCable", TextEdits.togglePrefix(Edit("# Compras\nCable", 4), "# ").text)
    }

    @Test fun numberedListCountsFromTheLineAbove() {
        assertEquals("1. a\n2. b", TextEdits.togglePrefix(Edit("1. a\nb", 6), "1. ").text)
        assertEquals("1. a", TextEdits.togglePrefix(Edit("a", 0), "1. ").text)
        assertEquals("a", TextEdits.togglePrefix(Edit("7. a", 4), "1. ").text)
    }

    @Test fun checkboxCyclesAndTapTicks() {
        val one = TextEdits.cycleCheck(Edit("Cable 8", 7))
        assertEquals("☐ Cable 8", one.text); assertEquals(9, one.start)
        assertEquals("☑ Cable 8", TextEdits.cycleCheck(one).text)
        assertEquals("Cable 8", TextEdits.cycleCheck(TextEdits.cycleCheck(one)).text)
        val tapped = TextEdits.tapOnBox(Edit("x\n☐ Cable 8", 1), Edit("x\n☐ Cable 8", 2))!!
        assertEquals("x\n☑ Cable 8", tapped.text); assertEquals(11, tapped.start)
        assertNull(TextEdits.tapOnBox(Edit("x\n☐ Cable 8", 1), Edit("x\n☐ Cable 8", 6)))   // inside the words: no tick
        assertNull(TextEdits.tapOnBox(Edit("x\nCable", 1), Edit("x\nCable", 2)))           // no box
    }

    @Test fun wrapAndUnwrap() {
        val bold = TextEdits.wrap(Edit("Spray negro", 6, 11), "**")
        assertEquals("Spray **negro**", bold.text); assertEquals(8 to 13, bold.start to bold.end)
        assertEquals("Spray negro", TextEdits.wrap(bold, "**").text)
        val empty = TextEdits.wrap(Edit("ab", 1), "~~")
        assertEquals("a~~~~b", empty.text); assertEquals(3, empty.start)
    }

    @Test fun enterContinuesAndEndsLists() {
        fun enter(text: String) = TextEdits.continueList(Edit(text, text.length), Edit("$text\n", text.length + 1))
        assertEquals("☐ a\n☐ ", enter("☐ a").text)
        assertEquals("☑ a\n☐ ", enter("☑ a").text)
        assertEquals("• a\n• ", enter("• a").text)
        assertEquals("9. a\n10. ", enter("9. a").text)
        assertEquals("x\n", enter("x\n• ").text)                // empty item ends the list
        assertEquals("# T\n", enter("# T").text)                // headings do not continue
        val typed = Edit("ab", 2)
        assertEquals(typed, TextEdits.continueList(Edit("a", 1), typed))
    }

    @Test fun tableAndPlainText() {
        val t = TextEdits.table(Edit("Lista", 5), columns = 2, rows = 2)
        assertEquals("Lista\n|  |  |\n|  |  |\n", t.text); assertEquals(8, t.start)
        assertEquals("Título\n○ a\n✓ b negrita", TextEdits.plain("## Título\n☐ a\n☑ b **negrita**"))
        assertEquals("ab", TextEdits.insert(Edit("aXb", 1, 2), "").text)
    }
}
