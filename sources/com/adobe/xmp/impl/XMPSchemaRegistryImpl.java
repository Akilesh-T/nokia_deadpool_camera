package com.adobe.xmp.impl;

import com.adobe.xmp.XMPConst;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPSchemaRegistry;
import com.adobe.xmp.options.AliasOptions;
import com.adobe.xmp.properties.XMPAliasInfo;
import com.hmdglobal.app.camera.gdepthfilter.GDepth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class XMPSchemaRegistryImpl implements XMPSchemaRegistry, XMPConst {
    private Map aliasMap = new HashMap();
    private Map namespaceToPrefixMap = new HashMap();
    private Pattern p = Pattern.compile("[/*?\\[\\]]");
    private Map prefixToNamespaceMap = new HashMap();

    public XMPSchemaRegistryImpl() {
        try {
            registerStandardNamespaces();
            registerStandardAliases();
        } catch (XMPException e) {
            throw new RuntimeException("The XMPSchemaRegistry cannot be initialized!");
        }
    }

    public synchronized String registerNamespace(String namespaceURI, String suggestedPrefix) throws XMPException {
        ParameterAsserts.assertSchemaNS(namespaceURI);
        ParameterAsserts.assertPrefix(suggestedPrefix);
        if (suggestedPrefix.charAt(suggestedPrefix.length() - 1) != ':') {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(suggestedPrefix);
            stringBuilder.append(':');
            suggestedPrefix = stringBuilder.toString();
        }
        if (Utils.isXMLNameNS(suggestedPrefix.substring(0, suggestedPrefix.length() - 1))) {
            String registeredPrefix = (String) this.namespaceToPrefixMap.get(namespaceURI);
            String registeredNS = (String) this.prefixToNamespaceMap.get(suggestedPrefix);
            if (registeredPrefix != null) {
                return registeredPrefix;
            }
            if (registeredNS != null) {
                String generatedPrefix = suggestedPrefix;
                int i = 1;
                while (this.prefixToNamespaceMap.containsKey(generatedPrefix)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(suggestedPrefix.substring(0, suggestedPrefix.length() - 1));
                    stringBuilder2.append("_");
                    stringBuilder2.append(i);
                    stringBuilder2.append("_:");
                    generatedPrefix = stringBuilder2.toString();
                    i++;
                }
                suggestedPrefix = generatedPrefix;
            }
            this.prefixToNamespaceMap.put(suggestedPrefix, namespaceURI);
            this.namespaceToPrefixMap.put(namespaceURI, suggestedPrefix);
            return suggestedPrefix;
        }
        throw new XMPException("The prefix is a bad XML name", 201);
    }

    public synchronized void deleteNamespace(String namespaceURI) {
        String prefixToDelete = getNamespacePrefix(namespaceURI);
        if (prefixToDelete != null) {
            this.namespaceToPrefixMap.remove(namespaceURI);
            this.prefixToNamespaceMap.remove(prefixToDelete);
        }
    }

    public synchronized String getNamespacePrefix(String namespaceURI) {
        return (String) this.namespaceToPrefixMap.get(namespaceURI);
    }

    public synchronized String getNamespaceURI(String namespacePrefix) {
        if (namespacePrefix != null) {
            try {
                if (!namespacePrefix.endsWith(":")) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(namespacePrefix);
                    stringBuilder.append(":");
                    namespacePrefix = stringBuilder.toString();
                }
            } finally {
            }
        }
        return (String) this.prefixToNamespaceMap.get(namespacePrefix);
    }

    public synchronized Map getNamespaces() {
        return Collections.unmodifiableMap(new TreeMap(this.namespaceToPrefixMap));
    }

    public synchronized Map getPrefixes() {
        return Collections.unmodifiableMap(new TreeMap(this.prefixToNamespaceMap));
    }

    private void registerStandardNamespaces() throws XMPException {
        registerNamespace(XMPConst.NS_XML, "xml");
        registerNamespace(XMPConst.NS_RDF, "rdf");
        registerNamespace(XMPConst.NS_DC, "dc");
        registerNamespace(XMPConst.NS_IPTCCORE, "Iptc4xmpCore");
        registerNamespace(XMPConst.NS_X, "x");
        registerNamespace(XMPConst.NS_IX, "iX");
        registerNamespace(XMPConst.NS_XMP, "xmp");
        registerNamespace(XMPConst.NS_XMP_RIGHTS, "xmpRights");
        registerNamespace(XMPConst.NS_XMP_MM, "xmpMM");
        registerNamespace(XMPConst.NS_XMP_BJ, "xmpBJ");
        registerNamespace(XMPConst.NS_XMP_NOTE, "xmpNote");
        registerNamespace(XMPConst.NS_PDF, "pdf");
        registerNamespace(XMPConst.NS_PDFX, "pdfx");
        registerNamespace(XMPConst.NS_PDFX_ID, "pdfxid");
        registerNamespace(XMPConst.NS_PDFA_SCHEMA, "pdfaSchema");
        registerNamespace(XMPConst.NS_PDFA_PROPERTY, "pdfaProperty");
        registerNamespace(XMPConst.NS_PDFA_TYPE, "pdfaType");
        registerNamespace(XMPConst.NS_PDFA_FIELD, "pdfaField");
        registerNamespace(XMPConst.NS_PDFA_ID, "pdfaid");
        registerNamespace(XMPConst.NS_PDFA_EXTENSION, "pdfaExtension");
        registerNamespace(XMPConst.NS_PHOTOSHOP, "photoshop");
        registerNamespace(XMPConst.NS_PSALBUM, "album");
        registerNamespace(XMPConst.NS_EXIF, "exif");
        registerNamespace(XMPConst.NS_EXIF_AUX, "aux");
        registerNamespace(XMPConst.NS_TIFF, "tiff");
        registerNamespace(XMPConst.NS_PNG, "png");
        registerNamespace(XMPConst.NS_JPEG, "jpeg");
        registerNamespace(XMPConst.NS_JP2K, "jp2k");
        registerNamespace(XMPConst.NS_CAMERARAW, "crs");
        registerNamespace(XMPConst.NS_ADOBESTOCKPHOTO, "bmsp");
        registerNamespace(XMPConst.NS_CREATOR_ATOM, "creatorAtom");
        registerNamespace(XMPConst.NS_ASF, "asf");
        registerNamespace(XMPConst.NS_WAV, "wav");
        registerNamespace(XMPConst.NS_DM, "xmpDM");
        registerNamespace(XMPConst.NS_TRANSIENT, "xmpx");
        registerNamespace(XMPConst.TYPE_TEXT, "xmpT");
        registerNamespace(XMPConst.TYPE_PAGEDFILE, "xmpTPg");
        registerNamespace(XMPConst.TYPE_GRAPHICS, "xmpG");
        registerNamespace(XMPConst.TYPE_IMAGE, "xmpGImg");
        registerNamespace(XMPConst.TYPE_FONT, "stFNT");
        registerNamespace(XMPConst.TYPE_DIMENSIONS, "stDim");
        registerNamespace(XMPConst.TYPE_RESOURCEEVENT, "stEvt");
        registerNamespace(XMPConst.TYPE_RESOURCEREF, "stRef");
        registerNamespace(XMPConst.TYPE_ST_VERSION, "stVer");
        registerNamespace(XMPConst.TYPE_ST_JOB, "stJob");
        registerNamespace(XMPConst.TYPE_MANIFESTITEM, "stMfs");
        registerNamespace(XMPConst.TYPE_IDENTIFIERQUAL, "xmpidq");
    }

    public synchronized XMPAliasInfo resolveAlias(String aliasNS, String aliasProp) {
        String aliasPrefix = getNamespacePrefix(aliasNS);
        if (aliasPrefix == null) {
            return null;
        }
        Map map = this.aliasMap;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(aliasPrefix);
        stringBuilder.append(aliasProp);
        return (XMPAliasInfo) map.get(stringBuilder.toString());
    }

    public synchronized XMPAliasInfo findAlias(String qname) {
        return (XMPAliasInfo) this.aliasMap.get(qname);
    }

    public synchronized XMPAliasInfo[] findAliases(String aliasNS) {
        List result;
        String prefix = getNamespacePrefix(aliasNS);
        result = new ArrayList();
        if (prefix != null) {
            for (String qname : this.aliasMap.keySet()) {
                if (qname.startsWith(prefix)) {
                    result.add(findAlias(qname));
                }
            }
        }
        return (XMPAliasInfo[]) result.toArray(new XMPAliasInfo[result.size()]);
    }

    /* Access modifiers changed, original: declared_synchronized */
    public synchronized void registerAlias(String aliasNS, String aliasProp, String actualNS, String actualProp, AliasOptions aliasForm) throws XMPException {
        AliasOptions aliasOptions;
        ParameterAsserts.assertSchemaNS(aliasNS);
        ParameterAsserts.assertPropName(aliasProp);
        ParameterAsserts.assertSchemaNS(actualNS);
        ParameterAsserts.assertPropName(actualProp);
        if (aliasForm != null) {
            aliasOptions = new AliasOptions(XMPNodeUtils.verifySetOptions(aliasForm.toPropertyOptions(), null).getOptions());
        } else {
            aliasOptions = new AliasOptions();
        }
        final AliasOptions aliasOpts = aliasOptions;
        if (this.p.matcher(aliasProp).find() || this.p.matcher(actualProp).find()) {
            throw new XMPException("Alias and actual property names must be simple", 102);
        }
        String aliasPrefix = getNamespacePrefix(aliasNS);
        String actualPrefix = getNamespacePrefix(actualNS);
        if (aliasPrefix == null) {
            throw new XMPException("Alias namespace is not registered", 101);
        } else if (actualPrefix != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(aliasPrefix);
            stringBuilder.append(aliasProp);
            String key = stringBuilder.toString();
            if (this.aliasMap.containsKey(key)) {
                throw new XMPException("Alias is already existing", 4);
            }
            Map map = this.aliasMap;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(actualPrefix);
            stringBuilder2.append(actualProp);
            if (map.containsKey(stringBuilder2.toString())) {
                throw new XMPException("Actual property is already an alias, use the base property", 4);
            }
            final String str = actualNS;
            final String str2 = actualPrefix;
            final String str3 = actualProp;
            this.aliasMap.put(key, new XMPAliasInfo() {
                public String getNamespace() {
                    return str;
                }

                public String getPrefix() {
                    return str2;
                }

                public String getPropName() {
                    return str3;
                }

                public AliasOptions getAliasForm() {
                    return aliasOpts;
                }

                public String toString() {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(str2);
                    stringBuilder.append(str3);
                    stringBuilder.append(" NS(");
                    stringBuilder.append(str);
                    stringBuilder.append("), FORM (");
                    stringBuilder.append(getAliasForm());
                    stringBuilder.append(")");
                    return stringBuilder.toString();
                }
            });
        } else {
            throw new XMPException("Actual namespace is not registered", 101);
        }
    }

    public synchronized Map getAliases() {
        return Collections.unmodifiableMap(new TreeMap(this.aliasMap));
    }

    private void registerStandardAliases() throws XMPException {
        AliasOptions aliasToArrayOrdered = new AliasOptions().setArrayOrdered(true);
        AliasOptions aliasToArrayAltText = new AliasOptions().setArrayAltText(true);
        registerAlias(XMPConst.NS_XMP, "Author", XMPConst.NS_DC, "creator", aliasToArrayOrdered);
        registerAlias(XMPConst.NS_XMP, "Authors", XMPConst.NS_DC, "creator", null);
        registerAlias(XMPConst.NS_XMP, "Description", XMPConst.NS_DC, "description", null);
        registerAlias(XMPConst.NS_XMP, GDepth.PROPERTY_FORMAT, XMPConst.NS_DC, "format", null);
        registerAlias(XMPConst.NS_XMP, "Keywords", XMPConst.NS_DC, "subject", null);
        registerAlias(XMPConst.NS_XMP, "Locale", XMPConst.NS_DC, "language", null);
        registerAlias(XMPConst.NS_XMP, "Title", XMPConst.NS_DC, "title", null);
        registerAlias(XMPConst.NS_XMP_RIGHTS, "Copyright", XMPConst.NS_DC, "rights", null);
        registerAlias(XMPConst.NS_PDF, "Author", XMPConst.NS_DC, "creator", aliasToArrayOrdered);
        registerAlias(XMPConst.NS_PDF, "BaseURL", XMPConst.NS_XMP, "BaseURL", null);
        registerAlias(XMPConst.NS_PDF, "CreationDate", XMPConst.NS_XMP, "CreateDate", null);
        registerAlias(XMPConst.NS_PDF, "Creator", XMPConst.NS_XMP, "CreatorTool", null);
        registerAlias(XMPConst.NS_PDF, "ModDate", XMPConst.NS_XMP, "ModifyDate", null);
        AliasOptions aliasOptions = aliasToArrayAltText;
        registerAlias(XMPConst.NS_PDF, "Subject", XMPConst.NS_DC, "description", aliasOptions);
        registerAlias(XMPConst.NS_PDF, "Title", XMPConst.NS_DC, "title", aliasOptions);
        registerAlias(XMPConst.NS_PHOTOSHOP, "Author", XMPConst.NS_DC, "creator", aliasToArrayOrdered);
        registerAlias(XMPConst.NS_PHOTOSHOP, "Caption", XMPConst.NS_DC, "description", aliasOptions);
        registerAlias(XMPConst.NS_PHOTOSHOP, "Copyright", XMPConst.NS_DC, "rights", aliasOptions);
        registerAlias(XMPConst.NS_PHOTOSHOP, "Keywords", XMPConst.NS_DC, "subject", null);
        registerAlias(XMPConst.NS_PHOTOSHOP, "Marked", XMPConst.NS_XMP_RIGHTS, "Marked", null);
        registerAlias(XMPConst.NS_PHOTOSHOP, "Title", XMPConst.NS_DC, "title", aliasToArrayAltText);
        registerAlias(XMPConst.NS_PHOTOSHOP, "WebStatement", XMPConst.NS_XMP_RIGHTS, "WebStatement", null);
        registerAlias(XMPConst.NS_TIFF, "Artist", XMPConst.NS_DC, "creator", aliasToArrayOrdered);
        registerAlias(XMPConst.NS_TIFF, "Copyright", XMPConst.NS_DC, "rights", null);
        registerAlias(XMPConst.NS_TIFF, "DateTime", XMPConst.NS_XMP, "ModifyDate", null);
        registerAlias(XMPConst.NS_TIFF, "ImageDescription", XMPConst.NS_DC, "description", null);
        registerAlias(XMPConst.NS_TIFF, "Software", XMPConst.NS_XMP, "CreatorTool", null);
        registerAlias(XMPConst.NS_PNG, "Author", XMPConst.NS_DC, "creator", aliasToArrayOrdered);
        registerAlias(XMPConst.NS_PNG, "Copyright", XMPConst.NS_DC, "rights", aliasToArrayAltText);
        registerAlias(XMPConst.NS_PNG, "CreationTime", XMPConst.NS_XMP, "CreateDate", null);
        registerAlias(XMPConst.NS_PNG, "Description", XMPConst.NS_DC, "description", aliasToArrayAltText);
        registerAlias(XMPConst.NS_PNG, "ModificationTime", XMPConst.NS_XMP, "ModifyDate", null);
        registerAlias(XMPConst.NS_PNG, "Software", XMPConst.NS_XMP, "CreatorTool", null);
        registerAlias(XMPConst.NS_PNG, "Title", XMPConst.NS_DC, "title", aliasToArrayAltText);
    }
}
