package com.hmdglobal.app.camera.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.hmdglobal.app.camera.AnimationManager;
import com.hmdglobal.app.camera.R;

public class FaceBeautyOption extends LinearLayout implements OnClickListener {
    private static final int HIDE_MANUAL_PROGRESS = 0;
    private static final int HIDE_MANUAL_PROGRESS_DELAY = 3000;
    private static final String TAG = "FaceBeautyOption";
    private static final int UPDATE_FACEBEAUTY_SETTING = 1;
    private static final int UPDATE_FACEBEAUTY_SETTING_DELAY = 100;
    private FaceBeautySettingCallBack mCallback;
    private View mCustomSeekBar;
    private RotateImageView mFacebeautyMenu;
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    FaceBeautyOption.this.animateHide();
                    FaceBeautyOption.this.mFacebeautyMenu.setImageLevel(0);
                    return;
                case 1:
                    if (FaceBeautyOption.this.mCallback != null) {
                        FaceBeautyOption.this.mCallback.updateFaceBeautySetting(FaceBeautyOption.this.mKey, FaceBeautyOption.this.mProgress);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private ValueAnimator mHideSeekbarAnimator;
    private String mKey;
    private int mProgress;
    private SeekBar mSeekBar;
    private ValueAnimator mShowSeekbarAnmator;

    public interface FaceBeautySettingCallBack {
        void updateFaceBeautySetting(String str, int i);
    }

    public FaceBeautyOption(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mSeekBar = (SeekBar) findViewById(R.id.customseekbar);
        this.mCustomSeekBar = findViewById(R.id.seekbar);
        this.mFacebeautyMenu = (RotateImageView) findViewById(R.id.face_beauty_menu);
        this.mFacebeautyMenu.setOnClickListener(this);
    }

    public void initData(String key, int defaultValue, int maxValue) {
        this.mKey = key;
        this.mSeekBar.setMax(maxValue);
        this.mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                FaceBeautyOption.this.mHandler.removeMessages(0);
                FaceBeautyOption.this.mHandler.sendEmptyMessageDelayed(0, 3000);
                FaceBeautyOption.this.mHandler.removeMessages(1);
                FaceBeautyOption.this.mHandler.sendEmptyMessage(1);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                FaceBeautyOption.this.mHandler.removeMessages(0);
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                FaceBeautyOption.this.mProgress = progress;
                FaceBeautyOption.this.mHandler.removeMessages(1);
                FaceBeautyOption.this.mHandler.sendEmptyMessageDelayed(1, 100);
            }
        });
        this.mProgress = defaultValue;
        this.mSeekBar.setProgress(defaultValue);
    }

    public void onClick(View v) {
        if (this.mCustomSeekBar.getVisibility() == 0) {
            this.mFacebeautyMenu.setImageLevel(0);
            animateHide();
            this.mHandler.removeMessages(0);
            return;
        }
        this.mFacebeautyMenu.setImageLevel(1);
        animateShow();
        this.mHandler.removeMessages(0);
        this.mHandler.sendEmptyMessageDelayed(0, 3000);
    }

    public void hideSeekBar() {
        if (this.mCustomSeekBar.getVisibility() == 0) {
            this.mFacebeautyMenu.setImageLevel(0);
            animateHide();
            this.mHandler.removeMessages(0);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != 0 && this.mHandler != null && this.mHandler.hasMessages(0)) {
            this.mHandler.removeMessages(0);
        }
    }

    private void animateHide() {
        if (this.mCustomSeekBar.getVisibility() != 8) {
            if (this.mHideSeekbarAnimator == null || this.mShowSeekbarAnmator == null) {
                this.mShowSeekbarAnmator = AnimationManager.buildShowingAnimator(this.mCustomSeekBar);
                this.mHideSeekbarAnimator = AnimationManager.buildHidingAnimator(this.mCustomSeekBar);
            }
            if (this.mShowSeekbarAnmator.isRunning()) {
                this.mShowSeekbarAnmator.cancel();
            }
            if (!this.mHideSeekbarAnimator.isRunning()) {
                this.mHideSeekbarAnimator.start();
            }
        }
    }

    private void animateShow() {
        if (this.mCustomSeekBar.getVisibility() != 0) {
            if (this.mHideSeekbarAnimator == null || this.mShowSeekbarAnmator == null) {
                this.mShowSeekbarAnmator = AnimationManager.buildShowingAnimator(this.mCustomSeekBar);
                this.mHideSeekbarAnimator = AnimationManager.buildHidingAnimator(this.mCustomSeekBar);
            }
            if (this.mHideSeekbarAnimator.isRunning()) {
                this.mHideSeekbarAnimator.cancel();
            }
            if (!this.mShowSeekbarAnmator.isRunning()) {
                this.mShowSeekbarAnmator.start();
            }
        }
    }

    public void setFaceBeautySettingCallBack(FaceBeautySettingCallBack callback) {
        this.mCallback = callback;
    }

    public void reset() {
        this.mCallback = null;
        if (this.mHandler != null) {
            this.mHandler.removeMessages(0);
            this.mHandler.removeMessages(1);
        }
    }
}
