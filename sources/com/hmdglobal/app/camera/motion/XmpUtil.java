package com.hmdglobal.app.camera.motion;

import android.util.Log;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.options.SerializeOptions;
import com.bumptech.glide.load.Key;
import com.hmdglobal.app.camera.Storage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class XmpUtil {
    private static final String GOOGLE_PANO_NAMESPACE = "http://ns.google.com/photos/1.0/camera/";
    private static final int MAX_XMP_BUFFER_SIZE = 65502;
    private static final int M_APP1 = 225;
    private static final int M_SOI = 216;
    private static final int M_SOS = 218;
    private static final String PANO_PREFIX = "GCamera";
    private static final String TAG = "XmpUtil";
    private static final String XMP_HEADER = "http://ns.adobe.com/xap/1.0/\u0000";
    private static final int XMP_HEADER_SIZE = 29;

    private static class Section {
        public byte[] data;
        public int length;
        public int marker;

        private Section() {
        }
    }

    static {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace("http://ns.google.com/photos/1.0/camera/", PANO_PREFIX);
        } catch (XMPException e) {
            e.printStackTrace();
        }
    }

    public static XMPMeta extractXMPMeta(String filename) {
        if (filename.toLowerCase().endsWith(Storage.JPEG_POSTFIX) || filename.toLowerCase().endsWith(".jpeg")) {
            try {
                return extractXMPMeta(new FileInputStream(filename));
            } catch (FileNotFoundException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Could not read file: ");
                stringBuilder.append(filename);
                Log.e(str, stringBuilder.toString(), e);
                return null;
            }
        }
        Log.d(TAG, "XMP parse: only jpeg file is supported");
        return null;
    }

    public static XMPMeta extractXMPMeta(InputStream is) {
        List<Section> sections = parse(is, true);
        if (sections == null) {
            return null;
        }
        for (Section section : sections) {
            if (hasXMPHeader(section.data)) {
                byte[] buffer = new byte[(getXMPContentEnd(section.data) - 29)];
                System.arraycopy(section.data, 29, buffer, 0, buffer.length);
                try {
                    return XMPMetaFactory.parseFromBuffer(buffer);
                } catch (XMPException e) {
                    Log.d(TAG, "XMP parse error", e);
                    return null;
                }
            }
        }
        return null;
    }

    public static XMPMeta createXMPMeta() {
        return XMPMetaFactory.create();
    }

    public static XMPMeta extractOrCreateXMPMeta(String filename) {
        XMPMeta meta = extractXMPMeta(filename);
        return meta == null ? createXMPMeta() : meta;
    }

    public static boolean writeXMPMeta(String filename, XMPMeta meta) {
        if (filename == null || filename.toLowerCase().endsWith(Storage.JPEG_POSTFIX) || filename.toLowerCase().endsWith(".jpeg")) {
            FileOutputStream os = null;
            List<Section> sections = null;
            try {
                sections = insertXMPSection(parse(new FileInputStream(filename), false), meta);
                if (sections == null) {
                    return false;
                }
                try {
                    os = new FileOutputStream(filename);
                    writeJpegFile(os, sections);
                    try {
                        os.close();
                    } catch (IOException e) {
                    }
                    return true;
                } catch (IOException e2) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Write file failed:");
                    stringBuilder.append(filename);
                    Log.d(str, stringBuilder.toString(), e2);
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e3) {
                        }
                    }
                    return false;
                } catch (Throwable th) {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (IOException e4) {
                        }
                    }
                }
            } catch (FileNotFoundException e5) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Could not read file: ");
                stringBuilder2.append(filename);
                Log.e(str2, stringBuilder2.toString(), e5);
                return false;
            }
        }
        Log.d(TAG, "XMP parse: only jpeg file is supported");
        return false;
    }

    public static boolean writeXMPMeta(InputStream inputStream, OutputStream outputStream, XMPMeta meta) {
        List<Section> sections = insertXMPSection(parse(inputStream, false), meta);
        if (sections == null) {
            return false;
        }
        try {
            writeJpegFile(outputStream, sections);
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
            return true;
        } catch (IOException e2) {
            Log.d(TAG, "Write to stream failed", e2);
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e3) {
                }
            }
            return false;
        } catch (Throwable th) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    private static void writeJpegFile(OutputStream os, List<Section> sections) throws IOException {
        os.write(255);
        os.write(M_SOI);
        for (Section section : sections) {
            os.write(255);
            os.write(section.marker);
            if (section.length > 0) {
                int ll = section.length & 255;
                os.write(section.length >> 8);
                os.write(ll);
            }
            os.write(section.data);
        }
    }

    private static List<Section> insertXMPSection(List<Section> sections, XMPMeta meta) {
        if (sections != null) {
            int position = 1;
            if (sections.size() > 1) {
                try {
                    SerializeOptions options = new SerializeOptions();
                    options.setUseCompactFormat(true);
                    options.setOmitPacketWrapper(true);
                    byte[] buffer = XMPMetaFactory.serializeToBuffer(meta, options);
                    if (buffer.length > MAX_XMP_BUFFER_SIZE) {
                        return null;
                    }
                    byte[] xmpdata = new byte[(buffer.length + 29)];
                    System.arraycopy(XMP_HEADER.getBytes(), 0, xmpdata, 0, 29);
                    System.arraycopy(buffer, 0, xmpdata, 29, buffer.length);
                    Section xmpSection = new Section();
                    xmpSection.marker = M_APP1;
                    xmpSection.length = xmpdata.length + 2;
                    xmpSection.data = xmpdata;
                    int i = 0;
                    while (i < sections.size()) {
                        if (((Section) sections.get(i)).marker == M_APP1 && hasXMPHeader(((Section) sections.get(i)).data)) {
                            sections.set(i, xmpSection);
                            return sections;
                        }
                        i++;
                    }
                    List<Section> newSections = new ArrayList();
                    if (((Section) sections.get(0)).marker != M_APP1) {
                        position = 0;
                    }
                    newSections.addAll(sections.subList(0, position));
                    newSections.add(xmpSection);
                    newSections.addAll(sections.subList(position, sections.size()));
                    return newSections;
                } catch (XMPException e) {
                    Log.d(TAG, "Serialize xmp failed", e);
                    return null;
                }
            }
        }
        return null;
    }

    private static boolean hasXMPHeader(byte[] data) {
        if (data.length < 29) {
            return false;
        }
        try {
            byte[] header = new byte[29];
            System.arraycopy(data, 0, header, 0, 29);
            if (new String(header, Key.STRING_CHARSET_NAME).equals(XMP_HEADER)) {
                return true;
            }
            return false;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    private static int getXMPContentEnd(byte[] data) {
        int i = data.length - 1;
        while (i >= 1) {
            if (data[i] == (byte) 62 && data[i - 1] != (byte) 63) {
                return i + 1;
            }
            i--;
        }
        return data.length;
    }

    /* JADX WARNING: Missing block: B:30:0x0044, code skipped:
            if (r13 != false) goto L_0x0063;
     */
    /* JADX WARNING: Missing block: B:32:?, code skipped:
            r2 = new com.hmdglobal.app.camera.motion.XmpUtil.Section();
            r2.marker = r3;
            r2.length = -1;
            r2.data = new byte[r12.available()];
            r12.read(r2.data, 0, r2.data.length);
            r1.add(r2);
     */
    /* JADX WARNING: Missing block: B:33:0x0063, code skipped:
            if (r12 == null) goto L_0x006a;
     */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r12.close();
     */
    private static java.util.List<com.hmdglobal.app.camera.motion.XmpUtil.Section> parse(java.io.InputStream r12, boolean r13) {
        /*
        r0 = 0;
        r1 = r12.read();	 Catch:{ IOException -> 0x00c2 }
        r2 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        if (r1 != r2) goto L_0x00b7;
    L_0x0009:
        r1 = r12.read();	 Catch:{ IOException -> 0x00c2 }
        r3 = 216; // 0xd8 float:3.03E-43 double:1.067E-321;
        if (r1 == r3) goto L_0x0013;
    L_0x0011:
        goto L_0x00b7;
    L_0x0013:
        r1 = new java.util.ArrayList;	 Catch:{ IOException -> 0x00c2 }
        r1.<init>();	 Catch:{ IOException -> 0x00c2 }
    L_0x0018:
        r3 = r12.read();	 Catch:{ IOException -> 0x00c2 }
        r4 = r3;
        r5 = -1;
        if (r3 == r5) goto L_0x00ae;
    L_0x0020:
        if (r4 == r2) goto L_0x002b;
        if (r12 == 0) goto L_0x002a;
    L_0x0025:
        r12.close();	 Catch:{ IOException -> 0x0029 }
        goto L_0x002a;
    L_0x0029:
        r2 = move-exception;
    L_0x002a:
        return r0;
    L_0x002b:
        r3 = r12.read();	 Catch:{ IOException -> 0x00c2 }
        r4 = r3;
        if (r3 != r2) goto L_0x0033;
    L_0x0032:
        goto L_0x002b;
    L_0x0033:
        if (r4 != r5) goto L_0x003e;
        if (r12 == 0) goto L_0x003d;
    L_0x0038:
        r12.close();	 Catch:{ IOException -> 0x003c }
        goto L_0x003d;
    L_0x003c:
        r2 = move-exception;
    L_0x003d:
        return r0;
    L_0x003e:
        r3 = r4;
        r6 = 218; // 0xda float:3.05E-43 double:1.077E-321;
        r7 = 0;
        if (r3 != r6) goto L_0x006b;
    L_0x0044:
        if (r13 != 0) goto L_0x0062;
    L_0x0046:
        r2 = new com.hmdglobal.app.camera.motion.XmpUtil$Section;	 Catch:{ IOException -> 0x00c2 }
        r2.<init>();	 Catch:{ IOException -> 0x00c2 }
        r2.marker = r3;	 Catch:{ IOException -> 0x00c2 }
        r2.length = r5;	 Catch:{ IOException -> 0x00c2 }
        r5 = r12.available();	 Catch:{ IOException -> 0x00c2 }
        r5 = new byte[r5];	 Catch:{ IOException -> 0x00c2 }
        r2.data = r5;	 Catch:{ IOException -> 0x00c2 }
        r5 = r2.data;	 Catch:{ IOException -> 0x00c2 }
        r6 = r2.data;	 Catch:{ IOException -> 0x00c2 }
        r6 = r6.length;	 Catch:{ IOException -> 0x00c2 }
        r12.read(r5, r7, r6);	 Catch:{ IOException -> 0x00c2 }
        r1.add(r2);	 Catch:{ IOException -> 0x00c2 }
        if (r12 == 0) goto L_0x006a;
    L_0x0065:
        r12.close();	 Catch:{ IOException -> 0x0069 }
        goto L_0x006a;
    L_0x0069:
        r0 = move-exception;
    L_0x006a:
        return r1;
    L_0x006b:
        r6 = r12.read();	 Catch:{ IOException -> 0x00c2 }
        r8 = r12.read();	 Catch:{ IOException -> 0x00c2 }
        if (r6 == r5) goto L_0x00a5;
    L_0x0075:
        if (r8 != r5) goto L_0x0078;
    L_0x0077:
        goto L_0x00a5;
    L_0x0078:
        r5 = r6 << 8;
        r5 = r5 | r8;
        if (r13 == 0) goto L_0x0089;
    L_0x007d:
        r9 = 225; // 0xe1 float:3.15E-43 double:1.11E-321;
        if (r4 != r9) goto L_0x0082;
    L_0x0081:
        goto L_0x0089;
    L_0x0082:
        r7 = r5 + -2;
        r9 = (long) r7;	 Catch:{ IOException -> 0x00c2 }
        r12.skip(r9);	 Catch:{ IOException -> 0x00c2 }
        goto L_0x00a3;
    L_0x0089:
        r9 = new com.hmdglobal.app.camera.motion.XmpUtil$Section;	 Catch:{ IOException -> 0x00c2 }
        r9.<init>();	 Catch:{ IOException -> 0x00c2 }
        r9.marker = r3;	 Catch:{ IOException -> 0x00c2 }
        r9.length = r5;	 Catch:{ IOException -> 0x00c2 }
        r10 = r5 + -2;
        r10 = new byte[r10];	 Catch:{ IOException -> 0x00c2 }
        r9.data = r10;	 Catch:{ IOException -> 0x00c2 }
        r10 = r9.data;	 Catch:{ IOException -> 0x00c2 }
        r11 = r5 + -2;
        r12.read(r10, r7, r11);	 Catch:{ IOException -> 0x00c2 }
        r1.add(r9);	 Catch:{ IOException -> 0x00c2 }
    L_0x00a3:
        goto L_0x0018;
        if (r12 == 0) goto L_0x00ad;
    L_0x00a8:
        r12.close();	 Catch:{ IOException -> 0x00ac }
        goto L_0x00ad;
    L_0x00ac:
        r2 = move-exception;
    L_0x00ad:
        return r0;
        if (r12 == 0) goto L_0x00b6;
    L_0x00b1:
        r12.close();	 Catch:{ IOException -> 0x00b5 }
        goto L_0x00b6;
    L_0x00b5:
        r0 = move-exception;
    L_0x00b6:
        return r1;
        if (r12 == 0) goto L_0x00bf;
    L_0x00ba:
        r12.close();	 Catch:{ IOException -> 0x00be }
        goto L_0x00bf;
    L_0x00be:
        r1 = move-exception;
    L_0x00bf:
        return r0;
    L_0x00c0:
        r0 = move-exception;
        goto L_0x00d3;
    L_0x00c2:
        r1 = move-exception;
        r2 = "XmpUtil";
        r3 = "Could not parse file.";
        android.util.Log.d(r2, r3, r1);	 Catch:{ all -> 0x00c0 }
        if (r12 == 0) goto L_0x00d2;
    L_0x00cd:
        r12.close();	 Catch:{ IOException -> 0x00d1 }
        goto L_0x00d2;
    L_0x00d1:
        r2 = move-exception;
    L_0x00d2:
        return r0;
    L_0x00d3:
        if (r12 == 0) goto L_0x00da;
    L_0x00d5:
        r12.close();	 Catch:{ IOException -> 0x00d9 }
        goto L_0x00da;
    L_0x00d9:
        r1 = move-exception;
    L_0x00da:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.motion.XmpUtil.parse(java.io.InputStream, boolean):java.util.List");
    }

    private XmpUtil() {
    }
}
