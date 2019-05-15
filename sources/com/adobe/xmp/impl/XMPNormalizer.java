package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.xpath.XMPPathParser;
import com.adobe.xmp.options.ParseOptions;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.properties.XMPAliasInfo;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XMPNormalizer {
    private static Map dcArrayForms;

    static {
        initDCArrays();
    }

    private XMPNormalizer() {
    }

    static XMPMeta process(XMPMetaImpl xmp, ParseOptions options) throws XMPException {
        XMPNode tree = xmp.getRoot();
        touchUpDataModel(xmp);
        moveExplicitAliases(tree, options);
        tweakOldXMP(tree);
        deleteEmptySchemas(tree);
        return xmp;
    }

    private static void tweakOldXMP(XMPNode tree) throws XMPException {
        if (tree.getName() != null && tree.getName().length() >= 36) {
            String nameStr = tree.getName().toLowerCase();
            if (nameStr.startsWith("uuid:")) {
                nameStr = nameStr.substring(5);
            }
            if (Utils.checkUUIDFormat(nameStr)) {
                XMPNode idNode = XMPNodeUtils.findNode(tree, XMPPathParser.expandXPath(XMPConst.NS_XMP_MM, "InstanceID"), true, null);
                if (idNode != null) {
                    idNode.setOptions(null);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("uuid:");
                    stringBuilder.append(nameStr);
                    idNode.setValue(stringBuilder.toString());
                    idNode.removeChildren();
                    idNode.removeQualifiers();
                    tree.setName(null);
                    return;
                }
                throw new XMPException("Failure creating xmpMM:InstanceID", 9);
            }
        }
    }

    private static void touchUpDataModel(XMPMetaImpl xmp) throws XMPException {
        XMPNodeUtils.findSchemaNode(xmp.getRoot(), XMPConst.NS_DC, true);
        Iterator it = xmp.getRoot().iterateChildren();
        while (it.hasNext()) {
            XMPNode currSchema = (XMPNode) it.next();
            XMPNode arrayNode;
            if (XMPConst.NS_DC.equals(currSchema.getName())) {
                normalizeDCArrays(currSchema);
            } else if (XMPConst.NS_EXIF.equals(currSchema.getName())) {
                fixGPSTimeStamp(currSchema);
                arrayNode = XMPNodeUtils.findChildNode(currSchema, "exif:UserComment", false);
                if (arrayNode != null) {
                    repairAltText(arrayNode);
                }
            } else if (XMPConst.NS_DM.equals(currSchema.getName())) {
                arrayNode = XMPNodeUtils.findChildNode(currSchema, "xmpDM:copyright", false);
                if (arrayNode != null) {
                    migrateAudioCopyright(xmp, arrayNode);
                }
            } else if (XMPConst.NS_XMP_RIGHTS.equals(currSchema.getName())) {
                arrayNode = XMPNodeUtils.findChildNode(currSchema, "xmpRights:UsageTerms", false);
                if (arrayNode != null) {
                    repairAltText(arrayNode);
                }
            }
        }
    }

    private static void normalizeDCArrays(XMPNode dcSchema) throws XMPException {
        for (int i = 1; i <= dcSchema.getChildrenLength(); i++) {
            XMPNode currProp = dcSchema.getChild(i);
            PropertyOptions arrayForm = (PropertyOptions) dcArrayForms.get(currProp.getName());
            if (arrayForm != null) {
                if (currProp.getOptions().isSimple()) {
                    XMPNode newArray = new XMPNode(currProp.getName(), arrayForm);
                    currProp.setName(XMPConst.ARRAY_ITEM_NAME);
                    newArray.addChild(currProp);
                    dcSchema.replaceChild(i, newArray);
                    if (arrayForm.isArrayAltText() && !currProp.getOptions().getHasLanguage()) {
                        currProp.addQualifier(new XMPNode(XMPConst.XML_LANG, XMPConst.X_DEFAULT, null));
                    }
                } else {
                    currProp.getOptions().setOption(7680, false);
                    currProp.getOptions().mergeWith(arrayForm);
                    if (arrayForm.isArrayAltText()) {
                        repairAltText(currProp);
                    }
                }
            }
        }
    }

    private static void repairAltText(XMPNode arrayNode) throws XMPException {
        if (arrayNode != null && arrayNode.getOptions().isArray()) {
            arrayNode.getOptions().setArrayOrdered(true).setArrayAlternate(true).setArrayAltText(true);
            Iterator it = arrayNode.iterateChildren();
            while (it.hasNext()) {
                XMPNode currChild = (XMPNode) it.next();
                if (currChild.getOptions().isCompositeProperty()) {
                    it.remove();
                } else if (!currChild.getOptions().getHasLanguage()) {
                    String childValue = currChild.getValue();
                    if (childValue == null || childValue.length() == 0) {
                        it.remove();
                    } else {
                        currChild.addQualifier(new XMPNode(XMPConst.XML_LANG, "x-repair", null));
                    }
                }
            }
        }
    }

    private static void moveExplicitAliases(XMPNode tree, ParseOptions options) throws XMPException {
        if (tree.getHasAliases()) {
            tree.setHasAliases(false);
            boolean strictAliasing = options.getStrictAliasing();
            for (XMPNode currSchema : tree.getUnmodifiableChildren()) {
                if (currSchema.getHasAliases()) {
                    Iterator propertyIt = currSchema.iterateChildren();
                    while (propertyIt.hasNext()) {
                        XMPNode currProp = (XMPNode) propertyIt.next();
                        if (currProp.isAlias()) {
                            currProp.setAlias(false);
                            XMPAliasInfo info = XMPMetaFactory.getSchemaRegistry().findAlias(currProp.getName());
                            if (info != null) {
                                XMPNode baseSchema = XMPNodeUtils.findSchemaNode(tree, info.getNamespace(), null, true);
                                baseSchema.setImplicit(false);
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(info.getPrefix());
                                stringBuilder.append(info.getPropName());
                                XMPNode baseNode = XMPNodeUtils.findChildNode(baseSchema, stringBuilder.toString(), false);
                                if (baseNode == null) {
                                    if (info.getAliasForm().isSimple()) {
                                        String qname = new StringBuilder();
                                        qname.append(info.getPrefix());
                                        qname.append(info.getPropName());
                                        currProp.setName(qname.toString());
                                        baseSchema.addChild(currProp);
                                        propertyIt.remove();
                                    } else {
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append(info.getPrefix());
                                        stringBuilder2.append(info.getPropName());
                                        baseNode = new XMPNode(stringBuilder2.toString(), info.getAliasForm().toPropertyOptions());
                                        baseSchema.addChild(baseNode);
                                        transplantArrayItemAlias(propertyIt, currProp, baseNode);
                                    }
                                } else if (info.getAliasForm().isSimple()) {
                                    if (strictAliasing) {
                                        compareAliasedSubtrees(currProp, baseNode, true);
                                    }
                                    propertyIt.remove();
                                } else {
                                    XMPNode itemNode = null;
                                    if (info.getAliasForm().isArrayAltText()) {
                                        int xdIndex = XMPNodeUtils.lookupLanguageItem(baseNode, XMPConst.X_DEFAULT);
                                        if (xdIndex != -1) {
                                            itemNode = baseNode.getChild(xdIndex);
                                        }
                                    } else if (baseNode.hasChildren()) {
                                        itemNode = baseNode.getChild(1);
                                    }
                                    if (itemNode == null) {
                                        transplantArrayItemAlias(propertyIt, currProp, baseNode);
                                    } else {
                                        if (strictAliasing) {
                                            compareAliasedSubtrees(currProp, itemNode, true);
                                        }
                                        propertyIt.remove();
                                    }
                                }
                            }
                        }
                    }
                    currSchema.setHasAliases(false);
                }
            }
        }
    }

    private static void transplantArrayItemAlias(Iterator propertyIt, XMPNode childNode, XMPNode baseArray) throws XMPException {
        if (baseArray.getOptions().isArrayAltText()) {
            if (childNode.getOptions().getHasLanguage()) {
                throw new XMPException("Alias to x-default already has a language qualifier", 203);
            }
            childNode.addQualifier(new XMPNode(XMPConst.XML_LANG, XMPConst.X_DEFAULT, null));
        }
        propertyIt.remove();
        childNode.setName(XMPConst.ARRAY_ITEM_NAME);
        baseArray.addChild(childNode);
    }

    private static void fixGPSTimeStamp(XMPNode exifSchema) throws XMPException {
        XMPNode gpsDateTime = XMPNodeUtils.findChildNode(exifSchema, "exif:GPSTimeStamp", false);
        if (gpsDateTime != null) {
            try {
                XMPDateTime binGPSStamp = XMPUtils.convertToDate(gpsDateTime.getValue());
                if (binGPSStamp.getYear() == 0 && binGPSStamp.getMonth() == 0) {
                    if (binGPSStamp.getDay() == 0) {
                        XMPNode otherDate = XMPNodeUtils.findChildNode(exifSchema, "exif:DateTimeOriginal", false);
                        if (otherDate == null) {
                            otherDate = XMPNodeUtils.findChildNode(exifSchema, "exif:DateTimeDigitized", false);
                        }
                        XMPDateTime binOtherDate = XMPUtils.convertToDate(otherDate.getValue());
                        Calendar cal = binGPSStamp.getCalendar();
                        cal.set(1, binOtherDate.getYear());
                        cal.set(2, binOtherDate.getMonth());
                        cal.set(5, binOtherDate.getDay());
                        gpsDateTime.setValue(XMPUtils.convertFromDate(new XMPDateTimeImpl(cal)));
                    }
                }
            } catch (XMPException e) {
            }
        }
    }

    private static void deleteEmptySchemas(XMPNode tree) {
        Iterator it = tree.iterateChildren();
        while (it.hasNext()) {
            if (!((XMPNode) it.next()).hasChildren()) {
                it.remove();
            }
        }
    }

    private static void compareAliasedSubtrees(XMPNode aliasNode, XMPNode baseNode, boolean outerCall) throws XMPException {
        if (!aliasNode.getValue().equals(baseNode.getValue()) || aliasNode.getChildrenLength() != baseNode.getChildrenLength()) {
            throw new XMPException("Mismatch between alias and base nodes", 203);
        } else if (outerCall || (aliasNode.getName().equals(baseNode.getName()) && aliasNode.getOptions().equals(baseNode.getOptions()) && aliasNode.getQualifierLength() == baseNode.getQualifierLength())) {
            Iterator an = aliasNode.iterateChildren();
            Iterator bn = baseNode.iterateChildren();
            while (an.hasNext() && bn.hasNext()) {
                compareAliasedSubtrees((XMPNode) an.next(), (XMPNode) bn.next(), false);
            }
            an = aliasNode.iterateQualifier();
            bn = baseNode.iterateQualifier();
            while (an.hasNext() && bn.hasNext()) {
                compareAliasedSubtrees((XMPNode) an.next(), (XMPNode) bn.next(), false);
            }
        } else {
            throw new XMPException("Mismatch between alias and base nodes", 203);
        }
    }

    private static void migrateAudioCopyright(XMPMeta xmp, XMPNode dmCopyright) {
        try {
            XMPNode dcSchema = XMPNodeUtils.findSchemaNode(((XMPMetaImpl) xmp).getRoot(), XMPConst.NS_DC, true);
            String dmValue = dmCopyright.getValue();
            String doubleLF = "\n\n";
            XMPNode dcRightsArray = XMPNodeUtils.findChildNode(dcSchema, "dc:rights", false);
            if (dcRightsArray != null) {
                if (dcRightsArray.hasChildren()) {
                    int xdIndex = XMPNodeUtils.lookupLanguageItem(dcRightsArray, XMPConst.X_DEFAULT);
                    if (xdIndex < 0) {
                        XMPMeta xMPMeta = xmp;
                        xMPMeta.setLocalizedText(XMPConst.NS_DC, "rights", "", XMPConst.X_DEFAULT, dcRightsArray.getChild(1).getValue(), null);
                        xdIndex = XMPNodeUtils.lookupLanguageItem(dcRightsArray, XMPConst.X_DEFAULT);
                    }
                    XMPNode defaultNode = dcRightsArray.getChild(xdIndex);
                    String defaultValue = defaultNode.getValue();
                    int lfPos = defaultValue.indexOf(doubleLF);
                    if (lfPos < 0) {
                        if (!dmValue.equals(defaultValue)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(defaultValue);
                            stringBuilder.append(doubleLF);
                            stringBuilder.append(dmValue);
                            defaultNode.setValue(stringBuilder.toString());
                        }
                    } else if (!defaultValue.substring(lfPos + 2).equals(dmValue)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(defaultValue.substring(0, lfPos + 2));
                        stringBuilder2.append(dmValue);
                        defaultNode.setValue(stringBuilder2.toString());
                    }
                    dmCopyright.getParent().removeChild(dmCopyright);
                }
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(doubleLF);
            stringBuilder3.append(dmValue);
            String dmValue2 = stringBuilder3.toString();
            xmp.setLocalizedText(XMPConst.NS_DC, "rights", "", XMPConst.X_DEFAULT, dmValue2, null);
            dmValue = dmValue2;
            dmCopyright.getParent().removeChild(dmCopyright);
        } catch (XMPException e) {
        }
    }

    private static void initDCArrays() {
        dcArrayForms = new HashMap();
        PropertyOptions bagForm = new PropertyOptions();
        bagForm.setArray(true);
        dcArrayForms.put("dc:contributor", bagForm);
        dcArrayForms.put("dc:language", bagForm);
        dcArrayForms.put("dc:publisher", bagForm);
        dcArrayForms.put("dc:relation", bagForm);
        dcArrayForms.put("dc:subject", bagForm);
        dcArrayForms.put("dc:type", bagForm);
        PropertyOptions seqForm = new PropertyOptions();
        seqForm.setArray(true);
        seqForm.setArrayOrdered(true);
        dcArrayForms.put("dc:creator", seqForm);
        dcArrayForms.put("dc:date", seqForm);
        PropertyOptions altTextForm = new PropertyOptions();
        altTextForm.setArray(true);
        altTextForm.setArrayOrdered(true);
        altTextForm.setArrayAlternate(true);
        altTextForm.setArrayAltText(true);
        dcArrayForms.put("dc:description", altTextForm);
        dcArrayForms.put("dc:rights", altTextForm);
        dcArrayForms.put("dc:title", altTextForm);
    }
}
