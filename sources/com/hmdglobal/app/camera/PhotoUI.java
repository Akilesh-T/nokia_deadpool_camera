package com.hmdglobal.app.camera;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.params.Face;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.ex.camera2.portability.CameraAgent.CameraFaceDetectionCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.FocusOverlayManager.FaceDetector;
import com.hmdglobal.app.camera.FocusOverlayManager.FocusUI;
import com.hmdglobal.app.camera.PhotoModule.AspectRatioDialogCallback;
import com.hmdglobal.app.camera.PhotoModule.LocationDialogCallback;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.DebugPropertyHelper;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.CountDownView;
import com.hmdglobal.app.camera.ui.CountDownView.OnCountDownStatusListener;
import com.hmdglobal.app.camera.ui.EvoSlider;
import com.hmdglobal.app.camera.ui.EvoSlider.EVOChangedListener;
import com.hmdglobal.app.camera.ui.FaceBeautyOption;
import com.hmdglobal.app.camera.ui.FaceBeautyOption.FaceBeautySettingCallBack;
import com.hmdglobal.app.camera.ui.GesturePalmOption;
import com.hmdglobal.app.camera.ui.GestureView;
import com.hmdglobal.app.camera.ui.IntentReviewControls;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper.OnAllViewRemovedListener;
import com.hmdglobal.app.camera.ui.PreviewOverlay;
import com.hmdglobal.app.camera.ui.PreviewOverlay.OnZoomChangedListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;
import com.hmdglobal.app.camera.ui.RotateImageView;
import com.hmdglobal.app.camera.ui.RotateLayout;
import com.hmdglobal.app.camera.ui.SoundGroup;
import com.hmdglobal.app.camera.ui.ZoomBar;
import com.hmdglobal.app.camera.ui.ZoomBar.ProgressChangeListener;
import com.hmdglobal.app.camera.ui.camera2.Camera2FaceView;
import com.hmdglobal.app.camera.ui.camera2.ExtendedFace;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.ArrayUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.GservicesHelper;
import com.hmdglobal.app.camera.widget.AspectRatioDialogLayout;
import com.hmdglobal.app.camera.widget.AspectRatioDialogLayout.AspectRatioChangedListener;
import com.hmdglobal.app.camera.widget.AspectRatioSelector.AspectRatio;
import com.hmdglobal.app.camera.widget.LocationDialogLayout;
import com.hmdglobal.app.camera.widget.LocationDialogLayout.LocationTaggingSelectionListener;
import java.util.ArrayList;

public class PhotoUI implements PreviewStatusListener, CameraFaceDetectionCallback, PreviewAreaChangedListener, OnAllViewRemovedListener, FaceDetector {
    private static final int DOWN_SAMPLE_FACTOR = 4;
    private static final boolean HIDE_BURST_COUNT = false;
    private static final Tag TAG = new Tag("PhotoUI");
    private static final float UNSET = 0.0f;
    protected final CameraActivity mActivity;
    private float mAspectRatio = 0.0f;
    private final RotateLayout mBurstCountLayout;
    private final TextView mBurstCountView;
    private Size mCaptureSize;
    private final PhotoController mController;
    private final CountDownView mCountdownView;
    private DecodeImageForReview mDecodeTaskForReview = null;
    private Dialog mDialog = null;
    private final EvoSlider mEvoSlider;
    private final RotateImageView mFaceBeautyMenu;
    private final FaceBeautyOption mFaceBeautyOption;
    private final Camera2FaceView mFaceView;
    private boolean mFacebeautyInitialized = false;
    private final FocusUI mFocusUI;
    private final RotateImageView mGesturePalm;
    private final GesturePalmOption mGesturePalmOption;
    private final GestureView mGestureView;
    private final RotateLayout mImageReviewLayout;
    private final IntentReviewControls mIntentReviewControls;
    private ImageView mIntentReviewImageView;
    private final RotateLayout mIntentReviewLayout;
    private Face[] mLatestCamera2Faces;
    private Camera.Face[] mLatestFaces;
    private Handler mMainHalder = new Handler(Looper.getMainLooper());
    private final OnDismissListener mOnDismissListener = new OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            PhotoUI.this.mDialog = null;
        }
    };
    private final OnGestureListener mPreviewGestureListener = new SimpleOnGestureListener() {
        public boolean onSingleTapUp(MotionEvent ev) {
            PhotoUI.this.mController.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }

        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            PhotoUI.this.mController.onLongPress(e.getX(), e.getY());
        }
    };
    private int mPreviewHeight = 0;
    private final PreviewOverlay mPreviewOverlay;
    private final OnTouchListener mPreviewTouchListener = new OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            if (PhotoUI.this.mEvoSlider.getVisibility() == 8 || event.getAction() != 2) {
                return false;
            }
            if (PhotoUI.this.mZoomBar.getVisibility() == 0) {
                PhotoUI.this.mZoomBar.setVisibility(8);
            }
            return PhotoUI.this.mEvoSlider.onTouchEvent(event);
        }
    };
    private int mPreviewWidth = 0;
    private final View mRootView;
    private Runnable mRunnableForNextFrame = null;
    private SoundGroup mSoundGroup;
    private final ZoomBar mZoomBar;
    private final RotateLayout mZoomLayout;
    private float mZoomMax;

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte[] mData;
        private final boolean mMirror;
        private final int mOrientation;

        public DecodeTask(byte[] data, int orientation, boolean mirror) {
            this.mData = data;
            this.mOrientation = orientation;
            this.mMirror = mirror;
        }

        /* Access modifiers changed, original: protected|varargs */
        public Bitmap doInBackground(Void... params) {
            Bitmap bitmap = CameraUtil.downSample(this.mData, 4);
            if (this.mOrientation == 0 && !this.mMirror) {
                return bitmap;
            }
            Matrix m = new Matrix();
            if (this.mMirror) {
                m.setScale(-1.0f, 1.0f);
            }
            m.preRotate((float) this.mOrientation);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
        }
    }

    private class DecodeImageForReview extends DecodeTask {
        public DecodeImageForReview(byte[] data, int orientation, boolean mirror) {
            super(data, orientation, mirror);
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Bitmap bitmap) {
            if (!isCancelled()) {
                PhotoUI.this.mIntentReviewImageView.setImageBitmap(bitmap);
                PhotoUI.this.mIntentReviewImageView.setBackgroundColor(PhotoUI.this.mActivity.getResources().getColor(17170444));
                PhotoUI.this.showIntentReviewImageView();
                PhotoUI.this.mDecodeTaskForReview = null;
            }
        }
    }

    private class ZoomChangeListener implements OnZoomChangedListener {
        private ZoomChangeListener() {
        }

        /* synthetic */ ZoomChangeListener(PhotoUI x0, AnonymousClass1 x1) {
            this();
        }

        public void onZoomValueChanged(float ratio) {
            if (PhotoUI.this.mZoomBar != null) {
                PhotoUI.this.mZoomBar.zoomRatioChanged(ratio);
            }
            PhotoUI.this.mController.onZoomChanged(ratio);
        }

        public void onZoomStart() {
        }

        public void onZoomEnd() {
        }
    }

    public void setZoomBarVisible(boolean visible) {
        if (this.mZoomBar != null) {
            this.mZoomBar.setZoomBarTouchVisible(visible);
        }
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

    public void onPreviewLayoutChanged(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        int width = right - left;
        int height = bottom - top;
        if (!(this.mPreviewWidth == width && this.mPreviewHeight == height)) {
            this.mPreviewWidth = width;
            this.mPreviewHeight = height;
        }
        this.mEvoSlider.setBound(width, height);
        if (this.mFaceView != null) {
            this.mFaceView.onPreviewLayoutChanged(width, height);
        }
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

    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return true;
    }

    public boolean shouldAutoAdjustBottomBar() {
        return true;
    }

    public void onPreviewFlipped() {
        this.mController.updateCameraOrientation();
    }

    public void setRunnableForNextFrame(Runnable runnable) {
        this.mRunnableForNextFrame = runnable;
    }

    public void startCountdown(int sec) {
        this.mCountdownView.startCountDown(sec);
    }

    public void setCountdownFinishedListener(OnCountDownStatusListener listener) {
        this.mCountdownView.setCountDownStatusListener(listener);
    }

    public boolean isCountingDown() {
        return this.mCountdownView.isCountingDown();
    }

    public void cancelCountDown() {
        this.mCountdownView.cancelCountDown();
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        if (this.mFaceView != null) {
            this.mFaceView.onPreviewAreaChanged(previewArea);
        }
        if (this.mGestureView != null) {
            this.mGestureView.onPreviewAreaChanged(previewArea);
        }
        this.mCountdownView.onPreviewAreaChanged(previewArea);
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
        controller.removeRotatableFromListenerPool(this.mImageReviewLayout.hashCode());
        controller.removeRotatableFromListenerPool(this.mIntentReviewLayout.hashCode());
        controller.removeRotatableFromListenerPool(this.mCountdownView.hashCode());
        controller.removeRotatableFromListenerPool(this.mBurstCountLayout.hashCode());
        controller.removeRotatableFromListenerPool(this.mFaceBeautyMenu.hashCode());
        controller.removeRotatableFromListenerPool(this.mGesturePalm.hashCode());
    }

    public PhotoUI(CameraActivity activity, PhotoController controller, View parent) {
        this.mActivity = activity;
        this.mController = controller;
        this.mRootView = parent;
        ModuleLayoutWrapper moduleRoot = (ModuleLayoutWrapper) this.mRootView.findViewById(R.id.module_layout);
        moduleRoot.setOnAllViewRemovedListener(this, this.mActivity);
        this.mActivity.getLayoutInflater().inflate(R.layout.photo_module, moduleRoot, true);
        initIndicators();
        this.mFocusUI = (FocusUI) this.mRootView.findViewById(R.id.focus_overlay);
        ((View) this.mFocusUI).setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (PhotoUI.this.mEvoSlider.getVisibility() == 0) {
                    PhotoUI.this.mEvoSlider.dispatchTouchEvent(event);
                }
                return true;
            }
        });
        this.mFocusUI.setFaceDetector(this);
        this.mEvoSlider = (EvoSlider) this.mRootView.findViewById(R.id.evo_slider);
        this.mEvoSlider.setEvoListener(new EVOChangedListener() {
            public void onEVOChanged(int value) {
                PhotoUI.this.mController.onEvoChanged(value);
            }
        });
        this.mBurstCountView = (TextView) this.mRootView.findViewById(R.id.burst_count_view);
        this.mBurstCountLayout = (RotateLayout) this.mRootView.findViewById(R.id.burst_count_layout);
        this.mPreviewOverlay = (PreviewOverlay) this.mRootView.findViewById(R.id.preview_overlay);
        this.mZoomLayout = (RotateLayout) this.mRootView.findViewById(R.id.zoom_rotate_layout);
        this.mZoomBar = (ZoomBar) this.mRootView.findViewById(R.id.zoom_bar);
        this.mZoomBar.setProgressChangeListener(new ProgressChangeListener() {
            public void onProgressChanged(float ratio) {
                PhotoUI.this.mPreviewOverlay.setRatio(ratio);
                PhotoUI.this.mController.onZoomChanged(ratio);
            }
        });
        this.mImageReviewLayout = (RotateLayout) this.mRootView.findViewById(R.id.intent_review_imageview_layout);
        this.mIntentReviewLayout = (RotateLayout) this.mRootView.findViewById(R.id.intent_review_rotate_layout);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mImageReviewLayout, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mIntentReviewLayout, false));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mBurstCountLayout, false));
        this.mIntentReviewControls = (IntentReviewControls) this.mRootView.findViewById(R.id.intent_review_controls);
        this.mCountdownView = (CountDownView) this.mRootView.findViewById(R.id.count_down_view);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mCountdownView, false));
        if (DebugPropertyHelper.showCaptureDebugUI() || DebugPropertyHelper.needShowFaceView()) {
            this.mFaceView = (Camera2FaceView) this.mRootView.findViewById(R.id.face_view);
        } else {
            this.mFaceView = null;
        }
        this.mGestureView = (GestureView) this.mRootView.findViewById(R.id.gesture_view);
        if (this.mController.isImageCaptureIntent()) {
            initIntentReviewImageView();
        }
        this.mSoundGroup = (SoundGroup) this.mRootView.findViewById(R.id.sound_group);
        this.mFaceBeautyOption = (FaceBeautyOption) this.mRootView.findViewById(R.id.face_beauty);
        this.mFaceBeautyMenu = (RotateImageView) this.mRootView.findViewById(R.id.face_beauty_menu);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mFaceBeautyMenu, true));
        this.mGesturePalmOption = (GesturePalmOption) this.mRootView.findViewById(R.id.gesture_palm_option);
        this.mGesturePalm = (RotateImageView) this.mRootView.findViewById(R.id.gesture_palm);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mGesturePalm, true));
    }

    private void initIntentReviewImageView() {
        this.mIntentReviewImageView = (ImageView) this.mRootView.findViewById(R.id.intent_review_imageview);
    }

    public void updateBurstCount(int count) {
        if (count == 0) {
            this.mBurstCountView.setVisibility(8);
            return;
        }
        if (this.mBurstCountView.getVisibility() != 0) {
            this.mBurstCountView.setVisibility(0);
            if (this.mSoundGroup != null && this.mSoundGroup.getVisibility() == 0) {
                this.mSoundGroup.hideKidSound();
            }
            if (this.mFaceBeautyOption != null && this.mFaceBeautyOption.getVisibility() == 0) {
                this.mFaceBeautyOption.hideSeekBar();
            }
        }
        TextView textView = this.mBurstCountView;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(count);
        textView.setText(stringBuilder.toString());
    }

    public void showIntentReviewImageView() {
        if (this.mIntentReviewImageView != null) {
            this.mIntentReviewImageView.setVisibility(0);
        }
    }

    public void hideIntentReviewImageView() {
        if (this.mIntentReviewImageView != null) {
            this.mIntentReviewImageView.setVisibility(4);
        }
    }

    public FocusUI getFocusUI() {
        return this.mFocusUI;
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0.0f) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid aspect ratio: ");
            stringBuilder.append(aspectRatio);
            Log.e(tag, stringBuilder.toString());
            return;
        }
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        if (this.mAspectRatio != aspectRatio) {
            this.mAspectRatio = aspectRatio;
            this.mController.updatePreviewAspectRatio(this.mAspectRatio);
        }
    }

    public void initEvoSlider(float x, float y) {
        this.mEvoSlider.setCoord(x, y);
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.mController.onPreviewUIReady();
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        this.mController.onPreviewUIDestroyed();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (this.mRunnableForNextFrame != null) {
            this.mRootView.post(this.mRunnableForNextFrame);
            this.mRunnableForNextFrame = null;
        }
    }

    public View getRootView() {
        return this.mRootView;
    }

    private void initIndicators() {
    }

    public void onCameraOpened(CameraCapabilities capabilities, CameraSettings settings) {
        initializeZoom(capabilities, settings);
        initializeFacebeauty();
        initializeAttentionseeker();
        initializeGesturePalm();
    }

    public void animateCapture(byte[] jpegData, int orientation, boolean mirror) {
        new DecodeTask(jpegData, orientation, mirror).execute(new Void[0]);
    }

    public void initializeFirstTime() {
    }

    public void initializeSecondTime(CameraCapabilities capabilities, CameraSettings settings) {
        initializeZoom(capabilities, settings);
        if (this.mController.isImageCaptureIntent()) {
            hidePostCaptureAlert();
        }
    }

    public void showLocationAndAspectRatioDialog(final LocationDialogCallback locationCallback, final AspectRatioDialogCallback aspectRatioDialogCallback) {
        setDialog(new Dialog(this.mActivity, 16973834));
        LocationDialogLayout locationDialogLayout = (LocationDialogLayout) this.mActivity.getLayoutInflater().inflate(R.layout.location_dialog_layout, null);
        locationDialogLayout.setLocationTaggingSelectionListener(new LocationTaggingSelectionListener() {
            public void onLocationTaggingSelected(boolean selected) {
                locationCallback.onLocationTaggingSelected(selected);
                if (PhotoUI.this.showAspectRatioDialogOnThisDevice()) {
                    PhotoUI.this.showAspectRatioDialog(aspectRatioDialogCallback, PhotoUI.this.mDialog);
                } else if (PhotoUI.this.mDialog != null) {
                    PhotoUI.this.mDialog.dismiss();
                }
            }
        });
        this.mDialog.setContentView(locationDialogLayout, new LayoutParams(-1, -1));
        this.mDialog.show();
    }

    private void setDialog(Dialog dialog) {
        if (this.mDialog != null) {
            this.mDialog.setOnDismissListener(null);
            this.mDialog.dismiss();
        }
        this.mDialog = dialog;
        if (this.mDialog != null) {
            this.mDialog.setOnDismissListener(this.mOnDismissListener);
        }
    }

    public boolean showAspectRatioDialog(AspectRatioDialogCallback callback) {
        if (!showAspectRatioDialogOnThisDevice()) {
            return false;
        }
        setDialog(new Dialog(this.mActivity, 16973834));
        showAspectRatioDialog(callback, this.mDialog);
        return true;
    }

    private boolean showAspectRatioDialog(final AspectRatioDialogCallback callback, Dialog aspectRatioDialog) {
        if (aspectRatioDialog == null) {
            Log.e(TAG, "Dialog for aspect ratio is null.");
            return false;
        }
        AspectRatioDialogLayout aspectRatioDialogLayout = (AspectRatioDialogLayout) this.mActivity.getLayoutInflater().inflate(R.layout.aspect_ratio_dialog_layout, null);
        aspectRatioDialogLayout.initialize(new AspectRatioChangedListener() {
            public void onAspectRatioChanged(AspectRatio aspectRatio) {
                callback.onAspectRatioSelected(aspectRatio, new Runnable() {
                    public void run() {
                        if (PhotoUI.this.mDialog != null) {
                            PhotoUI.this.mDialog.dismiss();
                        }
                    }
                });
            }
        }, callback.getCurrentAspectRatio());
        aspectRatioDialog.setContentView(aspectRatioDialogLayout, new LayoutParams(-1, -1));
        aspectRatioDialog.show();
        return true;
    }

    private boolean showAspectRatioDialogOnThisDevice() {
        return !GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity) && (ApiHelper.IS_NEXUS_4 || ApiHelper.IS_NEXUS_5 || ApiHelper.IS_NEXUS_6);
    }

    public void initializeZoom(CameraCapabilities capabilities, CameraSettings settings) {
        if (capabilities != null && settings != null && capabilities.supports(Feature.ZOOM)) {
            this.mZoomMax = capabilities.getMaxZoomRatio();
            resetZoombar();
            boolean bSupportFrontPinchZoom = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_FRONTFACING_PINCH_ZOOM, false);
            if (!(Keys.isCameraBackFacing(this.mActivity.getSettingsManager(), SettingsManager.SCOPE_GLOBAL) ^ 1) || bSupportFrontPinchZoom) {
                this.mPreviewOverlay.setupZoom(this.mZoomMax, settings.getCurrentZoomRatio(), new ZoomChangeListener(this, null));
                if (this.mZoomBar != null) {
                    this.mZoomBar.setZoomMax(this.mZoomMax);
                }
            } else {
                this.mPreviewOverlay.setupZoom(this.mZoomMax, settings.getCurrentZoomRatio(), null);
            }
        }
    }

    private void initializeFacebeauty() {
        if (!this.mController.isFacebeautyEnabled() || this.mActivity.getCameraAppUI().isInIntentReview()) {
            this.mFaceBeautyOption.setVisibility(8);
            this.mFaceBeautyOption.hideSeekBar();
            return;
        }
        this.mFacebeautyInitialized = true;
        this.mFaceBeautyOption.hideSeekBar();
        this.mFaceBeautyOption.setVisibility(8);
        String key = Keys.KEY_FACEBEAUTY_SKIN_SMOOTHING;
        this.mFaceBeautyOption.initData(key, this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, key, Integer.valueOf(CustomUtil.getInstance().getInt(CustomFields.DEF_CAMERA_SKIN_SMOOTHING, 50))).intValue(), 100);
        this.mFaceBeautyOption.setFaceBeautySettingCallBack(new FaceBeautySettingCallBack() {
            public void updateFaceBeautySetting(String key, int value) {
                PhotoUI.this.mController.updateFaceBeautySetting(key, value);
            }
        });
    }

    public void showFacebeauty() {
        if (this.mController.isFacebeautyEnabled() && !this.mActivity.getCameraAppUI().isInIntentReview()) {
            if (this.mFacebeautyInitialized) {
                this.mFaceBeautyOption.setVisibility(8);
            } else {
                initializeFacebeauty();
            }
        }
    }

    public void hideFacebeauty() {
        this.mFaceBeautyOption.setVisibility(8);
        this.mFaceBeautyOption.hideSeekBar();
    }

    private void initializeAttentionseeker() {
        if (!this.mController.isAttentionSeekerShow() || this.mController.isImageCaptureIntent()) {
            this.mSoundGroup.finishKidSound();
            this.mSoundGroup.setVisibility(8);
            this.mSoundGroup.removeRotatableToListenerPool();
            return;
        }
        this.mSoundGroup.hideKidSound();
        this.mSoundGroup.setVisibility(0);
        this.mSoundGroup.addRotatableToListenerPool();
    }

    public void animateFlash() {
        this.mController.startPreCaptureAnimation();
    }

    public boolean onBackPressed() {
        if (this.mController.isImageCaptureIntent()) {
            this.mController.onCaptureCancelled();
            return true;
        } else if (this.mController.canCloseCamera()) {
            return false;
        } else {
            return true;
        }
    }

    /* Access modifiers changed, original: protected */
    public void showCapturedImageForReview(byte[] jpegData, int orientation, boolean mirror) {
        this.mDecodeTaskForReview = new DecodeImageForReview(jpegData, orientation, mirror);
        this.mDecodeTaskForReview.execute(new Void[0]);
        this.mActivity.getCameraAppUI().transitionToIntentReviewLayout();
        pauseFaceDetection();
        hideFacebeauty();
        hideGesturePalm();
    }

    /* Access modifiers changed, original: protected */
    public void hidePostCaptureAlert() {
        if (this.mDecodeTaskForReview != null) {
            this.mDecodeTaskForReview.cancel(true);
        }
        resumeFaceDetection();
        showFacebeauty();
        showGesturePalm();
    }

    /* Access modifiers changed, original: protected */
    public void clearReviewImage() {
        if (this.mIntentReviewImageView != null && this.mIntentReviewImageView.getDrawable() != null) {
            this.mIntentReviewImageView.setImageBitmap(null);
        }
    }

    public void setDisplayOrientation(int orientation) {
        if (this.mFaceView != null) {
            this.mFaceView.setDisplayOrientation(orientation);
        }
    }

    public void setPostGestureOrientation(int orientation) {
        if (this.mGestureView != null) {
            this.mGestureView.setPostGestureRotation(orientation);
        }
    }

    public void setGestureDisplayOrientation(int orientation) {
        if (this.mGestureView != null) {
            this.mGestureView.setDisplayOrientation(orientation);
        }
    }

    public void setSensorOrientation(int orientation) {
        if (this.mGestureView != null) {
            this.mGestureView.setSensorOrientation(orientation);
        }
    }

    public void setGestureMirrored(boolean isMirrored) {
        if (this.mGestureView != null) {
            this.mGestureView.setPreviewMirrored(isMirrored);
        }
    }

    public void disableZoom() {
        if (this.mPreviewOverlay != null) {
            this.mPreviewOverlay.setTouchEnabled(false);
        }
    }

    public void enableZoom() {
        if (this.mPreviewOverlay != null) {
            this.mPreviewOverlay.setTouchEnabled(true);
        }
    }

    public void setSwipingEnabled(boolean enable) {
        this.mActivity.setSwipingEnabled(enable);
    }

    public void onPause() {
        if (this.mFaceView != null) {
            this.mFaceView.clear();
        }
        if (!(this.mActivity.getCameraAppUI().isInIntentReview() || !this.mController.isFacebeautyEnabled() || this.mFaceBeautyOption == null)) {
            this.mFaceBeautyOption.reset();
            this.mFaceBeautyOption.hideSeekBar();
        }
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
        this.mAspectRatio = 0.0f;
        if (this.mSoundGroup != null) {
            this.mSoundGroup.finishKidSound();
        }
        if (this.mGesturePalmOption != null) {
            this.mGesturePalmOption.hideGestureHelpTip();
        }
    }

    public void clearFaces() {
        android.util.Log.d("20190111", "clearFaces");
        if (this.mFaceView != null) {
            this.mFaceView.clear();
        }
    }

    public void clearFocus() {
        if (this.mFocusUI != null) {
            this.mFocusUI.clearFocus();
        }
    }

    public boolean hasFaces() {
        return this.mFaceView != null && this.mFaceView.faceExists();
    }

    public void pauseFaceDetection() {
        if (this.mFaceView != null) {
            this.mFaceView.pause();
        }
    }

    public void resumeFaceDetection() {
        android.util.Log.d("20190111", "resumeFaceDetection");
        if (this.mFaceView != null) {
            this.mFaceView.resume();
        }
    }

    public void onStartFaceDetection(int orientation, boolean mirror, Rect cameraBound, float zoom) {
        if (this.mFaceView != null) {
            this.mFaceView.clear();
            this.mFaceView.setDisplayOrientation(orientation);
            this.mFaceView.setMirror(mirror);
            this.mFaceView.setCameraBound(cameraBound);
            this.mFaceView.setZoom(zoom);
            this.mFaceView.resume();
        }
    }

    public void onFaceDetection(Camera.Face[] faces, CameraProxy camera) {
        this.mLatestFaces = faces;
        if (this.mFaceView != null) {
            this.mFaceView.setFaces(faces);
        }
    }

    public void setCaptureSize(Size captureSize) {
        this.mCaptureSize = captureSize;
    }

    public void onFaceDetection(final Rect cameraBound, @Nullable final Face[] faces, @Nullable final ExtendedFace[] extendedFaces, CameraProxy camera) {
        if (this.mFaceView != null && cameraBound != null && faces != null) {
            this.mLatestCamera2Faces = faces;
            this.mMainHalder.post(new Runnable() {
                public void run() {
                    PhotoUI.this.mFaceView.setBlockDraw(false);
                    PhotoUI.this.mFaceView.setOriginalCameraBound(cameraBound);
                    PhotoUI.this.mFaceView.setFaces(faces, extendedFaces);
                }
            });
        }
    }

    public void showGesture(Rect gestureBound, Size previewSize) {
        if (this.mGestureView != null) {
            this.mGestureView.showGesture(gestureBound, previewSize);
        }
    }

    public void hideGesture() {
        if (this.mGestureView != null) {
            this.mGestureView.hideGesture();
        }
    }

    public boolean isGestureViewShow() {
        return this.mGestureView != null && this.mGestureView.getVisibility() == 0;
    }

    public Face[] getLatestFaces() {
        return this.mLatestCamera2Faces;
    }

    public ArrayList<RectF> filterAndAdjustFaces(boolean mirror, int jpgRotation) {
        ArrayList<RectF> rectFs = new ArrayList();
        if (ArrayUtil.isEmpty(this.mLatestCamera2Faces)) {
            return rectFs;
        }
        Matrix matrix = new Matrix();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mirror:");
        stringBuilder.append(mirror);
        stringBuilder.append(", ");
        stringBuilder.append(jpgRotation);
        Log.d(tag, stringBuilder.toString());
        matrix.setScale(mirror ? -1.0f : 1.0f, 1.0f);
        matrix.postRotate((float) jpgRotation);
        for (Face mLatestCamera2Face : this.mLatestCamera2Faces) {
            if (mLatestCamera2Face.getScore() >= 50) {
                RectF rect = new RectF();
                rect.set(mLatestCamera2Face.getBounds());
                matrix.mapRect(rect);
                rectFs.add(rect);
            }
        }
        return rectFs;
    }

    public void resetZoombar() {
        if (this.mZoomBar != null) {
            this.mZoomBar.resetZoomRatio();
        }
        this.mPreviewOverlay.resetZoom();
    }

    public void showSoundGroup() {
        if (this.mController.isAttentionSeekerShow() && !this.mController.isImageCaptureIntent() && this.mSoundGroup != null) {
            this.mSoundGroup.setVisibility(0);
        }
    }

    public void hideSoundGroup() {
        if (this.mSoundGroup != null && this.mSoundGroup.getVisibility() == 0) {
            this.mSoundGroup.setVisibility(8);
            this.mSoundGroup.finishKidSound();
        }
    }

    public boolean isSoundGroupPlaying() {
        if (this.mSoundGroup != null && this.mSoundGroup.getVisibility() == 0 && this.mSoundGroup.isSoundPlaying()) {
            return true;
        }
        return false;
    }

    private void initializeGesturePalm() {
        if (!this.mController.isGesturePalmShow() || this.mActivity.getCameraAppUI().isInIntentReview()) {
            this.mGesturePalmOption.setVisibility(8);
        } else {
            this.mGesturePalmOption.setVisibility(0);
        }
    }

    public void showGesturePalm() {
        if (this.mController.isGesturePalmShow() && !this.mActivity.getCameraAppUI().isInIntentReview()) {
            this.mGesturePalmOption.setVisibility(0);
        }
    }

    public void hideGesturePalm() {
        this.mGesturePalmOption.setVisibility(8);
        if (this.mGesturePalmOption != null) {
            this.mGesturePalmOption.hideGestureHelpTip();
        }
    }
}
