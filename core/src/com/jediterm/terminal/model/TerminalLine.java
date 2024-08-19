package com.jediterm.terminal.model;

import com.jediterm.terminal.StyledTextConsumer;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.util.CharUtils;
import kotlin.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author traff
 */
public final class TerminalLine {
  private static final Logger LOG = LoggerFactory.getLogger(TerminalLine.class);

  private TextEntries myTextEntries = new TextEntries();
  private boolean myWrapped = false;
  private final List<TerminalLineIntervalHighlighting> myCustomHighlightings = new CopyOnWriteArrayList<>();
  private final List<TerminalLineChangesListener> myListeners = new CopyOnWriteArrayList<>();
  TerminalLine myTypeAheadLine;

  public TerminalLine() {
  }

  public TerminalLine(@NotNull TextEntry entry) {
    myTextEntries.add(entry);
  }

  public static TerminalLine createEmpty() {
    return new TerminalLine();
  }

  public @NotNull String getText() {
    StringBuilder result = new StringBuilder(myTextEntries.myLength);
    for (TerminalLine.TextEntry textEntry : myTextEntries) {
      // NUL can only be at the end
      if (textEntry.getText().isNul()) {
        break;
      }
      result.append(textEntry.getText());
    }
    return result.toString();
  }

  public @NotNull TerminalLine copy() {
    TerminalLine result = new TerminalLine();
    for (TextEntry entry : myTextEntries) {
      result.myTextEntries.add(entry);
    }
    result.myWrapped = myWrapped;
    return result;
  }

  public char charAt(int x) {
    TerminalLine typeAheadLine = myTypeAheadLine;
    if (typeAheadLine != null) {
      return typeAheadLine.charAt(x);
    }
    String text = getText();
    return x < text.length() ? text.charAt(x) : CharUtils.EMPTY_CHAR;
  }

  /**
   * @return total length of text entries.
   */
  public int length() {
    return myTextEntries.length();
  }

  public boolean isWrapped() {
    return myWrapped;
  }

  public void setWrapped(boolean wrapped) {
    myWrapped = wrapped;
  }

  public void clear(@NotNull TextEntry filler) {
    myTextEntries.clear();
    myTextEntries.add(filler);
  }

  public void writeString(int x, @NotNull CharBuffer str, @NotNull TextStyle style) {
    writeCharacters(x, style, str);
  }

  public void insertString(int x, @NotNull CharBuffer str, @NotNull TextStyle style) {
    insertCharacters(x, style, str);
  }

  private void writeCharacters(int x, @NotNull TextStyle style, @NotNull CharBuffer characters) {
    int len = myTextEntries.length();

    if (x >= len) {
      // fill the gap
      if (x - len > 0) {
        appendEntry(new TextEntry(TextStyle.EMPTY, new CharBuffer(CharUtils.NUL_CHAR, x - len)));
      }
      appendEntry(new TextEntry(style, characters));
    } else {
      int newLen = Math.max(len, x + characters.length());
      myTextEntries = merge(x, characters, style, myTextEntries, newLen);

      int endX = Math.min(len, x + characters.length());
      fireLineChanged(x, endX, new TextEntry(style, characters));
    }
  }

  private void insertCharacters(int x, @NotNull TextStyle style, @NotNull CharBuffer characters) {
    int length = myTextEntries.length();
    if (x > length) {
      writeCharacters(x, style, characters);
      return;
    }

    Pair<char[], TextStyle[]> pair = toBuf(myTextEntries, length + characters.length());

    for (int i = length - 1; i >= x; i--) {
      pair.getFirst()[i + characters.length()] = pair.getFirst()[i];
      pair.getSecond()[i + characters.length()] = pair.getSecond()[i];
    }
    for (int i = 0; i < characters.length(); i++) {
      pair.getFirst()[i + x] = characters.charAt(i);
      pair.getSecond()[i + x] = style;
    }
    myTextEntries = collectFromBuffer(pair.getFirst(), pair.getSecond());
  }

  private static TextEntries merge(int x, @NotNull CharBuffer str, @NotNull TextStyle style, @NotNull TextEntries entries, int lineLength) {
    Pair<char[], TextStyle[]> pair = toBuf(entries, lineLength);

    for (int i = 0; i < str.length(); i++) {
      pair.getFirst()[i + x] = str.charAt(i);
      pair.getSecond()[i + x] = style;
    }

    return collectFromBuffer(pair.getFirst(), pair.getSecond());
  }

  private static Pair<char[], TextStyle[]> toBuf(TextEntries entries, int lineLength) {
    Pair<char[], TextStyle[]> pair = new Pair<>(new char[lineLength], new TextStyle[lineLength]);


    int p = 0;
    for (TextEntry entry : entries) {
      for (int i = 0; i < entry.getLength(); i++) {
        pair.getFirst()[p + i] = entry.getText().charAt(i);
        pair.getSecond()[p + i] = entry.getStyle();
      }
      p += entry.getLength();
    }
    return pair;
  }

  private static TextEntries collectFromBuffer(char[] buf, @NotNull TextStyle[] styles) {
    TextEntries result = new TextEntries();

    TextStyle curStyle = styles[0];
    int start = 0;

    for (int i = 1; i < buf.length; i++) {
      if (styles[i] != curStyle) {
        result.add(new TextEntry(curStyle, new CharBuffer(buf, start, i - start)));
        curStyle = styles[i];
        start = i;
      }
    }

    result.add(new TextEntry(curStyle, new CharBuffer(buf, start, buf.length - start)));

    return result;
  }

  public void deleteCharacters(int x) {
    deleteCharacters(x, TextStyle.EMPTY);
  }

  public void deleteCharacters(int x, @NotNull TextStyle style) {
    deleteCharacters(x, myTextEntries.length() - x, style);
  }

  public void deleteCharacters(int x, int count, @NotNull TextStyle style) {
    int lengthBefore = myTextEntries.length();
    int p = 0;
    TextEntries newEntries = new TextEntries();

    int remaining = count;

    for (TextEntry entry : myTextEntries) {
      if (remaining == 0) {
        newEntries.add(entry);
        continue;
      }
      int len = entry.getLength();
      if (p + len <= x) {
        p += len;
        newEntries.add(entry);
        continue;
      }
      int dx = x - p; //>=0
      if (dx > 0) {
        //part of entry before x
        newEntries.add(new TextEntry(entry.getStyle(), entry.getText().subBuffer(0, dx)));
        p = x;
      }
      if (dx + remaining < len) {
        //part that left after deleting count 
        newEntries.add(new TextEntry(entry.getStyle(), entry.getText().subBuffer(dx + remaining, len - (dx + remaining))));
        remaining = 0;
      } else {
        remaining -= (len - dx);
        p = x;
      }
    }

    int deletedCount = Math.min(count, lengthBefore - x);
    fireLineChanged(x, x + deletedCount, TextEntry.EMPTY);

    if (count > 0 && style != TextStyle.EMPTY) { // apply style to the end of the line
      var fillerEntry = new TextEntry(style, new CharBuffer(CharUtils.NUL_CHAR, count));
      newEntries.add(fillerEntry);
      fireLineChanged(lengthBefore - deletedCount, lengthBefore - deletedCount, fillerEntry);
    }

    myTextEntries = newEntries;
  }

  /**
   * Tries to insert count of blank characters at position x.
   * If the resulting line length is greater than maxLen, then the ending of the line is trimmed to the maxLen.
   * If x is greater than the line length, new characters will be appended,
   * but the resulting line length won't become greater than the provided count.
   */
  public void insertBlankCharacters(int x, int count, int maxLen, @NotNull TextStyle style) {
    int lengthBefore = myTextEntries.length();
    int newLen = Math.min(lengthBefore + count, maxLen);

    if (x < lengthBefore) {
      // Insert in the middle
      char[] buf = new char[newLen];
      TextStyle[] styles = new TextStyle[newLen];

      int p = 0;
      for (TextEntry entry : myTextEntries) {
        for (int i = 0; i < entry.getLength() && p < newLen; i++) {
          if (p == x) {
            for (int j = 0; j < count && p < newLen; j++) {
              buf[p] = CharUtils.EMPTY_CHAR;
              styles[p] = style;
              p++;
            }
          }
          if (p < newLen) {
            buf[p] = entry.getText().charAt(i);
            styles[p] = entry.getStyle();
            p++;
          }
        }
        if (p >= newLen) {
          break;
        }
      }

      myTextEntries = collectFromBuffer(buf, styles);

      fireLineChanged(x, x, new TextEntry(style, new CharBuffer(CharUtils.EMPTY_CHAR, count)));
      int trimmedCount = lengthBefore + count - maxLen;
      if (trimmedCount > 0) {
        fireLineChanged(newLen, newLen + trimmedCount, TextEntry.EMPTY);
      }
    }
    else {
      // Append after the end
      int emptyCountToAppend = Math.min(x, newLen) - lengthBefore;
      if (emptyCountToAppend > 0) {
        appendEntry(new TextEntry(TextStyle.EMPTY, new CharBuffer(CharUtils.EMPTY_CHAR, emptyCountToAppend)));
      }
      int blankCountToAppend = Math.min(x + count, newLen) - x;
      if (blankCountToAppend > 0) {
        appendEntry(new TextEntry(style, new CharBuffer(CharUtils.EMPTY_CHAR, blankCountToAppend)));
      }
    }
  }

  public void clearArea(int leftX, int rightX, @NotNull TextStyle style) {
    if (rightX == -1) {
      rightX = Math.max(myTextEntries.length(), leftX);
    }
    writeCharacters(leftX, style, new CharBuffer(
            rightX >= myTextEntries.length() ? CharUtils.NUL_CHAR : CharUtils.EMPTY_CHAR,
            rightX - leftX));
  }

  public @Nullable TextStyle getStyleAt(int x) {
    int i = 0;

    for (TextEntry te : myTextEntries) {
      if (x >= i && x < i + te.getLength()) {
        return te.getStyle();
      }
      i += te.getLength();
    }

    return null;
  }

  public void process(int y, StyledTextConsumer consumer, int startRow) {
    int x = 0;
    int nulIndex = -1;
    TerminalLineIntervalHighlighting highlighting = myCustomHighlightings.stream().findFirst().orElse(null);
    TerminalLine typeAheadLine = myTypeAheadLine;
    TextEntries textEntries = typeAheadLine != null ? typeAheadLine.myTextEntries : myTextEntries;
    for (TextEntry te : textEntries) {
      if (te.getText().isNul()) {
        if (nulIndex < 0) {
          nulIndex = x;
        }
        consumer.consumeNul(x, y, nulIndex, te.getStyle(), te.getText(), startRow);
      } else {
        if (highlighting != null && te.getLength() > 0 && highlighting.intersectsWith(x, x + te.getLength())) {
          processIntersection(x, y, te, consumer, startRow, highlighting);
        }
        else {
          consumer.consume(x, y, te.getStyle(), te.getText(), startRow);
        }
      }
      x += te.getLength();
    }
    consumer.consumeQueue(x, y, nulIndex < 0 ? x : nulIndex, startRow);
  }

  private void processIntersection(int startTextOffset, int y, @NotNull TextEntry te, @NotNull StyledTextConsumer consumer,
                                   int startRow, @NotNull TerminalLineIntervalHighlighting highlighting) {
    CharBuffer text = te.getText();
    int endTextOffset = startTextOffset + text.length();
    int[] offsets = new int[] {startTextOffset, endTextOffset, highlighting.getStartOffset(), highlighting.getEndOffset()};
    Arrays.sort(offsets);
    int startTextOffsetInd = Arrays.binarySearch(offsets, startTextOffset);
    int endTextOffsetInd = Arrays.binarySearch(offsets, endTextOffset);
    if (startTextOffsetInd < 0 || endTextOffsetInd < 0) {
      LOG.error("Cannot find " + Arrays.toString(new int[] {startTextOffset, endTextOffset})
        + " in " + Arrays.toString(offsets) + ": " + Arrays.toString(new int[] {startTextOffsetInd, endTextOffsetInd}));
      consumer.consume(startTextOffset, y, te.getStyle(), text, startRow);
      return;
    }
    for (int i = startTextOffsetInd; i < endTextOffsetInd; i++) {
      int length = offsets[i + 1] - offsets[i];
      if (length == 0) continue;
      CharBuffer subText = new SubCharBuffer(text, offsets[i] - startTextOffset, length);
      if (highlighting.intersectsWith(offsets[i], offsets[i + 1])) {
        consumer.consume(offsets[i], y, highlighting.mergeWith(te.getStyle()), subText, startRow);
      }
      else {
        consumer.consume(offsets[i], y, te.getStyle(), subText, startRow);
      }
    }
  }

  public boolean isNul() {
    for (TextEntry e : myTextEntries) {
      if (!e.isNul()) {
        return false;
      }
    }

    return true;
  }

  public boolean isEmpty() {
    for (TextEntry e : myTextEntries) {
      if (!e.isNul() && e.getLength() > 0) {
        return false;
      }
    }
    return true;
  }

  public boolean isNulOrEmpty() {
    return isNul() || isEmpty();
  }

  public void forEachEntry(@NotNull Consumer<TextEntry> action) {
    myTextEntries.forEach(action);
  }

  public @NotNull List<TextEntry> getEntries() {
    return Collections.unmodifiableList(myTextEntries.entries());
  }

  void appendEntry(@NotNull TextEntry entry) {
    int lenBefore = myTextEntries.length();
    myTextEntries.add(entry);
    fireLineChanged(lenBefore, lenBefore, entry);
  }

  @SuppressWarnings("unused") // used by IntelliJ
  public @NotNull TerminalLineIntervalHighlighting addCustomHighlighting(int startOffset, int length, @NotNull TextStyle textStyle) {
    TerminalLineIntervalHighlighting highlighting = new TerminalLineIntervalHighlighting(this, startOffset, length, textStyle) {
      @Override
      protected void doDispose() {
        myCustomHighlightings.remove(this);
      }
    };
    myCustomHighlightings.add(highlighting);
    return highlighting;
  }

  @ApiStatus.Experimental
  public void addChangesListener(@NotNull TerminalLineChangesListener listener) {
    myListeners.add(listener);
  }

  @ApiStatus.Experimental
  public void removeChangesListener(@NotNull TerminalLineChangesListener listener) {
    myListeners.remove(listener);
  }

  private void fireLineChanged(int xStart, int xEnd, @NotNull TextEntry newEntry) {
    for (TerminalLineChangesListener listener : myListeners) {
      listener.lineRangeChanged(xStart, xEnd, newEntry);
    }
  }

  @Override
  public String toString() {
    return myTextEntries.length() + " chars, " +
        (myWrapped ? "wrapped, " : "") +
        myTextEntries.myTextEntries.size() + " entries: " +
        myTextEntries.myTextEntries.stream()
          .map(entry -> entry.getText().toString())
          .collect(Collectors.joining("|"));
  }

  public static class TextEntry {
    public static final TextEntry EMPTY = new TextEntry(TextStyle.EMPTY, CharBuffer.EMPTY);

    private final TextStyle myStyle;
    private final CharBuffer myText;

    public TextEntry(@NotNull TextStyle style, @NotNull CharBuffer text) {
      myStyle = style;
      myText = text.clone();
    }

    public TextStyle getStyle() {
      return myStyle;
    }

    public CharBuffer getText() {
      return myText;
    }

    public int getLength() {
      return myText.length();
    }

    public boolean isNul() {
      return myText.isNul();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      TextEntry textEntry = (TextEntry) o;
      return Objects.equals(myStyle, textEntry.myStyle) && Objects.equals(myText, textEntry.myText);
    }

    @Override
    public String toString() {
      return myText.length() + " chars, style: " + myStyle + ", text: " + myText;
    }
  }

  private static class TextEntries implements Iterable<TextEntry> {
    private final List<TextEntry> myTextEntries = new ArrayList<>();

    private int myLength = 0;

    public void add(TextEntry entry) {
      // NUL can only be at the end of the line
      if (!entry.getText().isNul()) {
        for (TextEntry t : myTextEntries) {
          if (t.getText().isNul()) {
            t.getText().unNullify();
          }
        }
      }
      myTextEntries.add(entry);
      myLength += entry.getLength();
    }

    private List<TextEntry> entries() {
      return myTextEntries;
    }

    @NotNull
    public Iterator<TextEntry> iterator() {
      return myTextEntries.iterator();
    }

    public int length() {
      return myLength;
    }

    public void clear() {
      myTextEntries.clear();
      myLength = 0;
    }
  }
}
