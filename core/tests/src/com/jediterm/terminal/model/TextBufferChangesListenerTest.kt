package com.jediterm.terminal.model

import com.jediterm.core.util.CellPosition
import com.jediterm.core.util.TermSize
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

  // -------------------- Insert Blank Characters -------------------------------------------------

  fun `test write string without facing line length limit`() {
    val buffer = TerminalTextBuffer(15, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.writeString(4, 1, CharBuffer("Other"))
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 4, xEnd = 8, textEntry("Other"))
    )
    assertEquals(expected, events)
  }

  fun `test write string with facing line length limit`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.writeString(4, 1, CharBuffer("OtherLine"))
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 4, xEnd = 8, textEntry("OtherLine"))
    )
    assertEquals(expected, events)
  }

  fun `test write string at the end of line`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.writeString(8, 1, CharBuffer("Line"))
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 8, xEnd = 8, textEntry("Line"))
    )
    assertEquals(expected, events)
  }

  fun `test write string after the end of line`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.writeString(10, 1, CharBuffer("Line"))
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 8, xEnd = 8, textEntry("  ")),
      LineChangedEvent(index = 0, xStart = 10, xEnd = 10, textEntry("Line"))
    )
    assertEquals(expected, events)
  }

  // -------------------- Clear and Erase Characters ----------------------------------------------

  fun `test clear line`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.clearLines(1, 2)
    }

    val expected = listOf(
      LineChangedEvent(index = 1, xStart = 0, xEnd = 10, createFillerEntry(10)),
      LineChangedEvent(index = 2, xStart = 0, xEnd = 9, createFillerEntry(10))
    )
    assertEquals(expected, events)
  }

  fun `test erase characters in the middle`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.eraseCharacters(2, 6, 0)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 2, xEnd = 6, spacesEntry(4))
    )
    assertEquals(expected, events)
  }

  fun `test erase characters at the end of line`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("someLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.eraseCharacters(4, 12, 0)
    }

    val expected = listOf(
      LineChangedEvent(index = 0, xStart = 4, xEnd = 8, createFillerEntry(8))
    )
    assertEquals(expected, events)
  }

  // -------------------- Resize ------------------------------------------------------------------

  fun `test resize with decreasing height`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))
    buffer.addLine(terminalLine("fifthLine"))
    buffer.addLine(terminalLine(spacesEntry(4)))
    buffer.addLine(terminalLine(createFillerEntry(10)))

    val events = buffer.doWithCollectingEvents {
      buffer.resize(TermSize(10, 3), CellPosition(1, 5), selection = null)
    }

    val expected = listOf(
      LinesRemovedEvent(index = 6, count = 1),
      LinesMovedToHistoryEvent(count = 3)
    )
    assertEquals(expected, events)
  }

  fun `test resize with increasing height`() {
    val buffer = TerminalTextBuffer(10, 3, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.moveScreenLinesToHistory()
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))
    buffer.addLine(terminalLine("fifthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.resize(TermSize(10, 10), CellPosition(9, 3), selection = null)
    }

    // Nothing is moved from the history to the screen
    val expected = emptyList<TextBufferChangeEvent>()
    assertEquals(expected, events)
  }

  // -------------------- Add, Move to History ----------------------------------------------------

  fun `test adding new line`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("zeroLine"))

    val line1 = terminalLine("firstLine")
    val line2 = terminalLine("secondLine")

    val events = buffer.doWithCollectingEvents {
      buffer.addLine(line1)
      buffer.addLine(line2)
    }

    val expected = listOf(
      LinesAddedEvent(1, listOf(line1)),
      LinesAddedEvent(2, listOf(line2))
    )
    assertEquals(expected, events)
  }

  fun `test move screen lines to history`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))
    buffer.addLine(terminalLine("fifthLine"))
    buffer.addLine(terminalLine(spacesEntry(4)))
    buffer.addLine(terminalLine(createFillerEntry(10)))

    val events = buffer.doWithCollectingEvents {
      buffer.moveScreenLinesToHistory()
    }

    val expected = listOf(
      LinesRemovedEvent(index = 6, count = 1),
      LinesMovedToHistoryEvent(count = 6)
    )
    assertEquals(expected, events)
  }

  // -------------------- Clear -------------------------------------------------------------------

  fun `test clear screen buffer`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.clearScreenBuffer()
    }

    val expected = listOf(
      LinesRemovedEvent(index = 0, count = 3)
    )
    assertEquals(expected, events)
  }

  fun `test clear history buffer`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.moveScreenLinesToHistory()
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.clearHistory()
    }

    val expected = listOf(
      HistoryClearedEvent
    )
    assertEquals(expected, events)
  }

  fun `test clear screen and history buffers`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.moveScreenLinesToHistory()
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.clearScreenAndHistoryBuffers()
    }

    val expected = listOf(
      LinesRemovedEvent(index = 0, count = 2),
      HistoryClearedEvent
    )
    assertEquals(expected, events)
  }

  // -------------------- Insert Lines ------------------------------------------------------------

  fun `test insert lines to start`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertLines(y = 0, count = 2, scrollRegionBottom = 4)
    }

    val fillerLine = terminalLine(createFillerEntry(10))
    val expected = listOf(
      LinesAddedEvent(index = 0, lines = listOf(fillerLine, fillerLine)),
      LinesRemovedEvent(index = 4, count = 2)
    )
    assertEquals(expected, events)
  }

  fun `test insert lines in the middle`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertLines(y = 1, count = 2, scrollRegionBottom = 4)
    }

    val fillerLine = terminalLine(createFillerEntry(10))
    val expected = listOf(
      LinesAddedEvent(index = 1, lines = listOf(fillerLine, fillerLine)),
      LinesRemovedEvent(index = 4, count = 2)
    )
    assertEquals(expected, events)
  }

  fun `test insert lines before last line`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertLines(y = 3, count = 2, scrollRegionBottom = 4)
    }

    val fillerLine = terminalLine(createFillerEntry(10))
    val expected = listOf(
      LinesAddedEvent(index = 3, lines = listOf(fillerLine, fillerLine)),
      LinesRemovedEvent(index = 4, count = 2)
    )
    assertEquals(expected, events)
  }

  fun `test insert lines after the end`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertLines(y = 4, count = 2, scrollRegionBottom = 4)
    }

    // Nothing should be changed
    val expected = emptyList<TextBufferChangeEvent>()
    assertEquals(expected, events)
  }

  fun `test insert lines preserving end lines`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertLines(y = 1, count = 2, scrollRegionBottom = 3)
    }

    val fillerLine = terminalLine(createFillerEntry(10))
    val expected = listOf(
      LinesAddedEvent(index = 1, lines = listOf(fillerLine, fillerLine)),
      LinesRemovedEvent(index = 3, count = 2)
    )
    assertEquals(expected, events)
  }

  fun `test insert more lines than in the y to lastLine range`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertLines(y = 1, count = 5, scrollRegionBottom = 3)
    }

    val fillerLine = terminalLine(createFillerEntry(10))
    val expected = listOf(
      LinesAddedEvent(index = 1, lines = listOf(fillerLine, fillerLine, fillerLine, fillerLine, fillerLine)),
      LinesRemovedEvent(index = 3, count = 5)
    )
    assertEquals(expected, events)
  }

  fun `test insert zero lines`() {
    val buffer = TerminalTextBuffer(10, 10, StyleState())
    buffer.addLine(terminalLine("firstLine"))
    buffer.addLine(terminalLine("secondLine"))
    buffer.addLine(terminalLine("thirdLine"))
    buffer.addLine(terminalLine("forthLine"))

    val events = buffer.doWithCollectingEvents {
      buffer.insertLines(y = 1, count = 0, scrollRegionBottom = 4)
    }

    // Nothing should be changed
    val expected = emptyList<TextBufferChangeEvent>()
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

      override fun linesMovedToHistory(count: Int) {
        events.add(LinesMovedToHistoryEvent(count))
      }

      override fun historyCleared() {
        events.add(HistoryClearedEvent)
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

    data class LinesMovedToHistoryEvent(val count: Int) : TextBufferChangeEvent

    data object HistoryClearedEvent : TextBufferChangeEvent

    data class LineChangedEvent(val index: Int, val xStart: Int, val xEnd: Int, val newEntry: TextEntry) : TextBufferChangeEvent

    data class LineWrappedStateChangedEvent(val index: Int, val isWrapped: Boolean) : TextBufferChangeEvent

    data class BufferWidthResizedEvent(val newWidth: Int) : TextBufferChangeEvent
  }
}