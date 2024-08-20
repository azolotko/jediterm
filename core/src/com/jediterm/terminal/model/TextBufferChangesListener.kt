package com.jediterm.terminal.model

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
interface TextBufferChangesListener {
  fun linesAdded(index: Int, lines: List<TerminalLine>) {}

  fun linesRemoved(index: Int, count: Int) {}

  /**
   * [count] of top lines of the screen are moved to the bottom of the history buffer.
   * It means that these lines are finalized and won't be changed anymore.
   */
  fun linesMovedToHistory(count: Int) {}

  /**
   * The range from [xStart] (inclusively) to [xEnd] (exclusively) was replaced with [newEntry] in the line on [index].
   */
  fun lineChanged(index: Int, xStart: Int, xEnd: Int, newEntry: TerminalLine.TextEntry) {}

  fun lineWrappedStateChanged(index: Int, isWrapped: Boolean) {}

  fun bufferWidthResized(newWidth: Int) {}
}