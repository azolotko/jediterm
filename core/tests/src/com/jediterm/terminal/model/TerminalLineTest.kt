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
}