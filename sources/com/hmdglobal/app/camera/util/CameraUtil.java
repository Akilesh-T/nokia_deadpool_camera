package com.hmdglobal.app.camera.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Location;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.CameraSettings.GpsData;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.CameraDisabledException;
import com.hmdglobal.app.camera.GuideActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CameraUtil {
    public static final String ACTION_CAMERA_SHUTTER_CLICK = "com.hmdglobal.app.camera.action.SHUTTER_CLICK";
    public static final String ACTION_CAMERA_STARTED = "com.hmdglobal.app.camera.action.CAMERA_STARTED";
    public static final String ACTION_CAMERA_STOPPED = "com.hmdglobal.app.camera.action.CAMERA_STOPPED";
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";
    public static final String ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO";
    public static final int BOOM_EFFECT_OFF = 0;
    public static final int BOOM_EFFECT_ON = 1;
    public static final String BOOM_EFFECT_SETTINGS = "boom_key_unlock_enable";
    public static final int BOOM_KEY = 276;
    public static final int CAMERA_KEY_ON = 0;
    public static final String CAMERA_KEY_SETTINGS = "boom_key_action";
    private static final String EXTRAS_CAMERA_FACING = "android.intent.extras.CAMERA_FACING";
    private static final String GOOGLE_LENS_CLASSNAME = "com.google.vr.apps.ornament.app.lens.LensLauncherActivity";
    private static final String GOOGLE_LENS_PACKAGE = "com.google.ar.lens";
    public static final String KEY_RETURN_DATA = "return-data";
    public static final String KEY_SHOW_WHEN_LOCKED = "showWhenLocked";
    public static final String KEY_TREAT_UP_AS_BACK = "treat-up-as-back";
    private static final String LOW_POWER_MODE = "low_power";
    private static final int LOW_POWER_MODE_OFF = 0;
    private static final int LOW_POWER_MODE_ON = 1;
    private static final String MAPS_CLASS_NAME = "com.google.android.maps.MapsActivity";
    private static final String MAPS_PACKAGE_NAME = "com.google.android.apps.maps";
    private static final int MAX_PREVIEW_FPS_TIMES_1000 = 400000;
    public static final int ORIENTATION_HYSTERESIS = 5;
    public static final String PREFERENCE_SUFFIX = ".xml";
    private static final int PREFERRED_PREVIEW_FPS_TIMES_1000 = 30000;
    public static final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
    public static final int SETGPS = 1000;
    private static final String SPLITTER = "_";
    private static final Tag TAG = new Tag("Util");
    public static final String TIZR_PACKAGE_NAME = "com.app_tizr.app";
    public static final String TIZR_URI = "tizr://stream";
    public static final int TIZR_VIBRATOR_DURATION = 110;
    public static boolean isCameraModeSwitching = false;
    private static ImageFileNamer sImageFileNamer;
    private static int[] sLocation = new int[2];
    private static float sPixelDensity = 1.0f;

    private static class ImageFileNamer {
        private final SimpleDateFormat mFormat;
        private long mLastDate;
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            this.mFormat = new SimpleDateFormat(format);
        }

        public String generateName(long dateTaken) {
            String result = this.mFormat.format(new Date(dateTaken));
            if (dateTaken / 1000 == this.mLastDate / 1000) {
                this.mSameSecondCount++;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(result);
                stringBuilder.append(CameraUtil.SPLITTER);
                stringBuilder.append(this.mSameSecondCount);
                return stringBuilder.toString();
            }
            this.mLastDate = dateTaken;
            this.mSameSecondCount = 0;
            return result;
        }
    }

    private CameraUtil() {
    }

    public static void initialize(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics);
        sPixelDensity = metrics.density;
        sImageFileNamer = new ImageFileNamer(context.getString(R.string.image_file_name_format));
    }

    public static int dpToPixel(int dp) {
        return Math.round(sPixelDensity * ((float) dp));
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
        return rotateAndMirror(b, degrees, false);
    }

    public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
        if ((degrees == 0 && !mirror) || b == null) {
            return b;
        }
        Matrix m = new Matrix();
        if (mirror) {
            m.postScale(-1.0f, 1.0f);
            degrees = (degrees + 360) % 360;
            if (degrees == 0 || degrees == MediaProviderUtils.ROTATION_180) {
                m.postTranslate((float) b.getWidth(), 0.0f);
            } else if (degrees == 90 || degrees == MediaProviderUtils.ROTATION_270) {
                m.postTranslate((float) b.getHeight(), 0.0f);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid degrees=");
                stringBuilder.append(degrees);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (degrees != 0) {
            m.postRotate((float) degrees, ((float) b.getWidth()) / 2.0f, ((float) b.getHeight()) / 2.0f);
        }
        try {
            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (b == b2) {
                return b;
            }
            b.recycle();
            return b2;
        } catch (OutOfMemoryError e) {
            return b;
        }
    }

    public static Bitmap mirrorBitmap(Bitmap b) {
        Matrix matrix = new Matrix();
        matrix.setScale(-1.0f, 1.0f);
        Bitmap newBM = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
        if (newBM.equals(b)) {
            return newBM;
        }
        b.recycle();
        return newBM;
    }

    public static Bitmap rotateBitmap(Bitmap b, float rotation) {
        if (b == null) {
            return null;
        }
        int width = b.getWidth();
        int height = b.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation, ((float) width) / 2.0f, ((float) height) / 2.0f);
        Bitmap newBM = Bitmap.createBitmap(b, 0, 0, width, height, matrix, false);
        if (newBM.equals(b)) {
            return newBM;
        }
        b.recycle();
        return newBM;
    }

    public static int computeSampleSize(Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        if (initialSize > 8) {
            return 8 * ((initialSize + 7) / 8);
        }
        int roundedSize = 1;
        while (roundedSize < initialSize) {
            roundedSize <<= 1;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(Options options, int minSideLength, int maxNumOfPixels) {
        int upperBound;
        double w = (double) options.outWidth;
        double h = (double) options.outHeight;
        int lowerBound = maxNumOfPixels < 0 ? 1 : (int) Math.ceil(Math.sqrt((w * h) / ((double) maxNumOfPixels)));
        if (minSideLength < 0) {
            upperBound = 128;
        } else {
            upperBound = (int) Math.min(Math.floor(w / ((double) minSideLength)), Math.floor(h / ((double) minSideLength)));
        }
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if (maxNumOfPixels < 0 && minSideLength < 0) {
            return 1;
        }
        if (minSideLength < 0) {
            return lowerBound;
        }
        return upperBound;
    }

    public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
        try {
            Options options = new Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
            if (!(options.mCancel || options.outWidth == -1)) {
                if (options.outHeight != -1) {
                    options.inSampleSize = computeSampleSize(options, -1, maxNumOfPixels);
                    options.inJustDecodeBounds = false;
                    options.inDither = false;
                    options.inPreferredConfig = Config.ARGB_8888;
                    return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
                }
            }
            return null;
        } catch (OutOfMemoryError ex) {
            Log.e(TAG, "Got oom exception ", ex);
            return null;
        }
    }

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable th) {
            }
        }
    }

    public static void Assert(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static AlertDialog showStorageLowAndFinish(final Activity activity, String msgStr) {
        OnClickListener buttonListener = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        };
        activity.getTheme().resolveAttribute(16843605, new TypedValue(), true);
        if (activity.isFinishing()) {
            return null;
        }
        Log.e(TAG, "showStorageLowAndFinish");
        Builder builder = new Builder(activity);
        builder.setCancelable(false);
        builder.setTitle(R.string.camera_error_title_alert_dialog);
        builder.setMessage(msgStr);
        builder.setPositiveButton(R.string.storage_low_close_alert_dialog, buttonListener);
        builder.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == 4 && dialog != null) {
                    dialog.dismiss();
                    activity.finish();
                }
                return true;
            }
        });
        return builder.show();
    }

    public static AlertDialog MountedDialog(final CameraActivity activity, String title, String message, String pBtn, String nBtn) {
        activity.getTheme().resolveAttribute(16843605, new TypedValue(), true);
        if (activity.isFinishing()) {
            return null;
        }
        Builder builder = new Builder(activity);
        builder.setCancelable(false);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(pBtn, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (Storage.getSavePath().equals("0")) {
                    Storage.setSavePath("1");
                    String path = Storage.getSavePath();
                    activity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_SAVEPATH, "1");
                    ToastUtil.showToast(activity, activity.getResources().getString(R.string.data_stroage_changed_to_memory_card), 1);
                }
                activity.getCameraAppUI().setViewFinderLayoutVisibile(false);
            }
        });
        builder.setNegativeButton(nBtn, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                activity.getCameraAppUI().setViewFinderLayoutVisibile(false);
            }
        });
        builder.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                activity.getCameraAppUI().setViewFinderLayoutVisibile(false);
                if (keyCode == 4 && dialog != null) {
                    dialog.dismiss();
                }
                return true;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(-1).setTextColor(activity.getResources().getColor(R.color.dialog_button_font_color));
        dialog.getButton(-2).setTextColor(activity.getResources().getColor(R.color.dialog_button_font_color));
        return dialog;
    }

    public static AlertDialog UnAccessDialog(final CameraActivity activity, String title, String message, String pBtn) {
        activity.getTheme().resolveAttribute(16843605, new TypedValue(), true);
        if (activity.isFinishing()) {
            return null;
        }
        Builder builder = new Builder(activity);
        builder.setCancelable(false);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(pBtn, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                activity.getCameraAppUI().setViewFinderLayoutVisibile(false);
            }
        });
        builder.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == 4 && dialog != null) {
                    dialog.dismiss();
                    activity.getCameraAppUI().setViewFinderLayoutVisibile(false);
                }
                return true;
            }
        });
        return builder.show();
    }

    public static boolean checkLensAvailability(Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(GOOGLE_LENS_PACKAGE, 0);
        } catch (Exception e) {
            packageInfo = null;
        }
        if (packageInfo == null) {
            return false;
        }
        return true;
    }

    public static void launchLensApk(Context context) {
        try {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setComponent(new ComponentName(GOOGLE_LENS_PACKAGE, GOOGLE_LENS_CLASSNAME));
            intent.setFlags(268435456);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't find LensApk");
            stringBuilder.append(e);
            Log.d(tag, stringBuilder.toString());
        }
    }

    public static void showErrorAndFinish(Activity activity, int msgId) {
        activity.finish();
        Process.killProcess(Process.myPid());
    }

    public static void setPreviewIsOk() {
        isCameraModeSwitching = false;
    }

    public static <T> T checkNotNull(T object) {
        if (object != null) {
            return object;
        }
        throw new NullPointerException();
    }

    public static boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int nextPowerOf2(int n) {
        n--;
        n |= n >>> 16;
        n |= n >>> 8;
        n |= n >>> 4;
        n |= n >>> 2;
        return (n | (n >>> 1)) + 1;
    }

    public static float distance(float x, float y, float sx, float sy) {
        float dx = x - sx;
        float dy = y - sy;
        return (float) Math.sqrt((double) ((dx * dx) + (dy * dy)));
    }

    public static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public static float clamp(float x, float min, float max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public static float lerp(float a, float b, float t) {
        return ((b - a) * t) + a;
    }

    public static PointF normalizedSensorCoordsForNormalizedDisplayCoords(float nx, float ny, int sensorOrientation) {
        if (sensorOrientation == 0) {
            return new PointF(nx, ny);
        }
        if (sensorOrientation == 90) {
            return new PointF(ny, 1.0f - nx);
        }
        if (sensorOrientation == MediaProviderUtils.ROTATION_180) {
            return new PointF(1.0f - nx, 1.0f - ny);
        }
        if (sensorOrientation != MediaProviderUtils.ROTATION_270) {
            return null;
        }
        return new PointF(1.0f - ny, nx);
    }

    public static Size constrainToAspectRatio(Size size, float aspectRatio) {
        float width = (float) size.getWidth();
        float height = (float) size.getHeight();
        float currentAspectRatio = (1.0f * width) / height;
        if (currentAspectRatio > aspectRatio) {
            if (width > height) {
                width = height * aspectRatio;
            } else {
                height = width / aspectRatio;
            }
        } else if (currentAspectRatio < aspectRatio) {
            if (width < height) {
                width = height * aspectRatio;
            } else {
                height = width / aspectRatio;
            }
        }
        return new Size((int) width, (int) height);
    }

    public static int getDisplayRotation(Context context) {
        switch (((WindowManager) context.getSystemService("window")).getDefaultDisplay().getRotation()) {
            case 0:
                return 0;
            case 1:
                return 90;
            case 2:
                return MediaProviderUtils.ROTATION_180;
            case 3:
                return MediaProviderUtils.ROTATION_270;
            default:
                return 0;
        }
    }

    public static boolean isDefaultToPortrait(Context context) {
        int naturalWidth;
        int naturalHeight;
        Display currentDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Point displaySize = new Point();
        currentDisplay.getSize(displaySize);
        int orientation = currentDisplay.getRotation();
        if (orientation == 0 || orientation == 2) {
            naturalWidth = displaySize.x;
            naturalHeight = displaySize.y;
        } else {
            naturalWidth = displaySize.y;
            naturalHeight = displaySize.x;
        }
        return naturalWidth < naturalHeight;
    }

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == -1) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            changeOrientation = Math.min(dist, 360 - dist) >= 50;
        }
        if (changeOrientation) {
            return (((orientation + 45) / 90) * 90) % 360;
        }
        return orientationHistory;
    }

    private static Size getDefaultDisplaySize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService("window");
        Point res = new Point();
        windowManager.getDefaultDisplay().getSize(res);
        return new Size(res);
    }

    public static Size getOptimalPreviewSize(Context context, List<Size> sizes, double targetRatio) {
        int optimalPickIndex = getOptimalPreviewSizeIndex(context, Size.convert((List) sizes), targetRatio);
        if (optimalPickIndex == -1) {
            return null;
        }
        return (Size) sizes.get(optimalPickIndex);
    }

    public static int getOptimalPreviewSizeIndex(Context context, List<Size> sizes, double targetRatio) {
        double ASPECT_TOLERANCE;
        List<Size> list = sizes;
        if (targetRatio <= 1.3433d || targetRatio >= 1.35d) {
            ASPECT_TOLERANCE = 0.01d;
        } else {
            Log.w(TAG, "4:3 ratio out of normal tolerance, increasing tolerance to 0.02");
            ASPECT_TOLERANCE = 0.02d;
        }
        if (list == null) {
            return -1;
        }
        Size defaultDisplaySize = getDefaultDisplaySize(context);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("default size is ");
        stringBuilder.append(defaultDisplaySize);
        Log.w(tag, stringBuilder.toString());
        int targetHeight = Math.min(defaultDisplaySize.getWidth(), defaultDisplaySize.getHeight());
        double minDiff = Double.MAX_VALUE;
        int optimalSizeIndex = -1;
        int i = 0;
        while (i < sizes.size()) {
            Size size = (Size) list.get(i);
            int i2 = i;
            if (Math.abs((((double) size.getWidth()) / ((double) size.getHeight())) - targetRatio) <= ASPECT_TOLERANCE) {
                double minDiff2 = (double) Math.abs(size.getHeight() - targetHeight);
                if (minDiff2 < minDiff) {
                    optimalSizeIndex = i2;
                    minDiff = minDiff2;
                } else if (minDiff2 == minDiff && size.getHeight() < targetHeight) {
                    optimalSizeIndex = i2;
                    minDiff = minDiff2;
                }
            }
            i = i2 + 1;
        }
        if (optimalSizeIndex == -1) {
            Tag tag2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No preview size match the aspect ratio. available sizes: ");
            stringBuilder2.append(list);
            Log.w(tag2, stringBuilder2.toString());
            minDiff = Double.MAX_VALUE;
            int i3 = 0;
            while (true) {
                int i4 = i3;
                if (i4 >= sizes.size()) {
                    break;
                }
                Size size2 = (Size) list.get(i4);
                if (((double) Math.abs(size2.getHeight() - targetHeight)) < minDiff) {
                    optimalSizeIndex = i4;
                    minDiff = (double) Math.abs(size2.getHeight() - targetHeight);
                }
                i3 = i4 + 1;
            }
        }
        return optimalSizeIndex;
    }

    public static Size getOptimalVideoSnapshotPictureSize(List<Size> sizes, int targetWidth, int targetHeight) {
        if (sizes == null) {
            return null;
        }
        Size optimalSize = null;
        for (Size size : sizes) {
            if (size.height() == targetHeight && size.width() == targetWidth) {
                return size;
            }
        }
        double targetRatio = ((double) targetWidth) / ((double) targetHeight);
        for (Size size2 : sizes) {
            if (Math.abs((((double) size2.width()) / ((double) size2.height())) - targetRatio) <= 0.001d) {
                if (optimalSize == null || size2.width() > optimalSize.width()) {
                    optimalSize = size2;
                }
            }
        }
        if (optimalSize == null) {
            Log.w(TAG, "No picture size match the aspect ratio");
            for (Size size22 : sizes) {
                if (optimalSize == null || size22.width() > optimalSize.width()) {
                    optimalSize = size22;
                }
            }
        }
        return optimalSize;
    }

    public static Camera.Size getOptimalExifSize(List<Camera.Size> supportedExifSizes, Size photoSize) {
        if (photoSize == null) {
            return null;
        }
        Collections.sort(supportedExifSizes, new Comparator<Camera.Size>() {
            public int compare(Camera.Size size, Camera.Size t1) {
                if (size.width * size.height < t1.width * t1.height) {
                    return 1;
                }
                return -1;
            }
        });
        Camera.Size optimalThumbSize = null;
        double photoSizeRatio = ((double) photoSize.width()) / ((double) photoSize.height());
        for (Camera.Size size : supportedExifSizes) {
            if (Math.abs(photoSizeRatio - (((double) size.width) / ((double) size.height))) < 0.01d) {
                optimalThumbSize = size;
                break;
            }
        }
        return optimalThumbSize;
    }

    public static String getProximateResolution(Set set, int width, int height) {
        int size = width * height;
        int minError = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        String proximateResolution = null;
        for (String key : set) {
            int pos = key.indexOf(120);
            if (pos != -1) {
                int error = Math.abs((Integer.parseInt(key.substring(0, pos)) * Integer.parseInt(key.substring(pos + 1))) - size);
                if (error < minError) {
                    minError = error;
                    proximateResolution = key;
                }
            }
        }
        return proximateResolution;
    }

    public static Size getProximateSize(List<Size> sizes, int width, int height) {
        int maxPixels = 0;
        Size maxSize = null;
        for (Size size : sizes) {
            int w = size.width();
            int h = size.height();
            if (w <= width && h <= height && w * h > maxPixels) {
                maxPixels = w * h;
                maxSize = size;
            }
        }
        return maxSize;
    }

    public static boolean isMmsCapable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        if (telephonyManager == null) {
            return false;
        }
        try {
            return ((Boolean) TelephonyManager.class.getMethod("isVoiceCapable", new Class[0]).invoke(telephonyManager, new Object[0])).booleanValue();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            return true;
        }
    }

    public static int getCameraFacingIntentExtras(Activity currentActivity) {
        int intentCameraId = currentActivity.getIntent().getIntExtra(EXTRAS_CAMERA_FACING, -1);
        int frontCameraId;
        if (isFrontCameraIntent(intentCameraId)) {
            frontCameraId = ((CameraActivity) currentActivity).getCameraProvider().getFirstFrontCameraId();
            if (frontCameraId != -1) {
                return frontCameraId;
            }
            return -1;
        } else if (!isBackCameraIntent(intentCameraId)) {
            return -1;
        } else {
            frontCameraId = ((CameraActivity) currentActivity).getCameraProvider().getFirstBackCameraId();
            if (frontCameraId != -1) {
                return frontCameraId;
            }
            return -1;
        }
    }

    private static boolean isFrontCameraIntent(int intentCameraId) {
        return intentCameraId == 1;
    }

    private static boolean isBackCameraIntent(int intentCameraId) {
        return intentCameraId == 0;
    }

    public static boolean pointInView(float x, float y, View v) {
        v.getLocationInWindow(sLocation);
        return x >= ((float) sLocation[0]) && x < ((float) (sLocation[0] + v.getWidth())) && y >= ((float) sLocation[1]) && y < ((float) (sLocation[1] + v.getHeight()));
    }

    public static int[] getRelativeLocation(View reference, View view) {
        reference.getLocationInWindow(sLocation);
        int referenceX = sLocation[0];
        int referenceY = sLocation[1];
        view.getLocationInWindow(sLocation);
        int[] iArr = sLocation;
        iArr[0] = iArr[0] - referenceX;
        int[] iArr2 = sLocation;
        iArr2[1] = iArr2[1] - referenceY;
        return sLocation;
    }

    public static boolean isUriValid(Uri uri, ContentResolver resolver) {
        if (uri == null) {
            return false;
        }
        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Fail to open URI. URI=");
                stringBuilder.append(uri);
                Log.e(tag, stringBuilder.toString());
                return false;
            }
            pfd.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void dumpRect(RectF rect, String msg) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append("=(");
        stringBuilder.append(rect.left);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(rect.top);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(rect.right);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(rect.bottom);
        stringBuilder.append(")");
        Log.v(tag, stringBuilder.toString());
    }

    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    public static Rect rectFToRect(RectF rectF) {
        Rect rect = new Rect();
        rectFToRect(rectF, rect);
        return rect;
    }

    public static RectF rectToRectF(Rect r) {
        return new RectF((float) r.left, (float) r.top, (float) r.right, (float) r.bottom);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, int viewWidth, int viewHeight) {
        matrix.setScale(mirror ? -1.0f : 1.0f, 1.0f);
        matrix.postRotate((float) displayOrientation);
        matrix.postScale(((float) viewWidth) / 2000.0f, ((float) viewHeight) / 2000.0f);
        matrix.postTranslate(((float) viewWidth) / 2.0f, ((float) viewHeight) / 2.0f);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, Rect previewRect) {
        matrix.setScale(mirror ? -1.0f : 1.0f, 1.0f);
        matrix.postRotate((float) displayOrientation);
        Matrix mapping = new Matrix();
        mapping.setRectToRect(new RectF(-1000.0f, -1000.0f, 1000.0f, 1000.0f), rectToRectF(previewRect), ScaleToFit.FILL);
        matrix.setConcat(mapping, matrix);
    }

    public static String createJpegName(long dateTaken) {
        String generateName;
        synchronized (sImageFileNamer) {
            generateName = sImageFileNamer.generateName(dateTaken);
        }
        return generateName;
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        context.sendBroadcast(new Intent(ACTION_NEW_PICTURE, uri));
        context.sendBroadcast(new Intent("com.hmdglobal.app.camera.NEW_PICTURE", uri));
    }

    public static void cleanSharedPreference(Context context) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/data/data/");
        stringBuilder.append(context.getPackageName());
        stringBuilder.append("/shared_prefs");
        File dir = new File(stringBuilder.toString());
        if (dir.exists() && dir.isDirectory()) {
            for (File item : dir.listFiles()) {
                String path = item.getName();
                int index = path.lastIndexOf(PREFERENCE_SUFFIX);
                if (index != -1) {
                    String preferenceName = path.substring(0, index);
                    Tag tag = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("cleanSharedPreference  item: ");
                    stringBuilder2.append(path);
                    stringBuilder2.append(",  ");
                    stringBuilder2.append(preferenceName);
                    Log.d(tag, stringBuilder2.toString());
                    SharedPreferences preferences = context.getSharedPreferences(preferenceName, 0);
                    if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false) && preferences.equals(PreferenceManager.getDefaultSharedPreferences(context))) {
                        filterClearHelpTipsPreference(preferences);
                    } else if (preferenceName.equals(GuideActivity.PREF_GUIDE)) {
                        for (String key : preferences.getAll().keySet()) {
                            if (key.hashCode() == -1894205568 && key.equals(Keys.KEY_GUIDE)) {
                                preferences.edit().remove(key).commit();
                            } else {
                                preferences.edit().remove(key).commit();
                            }
                        }
                    } else {
                        preferences.edit().clear().commit();
                    }
                }
            }
        }
    }

    private static void filterClearHelpTipsPreference(SharedPreferences preferences) {
        for (String key : preferences.getAll().keySet()) {
            Object obj = -1;
            switch (key.hashCode()) {
                case -1198083857:
                    if (key.equals(Keys.KEY_HELP_TIP_WELCOME_STEP)) {
                        obj = 4;
                        break;
                    }
                    break;
                case -1014432882:
                    if (key.equals(Keys.KEY_HELP_TIP_GESTURE_FINISHED)) {
                        obj = null;
                        break;
                    }
                    break;
                case -924541127:
                    if (key.equals(Keys.KEY_HELP_TIP_PANO_FINISHED)) {
                        obj = 2;
                        break;
                    }
                    break;
                case 859374733:
                    if (key.equals(Keys.KEY_HELP_TIP_MANUAL_FINISHED)) {
                        obj = 1;
                        break;
                    }
                    break;
                case 2007838229:
                    if (key.equals(Keys.KEY_HELP_TIP_WELCOME_FINISHED)) {
                        obj = 3;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                case 1:
                case 2:
                case 3:
                case 4:
                    break;
                default:
                    preferences.edit().remove(key).commit();
                    break;
            }
        }
    }

    public static void fadeIn(View view, float startAlpha, float endAlpha, long duration) {
        if (view.getVisibility() != 0) {
            view.setVisibility(0);
            Animation animation = new AlphaAnimation(startAlpha, endAlpha);
            animation.setDuration(duration);
            view.startAnimation(animation);
        }
    }

    public static Bitmap downSample(byte[] data, int downSampleFactor) {
        try {
            Options opts = new Options();
            opts.inSampleSize = downSampleFactor;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (OutOfMemoryError e) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("OutOfMemoryError when down-sample the jpeg");
            stringBuilder.append(e);
            Log.e(tag, stringBuilder.toString());
            return null;
        }
    }

    public static void setGpsParameters(CameraSettings settings, Location loc) {
        CameraSettings cameraSettings = settings;
        settings.clearGpsData();
        boolean hasLatLon = false;
        double d = Camera2ParamsFragment.TARGET_EV;
        if (loc != null) {
            boolean z = (loc.getLatitude() == Camera2ParamsFragment.TARGET_EV && loc.getLongitude() == Camera2ParamsFragment.TARGET_EV) ? false : true;
            hasLatLon = z;
        }
        if (hasLatLon) {
            long j;
            Log.d(TAG, "Set gps location");
            long utcTimeSeconds = loc.getTime() / 1000;
            double latitude = loc.getLatitude();
            double longitude = loc.getLongitude();
            if (loc.hasAltitude()) {
                d = loc.getAltitude();
            }
            double d2 = d;
            if (utcTimeSeconds != 0) {
                j = utcTimeSeconds;
            } else {
                j = System.currentTimeMillis();
            }
            cameraSettings.setGpsData(new GpsData(latitude, longitude, d2, j, loc.getProvider().toUpperCase()));
            return;
        }
        cameraSettings.setGpsData(new GpsData(Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, System.currentTimeMillis() / 1000, null));
    }

    public static int[] getPhotoPreviewFpsRange(CameraCapabilities capabilities) {
        return getPhotoPreviewFpsRange(capabilities.getSupportedPreviewFpsRange());
    }

    public static int[] getPhotoPreviewFpsRange(List<int[]> frameRates) {
        for (int[] rate : frameRates) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("support ranges:");
            stringBuilder.append(Arrays.toString(rate));
            Log.d(tag, stringBuilder.toString());
        }
        if (frameRates.size() == 0) {
            Log.e(TAG, "No suppoted frame rates returned!");
            return null;
        }
        int lowestMinRate = MAX_PREVIEW_FPS_TIMES_1000;
        for (int[] rate2 : frameRates) {
            int minFps = rate2[0];
            if (rate2[1] >= PREFERRED_PREVIEW_FPS_TIMES_1000 && minFps <= PREFERRED_PREVIEW_FPS_TIMES_1000 && minFps < lowestMinRate) {
                lowestMinRate = minFps;
            }
        }
        int highestMaxRate = 0;
        int resultIndex = -1;
        for (int i = 0; i < frameRates.size(); i++) {
            int[] rate3 = (int[]) frameRates.get(i);
            int minFps2 = rate3[0];
            int maxFps = rate3[1];
            if (minFps2 == lowestMinRate && highestMaxRate < maxFps) {
                highestMaxRate = maxFps;
                resultIndex = i;
            }
        }
        if (resultIndex >= 0) {
            return (int[]) frameRates.get(resultIndex);
        }
        Log.e(TAG, "Can't find an appropiate frame rate range!");
        return null;
    }

    public static int[] getMaxPreviewFpsRange(List<int[]> frameRates) {
        if (frameRates == null || frameRates.size() <= 0) {
            return new int[0];
        }
        return (int[]) frameRates.get(frameRates.size() - 1);
    }

    public static void throwIfCameraDisabled(Context context) throws CameraDisabledException {
        if (((DevicePolicyManager) context.getSystemService("device_policy")).getCameraDisabled(null)) {
            throw new CameraDisabledException();
        }
    }

    private static void getGaussianMask(float[] mask) {
        int i;
        int len = mask.length;
        int mid = len / 2;
        float sigma = (float) len;
        int i2 = 0;
        float sum = 0.0f;
        for (i = 0; i <= mid; i++) {
            float ex = ((float) Math.exp((double) (((-(i - mid)) * (i - mid)) / (mid * mid)))) / ((2.0f * sigma) * sigma);
            int symmetricIndex = (len - 1) - i;
            mask[i] = ex;
            mask[symmetricIndex] = ex;
            sum += mask[i];
            if (i != symmetricIndex) {
                sum += mask[symmetricIndex];
            }
        }
        while (true) {
            i = i2;
            if (i < mask.length) {
                mask[i] = mask[i] / sum;
                i2 = i + 1;
            } else {
                return;
            }
        }
    }

    public static int addPixel(int pixel, int newPixel, float weight) {
        int g = MotionEventCompat.ACTION_POINTER_INDEX_MASK & ((pixel & MotionEventCompat.ACTION_POINTER_INDEX_MASK) + ((int) (((float) (newPixel & MotionEventCompat.ACTION_POINTER_INDEX_MASK)) * weight)));
        return ((ViewCompat.MEASURED_STATE_MASK | (16711680 & ((pixel & 16711680) + ((int) (((float) (newPixel & 16711680)) * weight))))) | g) | (((pixel & 255) + ((int) (((float) (newPixel & 255)) * weight))) & 255);
    }

    public static void blur(int[] src, int[] out, int w, int h, int size) {
        int y;
        int x;
        int sum;
        int i;
        int[] iArr = src;
        int i2 = w;
        int i3 = h;
        int i4 = size;
        float[] k = new float[i4];
        int off = i4 / 2;
        getGaussianMask(k);
        int[] tmp = new int[iArr.length];
        int rowPointer = 0;
        for (y = 0; y < i3; y++) {
            for (x = 0; x < i2; x++) {
                sum = 0;
                for (i = 0; i < k.length; i++) {
                    sum = addPixel(sum, iArr[rowPointer + clamp((x + i) - off, 0, i2 - 1)], k[i]);
                }
                tmp[x + rowPointer] = sum;
            }
            rowPointer += i2;
        }
        for (y = 0; y < i2; y++) {
            x = 0;
            for (rowPointer = 0; rowPointer < i3; rowPointer++) {
                sum = 0;
                for (i = 0; i < k.length; i++) {
                    sum = addPixel(sum, tmp[(clamp((rowPointer + i) - off, 0, i3 - 1) * i2) + y], k[i]);
                }
                out[y + x] = sum;
                x += i2;
            }
        }
    }

    public static Point resizeToFill(int imageWidth, int imageHeight, int imageRotation, int boundWidth, int boundHeight) {
        if (imageRotation % MediaProviderUtils.ROTATION_180 != 0) {
            int savedWidth = imageWidth;
            imageWidth = imageHeight;
            imageHeight = savedWidth;
        }
        if (imageWidth == -2 || imageHeight == -2) {
            imageWidth = boundWidth;
            imageHeight = boundHeight;
        }
        Point p = new Point();
        p.x = boundWidth;
        p.y = boundHeight;
        if (imageWidth * boundHeight > boundWidth * imageHeight) {
            p.y = (p.x * imageHeight) / imageWidth;
        } else {
            p.x = (p.y * imageWidth) / imageHeight;
        }
        return p;
    }

    public static void playVideo(Activity activity, Uri uri, String title) {
        try {
            CameraActivity cameraActivity = (CameraActivity) activity;
            if (cameraActivity.isSecureCamera()) {
                activity.finish();
            } else {
                cameraActivity.launchActivityByIntent(IntentHelper.getVideoPlayerIntent(uri).putExtra("android.intent.extra.TITLE", title).putExtra(KEY_TREAT_UP_AS_BACK, true));
            }
        } catch (ActivityNotFoundException e) {
            ToastUtil.showToast((Context) activity, activity.getString(R.string.video_err), 0);
        }
    }

    public static void showOnMap(Activity activity, double[] latLong) {
        try {
            String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?f=q&q=(%f,%f)", new Object[]{Double.valueOf(latLong[0]), Double.valueOf(latLong[1])});
            Intent mapsIntent = new Intent("android.intent.action.VIEW", Uri.parse(uri)).setComponent(new ComponentName(MAPS_PACKAGE_NAME, MAPS_CLASS_NAME));
            mapsIntent.addFlags(524288);
            activity.startActivity(mapsIntent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "GMM activity not found!", e);
            activity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(String.format(Locale.ENGLISH, "geo:%f,%f", new Object[]{Double.valueOf(latLong[0]), Double.valueOf(latLong[1])}))));
        }
    }

    public static String dumpStackTrace(int level) {
        StackTraceElement[] elems = Thread.currentThread().getStackTrace();
        level = level == 0 ? elems.length : Math.min(level + 3, elems.length);
        String ret = new String();
        for (int i = 3; i < level; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ret);
            stringBuilder.append("\t");
            stringBuilder.append(elems[i].toString());
            stringBuilder.append(10);
            ret = stringBuilder.toString();
        }
        return ret;
    }

    public static int getCameraThemeColorId(int modeIndex, Context context) {
        TypedArray colorRes = context.getResources().obtainTypedArray(R.array.camera_mode_theme_color);
        if (modeIndex < colorRes.length() && modeIndex >= 0) {
            return colorRes.getResourceId(modeIndex, 0);
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode index: ");
        stringBuilder.append(modeIndex);
        Log.e(tag, stringBuilder.toString());
        return 0;
    }

    public static int getCameraModeIconResId(int modeIndex, Context context) {
        TypedArray cameraModesIcons = context.getResources().obtainTypedArray(R.array.camera_mode_icon);
        if (modeIndex < cameraModesIcons.length() && modeIndex >= 0) {
            return cameraModesIcons.getResourceId(modeIndex, 0);
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode index: ");
        stringBuilder.append(modeIndex);
        Log.e(tag, stringBuilder.toString());
        return 0;
    }

    public static String getCameraModeText(int modeIndex, Context context) {
        String[] cameraModesText = context.getResources().getStringArray(R.array.camera_mode_text);
        if (modeIndex >= 0 && modeIndex < cameraModesText.length) {
            return cameraModesText[modeIndex];
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode index: ");
        stringBuilder.append(modeIndex);
        Log.e(tag, stringBuilder.toString());
        return new String();
    }

    public static String getCameraModeContentDescription(int modeIndex, Context context) {
        String[] cameraModesDesc = context.getResources().getStringArray(R.array.camera_mode_content_description);
        if (modeIndex >= 0 && modeIndex < cameraModesDesc.length) {
            return cameraModesDesc[modeIndex];
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode index: ");
        stringBuilder.append(modeIndex);
        Log.e(tag, stringBuilder.toString());
        return new String();
    }

    public static int getCameraShutterIconId(int modeIndex, Context context) {
        TypedArray shutterIcons = context.getResources().obtainTypedArray(R.array.camera_mode_shutter_icon);
        if (modeIndex >= 0 && modeIndex < shutterIcons.length()) {
            return shutterIcons.getResourceId(modeIndex, 0);
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode index: ");
        stringBuilder.append(modeIndex);
        Log.e(tag, stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode index: ");
        stringBuilder.append(modeIndex);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public static int getCameraShutterNormalStateIconId(int modeIndex, Context context) {
        TypedArray shutterIcons = context.getResources().obtainTypedArray(R.array.camera_mode_shutter_normal_state_icon);
        if (modeIndex < 0 || modeIndex >= shutterIcons.length()) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid mode index: ");
            stringBuilder.append(modeIndex);
            Log.e(tag, stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid mode index: ");
            stringBuilder.append(modeIndex);
            throw new IllegalStateException(stringBuilder.toString());
        }
        int resId = shutterIcons.getResourceId(modeIndex, 0);
        if (resId == 0) {
            return -1;
        }
        return resId;
    }

    public static int getCameraModeParentModeId(int modeIndex, Context context) {
        int[] cameraModeParent = context.getResources().getIntArray(R.array.camera_mode_nested_in_nav_drawer);
        if (modeIndex >= 0 && modeIndex < cameraModeParent.length) {
            return cameraModeParent[modeIndex];
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode index: ");
        stringBuilder.append(modeIndex);
        Log.e(tag, stringBuilder.toString());
        return 0;
    }

    public static int getCameraModeCoverIconResId(int modeIndex, Context context) {
        TypedArray cameraModesIcons = context.getResources().obtainTypedArray(R.array.camera_mode_cover_icon);
        if (modeIndex < cameraModesIcons.length() && modeIndex >= 0) {
            return cameraModesIcons.getResourceId(modeIndex, 0);
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid mode index: ");
        stringBuilder.append(modeIndex);
        Log.e(tag, stringBuilder.toString());
        return 0;
    }

    public static int getNumCpuCores() {
        try {
            return new File("/sys/devices/system/cpu/").listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    if (Pattern.matches("cpu[0-9]+", pathname.getName())) {
                        return true;
                    }
                    return false;
                }
            }).length;
        } catch (Exception e) {
            Log.e(TAG, "Failed to count number of cores, defaulting to 1", e);
            return 1;
        }
    }

    public static int getJpegRotation(int deviceOrientationDegrees, CameraCharacteristics characteristics) {
        if (deviceOrientationDegrees == -1) {
            return 0;
        }
        int facing = ((Integer) characteristics.get(CameraCharacteristics.LENS_FACING)).intValue();
        int sensorOrientation = ((Integer) characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        if (facing == 0) {
            return (sensorOrientation + deviceOrientationDegrees) % 360;
        }
        return ((sensorOrientation - deviceOrientationDegrees) + 360) % 360;
    }

    public static String serializeToJson(Map<String, Object> jsonEntities) {
        JSONObject job = new JSONObject();
        try {
            for (String key : jsonEntities.keySet()) {
                Object value = jsonEntities.get(key);
                if (value instanceof List) {
                    JSONArray array = new JSONArray();
                    for (Object item : (List) value) {
                        array.put(item);
                    }
                    job.put(key, array);
                } else {
                    job.put(key, jsonEntities.get(key));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String jsonString = job.toString();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("jsonString is \n");
        stringBuilder.append(jsonString);
        Log.w(tag, stringBuilder.toString());
        return jsonString;
    }

    public static Object parseJSON(String jsonString, String key) throws JSONException {
        return new JSONObject(jsonString).get(key);
    }

    public static List<String> getCompensatedFaceRects(Face[] faces, Size previewSize, Size photoSize) {
        List<String> rects = new ArrayList();
        if (faces == null) {
            return rects;
        }
        for (Face face : faces) {
            if (face.score >= 50) {
                Rect faceRectInPreview = face.rect;
                StringBuilder rectStrBuilder = new StringBuilder();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(faceRectInPreview.left);
                rectStrBuilder.append(stringBuilder.toString());
                rectStrBuilder.append(SPLITTER);
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(faceRectInPreview.top);
                rectStrBuilder.append(stringBuilder.toString());
                rectStrBuilder.append(SPLITTER);
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(faceRectInPreview.right);
                rectStrBuilder.append(stringBuilder.toString());
                rectStrBuilder.append(SPLITTER);
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(faceRectInPreview.bottom);
                rectStrBuilder.append(stringBuilder.toString());
                rects.add(rectStrBuilder.toString());
            }
        }
        return rects;
    }

    public static List<String> getCompensatedFaceRects(ArrayList<RectF> faces) {
        List<String> rects = new ArrayList();
        if (faces == null || faces.size() < 1) {
            return rects;
        }
        Iterator it = faces.iterator();
        while (it.hasNext()) {
            RectF face = (RectF) it.next();
            Rect faceRectInPreview = new Rect();
            face.round(faceRectInPreview);
            StringBuilder rectStrBuilder = new StringBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(faceRectInPreview.left);
            rectStrBuilder.append(stringBuilder.toString());
            rectStrBuilder.append(SPLITTER);
            stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(faceRectInPreview.top);
            rectStrBuilder.append(stringBuilder.toString());
            rectStrBuilder.append(SPLITTER);
            stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(faceRectInPreview.right);
            rectStrBuilder.append(stringBuilder.toString());
            rectStrBuilder.append(SPLITTER);
            stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(faceRectInPreview.bottom);
            rectStrBuilder.append(stringBuilder.toString());
            rects.add(rectStrBuilder.toString());
            Tag tag = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCompensatedFaceRects:");
            stringBuilder2.append(face);
            stringBuilder2.append(", ");
            stringBuilder2.append(faceRectInPreview);
            stringBuilder2.append(", ");
            stringBuilder2.append(rectStrBuilder.toString());
            Log.d(tag, stringBuilder2.toString());
        }
        return rects;
    }

    public static boolean isSupported(String value, List<String> supported) {
        return supported != null && supported.indexOf(value) >= 0;
    }

    public static AlertDialog showBatteryInfoDialog(final Activity activity, boolean cancelable, int titleId, int msgId, int buttonId, final Runnable runnable) {
        Builder builder = new Builder(activity);
        builder.setCancelable(cancelable);
        builder.setTitle(titleId);
        builder.setMessage(msgId);
        builder.setNegativeButton(buttonId, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (runnable != null) {
                    activity.runOnUiThread(runnable);
                }
            }
        });
        builder.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == 4 && dialog != null) {
                    if (runnable != null) {
                        activity.runOnUiThread(runnable);
                    }
                    dialog.dismiss();
                }
                return true;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(-1).setTextColor(activity.getResources().getColor(R.color.dialog_button_font_color));
        dialog.getButton(-2).setTextColor(activity.getResources().getColor(R.color.dialog_button_font_color));
        return dialog;
    }

    public static boolean gotoGpsSetting(final Activity activity, SettingsManager settingsManager, int resId) {
        if (!settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION) || checkGpsEnable(activity)) {
            return false;
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION, false);
        AlertDialog alertDialog = new Builder(activity).create();
        alertDialog.setIconAttribute(16843605);
        alertDialog.setIcon(resId);
        alertDialog.setTitle(R.string.location_service_dialog_title);
        alertDialog.setMessage(activity.getResources().getString(R.string.location_service_dialog_msg));
        alertDialog.setButton(-1, activity.getResources().getString(R.string.location_service__dialog_setting), new OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                activity.startActivityForResult(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"), 1000);
            }
        });
        alertDialog.setButton(-2, activity.getResources().getString(R.string.location_service_dialog_cancel), new OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                dialog.cancel();
            }
        });
        alertDialog.show();
        alertDialog.getButton(-1).setTextColor(activity.getColor(R.color.dialog_button_font_color));
        alertDialog.getButton(-2).setTextColor(activity.getColor(R.color.dialog_button_font_color));
        return true;
    }

    public static boolean backFromGpsSetting(Activity activity, SettingsManager settingsManager) {
        if (!checkGpsEnable(activity)) {
            return false;
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION, true);
        return true;
    }

    public static boolean checkGpsEnable(Context context) {
        boolean gpsEnabled = Secure.isLocationProviderEnabled(context.getContentResolver(), "gps");
        boolean networkEnabled = Secure.isLocationProviderEnabled(context.getContentResolver(), "network");
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("gpsEnabled = ");
        stringBuilder.append(gpsEnabled);
        stringBuilder.append(" ; networkEnabled = ");
        stringBuilder.append(networkEnabled);
        Log.w(tag, stringBuilder.toString());
        return gpsEnabled || networkEnabled;
    }

    public static boolean isBatterySaverEnabled(Context context) {
        boolean z = false;
        if (context == null) {
            return false;
        }
        int lowPowerMode = Global.getInt(context.getContentResolver(), LOW_POWER_MODE, 0);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LowPowerMode is ");
        stringBuilder.append(lowPowerMode);
        Log.d(tag, stringBuilder.toString());
        if (lowPowerMode == 1) {
            z = true;
        }
        return z;
    }
}
