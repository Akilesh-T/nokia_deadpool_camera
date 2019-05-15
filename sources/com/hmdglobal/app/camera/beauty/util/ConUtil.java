package com.hmdglobal.app.camera.beauty.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import com.hmdglobal.app.camera.Storage;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConUtil {
    public static Bitmap bitmap;
    private static Toast toast;
    public static WakeLock wakeLock = null;

    public static float[] rotateFloat(float[] data, int width, int height, int angle, boolean isMirror) {
        if (angle == 90) {
            return rotateFloat_90(data, width, height, isMirror);
        }
        if (angle == MediaProviderUtils.ROTATION_180) {
            return rotateFloat_180(data, width, height, isMirror);
        }
        if (angle == MediaProviderUtils.ROTATION_270) {
            return rotateFloat_270(data, width, height, isMirror);
        }
        return data;
    }

    public static float[] rotateFloat_90(float[] data, int width, int height, boolean isMirror) {
        if (data == null || width == 0 || height == 0) {
            return null;
        }
        int j;
        float[][] data_1 = (float[][]) Array.newInstance(float.class, new int[]{height, width});
        for (int i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                data_1[i][j] = data[(i * width) + j];
            }
        }
        float[] data_2 = new float[(width * height)];
        int n = 0;
        j = 0;
        while (j < width) {
            int n2 = n;
            for (n = 0; n < height; n++) {
                if (isMirror) {
                    data_2[n2] = data_1[(height - n) - 1][(width - 1) - j];
                } else {
                    data_2[n2] = data_1[(height - n) - 1][j];
                }
                n2++;
            }
            j++;
            n = n2;
        }
        return data_2;
    }

    public static float[] rotateFloat_270(float[] data, int width, int height, boolean isMirror) {
        if (data == null || width == 0 || height == 0) {
            return null;
        }
        int i;
        int j;
        float[][] data_1 = (float[][]) Array.newInstance(float.class, new int[]{height, width});
        for (i = 0; i < height; i++) {
            for (j = 0; j < width; j++) {
                data_1[i][j] = data[(i * width) + j];
            }
        }
        float[] data_2 = new float[(width * height)];
        i = 0;
        for (j = width - 1; j >= 0; j--) {
            for (int j2 = height - 1; j2 >= 0; j2--) {
                if (isMirror) {
                    data_2[i] = data_1[(height - j2) - 1][(width - 1) - j];
                } else {
                    data_2[i] = data_1[(height - j2) - 1][j];
                }
                i++;
            }
        }
        return data_2;
    }

    public static float[] rotateFloat_180(float[] data, int width, int height, boolean isMirror) {
        if (data == null || width == 0 || height == 0) {
            return null;
        }
        float[] data_1 = new float[(width * height)];
        for (int i = 0; i < data.length; i++) {
            data_1[i] = data[(data.length - 1) - i];
        }
        return data_1;
    }

    public static Bitmap setBitmapPixel(Context context, int width, int height, byte[] data) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        int mBitmapWidth = bitmap.getWidth();
        int mBitmapHeight = bitmap.getHeight();
        for (int i = 0; i < mBitmapHeight; i++) {
            for (int j = 0; j < mBitmapWidth; j++) {
                int index = ((i * mBitmapWidth) + j) * 4;
                byte d = data[index];
                bitmap.setPixel(j, i, Color.rgb(data[index] + 0, data[index] + 1, data[index] + 2));
            }
        }
        return bitmap;
    }

    public static Bitmap setBitmapPixel(Context context, int width, int height, float[] data) {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        }
        int mBitmapWidth = bitmap.getWidth();
        int mBitmapHeight = bitmap.getHeight();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPixel: data.length == ");
        stringBuilder.append(data.length);
        stringBuilder.append(", ");
        stringBuilder.append(width);
        stringBuilder.append(", ");
        stringBuilder.append(height);
        Log.w("ceshi", stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("getPixel: mBitmapWidth == ");
        stringBuilder.append(mBitmapWidth);
        stringBuilder.append(", ");
        stringBuilder.append(mBitmapHeight);
        stringBuilder.append(", ");
        stringBuilder.append(mBitmapWidth * mBitmapHeight);
        Log.w("ceshi", stringBuilder.toString());
        for (int i = 0; i < mBitmapHeight; i++) {
            for (int j = 0; j < mBitmapWidth; j++) {
                float d = data[(i * mBitmapWidth) + j];
                bitmap.setPixel(j, i, Color.rgb((int) (d * 255.0f), (int) (d * 255.0f), (int) (255.0f * d)));
            }
        }
        return bitmap;
    }

    public static byte[] Ints2Bytes(int[] s) {
        byte[] buf = new byte[(s.length * 4)];
        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = getBytes(s[iLoop]);
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("1out->");
            stringBuilder.append(s[iLoop]);
            printStream.println(stringBuilder.toString());
            for (int jLoop = 0; jLoop < 4; jLoop++) {
                buf[(iLoop * 4) + jLoop] = temp[jLoop];
            }
        }
        return buf;
    }

    public static byte[] getBytes(int s, boolean bBigEnding) {
        byte[] buf = new byte[4];
        int i;
        if (bBigEnding) {
            for (i = buf.length - 1; i >= 0; i--) {
                buf[i] = (byte) (s & 255);
                s >>= 8;
            }
        } else {
            System.out.println("1");
            for (i = 0; i < buf.length; i++) {
                buf[i] = (byte) (s & 255);
                s >>= 8;
            }
        }
        return buf;
    }

    public static byte[] getBytes(int i) {
        return getBytes(i, testCPU());
    }

    public static boolean testCPU() {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            return true;
        }
        return false;
    }

    public static ArrayList<HashMap<String, String>> getParserFilter(Context context, String name) {
        ArrayList<HashMap<String, String>> filterList = new ArrayList();
        InputStream inputStream = null;
        ByteArrayOutputStream baos = null;
        try {
            inputStream = context.getResources().getAssets().open(name);
            byte[] buffer = new byte[1024];
            baos = new ByteArrayOutputStream();
            int i = 0;
            int count = 0;
            while (true) {
                int read = inputStream.read(buffer);
                count = read;
                if (read <= 0) {
                    break;
                }
                baos.write(buffer, 0, count);
            }
            JSONArray rootJson = new JSONArray(new String(baos.toByteArray()));
            while (i < rootJson.length()) {
                HashMap<String, String> map = new HashMap();
                JSONObject json = rootJson.getJSONObject(i);
                map.put("name", json.getString("title_chinese"));
                map.put("name_english", json.getString("title_english"));
                map.put("filter", json.getString("filter"));
                map.put("image", json.getString("sample"));
                filterList.add(map);
                i++;
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                baos.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e22) {
                    e22.printStackTrace();
                }
            }
            if (baos != null) {
                baos.close();
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e42) {
                    e42.printStackTrace();
                }
            }
        }
        return filterList;
    }

    public static String saveAssestsData(Context context, String filePath, String newPath, String name) {
        try {
            InputStream inputStream = context.getResources().getAssets();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(filePath);
            stringBuilder.append("/");
            stringBuilder.append(name);
            inputStream = inputStream.open(stringBuilder.toString());
            String path = new StringBuilder();
            path.append(newPath);
            path.append(name);
            path = path.toString();
            File stickerFile = new File(path);
            if (stickerFile.exists()) {
                return stickerFile.getAbsolutePath();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            byte[] buffer = new byte[512];
            int count = 0;
            while (true) {
                int read = inputStream.read(buffer);
                count = read;
                if (read > 0) {
                    fileOutputStream.write(buffer, 0, count);
                } else {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    inputStream.close();
                    return path;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getJsonString(HashMap<String, byte[]> featureMap) {
        try {
            return new JSONObject(featureMap).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getYUVBitMap(byte[] data, Camera camera, int Angle) {
        int width = camera.getParameters().getPreviewSize().width;
        int height = camera.getParameters().getPreviewSize().height;
        if (Angle == 90 || Angle == MediaProviderUtils.ROTATION_270) {
            width = camera.getParameters().getPreviewSize().height;
            height = camera.getParameters().getPreviewSize().width;
        }
        YuvImage yuvImage = new YuvImage(data, camera.getParameters().getPreviewFormat(), width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, byteArrayOutputStream);
        byte[] jpegData = byteArrayOutputStream.toByteArray();
        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length).copy(Config.ARGB_8888, true);
    }

    static void RGBToYUV(int Red, int Green, int Blue, int[] yuv, int offset) {
        yuv[offset + 0] = ((((((((((((Red << 6) + (Red << 3)) + (Red << 2)) + Red) + (Green << 7)) + (Green << 4)) + (Green << 2)) + (Green << 1)) + (Blue << 4)) + (Blue << 3)) + (Blue << 2)) + Blue) >> 8;
        yuv[offset + 1] = (((-(((Red << 5) + (Red << 2)) + (Red << 1))) - (((Green << 6) + (Green << 3)) + (Green << 1))) + (((Blue << 6) + (Blue << 5)) + (Blue << 4))) >> 8;
        yuv[offset + 2] = (((((((Red << 7) + (Red << 4)) + (Red << 3)) + (Red << 2)) + (Red << 1)) - ((Green << 7) + (Green << 2))) - (((Blue << 4) + (Blue << 3)) + (Blue << 1))) >> 8;
    }

    static void YUVToRGB(int Y, int U, int V, int[] rgb, int offset) {
        rgb[offset + 0] = ((Y << 8) + (((V << 8) + (V << 5)) + (V << 2))) >> 8;
        rgb[offset + 1] = (((Y << 8) - (((U << 6) + (U << 5)) + (U << 2))) - ((((V << 7) + (V << 4)) + (V << 2)) + V)) >> 8;
        rgb[offset + 2] = (((Y << 8) + (U << 9)) + (U << 3)) >> 8;
    }

    public static void decodeYUV420SPRGB3(byte[] rgb, byte[] yuv420sp, int width, int height) {
        for (int i = 0; i < width * height; i++) {
            int rgbIndex = i << 2;
            rgb[rgbIndex] = yuv420sp[i];
            rgb[rgbIndex + 1] = yuv420sp[i];
            rgb[rgbIndex + 2] = yuv420sp[i];
            rgb[rgbIndex + 3] = (byte) -1;
        }
    }

    public static void decodeYUV420SPRGB2(byte[] rgb, byte[] yuv420sp, int width, int height) {
        int i = width;
        int i2 = height;
        int size = i * i2;
        int y = 0;
        while (y < i2) {
            int offsetY = y * i;
            int offsetRGB = offsetY << 2;
            int yoffsetUV = (y / 2) * (i / 2);
            int x = 0;
            while (x < i) {
                int Y = yuv420sp[offsetY + x] & 255;
                int pos = ((yoffsetUV * 2) + size) + ((x / 2) * 2);
                int U = yuv420sp[pos] & 255;
                int V = yuv420sp[pos + 1] & 255;
                int startOffset = (x * 4) + offsetRGB;
                int size2 = size;
                double B = (((double) (Y - 16)) * 1.164d) + (2.018d * ((double) (U - 128)));
                int offsetY2 = offsetY;
                int offsetRGB2 = offsetRGB;
                double G = ((((double) (Y - 16)) * 1.164d) - (0.813d * ((double) (V - 128)))) - (0.391d * ((double) (U - 128)));
                int startOffset2 = startOffset + 1;
                rgb[startOffset] = (byte) ((int) ((1.164d * ((double) (Y - 16))) + (1.596d * ((double) (V - 128)))));
                pos = startOffset2 + 1;
                rgb[startOffset2] = (byte) ((int) G);
                startOffset2 = pos + 1;
                rgb[pos] = (byte) ((int) B);
                pos = startOffset2 + 1;
                rgb[startOffset2] = (byte) -1;
                x++;
                size = size2;
                offsetY = offsetY2;
                offsetRGB = offsetRGB2;
                i = width;
                i2 = height;
            }
            y++;
            i = width;
            i2 = height;
        }
    }

    public static void decodeYUV420SPRGB(byte[] rgb, byte[] yuv420sp, int width, int height) {
        int i = width;
        int i2 = height;
        int frameSize = i * i2;
        int j = 0;
        int yp = 0;
        while (j < i2) {
            int uvp = ((j >> 1) * i) + frameSize;
            int u = 0;
            int v = 0;
            int yp2 = yp;
            yp = 0;
            while (yp < i) {
                int uvp2;
                int y = (yuv420sp[yp2] & 255) - 16;
                if (y < 0) {
                    y = 0;
                }
                if ((yp & 1) == 0) {
                    uvp2 = uvp + 1;
                    u = (yuv420sp[uvp2] & 255) - 128;
                    int i3 = uvp2 + 1;
                    v = (yuv420sp[uvp] & 255) - 128;
                    uvp = i3;
                }
                uvp2 = 1192 * y;
                int r = (1634 * v) + uvp2;
                int g = (uvp2 - (833 * v)) - (400 * u);
                int b = uvp2 + (2066 * u);
                if (r < 0) {
                    r = 0;
                } else if (r > 262143) {
                    r = 262143;
                }
                if (g < 0) {
                    g = 0;
                } else if (g > 262143) {
                    g = 262143;
                }
                if (b < 0) {
                    b = 0;
                } else if (b > 262143) {
                    b = 262143;
                }
                rgb[4 * yp2] = (byte) ((r >> 10) & 255);
                rgb[(4 * yp2) + 1] = (byte) ((g >> 10) & 255);
                rgb[(4 * yp2) + 2] = (byte) ((b >> 10) & 255);
                rgb[(4 * yp2) + 3] = (byte) -1;
                yp++;
                yp2++;
                i = width;
            }
            j++;
            yp = yp2;
            i = width;
        }
    }

    public static void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        byte[] bArr = yuv420sp;
        int i = width;
        int i2 = height;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("decodeYUV420SP width is ");
        stringBuilder.append(i);
        Log.e("wangshuai", stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("decodeYUV420SP height is ");
        stringBuilder.append(i2);
        Log.e("wangshuai", stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("decodeYUV420SP yuv420sp is ");
        stringBuilder.append(bArr.length);
        Log.e("wangshuai", stringBuilder.toString());
        int frameSize = i * i2;
        int j = 0;
        int yp = 0;
        while (j < i2) {
            int uvp = ((j >> 1) * i) + frameSize;
            int u = 0;
            int v = 0;
            int yp2 = yp;
            yp = 0;
            while (yp < i) {
                int uvp2;
                int y = (bArr[yp2] & 255) - 16;
                if (y < 0) {
                    y = 0;
                }
                if ((yp & 1) == 0) {
                    uvp2 = uvp + 1;
                    u = (bArr[uvp2] & 255) - 128;
                    int i3 = uvp2 + 1;
                    v = (bArr[uvp] & 255) - 128;
                    uvp = i3;
                }
                uvp2 = 1192 * y;
                int r = (1634 * v) + uvp2;
                int g = (uvp2 - (833 * v)) - (400 * u);
                int b = uvp2 + (2066 * u);
                if (r < 0) {
                    r = 0;
                } else if (r > 262143) {
                    r = 262143;
                }
                if (g < 0) {
                    g = 0;
                } else if (g > 262143) {
                    g = 262143;
                }
                if (b < 0) {
                    b = 0;
                } else if (b > 262143) {
                    b = 262143;
                }
                rgb[yp2] = ((b >> 10) & 255) | ((ViewCompat.MEASURED_STATE_MASK | ((r << 6) & 16711680)) | ((g >> 2) & MotionEventCompat.ACTION_POINTER_INDEX_MASK));
                yp++;
                yp2++;
                bArr = yuv420sp;
                i = width;
            }
            j++;
            yp = yp2;
            bArr = yuv420sp;
            i = width;
        }
    }

    public static Bitmap getBitMap(byte[] data, Camera camera, boolean mIsFrontalCamera) {
        int width = camera.getParameters().getPreviewSize().width;
        int height = camera.getParameters().getPreviewSize().height;
        YuvImage yuvImage = new YuvImage(data, camera.getParameters().getPreviewFormat(), width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, byteArrayOutputStream);
        byte[] jpegData = byteArrayOutputStream.toByteArray();
        Bitmap tmpBitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(0.0f);
        tmpBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight(), matrix, true).copy(Config.ARGB_8888, true);
        float scale = ((float) (tmpBitmap.getHeight() > tmpBitmap.getWidth() ? tmpBitmap.getHeight() : tmpBitmap.getWidth())) / 800.0f;
        if (scale > 1.0f) {
            return Bitmap.createScaledBitmap(tmpBitmap, (int) (((float) tmpBitmap.getWidth()) / scale), (int) (((float) tmpBitmap.getHeight()) / scale), false);
        }
        return tmpBitmap;
    }

    public static String getFormatterDate(long time) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date(time));
    }

    public static void toggleHideyBar(Activity activity) {
        int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        if (VERSION.SDK_INT >= 14) {
            newUiOptions |= 2;
        }
        if (VERSION.SDK_INT >= 16) {
            newUiOptions |= 4;
        }
        if (VERSION.SDK_INT >= 19) {
            newUiOptions |= 4096;
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    public static void isGoneKeyBoard(Activity activity) {
        if (activity.getCurrentFocus() != null) {
            ((InputMethodManager) activity.getSystemService("input_method")).hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 2);
        }
    }

    public static void acquireWakeLock(Context context) {
        if (wakeLock == null) {
            wakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(6, "My Tag");
            wakeLock.acquire();
        }
    }

    public static void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    public static byte[] getPixelsRGBA(Bitmap image) {
        ByteBuffer buffer = ByteBuffer.allocate(image.getByteCount());
        image.copyPixelsToBuffer(buffer);
        byte[] temp = buffer.array();
        byte[] pixels = new byte[temp.length];
        for (int i = 0; i < temp.length / 4; i++) {
            pixels[(i * 4) + 0] = temp[(i * 4) + 0];
            pixels[(i * 4) + 1] = temp[(i * 4) + 1];
            pixels[(i * 4) + 2] = temp[(i * 4) + 2];
            pixels[(i * 4) + 3] = temp[(i * 4) + 3];
        }
        return pixels;
    }

    public static byte[] getGrayscale(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        byte[] ret = new byte[(bitmap.getWidth() * bitmap.getHeight())];
        for (int j = 0; j < bitmap.getHeight(); j++) {
            for (int i = 0; i < bitmap.getWidth(); i++) {
                int pixel = bitmap.getPixel(i, j);
                ret[(bitmap.getWidth() * j) + i] = (byte) ((((299 * ((16711680 & pixel) >> 16)) + (587 * ((MotionEventCompat.ACTION_POINTER_INDEX_MASK & pixel) >> 8))) + (114 * (pixel & 255))) / 1000);
            }
        }
        return ret;
    }

    public static byte[] getFileContent(Context context, int id) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count = -1;
        try {
            InputStream inputStream = context.getResources().openRawResource(id);
            while (true) {
                int read = inputStream.read(buffer);
                count = read;
                if (read != -1) {
                    byteArrayOutputStream.write(buffer, 0, count);
                } else {
                    byteArrayOutputStream.close();
                    return byteArrayOutputStream.toByteArray();
                }
            }
        } catch (IOException e) {
            return null;
        } catch (Throwable th) {
            throw th;
        }
    }

    public static void showToast(Context context, String str) {
        if (toast != null) {
            toast.cancel();
        }
        if (context != null) {
            toast = Toast.makeText(context, str, 0);
            toast.setGravity(48, 0, 30);
            toast.show();
        }
    }

    public static void showLongToast(Context context, String str) {
        if (toast != null) {
            toast.cancel();
        }
        if (context != null) {
            toast = Toast.makeText(context, str, 1);
            toast.setGravity(48, 0, 30);
            toast.show();
        }
    }

    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap convert(Bitmap bitmap, boolean mIsFrontalCamera) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Bitmap newbBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas cv = new Canvas(newbBitmap);
        Matrix m = new Matrix();
        if (mIsFrontalCamera) {
            m.postScale(-1.0f, 1.0f);
        }
        Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, w, h, m, true);
        cv.drawBitmap(bitmap2, new Rect(0, 0, bitmap2.getWidth(), bitmap2.getHeight()), new Rect(0, 0, w, h), null);
        return newbBitmap;
    }

    public static String saveBitmap(Bitmap bitmaptosave) {
        if (bitmaptosave == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getExternalStorageDirectory().getPath());
        stringBuilder.append("/DCIM/Camera/");
        File mediaStorageDir = new File(stringBuilder.toString());
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            return null;
        }
        String bitmapFileName = new StringBuilder();
        bitmapFileName.append(System.currentTimeMillis());
        bitmapFileName.append(Storage.JPEG_POSTFIX);
        bitmapFileName = bitmapFileName.toString();
        FileOutputStream fos = null;
        try {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(mediaStorageDir);
            stringBuilder2.append("/");
            stringBuilder2.append(bitmapFileName);
            fos = new FileOutputStream(stringBuilder2.toString());
            if (bitmaptosave.compress(CompressFormat.JPEG, 100, fos)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(mediaStorageDir.getAbsolutePath());
                stringBuilder2.append("/");
                stringBuilder2.append(bitmapFileName);
                String stringBuilder3 = stringBuilder2.toString();
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return stringBuilder3;
            }
            try {
                fos.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            return null;
        } catch (FileNotFoundException e3) {
            e3.printStackTrace();
            try {
                fos.close();
            } catch (IOException e22) {
                e22.printStackTrace();
            }
            return null;
        } catch (Throwable th) {
            try {
                fos.close();
            } catch (IOException e4) {
                e4.printStackTrace();
            }
            throw th;
        }
    }

    public static String saveBytw(byte[] b, Context context) {
        BufferedOutputStream stream = null;
        File file = null;
        try {
            file = context.getExternalFilesDir("megvii");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(file);
            stringBuilder.append("/");
            stringBuilder.append(System.currentTimeMillis());
            stream = new BufferedOutputStream(new FileOutputStream(stringBuilder.toString()));
            stream.write(b);
            try {
                stream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e12) {
                    e12.printStackTrace();
                }
            }
        }
        return file.getAbsolutePath();
    }
}
