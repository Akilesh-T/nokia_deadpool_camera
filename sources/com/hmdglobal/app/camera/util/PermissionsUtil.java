package com.hmdglobal.app.camera.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.util.ArrayList;

public class PermissionsUtil {
    public static boolean DEBUG = true;
    private static final int DIALOG_DISMISS_DELAY = 400;
    public static final int DURATION_CRITICAL = 300;
    private static final int DURATION_LOCATION = 500;
    public static final String PERMS_ACCESSWRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
    public static final String PERMS_ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    public static final String PERMS_ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    public static final String PERMS_CAMERA = "android.permission.CAMERA";
    public static final String PERMS_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    public static final String PERMS_RECORD_AUDIO = "android.permission.RECORD_AUDIO";
    public static final String PERMS_WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    public static final int REQUEST_ALERT_WINDOW = 32;
    public static final int REQUEST_CAMERA = 1;
    public static final int REQUEST_CRITICAL = 7;
    public static final int REQUEST_EXTERNAL_STORAGE = 2;
    public static final int REQUEST_LOCATION = 8;
    public static final int REQUEST_MICROPHONE = 4;
    public static final int REQUEST_SETTINGS = 16;
    private static final Tag TAG = new Tag("PermissionsUtil");
    private static Dialog grantAccess;
    private static Dialog location;
    private static RequestingPerms mRequestingPerms;
    private static Dialog permsNeeded;

    public static class CriticalPermsStatus {
        public boolean cameraGranted;
        public boolean microphoneGranted;
        public boolean storageGranted;

        public CriticalPermsStatus(boolean cameraGranted, boolean storageGranted, boolean microphoneGranted) {
            this.cameraGranted = cameraGranted;
            this.storageGranted = storageGranted;
            this.microphoneGranted = microphoneGranted;
        }

        public void set(boolean cameraGranted, boolean storageGranted, boolean microphoneGranted) {
            this.cameraGranted = cameraGranted;
            this.storageGranted = storageGranted;
            this.microphoneGranted = microphoneGranted;
        }
    }

    public static class RequestingPerms {
        public int code;
        public String[] perms;

        public RequestingPerms(int requestCode, String... permissions) {
            set(requestCode, permissions);
        }

        public void set(int requestCode, String... permissions) {
            this.code = requestCode;
            this.perms = permissions;
        }

        public void clear() {
            this.code = 0;
            this.perms = null;
        }
    }

    public static boolean isPermissionGranted(Context context, String permission) {
        int status = PermissionChecker.checkSelfPermission(context, permission);
        if (DEBUG) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Permission ");
            stringBuilder.append(permission);
            stringBuilder.append(", checkSelfPermission ");
            stringBuilder.append(status);
            Log.i(tag, stringBuilder.toString());
        }
        return status == 0;
    }

    public static boolean isExplanationNeeded(Activity activity, String permission) {
        boolean need = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
        if (DEBUG) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Permission ");
            stringBuilder.append(permission);
            stringBuilder.append(", shouldShowRequestPermissionRationale ");
            stringBuilder.append(need);
            Log.i(tag, stringBuilder.toString());
        }
        return need;
    }

    public static void requestPermissions(Activity activity, int requestCode, String... permissions) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
        if (mRequestingPerms == null) {
            mRequestingPerms = new RequestingPerms(requestCode, permissions);
            return;
        }
        mRequestingPerms.clear();
        mRequestingPerms.set(requestCode, permissions);
    }

    public static boolean inRequesting() {
        boolean z = false;
        if (mRequestingPerms == null) {
            return false;
        }
        if (mRequestingPerms.code > 0) {
            z = true;
        }
        return z;
    }

    public static RequestingPerms getRequestingPerms() {
        return mRequestingPerms;
    }

    public static boolean checkCriticalPerms(final Activity activity) {
        ArrayList<String> perms = new ArrayList();
        int code = 0;
        if (!isPermissionGranted(activity.getApplicationContext(), PERMS_CAMERA)) {
            perms.add(PERMS_CAMERA);
            code = 0 + 1;
        }
        if (!(isPermissionGranted(activity.getApplicationContext(), PERMS_READ_EXTERNAL_STORAGE) && isPermissionGranted(activity.getApplicationContext(), PERMS_WRITE_EXTERNAL_STORAGE))) {
            perms.add(PERMS_READ_EXTERNAL_STORAGE);
            perms.add(PERMS_WRITE_EXTERNAL_STORAGE);
            code += 2;
        }
        if (!isPermissionGranted(activity.getApplicationContext(), PERMS_RECORD_AUDIO)) {
            perms.add(PERMS_RECORD_AUDIO);
            code += 4;
        }
        if (code == 0) {
            return true;
        }
        final int requestCode = code;
        final String[] request = (String[]) perms.toArray(new String[perms.size()]);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                PermissionsUtil.requestPermissions(activity, requestCode, request);
            }
        }, 300);
        return false;
    }

    public static boolean isCriticalPermissionGranted(Context context) {
        int code = 0;
        if (!isPermissionGranted(context, PERMS_CAMERA)) {
            code = 0 + 1;
        }
        if (!(isPermissionGranted(context, PERMS_READ_EXTERNAL_STORAGE) && isPermissionGranted(context, PERMS_WRITE_EXTERNAL_STORAGE))) {
            code += 2;
        }
        if (!isPermissionGranted(context, PERMS_RECORD_AUDIO)) {
            code += 4;
        }
        return code == 0;
    }

    public static CriticalPermsStatus getCriticalPermsStatus(int requestCode) {
        CriticalPermsStatus status = new CriticalPermsStatus(false, false, false);
        switch (requestCode) {
            case 1:
                status.set(true, false, false);
                break;
            case 2:
                status.set(false, true, false);
                break;
            case 3:
                status.set(true, true, false);
                break;
            case 4:
                status.set(false, false, true);
                break;
            case 5:
                status.set(true, false, true);
                break;
            case 6:
                status.set(false, true, true);
                break;
            case 7:
                status.set(true, true, true);
                break;
        }
        return status;
    }

    public static boolean checkNonCriticalPerms(Activity activity) {
        if (isPermissionGranted(activity.getApplicationContext(), PERMS_ACCESS_COARSE_LOCATION) && isPermissionGranted(activity.getApplicationContext(), PERMS_ACCESS_FINE_LOCATION)) {
            return true;
        }
        requestPermissions(activity, 8, PERMS_ACCESS_COARSE_LOCATION, PERMS_ACCESS_FINE_LOCATION);
        return false;
    }

    public static void showDialogPermsNeeded(Activity activity) {
        permsNeeded = new Dialog(activity, R.style.perms_dialog);
        permsNeeded.setContentView((FrameLayout) activity.getLayoutInflater().inflate(R.layout.perms_needed, null), new LayoutParams(-1, -1));
        permsNeeded.getWindow().setWindowAnimations(R.style.perms_dialog_anim);
        permsNeeded.show();
    }

    public static void dismissDialogPermsNeeded() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (PermissionsUtil.permsNeeded != null) {
                    PermissionsUtil.permsNeeded.dismiss();
                    PermissionsUtil.permsNeeded = null;
                }
            }
        }, 400);
    }

    public static void showDialogGrantAccess(final Activity activity, final boolean immediate) {
        grantAccess = new Dialog(activity, R.style.perms_dialog);
        FrameLayout f = (FrameLayout) activity.getLayoutInflater().inflate(R.layout.grant_access, null);
        ((LinearLayout) f.findViewById(R.id.exit_layout)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                activity.finish();
                PermissionsUtil.dismissDialogGrantAccess(immediate);
            }
        });
        ((LinearLayout) f.findViewById(R.id.settings_layout)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                PermissionsUtil.gotoSettings(activity);
                activity.finish();
                PermissionsUtil.dismissDialogGrantAccess(immediate);
            }
        });
        grantAccess.setContentView(f, new LayoutParams(-1, -1));
        grantAccess.setCancelable(false);
        grantAccess.getWindow().setWindowAnimations(R.style.perms_dialog_anim);
        grantAccess.show();
    }

    public static void showDialogGrantAccess(Activity activity) {
        activity.finish();
    }

    public static void dismissDialogGrantAccess(boolean immediate) {
        if (!immediate) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    if (PermissionsUtil.grantAccess != null) {
                        PermissionsUtil.grantAccess.dismiss();
                        PermissionsUtil.grantAccess = null;
                    }
                }
            }, 400);
        } else if (grantAccess != null) {
            grantAccess.dismiss();
            grantAccess = null;
        }
    }

    public static void showDialogLocation(Activity activity) {
        location = new Dialog(activity, R.style.perms_dialog);
        location.setContentView((FrameLayout) activity.getLayoutInflater().inflate(R.layout.tag_location_new, null), new LayoutParams(-1, -1));
        location.setCancelable(false);
        location.getWindow().setWindowAnimations(R.style.perms_dialog_anim);
        location.show();
    }

    public static void dismissDialogLocation() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (PermissionsUtil.location != null) {
                    PermissionsUtil.location.dismiss();
                    PermissionsUtil.location = null;
                }
            }
        }, 400);
    }

    public static void showSnackBar(Activity activity, View view, int textString, int actionString) {
    }

    public static void dismissAllDialogWhenDestroy() {
        if (permsNeeded != null) {
            permsNeeded.dismiss();
            permsNeeded = null;
        }
        if (grantAccess != null) {
            grantAccess.dismiss();
            grantAccess = null;
        }
        if (location != null) {
            location.dismiss();
            location = null;
        }
    }

    public static void gotoSettings(Context context) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("package:");
        stringBuilder.append(context.getPackageName());
        context.startActivity(new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.parse(stringBuilder.toString())));
    }

    public static boolean isIntentExisting(Context context, String action) {
        if (context.getPackageManager().queryIntentActivities(new Intent(action), 131072).size() > 0) {
            return true;
        }
        return false;
    }
}
