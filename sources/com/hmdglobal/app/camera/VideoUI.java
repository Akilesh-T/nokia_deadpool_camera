package com.hmdglobal.app.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;
import com.hmdglobal.app.camera.FocusOverlayManager.FocusUI;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.EvoSlider;
import com.hmdglobal.app.camera.ui.EvoSlider.EVOChangedListener;
import com.hmdglobal.app.camera.ui.FocusOverlay;
import com.hmdglobal.app.camera.ui.IntentReviewControls;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper.OnAllViewRemovedListener;
import com.hmdglobal.app.camera.ui.PreviewOverlay;
import com.hmdglobal.app.camera.ui.PreviewOverlay.OnZoomChangedListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener;
import com.hmdglobal.app.camera.ui.RecordTimeLayout;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;
import com.hmdglobal.app.camera.ui.RotatableButton;
import com.hmdglobal.app.camera.ui.RotateLayout;
import com.hmdglobal.app.camera.ui.ZoomBar;
import com.hmdglobal.app.camera.ui.ZoomBar.ProgressChangeListener;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;

public class VideoUI implements PreviewStatusListener, OnAllViewRemovedListener {
    private static final int BOOK_KEY_TIP_ANIMATION_DELAY = 1000;
    private static final int BOOK_KEY_TIP_ANIMATION_DURATION = 400;
    private static final int DISMISS_BOOK_KEY_TIP_DELAY = 3000;
    private static final Tag TAG = new Tag("VideoUI");
    private static final float UNSET = 0.0f;
    private View boomkeyTip;
    private final CameraActivity mActivity;
    private final AnimationManager mAnimationManager;
    private float mAspectRatio = 0.0f;
    private int mBoomKeyTipRepeatCount = 0;
    private final VideoController mController;
    private final EvoSlider mEvoSlider;
    private final FocusOverlay mFocusUI;
    private final RotateLayout mImageReviewLayout;
    private final IntentReviewControls mIntentReviewControls;
    private final RotateLayout mIntentReviewLayout;
    private LinearLayout mLabelsLinearLayout;
    protected boolean mMediaRecoderRecordingPaused = false;
    private final OnGestureListener mPreviewGestureListener = new SimpleOnGestureListener() {
        public boolean onSingleTapUp(MotionEvent ev) {
            VideoUI.this.mController.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }
    };
    private final PreviewOverlay mPreviewOverlay;
    private final OnTouchListener mPreviewTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (VideoUI.this.mEvoSlider.getVisibility() == 8 || event.getAction() != 2) {
                return false;
            }
            if (VideoUI.this.mZoomBar.getVisibility() == 0) {
                VideoUI.this.mZoomBar.setVisibility(8);
            }
            return VideoUI.this.mEvoSlider.onTouchEvent(event);
        }
    };
    private boolean mRecordingStarted = false;
    private RecordTimeLayout mRecordingTimeRect;
    private TextView mRecordingTimeView;
    private ImageView mReviewImage;
    protected final View mRootView;
    private boolean mStop = true;
    private final RotatableButton mVideoCaptureButton;
    private final RotatableButton mVideoPauseButton;
    private final ZoomBar mZoomBar;
    private final RotateLayout mZoomLayout;
    private float mZoomMax;
    private ValueAnimator va;

    private class ZoomChangeListener implements OnZoomChangedListener {
        private ZoomChangeListener() {
        }

        /* synthetic */ ZoomChangeListener(VideoUI x0, AnonymousClass1 x1) {
            this();
        }

        public void onZoomValueChanged(float ratio) {
            if (VideoUI.this.mZoomBar != null) {
                VideoUI.this.mZoomBar.zoomRatioChanged(ratio);
            }
            VideoUI.this.mController.onZoomChanged(ratio);
        }

        public void onZoomStart() {
        }

        public void onZoomEnd() {
        }
    }

    public void onPreviewLayoutChanged(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        this.mEvoSlider.setBound(right - left, bottom - top);
    }

    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return true;
    }

    public boolean shouldAutoAdjustBottomBar() {
        return true;
    }

    public void onPreviewFlipped() {
        this.mController.updateCameraOrientation();
    }

    public VideoUI(CameraActivity activity, VideoController controller, View parent) {
        this.mActivity = activity;
        this.mController = controller;
        this.mRootView = parent;
        ModuleLayoutWrapper moduleRoot = (ModuleLayoutWrapper) this.mRootView.findViewById(R.id.module_layout);
        moduleRoot.setOnAllViewRemovedListener(this, this.mActivity);
        this.mActivity.getLayoutInflater().inflate(R.layout.video_module, moduleRoot, true);
        this.mPreviewOverlay = (PreviewOverlay) this.mRootView.findViewById(R.id.preview_overlay);
        this.mZoomLayout = (RotateLayout) this.mRootView.findViewById(R.id.zoom_rotate_layout);
        this.mZoomBar = (ZoomBar) this.mRootView.findViewById(R.id.zoom_bar);
        this.mZoomBar.setProgressChangeListener(new ProgressChangeListener() {
            public void onProgressChanged(float ratio) {
                VideoUI.this.mPreviewOverlay.setRatio(ratio);
                VideoUI.this.mController.onZoomChanged(ratio);
            }
        });
        this.mImageReviewLayout = (RotateLayout) this.mRootView.findViewById(R.id.intent_review_imageview_layout);
        this.mIntentReviewLayout = (RotateLayout) this.mRootView.findViewById(R.id.intent_review_rotate_layout);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mImageReviewLayout, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mIntentReviewLayout, false));
        this.mIntentReviewControls = (IntentReviewControls) this.mRootView.findViewById(R.id.intent_review_controls);
        this.mIntentReviewControls.setFromVideoUI(true);
        initializeMiscControls();
        this.mAnimationManager = new AnimationManager();
        this.mFocusUI = (FocusOverlay) this.mRootView.findViewById(R.id.focus_overlay);
        this.mFocusUI.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (VideoUI.this.mEvoSlider.getVisibility() == 0) {
                    VideoUI.this.mEvoSlider.dispatchTouchEvent(event);
                }
                return true;
            }
        });
        this.mEvoSlider = (EvoSlider) this.mRootView.findViewById(R.id.evo_slider);
        this.mEvoSlider.setEvoListener(new EVOChangedListener() {
            public void onEVOChanged(int value) {
                VideoUI.this.mController.onEvoChanged(value);
            }
        });
        this.mVideoPauseButton = (RotatableButton) this.mRootView.findViewById(R.id.video_pause_button);
        this.mVideoPauseButton.setImageResource(R.drawable.camera_btn_pause);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mVideoPauseButton, true));
        this.mVideoPauseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.i("camera", "mVideoPauseButton");
                VideoUI.this.pauseVideo();
            }
        });
        this.mVideoCaptureButton = (RotatableButton) this.mRootView.findViewById(R.id.video_snap_button);
        this.mVideoCaptureButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (VideoUI.this.mActivity.getHelpTipsManager() != null) {
                    VideoUI.this.mActivity.getHelpTipsManager().goNextHelpTipStage();
                }
                if (!VideoUI.this.mStop) {
                    VideoUI.this.hideBoomKeyTipUI();
                }
                VideoUI.this.mController.doVideoCapture();
            }
        });
        this.mActivity.addLockableToListenerPool(this.mVideoCaptureButton);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mRecordingTimeRect, true));
    }

    public void initEvoSlider(float x, float y) {
        this.mEvoSlider.setCoord(x, y);
    }

    public void parseEvoBound(int max, int min) {
        this.mEvoSlider.setValueBound(max, min);
    }

    public void resetEvoSlider(int evo) {
        this.mEvoSlider.resetSlider(evo);
    }

    public void showEvoSlider() {
        this.mEvoSlider.setVisibility(0);
    }

    public void pauseVideo() {
        if (this.mActivity.isRecording()) {
            if (this.mMediaRecoderRecordingPaused) {
                this.mVideoPauseButton.setImageResource(R.drawable.camera_btn_pause);
            } else {
                this.mVideoPauseButton.setImageResource(R.drawable.camera_btn_record);
            }
            setRecordingTimeImage(this.mMediaRecoderRecordingPaused);
            this.mMediaRecoderRecordingPaused ^= 1;
            this.mController.pauseVideoRecording();
        }
    }

    public void unlockCaptureView() {
        if (this.mVideoCaptureButton != null) {
            this.mVideoCaptureButton.unLockSelf();
        }
    }

    public void setPreviewSize(int width, int height) {
        if (width == 0 || height == 0) {
            com.hmdglobal.app.camera.debug.Log.w(TAG, "Preview size should not be 0.");
            return;
        }
        float aspectRatio;
        if (width > height) {
            aspectRatio = ((float) width) / ((float) height);
        } else {
            aspectRatio = ((float) height) / ((float) width);
        }
        setAspectRatio(aspectRatio);
    }

    public FocusUI getFocusUI() {
        return this.mFocusUI;
    }

    public void animateFlash() {
        this.mController.startPreCaptureAnimation();
    }

    public void cancelAnimations() {
        this.mAnimationManager.cancelAnimations();
    }

    public void setOrientationIndicator(int orientation, boolean animation) {
        if (this.mLabelsLinearLayout == null) {
            return;
        }
        if (((orientation / 90) & 1) == 0) {
            this.mLabelsLinearLayout.setOrientation(1);
        } else {
            this.mLabelsLinearLayout.setOrientation(0);
        }
    }

    private void initializeMiscControls() {
        this.mRecordingTimeView = (TextView) this.mRootView.findViewById(R.id.recording_time);
        this.mRecordingTimeRect = (RecordTimeLayout) this.mRootView.findViewById(R.id.recording_time_rect);
        this.mReviewImage = (ImageView) this.mRootView.findViewById(R.id.intent_review_imageview);
        this.mLabelsLinearLayout = (LinearLayout) this.mRootView.findViewById(R.id.labels);
    }

    public void updateOnScreenIndicators(CameraSettings settings) {
    }

    public void setAspectRatio(float ratio) {
        if (ratio > 0.0f) {
            float aspectRatio = ratio > 1.0f ? ratio : 1.0f / ratio;
            if (aspectRatio != this.mAspectRatio) {
                this.mAspectRatio = aspectRatio;
                this.mController.updatePreviewAspectRatio(this.mAspectRatio);
            }
        }
    }

    public void setSwipingEnabled(boolean enable) {
        this.mActivity.setSwipingEnabled(enable);
    }

    public void showPreviewBorder(boolean enable) {
    }

    public boolean isVideoCaptureIntent() {
        return "android.media.action.VIDEO_CAPTURE".equals(this.mActivity.getIntent().getAction());
    }

    public void showRecordingUI(boolean recording) {
        this.mRecordingStarted = recording;
        if (recording) {
            com.hmdglobal.app.camera.debug.Log.w(TAG, "show recording UI");
            this.mRecordingTimeView.setText("");
            this.mVideoCaptureButton.setVisibility(0);
            if (isVideoCaptureIntent()) {
                this.mVideoCaptureButton.setVisibility(8);
            }
            this.mRecordingTimeView.setVisibility(0);
            this.mRecordingTimeView.announceForAccessibility(this.mActivity.getResources().getString(R.string.video_recording_started));
            return;
        }
        this.mRecordingTimeView.announceForAccessibility(this.mActivity.getResources().getString(R.string.video_recording_stopped));
        this.mRecordingTimeView.setVisibility(8);
        this.mVideoCaptureButton.setVisibility(8);
    }

    public void hideCapButton() {
        this.mVideoCaptureButton.setVisibility(8);
        this.mVideoPauseButton.setVisibility(8);
    }

    public void setRecordingTimeImage(boolean recording) {
        Drawable drawable;
        if (recording) {
            drawable = this.mActivity.getResources().getDrawable(R.drawable.ic_recording_indicator_oval);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
            this.mRecordingTimeView.setCompoundDrawables(drawable, null, null, null);
            return;
        }
        drawable = this.mActivity.getResources().getDrawable(R.drawable.camera_icon_top_pause);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        this.mRecordingTimeView.setCompoundDrawables(drawable, null, null, null);
    }

    public void lockRecordingOrientation() {
        this.mActivity.lockRotatableOrientation(this.mRecordingTimeRect.hashCode());
        this.mActivity.getCameraAppUI().lockButtonOrientation();
    }

    public void unlockRecordingOrientation() {
        this.mActivity.unlockRotatableOrientation(this.mRecordingTimeRect.hashCode());
        this.mActivity.getCameraAppUI().unlockButtonOrientation();
    }

    public void showBoomKeyTipUI() {
    }

    private void showBoomKeyTipAnimation() {
        this.boomkeyTip = this.mActivity.findViewById(R.id.boom_key_tip);
        this.boomkeyTip.setVisibility(0);
        this.boomkeyTip.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (v.getId() == R.id.boom_key_tip) {
                    VideoUI.this.hideBoomKeyTipUI();
                }
            }
        });
        this.boomkeyTip.postDelayed(new Runnable() {
            public void run() {
                VideoUI.this.hideBoomKeyTipUI();
            }
        }, 3000);
        String keyBoomKeyTip = CustomFields.DEF_VIDEO_RECORDING_BOOMKEY_TIP_CUSTOMIZE;
        int defaultMarginTop = this.mActivity.getResources().getInteger(R.integer.video_recording_boom_key_tip_margin_top_default);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("The defaultMarginTop is ");
        stringBuilder.append(defaultMarginTop);
        com.hmdglobal.app.camera.debug.Log.d(tag, stringBuilder.toString());
        int boomKeyTipMarginTop = CustomUtil.getInstance().getInt(keyBoomKeyTip, defaultMarginTop);
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("The boomKeyTipMarginTop is ");
        stringBuilder2.append(boomKeyTipMarginTop);
        com.hmdglobal.app.camera.debug.Log.d(tag2, stringBuilder2.toString());
        this.boomkeyTip.setPadding(0, defaultMarginTop, 0, 0);
        final View boomKeyBtn = this.boomkeyTip.findViewById(R.id.boom_key_btn);
        boomKeyBtn.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                boomKeyBtn.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                final float boomKeyBtnWidth = (float) boomKeyBtn.getMeasuredWidth();
                VideoUI.this.va = VideoUI.ofFloat(boomKeyBtn, "translationX", 0.0f, -boomKeyBtnWidth);
                VideoUI.this.va.setRepeatMode(2);
                VideoUI.this.va.setRepeatCount(-1);
                VideoUI.this.va.setDuration(400);
                VideoUI.this.va.addListener(new AnimatorListenerAdapter() {
                    public void onAnimationRepeat(Animator animation) {
                        if (VideoUI.this.mStop) {
                            animation.cancel();
                        } else if (boomKeyBtn.getTranslationX() > boomKeyBtnWidth / 2.0f) {
                            VideoUI.this.mBoomKeyTipRepeatCount = VideoUI.this.mBoomKeyTipRepeatCount + 1;
                            if (VideoUI.this.mBoomKeyTipRepeatCount % 2 == 0) {
                                animation.cancel();
                                animation.setStartDelay(1000);
                                animation.start();
                            }
                        }
                    }
                });
                VideoUI.this.va.start();
            }
        });
    }

    public void hideBoomKeyTipUI() {
    }

    private static ObjectAnimator ofFloat(View target, String propertyName, float... values) {
        ObjectAnimator anim = new ObjectAnimator();
        anim.setTarget(target);
        anim.setPropertyName(propertyName);
        anim.setFloatValues(values);
        return anim;
    }

    private void stopAnimation() {
        this.mStop = true;
    }

    public void showReviewImage(Bitmap bitmap) {
        this.mReviewImage.setImageBitmap(bitmap);
        this.mReviewImage.setBackgroundColor(this.mActivity.getResources().getColor(17170444));
        this.mReviewImage.setVisibility(0);
    }

    public void showReviewControls() {
        this.mActivity.getCameraAppUI().transitionToIntentReviewLayout();
        this.mReviewImage.setVisibility(0);
    }

    public void hideReviewControls() {
        this.mActivity.getCameraAppUI().transitionToCancel();
        this.mReviewImage.setVisibility(8);
    }

    public void resetZoombar() {
        if (this.mZoomBar != null) {
            this.mZoomBar.resetZoomRatio();
        }
        this.mPreviewOverlay.resetZoom();
    }

    public void initializeZoom(CameraSettings settings, CameraCapabilities capabilities) {
        resetZoombar();
        this.mZoomMax = capabilities.getMaxZoomRatio();
        boolean bSupportFrontPinchZoom = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_FRONTFACING_PINCH_ZOOM, false);
        if (!(Keys.isCameraBackFacing(this.mActivity.getSettingsManager(), SettingsManager.SCOPE_GLOBAL) ^ 1) || bSupportFrontPinchZoom) {
            this.mPreviewOverlay.setupZoom(this.mZoomMax, settings.getCurrentZoomRatio(), new ZoomChangeListener(this, null));
            if (this.mZoomBar != null) {
                this.mZoomBar.setZoomMax(this.mZoomMax);
                return;
            }
            return;
        }
        this.mPreviewOverlay.setupZoom(this.mZoomMax, settings.getCurrentZoomRatio(), null);
    }

    public void setRecordingTime(String text) {
        this.mRecordingTimeView.setText(text);
    }

    public void setRecordingTimeTextColor(int color) {
        this.mRecordingTimeView.setTextColor(color);
    }

    public boolean isVisible() {
        return false;
    }

    public OnGestureListener getGestureListener() {
        return this.mPreviewGestureListener;
    }

    public void clearEvoPendingUI() {
        this.mEvoSlider.resetSlider();
        if (this.mEvoSlider.getVisibility() == 0) {
            this.mEvoSlider.setVisibility(8);
        }
    }

    public OnTouchListener getTouchListener() {
        return this.mPreviewTouchListener;
    }

    public void showFocusUI(boolean show) {
        if (this.mFocusUI != null) {
            this.mFocusUI.setVisibility(show ? 0 : 4);
        }
    }

    public void showVideoRecordingHints(boolean show) {
    }

    public Point getPreviewScreenSize() {
        return new Point(this.mRootView.getMeasuredWidth(), this.mRootView.getMeasuredHeight());
    }

    public void onOrientationChanged(int orientation) {
        if (this.mZoomBar != null) {
            this.mZoomBar.setLayoutOrientation(orientation);
        }
        if (this.mIntentReviewControls != null) {
            this.mIntentReviewControls.setLayoutOrientation(orientation);
        }
    }

    public void onAllViewRemoved(AppController controller) {
        controller.removeLockableFromListenerPool(this.mVideoCaptureButton);
        controller.removeRotatableFromListenerPool(this.mRecordingTimeRect.hashCode());
        controller.removeRotatableFromListenerPool(this.mImageReviewLayout.hashCode());
        controller.removeRotatableFromListenerPool(this.mIntentReviewLayout.hashCode());
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.mController.onPreviewUIReady();
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        this.mController.onPreviewUIDestroyed();
        com.hmdglobal.app.camera.debug.Log.d(TAG, "surfaceTexture is destroyed");
        return true;
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void onPause() {
        this.mAspectRatio = 0.0f;
        if (!this.mStop) {
            hideBoomKeyTipUI();
        }
    }
}
