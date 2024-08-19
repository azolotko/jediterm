package com.jediterm.terminal.model

import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.model.TerminalLine.TextEntry
import com.jediterm.terminal.model.TextBufferChangesListenerTest.TextBufferChangeEvent.*
import com.jediterm.terminal.util.CharUtils
import junit.framework.TestCase

class TextBufferChangesListenerTest : TestCase() {

  // -------------------- Delete Characters -------------------------------------------------------

  fun `test delete characters`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.deleteCharacters(x = 2, y = 0, count = 4)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 2, xEnd = 6, TextEntry.EMPTY),
      LineChangedEvent(index = 0, xStart = 4, xEnd = 4, createFillerEntry(4))
    )
    assertEquals(expected, events)
  }

  fun `test delete more than buffer width`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.deleteCharacters(x = 2, y = 0, count = 10)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 2, xEnd = 8, TextEntry.EMPTY),
      LineChangedEvent(index = 0, xStart = 2, xEnd = 2, createFillerEntry(10))
    )
    assertEquals(expected, events)
  }

  // -------------------- Insert Blank Characters -------------------------------------------------

  fun `test insert blank characters in the middle not facing the line length limit`() {
    val buffer = TerminalTextBuffer(15, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertBlankCharacters(x = 6, y = 0, count = 4)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 6, xEnd = 6, spacesEntry(4)),
    )
    assertEquals(expected, events)
  }

  fun `test insert blank characters in the middle facing the line length limit`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertBlankCharacters(x = 6, y = 0, count = 4)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 6, xEnd = 6, spacesEntry(4)),
      LineChangedEvent(index = 0, xStart = 10, xEnd = 12, TextEntry.EMPTY)
    )
    assertEquals(expected, events)
  }

  fun `test insert blank characters on line end`() {
    val buffer = TerminalTextBuffer(12, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertBlankCharacters(x = 8, y = 0, count = 2)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 8, xEnd = 8, spacesEntry(2))
    )
    assertEquals(expected, events)
  }

  fun `test insert blank characters after line end`() {
    val buffer = TerminalTextBuffer(15, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertBlankCharacters(x = 10, y = 0, count = 4)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 8, xEnd = 8, spacesEntry(2)),
      LineChangedEvent(index = 0, xStart = 10, xEnd = 10, spacesEntry(2))
    )
    assertEquals(expected, events)
  }

  fun `test insert blank characters after line end facing the line length limit`() {
    val buffer = TerminalTextBuffer(11, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertBlankCharacters(x = 10, y = 0, count = 4)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 8, xEnd = 8, spacesEntry(2)),
      LineChangedEvent(index = 0, xStart = 10, xEnd = 10, spacesEntry(1))
    )
    assertEquals(expected, events)
  }

  private fun spacesEntry(width: Int): TextEntry {
    return TextEntry(TextStyle.EMPTY, CharBuffer(CharUtils.EMPTY_CHAR, width))
  }

  private fun TerminalTextBuffer.doWithCollectingEvents(action: () -> Unit): List<TextBufferChangeEvent> {
    val events = mutableListOf<TextBufferChangeEvent>()

    val listener = object : TextBufferChangesListener {
      override fun linesAdded(index: Int, lines: List<TerminalLine>) {
        events.add(LinesAddedEvent(index, lines))
      }

      override fun linesRemoved(index: Int, count: Int) {
        events.add(LinesRemovedEvent(index, count))
      }

      override fun lineChanged(index: Int, xStart: Int, xEnd: Int, newEntry: TextEntry) {
        events.add(LineChangedEvent(index, xStart, xEnd, newEntry))
      }

      override fun lineWrappedStateChanged(index: Int, isWrapped: Boolean) {
        events.add(LineWrappedStateChangedEvent(index, isWrapped))
      }

      override fun bufferWidthResized(newWidth: Int) {
        events.add(BufferWidthResizedEvent(newWidth))
      }
    }

    addChangesListener(listener)
    try {
      action()
    }
    finally {
      removeChangesListener(listener)
    }
    return events
  }

  private sealed interface TextBufferChangeEvent {
    data class LinesAddedEvent(val index: Int, val lines: List<TerminalLine>) : TextBufferChangeEvent

    data class LinesRemovedEvent(val index: Int, val count: Int) : TextBufferChangeEvent

    data class LineChangedEvent(val index: Int, val xStart: Int, val xEnd: Int, val newEntry: TextEntry) : TextBufferChangeEvent

    data class LineWrappedStateChangedEvent(val index: Int, val isWrapped: Boolean) : TextBufferChangeEvent

    data class BufferWidthResizedEvent(val newWidth: Int) : TextBufferChangeEvent
  }
}