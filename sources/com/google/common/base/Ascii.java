package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import javax.annotation.CheckReturnValue;

@GwtCompatible
public final class Ascii {
    public static final byte ACK = (byte) 6;
    public static final byte BEL = (byte) 7;
    public static final byte BS = (byte) 8;
    public static final byte CAN = (byte) 24;
    public static final byte CR = (byte) 13;
    public static final byte DC1 = (byte) 17;
    public static final byte DC2 = (byte) 18;
    public static final byte DC3 = (byte) 19;
    public static final byte DC4 = (byte) 20;
    public static final byte DEL = Byte.MAX_VALUE;
    public static final byte DLE = (byte) 16;
    public static final byte EM = (byte) 25;
    public static final byte ENQ = (byte) 5;
    public static final byte EOT = (byte) 4;
    public static final byte ESC = (byte) 27;
    public static final byte ETB = (byte) 23;
    public static final byte ETX = (byte) 3;
    public static final byte FF = (byte) 12;
    public static final byte FS = (byte) 28;
    public static final byte GS = (byte) 29;
    public static final byte HT = (byte) 9;
    public static final byte LF = (byte) 10;
    public static final char MAX = '';
    public static final char MIN = '\u0000';
    public static final byte NAK = (byte) 21;
    public static final byte NL = (byte) 10;
    public static final byte NUL = (byte) 0;
    public static final byte RS = (byte) 30;
    public static final byte SI = (byte) 15;
    public static final byte SO = (byte) 14;
    public static final byte SOH = (byte) 1;
    public static final byte SP = (byte) 32;
    public static final byte SPACE = (byte) 32;
    public static final byte STX = (byte) 2;
    public static final byte SUB = (byte) 26;
    public static final byte SYN = (byte) 22;
    public static final byte US = (byte) 31;
    public static final byte VT = (byte) 11;
    public static final byte XOFF = (byte) 19;
    public static final byte XON = (byte) 17;

    private Ascii() {
    }

    public static String toLowerCase(String string) {
        int length = string.length();
        int i = 0;
        while (i < length) {
            if (isUpperCase(string.charAt(i))) {
                char[] chars = string.toCharArray();
                while (i < length) {
                    char c = chars[i];
                    if (isUpperCase(c)) {
                        chars[i] = (char) (c ^ 32);
                    }
                    i++;
                }
                return String.valueOf(chars);
            }
            i++;
        }
        return string;
    }

    public static String toLowerCase(CharSequence chars) {
        if (chars instanceof String) {
            return toLowerCase((String) chars);
        }
        int length = chars.length();
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(toLowerCase(chars.charAt(i)));
        }
        return builder.toString();
    }

    public static char toLowerCase(char c) {
        return isUpperCase(c) ? (char) (c ^ 32) : c;
    }

    public static String toUpperCase(String string) {
        int length = string.length();
        int i = 0;
        while (i < length) {
            if (isLowerCase(string.charAt(i))) {
                char[] chars = string.toCharArray();
                while (i < length) {
                    char c = chars[i];
                    if (isLowerCase(c)) {
                        chars[i] = (char) (c & 95);
                    }
                    i++;
                }
                return String.valueOf(chars);
            }
            i++;
        }
        return string;
    }

    public static String toUpperCase(CharSequence chars) {
        if (chars instanceof String) {
            return toUpperCase((String) chars);
        }
        int length = chars.length();
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(toUpperCase(chars.charAt(i)));
        }
        return builder.toString();
    }

    public static char toUpperCase(char c) {
        return isLowerCase(c) ? (char) (c & 95) : c;
    }

    public static boolean isLowerCase(char c) {
        return c >= 'a' && c <= 'z';
    }

    public static boolean isUpperCase(char c) {
        return c >= 'A' && c <= 'Z';
    }

    @CheckReturnValue
    @Beta
    public static String truncate(CharSequence seq, int maxLength, String truncationIndicator) {
        Preconditions.checkNotNull(seq);
        int truncationLength = maxLength - truncationIndicator.length();
        Preconditions.checkArgument(truncationLength >= 0, "maxLength (%s) must be >= length of the truncation indicator (%s)", Integer.valueOf(maxLength), Integer.valueOf(truncationIndicator.length()));
        if (seq.length() <= maxLength) {
            String string = seq.toString();
            if (string.length() <= maxLength) {
                return string;
            }
            seq = string;
        }
        StringBuilder stringBuilder = new StringBuilder(maxLength);
        stringBuilder.append(seq, 0, truncationLength);
        stringBuilder.append(truncationIndicator);
        return stringBuilder.toString();
    }

    @Beta
    public static boolean equalsIgnoreCase(CharSequence s1, CharSequence s2) {
        int length = s1.length();
        if (s1 == s2) {
            return true;
        }
        if (length != s2.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (c1 != c2) {
                int alphaIndex = getAlphaIndex(c1);
                if (alphaIndex >= 26 || alphaIndex != getAlphaIndex(c2)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int getAlphaIndex(char c) {
        return (char) ((c | 32) - 97);
    }
}
