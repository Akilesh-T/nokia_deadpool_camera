package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.google.common.base.Ascii;

public class Utils implements XMPConst {
    public static final int UUID_LENGTH = 36;
    public static final int UUID_SEGMENT_COUNT = 4;
    private static boolean[] xmlNameChars;
    private static boolean[] xmlNameStartChars;

    static {
        initCharTables();
    }

    private Utils() {
    }

    public static String normalizeLangValue(String value) {
        if (XMPConst.X_DEFAULT.equals(value)) {
            return value;
        }
        int subTag = 1;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            char charAt = value.charAt(i);
            if (charAt != ' ') {
                if (charAt == '-' || charAt == '_') {
                    buffer.append('-');
                    subTag++;
                } else if (subTag != 2) {
                    buffer.append(Character.toLowerCase(value.charAt(i)));
                } else {
                    buffer.append(Character.toUpperCase(value.charAt(i)));
                }
            }
        }
        return buffer.toString();
    }

    static String[] splitNameAndValue(String selector) {
        int eq = selector.indexOf(61);
        int pos = 1;
        if (selector.charAt(1) == '?') {
            pos = 1 + 1;
        }
        String name = selector.substring(pos, eq);
        pos = eq + 1;
        char quote = selector.charAt(pos);
        pos++;
        int end = selector.length() - 2;
        StringBuffer value = new StringBuffer(end - eq);
        while (pos < end) {
            value.append(selector.charAt(pos));
            pos++;
            if (selector.charAt(pos) == quote) {
                pos++;
            }
        }
        return new String[]{name, value.toString()};
    }

    static boolean isInternalProperty(String schema, String prop) {
        if (XMPConst.NS_DC.equals(schema)) {
            if ("dc:format".equals(prop) || "dc:language".equals(prop)) {
                return true;
            }
            return false;
        } else if (XMPConst.NS_XMP.equals(schema)) {
            if ("xmp:BaseURL".equals(prop) || "xmp:CreatorTool".equals(prop) || "xmp:Format".equals(prop) || "xmp:Locale".equals(prop) || "xmp:MetadataDate".equals(prop) || "xmp:ModifyDate".equals(prop)) {
                return true;
            }
            return false;
        } else if (XMPConst.NS_PDF.equals(schema)) {
            if ("pdf:BaseURL".equals(prop) || "pdf:Creator".equals(prop) || "pdf:ModDate".equals(prop) || "pdf:PDFVersion".equals(prop) || "pdf:Producer".equals(prop)) {
                return true;
            }
            return false;
        } else if (XMPConst.NS_TIFF.equals(schema)) {
            if ("tiff:ImageDescription".equals(prop) || "tiff:Artist".equals(prop) || "tiff:Copyright".equals(prop)) {
                return false;
            }
            return true;
        } else if (XMPConst.NS_EXIF.equals(schema)) {
            if ("exif:UserComment".equals(prop)) {
                return false;
            }
            return true;
        } else if (XMPConst.NS_EXIF_AUX.equals(schema)) {
            return true;
        } else {
            if (XMPConst.NS_PHOTOSHOP.equals(schema)) {
                if ("photoshop:ICCProfile".equals(prop)) {
                    return true;
                }
                return false;
            } else if (XMPConst.NS_CAMERARAW.equals(schema)) {
                if ("crs:Version".equals(prop) || "crs:RawFileName".equals(prop) || "crs:ToneCurveName".equals(prop)) {
                    return true;
                }
                return false;
            } else if (XMPConst.NS_ADOBESTOCKPHOTO.equals(schema)) {
                return true;
            } else {
                if (XMPConst.NS_XMP_MM.equals(schema)) {
                    return true;
                }
                if (XMPConst.TYPE_TEXT.equals(schema)) {
                    return true;
                }
                if (XMPConst.TYPE_PAGEDFILE.equals(schema)) {
                    return true;
                }
                if (XMPConst.TYPE_GRAPHICS.equals(schema)) {
                    return true;
                }
                if (XMPConst.TYPE_IMAGE.equals(schema)) {
                    return true;
                }
                if (XMPConst.TYPE_FONT.equals(schema)) {
                    return true;
                }
                return false;
            }
        }
    }

    static boolean checkUUIDFormat(String uuid) {
        boolean result = true;
        int delimCnt = 0;
        boolean z = false;
        if (uuid == null) {
            return false;
        }
        int delimPos = 0;
        while (true) {
            boolean z2 = true;
            if (delimPos >= uuid.length()) {
                break;
            }
            if (uuid.charAt(delimPos) == '-') {
                delimCnt++;
                if (!(result && (delimPos == 8 || delimPos == 13 || delimPos == 18 || delimPos == 23))) {
                    z2 = false;
                }
                result = z2;
            }
            delimPos++;
        }
        if (result && 4 == delimCnt && 36 == delimPos) {
            z = true;
        }
        return z;
    }

    public static boolean isXMLName(String name) {
        if (name.length() > 0 && !isNameStartChar(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!isNameChar(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isXMLNameNS(String name) {
        if (name.length() > 0 && (!isNameStartChar(name.charAt(0)) || name.charAt(0) == ':')) {
            return false;
        }
        int i = 1;
        while (i < name.length()) {
            if (!isNameChar(name.charAt(i)) || name.charAt(i) == ':') {
                return false;
            }
            i++;
        }
        return true;
    }

    static boolean isControlChar(char c) {
        return ((c > 31 && c != Ascii.MAX) || c == 9 || c == 10 || c == 13) ? false : true;
    }

    public static String escapeXML(String value, boolean forAttribute, boolean escapeWhitespaces) {
        char c;
        boolean needsEscaping = false;
        int i = 0;
        for (int i2 = 0; i2 < value.length(); i2++) {
            c = value.charAt(i2);
            if (c == '<' || c == '>' || c == '&' || ((escapeWhitespaces && (c == 9 || c == 10 || c == 13)) || (forAttribute && c == '\"'))) {
                needsEscaping = true;
                break;
            }
        }
        if (!needsEscaping) {
            return value;
        }
        StringBuffer buffer = new StringBuffer((value.length() * 4) / 3);
        while (i < value.length()) {
            c = value.charAt(i);
            if (escapeWhitespaces && (c == 9 || c == 10 || c == 13)) {
                buffer.append("&#x");
                buffer.append(Integer.toHexString(c).toUpperCase());
                buffer.append(';');
            } else if (c == '\"') {
                buffer.append(forAttribute ? "&quot;" : "\"");
            } else if (c == '&') {
                buffer.append("&amp;");
            } else if (c == '<') {
                buffer.append("&lt;");
            } else if (c != '>') {
                buffer.append(c);
            } else {
                buffer.append("&gt;");
            }
            i++;
        }
        return buffer.toString();
    }

    static String removeControlChars(String value) {
        StringBuffer buffer = new StringBuffer(value);
        for (int i = 0; i < buffer.length(); i++) {
            if (isControlChar(buffer.charAt(i))) {
                buffer.setCharAt(i, ' ');
            }
        }
        return buffer.toString();
    }

    private static boolean isNameStartChar(char ch) {
        return ch > 255 || xmlNameStartChars[ch];
    }

    private static boolean isNameChar(char ch) {
        return ch > 255 || xmlNameChars[ch];
    }

    private static void initCharTables() {
        xmlNameChars = new boolean[256];
        xmlNameStartChars = new boolean[256];
        char ch = 0;
        while (ch < xmlNameChars.length) {
            boolean[] zArr = xmlNameStartChars;
            boolean z = true;
            boolean z2 = ('a' <= ch && ch <= 'z') || (('A' <= ch && ch <= 'Z') || ch == ':' || ch == '_' || ((192 <= ch && ch <= 214) || (216 <= ch && ch <= 246)));
            zArr[ch] = z2;
            zArr = xmlNameChars;
            if (('a' > ch || ch > 'z') && (('A' > ch || ch > 'Z') && !(('0' <= ch && ch <= '9') || ch == ':' || ch == '_' || ch == '-' || ch == '.' || ch == 183 || ((192 <= ch && ch <= 214) || (216 <= ch && ch <= 246))))) {
                z = false;
            }
            zArr[ch] = z;
            ch = (char) (ch + 1);
        }
    }
}
