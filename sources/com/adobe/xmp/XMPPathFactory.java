package com.adobe.xmp;

import com.adobe.xmp.impl.Utils;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.impl.xpath.XMPPathParser;

public final class XMPPathFactory {
    private XMPPathFactory() {
    }

    public static String composeArrayItemPath(String arrayName, int itemIndex) throws XMPException {
        StringBuilder stringBuilder;
        if (itemIndex > 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(arrayName);
            stringBuilder.append('[');
            stringBuilder.append(itemIndex);
            stringBuilder.append(']');
            return stringBuilder.toString();
        } else if (itemIndex == -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(arrayName);
            stringBuilder.append("[last()]");
            return stringBuilder.toString();
        } else {
            throw new XMPException("Array index must be larger than zero", 104);
        }
    }

    public static String composeStructFieldPath(String fieldNS, String fieldName) throws XMPException {
        assertFieldNS(fieldNS);
        assertFieldName(fieldName);
        XMPPath fieldPath = XMPPathParser.expandXPath(fieldNS, fieldName);
        if (fieldPath.size() == 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append('/');
            stringBuilder.append(fieldPath.getSegment(1).getName());
            return stringBuilder.toString();
        }
        throw new XMPException("The field name must be simple", 102);
    }

    public static String composeQualifierPath(String qualNS, String qualName) throws XMPException {
        assertQualNS(qualNS);
        assertQualName(qualName);
        XMPPath qualPath = XMPPathParser.expandXPath(qualNS, qualName);
        if (qualPath.size() == 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/?");
            stringBuilder.append(qualPath.getSegment(1).getName());
            return stringBuilder.toString();
        }
        throw new XMPException("The qualifier name must be simple", 102);
    }

    public static String composeLangSelector(String arrayName, String langName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(arrayName);
        stringBuilder.append("[?xml:lang=\"");
        stringBuilder.append(Utils.normalizeLangValue(langName));
        stringBuilder.append("\"]");
        return stringBuilder.toString();
    }

    public static String composeFieldSelector(String arrayName, String fieldNS, String fieldName, String fieldValue) throws XMPException {
        XMPPath fieldPath = XMPPathParser.expandXPath(fieldNS, fieldName);
        if (fieldPath.size() == 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(arrayName);
            stringBuilder.append('[');
            stringBuilder.append(fieldPath.getSegment(1).getName());
            stringBuilder.append("=\"");
            stringBuilder.append(fieldValue);
            stringBuilder.append("\"]");
            return stringBuilder.toString();
        }
        throw new XMPException("The fieldName name must be simple", 102);
    }

    private static void assertQualNS(String qualNS) throws XMPException {
        if (qualNS == null || qualNS.length() == 0) {
            throw new XMPException("Empty qualifier namespace URI", 101);
        }
    }

    private static void assertQualName(String qualName) throws XMPException {
        if (qualName == null || qualName.length() == 0) {
            throw new XMPException("Empty qualifier name", 102);
        }
    }

    private static void assertFieldNS(String fieldNS) throws XMPException {
        if (fieldNS == null || fieldNS.length() == 0) {
            throw new XMPException("Empty field namespace URI", 101);
        }
    }

    private static void assertFieldName(String fieldName) throws XMPException {
        if (fieldName == null || fieldName.length() == 0) {
            throw new XMPException("Empty f name", 102);
        }
    }
}
