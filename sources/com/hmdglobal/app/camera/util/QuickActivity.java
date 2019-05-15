package com.hmdglobal.app.camera.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public abstract class QuickActivity extends Activity {
    private static final long ON_RESUME_DELAY_MILLIS = 20;
    private static final Tag TAG = new Tag("QuickActivity");
    private boolean mCanceledResumeTasks = false;
    private Handler mMainHandler;
    private final Runnable mOnResumeTasks = new Runnable() {
        public void run() {
            QuickActivity.this.logLifecycle("onResumeTasks", true);
            if (QuickActivity.this.mPaused) {
                QuickActivity.this.onResumeTasks();
                QuickActivity.this.mPaused = false;
                QuickActivity.this.mCanceledResumeTasks = false;
            }
            QuickActivity.this.logLifecycle("onResumeTasks", false);
        }
    };
    private boolean mPaused;

    /* Access modifiers changed, original: protected|final */
    public final void onNewIntent(Intent intent) {
        logLifecycle("onNewIntent", true);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Intent Action = ");
        stringBuilder.append(intent.getAction());
        Log.v(tag, stringBuilder.toString());
        setIntent(intent);
        super.onNewIntent(intent);
        onNewIntentTasks(intent);
        logLifecycle("onNewIntent", false);
    }

    private String getPackageVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 16384).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    /* Access modifiers changed, original: protected|final */
    public final void onCreate(Bundle bundle) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("VERSION_NAME = ");
        stringBuilder.append(getPackageVersion());
        Log.v(tag, stringBuilder.toString());
        logLifecycle("onCreate", true);
        tag = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Intent Action = ");
        stringBuilder2.append(getIntent().getAction());
        Log.v(tag, stringBuilder2.toString());
        super.onCreate(bundle);
        this.mMainHandler = new Handler(getMainLooper());
        onCreateTasks(bundle);
        this.mPaused = true;
        logLifecycle("onCreate", false);
    }

    /* Access modifiers changed, original: protected|final */
    public final void onStart() {
        logLifecycle("onStart", true);
        onStartTasks();
        super.onStart();
        logLifecycle("onStart", false);
    }

    /* Access modifiers changed, original: protected|final */
    public final void onResume() {
        logLifecycle("onResume", true);
        this.mMainHandler.removeCallbacks(this.mOnResumeTasks);
        if (delayOnResumeOnStart() && !this.mCanceledResumeTasks) {
            this.mMainHandler.postDelayed(this.mOnResumeTasks, ON_RESUME_DELAY_MILLIS);
        } else if (this.mPaused) {
            onResumeTasks();
            this.mPaused = false;
            this.mCanceledResumeTasks = false;
        }
        super.onResume();
        logLifecycle("onResume", false);
    }

    /* Access modifiers changed, original: protected|final */
    public final void onPause() {
        logLifecycle("onPause", true);
        this.mMainHandler.removeCallbacks(this.mOnResumeTasks);
        if (this.mPaused) {
            this.mCanceledResumeTasks = true;
        } else {
            onPauseTasks();
            this.mPaused = true;
        }
        super.onPause();
        logLifecycle("onPause", false);
    }

    /* Access modifiers changed, original: protected|final */
    public final void onStop() {
        if (isChangingConfigurations()) {
            Log.v(TAG, "changing configurations");
        }
        logLifecycle("onStop", true);
        onStopTasks();
        super.onStop();
        logLifecycle("onStop", false);
    }

    /* Access modifiers changed, original: protected|final */
    public final void onRestart() {
        logLifecycle("onRestart", true);
        onReStartTasks();
        super.onRestart();
        logLifecycle("onRestart", false);
    }

    /* Access modifiers changed, original: protected|final */
    public final void onDestroy() {
        logLifecycle("onDestroy", true);
        onDestroyTasks();
        super.onDestroy();
        logLifecycle("onDestroy", false);
    }

    private void logLifecycle(String methodName, boolean start) {
        String prefix = start ? "START" : "END";
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(" ");
        stringBuilder.append(methodName);
        stringBuilder.append(": Activity = ");
        stringBuilder.append(toString());
        Log.v(tag, stringBuilder.toString());
    }

    private boolean delayOnResumeOnStart() {
        return CameraActivity.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(getIntent().getAction());
    }

    /* Access modifiers changed, original: protected */
    public void onNewIntentTasks(Intent newIntent) {
    }

    /* Access modifiers changed, original: protected */
    public void onCreateTasks(Bundle savedInstanceState) {
    }

    /* Access modifiers changed, original: protected */
    public void onStartTasks() {
    }

    /* Access modifiers changed, original: protected */
    public void onResumeTasks() {
    }

    /* Access modifiers changed, original: protected */
    public void onPauseTasks() {
    }

    /* Access modifiers changed, original: protected */
    public void onStopTasks() {
    }

    /* Access modifiers changed, original: protected */
    public void onDestroyTasks() {
    }

    /* Access modifiers changed, original: protected */
    public void onReStartTasks() {
    }
}
