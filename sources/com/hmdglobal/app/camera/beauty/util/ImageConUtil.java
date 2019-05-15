package com.hmdglobal.app.camera.beauty.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
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
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import com.hmdglobal.app.camera.Storage;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

public class ImageConUtil {
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
            newUiOptions ^= 2;
        }
        if (VERSION.SDK_INT >= 19) {
            newUiOptions ^= 4096;
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

    public static String getRealPathFromURI(Context context, Uri contentURI) {
        Cursor cursor = context.getContentResolver().query(contentURI, new String[]{"_data"}, null, null, null);
        if (cursor == null) {
            return contentURI.getPath();
        }
        cursor.moveToFirst();
        String result = cursor.getString(cursor.getColumnIndex("_data"));
        cursor.close();
        return result;
    }

    public static Bitmap getImage(String path) {
        try {
            Bitmap src = BitmapFactory.decodeFile(path);
            int rotation = new ExifInterface(path).getAttributeInt("Orientation", 1);
            Matrix matrix = new Matrix();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(rotation);
            Log.e("GetImage Rotation", stringBuilder.toString());
            switch (rotation) {
                case 1:
                    return src;
                case 2:
                    matrix.setScale(-1.0f, 1.0f);
                    break;
                case 3:
                    matrix.setRotate(180.0f);
                    break;
                case 4:
                    matrix.setRotate(180.0f);
                    matrix.postScale(-1.0f, 1.0f);
                    break;
                case 5:
                    matrix.setRotate(90.0f);
                    matrix.postScale(-1.0f, 1.0f);
                    break;
                case 6:
                    matrix.setRotate(90.0f);
                    break;
                case 7:
                    matrix.setRotate(-90.0f);
                    matrix.postScale(-1.0f, 1.0f);
                    break;
                case 8:
                    matrix.setRotate(-90.0f);
                    break;
                default:
                    return src;
            }
            Bitmap dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
            src.recycle();
            return dst;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

    public static byte[] getPixelsBGR(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] tmp = new int[(width * height)];
        image.getPixels(tmp, 0, width, 0, 0, width, height);
        byte[] pixels = new byte[((width * height) * 3)];
        for (int i = 0; i < width * height; i++) {
            int argb = tmp[i];
            pixels[(i * 3) + 0] = (byte) Color.blue(argb);
            pixels[(i * 3) + 1] = (byte) Color.green(argb);
            pixels[(i * 3) + 2] = (byte) Color.red(argb);
        }
        return pixels;
    }

    public static byte[] setBitmapAlpha(Bitmap image, float[] alphas) {
        ByteBuffer buffer = ByteBuffer.allocate(image.getByteCount());
        image.copyPixelsToBuffer(buffer);
        byte[] temp = buffer.array();
        byte[] pixels = new byte[temp.length];
        for (int i = 0; i < temp.length / 4; i++) {
            pixels[(i * 4) + 0] = temp[(i * 4) + 0];
            pixels[(i * 4) + 1] = temp[(i * 4) + 1];
            pixels[(i * 4) + 2] = temp[(i * 4) + 2];
            if (alphas[i] < 0.15f) {
                alphas[i] = 0.0f;
            } else if (alphas[i] > 0.55f) {
                alphas[i] = 1.0f;
            }
            pixels[(i * 4) + 3] = (byte) ((int) (255.0f * alphas[i]));
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

    public static byte[] getFileContent(String path) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count = -1;
        try {
            InputStream inputStream = new FileInputStream(path);
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

    public static String saveBitmap(Context context, Bitmap bitmaptosave) {
        if (bitmaptosave == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        stringBuilder.append("/camera");
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
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(mediaStorageDir);
                stringBuilder3.append("/");
                stringBuilder3.append(bitmapFileName);
                updateAlbum(context, new File(stringBuilder3.toString()));
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(mediaStorageDir.getAbsolutePath());
                stringBuilder2.append("/");
                stringBuilder2.append(bitmapFileName);
                String stringBuilder4 = stringBuilder2.toString();
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return stringBuilder4;
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

    public static void updateAlbum(Context context, File file) {
        Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        intent.setData(Uri.fromFile(file));
        context.sendBroadcast(intent);
    }

    public static Bitmap mergeBitmapWithLogo(Context context, Bitmap srcBitmap, Bitmap bmpLogo) {
        Bitmap resultBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(srcBitmap, new Rect(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()), new Rect(0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()), null);
        Rect srcRect = new Rect(0, 0, bmpLogo.getWidth(), bmpLogo.getHeight());
        int width = srcBitmap.getWidth();
        int margin = (width * 6) / 120;
        int logoWidth = (width * 36) / 120;
        int logoHeight = (bmpLogo.getHeight() * logoWidth) / bmpLogo.getWidth();
        int x = (srcBitmap.getWidth() - logoWidth) - margin;
        int y = (srcBitmap.getHeight() - logoHeight) - margin;
        canvas.drawBitmap(bmpLogo, srcRect, new Rect(x, y, x + logoWidth, y + logoHeight), null);
        return resultBitmap;
    }

    public static int getExifOrientation(int angle, boolean flip) {
        if (angle == 90) {
            return flip ? 5 : 6;
        } else if (angle == MediaProviderUtils.ROTATION_180) {
            return flip ? 4 : 3;
        } else if (angle != MediaProviderUtils.ROTATION_270) {
            return flip ? 2 : 1;
        } else {
            return flip ? 7 : 8;
        }
    }
}
