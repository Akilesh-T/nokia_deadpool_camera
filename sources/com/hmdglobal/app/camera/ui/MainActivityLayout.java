package com.hmdglobal.app.camera.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.HelpTipsManager;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraAppUI.NonDecorWindowSizeChangedListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.widget.ModeOptionsOverlay;

public class MainActivityLayout extends FrameLayout {
    private static final int SWIPE_TIME_OUT = 500;
    private final Tag TAG = new Tag("MainActivityLayout");
    private FrameLayout camera_app_root;
    private boolean mCheckToIntercept;
    private MotionEvent mDown;
    private EffectsContainerWrapper mEffectsContainerWrapper;
    private HelpTipsManager mHelpTipsManager;
    private final boolean mIsCaptureIntent;
    private ModeOptionsOverlay mModeOptionsOverlay;
    private StereoModeStripView mModeStrip;
    private NonDecorWindowSizeChangedListener mNonDecorWindowSizeChangedListener;
    private boolean mRequestToInterceptTouchEvents;
    private final int mSlop;
    @Deprecated
    private boolean mSwipeEnabled;
    private View mTouchReceiver;

    public MainActivityLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        boolean z = false;
        this.mRequestToInterceptTouchEvents = false;
        this.mTouchReceiver = null;
        this.mNonDecorWindowSizeChangedListener = null;
        this.mSwipeEnabled = true;
        this.mSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        String action = ((Activity) context).getIntent().getAction();
        if ("android.media.action.IMAGE_CAPTURE".equals(action) || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action)) {
            z = true;
        }
        this.mIsCaptureIntent = z;
    }

    @Deprecated
    public void setSwipeEnabled(boolean enabled) {
        this.mSwipeEnabled = enabled;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0) {
            this.mCheckToIntercept = true;
            this.mDown = MotionEvent.obtain(ev);
            if (this.mHelpTipsManager == null || !this.mHelpTipsManager.isHelpTipShowExist()) {
                this.mTouchReceiver = null;
            } else {
                this.mTouchReceiver = this.mHelpTipsManager.getHelpTipView();
            }
            this.mRequestToInterceptTouchEvents = false;
            return false;
        } else if (this.mRequestToInterceptTouchEvents) {
            this.mRequestToInterceptTouchEvents = false;
            onTouchEvent(this.mDown);
            return true;
        } else if (ev.getActionMasked() == 5) {
            this.mCheckToIntercept = false;
            return false;
        } else if (!this.mCheckToIntercept || ev.getEventTime() - ev.getDownTime() > 500 || this.mIsCaptureIntent || !this.mSwipeEnabled) {
            return false;
        } else {
            int deltaX = (int) (ev.getX() - this.mDown.getX());
            int deltaY = (int) (ev.getY() - this.mDown.getY());
            if (ev.getActionMasked() == 2 && Math.abs(deltaX) > this.mSlop && !this.mModeStrip.isPaused()) {
                if (deltaX >= Math.abs(deltaY) * 2) {
                    if (this.mHelpTipsManager != null && this.mHelpTipsManager.isHelpTipShowExist() && this.mHelpTipsManager.helpTipCheckToIntercept()) {
                        this.mTouchReceiver = this.mHelpTipsManager.getHelpTipView();
                    } else {
                        this.mTouchReceiver = this.mModeStrip;
                    }
                    onTouchEvent(this.mDown);
                    return true;
                } else if (deltaX < (-Math.abs(deltaY)) * 2) {
                    if (this.mHelpTipsManager != null && this.mHelpTipsManager.isHelpTipShowExist() && this.mHelpTipsManager.helpTipCheckToIntercept()) {
                        this.mTouchReceiver = this.mHelpTipsManager.getHelpTipView();
                    } else {
                        this.mTouchReceiver = this.mModeStrip;
                    }
                    onTouchEvent(this.mDown);
                    return true;
                }
            }
            return false;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mEffectsContainerWrapper == null) {
            this.mEffectsContainerWrapper = (EffectsContainerWrapper) this.camera_app_root.findViewById(R.id.effect_layout_wrapper);
        }
        if (this.mEffectsContainerWrapper != null && this.mEffectsContainerWrapper.getVisibility() == 0 && ev.getY() > ((float) this.mEffectsContainerWrapper.getTop()) && ev.getY() < ((float) this.mEffectsContainerWrapper.getBottom())) {
            return true;
        }
        if (this.mTouchReceiver != null) {
            return this.mTouchReceiver.dispatchTouchEvent(ev);
        }
        return false;
    }

    public void onFinishInflate() {
        this.camera_app_root = (FrameLayout) findViewById(R.id.camera_app_root);
        this.mModeStrip = (StereoModeStripView) findViewById(R.id.mode_strip_view);
        this.mModeOptionsOverlay = (ModeOptionsOverlay) findViewById(R.id.mode_options_overlay);
        this.mEffectsContainerWrapper = (EffectsContainerWrapper) this.camera_app_root.findViewById(R.id.effect_layout_wrapper);
    }

    public void redirectTouchEventsTo(View touchReceiver) {
        if (touchReceiver == null) {
            Log.e(this.TAG, "Cannot redirect touch to a null receiver.");
            return;
        }
        this.mTouchReceiver = touchReceiver;
        this.mRequestToInterceptTouchEvents = true;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mNonDecorWindowSizeChangedListener != null) {
            this.mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec), CameraUtil.getDisplayRotation(getContext()));
            this.mModeOptionsOverlay.requestLayout();
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setNonDecorWindowSizeChangedListener(NonDecorWindowSizeChangedListener listener) {
        this.mNonDecorWindowSizeChangedListener = listener;
        if (this.mNonDecorWindowSizeChangedListener != null) {
            this.mNonDecorWindowSizeChangedListener.onNonDecorWindowSizeChanged(getMeasuredWidth(), getMeasuredHeight(), CameraUtil.getDisplayRotation(getContext()));
            this.mModeOptionsOverlay.requestLayout();
        }
    }

    public void setHelpTipManager(HelpTipsManager helpTipManager) {
        this.mHelpTipsManager = helpTipManager;
    }
}
