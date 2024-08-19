package com.jediterm.terminal.model

import com.jediterm.terminal.TextStyle
import com.jediterm.terminal.model.TerminalLine.TextEntry
import com.jediterm.terminal.util.CharUtils
import com.jediterm.util.CharBufferUtil

@JvmOverloads
fun terminalLine(text: String, style: TextStyle = TextStyle.EMPTY): TerminalLine {
  return TerminalLine(TextEntry(style, CharBufferUtil.create(text)))
}

fun terminalLine(vararg textEntries: TextEntry): TerminalLine {
  val line = TerminalLine()
  for (entry in textEntries) {
    line.appendEntry(entry)
  }
  return line
}

fun textEntry(text: String, style: TextStyle = TextStyle.EMPTY): TextEntry {
  return TextEntry(style, CharBufferUtil.create(text))
}

fun createFillerEntry(width: Int, style: TextStyle = TextStyle.EMPTY): TextEntry {
  return TextEntry(style, CharBuffer(CharUtils.NUL_CHAR, width))
}

fun LinesStorage.getLineTexts(): List<String> {
  val lines = ArrayList<String>(size)
  for (line in this) {
    lines.add(line.text)
  }
  return lines
}
