package com.google.common.escape;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

@GwtCompatible
@Beta
public abstract class UnicodeEscaper extends Escaper {
    private static final int DEST_PAD = 32;

    public abstract char[] escape(int i);

    protected UnicodeEscaper() {
    }

    /* Access modifiers changed, original: protected */
    public int nextEscapeIndex(CharSequence csq, int start, int end) {
        int index = start;
        while (index < end) {
            int cp = codePointAt(csq, index, end);
            if (cp < 0 || escape(cp) != null) {
                break;
            }
            index += Character.isSupplementaryCodePoint(cp) ? 2 : 1;
        }
        return index;
    }

    public String escape(String string) {
        Preconditions.checkNotNull(string);
        int end = string.length();
        int index = nextEscapeIndex(string, 0, end);
        return index == end ? string : escapeSlow(string, index);
    }

    /* Access modifiers changed, original: protected|final */
    public final String escapeSlow(String s, int index) {
        int cp;
        int end = s.length();
        int destIndex = 0;
        char[] dest = Platform.charBufferFromThreadLocal();
        int index2 = index;
        index = 0;
        while (index2 < end) {
            cp = codePointAt(s, index2, end);
            if (cp >= 0) {
                char[] escaped = escape(cp);
                int nextIndex = (Character.isSupplementaryCodePoint(cp) ? 2 : 1) + index2;
                if (escaped != null) {
                    int charsSkipped = index2 - index;
                    int sizeNeeded = (destIndex + charsSkipped) + escaped.length;
                    if (dest.length < sizeNeeded) {
                        dest = growBuffer(dest, destIndex, ((end - index2) + sizeNeeded) + 32);
                    }
                    if (charsSkipped > 0) {
                        s.getChars(index, index2, dest, destIndex);
                        destIndex += charsSkipped;
                    }
                    if (escaped.length > 0) {
                        System.arraycopy(escaped, 0, dest, destIndex, escaped.length);
                        destIndex += escaped.length;
                    }
                    index = nextIndex;
                }
                index2 = nextEscapeIndex(s, nextIndex, end);
            } else {
                throw new IllegalArgumentException("Trailing high surrogate at end of input");
            }
        }
        cp = end - index;
        if (cp > 0) {
            int endIndex = destIndex + cp;
            if (dest.length < endIndex) {
                dest = growBuffer(dest, destIndex, endIndex);
            }
            s.getChars(index, end, dest, destIndex);
            destIndex = endIndex;
        }
        return new String(dest, 0, destIndex);
    }

    protected static int codePointAt(CharSequence seq, int index, int end) {
        Preconditions.checkNotNull(seq);
        if (index < end) {
            int index2 = index + 1;
            index = seq.charAt(index);
            if (index < 55296 || index > 57343) {
                return index;
            }
            if (index > 56319) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected low surrogate character '");
                stringBuilder.append(index);
                stringBuilder.append("' with value ");
                stringBuilder.append(index);
                stringBuilder.append(" at index ");
                stringBuilder.append(index2 - 1);
                stringBuilder.append(" in '");
                stringBuilder.append(seq);
                stringBuilder.append("'");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (index2 == end) {
                return -index;
            } else {
                char c2 = seq.charAt(index2);
                if (Character.isLowSurrogate(c2)) {
                    return Character.toCodePoint(index, c2);
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Expected low surrogate but got char '");
                stringBuilder2.append(c2);
                stringBuilder2.append("' with value ");
                stringBuilder2.append(c2);
                stringBuilder2.append(" at index ");
                stringBuilder2.append(index2);
                stringBuilder2.append(" in '");
                stringBuilder2.append(seq);
                stringBuilder2.append("'");
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }
        throw new IndexOutOfBoundsException("Index exceeds specified range");
    }

    private static char[] growBuffer(char[] dest, int index, int size) {
        char[] copy = new char[size];
        if (index > 0) {
            System.arraycopy(dest, 0, copy, 0, index);
        }
        return copy;
    }
}
