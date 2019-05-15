package com.google.common.escape;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

@GwtCompatible
@Beta
public abstract class CharEscaper extends Escaper {
    private static final int DEST_PAD_MULTIPLIER = 2;

    public abstract char[] escape(char c);

    protected CharEscaper() {
    }

    public String escape(String string) {
        Preconditions.checkNotNull(string);
        int length = string.length();
        for (int index = 0; index < length; index++) {
            if (escape(string.charAt(index)) != null) {
                return escapeSlow(string, index);
            }
        }
        return string;
    }

    /* Access modifiers changed, original: protected|final */
    public final String escapeSlow(String s, int index) {
        int rlen;
        int slen = s.length();
        char[] dest = Platform.charBufferFromThreadLocal();
        int destSize = dest.length;
        int destIndex = 0;
        char[] dest2 = dest;
        index = 0;
        for (int index2 = index; index2 < slen; index2++) {
            char[] r = escape(s.charAt(index2));
            if (r != null) {
                rlen = r.length;
                int charsSkipped = index2 - index;
                int sizeNeeded = (destIndex + charsSkipped) + rlen;
                if (destSize < sizeNeeded) {
                    destSize = sizeNeeded + (2 * (slen - index2));
                    dest2 = growBuffer(dest2, destIndex, destSize);
                }
                if (charsSkipped > 0) {
                    s.getChars(index, index2, dest2, destIndex);
                    destIndex += charsSkipped;
                }
                if (rlen > 0) {
                    System.arraycopy(r, 0, dest2, destIndex, rlen);
                    destIndex += rlen;
                }
                index = index2 + 1;
            }
        }
        int charsLeft = slen - index;
        if (charsLeft > 0) {
            rlen = destIndex + charsLeft;
            if (destSize < rlen) {
                dest2 = growBuffer(dest2, destIndex, rlen);
            }
            s.getChars(index, slen, dest2, destIndex);
            destIndex = rlen;
        }
        return new String(dest2, 0, destIndex);
    }

    private static char[] growBuffer(char[] dest, int index, int size) {
        char[] copy = new char[size];
        if (index > 0) {
            System.arraycopy(dest, 0, copy, 0, index);
        }
        return copy;
    }
}
