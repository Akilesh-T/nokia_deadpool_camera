package com.hmdglobal.app.camera;

import android.app.KeyguardManager;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.util.PermissionsUtil;

public class SecureCameraActivity extends CameraActivity {
    Tag TAG = new Tag("CAM_SecureCameraActivity");
    private BroadcastReceiver mBatInfoReceiver = null;
    private boolean mIsKeyguardLocked = false;
    private KeyguardManager mKeyguardManager = null;
    private boolean mNoPermsGranted = false;

    public void onCreateTasks(Bundle state) {
        this.mIsKeyguardLocked = isKeyguardLocked();
        if (!getSharedPreferences(GuideActivity.PREF_GUIDE, 0).getBoolean(Keys.KEY_GUIDE, false)) {
            this.mNoPermsGranted = true;
            Intent intent = new Intent(this, GuideActivity.class);
            intent.setAction(getIntent().getAction());
            startActivity(intent);
            finish();
        } else if (!this.mIsKeyguardLocked || PermissionsUtil.isCriticalPermissionGranted(this)) {
            super.onCreateTasks(state);
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.USER_PRESENT");
            filter.addAction("android.intent.action.USER_UNLOCKED");
            this.mBatInfoReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    Log.d(SecureCameraActivity.this.TAG, "onReceive");
                    SecureCameraActivity.this.finish();
                }
            };
            registerReceiver(this.mBatInfoReceiver, filter);
        } else {
            this.mNoPermsGranted = true;
            sendNotificationForPerms();
            getWindow().addFlags(524288);
        }
    }

    public void onNewIntentTasks(Intent intent) {
        if (!this.mNoPermsGranted) {
            super.onNewIntentTasks(intent);
        }
    }

    public void onStartTasks() {
        if (!this.mNoPermsGranted) {
            super.onStartTasks();
        }
    }

    public void onResumeTasks() {
        if (this.mNoPermsGranted) {
            finish();
        } else {
            super.onResumeTasks();
        }
    }

    /* Access modifiers changed, original: protected */
    public void onStopTasks() {
        if (!this.mNoPermsGranted) {
            super.onStopTasks();
        }
    }

    public void onPauseTasks() {
        if (!this.mNoPermsGranted) {
            super.onPauseTasks();
        }
    }

    public void onDestroyTasks() {
        if (!this.mNoPermsGranted) {
            super.onDestroyTasks();
            unregisterReceiver(this.mBatInfoReceiver);
            this.mBatInfoReceiver = null;
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isKeyguardLocked() {
        if (this.mKeyguardManager == null) {
            this.mKeyguardManager = (KeyguardManager) getSystemService("keyguard");
        }
        if (this.mKeyguardManager != null) {
            return this.mKeyguardManager.isKeyguardLocked();
        }
        return false;
    }

    private void sendNotificationForPerms() {
        Intent intent = new Intent();
        String CLASS_NAME = "com.hmdglobal.app.camera.PermsActivity";
        intent.setComponent(new ComponentName(getPackageName(), "com.hmdglobal.app.camera.PermsActivity"));
        intent.setFlags(268468224);
        ((NotificationManager) getSystemService("notification")).notify(1, new Builder(this).setSmallIcon(17301642).setContentTitle(getResources().getString(R.string.permission_title)).setContentText(getResources().getString(R.string.permission_content)).setWhen(System.currentTimeMillis()).setContentIntent(PendingIntent.getActivity(this, 0, intent, 0)).setAutoCancel(true).setDefaults(2).build());
    }

    /* Access modifiers changed, original: protected */
    public boolean checkCriticalPermissions() {
        if (!this.mIsKeyguardLocked) {
            return super.checkCriticalPermissions();
        }
        if (this.mNoPermsGranted) {
            return false;
        }
        return true;
    }

    /* Access modifiers changed, original: protected */
    public boolean checkNonCriticalPermissions() {
        if (!this.mIsKeyguardLocked) {
            return super.checkNonCriticalPermissions();
        }
        boolean z = PermissionsUtil.isPermissionGranted(this, PermissionsUtil.PERMS_ACCESS_COARSE_LOCATION) && PermissionsUtil.isPermissionGranted(this, PermissionsUtil.PERMS_ACCESS_FINE_LOCATION);
        return z;
    }
}
