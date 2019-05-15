package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPError;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;
import com.adobe.xmp.options.PropertyOptions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class ParseRDF implements XMPError, XMPConst {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final String DEFAULT_PREFIX = "_dflt";
    public static final int RDFTERM_ABOUT = 3;
    public static final int RDFTERM_ABOUT_EACH = 10;
    public static final int RDFTERM_ABOUT_EACH_PREFIX = 11;
    public static final int RDFTERM_BAG_ID = 12;
    public static final int RDFTERM_DATATYPE = 7;
    public static final int RDFTERM_DESCRIPTION = 8;
    public static final int RDFTERM_FIRST_CORE = 1;
    public static final int RDFTERM_FIRST_OLD = 10;
    public static final int RDFTERM_FIRST_SYNTAX = 1;
    public static final int RDFTERM_ID = 2;
    public static final int RDFTERM_LAST_CORE = 7;
    public static final int RDFTERM_LAST_OLD = 12;
    public static final int RDFTERM_LAST_SYNTAX = 9;
    public static final int RDFTERM_LI = 9;
    public static final int RDFTERM_NODE_ID = 6;
    public static final int RDFTERM_OTHER = 0;
    public static final int RDFTERM_PARSE_TYPE = 4;
    public static final int RDFTERM_RDF = 1;
    public static final int RDFTERM_RESOURCE = 5;

    static XMPMetaImpl parse(Node xmlRoot) throws XMPException {
        XMPMetaImpl xmp = new XMPMetaImpl();
        rdf_RDF(xmp, xmlRoot);
        return xmp;
    }

    static void rdf_RDF(XMPMetaImpl xmp, Node rdfRdfNode) throws XMPException {
        if (rdfRdfNode.hasAttributes()) {
            rdf_NodeElementList(xmp, xmp.getRoot(), rdfRdfNode);
            return;
        }
        throw new XMPException("Invalid attributes of rdf:RDF element", 202);
    }

    private static void rdf_NodeElementList(XMPMetaImpl xmp, XMPNode xmpParent, Node rdfRdfNode) throws XMPException {
        for (int i = 0; i < rdfRdfNode.getChildNodes().getLength(); i++) {
            Node child = rdfRdfNode.getChildNodes().item(i);
            if (!isWhitespaceNode(child)) {
                rdf_NodeElement(xmp, xmpParent, child, true);
            }
        }
    }

    private static void rdf_NodeElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        int nodeTerm = getRDFTermKind(xmlNode);
        if (nodeTerm != 8 && nodeTerm != 0) {
            throw new XMPException("Node element must be rdf:Description or typed node", 202);
        } else if (isTopLevel && nodeTerm == 0) {
            throw new XMPException("Top level typed node not allowed", 203);
        } else {
            rdf_NodeElementAttrs(xmp, xmpParent, xmlNode, isTopLevel);
            rdf_PropertyElementList(xmp, xmpParent, xmlNode, isTopLevel);
        }
    }

    private static void rdf_NodeElementAttrs(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        int exclusiveAttrs = 0;
        for (int i = 0; i < xmlNode.getAttributes().getLength(); i++) {
            Node attribute = xmlNode.getAttributes().item(i);
            if (!("xmlns".equals(attribute.getPrefix()) || (attribute.getPrefix() == null && "xmlns".equals(attribute.getNodeName())))) {
                int attrTerm = getRDFTermKind(attribute);
                if (attrTerm != 0) {
                    if (attrTerm != 6) {
                        switch (attrTerm) {
                            case 2:
                            case 3:
                                break;
                            default:
                                throw new XMPException("Invalid nodeElement attribute", 202);
                        }
                    }
                    if (exclusiveAttrs <= 0) {
                        exclusiveAttrs++;
                        if (isTopLevel && attrTerm == 3) {
                            if (xmpParent.getName() == null || xmpParent.getName().length() <= 0) {
                                xmpParent.setName(attribute.getNodeValue());
                            } else if (!xmpParent.getName().equals(attribute.getNodeValue())) {
                                throw new XMPException("Mismatched top level rdf:about values", 203);
                            }
                        }
                    } else {
                        throw new XMPException("Mutally exclusive about, ID, nodeID attributes", 202);
                    }
                }
                addChildNode(xmp, xmpParent, attribute, attribute.getNodeValue(), isTopLevel);
            }
        }
    }

    private static void rdf_PropertyElementList(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlParent, boolean isTopLevel) throws XMPException {
        for (int i = 0; i < xmlParent.getChildNodes().getLength(); i++) {
            Node currChild = xmlParent.getChildNodes().item(i);
            if (!isWhitespaceNode(currChild)) {
                if (currChild.getNodeType() == (short) 1) {
                    rdf_PropertyElement(xmp, xmpParent, currChild, isTopLevel);
                } else {
                    throw new XMPException("Expected property element node not found", 202);
                }
            }
        }
    }

    private static void rdf_PropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        if (isPropertyElementName(getRDFTermKind(xmlNode))) {
            int i;
            NamedNodeMap attributes = xmlNode.getAttributes();
            int i2 = 0;
            List nsAttrs = null;
            for (i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                if ("xmlns".equals(attribute.getPrefix()) || (attribute.getPrefix() == null && "xmlns".equals(attribute.getNodeName()))) {
                    if (nsAttrs == null) {
                        nsAttrs = new ArrayList();
                    }
                    nsAttrs.add(attribute.getNodeName());
                }
            }
            if (nsAttrs != null) {
                for (String ns : nsAttrs) {
                    attributes.removeNamedItem(ns);
                }
            }
            if (attributes.getLength() > 3) {
                rdf_EmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
            } else {
                for (i = 0; i < attributes.getLength(); i++) {
                    Node attribute2 = attributes.item(i);
                    String attrLocal = attribute2.getLocalName();
                    String attrNS = attribute2.getNamespaceURI();
                    String attrValue = attribute2.getNodeValue();
                    if (!XMPConst.XML_LANG.equals(attribute2.getNodeName()) || ("ID".equals(attrLocal) && XMPConst.NS_RDF.equals(attrNS))) {
                        if ("datatype".equals(attrLocal) && XMPConst.NS_RDF.equals(attrNS)) {
                            rdf_LiteralPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                        } else if (!"parseType".equals(attrLocal) || !XMPConst.NS_RDF.equals(attrNS)) {
                            rdf_EmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                        } else if ("Literal".equals(attrValue)) {
                            rdf_ParseTypeLiteralPropertyElement();
                        } else if ("Resource".equals(attrValue)) {
                            rdf_ParseTypeResourcePropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                        } else if ("Collection".equals(attrValue)) {
                            rdf_ParseTypeCollectionPropertyElement();
                        } else {
                            rdf_ParseTypeOtherPropertyElement();
                        }
                        return;
                    }
                }
                if (xmlNode.hasChildNodes()) {
                    while (true) {
                        i = i2;
                        if (i >= xmlNode.getChildNodes().getLength()) {
                            rdf_LiteralPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                            break;
                        } else if (xmlNode.getChildNodes().item(i).getNodeType() != (short) 3) {
                            rdf_ResourcePropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                            return;
                        } else {
                            i2 = i + 1;
                        }
                    }
                } else {
                    rdf_EmptyPropertyElement(xmp, xmpParent, xmlNode, isTopLevel);
                }
            }
            return;
        }
        throw new XMPException("Invalid property element name", 202);
    }

    private static void rdf_ResourcePropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        if (!isTopLevel || !"iX:changes".equals(xmlNode.getNodeName())) {
            int i;
            Node attribute;
            XMPNode newCompound = addChildNode(xmp, xmpParent, xmlNode, "", isTopLevel);
            for (i = 0; i < xmlNode.getAttributes().getLength(); i++) {
                attribute = xmlNode.getAttributes().item(i);
                if (!("xmlns".equals(attribute.getPrefix()) || (attribute.getPrefix() == null && "xmlns".equals(attribute.getNodeName())))) {
                    String attrLocal = attribute.getLocalName();
                    String attrNS = attribute.getNamespaceURI();
                    if (XMPConst.XML_LANG.equals(attribute.getNodeName())) {
                        addQualifierNode(newCompound, XMPConst.XML_LANG, attribute.getNodeValue());
                    } else if (!"ID".equals(attrLocal) || !XMPConst.NS_RDF.equals(attrNS)) {
                        throw new XMPException("Invalid attribute for resource property element", 202);
                    }
                }
            }
            boolean found = false;
            attribute = null;
            for (i = 0; i < xmlNode.getChildNodes().getLength(); i++) {
                attribute = xmlNode.getChildNodes().item(i);
                if (!isWhitespaceNode(attribute)) {
                    if (attribute.getNodeType() == (short) 1 && !found) {
                        boolean isRDF = XMPConst.NS_RDF.equals(attribute.getNamespaceURI());
                        String childLocal = attribute.getLocalName();
                        if (isRDF && "Bag".equals(childLocal)) {
                            newCompound.getOptions().setArray(true);
                        } else if (isRDF && "Seq".equals(childLocal)) {
                            newCompound.getOptions().setArray(true).setArrayOrdered(true);
                        } else if (isRDF && "Alt".equals(childLocal)) {
                            newCompound.getOptions().setArray(true).setArrayOrdered(true).setArrayAlternate(true);
                        } else {
                            newCompound.getOptions().setStruct(true);
                            if (!(isRDF || "Description".equals(childLocal))) {
                                String typeName = attribute.getNamespaceURI();
                                if (typeName != null) {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append(typeName);
                                    stringBuilder.append(':');
                                    stringBuilder.append(childLocal);
                                    addQualifierNode(newCompound, XMPConst.RDF_TYPE, stringBuilder.toString());
                                } else {
                                    throw new XMPException("All XML elements must be in a namespace", 203);
                                }
                            }
                        }
                        rdf_NodeElement(xmp, newCompound, attribute, false);
                        if (newCompound.getHasValueChild()) {
                            fixupQualifiedNode(newCompound);
                        } else if (newCompound.getOptions().isArrayAlternate()) {
                            XMPNodeUtils.detectAltText(newCompound);
                        }
                        found = true;
                    } else if (found) {
                        throw new XMPException("Invalid child of resource property element", 202);
                    } else {
                        throw new XMPException("Children of resource property element must be XML elements", 202);
                    }
                }
            }
            if (!found) {
                throw new XMPException("Missing child of resource property element", 202);
            }
        }
    }

    private static void rdf_LiteralPropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        Node attribute;
        XMPNode newChild = addChildNode(xmp, xmpParent, xmlNode, null, isTopLevel);
        int i = 0;
        for (int i2 = 0; i2 < xmlNode.getAttributes().getLength(); i2++) {
            attribute = xmlNode.getAttributes().item(i2);
            if (!("xmlns".equals(attribute.getPrefix()) || (attribute.getPrefix() == null && "xmlns".equals(attribute.getNodeName())))) {
                String attrNS = attribute.getNamespaceURI();
                String attrLocal = attribute.getLocalName();
                if (XMPConst.XML_LANG.equals(attribute.getNodeName())) {
                    addQualifierNode(newChild, XMPConst.XML_LANG, attribute.getNodeValue());
                } else if (!(XMPConst.NS_RDF.equals(attrNS) && ("ID".equals(attrLocal) || "datatype".equals(attrLocal)))) {
                    throw new XMPException("Invalid attribute for literal property element", 202);
                }
            }
        }
        String textValue = "";
        while (i < xmlNode.getChildNodes().getLength()) {
            attribute = xmlNode.getChildNodes().item(i);
            if (attribute.getNodeType() == (short) 3) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(textValue);
                stringBuilder.append(attribute.getNodeValue());
                textValue = stringBuilder.toString();
                i++;
            } else {
                throw new XMPException("Invalid child of literal property element", 202);
            }
        }
        newChild.setValue(textValue);
    }

    private static void rdf_ParseTypeLiteralPropertyElement() throws XMPException {
        throw new XMPException("ParseTypeLiteral property element not allowed", 203);
    }

    private static void rdf_ParseTypeResourcePropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        XMPNode newStruct = addChildNode(xmp, xmpParent, xmlNode, "", isTopLevel);
        newStruct.getOptions().setStruct(true);
        for (int i = 0; i < xmlNode.getAttributes().getLength(); i++) {
            Node attribute = xmlNode.getAttributes().item(i);
            if (!("xmlns".equals(attribute.getPrefix()) || (attribute.getPrefix() == null && "xmlns".equals(attribute.getNodeName())))) {
                String attrLocal = attribute.getLocalName();
                String attrNS = attribute.getNamespaceURI();
                if (XMPConst.XML_LANG.equals(attribute.getNodeName())) {
                    addQualifierNode(newStruct, XMPConst.XML_LANG, attribute.getNodeValue());
                } else if (!(XMPConst.NS_RDF.equals(attrNS) && ("ID".equals(attrLocal) || "parseType".equals(attrLocal)))) {
                    throw new XMPException("Invalid attribute for ParseTypeResource property element", 202);
                }
            }
        }
        rdf_PropertyElementList(xmp, newStruct, xmlNode, false);
        if (newStruct.getHasValueChild()) {
            fixupQualifiedNode(newStruct);
        }
    }

    private static void rdf_ParseTypeCollectionPropertyElement() throws XMPException {
        throw new XMPException("ParseTypeCollection property element not allowed", 203);
    }

    private static void rdf_ParseTypeOtherPropertyElement() throws XMPException {
        throw new XMPException("ParseTypeOther property element not allowed", 203);
    }

    private static void rdf_EmptyPropertyElement(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, boolean isTopLevel) throws XMPException {
        XMPMetaImpl xMPMetaImpl = xmp;
        boolean hasResourceAttr = false;
        boolean hasValueAttr = false;
        Node valueNode = null;
        if (xmlNode.hasChildNodes()) {
            XMPNode xMPNode = xmpParent;
            Node node = xmlNode;
            boolean z = isTopLevel;
            throw new XMPException("Nested content not allowed with rdf:resource or property attributes", 202);
        }
        boolean hasNodeIDAttr = false;
        boolean hasNodeIDAttr2 = false;
        for (int i = 0; i < xmlNode.getAttributes().getLength(); i++) {
            Node attribute = xmlNode.getAttributes().item(i);
            if (!("xmlns".equals(attribute.getPrefix()) || (attribute.getPrefix() == null && "xmlns".equals(attribute.getNodeName())))) {
                int attrTerm = getRDFTermKind(attribute);
                if (attrTerm != 0) {
                    if (attrTerm != 2) {
                        switch (attrTerm) {
                            case 5:
                                if (hasNodeIDAttr) {
                                    throw new XMPException("Empty property element can't have both rdf:resource and rdf:nodeID", 202);
                                } else if (!hasValueAttr) {
                                    hasResourceAttr = true;
                                    if (!hasValueAttr) {
                                        valueNode = attribute;
                                        break;
                                    }
                                    break;
                                } else {
                                    throw new XMPException("Empty property element can't have both rdf:value and rdf:resource", 203);
                                }
                            case 6:
                                if (!hasResourceAttr) {
                                    hasNodeIDAttr = true;
                                    break;
                                }
                                throw new XMPException("Empty property element can't have both rdf:resource and rdf:nodeID", 202);
                            default:
                                throw new XMPException("Unrecognized attribute of empty property element", 202);
                        }
                    }
                    continue;
                } else if ("value".equals(attribute.getLocalName()) && XMPConst.NS_RDF.equals(attribute.getNamespaceURI())) {
                    if (hasResourceAttr) {
                        throw new XMPException("Empty property element can't have both rdf:value and rdf:resource", 203);
                    }
                    hasValueAttr = true;
                    valueNode = attribute;
                } else if (!XMPConst.XML_LANG.equals(attribute.getNodeName())) {
                    hasNodeIDAttr2 = true;
                }
            }
        }
        boolean hasPropertyAttrs = addChildNode(xMPMetaImpl, xmpParent, xmlNode, "", isTopLevel);
        boolean childIsStruct = false;
        if (hasValueAttr || hasResourceAttr) {
            hasPropertyAttrs.setValue(valueNode != null ? valueNode.getNodeValue() : "");
            if (!hasValueAttr) {
                hasPropertyAttrs.getOptions().setURI(true);
            }
        } else if (hasNodeIDAttr2) {
            hasPropertyAttrs.getOptions().setStruct(true);
            childIsStruct = true;
        }
        int i2 = 0;
        while (i2 < xmlNode.getAttributes().getLength()) {
            boolean hasResourceAttr2;
            Node attribute2 = xmlNode.getAttributes().item(i2);
            if (attribute2 == valueNode || "xmlns".equals(attribute2.getPrefix())) {
                hasResourceAttr2 = hasResourceAttr;
            } else if (attribute2.getPrefix() == null && "xmlns".equals(attribute2.getNodeName())) {
                hasResourceAttr2 = hasResourceAttr;
            } else {
                int attrTerm2 = getRDFTermKind(attribute2);
                if (attrTerm2 != 0) {
                    if (attrTerm2 != 2) {
                        switch (attrTerm2) {
                            case 5:
                                hasResourceAttr2 = hasResourceAttr;
                                addQualifierNode(hasPropertyAttrs, "rdf:resource", attribute2.getNodeValue());
                                break;
                            case 6:
                                break;
                            default:
                                hasResourceAttr2 = hasResourceAttr;
                                throw new XMPException("Unrecognized attribute of empty property element", true);
                        }
                    }
                    hasResourceAttr2 = hasResourceAttr;
                } else {
                    hasResourceAttr2 = hasResourceAttr;
                    if (!childIsStruct) {
                        addQualifierNode(hasPropertyAttrs, attribute2.getNodeName(), attribute2.getNodeValue());
                    } else if (XMPConst.XML_LANG.equals(attribute2.getNodeName())) {
                        addQualifierNode(hasPropertyAttrs, XMPConst.XML_LANG, attribute2.getNodeValue());
                    } else {
                        addChildNode(xMPMetaImpl, hasPropertyAttrs, attribute2, attribute2.getNodeValue(), false);
                    }
                }
            }
            i2++;
            hasResourceAttr = hasResourceAttr2;
        }
    }

    private static XMPNode addChildNode(XMPMetaImpl xmp, XMPNode xmpParent, Node xmlNode, String value, boolean isTopLevel) throws XMPException {
        XMPSchemaRegistry registry = XMPMetaFactory.getSchemaRegistry();
        String namespace = xmlNode.getNamespaceURI();
        if (namespace != null) {
            if (XMPConst.NS_DC_DEPRECATED.equals(namespace)) {
                namespace = XMPConst.NS_DC;
            }
            String prefix = registry.getNamespacePrefix(namespace);
            if (prefix == null) {
                prefix = registry.registerNamespace(namespace, xmlNode.getPrefix() != null ? xmlNode.getPrefix() : DEFAULT_PREFIX);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append(xmlNode.getLocalName());
            prefix = stringBuilder.toString();
            PropertyOptions childOptions = new PropertyOptions();
            boolean isAlias = false;
            if (isTopLevel) {
                XMPNode schemaNode = XMPNodeUtils.findSchemaNode(xmp.getRoot(), namespace, DEFAULT_PREFIX, true);
                schemaNode.setImplicit(false);
                xmpParent = schemaNode;
                if (registry.findAlias(prefix) != null) {
                    isAlias = true;
                    xmp.getRoot().setHasAliases(true);
                    schemaNode.setHasAliases(true);
                }
            }
            boolean isArrayItem = "rdf:li".equals(prefix);
            boolean isValueNode = "rdf:value".equals(prefix);
            XMPNode newChild = new XMPNode(prefix, value, childOptions);
            newChild.setAlias(isAlias);
            if (isValueNode) {
                xmpParent.addChild(1, newChild);
            } else {
                xmpParent.addChild(newChild);
            }
            if (isValueNode) {
                if (isTopLevel || !xmpParent.getOptions().isStruct()) {
                    throw new XMPException("Misplaced rdf:value element", 202);
                }
                xmpParent.setHasValueChild(true);
            }
            if (isArrayItem) {
                if (xmpParent.getOptions().isArray()) {
                    newChild.setName(XMPConst.ARRAY_ITEM_NAME);
                } else {
                    throw new XMPException("Misplaced rdf:li element", 202);
                }
            }
            return newChild;
        }
        throw new XMPException("XML namespace required for all elements and attributes", 202);
    }

    private static XMPNode addQualifierNode(XMPNode xmpParent, String name, String value) throws XMPException {
        XMPNode newQual = new XMPNode(name, XMPConst.XML_LANG.equals(name) ? Utils.normalizeLangValue(value) : value, null);
        xmpParent.addQualifier(newQual);
        return newQual;
    }

    private static void fixupQualifiedNode(XMPNode xmpParent) throws XMPException {
        int i = 1;
        XMPNode valueNode = xmpParent.getChild(1);
        if (valueNode.getOptions().getHasLanguage()) {
            if (xmpParent.getOptions().getHasLanguage()) {
                throw new XMPException("Redundant xml:lang for rdf:value element", 203);
            }
            XMPNode langQual = valueNode.getQualifier(1);
            valueNode.removeQualifier(langQual);
            xmpParent.addQualifier(langQual);
        }
        while (i <= valueNode.getQualifierLength()) {
            xmpParent.addQualifier(valueNode.getQualifier(i));
            i++;
        }
        for (i = 2; i <= xmpParent.getChildrenLength(); i++) {
            xmpParent.addQualifier(xmpParent.getChild(i));
        }
        xmpParent.setHasValueChild(false);
        xmpParent.getOptions().setStruct(false);
        xmpParent.getOptions().mergeWith(valueNode.getOptions());
        xmpParent.setValue(valueNode.getValue());
        xmpParent.removeChildren();
        Iterator it = valueNode.iterateChildren();
        while (it.hasNext()) {
            xmpParent.addChild((XMPNode) it.next());
        }
    }

    private static boolean isWhitespaceNode(Node node) {
        if (node.getNodeType() != (short) 3) {
            return false;
        }
        String value = node.getNodeValue();
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPropertyElementName(int term) {
        if (term == 8 || isOldTerm(term)) {
            return false;
        }
        return isCoreSyntaxTerm(term) ^ 1;
    }

    private static boolean isOldTerm(int term) {
        return 10 <= term && term <= 12;
    }

    private static boolean isCoreSyntaxTerm(int term) {
        return 1 <= term && term <= 7;
    }

    private static int getRDFTermKind(Node node) {
        String localName = node.getLocalName();
        String namespace = node.getNamespaceURI();
        if (namespace == null && (("about".equals(localName) || "ID".equals(localName)) && (node instanceof Attr) && XMPConst.NS_RDF.equals(((Attr) node).getOwnerElement().getNamespaceURI()))) {
            namespace = XMPConst.NS_RDF;
        }
        if (XMPConst.NS_RDF.equals(namespace)) {
            if ("li".equals(localName)) {
                return 9;
            }
            if ("parseType".equals(localName)) {
                return 4;
            }
            if ("Description".equals(localName)) {
                return 8;
            }
            if ("about".equals(localName)) {
                return 3;
            }
            if ("resource".equals(localName)) {
                return 5;
            }
            if ("RDF".equals(localName)) {
                return 1;
            }
            if ("ID".equals(localName)) {
                return 2;
            }
            if ("nodeID".equals(localName)) {
                return 6;
            }
            if ("datatype".equals(localName)) {
                return 7;
            }
            if ("aboutEach".equals(localName)) {
                return 10;
            }
            if ("aboutEachPrefix".equals(localName)) {
                return 11;
            }
            if ("bagID".equals(localName)) {
                return 12;
            }
        }
        return 0;
    }
}
