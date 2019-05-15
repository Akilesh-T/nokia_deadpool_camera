package com.hmdglobal.app.camera;

import android.app.Activity;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class OnScreenHint {
    static final Tag TAG = new Tag("OnScreenHint");
    int mGravity = 81;
    private final Handler mHandler = new Handler();
    private final Runnable mHide = new Runnable() {
        public void run() {
            OnScreenHint.this.handleHide();
        }
    };
    float mHorizontalMargin;
    View mNextView;
    private final LayoutParams mParams = new LayoutParams();
    private final Runnable mShow = new Runnable() {
        public void run() {
            OnScreenHint.this.handleShow();
        }
    };
    float mVerticalMargin;
    View mView;
    private final WindowManager mWM;
    int mX;
    int mY;

    private OnScreenHint(Activity activity) {
        this.mWM = (WindowManager) activity.getSystemService("window");
        this.mY = activity.getResources().getDimensionPixelSize(R.dimen.hint_y_offset);
        this.mParams.height = -2;
        this.mParams.width = -2;
        this.mParams.flags = 24;
        this.mParams.format = -3;
        this.mParams.windowAnimations = R.style.Animation_OnScreenHint;
        this.mParams.type = 1000;
        this.mParams.setTitle("OnScreenHint");
    }

    public void show() {
        if (this.mNextView != null) {
            this.mHandler.post(this.mShow);
            return;
        }
        throw new RuntimeException("View is not initialized");
    }

    public void cancel() {
        this.mHandler.post(this.mHide);
    }

    public static OnScreenHint makeText(Activity activity, CharSequence text) {
        OnScreenHint result = new OnScreenHint(activity);
        View v = ((LayoutInflater) activity.getSystemService("layout_inflater")).inflate(R.layout.on_screen_hint, null);
        ((TextView) v.findViewById(R.id.message)).setText(text);
        result.mNextView = v;
        return result;
    }

    public void setText(CharSequence s) {
        if (this.mNextView != null) {
            TextView tv = (TextView) this.mNextView.findViewById(R.id.message);
            if (tv != null) {
                tv.setText(s);
                return;
            }
            throw new RuntimeException("This OnScreenHint was not created with OnScreenHint.makeText()");
        }
        throw new RuntimeException("This OnScreenHint was not created with OnScreenHint.makeText()");
    }

    private synchronized void handleShow() {
        if (this.mView != this.mNextView) {
            handleHide();
            this.mView = this.mNextView;
            int gravity = this.mGravity;
            this.mParams.gravity = gravity;
            if ((gravity & 7) == 7) {
                this.mParams.horizontalWeight = 1.0f;
            }
            if ((gravity & 112) == 112) {
                this.mParams.verticalWeight = 1.0f;
            }
            this.mParams.x = this.mX;
            this.mParams.y = this.mY;
            this.mParams.verticalMargin = this.mVerticalMargin;
            this.mParams.horizontalMargin = this.mHorizontalMargin;
            if (this.mView.getParent() != null) {
                this.mWM.removeView(this.mView);
            }
            this.mWM.addView(this.mView, this.mParams);
        }
    }

    private synchronized void handleHide() {
        if (this.mView != null) {
            if (this.mView.getParent() != null) {
                this.mWM.removeView(this.mView);
            }
            this.mView = null;
        }
    }
}
