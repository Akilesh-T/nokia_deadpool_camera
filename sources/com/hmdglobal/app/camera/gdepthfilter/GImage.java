package com.hmdglobal.app.camera.gdepthfilter;

import android.util.Base64;
import android.util.Log;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMetaFactory;
import com.hmdglobal.app.camera.Exif;
import com.hmdglobal.app.camera.exif.ExifInterface;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GImage {
    public static final String NAMESPACE_URL = "http://ns.google.com/photos/1.0/image/";
    public static final String PREFIX = "GImage";
    public static final String PROPERTY_DATA = "Data";
    public static final String PROPERTY_MIME = "Mime";
    private static final String TAG = "Flow_GImage";
    private String mData;
    private String mMime = "image/jpeg";

    static {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace(NAMESPACE_URL, PREFIX);
        } catch (XMPException e) {
            e.printStackTrace();
        }
    }

    public GImage(byte[] data, String mime) {
        ExifInterface exif = Exif.getExif(data);
        exif.addOrientationTag(0);
        OutputStream jpegOut = new ByteArrayOutputStream();
        try {
            exif.writeExif(data, jpegOut);
        } catch (IOException e) {
            Log.e(TAG, "wa_ Could not write EXIF", e);
        }
        this.mData = Base64.encodeToString(jpegOut.toByteArray(), 0);
        this.mMime = mime;
    }

    public String getMime() {
        return this.mMime;
    }

    public String getData() {
        return this.mData;
    }
}
