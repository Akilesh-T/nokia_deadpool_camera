package com.adobe.xmp.impl;

import com.bumptech.glide.load.Key;
import java.io.UnsupportedEncodingException;

public class Latin1Converter {
    private static final int STATE_START = 0;
    private static final int STATE_UTF8CHAR = 11;

    private Latin1Converter() {
    }

    public static ByteBuffer convert(ByteBuffer buffer) {
        if (!Key.STRING_CHARSET_NAME.equals(buffer.getEncoding())) {
            return buffer;
        }
        byte[] readAheadBuffer = new byte[8];
        int expectedBytes = 0;
        ByteBuffer out = new ByteBuffer((buffer.length() * 4) / 3);
        int state = 0;
        int j = 0;
        int readAhead = 0;
        int i = 0;
        while (i < buffer.length()) {
            int b = buffer.charAt(i);
            int expectedBytes2;
            if (state != 11) {
                if (b < 127) {
                    out.append((byte) b);
                } else if (b >= 192) {
                    expectedBytes2 = -1;
                    expectedBytes = b;
                    while (expectedBytes2 < 8 && (expectedBytes & 128) == 128) {
                        expectedBytes2++;
                        expectedBytes <<= 1;
                    }
                    int readAhead2 = readAhead + 1;
                    readAheadBuffer[readAhead] = (byte) b;
                    state = 11;
                    expectedBytes = expectedBytes2;
                    readAhead = readAhead2;
                } else {
                    out.append(convertToUTF8((byte) b));
                }
            } else if (expectedBytes <= 0 || (b & 192) != 128) {
                out.append(convertToUTF8(readAheadBuffer[0]));
                i -= readAhead;
                readAhead = 0;
                state = 0;
            } else {
                expectedBytes2 = readAhead + 1;
                readAheadBuffer[readAhead] = (byte) b;
                expectedBytes--;
                if (expectedBytes == 0) {
                    out.append(readAheadBuffer, 0, expectedBytes2);
                    readAhead = 0;
                    state = 0;
                } else {
                    readAhead = expectedBytes2;
                }
            }
            i++;
        }
        if (state == 11) {
            while (true) {
                int j2 = j;
                if (j2 >= readAhead) {
                    break;
                }
                out.append(convertToUTF8(readAheadBuffer[j2]));
                j = j2 + 1;
            }
        }
        return out;
    }

    private static byte[] convertToUTF8(byte ch) {
        int c = ch & 255;
        if (c >= 128) {
            if (c == Const.CODE_C1_CW1 || c == Const.CODE_C1_DLY || c == 143 || c == Const.CODE_C1_SPA || c == Const.CODE_C1_DF5) {
                return new byte[]{(byte) 32};
            }
            try {
                return new String(new byte[]{ch}, "cp1252").getBytes(Key.STRING_CHARSET_NAME);
            } catch (UnsupportedEncodingException e) {
            }
        }
        return new byte[]{ch};
    }
}
