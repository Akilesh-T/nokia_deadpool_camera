package com.hmdglobal.app.camera;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class IdleSleepActivity extends Activity {
    private static String TAG = IdleSleepActivity.class.getSimpleName();
    private Handler handler = new Handler();
    private BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_REASON = "reason";
        String SYSTEM_RECENT_APPS = "recentapps";

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.CLOSE_SYSTEM_DIALOGS")) {
                String reason = intent.getStringExtra(this.SYSTEM_REASON);
                if (TextUtils.equals(reason, this.SYSTEM_HOME_KEY) || TextUtils.equals(reason, this.SYSTEM_RECENT_APPS)) {
                    IdleSleepActivity.this.finish();
                }
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                Log.d(IdleSleepActivity.TAG, "ACTION_SCREEN_ON  >>>>>>>>>>>>>>>>>>>>>>>>00000000000000000000)");
                if (!IdleSleepActivity.this.isKeyguardLocked()) {
                    Log.d(IdleSleepActivity.TAG, "ACTION_SCREEN_ON  >>>>>>>>>>>>>>>>>>>>>>>>IdleSleepActivity.this.finish()");
                    IdleSleepActivity.this.finish();
                }
            }
        }
    };
    private KeyguardManager mKeyguardManager = null;

    /* Access modifiers changed, original: protected|final */
    public final void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.keyguard_widget);
        findViewById(R.id.keyguard_widget).setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                IdleSleepActivity.this.finish();
                return true;
            }
        });
        IntentFilter homeFilter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        homeFilter.addAction("android.intent.action.SCREEN_ON");
        registerReceiver(this.mHomeKeyEventReceiver, homeFilter);
    }

    /* Access modifiers changed, original: protected|final */
    public final void onRestart() {
        if (isKeyguardLocked()) {
            Log.d(TAG, "onRestart  >>>>>>>>>>>>>>>>>>>>>>>>isKeyguardLocked");
            this.handler.postDelayed(new Runnable() {
                public void run() {
                    IdleSleepActivity.this.finish();
                }
            }, 650);
        }
        super.onRestart();
    }

    /* Access modifiers changed, original: protected|final */
    public final void onStop() {
        super.onStop();
    }

    /* Access modifiers changed, original: protected|final */
    public final void onDestroy() {
        if (this.mHomeKeyEventReceiver != null) {
            unregisterReceiver(this.mHomeKeyEventReceiver);
        }
        super.onDestroy();
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
}
