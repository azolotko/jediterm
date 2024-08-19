package com.jediterm.terminal.model

import com.jediterm.terminal.model.TerminalLine.TextEntry
import org.jetbrains.annotations.ApiStatus

/**
 * Emits detailed events about changes inside [TerminalLine].
 */
@ApiStatus.Experimental
interface TerminalLineChangesListener {
  /**
   * It is guaranteed that [xStart] and [xEnd] are in the range [0, lineLength] and [xStart] is less or equal [xEnd].
   * @param xStart index of the first modified character (including).
   * @param xEnd index of the last modified character (excluding).
   * @param newEntry new text entry that should replace the actual text in the range [xStart, xEnd].
   */
  fun lineRangeChanged(xStart: Int, xEnd: Int, newEntry: TextEntry)
}