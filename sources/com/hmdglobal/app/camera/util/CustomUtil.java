package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;

public class CustomUtil {
    private static final String KEY = "ro.build.product";
    private static final String SKUID = "ro.boot.skuid";
    private static CustomUtil sCustomUtil = null;
    private final String BOOL = "bool";
    private String FILE = "/custpack/plf/HmdCamera/isdm_HmdCamera_defaults.xml";
    private final String HAS_PARSERED = "has_parsered";
    private final String INTEGER = "integer";
    private final String SPKEY = "isdm_HmdCamera_defaults";
    private final String START_TAG = "resources";
    private final String STRING = "string";
    private final String SYSTEM_VERSION = "system_version";
    private final String TAG = "CustomUtil";
    private Context mContext = null;
    private String mPackageName = null;
    private SharedPreferences mSharedPreferences = null;

    private CustomUtil(Context context) {
        this.mContext = context;
        this.mPackageName = this.mContext.getPackageName();
        this.mSharedPreferences = context.getSharedPreferences("isdm_HmdCamera_defaults", 0);
    }

    public static CustomUtil getInstance(Context context) {
        if (sCustomUtil == null) {
            sCustomUtil = new CustomUtil(context);
        }
        return sCustomUtil;
    }

    public static CustomUtil getInstance() {
        return sCustomUtil;
    }

    public SharedPreferences getSharedPreferences() {
        return this.mSharedPreferences;
    }

    private boolean systemCustFileExist() {
        File file = new File(this.FILE);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("File = ");
        stringBuilder.append(file.exists());
        Log.i("Custom", stringBuilder.toString());
        return file.exists();
    }

    private boolean needParserFile() {
        String systemVersion = Build.FINGERPRINT;
        if (this.mSharedPreferences.getBoolean("has_parsered", false) && systemVersion.equals(this.mSharedPreferences.getString("system_version", "default"))) {
            return false;
        }
        return true;
    }

    public boolean isPanther() {
        return "Panther".equals(SystemProperties.get(KEY, "default"));
    }

    public boolean isSkuid() {
        return "600ID".equals(SystemProperties.get(SKUID, "default"));
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x003c  */
    /* JADX WARNING: Removed duplicated region for block: B:6:0x000e  */
    private static void beginDocument(org.xmlpull.v1.XmlPullParser r4, java.lang.String r5) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        /*
    L_0x0000:
        r0 = r4.next();
        r1 = r0;
        r2 = 2;
        if (r0 == r2) goto L_0x000c;
    L_0x0008:
        r0 = 1;
        if (r1 == r0) goto L_0x000c;
    L_0x000b:
        goto L_0x0000;
    L_0x000c:
        if (r1 != r2) goto L_0x003c;
    L_0x000e:
        r0 = r4.getName();
        r0 = r0.equals(r5);
        if (r0 == 0) goto L_0x0019;
    L_0x0018:
        return;
    L_0x0019:
        r0 = new org.xmlpull.v1.XmlPullParserException;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Unexpected start tag: found ";
        r2.append(r3);
        r3 = r4.getName();
        r2.append(r3);
        r3 = ", expected ";
        r2.append(r3);
        r2.append(r5);
        r2 = r2.toString();
        r0.<init>(r2);
        throw r0;
    L_0x003c:
        r0 = new org.xmlpull.v1.XmlPullParserException;
        r2 = "No start tag found";
        r0.<init>(r2);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.util.CustomUtil.beginDocument(org.xmlpull.v1.XmlPullParser, java.lang.String):void");
    }

    private HashMap<String, String> parserFile() {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(this.FILE));
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, "utf-8");
            beginDocument(parser, "resources");
            int depth = parser.getDepth();
            HashMap<String, String> hm = new HashMap(10);
            while (true) {
                int next = parser.next();
                int type = next;
                if ((next == 3 && parser.getDepth() <= depth) || type == 1) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                    return hm;
                } else if (type == 2) {
                    hm.put(parser.getAttributeValue(null, "name"), parser.nextText());
                }
            }
        } catch (Exception e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error! when parser ");
            stringBuilder.append(this.FILE);
            Log.e("CustomUtil", stringBuilder.toString());
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                }
            }
            return null;
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    private void saveInSharedPreference(HashMap<String, String> hm) {
        if (this.mSharedPreferences == null || hm == null) {
            Log.d("CustomUtil", "Maybe HashMap is null. has parsered before");
            return;
        }
        Editor e = this.mSharedPreferences.edit();
        for (String str : hm.keySet()) {
            e.putString(str, (String) hm.get(str));
        }
        e.putBoolean("has_parsered", true);
        e.putString("system_version", Build.FINGERPRINT);
        e.apply();
        e.commit();
    }

    private void testForEngineer() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" get all :");
        stringBuilder.append(this.mSharedPreferences.getAll().toString());
        Log.d("CustomUtil", stringBuilder.toString());
    }

    public static void TraceLog(String msg) {
        Log.w("CustomUtil", msg);
    }

    public void setCustomFromSystem() {
        if (systemCustFileExist() && needParserFile()) {
            Log.d("CustomUtil", "start to parser file");
            saveInSharedPreference(parserFile());
        }
    }

    public int getInt(String key, int defaultValue) {
        if (this.mSharedPreferences.contains(key)) {
            String value = this.mSharedPreferences;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(defaultValue);
            stringBuilder.append("");
            try {
                return Integer.parseInt(value.getString(key, stringBuilder.toString()));
            } catch (Exception e) {
            }
        } else {
            Resources res = this.mContext.getResources();
            int id = res.getIdentifier(key, "integer", this.mPackageName);
            if (id > 0) {
                return res.getInteger(id);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("key = ");
            stringBuilder2.append(key);
            stringBuilder2.append(" defaultValue=");
            stringBuilder2.append(defaultValue);
            TraceLog(stringBuilder2.toString());
            return defaultValue;
        }
    }

    public String getString(String key, String defaultValue) {
        if (this.mSharedPreferences.contains(key)) {
            return this.mSharedPreferences.getString(key, defaultValue);
        }
        Resources res = this.mContext.getResources();
        int id = res.getIdentifier(key, "string", this.mPackageName);
        if (id > 0) {
            return res.getString(id);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("key = ");
        stringBuilder.append(key);
        stringBuilder.append(" defaultValue=");
        stringBuilder.append(defaultValue);
        TraceLog(stringBuilder.toString());
        return defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (this.mSharedPreferences.contains(key)) {
            try {
                return Boolean.parseBoolean(this.mSharedPreferences.getString(key, ""));
            } catch (Exception e) {
            }
        } else {
            Resources res = this.mContext.getResources();
            int id = res.getIdentifier(key, "bool", this.mPackageName);
            if (id > 0) {
                return res.getBoolean(id);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("key = ");
            stringBuilder.append(key);
            stringBuilder.append(" defaultValue=");
            stringBuilder.append(defaultValue);
            TraceLog(stringBuilder.toString());
            return defaultValue;
        }
    }
}
