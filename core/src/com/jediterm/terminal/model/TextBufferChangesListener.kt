package com.jediterm.terminal.model

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
interface TextBufferChangesListener {
  fun linesAdded(index: Int, lines: List<TerminalLine>) {}

  fun linesRemoved(index: Int, count: Int) {}

  /**
   * The range from [xStart] (inclusively) to [xEnd] (exclusively) was replaced with [newEntry] in the line on [index].
   */
  fun lineChanged(index: Int, xStart: Int, xEnd: Int, newEntry: TerminalLine.TextEntry) {}

  fun lineWrappedStateChanged(index: Int, isWrapped: Boolean) {}

  fun bufferWidthResized(newWidth: Int) {}
}