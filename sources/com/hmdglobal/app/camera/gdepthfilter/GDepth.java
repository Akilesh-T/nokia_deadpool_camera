package com.hmdglobal.app.camera.gdepthfilter;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class GDepth {
    public static final String FORMAT_8_BIT = "8-bit";
    public static final String FORMAT_RANGE_INVERSE = "RangeInverse";
    public static final String FORMAT_RANGLE_LINEAR = "RangeLinear";
    private static final String MIME = "image/jpeg";
    public static final String NAMESPACE_URL = "http://ns.google.com/photos/1.0/depthmap/";
    public static final String PREFIX = "GDepth";
    public static final String PROPERTY_DATA = "Data";
    public static final String PROPERTY_FAR = "Far";
    public static final String PROPERTY_FORMAT = "Format";
    public static final String PROPERTY_MIME = "Mime";
    public static final String PROPERTY_NEAR = "Near";
    public static final String PROPERTY_ROI_HEIGHT = "RoiHeight";
    public static final String PROPERTY_ROI_WIDTH = "RoiWidth";
    public static final String PROPERTY_ROI_X = "RoiX";
    public static final String PROPERTY_ROI_Y = "RoiY";
    private static final String TAG = "Flow_GDepth";
    private String mData;
    private DepthMap mDepthMap;
    private float mFar = 800.0f;
    private String mFormat = FORMAT_RANGE_INVERSE;
    private byte[] mGdepthJpeg;
    private int[] mMap;
    private float mNear = 20.0f;
    private Rect mRoi;

    public static class DepthMap {
        public byte[] buffer;
        public int height;
        public int orientation;
        public Rect roi;
        public int width;

        public DepthMap(int width, int height, int orientation) {
            this.width = width;
            this.height = height;
            this.orientation = orientation;
        }
    }

    static {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace(NAMESPACE_URL, PREFIX);
        } catch (XMPException e) {
            e.printStackTrace();
        }
    }

    private GDepth(DepthMap depthMap) {
        this.mDepthMap = depthMap;
        this.mRoi = depthMap.roi;
        if (depthMap != null && depthMap.buffer != null) {
            this.mMap = new int[depthMap.buffer.length];
            for (int i = 0; i < this.mMap.length; i++) {
                int gray = depthMap.buffer[i] & 255;
                this.mMap[i] = Color.rgb(gray, gray, gray);
            }
        }
    }

    private GDepth(byte[] gdepthJpeg) {
        this.mGdepthJpeg = gdepthJpeg;
    }

    public void setFormat(byte format) {
        if (format == (byte) 0) {
            this.mFormat = FORMAT_RANGE_INVERSE;
        } else {
            this.mFormat = FORMAT_RANGLE_LINEAR;
        }
    }

    public String getFormat() {
        return this.mFormat;
    }

    public String getMime() {
        return "image/jpeg";
    }

    public void setNear(float near) {
        this.mNear = near;
    }

    public float getNear() {
        return this.mNear;
    }

    public void setFar(float far) {
        this.mFar = far;
    }

    public float getFar() {
        return this.mFar;
    }

    public String getData() {
        return this.mData;
    }

    public Rect getRoi() {
        return this.mRoi;
    }

    public void setRoi(Rect roi) {
        this.mRoi = roi;
    }

    public static GDepth createGDepth(DepthMap depthMap) {
        GDepth gDepth = new GDepth(depthMap);
        if (gDepth.encoding()) {
            return gDepth;
        }
        return null;
    }

    public static GDepth createGDepth(byte[] gdepthJpeg) {
        GDepth gDepth = new GDepth(gdepthJpeg);
        if (gDepth.encodeDepthmapJpeg()) {
            return gDepth;
        }
        return null;
    }

    private boolean encoding() {
        Log.d(TAG, "encoding");
        Bitmap bitmap = Bitmap.createBitmap(this.mMap, this.mDepthMap.width, this.mDepthMap.height, Config.ARGB_8888);
        if (this.mDepthMap.orientation != 0) {
            Log.i(TAG, "wa_ adjust depth image rotation");
            bitmap = CameraUtil.rotate(bitmap, this.mDepthMap.orientation);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, 100, outputStream);
        byte[] jpegBytes = outputStream.toByteArray();
        this.mGdepthJpeg = jpegBytes;
        if (jpegBytes != null) {
            this.mData = serializeAsBase64Str(jpegBytes);
            return true;
        }
        Log.e(TAG, "compressToJPEG failure");
        return false;
    }

    private boolean encodeDepthmapJpeg() {
        Log.d(TAG, "encodeDepthmapJpeg");
        if (this.mGdepthJpeg != null) {
            this.mData = serializeAsBase64Str(this.mGdepthJpeg);
            return true;
        }
        Log.e(TAG, "compressToJPEG failure");
        return false;
    }

    public Bitmap getGdepthBitmap() {
        return Bitmap.createBitmap(this.mMap, this.mDepthMap.width, this.mDepthMap.height, Config.ARGB_8888);
    }

    public Bitmap getBitGdepthBitmap() {
        int[] data = new int[this.mMap.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = (this.mMap[i] & 255) << 24;
        }
        return Bitmap.createBitmap(data, this.mDepthMap.width, this.mDepthMap.height, Config.ALPHA_8);
    }

    private byte[] compressToJPEG(byte[] image) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("compressToJPEG byte[].size=");
        stringBuilder.append(image.length);
        Log.d(str, stringBuilder.toString());
        Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
        if (bmp == null) {
            Log.d(TAG, " buffer can't be decoded ");
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bmp.compress(CompressFormat.JPEG, 100, outputStream);
        return outputStream.toByteArray();
    }

    public byte[] getDepthJpeg() {
        return this.mGdepthJpeg;
    }

    private String serializeAsBase64Str(byte[] image) {
        Log.d(TAG, "serializeAsBase64Str");
        return Base64.encodeToString(image, 0);
    }

    private void saveAsFile(String str, String name) {
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("saveAsFile sdcard/DDM/Flow_GDepth");
        stringBuilder.append(name);
        stringBuilder.append(".log");
        Log.d(str2, stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("sdcard/DDM/Flow_GDepth");
        stringBuilder.append(name);
        stringBuilder.append(".log");
        File file = new File(stringBuilder.toString());
        OutputStream out = null;
        byte[] bytes = str.getBytes();
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(bytes, 0, bytes.length);
            try {
                out.close();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        } catch (Exception e2) {
            Log.d(TAG, e2.toString());
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e3) {
                    Log.d(TAG, e3.toString());
                }
            }
        }
    }

    private void saveAsJPEG(byte[] bytes) {
        Log.d(TAG, "saveAsJPEG");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sdcard/");
        stringBuilder.append(System.currentTimeMillis());
        stringBuilder.append("_depth.JPEG");
        File file = new File(stringBuilder.toString());
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(bytes, 0, bytes.length);
            try {
                out.close();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        } catch (Exception e2) {
            Log.d(TAG, e2.toString());
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e3) {
                    Log.d(TAG, e3.toString());
                }
            }
        }
    }

    private GDepth(int near, int far, String data) {
        this.mData = data;
    }

    public static GDepth createGDepth(XMPMeta xmpMeta) {
        try {
            int near = Integer.parseInt((String) xmpMeta.getProperty(NAMESPACE_URL, PROPERTY_NEAR).getValue());
            int far = Integer.parseInt((String) xmpMeta.getProperty(NAMESPACE_URL, PROPERTY_FAR).getValue());
            String data = (String) xmpMeta.getProperty(NAMESPACE_URL, "Data").getValue();
            String format = (String) xmpMeta.getProperty(NAMESPACE_URL, PROPERTY_FORMAT).getValue();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("new GDepth: nerar=");
            stringBuilder.append(near);
            stringBuilder.append(" far=");
            stringBuilder.append(far);
            stringBuilder.append("format=");
            stringBuilder.append(format);
            stringBuilder.append(" data=");
            stringBuilder.append(data);
            Log.d(str, stringBuilder.toString());
            int x = Integer.parseInt((String) xmpMeta.getProperty(NAMESPACE_URL, PROPERTY_ROI_X).getValue());
            int y = Integer.parseInt((String) xmpMeta.getProperty(NAMESPACE_URL, PROPERTY_ROI_Y).getValue());
            int width = Integer.parseInt((String) xmpMeta.getProperty(NAMESPACE_URL, PROPERTY_ROI_WIDTH).getValue());
            int height = Integer.parseInt((String) xmpMeta.getProperty(NAMESPACE_URL, PROPERTY_ROI_HEIGHT).getValue());
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("x=");
            stringBuilder2.append(x);
            stringBuilder2.append(" y=");
            stringBuilder2.append(y);
            stringBuilder2.append(" width=");
            stringBuilder2.append(width);
            stringBuilder2.append(" height=");
            stringBuilder2.append(height);
            Log.d(str2, stringBuilder2.toString());
            return new GDepth(near, far, data);
        } catch (XMPException e) {
            Log.e(TAG, e.toString());
            return null;
        } catch (Exception e2) {
            Log.e(TAG, e2.toString());
            return null;
        }
    }

    public boolean decode() {
        Log.d(TAG, "decode");
        byte[] depthBuffer = Base64.decode(this.mData, 0);
        saveAsJPEG(depthBuffer);
        int[] intDepthBuffer = new int[depthBuffer.length];
        int[] intDepth = new int[depthBuffer.length];
        for (int i = 0; i < intDepthBuffer.length; i++) {
            intDepthBuffer[i] = (depthBuffer[i] + 256) % 256;
        }
        return false;
    }
}
