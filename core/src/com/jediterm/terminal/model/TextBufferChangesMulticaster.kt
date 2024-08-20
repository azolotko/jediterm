package com.jediterm.terminal.model

import com.jediterm.terminal.model.TerminalLine.TextEntry
import java.util.concurrent.CopyOnWriteArrayList

internal class TextBufferChangesMulticaster : TextBufferChangesListener {
  private var listeners: MutableList<TextBufferChangesListener> = CopyOnWriteArrayList()

  fun addListener(listener: TextBufferChangesListener) {
    listeners.add(listener)
  }

  fun removeListener(listener: TextBufferChangesListener) {
    listeners.remove(listener)
  }

  override fun linesAdded(index: Int, lines: List<TerminalLine>) {
    forEachListeners {
      it.linesAdded(index, lines)
    }
  }

  override fun linesRemoved(index: Int, count: Int) {
    forEachListeners {
      it.linesRemoved(index, count)
    }
  }

  override fun linesMovedToHistory(count: Int) {
    forEachListeners {
      it.linesMovedToHistory(count)
    }
  }

  override fun lineChanged(index: Int, xStart: Int, xEnd: Int, newEntry: TextEntry) {
    forEachListeners {
      it.lineChanged(index, xStart, xEnd, newEntry)
    }
  }

  override fun lineWrappedStateChanged(index: Int, isWrapped: Boolean) {
    forEachListeners {
      it.lineWrappedStateChanged(index, isWrapped)
    }
  }

  override fun bufferWidthResized(newWidth: Int) {
    forEachListeners {
      it.bufferWidthResized(newWidth)
    }
  }

  private inline fun forEachListeners(action: (TextBufferChangesListener) -> Unit) {
    for (listener in listeners) {
      action(listener)
    }
  }
}
