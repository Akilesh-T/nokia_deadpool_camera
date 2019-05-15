package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.impl.xpath.XMPPath;
import com.adobe.xmp.impl.xpath.XMPPathParser;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.properties.XMPAliasInfo;
import java.util.Iterator;

public class XMPUtilsImpl implements XMPConst {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String COMMAS = ",，､﹐﹑、،՝";
    private static final String CONTROLS = "  ";
    private static final String QUOTES = "\"[]«»〝〞〟―‹›";
    private static final String SEMICOLA = ";；﹔؛;";
    private static final String SPACES = " 　〿";
    private static final int UCK_COMMA = 2;
    private static final int UCK_CONTROL = 5;
    private static final int UCK_NORMAL = 0;
    private static final int UCK_QUOTE = 4;
    private static final int UCK_SEMICOLON = 3;
    private static final int UCK_SPACE = 1;

    private XMPUtilsImpl() {
    }

    public static String catenateArrayItems(XMPMeta xmp, String schemaNS, String arrayName, String separator, String quotes, boolean allowCommas) throws XMPException {
        String separator2;
        String quotes2;
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        ParameterAsserts.assertImplementation(xmp);
        if (separator == null || separator.length() == 0) {
            separator2 = "; ";
        } else {
            separator2 = separator;
        }
        if (quotes == null || quotes.length() == 0) {
            quotes2 = "\"";
        } else {
            quotes2 = quotes;
        }
        XMPMetaImpl xmpImpl = (XMPMetaImpl) xmp;
        XMPNode arrayNode = XMPNodeUtils.findNode(xmpImpl.getRoot(), XMPPathParser.expandXPath(schemaNS, arrayName), false, null);
        if (arrayNode == null) {
            return "";
        }
        boolean z;
        if (!arrayNode.getOptions().isArray() || arrayNode.getOptions().isArrayAlternate()) {
            z = allowCommas;
            throw new XMPException("Named property must be non-alternate array", 4);
        }
        checkSeparator(separator2);
        char openQuote = quotes2.charAt(0);
        char closeQuote = checkQuotes(quotes2, openQuote);
        StringBuffer catinatedString = new StringBuffer();
        Iterator it = arrayNode.iterateChildren();
        while (it.hasNext()) {
            XMPNode currItem = (XMPNode) it.next();
            if (currItem.getOptions().isCompositeProperty()) {
                z = allowCommas;
                throw new XMPException("Array items must be simple", 4);
            }
            catinatedString.append(applyQuotes(currItem.getValue(), openQuote, closeQuote, allowCommas));
            if (it.hasNext()) {
                catinatedString.append(separator2);
            }
        }
        z = allowCommas;
        return catinatedString.toString();
    }

    public static void separateArrayItems(XMPMeta xmp, String schemaNS, String arrayName, String catedStr, PropertyOptions arrayOptions, boolean preserveCommas) throws XMPException {
        String str = catedStr;
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(arrayName);
        int i = 4;
        if (str != null) {
            ParameterAsserts.assertImplementation(xmp);
            XMPNode arrayNode = separateFindCreateArray(schemaNS, arrayName, arrayOptions, (XMPMetaImpl) xmp);
            int nextKind = 0;
            int charKind = 0;
            char ch = 0;
            int itemEnd = 0;
            int endPos = catedStr.length();
            while (itemEnd < endPos) {
                int charKind2 = charKind;
                charKind = itemEnd;
                while (charKind < endPos) {
                    ch = str.charAt(charKind);
                    charKind2 = classifyCharacter(ch);
                    if (charKind2 == 0 || charKind2 == i) {
                        break;
                    }
                    charKind++;
                }
                if (charKind >= endPos) {
                    charKind = charKind2;
                    return;
                }
                int nextKind2;
                int oldChild;
                String itemValue;
                int nextKind3;
                int i2 = 1;
                if (charKind2 != i) {
                    itemEnd = charKind;
                    while (itemEnd < endPos) {
                        ch = str.charAt(itemEnd);
                        charKind2 = classifyCharacter(ch);
                        if (!(charKind2 == 0 || charKind2 == r2 || (charKind2 == 2 && preserveCommas))) {
                            if (charKind2 != i2 || itemEnd + 1 >= endPos) {
                                break;
                            }
                            ch = str.charAt(itemEnd + 1);
                            nextKind = classifyCharacter(ch);
                            if (nextKind != 0) {
                                if (nextKind != 4) {
                                    if (nextKind != 2 || !preserveCommas) {
                                        break;
                                    }
                                } else {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                        }
                        itemEnd++;
                        i = 4;
                        i2 = 1;
                    }
                    String itemValue2 = str.substring(charKind, itemEnd);
                    nextKind2 = nextKind;
                    oldChild = 1;
                    itemValue = itemValue2;
                } else {
                    char openQuote = ch;
                    char closeQuote = getClosingQuote(openQuote);
                    itemEnd = charKind + 1;
                    int nextKind4 = nextKind;
                    itemValue = "";
                    while (itemEnd < endPos) {
                        ch = str.charAt(itemEnd);
                        charKind2 = classifyCharacter(ch);
                        StringBuilder stringBuilder;
                        if (charKind2 == 4 && isSurroundingQuote(ch, openQuote, closeQuote)) {
                            char nextChar;
                            if (itemEnd + 1 < endPos) {
                                char nextChar2 = str.charAt(itemEnd + 1);
                                nextChar = nextChar2;
                                nextKind2 = classifyCharacter(nextChar2);
                            } else {
                                nextKind2 = 3;
                                nextChar = ';';
                            }
                            if (ch != nextChar) {
                                if (isClosingingQuote(ch, openQuote, closeQuote)) {
                                    itemEnd++;
                                    oldChild = 1;
                                    break;
                                }
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(itemValue);
                                stringBuilder.append(ch);
                                str = stringBuilder.toString();
                            } else {
                                str = new StringBuilder();
                                str.append(itemValue);
                                str.append(ch);
                                str = str.toString();
                                itemEnd++;
                            }
                            itemValue = str;
                            nextKind4 = nextKind2;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(itemValue);
                            stringBuilder.append(ch);
                            itemValue = stringBuilder.toString();
                        }
                        itemEnd++;
                        str = catedStr;
                    }
                    oldChild = 1;
                    nextKind2 = nextKind4;
                }
                i = -1;
                for (oldChild = 
/*
Method generation error in method: com.adobe.xmp.impl.XMPUtilsImpl.separateArrayItems(com.adobe.xmp.XMPMeta, java.lang.String, java.lang.String, java.lang.String, com.adobe.xmp.options.PropertyOptions, boolean):void, dex: classes3.dex
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r0_16 'oldChild' int) = (r0_2 'oldChild' int), (r0_10 'oldChild' int), (r0_15 'oldChild' int) binds: {(r0_2 'oldChild' int)=B:32:0x0078, (r0_10 'oldChild' int)=B:49:0x00e9, (r0_15 'oldChild' int)=B:52:0x0103} in method: com.adobe.xmp.impl.XMPUtilsImpl.separateArrayItems(com.adobe.xmp.XMPMeta, java.lang.String, java.lang.String, java.lang.String, com.adobe.xmp.options.PropertyOptions, boolean):void, dex: classes3.dex
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:220)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 31 more

*/

    private static XMPNode separateFindCreateArray(String schemaNS, String arrayName, PropertyOptions arrayOptions, XMPMetaImpl xmp) throws XMPException {
        arrayOptions = XMPNodeUtils.verifySetOptions(arrayOptions, null);
        if (arrayOptions.isOnlyArrayOptions()) {
            XMPPath arrayPath = XMPPathParser.expandXPath(schemaNS, arrayName);
            XMPNode arrayNode = XMPNodeUtils.findNode(xmp.getRoot(), arrayPath, false, null);
            if (arrayNode != null) {
                PropertyOptions arrayForm = arrayNode.getOptions();
                if (!arrayForm.isArray() || arrayForm.isArrayAlternate()) {
                    throw new XMPException("Named property must be non-alternate array", 102);
                } else if (arrayOptions.equalArrayTypes(arrayForm)) {
                    throw new XMPException("Mismatch of specified and existing array form", 102);
                }
            }
            arrayNode = XMPNodeUtils.findNode(xmp.getRoot(), arrayPath, true, arrayOptions.setArray(true));
            if (arrayNode == null) {
                throw new XMPException("Failed to create named array", 102);
            }
            return arrayNode;
        }
        throw new XMPException("Options can only provide array form", 103);
    }

    public static void removeProperties(XMPMeta xmp, String schemaNS, String propName, boolean doAllProperties, boolean includeAliases) throws XMPException {
        ParameterAsserts.assertImplementation(xmp);
        XMPMetaImpl xmpImpl = (XMPMetaImpl) xmp;
        if (propName == null || propName.length() <= 0) {
            if (schemaNS == null || schemaNS.length() <= 0) {
                Iterator it = xmpImpl.getRoot().iterateChildren();
                while (it.hasNext()) {
                    if (removeSchemaChildren((XMPNode) it.next(), doAllProperties)) {
                        it.remove();
                    }
                }
                return;
            }
            XMPNode schemaNode = XMPNodeUtils.findSchemaNode(xmpImpl.getRoot(), schemaNS, false);
            if (schemaNode != null && removeSchemaChildren(schemaNode, doAllProperties)) {
                xmpImpl.getRoot().removeChild(schemaNode);
            }
            if (includeAliases) {
                XMPAliasInfo[] aliases = XMPMetaFactory.getSchemaRegistry().findAliases(schemaNS);
                for (XMPAliasInfo info : aliases) {
                    XMPNode actualProp = XMPNodeUtils.findNode(xmpImpl.getRoot(), XMPPathParser.expandXPath(info.getNamespace(), info.getPropName()), false, null);
                    if (actualProp != null) {
                        actualProp.getParent().removeChild(actualProp);
                    }
                }
            }
        } else if (schemaNS == null || schemaNS.length() == 0) {
            throw new XMPException("Property name requires schema namespace", 4);
        } else {
            XMPPath expPath = XMPPathParser.expandXPath(schemaNS, propName);
            XMPNode propNode = XMPNodeUtils.findNode(xmpImpl.getRoot(), expPath, false, null);
            if (propNode == null) {
                return;
            }
            if (doAllProperties || !Utils.isInternalProperty(expPath.getSegment(0).getName(), expPath.getSegment(1).getName())) {
                XMPNode parent = propNode.getParent();
                parent.removeChild(propNode);
                if (parent.getOptions().isSchemaNode() && !parent.hasChildren()) {
                    parent.getParent().removeChild(parent);
                }
            }
        }
    }

    public static void appendProperties(XMPMeta source, XMPMeta destination, boolean doAllProperties, boolean replaceOldValues, boolean deleteEmptyValues) throws XMPException {
        ParameterAsserts.assertImplementation(source);
        ParameterAsserts.assertImplementation(destination);
        XMPMetaImpl dest = (XMPMetaImpl) destination;
        Iterator it = ((XMPMetaImpl) source).getRoot().iterateChildren();
        while (it.hasNext()) {
            XMPNode sourceSchema = (XMPNode) it.next();
            XMPNode destSchema = XMPNodeUtils.findSchemaNode(dest.getRoot(), sourceSchema.getName(), false);
            boolean createdSchema = false;
            if (destSchema == null) {
                destSchema = new XMPNode(sourceSchema.getName(), sourceSchema.getValue(), new PropertyOptions().setSchemaNode(true));
                dest.getRoot().addChild(destSchema);
                createdSchema = true;
            }
            Iterator ic = sourceSchema.iterateChildren();
            while (ic.hasNext()) {
                XMPNode sourceProp = (XMPNode) ic.next();
                if (doAllProperties || !Utils.isInternalProperty(sourceSchema.getName(), sourceProp.getName())) {
                    appendSubtree(dest, sourceProp, destSchema, replaceOldValues, deleteEmptyValues);
                }
            }
            if (!destSchema.hasChildren() && (createdSchema || deleteEmptyValues)) {
                dest.getRoot().removeChild(destSchema);
            }
        }
    }

    private static boolean removeSchemaChildren(XMPNode schemaNode, boolean doAllProperties) {
        Iterator it = schemaNode.iterateChildren();
        while (it.hasNext()) {
            XMPNode currProp = (XMPNode) it.next();
            if (doAllProperties || !Utils.isInternalProperty(schemaNode.getName(), currProp.getName())) {
                it.remove();
            }
        }
        return schemaNode.hasChildren() ^ 1;
    }

    private static void appendSubtree(XMPMetaImpl destXMP, XMPNode sourceNode, XMPNode destParent, boolean replaceOldValues, boolean deleteEmptyValues) throws XMPException {
        XMPMetaImpl xMPMetaImpl = destXMP;
        XMPNode xMPNode = destParent;
        boolean z = replaceOldValues;
        boolean z2 = deleteEmptyValues;
        boolean z3 = false;
        XMPNode destNode = XMPNodeUtils.findChildNode(xMPNode, sourceNode.getName(), false);
        boolean valueIsEmpty = false;
        if (z2) {
            valueIsEmpty = sourceNode.getOptions().isSimple() ? z3 : z3;
            z3 = true;
        }
        if (z2 && valueIsEmpty) {
            if (destNode != null) {
                xMPNode.removeChild(destNode);
            }
        } else if (destNode == null) {
            xMPNode.addChild((XMPNode) sourceNode.clone());
        } else if (z) {
            xMPMetaImpl.setNode(destNode, sourceNode.getValue(), sourceNode.getOptions(), true);
            xMPNode.removeChild(destNode);
            xMPNode.addChild((XMPNode) sourceNode.clone());
        } else {
            PropertyOptions sourceForm = sourceNode.getOptions();
            if (sourceForm == destNode.getOptions()) {
                Iterator it;
                if (sourceForm.isStruct()) {
                    it = sourceNode.iterateChildren();
                    while (it.hasNext()) {
                        appendSubtree(xMPMetaImpl, (XMPNode) it.next(), destNode, z, z2);
                        if (z2 && !destNode.hasChildren()) {
                            xMPNode.removeChild(destNode);
                        }
                    }
                } else if (sourceForm.isArrayAltText()) {
                    Iterator it2 = sourceNode.iterateChildren();
                    while (it2.hasNext()) {
                        XMPNode sourceItem = (XMPNode) it2.next();
                        if (sourceItem.hasQualifier()) {
                            if (XMPConst.XML_LANG.equals(sourceItem.getQualifier(1).getName())) {
                                int destIndex = XMPNodeUtils.lookupLanguageItem(destNode, sourceItem.getQualifier(1).getValue());
                                if (z2 && (sourceItem.getValue() == null || sourceItem.getValue().length() == 0)) {
                                    if (destIndex != -1) {
                                        destNode.removeChild(destIndex);
                                        if (!destNode.hasChildren()) {
                                            xMPNode.removeChild(destNode);
                                        }
                                    }
                                } else if (destIndex == -1) {
                                    if (XMPConst.X_DEFAULT.equals(sourceItem.getQualifier(1).getValue()) && destNode.hasChildren()) {
                                        XMPNode destItem = new XMPNode(sourceItem.getName(), sourceItem.getValue(), sourceItem.getOptions());
                                        sourceItem.cloneSubtree(destItem);
                                        destNode.addChild(1, destItem);
                                    } else {
                                        sourceItem.cloneSubtree(destNode);
                                    }
                                }
                            }
                        }
                    }
                } else if (sourceForm.isArray()) {
                    it = sourceNode.iterateChildren();
                    while (it.hasNext()) {
                        XMPNode sourceItem2 = (XMPNode) it.next();
                        boolean match = false;
                        Iterator id = destNode.iterateChildren();
                        while (id.hasNext()) {
                            if (itemValuesMatch(sourceItem2, (XMPNode) id.next())) {
                                match = true;
                            }
                        }
                        if (!match) {
                            destNode = (XMPNode) sourceItem2.clone();
                            xMPNode.addChild(destNode);
                        }
                    }
                }
            }
        }
    }

    private static boolean itemValuesMatch(XMPNode leftNode, XMPNode rightNode) throws XMPException {
        PropertyOptions leftForm = leftNode.getOptions();
        if (leftForm.equals(rightNode.getOptions())) {
            return false;
        }
        Iterator il;
        XMPNode leftItem;
        if (leftForm.getOptions() == 0) {
            if (!leftNode.getValue().equals(rightNode.getValue()) || leftNode.getOptions().getHasLanguage() != rightNode.getOptions().getHasLanguage()) {
                return false;
            }
            if (!leftNode.getOptions().getHasLanguage() || leftNode.getQualifier(1).getValue().equals(rightNode.getQualifier(1).getValue())) {
                return true;
            }
            return false;
        } else if (!leftForm.isStruct()) {
            il = leftNode.iterateChildren();
            while (il.hasNext()) {
                leftItem = (XMPNode) il.next();
                boolean match = false;
                Iterator ir = rightNode.iterateChildren();
                while (ir.hasNext()) {
                    if (itemValuesMatch(leftItem, (XMPNode) ir.next())) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return false;
                }
            }
        } else if (leftNode.getChildrenLength() != rightNode.getChildrenLength()) {
            return false;
        } else {
            il = leftNode.iterateChildren();
            while (il.hasNext()) {
                leftItem = (XMPNode) il.next();
                XMPNode rightField = XMPNodeUtils.findChildNode(rightNode, leftItem.getName(), false);
                if (rightField == null || !itemValuesMatch(leftItem, rightField)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void checkSeparator(String separator) throws XMPException {
        boolean haveSemicolon = false;
        for (int i = 0; i < separator.length(); i++) {
            int charKind = classifyCharacter(separator.charAt(i));
            if (charKind == 3) {
                if (haveSemicolon) {
                    throw new XMPException("Separator can have only one semicolon", 4);
                }
                haveSemicolon = true;
            } else if (charKind != 1) {
                throw new XMPException("Separator can have only spaces and one semicolon", 4);
            }
        }
        if (!haveSemicolon) {
            throw new XMPException("Separator must have one semicolon", 4);
        }
    }

    private static char checkQuotes(String quotes, char openQuote) throws XMPException {
        if (classifyCharacter(openQuote) == 4) {
            char closeQuote;
            if (quotes.length() == 1) {
                closeQuote = openQuote;
            } else {
                closeQuote = quotes.charAt(1);
                if (classifyCharacter(closeQuote) != 4) {
                    throw new XMPException("Invalid quoting character", 4);
                }
            }
            if (closeQuote == getClosingQuote(openQuote)) {
                return closeQuote;
            }
            throw new XMPException("Mismatched quote pair", 4);
        }
        throw new XMPException("Invalid quoting character", 4);
    }

    private static int classifyCharacter(char ch) {
        if (SPACES.indexOf(ch) >= 0 || (8192 <= ch && ch <= 8203)) {
            return 1;
        }
        if (COMMAS.indexOf(ch) >= 0) {
            return 2;
        }
        if (SEMICOLA.indexOf(ch) >= 0) {
            return 3;
        }
        if (QUOTES.indexOf(ch) >= 0 || ((12296 <= ch && ch <= 12303) || (8216 <= ch && ch <= 8223))) {
            return 4;
        }
        if (ch < ' ' || CONTROLS.indexOf(ch) >= 0) {
            return 5;
        }
        return 0;
    }

    private static char getClosingQuote(char openQuote) {
        switch (openQuote) {
            case '\"':
                return '\"';
            case '[':
                return ']';
            case 171:
                return 187;
            case 187:
                return 171;
            case 8213:
                return 8213;
            case 8216:
                return 8217;
            case 8218:
                return 8219;
            case 8220:
                return 8221;
            case 8222:
                return 8223;
            case 8249:
                return 8250;
            case 8250:
                return 8249;
            case 12296:
                return 12297;
            case 12298:
                return 12299;
            case 12300:
                return 12301;
            case 12302:
                return 12303;
            case 12317:
                return 12319;
            default:
                return 0;
        }
    }

    private static String applyQuotes(String item, char openQuote, char closeQuote, boolean allowCommas) {
        if (item == null) {
            item = "";
        }
        boolean prevSpace = false;
        int i = 0;
        while (i < item.length()) {
            int charKind = classifyCharacter(item.charAt(i));
            if (i == 0 && charKind == 4) {
                break;
            }
            if (charKind != 1) {
                prevSpace = false;
                if (charKind != 3) {
                    if (charKind != 5) {
                        if (charKind == 2 && !allowCommas) {
                            break;
                        }
                    }
                    break;
                }
                break;
            } else if (prevSpace) {
                break;
            } else {
                prevSpace = true;
            }
            i++;
        }
        if (i >= item.length()) {
            return item;
        }
        StringBuffer newItem = new StringBuffer(item.length() + 2);
        int splitPoint = 0;
        while (splitPoint <= i && classifyCharacter(item.charAt(i)) != 4) {
            splitPoint++;
        }
        newItem.append(openQuote);
        newItem.append(item.substring(0, splitPoint));
        int charOffset = splitPoint;
        while (charOffset < item.length()) {
            newItem.append(item.charAt(charOffset));
            if (classifyCharacter(item.charAt(charOffset)) == 4 && isSurroundingQuote(item.charAt(charOffset), openQuote, closeQuote)) {
                newItem.append(item.charAt(charOffset));
            }
            charOffset++;
        }
        newItem.append(closeQuote);
        return newItem.toString();
    }

    private static boolean isSurroundingQuote(char ch, char openQuote, char closeQuote) {
        return ch == openQuote || isClosingingQuote(ch, openQuote, closeQuote);
    }

    private static boolean isClosingingQuote(char ch, char openQuote, char closeQuote) {
        return ch == closeQuote || ((openQuote == 12317 && ch == 12318) || ch == 12319);
    }
}
