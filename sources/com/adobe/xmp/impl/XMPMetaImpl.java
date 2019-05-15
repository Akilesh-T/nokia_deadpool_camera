package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPDateTime;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPPathFactory;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.impl.xpath.XMPPathParser;
import com.adobe.xmp.options.IteratorOptions;
import com.adobe.xmp.options.ParseOptions;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.properties.XMPProperty;
import java.util.Calendar;
import java.util.Iterator;

public class XMPMetaImpl implements XMPMeta, XMPConst {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int VALUE_BASE64 = 7;
    private static final int VALUE_BOOLEAN = 1;
    private static final int VALUE_CALENDAR = 6;
    private static final int VALUE_DATE = 5;
    private static final int VALUE_DOUBLE = 4;
    private static final int VALUE_INTEGER = 2;
    private static final int VALUE_LONG = 3;
    private static final int VALUE_STRING = 0;
    private String packetHeader;
    private XMPNode tree;

    public XMPMetaImpl() {
        this.packetHeader = null;
        this.tree = new XMPNode(null, null, null);
    }

    public XMPMetaImpl(XMPNode tree) {
        this.packetHeader = null;
        this.tree = tree;
    }

    public void appendArrayItem(String schemaNS, String arrayName, PropertyOptions arrayOptions, String itemValue, PropertyOptions itemOptions) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        if (arrayOptions == null) {
            arrayOptions = new PropertyOptions();
        }
        if (arrayOptions.isOnlyArrayOptions()) {
            arrayOptions = XMPNodeUtils.verifySetOptions(arrayOptions, null);
            XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
            XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, false, null);
            if (arrayNode != null) {
                if (!arrayNode.getOptions().isArray()) {
                    throw new XMPException("The named property is not an array", 102);
                }
            } else if (arrayOptions.isArray()) {
                arrayNode = XMPNodeUtils.findNode(this.tree, arrayPath, true, arrayOptions);
                if (arrayNode == null) {
                    throw new XMPException("Failure creating array node", 102);
                }
            } else {
                throw new XMPException("Explicit arrayOptions required to create new array", 103);
            }
            doSetArrayItem(arrayNode, -1, itemValue, itemOptions, true);
            return;
        }
        throw new XMPException("Only array form flags allowed for arrayOptions", 103);
    }

    public void appendArrayItem(String schemaNS, String arrayName, String itemValue) throws XMPException {
        appendArrayItem(schemaNS, arrayName, null, itemValue, null);
    }

    public int countArrayItems(String schemaNS, String arrayName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, arrayName), false, null);
        if (arrayNode == null) {
            return 0;
        }
        if (arrayNode.getOptions().isArray()) {
            return arrayNode.getChildrenLength();
        }
        throw new XMPException("The named property is not an array", 102);
    }

    public void deleteArrayItem(String schemaNS, String arrayName, int itemIndex) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertArrayName(arrayName);
            deleteProperty(schemaNS, XMPPathFactory.composeArrayItemPath(arrayName, itemIndex));
        } catch (XMPException e) {
        }
    }

    public void deleteProperty(String schemaNS, String propName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            XMPNode propNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, propName), false, null);
            if (propNode != null) {
                XMPNodeUtils.deleteNode(propNode);
            }
        } catch (XMPException e) {
        }
    }

    public void deleteQualifier(String schemaNS, String propName, String qualNS, String qualName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            String qualPath = new StringBuilder();
            qualPath.append(propName);
            qualPath.append(XMPPathFactory.composeQualifierPath(qualNS, qualName));
            deleteProperty(schemaNS, qualPath.toString());
        } catch (XMPException e) {
        }
    }

    public void deleteStructField(String schemaNS, String structName, String fieldNS, String fieldName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertStructName(structName);
            String fieldPath = new StringBuilder();
            fieldPath.append(structName);
            fieldPath.append(XMPPathFactory.composeStructFieldPath(fieldNS, fieldName));
            deleteProperty(schemaNS, fieldPath.toString());
        } catch (XMPException e) {
        }
    }

    public boolean doesPropertyExist(String schemaNS, String propName) {
        boolean z = false;
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            if (XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, propName), false, null) != null) {
                z = true;
            }
            return z;
        } catch (XMPException e) {
            return false;
        }
    }

    public boolean doesArrayItemExist(String schemaNS, String arrayName, int itemIndex) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertArrayName(arrayName);
            return doesPropertyExist(schemaNS, XMPPathFactory.composeArrayItemPath(arrayName, itemIndex));
        } catch (XMPException e) {
            return false;
        }
    }

    public boolean doesStructFieldExist(String schemaNS, String structName, String fieldNS, String fieldName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertStructName(structName);
            String path = XMPPathFactory.composeStructFieldPath(fieldNS, fieldName);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(structName);
            stringBuilder.append(path);
            return doesPropertyExist(schemaNS, stringBuilder.toString());
        } catch (XMPException e) {
            return false;
        }
    }

    public boolean doesQualifierExist(String schemaNS, String propName, String qualNS, String qualName) {
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            String path = XMPPathFactory.composeQualifierPath(qualNS, qualName);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(propName);
            stringBuilder.append(path);
            return doesPropertyExist(schemaNS, stringBuilder.toString());
        } catch (XMPException e) {
            return false;
        }
    }

    public XMPProperty getArrayItem(String schemaNS, String arrayName, int itemIndex) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        return getProperty(schemaNS, XMPPathFactory.composeArrayItemPath(arrayName, itemIndex));
    }

    public XMPProperty getLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(altTextName);
        ParameterAsserts.assertSpecificLang(specificLang);
        genericLang = genericLang != null ? Utils.normalizeLangValue(genericLang) : null;
        specificLang = Utils.normalizeLangValue(specificLang);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, altTextName), false, null);
        if (arrayNode == null) {
            return null;
        }
        Object[] result = XMPNodeUtils.chooseLocalizedText(arrayNode, genericLang, specificLang);
        final XMPNode itemNode = result[1];
        if (((Integer) result[0]).intValue() != 0) {
            return new XMPProperty() {
                public Object getValue() {
                    return itemNode.getValue();
                }

                public PropertyOptions getOptions() {
                    return itemNode.getOptions();
                }

                public String getLanguage() {
                    return itemNode.getQualifier(1).getValue();
                }

                public String toString() {
                    return itemNode.getValue().toString();
                }
            };
        }
        return null;
    }

    public void setLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang, String itemValue, PropertyOptions options) throws XMPException {
        String str = itemValue;
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(altTextName);
        ParameterAsserts.assertSpecificLang(specificLang);
        String genericLang2 = genericLang != null ? Utils.normalizeLangValue(genericLang) : null;
        String specificLang2 = Utils.normalizeLangValue(specificLang);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, altTextName), true, new PropertyOptions(7680));
        if (arrayNode != null) {
            XMPNode currItem;
            if (!arrayNode.getOptions().isArrayAltText()) {
                if (arrayNode.hasChildren() || !arrayNode.getOptions().isArrayAlternate()) {
                    throw new XMPException("Specified property is no alt-text array", 102);
                }
                arrayNode.getOptions().setArrayAltText(true);
            }
            boolean haveXDefault = false;
            XMPNode xdItem = null;
            Iterator it = arrayNode.iterateChildren();
            while (it.hasNext()) {
                currItem = (XMPNode) it.next();
                if (!currItem.hasQualifier() || !XMPConst.XML_LANG.equals(currItem.getQualifier(1).getName())) {
                    throw new XMPException("Language qualifier must be first", 102);
                } else if (XMPConst.X_DEFAULT.equals(currItem.getQualifier(1).getValue())) {
                    xdItem = currItem;
                    haveXDefault = true;
                    break;
                }
            }
            if (xdItem != null && arrayNode.getChildrenLength() > 1) {
                arrayNode.removeChild(xdItem);
                arrayNode.addChild(1, xdItem);
            }
            Object[] result = XMPNodeUtils.chooseLocalizedText(arrayNode, genericLang2, specificLang2);
            int match = ((Integer) result[0]).intValue();
            currItem = result[1];
            boolean specificXDefault = XMPConst.X_DEFAULT.equals(specificLang2);
            switch (match) {
                case 0:
                    XMPNodeUtils.appendLangItem(arrayNode, XMPConst.X_DEFAULT, str);
                    haveXDefault = true;
                    if (!specificXDefault) {
                        XMPNodeUtils.appendLangItem(arrayNode, specificLang2, str);
                        break;
                    }
                    break;
                case 1:
                    if (!specificXDefault) {
                        if (haveXDefault && xdItem != currItem && xdItem != null && xdItem.getValue().equals(currItem.getValue())) {
                            xdItem.setValue(str);
                        }
                        currItem.setValue(str);
                        break;
                    }
                    Iterator it2 = arrayNode.iterateChildren();
                    while (it2.hasNext()) {
                        XMPNode currItem2 = (XMPNode) it2.next();
                        if (currItem2 != xdItem) {
                            Object value;
                            String value2 = currItem2.getValue();
                            if (xdItem != null) {
                                value = xdItem.getValue();
                            } else {
                                value = null;
                            }
                            if (value2.equals(value)) {
                                currItem2.setValue(str);
                            }
                        }
                    }
                    if (xdItem != null) {
                        xdItem.setValue(str);
                        break;
                    }
                    break;
                case 2:
                    if (haveXDefault && xdItem != currItem && xdItem != null && xdItem.getValue().equals(currItem.getValue())) {
                        xdItem.setValue(str);
                    }
                    currItem.setValue(str);
                    break;
                case 3:
                    XMPNodeUtils.appendLangItem(arrayNode, specificLang2, str);
                    if (specificXDefault) {
                        haveXDefault = true;
                        break;
                    }
                    break;
                case 4:
                    if (xdItem != null && arrayNode.getChildrenLength() == 1) {
                        xdItem.setValue(str);
                    }
                    XMPNodeUtils.appendLangItem(arrayNode, specificLang2, str);
                    break;
                case 5:
                    XMPNodeUtils.appendLangItem(arrayNode, specificLang2, str);
                    if (specificXDefault) {
                        haveXDefault = true;
                        break;
                    }
                    break;
                default:
                    throw new XMPException("Unexpected result from ChooseLocalizedText", 9);
            }
            if (!haveXDefault && arrayNode.getChildrenLength() == 1) {
                XMPNodeUtils.appendLangItem(arrayNode, XMPConst.X_DEFAULT, str);
                return;
            }
            return;
        }
        throw new XMPException("Failed to find or create array node", 102);
    }

    public void setLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang, String itemValue) throws XMPException {
        setLocalizedText(schemaNS, altTextName, genericLang, specificLang, itemValue, null);
    }

    public XMPProperty getProperty(String schemaNS, String propName) throws XMPException {
        return getProperty(schemaNS, propName, 0);
    }

    /* Access modifiers changed, original: protected */
    public XMPProperty getProperty(String schemaNS, String propName, int valueType) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        final XMPNode propNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, propName), false, null);
        if (propNode == null) {
            return null;
        }
        if (valueType == 0 || !propNode.getOptions().isCompositeProperty()) {
            final Object value = evaluateNodeValue(valueType, propNode);
            return new XMPProperty() {
                public Object getValue() {
                    return value;
                }

                public PropertyOptions getOptions() {
                    return propNode.getOptions();
                }

                public String getLanguage() {
                    return null;
                }

                public String toString() {
                    return value.toString();
                }
            };
        }
        throw new XMPException("Property must be simple when a value type is requested", 102);
    }

    /* Access modifiers changed, original: protected */
    public Object getPropertyObject(String schemaNS, String propName, int valueType) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        XMPNode propNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, propName), false, null);
        if (propNode == null) {
            return null;
        }
        if (valueType == 0 || !propNode.getOptions().isCompositeProperty()) {
            return evaluateNodeValue(valueType, propNode);
        }
        throw new XMPException("Property must be simple when a value type is requested", 102);
    }

    public Boolean getPropertyBoolean(String schemaNS, String propName) throws XMPException {
        return (Boolean) getPropertyObject(schemaNS, propName, 1);
    }

    public void setPropertyBoolean(String schemaNS, String propName, boolean propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue ? XMPConst.TRUESTR : XMPConst.FALSESTR, options);
    }

    public void setPropertyBoolean(String schemaNS, String propName, boolean propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue ? XMPConst.TRUESTR : XMPConst.FALSESTR, null);
    }

    public Integer getPropertyInteger(String schemaNS, String propName) throws XMPException {
        return (Integer) getPropertyObject(schemaNS, propName, 2);
    }

    public void setPropertyInteger(String schemaNS, String propName, int propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Integer(propValue), options);
    }

    public void setPropertyInteger(String schemaNS, String propName, int propValue) throws XMPException {
        setProperty(schemaNS, propName, new Integer(propValue), null);
    }

    public Long getPropertyLong(String schemaNS, String propName) throws XMPException {
        return (Long) getPropertyObject(schemaNS, propName, 3);
    }

    public void setPropertyLong(String schemaNS, String propName, long propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Long(propValue), options);
    }

    public void setPropertyLong(String schemaNS, String propName, long propValue) throws XMPException {
        setProperty(schemaNS, propName, new Long(propValue), null);
    }

    public Double getPropertyDouble(String schemaNS, String propName) throws XMPException {
        return (Double) getPropertyObject(schemaNS, propName, 4);
    }

    public void setPropertyDouble(String schemaNS, String propName, double propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, new Double(propValue), options);
    }

    public void setPropertyDouble(String schemaNS, String propName, double propValue) throws XMPException {
        setProperty(schemaNS, propName, new Double(propValue), null);
    }

    public XMPDateTime getPropertyDate(String schemaNS, String propName) throws XMPException {
        return (XMPDateTime) getPropertyObject(schemaNS, propName, 5);
    }

    public void setPropertyDate(String schemaNS, String propName, XMPDateTime propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    public void setPropertyDate(String schemaNS, String propName, XMPDateTime propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public Calendar getPropertyCalendar(String schemaNS, String propName) throws XMPException {
        return (Calendar) getPropertyObject(schemaNS, propName, 6);
    }

    public void setPropertyCalendar(String schemaNS, String propName, Calendar propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    public void setPropertyCalendar(String schemaNS, String propName, Calendar propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public byte[] getPropertyBase64(String schemaNS, String propName) throws XMPException {
        return (byte[]) getPropertyObject(schemaNS, propName, 7);
    }

    public String getPropertyString(String schemaNS, String propName) throws XMPException {
        return (String) getPropertyObject(schemaNS, propName, 0);
    }

    public void setPropertyBase64(String schemaNS, String propName, byte[] propValue, PropertyOptions options) throws XMPException {
        setProperty(schemaNS, propName, propValue, options);
    }

    public void setPropertyBase64(String schemaNS, String propName, byte[] propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public XMPProperty getQualifier(String schemaNS, String propName, String qualNS, String qualName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        String qualPath = new StringBuilder();
        qualPath.append(propName);
        qualPath.append(XMPPathFactory.composeQualifierPath(qualNS, qualName));
        return getProperty(schemaNS, qualPath.toString());
    }

    public XMPProperty getStructField(String schemaNS, String structName, String fieldNS, String fieldName) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertStructName(structName);
        String fieldPath = new StringBuilder();
        fieldPath.append(structName);
        fieldPath.append(XMPPathFactory.composeStructFieldPath(fieldNS, fieldName));
        return getProperty(schemaNS, fieldPath.toString());
    }

    public XMPIterator iterator() throws XMPException {
        return iterator(null, null, null);
    }

    public XMPIterator iterator(IteratorOptions options) throws XMPException {
        return iterator(null, null, options);
    }

    public XMPIterator iterator(String schemaNS, String propName, IteratorOptions options) throws XMPException {
        return new XMPIteratorImpl(this, schemaNS, propName, options);
    }

    public void setArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, arrayName), false, null);
        if (arrayNode != null) {
            doSetArrayItem(arrayNode, itemIndex, itemValue, options, false);
            return;
        }
        throw new XMPException("Specified array does not exist", 102);
    }

    public void setArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue) throws XMPException {
        setArrayItem(schemaNS, arrayName, itemIndex, itemValue, null);
    }

    public void insertArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, arrayName), false, null);
        if (arrayNode != null) {
            doSetArrayItem(arrayNode, itemIndex, itemValue, options, true);
            return;
        }
        throw new XMPException("Specified array does not exist", 102);
    }

    public void insertArrayItem(String schemaNS, String arrayName, int itemIndex, String itemValue) throws XMPException {
        insertArrayItem(schemaNS, arrayName, itemIndex, itemValue, null);
    }

    public void setProperty(String schemaNS, String propName, Object propValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        options = XMPNodeUtils.verifySetOptions(options, propValue);
        XMPNode propNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, propName), true, options);
        if (propNode != null) {
            setNode(propNode, propValue, options, false);
            return;
        }
        throw new XMPException("Specified property does not exist", 102);
    }

    public void setProperty(String schemaNS, String propName, Object propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public void setQualifier(String schemaNS, String propName, String qualNS, String qualName, String qualValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        if (doesPropertyExist(schemaNS, propName)) {
            String qualPath = new StringBuilder();
            qualPath.append(propName);
            qualPath.append(XMPPathFactory.composeQualifierPath(qualNS, qualName));
            setProperty(schemaNS, qualPath.toString(), qualValue, options);
            return;
        }
        throw new XMPException("Specified property does not exist!", 102);
    }

    public void setQualifier(String schemaNS, String propName, String qualNS, String qualName, String qualValue) throws XMPException {
        setQualifier(schemaNS, propName, qualNS, qualName, qualValue, null);
    }

    public void setStructField(String schemaNS, String structName, String fieldNS, String fieldName, String fieldValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertStructName(structName);
        String fieldPath = new StringBuilder();
        fieldPath.append(structName);
        fieldPath.append(XMPPathFactory.composeStructFieldPath(fieldNS, fieldName));
        setProperty(schemaNS, fieldPath.toString(), fieldValue, options);
    }

    public void setStructField(String schemaNS, String structName, String fieldNS, String fieldName, String fieldValue) throws XMPException {
        setStructField(schemaNS, structName, fieldNS, fieldName, fieldValue, null);
    }

    public String getObjectName() {
        return this.tree.getName() != null ? this.tree.getName() : "";
    }

    public void setObjectName(String name) {
        this.tree.setName(name);
    }

    public String getPacketHeader() {
        return this.packetHeader;
    }

    public void setPacketHeader(String packetHeader) {
        this.packetHeader = packetHeader;
    }

    public Object clone() {
        return new XMPMetaImpl((XMPNode) this.tree.clone());
    }

    public String dumpObject() {
        return getRoot().dumpNode(true);
    }

    public void sort() {
        this.tree.sort();
    }

    public void normalize(ParseOptions options) throws XMPException {
        if (options == null) {
            options = new ParseOptions();
        }
        XMPNormalizer.process(this, options);
    }

    public XMPNode getRoot() {
        return this.tree;
    }

    private void doSetArrayItem(XMPNode arrayNode, int itemIndex, String itemValue, PropertyOptions itemOptions, boolean insert) throws XMPException {
        XMPNode itemNode = new XMPNode(XMPConst.ARRAY_ITEM_NAME, null);
        itemOptions = XMPNodeUtils.verifySetOptions(itemOptions, itemValue);
        int maxIndex = insert ? arrayNode.getChildrenLength() + 1 : arrayNode.getChildrenLength();
        if (itemIndex == -1) {
            itemIndex = maxIndex;
        }
        if (1 > itemIndex || itemIndex > maxIndex) {
            throw new XMPException("Array index out of bounds", 104);
        }
        if (!insert) {
            arrayNode.removeChild(itemIndex);
        }
        arrayNode.addChild(itemIndex, itemNode);
        setNode(itemNode, itemValue, itemOptions, false);
    }

    /* Access modifiers changed, original: 0000 */
    public void setNode(XMPNode node, Object value, PropertyOptions newOptions, boolean deleteExisting) throws XMPException {
        if (deleteExisting) {
            node.clear();
        }
        node.getOptions().mergeWith(newOptions);
        if (!node.getOptions().isCompositeProperty()) {
            XMPNodeUtils.setNodeValue(node, value);
        } else if (value == null || value.toString().length() <= 0) {
            node.removeChildren();
        } else {
            throw new XMPException("Composite nodes can't have values", 102);
        }
    }

    private Object evaluateNodeValue(int valueType, XMPNode propNode) throws XMPException {
        String rawValue = propNode.getValue();
        switch (valueType) {
            case 1:
                return new Boolean(XMPUtils.convertToBoolean(rawValue));
            case 2:
                return new Integer(XMPUtils.convertToInteger(rawValue));
            case 3:
                return new Long(XMPUtils.convertToLong(rawValue));
            case 4:
                return new Double(XMPUtils.convertToDouble(rawValue));
            case 5:
                return XMPUtils.convertToDate(rawValue);
            case 6:
                return XMPUtils.convertToDate(rawValue).getCalendar();
            case 7:
                return XMPUtils.decodeBase64(rawValue);
            default:
                return (rawValue != null || propNode.getOptions().isCompositeProperty()) ? rawValue : "";
        }
    }
}
