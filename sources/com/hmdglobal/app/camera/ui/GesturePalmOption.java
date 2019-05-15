package com.hmdglobal.app.camera.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.AnimationManager;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class GesturePalmOption extends LinearLayout {
    private static final int HIDE_GESTURE_HELP_TIP = 0;
    private static final int HIDE_GESTURE_HELP_TIP_DELAY = 3000;
    private static final Tag TAG = new Tag("GesturePalmOption");
    private CameraActivity mActivity;
    private Button mGestureDismissButton;
    private FrameLayout mGestureHelpTip;
    private RotateImageView mGesturePalm;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            if (message.what == 0) {
                GesturePalmOption.this.hideGestureHelpTip();
            }
        }
    };
    private ValueAnimator mHideHelpTipAnimator;
    private ValueAnimator mShowHelpTipAnmator;

    public GesturePalmOption(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mActivity = (CameraActivity) context;
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mGesturePalm = (RotateImageView) findViewById(R.id.gesture_palm);
        this.mGesturePalm.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if ("android.intent.action.MAIN".equals(GesturePalmOption.this.mActivity.getIntent().getAction())) {
                    GesturePalmOption.this.animateShowGestureHelpTip();
                    GesturePalmOption.this.mHandler.removeMessages(0);
                    GesturePalmOption.this.mHandler.sendEmptyMessageDelayed(0, 3000);
                    return;
                }
                Log.e(GesturePalmOption.TAG, "not action main and don't show gesture tip");
            }
        });
        this.mGestureHelpTip = (FrameLayout) this.mActivity.findViewById(R.id.front_gesture_help_view);
        this.mGestureDismissButton = (Button) this.mGestureHelpTip.findViewById(R.id.gesture_help_dismiss);
        this.mGestureDismissButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                GesturePalmOption.this.hideGestureHelpTip();
            }
        });
    }

    public void hideGestureHelpTip() {
        if (this.mGestureHelpTip.getVisibility() == 0) {
            animateHideGestureHelpTip();
            this.mHandler.removeMessages(0);
        }
    }

    private void animateShowGestureHelpTip() {
        if (this.mGestureHelpTip.getVisibility() != 0) {
            if (this.mHideHelpTipAnimator == null || this.mShowHelpTipAnmator == null) {
                this.mShowHelpTipAnmator = AnimationManager.buildShowingAnimator(this.mGestureHelpTip);
                this.mHideHelpTipAnimator = AnimationManager.buildHidingAnimator(this.mGestureHelpTip);
            }
            if (this.mHideHelpTipAnimator.isRunning()) {
                this.mHideHelpTipAnimator.cancel();
            }
            if (!this.mShowHelpTipAnmator.isRunning()) {
                this.mShowHelpTipAnmator.start();
            }
        }
    }

    private void animateHideGestureHelpTip() {
        if (this.mGestureHelpTip.getVisibility() != 4) {
            if (this.mHideHelpTipAnimator == null || this.mShowHelpTipAnmator == null) {
                this.mShowHelpTipAnmator = AnimationManager.buildShowingAnimator(this.mGestureHelpTip);
                this.mHideHelpTipAnimator = AnimationManager.buildHidingAnimator(this.mGestureHelpTip);
            }
            if (this.mShowHelpTipAnmator.isRunning()) {
                this.mShowHelpTipAnmator.cancel();
            }
            if (!this.mHideHelpTipAnimator.isRunning()) {
                this.mHideHelpTipAnimator.start();
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != 0 && this.mHandler != null && this.mHandler.hasMessages(0)) {
            this.mHandler.removeMessages(0);
        }
    }
}
