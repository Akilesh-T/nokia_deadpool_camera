package com.adobe.xmp.impl;

public class Base64 {
    private static final byte EQUAL = (byte) -3;
    private static final byte INVALID = (byte) -1;
    private static final byte WHITESPACE = (byte) -2;
    private static byte[] ascii = new byte[255];
    private static byte[] base64 = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 43, (byte) 47};

    static {
        int idx = 0;
        for (int idx2 = 0; idx2 < 255; idx2++) {
            ascii[idx2] = (byte) -1;
        }
        while (true) {
            int idx3 = idx;
            if (idx3 < base64.length) {
                ascii[base64[idx3]] = (byte) idx3;
                idx = idx3 + 1;
            } else {
                ascii[9] = WHITESPACE;
                ascii[10] = WHITESPACE;
                ascii[13] = WHITESPACE;
                ascii[32] = WHITESPACE;
                ascii[61] = EQUAL;
                return;
            }
        }
    }

    public static final byte[] encode(byte[] src) {
        return encode(src, 0);
    }

    public static final byte[] encode(byte[] src, int lineFeed) {
        int sidx;
        int lineFeed2 = (lineFeed / 4) * 4;
        if (lineFeed2 < 0) {
            lineFeed2 = 0;
        }
        lineFeed = ((src.length + 2) / 3) * 4;
        if (lineFeed2 > 0) {
            lineFeed += (lineFeed - 1) / lineFeed2;
        }
        byte[] dst = new byte[lineFeed];
        int sidx2 = 0;
        int didx = 0;
        int lf = 0;
        while (sidx2 + 3 <= src.length) {
            int sidx3 = sidx2 + 1;
            sidx = sidx3 + 1;
            sidx2 = ((src[sidx2] & 255) << 16) | ((src[sidx3] & 255) << 8);
            sidx3 = sidx + 1;
            sidx2 |= (src[sidx] & 255) << 0;
            int didx2 = didx + 1;
            dst[didx] = base64[(sidx2 & 16515072) >> 18];
            sidx = didx2 + 1;
            dst[didx2] = base64[(sidx2 & 258048) >> 12];
            int didx3 = sidx + 1;
            dst[sidx] = base64[(sidx2 & 4032) >> 6];
            sidx = didx3 + 1;
            dst[didx3] = base64[sidx2 & 63];
            lf += 4;
            if (sidx >= lineFeed || lineFeed2 <= 0 || lf % lineFeed2 != 0) {
                sidx2 = sidx3;
                didx = sidx;
            } else {
                didx3 = sidx + 1;
                dst[sidx] = (byte) 10;
                sidx2 = sidx3;
                didx = didx3;
            }
        }
        int bits24;
        if (src.length - sidx2 == 2) {
            bits24 = ((src[sidx2] & 255) << 16) | ((src[sidx2 + 1] & 255) << 8);
            sidx = didx + 1;
            dst[didx] = base64[(bits24 & 16515072) >> 18];
            didx = sidx + 1;
            dst[sidx] = base64[(bits24 & 258048) >> 12];
            sidx = didx + 1;
            dst[didx] = base64[(bits24 & 4032) >> 6];
            didx = sidx + 1;
            dst[sidx] = (byte) 61;
        } else if (src.length - sidx2 == 1) {
            bits24 = (src[sidx2] & 255) << 16;
            sidx = didx + 1;
            dst[didx] = base64[(bits24 & 16515072) >> 18];
            didx = sidx + 1;
            dst[sidx] = base64[(bits24 & 258048) >> 12];
            sidx = didx + 1;
            dst[didx] = (byte) 61;
            didx = sidx + 1;
            dst[sidx] = (byte) 61;
        }
        return dst;
    }

    public static final String encode(String src) {
        return new String(encode(src.getBytes()));
    }

    public static final byte[] decode(byte[] src) throws IllegalArgumentException {
        int didx = 0;
        int srcLen = 0;
        for (byte b : src) {
            byte val = ascii[b];
            if (val >= (byte) 0) {
                int srcLen2 = srcLen + 1;
                src[srcLen] = val;
                srcLen = srcLen2;
            } else if (val == (byte) -1) {
                throw new IllegalArgumentException("Invalid base 64 string");
            }
        }
        while (srcLen > 0 && src[srcLen - 1] == EQUAL) {
            srcLen--;
        }
        byte[] dst = new byte[((srcLen * 3) / 4)];
        int sidx = 0;
        while (didx < dst.length - 2) {
            dst[didx] = (byte) (((src[sidx] << 2) & 255) | ((src[sidx + 1] >>> 4) & 3));
            dst[didx + 1] = (byte) (((src[sidx + 1] << 4) & 255) | ((src[sidx + 2] >>> 2) & 15));
            dst[didx + 2] = (byte) (((src[sidx + 2] << 6) & 255) | (src[sidx + 3] & 63));
            sidx += 4;
            didx += 3;
        }
        if (didx < dst.length) {
            dst[didx] = (byte) (((src[sidx] << 2) & 255) | ((src[sidx + 1] >>> 4) & 3));
        }
        didx++;
        if (didx < dst.length) {
            dst[didx] = (byte) (((src[sidx + 1] << 4) & 255) | ((src[sidx + 2] >>> 2) & 15));
        }
        return dst;
    }

    public static final String decode(String src) {
        return new String(decode(src.getBytes()));
    }
}
