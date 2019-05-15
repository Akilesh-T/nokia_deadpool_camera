package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

@GwtCompatible
@Beta
public final class Utf8 {
    public static int encodedLength(CharSequence sequence) {
        int utf16Length = sequence.length();
        int utf8Length = utf16Length;
        int i = 0;
        while (i < utf16Length && sequence.charAt(i) < 128) {
            i++;
        }
        while (i < utf16Length) {
            char c = sequence.charAt(i);
            if (c >= 2048) {
                utf8Length += encodedLengthGeneral(sequence, i);
                break;
            }
            utf8Length += (127 - c) >>> 31;
            i++;
        }
        if (utf8Length >= utf16Length) {
            return utf8Length;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UTF-8 length does not fit in int: ");
        stringBuilder.append(((long) utf8Length) + 4294967296L);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static int encodedLengthGeneral(CharSequence sequence, int start) {
        int utf16Length = sequence.length();
        int utf8Length = 0;
        int i = start;
        while (i < utf16Length) {
            char c = sequence.charAt(i);
            if (c < 2048) {
                utf8Length += (127 - c) >>> 31;
            } else {
                utf8Length += 2;
                if (55296 <= c && c <= 57343) {
                    if (Character.codePointAt(sequence, i) >= 65536) {
                        i++;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unpaired surrogate at index ");
                        stringBuilder.append(i);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
            }
            i++;
        }
        return utf8Length;
    }

    public static boolean isWellFormed(byte[] bytes) {
        return isWellFormed(bytes, 0, bytes.length);
    }

    public static boolean isWellFormed(byte[] bytes, int off, int len) {
        int end = off + len;
        Preconditions.checkPositionIndexes(off, end, bytes.length);
        for (int i = off; i < end; i++) {
            if (bytes[i] < (byte) 0) {
                return isWellFormedSlowPath(bytes, i, end);
            }
        }
        return true;
    }

    private static boolean isWellFormedSlowPath(byte[] bytes, int off, int end) {
        int index = off;
        while (index < end) {
            int index2 = index + 1;
            byte b = bytes[index];
            byte byte1 = b;
            int index3;
            if (b >= (byte) 0) {
                index = index2;
            } else if (byte1 < (byte) -32) {
                if (index2 == end) {
                    return false;
                }
                if (byte1 >= (byte) -62) {
                    index = index2 + 1;
                    if (bytes[index2] > -65) {
                    }
                }
                return false;
            } else if (byte1 < (byte) -16) {
                if (index2 + 1 >= end) {
                    return false;
                }
                index3 = index2 + 1;
                index2 = bytes[index2];
                if (index2 > -65 || ((byte1 == (byte) -32 && index2 < -96) || (byte1 == (byte) -19 && -96 <= index2))) {
                } else {
                    index = index3 + 1;
                    if (bytes[index3] > -65) {
                    }
                }
                return false;
            } else if (index2 + 2 >= end) {
                return false;
            } else {
                index = index2 + 1;
                index2 = bytes[index2];
                if (index2 <= -65 && (((byte1 << 28) + (index2 + 112)) >> 30) == 0) {
                    index3 = index + 1;
                    if (bytes[index] <= -65) {
                        index = index3 + 1;
                        if (bytes[index3] > -65) {
                        }
                    }
                }
                return false;
            }
        }
        return true;
    }

    private Utf8() {
    }
}
