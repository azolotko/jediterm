package com.jediterm.terminal.model

import com.jediterm.terminal.TerminalColor
import com.jediterm.terminal.TextStyle
import junit.framework.TestCase

class TerminalLineTest : TestCase() {
  private val styles = listOf(
    TextStyle(),
    TextStyle(TerminalColor.index(0), null),
    TextStyle(TerminalColor.index(1), null),
    TextStyle(TerminalColor.index(2), null),
    TextStyle(TerminalColor.index(3), null),
  )

  // ------------------------- Delete Characters --------------------------------------------------

  fun `test delete characters inside single text entry`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry(" 1 ", styles[1])
    )

    line.deleteCharacters(1, 3, styles[4])

    val expected = listOf(
      textEntry("f", styles[0]),
      textEntry("o", styles[0]),
      textEntry(" 1 ", styles[1]),
      createFillerEntry(3, styles[4]),
    )
    assertEquals(expected, line.entries)
  }

  fun `test delete characters inside multiple text entries`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry(" 1 ", styles[1]),
      textEntry("barba", styles[2]),
      textEntry("abcde", styles[3])
    )

    line.deleteCharacters(2, 9, styles[4])

    val expected = listOf(
      textEntry("fo", styles[0]),
      textEntry("ba", styles[2]),
      textEntry("abcde", styles[3]),
      createFillerEntry(9, styles[4]),
    )
    assertEquals(expected, line.entries)
  }

  fun `test delete whole text entry`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry(" 1 ", styles[1]),
      textEntry("barba", styles[2])
    )

    line.deleteCharacters(5, 3, styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[2]),
      createFillerEntry(3, styles[4]),
    )
    assertEquals(expected, line.entries)
  }

  fun `test delete zero characters`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry(" 1 ", styles[1])
    )

    line.deleteCharacters(3, 0, styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry(" 1 ", styles[1])
    )
    assertEquals(expected, line.entries)
  }

  fun `test delete more characters than line length`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry(" 1 ", styles[1]),
      textEntry("barba", styles[2]),
      textEntry("abcde", styles[3])
    )

    line.deleteCharacters(2, 100, styles[4])

    val expected = listOf(
      textEntry("fo", styles[0]),
      createFillerEntry(100, styles[4]),
    )
    assertEquals(expected, line.entries)
  }

  // ------------------------- Insert Blank Characters --------------------------------------------

  fun `test insert blank characters in the middle not facing the line length limit`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
    )

    line.insertBlankCharacters(2, 5, 30, styles[4])

    val expected = listOf(
      textEntry("fo", styles[0]),
      textEntry("     ", styles[4]),
      textEntry("ofo", styles[0]),
      textEntry("barba", styles[1]),
    )
    assertEquals(expected, line.entries)
  }

  fun `test insert blank characters in the middle facing the line length limit`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
    )

    line.insertBlankCharacters(8, 4, 11, styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry("bar", styles[1]),
      textEntry("   ", styles[4])
    )
    assertEquals(expected, line.entries)
  }

  fun `test insert blank characters on line end`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
    )

    line.insertBlankCharacters(10, 4, 15, styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
      textEntry("    ", styles[4])
    )
    assertEquals(expected, line.entries)
  }

  fun `test insert blank characters after line end`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
    )

    // It will actually insert 2 blank characters after the line end and then 2 characters with the provided style.
    // So, the count limits the total added characters.
    line.insertBlankCharacters(12, 4, 20, styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
      textEntry("  ", TextStyle.EMPTY),
      textEntry("  ", styles[4])
    )
    assertEquals(expected, line.entries)
  }

  fun `test insert blank characters after line end facing the line length limit`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
    )

    // It will actually insert 2 blank characters after the line end and then 1 character with the provided style.
    // So, the count and max line length limit the total added characters.
    line.insertBlankCharacters(12, 4, 13, styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
      textEntry("  ", TextStyle.EMPTY),
      textEntry(" ", styles[4])
    )
    assertEquals(expected, line.entries)
  }

  // ------------------------- Write String -------------------------------------------------------

  fun `test write string inside single text entry`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1])
    )

    line.writeString(2, CharBuffer("ab"), styles[4])

    val expected = listOf(
      textEntry("fo", styles[0]),
      textEntry("ab", styles[4]),
      textEntry("o", styles[0]),
      textEntry("barba", styles[1])
    )
    assertEquals(expected, line.entries)
  }

  fun `test write string inside multiple text entries`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry(" 1 ", styles[1]),
      textEntry("barba", styles[2]),
      textEntry("abcde", styles[3])
    )

    line.writeString(2, CharBuffer("123456789"), styles[4])

    val expected = listOf(
      textEntry("fo", styles[0]),
      textEntry("123456789", styles[4]),
      textEntry("ba", styles[2]),
      textEntry("abcde", styles[3])
    )
    assertEquals(expected, line.entries)
  }

  fun `test write string with facing line length limit`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1])
    )

    line.writeString(8, CharBuffer("abcd"), styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry("bar", styles[1]),
      textEntry("abcd", styles[4]),
    )
    assertEquals(expected, line.entries)
  }

  fun `test write string at the end of line`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1])
    )

    line.writeString(10, CharBuffer("ab"), styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
      textEntry("ab", styles[4]),
    )
    assertEquals(expected, line.entries)
  }

  fun `test write string after the end of line`() {
    val line = terminalLine(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1])
    )

    line.writeString(12, CharBuffer("ab"), styles[4])

    val expected = listOf(
      textEntry("foofo", styles[0]),
      textEntry("barba", styles[1]),
      textEntry("  ", TextStyle.EMPTY),
      textEntry("ab", styles[4]),
    )
    assertEquals(expected, line.entries)
  }
}