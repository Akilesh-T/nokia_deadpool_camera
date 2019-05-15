package com.hmdglobal.app.camera.rapidcapture;

import android.app.KeyguardManager;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import com.hmdglobal.app.camera.FyuseAPI;
import com.hmdglobal.app.camera.GrantAccessActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.rapidcapture.RapidCaptureHelper.Callback;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.PermissionsUtil;

public class RapidCaptureService extends Service {
    public static final int BURSTSHOT_IN_PROGRESS = 2;
    private static final String CMD_BURSTSHOT_CAPTURE = "longpress";
    private static final String CMD_BURSTSHOT_CAPTURE_STOP = "stop";
    private static final String CMD_ONESHOT_CAPTURE = "shortpress";
    public static final int IDLE = 0;
    public static final int LOW_BATTERY_LEVEL = 5;
    public static final int ONESHOT_IN_PROGRESS = 1;
    private static final String TAG = "RapidCaptureService";
    private static final int TYPE_LOW_BATTERY = 0;
    private static final int TYPE_LOW_STORAGE = 1;
    private static final int TYPE_PERMISSION_NEED = 2;
    public static long mServiceOncreateTime;
    public static long mServiceOnstartTime;
    private int mCameraState = 0;
    RapidCaptureHelper mRapidCaptureHelper;
    private ServiceCallback mServiceCallback = new ServiceCallback();

    private class ServiceCallback implements Callback {
        private ServiceCallback() {
        }

        public void onCaptureDone(boolean reset) {
            if (RapidCaptureService.this.mRapidCaptureHelper != null) {
                RapidCaptureService.this.mRapidCaptureHelper.pause();
            }
            if (reset) {
                RapidCaptureService.this.mRapidCaptureHelper = null;
            }
            RapidCaptureService.this.mCameraState = 0;
        }
    }

    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return null;
    }

    public void onCreate() {
        mServiceOncreateTime = System.currentTimeMillis();
        super.onCreate();
        this.mRapidCaptureHelper = RapidCaptureHelper.getInstance();
        this.mRapidCaptureHelper.init(getApplication(), this.mServiceCallback);
        Log.d(TAG, "onCreate");
        this.mCameraState = 0;
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (this.mRapidCaptureHelper != null) {
            this.mRapidCaptureHelper.destroy();
            this.mRapidCaptureHelper = null;
        }
        super.onDestroy();
    }

    private void showWarningNotification(Context context, int type) {
        if (this.mRapidCaptureHelper == null) {
            this.mRapidCaptureHelper = RapidCaptureHelper.getInstance();
        }
        this.mRapidCaptureHelper.acquireScreenWakeLock(context);
        Resources res = context.getResources();
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        if (type == 0) {
            nm.notify(0, new Builder(context).setSmallIcon(17301543).setWhen(System.currentTimeMillis()).setContentTitle(res.getString(R.string.battery_warning_title)).setContentText(res.getString(R.string.battery_warning_content)).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0)).setAutoCancel(true).build());
        } else if (type == 2) {
            nm.notify(0, new Builder(context).setSmallIcon(17301543).setWhen(System.currentTimeMillis()).setContentTitle(res.getString(R.string.permission_title)).setContentText(res.getString(R.string.permission_content)).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, GrantAccessActivity.class), 1342177280)).setAutoCancel(true).build());
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startId:");
        stringBuilder.append(startId);
        Log.d(str, stringBuilder.toString());
        if (!checkPreconditions()) {
            return 2;
        }
        parseCommand(intent);
        return 1;
    }

    public boolean checkPreconditions() {
        if (PermissionsUtil.isCriticalPermissionGranted(getApplicationContext())) {
            if (ApiHelper.isLOrHigher()) {
                int batteryLevel = ((BatteryManager) getSystemService("batterymanager")).getIntProperty(4);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("batteryLevel:");
                stringBuilder.append(batteryLevel);
                Log.d(str, stringBuilder.toString());
                if (batteryLevel > 0 && batteryLevel <= 5) {
                    showWarningNotification(getApplicationContext(), 0);
                    return false;
                }
            }
            return true;
        }
        showWarningNotification(getApplicationContext(), 2);
        return false;
    }

    public void parseCommand(Intent intent) {
        if (intent != null) {
            Bundle b = intent.getExtras();
            if (b != null) {
                String cmd = b.getString(FyuseAPI.COMMAND);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parseCommand:");
                stringBuilder.append(cmd);
                Log.d(str, stringBuilder.toString());
                if (CMD_ONESHOT_CAPTURE.equalsIgnoreCase(cmd)) {
                    commandOneshotCapture();
                } else if (CMD_BURSTSHOT_CAPTURE.equalsIgnoreCase(cmd)) {
                    commandBurstCapture();
                } else if (CMD_BURSTSHOT_CAPTURE_STOP.equalsIgnoreCase(cmd)) {
                    commandStopCapture();
                }
            }
        }
    }

    private boolean checkCapturesPreconditions(Context context) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkCapturesPreconditions ");
        stringBuilder.append(this.mCameraState);
        Log.i(str, stringBuilder.toString());
        if (this.mCameraState != 0) {
            return false;
        }
        boolean isScreenOn = ((PowerManager) context.getSystemService("power")).isScreenOn();
        boolean isInKeyguard = ((KeyguardManager) context.getSystemService("keyguard")).isKeyguardLocked();
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("checkCapturesPreconditions ");
        stringBuilder2.append(isScreenOn);
        stringBuilder2.append(",  ");
        stringBuilder2.append(isInKeyguard);
        stringBuilder2.append(", ");
        stringBuilder2.append(RapidViewImageActivity.mIsRunning);
        Log.i(str2, stringBuilder2.toString());
        if (RapidViewImageActivity.mIsRunning) {
            return false;
        }
        return true;
    }

    private void commandOneshotCapture() {
        mServiceOnstartTime = System.currentTimeMillis();
        Log.i(TAG, "commandOneshotCapture");
        if (checkCapturesPreconditions(this)) {
            if (this.mRapidCaptureHelper == null) {
                this.mRapidCaptureHelper = RapidCaptureHelper.getInstance();
                this.mRapidCaptureHelper.init(getApplication(), this.mServiceCallback);
            }
            this.mCameraState = 1;
            this.mRapidCaptureHelper.resume(RapidCaptureHelper.TYPE_ONESHOT);
        }
    }

    private void commandBurstCapture() {
        Log.i(TAG, "commandBurstCapture");
        if (checkCapturesPreconditions(this)) {
            if (this.mRapidCaptureHelper == null) {
                this.mRapidCaptureHelper = RapidCaptureHelper.getInstance();
                this.mRapidCaptureHelper.init(getApplication(), this.mServiceCallback);
            }
            this.mCameraState = 2;
            this.mRapidCaptureHelper.resume(RapidCaptureHelper.TYPE_BURSTSHOT);
        }
    }

    private void commandStopCapture() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("commandStopCapture  mCameraState:");
        stringBuilder.append(this.mCameraState);
        Log.i(str, stringBuilder.toString());
        if (this.mCameraState == 2) {
            this.mRapidCaptureHelper.stopBurst();
        }
    }
}
