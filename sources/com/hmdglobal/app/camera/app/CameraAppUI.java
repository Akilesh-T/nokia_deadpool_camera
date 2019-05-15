package com.hmdglobal.app.camera.app;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.MediaPlayer2;
import android.text.TextUtils;
import android.util.CameraPerformanceTracker;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.hmdglobal.app.camera.AnimationManager;
import com.hmdglobal.app.camera.ButtonManager;
import com.hmdglobal.app.camera.ButtonManager.BeautyStateListener;
import com.hmdglobal.app.camera.ButtonManager.ButtonCallback;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.HelpTipsManager;
import com.hmdglobal.app.camera.LiveBokehModule;
import com.hmdglobal.app.camera.ManualModule;
import com.hmdglobal.app.camera.NormalPhotoModule;
import com.hmdglobal.app.camera.NormalVideoModule;
import com.hmdglobal.app.camera.OnCancelSelectionMenuListener;
import com.hmdglobal.app.camera.PhotoModule;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ShutterButton;
import com.hmdglobal.app.camera.ShutterButton.OnShutterButtonListener;
import com.hmdglobal.app.camera.SquareModule;
import com.hmdglobal.app.camera.TextureViewHelper;
import com.hmdglobal.app.camera.Thumbnail;
import com.hmdglobal.app.camera.TimeLapsedModule;
import com.hmdglobal.app.camera.VideoModule;
import com.hmdglobal.app.camera.beauty.cameragl.CameraRender.onPreviewBytes;
import com.hmdglobal.app.camera.beauty.cameragl.CameraSurfaceView;
import com.hmdglobal.app.camera.beauty.util.SharedUtil;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.hardware.HardwareSpec;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsManager.OnSettingChangedListener;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager;
import com.hmdglobal.app.camera.ui.AbstractTutorialOverlay;
import com.hmdglobal.app.camera.ui.BottomBar;
import com.hmdglobal.app.camera.ui.BottomBar.BottomBarSizeListener;
import com.hmdglobal.app.camera.ui.BottomBar.OnArcSeekBarListener;
import com.hmdglobal.app.camera.ui.BottomBarModeOptionsWrapper;
import com.hmdglobal.app.camera.ui.CaptureAnimationOverlay;
import com.hmdglobal.app.camera.ui.GridLines;
import com.hmdglobal.app.camera.ui.IntentReviewControls;
import com.hmdglobal.app.camera.ui.Lockable;
import com.hmdglobal.app.camera.ui.MainActivityLayout;
import com.hmdglobal.app.camera.ui.ManualGroup;
import com.hmdglobal.app.camera.ui.ModeListView.ModeListOpenListener;
import com.hmdglobal.app.camera.ui.ModeListView.ModeSwitchListener;
import com.hmdglobal.app.camera.ui.ModeStrip;
import com.hmdglobal.app.camera.ui.ModeStrip.OnModeIdListener;
import com.hmdglobal.app.camera.ui.ModeTransitionView;
import com.hmdglobal.app.camera.ui.ModeTransitionView.OnTransAnimationListener;
import com.hmdglobal.app.camera.ui.PeekImageView;
import com.hmdglobal.app.camera.ui.PreviewOverlay;
import com.hmdglobal.app.camera.ui.PreviewStatusListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAspectRatioChangedListener;
import com.hmdglobal.app.camera.ui.ProArcSeekBarUI;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;
import com.hmdglobal.app.camera.ui.RotatableButton;
import com.hmdglobal.app.camera.ui.RotateImageView;
import com.hmdglobal.app.camera.ui.StereoModeStripView;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import com.hmdglobal.app.camera.ui.ZoomBar;
import com.hmdglobal.app.camera.ui.camera2.Camera2FaceView;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.BlurUtil;
import com.hmdglobal.app.camera.util.BoostUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.Gusterpolator;
import com.hmdglobal.app.camera.util.PhotoSphereHelper;
import com.hmdglobal.app.camera.util.ToastUtil;
import com.hmdglobal.app.camera.widget.Cling;
import com.hmdglobal.app.camera.widget.IndicatorIconController;
import com.hmdglobal.app.camera.widget.ModeOptionsOverlay;
import com.hmdglobal.app.camera.widget.PeekView.OnCaptureStateListener;
import com.megvii.beautify.jni.BeaurifyJniSdk;
import com.morphoinc.app.panoramagp3.ButtonsFragment;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class CameraAppUI implements OnModeIdListener, ModeSwitchListener, SurfaceTextureListener, ModeListOpenListener, OnSettingChangedListener, OnShutterButtonListener, OnArcSeekBarListener, OnCancelSelectionMenuListener {
    private static final int COVER_HIDDEN = 0;
    private static final int COVER_SHOWN = 1;
    private static final int COVER_WILL_HIDE_AT_NEXT_FRAME = 2;
    private static final int COVER_WILL_HIDE_AT_NEXT_TEXTURE_UPDATE = 3;
    private static final int DOWN_SAMPLE_RATE_FOR_SCREENSHOT = 2;
    public static final int HIDE_NAVIGATION_VIEW = 2054;
    private static final int IDLE = 0;
    public static final int PANORAMA_SLIP_UP = 2;
    public static final int SHOW_NAVIGATION_VIEW = 1281;
    public static final int SHOW_SEEK_BAR = 1;
    private static final int SWIPE_DOWN = 2;
    private static final int SWIPE_LEFT = 3;
    private static final int SWIPE_RIGHT = 4;
    private static final int SWIPE_TIME_OUT_MS = 500;
    private static final int SWIPE_UP = 1;
    private static final Tag TAG = new Tag("CameraAppUI");
    public static long sLastShowOrHideGLSurfaceTime = 0;
    private final Tag SHUTTER_TAG;
    public final LockEventListener gLockEventListener;
    private boolean isArcSeekBarShow;
    private boolean isCommShow;
    private boolean isNeedShowArc;
    private View keyguard_widget;
    private int lastModeID;
    private BroadcastReceiver localBroadcastReceiver;
    private final AnimationManager mAnimationManager;
    private final MainActivityLayout mAppRootView;
    private int mBeauState;
    private boolean mBeautyEnable = false;
    private float mBeautySeek = 0.0f;
    private BottomBar mBottomBar;
    private BottomBarUISpec mBottomBarUISpec;
    private final CameraModuleScreenShotProvider mCameraModuleScreenShotProvider;
    private final FrameLayout mCameraRootView;
    private ImageView mCameraSwitchAnimation;
    private View mCameraSwitchAnimationBackgroud;
    private RotatableButton mCancelButton;
    private RotatableButton mCaptureButton;
    private final CaptureLayoutHelper mCaptureLayoutHelper;
    private CaptureAnimationOverlay mCaptureOverlay;
    private final AppController mController;
    private long mCoverHiddenTime;
    private String mCurrSelect;
    private boolean mDisableAllUserInteractions;
    private DisplayListener mDisplayListener;
    private boolean mEffectEnable = false;
    private final boolean mEnableBlurDuringTransition;
    private final Camera2FaceView mFaceView;
    private int mFlashState;
    private View mFocusOverlay;
    private final GestureDetector mGestureDetector;
    private CameraSurfaceView mGlSurfaceView;
    private GridLines mGridLines;
    private int mHdrState;
    private RotateImageView mHdrView;
    private HelpTipsManager mHelpTipsManager;
    private Runnable mHideCoverRunnable;
    private BottomBarModeOptionsWrapper mIndicatorBottomBarWrapper;
    private IndicatorIconController mIndicatorIconController;
    private final IntentReviewControls mIntentReviewControls;
    public boolean mIsCameraSwitchAnimationRunning;
    private final boolean mIsCaptureIntent;
    private final boolean mIsCaptureSecureIntent;
    private boolean mIsFirstLiveBokehModule;
    private boolean mIsFirstRun;
    private boolean mIsForceGlSurfaceChanged;
    private boolean mIsHide;
    private int mLastRotation;
    private Map<Integer, Lockable> mListeningLockable;
    private LockState mLockState;
    private MainHandler mMainHandler;
    private final int mMaxTimesForEffectClickToast;
    private View mMicroVideoProgressbar;
    private int mModeCoverState = 0;
    private ModeOptionsOverlay mModeOptionsOverlay;
    private View mModeOptionsToggle;
    private final ModeStrip mModeStripView;
    private final ModeTransitionView mModeTransitionView;
    private FrameLayout mModuleUI;
    private int mMotionState;
    private boolean mNeedBlur;
    private OnModeOptionsVisibilityChangedListener mOnModeOptionsVisibilityChangedListener;
    private PeekImageView mPeekThumb;
    private final OnLayoutChangeListener mPreviewLayoutChangeListener = new OnLayoutChangeListener() {
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (CameraAppUI.this.mPreviewStatusListener != null) {
                CameraAppUI.this.mPreviewStatusListener.onPreviewLayoutChanged(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
            }
        }
    };
    private PreviewOverlay mPreviewOverlay;
    private PreviewStatusListener mPreviewStatusListener;
    private ProArcSeekBarUI mProArcSeekBarUI;
    private TextView mPromptView;
    private RotatableButton mRemixButton;
    private RotatableButton mSegmentRemoveButton;
    private ShutterButton mShutterButton;
    private final int mSlop;
    private SurfaceTexture mSurface;
    private int mSurfaceHeight;
    private int mSurfaceWidth;
    private boolean mSwipeEnabled = true;
    private int mSwipeState = 0;
    private TextureView mTextureView;
    private TextureViewHelper mTextureViewHelper;
    private Integer mToken;
    private final boolean mVdfModeSwitcherOn;
    private RotatableButton mVideoShuttterBotton;
    private FrameLayout mViewFinderLayout;
    private ZoomBar mZoomBar;
    private boolean mdisableUI;
    private ProHandler proHandler;

    public interface AnimationFinishedListener {
        void onAnimationFinished(boolean z);
    }

    public static class BottomBarUISpec {
        public ButtonCallback cameraCallback;
        public OnClickListener cancelCallback;
        public OnClickListener doneCallback;
        public boolean enableCamera;
        public boolean enableExposureCompensation;
        public boolean enableFlash;
        public boolean enableGridLines;
        public boolean enableHdr;
        public boolean enableHdrPlusFlash;
        public boolean enablePanoOrientation;
        public boolean enableSelfTimer = false;
        public boolean enableTorchFlash;
        public ExposureCompensationSetCallback exposureCompensationSetCallback;
        public float exposureCompensationStep;
        public ButtonCallback flashCallback;
        public ButtonCallback gridLinesCallback;
        public ButtonCallback hdrCallback;
        public boolean hideBolken;
        public boolean hideCamera;
        public boolean hideCameraForced;
        public boolean hideFlash;
        public boolean hideGridLines;
        public boolean hideHdr;
        public boolean hideLive;
        public boolean hideLowlight;
        public boolean hideSetting;
        public ButtonCallback lowlightCallback;
        public int maxExposureCompensation;
        public int minExposureCompensation;
        public String moduleName = "Not-specified";
        public String moreName;
        public ButtonCallback panoOrientationCallback;
        public OnClickListener retakeCallback;
        public OnClickListener reviewCallback;
        public OnSeekBarChangeListener seekbarBolkenListener;
        public OnSeekBarChangeListener seekbarChangeListener;
        public boolean setCameraInvisible;
        public boolean showBeauty2;
        public boolean showBeautyButton = false;
        public boolean showCancel;
        public boolean showDone;
        public boolean showEffect2;
        public boolean showEffectButton = false;
        public boolean showMotion;
        public boolean showRetake;
        public boolean showReview;
        public boolean showSelfTimer = false;
        public boolean showWrapperButton = false;

        public interface ExposureCompensationSetCallback {
            void setExposure(int i);
        }
    }

    public interface BottomPanel {
        public static final int VIEWER_NONE = 0;
        public static final int VIEWER_OTHER = 3;
        public static final int VIEWER_PHOTO_SPHERE = 1;
        public static final int VIEWER_REFOCUS = 2;

        public interface Listener {
            void onDelete();

            void onEdit();

            void onExternalViewer();

            void onProgressErrorClicked();

            void onShare();

            void onTinyPlanet();
        }

        void clearClingForViewer(int i);

        Cling getClingForViewer(int i);

        void hideControls();

        void hideProgress();

        void hideProgressError();

        void setClingForViewer(int i, Cling cling);

        void setDeleteButtonVisibility(boolean z);

        void setDeleteEnabled(boolean z);

        void setEditButtonVisibility(boolean z);

        void setEditEnabled(boolean z);

        void setListener(Listener listener);

        void setProgress(int i);

        void setProgressText(CharSequence charSequence);

        void setShareButtonVisibility(boolean z);

        void setShareEnabled(boolean z);

        void setTinyPlanetEnabled(boolean z);

        void setViewEnabled(boolean z);

        void setViewerButtonVisibility(int i);

        void setVisible(boolean z);

        void showControls();

        void showProgress();

        void showProgressError(CharSequence charSequence);
    }

    public interface CameraModuleScreenShotProvider {
        Bitmap getPreviewFrame(int i);

        Bitmap getPreviewOverlayAndControls();

        Bitmap getRawPreviewFrame(int i);

        Bitmap getScreenShot(int i);
    }

    public interface EffectOnClick {
        void onItemClick(int i, String str);
    }

    public interface LockEventListener {
        void forceBlocking();

        void onIdle();

        void onMenuClicked(int i);

        void onModeSwitching();

        void onShutter();

        void onSwitching();
    }

    public enum LockState {
        BLOCKING,
        IDLE,
        BLOCK_FROM_SHUTTER,
        BLOCK_FROM_MENU,
        BLOCK_FROM_MODE_SWITCHING
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String currSelect = msg.obj;
            ButtonManager buttonManager = CameraAppUI.this.mController.getButtonManager();
            if (CameraAppUI.this.mController.getCurrentModuleIndex() != 4 && CameraAppUI.this.mController.getCurrentModuleIndex() != 5 && CameraAppUI.this.mController.getCurrentModuleIndex() != 15) {
                return;
            }
            if (currSelect != "") {
                buttonManager.showEffect2Button();
            } else {
                buttonManager.hideEffect2Button();
            }
        }
    }

    private class MyGestureListener extends SimpleOnGestureListener {
        private long lastTimeStamp;
        private MotionEvent mDown;

        private MyGestureListener() {
            this.lastTimeStamp = -1;
        }

        /* synthetic */ MyGestureListener(CameraAppUI x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onScroll(MotionEvent e1, MotionEvent ev, float distanceX, float distanceY) {
            if (ev.getEventTime() - ev.getDownTime() > 500 || CameraAppUI.this.mSwipeState != 0 || CameraAppUI.this.mIsCaptureIntent || !CameraAppUI.this.mSwipeEnabled) {
                return false;
            }
            int deltaX = (int) (ev.getX() - this.mDown.getX());
            int deltaY = (int) (ev.getY() - this.mDown.getY());
            if (ev.getActionMasked() == 2 && (Math.abs(deltaX) > CameraAppUI.this.mSlop || Math.abs(deltaY) > CameraAppUI.this.mSlop)) {
                if (deltaX >= Math.abs(deltaY)) {
                    setSwipeState(4);
                } else if (deltaX <= (-Math.abs(deltaY))) {
                    setSwipeState(3);
                }
            }
            return true;
        }

        private void setSwipeState(int swipeState) {
            CameraAppUI.this.mSwipeState = swipeState;
            CameraAppUI.this.onSwipeDetected(swipeState);
        }

        public boolean onSingleTapUp(MotionEvent e) {
            long time = System.currentTimeMillis();
            if (time - this.lastTimeStamp < 600) {
                this.lastTimeStamp = time;
                return true;
            }
            this.lastTimeStamp = time;
            if (CameraAppUI.this.mController.getButtonManager().tryToCancelMaskWrapper()) {
                return true;
            }
            return super.onSingleTapUp(e);
        }

        public boolean onDown(MotionEvent ev) {
            this.mDown = MotionEvent.obtain(ev);
            CameraAppUI.this.mSwipeState = 0;
            return false;
        }
    }

    private class MyTouchListener implements OnTouchListener {
        private boolean mScaleStarted;

        private MyTouchListener() {
            this.mScaleStarted = false;
        }

        /* synthetic */ MyTouchListener(CameraAppUI x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onTouch(View v, MotionEvent event) {
            CameraAppUI.this.setNeedShowArc(false);
            CameraAppUI.this.removeShowSeekBarMsg();
            if (CameraAppUI.this.isArcSeekBarShow()) {
                CameraAppUI.this.hideArcSeekBar();
            }
            if (CameraAppUI.this.isCommShow) {
                CameraAppUI.this.updateComBarState(-1, false);
                CameraAppUI.this.isCommShow = false;
            }
            CameraAppUI.this.mController.getButtonManager().tryCancelSelectionMenu();
            if (event.getActionMasked() == 0) {
                this.mScaleStarted = false;
            } else if (event.getActionMasked() == 5) {
                this.mScaleStarted = true;
            }
            if (this.mScaleStarted || !CameraAppUI.this.mGestureDetector.onTouchEvent(event)) {
                return false;
            }
            return true;
        }
    }

    public interface NonDecorWindowSizeChangedListener {
        void onNonDecorWindowSizeChanged(int i, int i2, int i3);
    }

    public interface OnModeOptionsVisibilityChangedListener {
        void onModeOptionsVisibilityChanged(int i);
    }

    private static class ProHandler extends Handler {
        private final WeakReference<AppController> mController;

        public ProHandler(AppController controller) {
            this.mController = new WeakReference(controller);
        }

        public void handleMessage(Message msg) {
            AppController controller = (AppController) this.mController.get();
            if (controller != null) {
                switch (msg.what) {
                    case 1:
                        controller.getCameraAppUI().showArcSeekBar();
                        return;
                    case 2:
                        Log.d(CameraAppUI.TAG, "[localBroadcastReceiver] PANORAMA_SLIP_UP");
                        controller.getCameraAppUI().slipUpShutterButton();
                        return;
                    default:
                        return;
                }
            }
        }
    }

    public class TakePictureRunnable implements Runnable {
        public Bitmap mBitmap;

        public void run() {
        }
    }

    public long getCoverHiddenTime() {
        return this.mCoverHiddenTime;
    }

    public void clearPreviewTransform() {
        this.mTextureViewHelper.clearTransform();
    }

    public SurfaceTexture getCameraSurfaceView() {
        return this.mSurface;
    }

    public CameraSurfaceView getCameraGLSurfaceView() {
        return this.mGlSurfaceView;
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mTextureViewHelper.updateAspectRatio(aspectRatio);
    }

    public void setDefaultBufferSizeToViewDimens() {
        if (this.mSurface == null || this.mTextureView == null) {
            Log.w(TAG, "Could not set SurfaceTexture default buffer dimensions, not yet setup");
        } else {
            this.mSurface.setDefaultBufferSize(this.mTextureView.getWidth(), this.mTextureView.getHeight());
        }
    }

    public void updatePreviewTransformFullscreen(Matrix matrix, float aspectRatio) {
        this.mTextureViewHelper.updateTransformFullScreen(matrix, aspectRatio);
    }

    public RectF getFullscreenRect() {
        return this.mTextureViewHelper.getFullscreenRect();
    }

    public void updatePreviewTransform(Matrix matrix) {
        this.mTextureViewHelper.updateTransform(matrix);
    }

    public void pauseFaceDetection() {
        if (this.mFaceView != null) {
            this.mFaceView.pause();
        }
    }

    public void resumeFaceDetection() {
        if (this.mFaceView != null) {
            this.mFaceView.resume();
        }
    }

    public CameraAppUI(AppController controller, MainActivityLayout appRootView, boolean isCaptureIntent, boolean isCaptureSecureIntent) {
        boolean z = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_BLUR, true);
        this.mEnableBlurDuringTransition = true;
        this.mMaxTimesForEffectClickToast = 5;
        this.mCameraModuleScreenShotProvider = new CameraModuleScreenShotProvider() {
            public Bitmap getPreviewFrame(int downSampleFactor) {
                if (CameraAppUI.this.mCameraRootView == null || CameraAppUI.this.mTextureView == null) {
                    return null;
                }
                Bitmap preview = null;
                if (CameraAppUI.this.mGlSurfaceView.getVisibility() == 0) {
                    try {
                        TakePictureRunnable run = new TakePictureRunnable();
                        CameraAppUI.this.takePicture(run);
                        while (run.mBitmap == null) {
                            Thread.sleep(10);
                        }
                        preview = run.mBitmap;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    preview = CameraAppUI.this.mTextureViewHelper.getPreviewBitmap(downSampleFactor);
                }
                if (preview == null) {
                    return null;
                }
                int originHeight;
                if (CameraAppUI.this.mEnableBlurDuringTransition) {
                    Log.v(CameraAppUI.TAG, "blur start");
                    originHeight = preview.getHeight();
                    preview = BlurUtil.scaleBitmap(BlurUtil.blur(BlurUtil.scaleBitmap(preview, 0.25f, 0.25f)), 4.0f, 4.2f);
                    preview = BlurUtil.cropBitmap(preview, preview.getHeight() - originHeight);
                }
                Log.v(CameraAppUI.TAG, "blur record");
                originHeight = preview.getHeight();
                if (1000 == originHeight) {
                    BlurUtil.fixBlackRect(preview, MotionPictureHelper.FRAME_HEIGHT_9);
                } else if (1130 == originHeight) {
                    BlurUtil.fixBlackRect(preview, 960);
                } else {
                    BlurUtil.fixBlackRect(preview, 1280);
                }
                return preview;
            }

            public Bitmap getRawPreviewFrame(int downSampleFactor) {
                if (CameraAppUI.this.mCameraRootView == null || CameraAppUI.this.mTextureView == null) {
                    return null;
                }
                Bitmap preview = null;
                if (CameraAppUI.this.mGlSurfaceView.getVisibility() == 0) {
                    try {
                        TakePictureRunnable run = new TakePictureRunnable();
                        CameraAppUI.this.takePicture(run);
                        while (run.mBitmap == null) {
                            Thread.sleep(10);
                        }
                        preview = run.mBitmap;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    preview = CameraAppUI.this.mTextureViewHelper.getPreviewBitmap(downSampleFactor);
                }
                return preview;
            }

            public Bitmap getPreviewOverlayAndControls() {
                Bitmap overlays = Bitmap.createBitmap(CameraAppUI.this.mCameraRootView.getWidth(), CameraAppUI.this.mCameraRootView.getHeight(), Config.ARGB_8888);
                CameraAppUI.this.mCameraRootView.draw(new Canvas(overlays));
                return overlays;
            }

            public Bitmap getScreenShot(int previewDownSampleFactor) {
                Bitmap screenshot = Bitmap.createBitmap(CameraAppUI.this.mCameraRootView.getWidth(), CameraAppUI.this.mCameraRootView.getHeight(), Config.ARGB_8888);
                Canvas canvas = new Canvas(screenshot);
                canvas.drawARGB(255, 0, 0, 0);
                Bitmap preview = CameraAppUI.this.mTextureViewHelper.getPreviewBitmap(previewDownSampleFactor);
                if (preview != null) {
                    canvas.drawBitmap(preview, null, CameraAppUI.this.mTextureViewHelper.getPreviewArea(), null);
                }
                Bitmap overlay = getPreviewOverlayAndControls();
                if (overlay != null) {
                    canvas.drawBitmap(overlay, 0.0f, 0.0f, null);
                }
                return screenshot;
            }
        };
        this.mCoverHiddenTime = -1;
        this.SHUTTER_TAG = new Tag("ShutterListener");
        this.mIsHide = true;
        this.gLockEventListener = new LockEventListener() {
            public void onIdle() {
                CameraAppUI.this.setLockState(LockState.IDLE);
            }

            public void onShutter() {
                CameraAppUI.this.setLockState(LockState.BLOCK_FROM_SHUTTER);
            }

            public void onSwitching() {
                CameraAppUI.this.setLockState(LockState.BLOCKING);
            }

            public void onMenuClicked(int hash) {
                Log.w(CameraAppUI.TAG, "on Menu clicked");
                CameraAppUI.this.lockPool(hash);
                CameraAppUI.this.setLockState(LockState.BLOCK_FROM_MENU);
            }

            public void forceBlocking() {
                CameraAppUI.this.setLockState(LockState.BLOCKING);
            }

            public void onModeSwitching() {
                CameraAppUI.this.setLockState(LockState.BLOCK_FROM_MODE_SWITCHING);
            }
        };
        this.mListeningLockable = new HashMap();
        this.localBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Tag access$600 = CameraAppUI.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[localBroadcastReceiver] action = ");
                stringBuilder.append(intent.getAction());
                stringBuilder.append(" Extra = ");
                stringBuilder.append(intent.getStringExtra(ButtonsFragment.ACTION_EXTRA));
                Log.d(access$600, stringBuilder.toString());
                if (ButtonsFragment.SLIP_UP_ACTION.equals(intent.getAction()) && ButtonsFragment.ACTION_END.equals(intent.getStringExtra(ButtonsFragment.ACTION_EXTRA))) {
                    CameraAppUI.this.proHandler.removeMessages(2);
                    Message message = Message.obtain();
                    message.what = 2;
                    CameraAppUI.this.proHandler.sendMessageDelayed(message, 200);
                }
            }
        };
        this.mIsFirstRun = true;
        this.mIsFirstLiveBokehModule = true;
        this.mIsForceGlSurfaceChanged = false;
        this.isNeedShowArc = false;
        this.isArcSeekBarShow = false;
        this.isCommShow = false;
        this.lastModeID = -1;
        this.mIsCameraSwitchAnimationRunning = false;
        this.mSlop = ViewConfiguration.get(controller.getAndroidContext()).getScaledTouchSlop();
        this.mController = controller;
        this.mIsCaptureIntent = isCaptureIntent;
        this.mIsCaptureSecureIntent = isCaptureSecureIntent;
        this.mAppRootView = appRootView;
        this.mCameraRootView = (FrameLayout) appRootView.findViewById(R.id.camera_app_root);
        this.mModeTransitionView = (ModeTransitionView) this.mAppRootView.findViewById(R.id.mode_transition_view);
        this.mModeStripView = (StereoModeStripView) this.mAppRootView.findViewById(R.id.mode_strip_view);
        if (this.mModeStripView != null) {
            this.mModeStripView.setModeIndexChangeListener(this);
            ((StereoModeStripView) this.mModeStripView).setOnCancelSelectionMenu(this);
        }
        this.mVdfModeSwitcherOn = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_FIX_MODE_SWITCHING, false);
        boolean z2 = this.mVdfModeSwitcherOn;
        this.mGestureDetector = new GestureDetector(controller.getAndroidContext(), new MyGestureListener(this, null));
        Resources res = controller.getAndroidContext().getResources();
        this.mCaptureLayoutHelper = new CaptureLayoutHelper(this.mController.getServices(), controller, res.getDimensionPixelSize(R.dimen.bottom_bar_height_min), res.getDimensionPixelSize(R.dimen.bottom_bar_height_max), res.getDimensionPixelSize(R.dimen.bottom_bar_height_optimal));
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) controller).getWindowManager().getDefaultDisplay().getMetrics(dm);
        this.mCaptureLayoutHelper.setDisplayHeight(dm.heightPixels);
        this.mCaptureLayoutHelper.setNavigationBarHeight(getNavigationHeight());
        this.keyguard_widget = appRootView.findViewById(R.id.keyguard_widget);
        this.mAnimationManager = new AnimationManager();
        this.mAppRootView.setNonDecorWindowSizeChangedListener(this.mCaptureLayoutHelper);
        initDisplayListener();
        this.mIntentReviewControls = (IntentReviewControls) appRootView.findViewById(R.id.intent_review_controls);
        this.mViewFinderLayout = (FrameLayout) this.mAppRootView.findViewById(R.id.view_finder_layout);
        this.mMainHandler = new MainHandler(Looper.myLooper());
        this.proHandler = new ProHandler(this.mController);
        this.mFaceView = (Camera2FaceView) this.mAppRootView.findViewById(R.id.face_view);
        this.mCameraSwitchAnimation = (ImageView) this.mAppRootView.findViewById(R.id.camera_switch_animation);
        this.mCameraSwitchAnimationBackgroud = this.mAppRootView.findViewById(R.id.camera_switch_animation_background);
        ColorDrawable cd = new ColorDrawable();
        cd.setColor(this.mCameraSwitchAnimationBackgroud.getResources().getColor(17170444));
        cd.setAlpha(255);
        this.mCameraSwitchAnimationBackgroud.setBackground(cd);
        ProcessingMediaManager.getInstance(this.mController.getAndroidContext()).setAppController(this.mController);
    }

    public boolean isScreenReversed() {
        return this.mCaptureLayoutHelper == null ? false : this.mCaptureLayoutHelper.isScreenReversed();
    }

    public void setViewFinderLayoutVisibile(boolean isVisible) {
        if (this.mViewFinderLayout != null) {
            if (isVisible) {
                this.mViewFinderLayout.setVisibility(0);
            } else {
                this.mViewFinderLayout.setVisibility(8);
            }
        }
    }

    public void setVisibleKeyguard() {
        android.util.Log.d("liugz", "cameraappUI ，set VisibleKeyguard");
        if (this.keyguard_widget.getVisibility() != 0) {
            this.keyguard_widget.setVisibility(0);
        }
    }

    public void setInVisibleKeyguard() {
        android.util.Log.d("liugz", "cameraappUI ，set InVisibleKeyguard");
        this.mMainHandler.postDelayed(new Runnable() {
            public void run() {
                if (CameraAppUI.this.keyguard_widget.getVisibility() == 0) {
                    CameraAppUI.this.keyguard_widget.setVisibility(4);
                }
            }
        }, 800);
    }

    public void setModeSwitchUIVisibility(boolean isVisible) {
        setModeStripViewVisibility(isVisible);
    }

    public void setModeStripViewVisibility(boolean isVisible) {
        if (this.mModeStripView != null) {
            if (isVisible) {
                ((StereoModeStripView) this.mModeStripView).setVisibility(0);
            } else {
                ((StereoModeStripView) this.mModeStripView).setVisibility(4);
            }
        }
    }

    public void setModeCoverState(int state) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set mode cover state: ");
        stringBuilder.append(state);
        Log.w(tag, stringBuilder.toString());
        this.mModeCoverState = state;
        if (this.mModeStripView != null && this.mModeCoverState == 1) {
            this.gLockEventListener.onModeSwitching();
        }
    }

    public void freezeScreenUntilPreviewReady() {
        freezeScreenUntilPreviewReady(false, null);
    }

    public void freezeScreenUntilPreviewReady(boolean needBlur, OnTransAnimationListener... listeners) {
        freezeScreenUntilPreviewReady(needBlur, -1, listeners);
    }

    public boolean isNeedBlur() {
        return this.mNeedBlur;
    }

    public void freezeScreenUntilPreviewReady(boolean needBlur, int duration, final OnTransAnimationListener... listeners) {
        this.mNeedBlur = needBlur;
        if (this.mModeCoverState != 1) {
            StringBuilder stringBuilder;
            if (!needBlur) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("1-needBlur = ");
                stringBuilder.append(needBlur);
                android.util.Log.d("20190111", stringBuilder.toString());
                this.mModeTransitionView.setupModeCoverTileAnimationDone(this.mCameraModuleScreenShotProvider.getRawPreviewFrame(2), 0, listeners);
            } else if (listeners == null || listeners.length == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("2-needBlur = ");
                stringBuilder.append(needBlur);
                android.util.Log.d("20190111", stringBuilder.toString());
                if (duration > 0) {
                    this.mModeTransitionView.setupModeCoverTileAnimationDone(this.mCameraModuleScreenShotProvider.getPreviewFrame(2), duration, new OnTransAnimationListener[0]);
                } else {
                    this.mModeTransitionView.setupModeCoverTileAnimationDone(this.mCameraModuleScreenShotProvider.getPreviewFrame(2), new OnTransAnimationListener[0]);
                }
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("3-needBlur = ");
                stringBuilder2.append(needBlur);
                android.util.Log.d("20190111", stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("listeners = ");
                stringBuilder2.append(listeners);
                android.util.Log.d("20190111", stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("listeners.length = ");
                stringBuilder2.append(listeners.length);
                android.util.Log.d("20190111", stringBuilder2.toString());
                this.mModeTransitionView.setupModeCoverTileAnimationDone(this.mCameraModuleScreenShotProvider.getPreviewFrame(2), listeners);
            }
            Log.w(TAG, "init hideCoverRunnable");
            this.mHideCoverRunnable = new Runnable() {
                public void run() {
                    CameraAppUI.this.mModeTransitionView.hideImageCover();
                }
            };
            setModeCoverState(1);
        } else if (listeners != null && listeners.length != 0) {
            this.mModeTransitionView.post(new Runnable() {
                public void run() {
                    for (OnTransAnimationListener listener : listeners) {
                        if (listener != null) {
                            listener.onAnimationDone();
                        }
                    }
                }
            });
        }
    }

    public Bitmap getCoveredBitmap() {
        if (this.mModeTransitionView == null) {
            return null;
        }
        return this.mModeTransitionView.getBackgroundBitmap();
    }

    public RectF getCoveredArea() {
        if (this.mModeTransitionView == null) {
            return new RectF();
        }
        return this.mModeTransitionView.getCoveredRect();
    }

    public RectF getPreviewArea() {
        if (this.mTextureViewHelper == null) {
            return new RectF();
        }
        return this.mTextureViewHelper.getPreviewArea();
    }

    public void clearClingForViewer(int viewerType) {
    }

    public void setSwipeEnabled(boolean enabled) {
        this.mSwipeEnabled = enabled;
        this.mAppRootView.setSwipeEnabled(enabled);
    }

    public void onDestroy() {
        ((DisplayManager) this.mController.getAndroidContext().getSystemService("display")).unregisterDisplayListener(this.mDisplayListener);
        if (this.mProArcSeekBarUI != null) {
            this.mProArcSeekBarUI.onDestroy();
        }
        LocalBroadcastManager.getInstance(this.mController.getAndroidContext()).unregisterReceiver(this.localBroadcastReceiver);
    }

    private void initDisplayListener() {
        if (ApiHelper.HAS_DISPLAY_LISTENER) {
            this.mLastRotation = CameraUtil.getDisplayRotation(this.mController.getAndroidContext());
            this.mDisplayListener = new DisplayListener() {
                public void onDisplayAdded(int arg0) {
                }

                public void onDisplayChanged(int displayId) {
                    int rotation = CameraUtil.getDisplayRotation(CameraAppUI.this.mController.getAndroidContext());
                    if (((rotation - CameraAppUI.this.mLastRotation) + 360) % 360 == MediaProviderUtils.ROTATION_180 && CameraAppUI.this.mPreviewStatusListener != null) {
                        CameraAppUI.this.mPreviewStatusListener.onPreviewFlipped();
                        CameraAppUI.this.mIndicatorBottomBarWrapper.requestLayout();
                        CameraAppUI.this.mTextureView.requestLayout();
                        CameraAppUI.this.mController.getButtonManager().requestMOBExtraLayout();
                    }
                    CameraAppUI.this.mLastRotation = rotation;
                }

                public void onDisplayRemoved(int arg0) {
                }
            };
            ((DisplayManager) this.mController.getAndroidContext().getSystemService("display")).registerDisplayListener(this.mDisplayListener, null);
        }
    }

    private void onSwipeDetected(int swipeState) {
        if (swipeState == 1 || swipeState == 2) {
            int currentModuleIndex = this.mController.getCurrentModuleIndex();
            final int moduleToTransitionTo = this.mController.getQuickSwitchToModuleId(currentModuleIndex);
            if (currentModuleIndex != moduleToTransitionTo) {
                int iconRes = CameraUtil.getCameraModeCoverIconResId(moduleToTransitionTo, this.mController.getAndroidContext());
                AnonymousClass7 anonymousClass7 = new AnimationFinishedListener() {
                    public void onAnimationFinished(boolean success) {
                        if (success) {
                            CameraAppUI.this.mHideCoverRunnable = new Runnable() {
                                public void run() {
                                    CameraAppUI.this.mModeTransitionView.startPeepHoleAnimation();
                                }
                            };
                            CameraAppUI.this.setModeCoverState(1);
                            CameraAppUI.this.mController.onModeSelected(moduleToTransitionTo);
                        }
                    }
                };
                return;
            }
            return;
        }
        if (swipeState == 3) {
        }
    }

    public void onStart() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ButtonsFragment.SLIP_UP_ACTION);
        LocalBroadcastManager.getInstance(this.mController.getAndroidContext()).registerReceiver(this.localBroadcastReceiver, intentFilter);
    }

    public void onStop() {
        LocalBroadcastManager.getInstance(this.mController.getAndroidContext()).unregisterReceiver(this.localBroadcastReceiver);
        if (this.mController.getCurrentModuleIndex() != this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro)) {
            return;
        }
        if (isArcSeekBarShow() || this.isCommShow) {
            hideArcSeekBar();
            updateComBarState(-1, false);
        }
    }

    private boolean isSpokenFeedbackAccessibilityEnabled() {
        return false;
    }

    public void openModeList() {
        this.mModeOptionsOverlay.closeModeOptions();
    }

    private void showModeCoverUntilPreviewReady() {
        this.mModeTransitionView.setupModeCover(R.color.mode_cover_default_color, CameraUtil.getCameraModeCoverIconResId(this.mController.getCurrentModuleIndex(), this.mController.getAndroidContext()));
        this.mHideCoverRunnable = new Runnable() {
            public void run() {
                CameraAppUI.this.mModeTransitionView.hideModeCover(null);
                if (!CameraAppUI.this.mDisableAllUserInteractions) {
                    CameraAppUI.this.showShimmyDelayed();
                }
            }
        };
        setModeCoverState(1);
    }

    private void showShimmyDelayed() {
        boolean z = this.mIsCaptureIntent;
    }

    private void hideModeCover() {
        if (this.mHideCoverRunnable != null) {
            Log.w(TAG, "hideModeCover in CameraAppUI");
            this.mAppRootView.post(this.mHideCoverRunnable);
            this.mHideCoverRunnable = null;
        }
        setModeCoverState(0);
        if (this.mCoverHiddenTime < 0) {
            this.mCoverHiddenTime = System.currentTimeMillis();
        }
    }

    public void onPreviewVisiblityChanged(int visibility) {
        if (visibility == 2) {
            setIndicatorBottomBarWrapperVisible(false);
        } else {
            setIndicatorBottomBarWrapperVisible(true);
        }
    }

    public void pausePreviewRendering() {
        if (this.mGlSurfaceView.getVisibility() == 8) {
            this.mTextureView.setVisibility(4);
        } else {
            this.mGlSurfaceView.setVisibility(4);
        }
    }

    public void resumePreviewRendering() {
        if (this.mGlSurfaceView.getVisibility() == 8) {
            this.mTextureView.setVisibility(0);
            return;
        }
        this.mGlSurfaceView.setVisibility(0);
        this.mGlSurfaceView.onResume();
    }

    public Matrix getPreviewTransform(Matrix m) {
        return this.mTextureView.getTransform(m);
    }

    public void onOpenFullScreen() {
    }

    public void onModeListOpenProgress(float progress) {
        progress = 1.0f - progress;
        this.mModeOptionsToggle.setAlpha(Gusterpolator.INSTANCE.getInterpolation(progress));
        this.mShutterButton.setAlpha((progress * 1.0f) + ((1.0f - progress) * 0.2f));
    }

    public void onModeListClosed() {
        this.mModeOptionsToggle.setAlpha(1.0f);
        this.mShutterButton.setAlpha(1.0f);
    }

    public boolean onBackPressed() {
        return false;
    }

    public void setPreviewStatusListener(PreviewStatusListener previewStatusListener) {
        this.mPreviewStatusListener = previewStatusListener;
        if (this.mPreviewStatusListener != null) {
            onPreviewListenerChanged();
        }
    }

    private void onPreviewListenerChanged() {
        OnGestureListener gestureListener = this.mPreviewStatusListener.getGestureListener();
        if (gestureListener != null) {
            this.mPreviewOverlay.setGestureListener(gestureListener);
        }
        OnTouchListener touchListener = this.mPreviewStatusListener.getTouchListener();
        if (touchListener != null) {
            this.mPreviewOverlay.setTouchListener(touchListener);
        }
        this.mTextureViewHelper.setAutoAdjustTransform(this.mPreviewStatusListener.shouldAutoAdjustTransformMatrixOnLayout());
    }

    public void onChangeCamera() {
        if (getCurrentModeIndex() == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro) || getCurrentModeIndex() == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo) || getCurrentModeIndex() == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video) || getCurrentModeIndex() == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_livebokeh)) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onChangeCamera switchShutterSlidingAbility true mIsCaptureIntent = ");
            stringBuilder.append(this.mIsCaptureIntent);
            stringBuilder.append(" mIsCaptureSecureIntent = ");
            stringBuilder.append(this.mIsCaptureSecureIntent);
            Log.d(tag, stringBuilder.toString());
            if (CustomUtil.getInstance().isPanther()) {
                boolean z = !this.mIsCaptureIntent || this.mIsCaptureSecureIntent;
                switchShutterSlidingAbility(z);
            } else {
                switchShutterSlidingAbility(false);
            }
        } else {
            switchShutterSlidingAbility(false);
        }
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        if (this.mIndicatorIconController != null) {
            this.mIndicatorIconController.syncIndicators();
        }
        if (this.isNeedShowArc) {
            setNeedShowArc(false);
            if (this.proHandler != null) {
                removeShowSeekBarMsg();
                Message message = Message.obtain();
                message.what = 1;
                this.proHandler.sendMessageDelayed(message, 500);
                return;
            }
            return;
        }
        if (this.mProArcSeekBarUI != null) {
            this.mProArcSeekBarUI.setActionMenuVisibility(1001);
        }
        removeShowSeekBarMsg();
    }

    public void setCameraProxy(CameraProxy camera) {
        if (this.mProArcSeekBarUI != null) {
            this.mProArcSeekBarUI.setCameraProxy(camera);
        }
    }

    public void addPreviewAreaChangedListener(PreviewAreaChangedListener listener) {
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(listener);
    }

    public void removePreviewAreaChangedListener(PreviewAreaChangedListener listener) {
        this.mTextureViewHelper.removePreviewAreaSizeChangedListener(listener);
    }

    public void prepareModuleUI() {
        this.mHelpTipsManager = this.mController.getHelpTipsManager();
        this.mAppRootView.setHelpTipManager(this.mHelpTipsManager);
        this.mController.getSettingsManager().addListener(this);
        this.mModuleUI = (FrameLayout) this.mCameraRootView.findViewById(R.id.module_layout);
        this.mTextureView = (TextureView) this.mCameraRootView.findViewById(R.id.preview_content);
        this.mGlSurfaceView = (CameraSurfaceView) this.mCameraRootView.findViewById(R.id.preview_content_gl);
        this.mTextureViewHelper = new TextureViewHelper(this.mTextureView, this.mModeTransitionView, this.mCaptureLayoutHelper, this.mController.getCameraProvider());
        this.mTextureViewHelper.setSurfaceTextureListener(this);
        this.mTextureViewHelper.setOnLayoutChangeListener(this.mPreviewLayoutChangeListener);
        this.mBottomBar = (BottomBar) this.mCameraRootView.findViewById(R.id.bottom_bar);
        int unpressedColor = this.mController.getAndroidContext().getResources().getColor(R.color.bottombar_unpressed);
        this.mBottomBar.setOnArcSeekBarListener(this);
        this.mBottomBar.setOnCancelSelectionMenu(this);
        setBottomBarColor(unpressedColor);
        updateModeSpecificUIColors();
        this.mBottomBar.setCaptureLayoutHelper(this.mCaptureLayoutHelper);
        this.mModeOptionsOverlay = (ModeOptionsOverlay) this.mCameraRootView.findViewById(R.id.mode_options_overlay);
        resetBottomControls(this.mController.getCurrentModuleController(), this.mController.getCurrentModuleIndex());
        this.mModeOptionsOverlay.setCaptureLayoutHelper(this.mCaptureLayoutHelper);
        this.mShutterButton = (ShutterButton) this.mCameraRootView.findViewById(R.id.shutter_button);
        addShutterListener(this.mController.getCurrentModuleController());
        addShutterListener(this.mModeOptionsOverlay);
        addShutterListener(this);
        if (this.mHelpTipsManager != null) {
            addShutterListener(this.mHelpTipsManager);
        }
        this.mPeekThumb = (PeekImageView) this.mCameraRootView.findViewById(R.id.peek_thumb);
        this.mPeekThumb.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                CameraAppUI.this.mController.onPeekThumbClicked(CameraAppUI.this.mPeekThumb.getUri());
            }
        });
        this.mMicroVideoProgressbar = this.mCameraRootView.findViewById(R.id.micro_video_progressbar);
        this.mSegmentRemoveButton = (RotatableButton) this.mCameraRootView.findViewById(R.id.button_segement_remove);
        this.mRemixButton = (RotatableButton) this.mCameraRootView.findViewById(R.id.button_remix);
        this.mController.addLockableToListenerPool(this.mRemixButton);
        this.mCaptureButton = (RotatableButton) this.mCameraRootView.findViewById(R.id.video_snap_button);
        this.mVideoShuttterBotton = (RotatableButton) this.mCameraRootView.findViewById(R.id.video_shutter_button);
        this.mCancelButton = (RotatableButton) this.mCameraRootView.findViewById(R.id.shutter_cancel_button);
        this.mHdrView = (RotateImageView) this.mCameraRootView.findViewById(R.id.hdr_badge);
        this.mPromptView = (TextView) this.mCameraRootView.findViewById(R.id.prompt_message);
        this.mVideoShuttterBotton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                android.util.Log.d("20190111", "onClick");
                if (CameraAppUI.this.mModeStripView.isLocked()) {
                    Log.e(CameraAppUI.TAG, "mModeStripView.isLocked()");
                    return;
                }
                if (CameraAppUI.this.mHelpTipsManager != null) {
                    CameraAppUI.this.mHelpTipsManager.goNextHelpTipStage();
                }
                Log.v(CameraAppUI.TAG, "click video shutter");
                CameraAppUI.this.gLockEventListener.forceBlocking();
                CameraAppUI.this.mController.onModeSelecting(true, new OnTransAnimationListener() {
                    public void onAnimationDone() {
                        Log.v(CameraAppUI.TAG, "try start video mode");
                        android.util.Log.d("20190111", "try start video mode");
                        CameraAppUI.this.onModeSelected(CameraAppUI.this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video));
                        CameraAppUI.this.hideModeOptions();
                    }
                });
            }
        });
        this.mController.addLockableToListenerPool(this.mPeekThumb);
        this.mController.addLockableToListenerPool(this.mVideoShuttterBotton);
        this.mController.addLockableToListenerPool(this.mShutterButton);
        this.mController.addLockableToListenerPool(this.mModeStripView);
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mPeekThumb, true));
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mShutterButton, true));
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mVideoShuttterBotton, true));
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mShutterButton, true));
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mSegmentRemoveButton, true));
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mRemixButton, true));
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mCaptureButton, true));
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mCancelButton, true));
        this.mController.addRotatableToListenerPool(new RotateEntity(this.mHdrView, true));
        this.mGridLines = (GridLines) this.mCameraRootView.findViewById(R.id.grid_lines);
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(this.mGridLines);
        this.mPreviewOverlay = (PreviewOverlay) this.mCameraRootView.findViewById(R.id.preview_overlay);
        this.mPreviewOverlay.setOnTouchListener(new MyTouchListener(this, null));
        this.mPreviewOverlay.addOnPreviewTouchedListener(this.mModeOptionsOverlay);
        this.mPreviewOverlay.setHelpTipsListener(this.mHelpTipsManager);
        this.mZoomBar = (ZoomBar) this.mCameraRootView.findViewById(R.id.zoom_bar);
        this.mZoomBar.setNavigationBarHeight(getNavigationHeight());
        this.mZoomBar.setCaptureLayoutHelper(this.mCaptureLayoutHelper);
        this.mCaptureOverlay = (CaptureAnimationOverlay) this.mCameraRootView.findViewById(R.id.capture_overlay);
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(this.mPreviewOverlay);
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(this.mCaptureOverlay);
        if (this.mIndicatorIconController == null) {
            this.mIndicatorIconController = new IndicatorIconController(this.mController, this.mAppRootView);
        }
        this.mController.getButtonManager().load(this.mCameraRootView);
        this.mController.getButtonManager().setListener(this.mIndicatorIconController);
        this.mController.getSettingsManager().addListener(this.mIndicatorIconController);
        this.mModeOptionsToggle = this.mCameraRootView.findViewById(R.id.mode_options_toggle);
        this.mFocusOverlay = this.mCameraRootView.findViewById(R.id.focus_overlay);
        this.mIndicatorBottomBarWrapper = (BottomBarModeOptionsWrapper) this.mAppRootView.findViewById(R.id.indicator_bottombar_wrapper);
        this.mIndicatorBottomBarWrapper.setCaptureLayoutHelper(this.mCaptureLayoutHelper);
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(new PreviewAreaChangedListener() {
            public void onPreviewAreaChanged(RectF previewArea) {
                if (CameraAppUI.this.mZoomBar != null) {
                    CameraAppUI.this.mZoomBar.setPreviewArea(previewArea);
                }
            }
        });
        this.mTextureViewHelper.addAspectRatioChangedListener(new PreviewAspectRatioChangedListener() {
            public void onPreviewAspectRatioChanged(float aspectRatio) {
                CameraAppUI.this.mModeOptionsOverlay.requestLayout();
                CameraAppUI.this.mBottomBar.requestLayout();
            }
        });
        filterBottomBarIconsWithoutAnimation(getCurrentModeIndex());
    }

    public Uri getPeekThumbUri() {
        if (this.mPeekThumb == null) {
            return null;
        }
        return this.mPeekThumb.getUri();
    }

    public void onVideoRecordingStateChanged(boolean started) {
        if (started) {
            this.mBottomBar.hidePeek();
        } else if (!this.mIsCaptureIntent) {
            this.mBottomBar.showPeek();
        }
    }

    public void updatePeekThumbContent(Thumbnail thumbnail) {
        this.mPeekThumb.setViewThumb(thumbnail);
    }

    public void updatePeekThumbBitmapWithAnimation(Bitmap bitmap) {
        this.mPeekThumb.animateThumbBitmap(bitmap);
    }

    public void updatePeekThumbBitmap(Bitmap bitmap) {
        this.mPeekThumb.setViewThumbBitmap(bitmap);
    }

    public void updatePeekThumbUri(Uri peekthumbUri) {
        this.mPeekThumb.setViewThumbUri(peekthumbUri);
    }

    public FrameLayout getModuleRootView() {
        return this.mCameraRootView;
    }

    public Integer getShutterHash() {
        return this.mShutterButton == null ? null : Integer.valueOf(this.mShutterButton.hashCode());
    }

    public boolean isShutterLocked() {
        return this.mShutterButton == null ? true : this.mShutterButton.isLocked();
    }

    public void clearModuleUI() {
        if (this.mModuleUI != null) {
            Log.v(TAG, "onAllView Removing ");
            this.mModuleUI.removeAllViews();
        }
        Log.w(TAG, "Remove shutter listener");
        removeShutterListener(this.mController.getCurrentModuleController());
        setShutterButtonEnabled(true);
        this.mPreviewStatusListener = null;
        this.mZoomBar.resetZoomRatio();
        this.mPreviewOverlay.reset();
        this.mFocusOverlay.setVisibility(4);
    }

    public void onPreviewReadyToStart() {
        if (this.mModeCoverState == 1) {
            setModeCoverState(2);
            this.mController.setupOneShotPreviewListener();
        }
    }

    public void onPreviewStarted() {
        Log.v(TAG, "onPreviewStarted");
        if (this.mModeCoverState == 1) {
            setModeCoverState(3);
        }
        enableModeOptions();
    }

    public void onGLSurfacePreviewStart() {
        this.mGlSurfaceView.onPreviewStarted();
    }

    public void onNewPreviewFrame() {
        Log.v(TAG, "onNewPreviewFrame");
        CameraPerformanceTracker.onEvent(5);
        hideModeCover();
    }

    public void onShutterButtonClick() {
        disableModeOptions();
    }

    public void onShutterButtonLongClick() {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonFocus(boolean pressed) {
    }

    public void enableModeOptions() {
        if (!this.mDisableAllUserInteractions && this.mModeOptionsOverlay != null) {
            this.mModeOptionsOverlay.setToggleClickable(true);
        }
    }

    public void disableModeOptions() {
        this.mModeOptionsOverlay.setToggleClickable(false);
    }

    public void setDisableAllUserInteractions(boolean disable, boolean mDuringCall, boolean mSLOorMIC) {
        if (disable) {
            disableModeOptions();
            setShutterButtonEnabled(false);
            setSwipeEnabled(false);
            this.mVideoShuttterBotton.setImageDrawable(this.mController.getAndroidContext().getResources().getDrawable(R.drawable.ic_video_record_pressed));
            this.mVideoShuttterBotton.setEnabled(false);
            if (this.mToken == null) {
                this.mToken = this.mController.lockModuleSelection();
            }
            this.mdisableUI = true;
        } else {
            enableModeOptions();
            setShutterButtonEnabled(true);
            setSwipeEnabled(true);
            if (!mDuringCall) {
                this.mVideoShuttterBotton.setImageDrawable(this.mController.getAndroidContext().getResources().getDrawable(R.drawable.btn_shutter_video_normal));
                this.mVideoShuttterBotton.setEnabled(true);
            }
            if (this.mToken != null) {
                this.mController.unlockModuleSelection(this.mToken);
                this.mToken = null;
            }
            this.mdisableUI = false;
        }
        if (disable || !mSLOorMIC || !mDuringCall) {
            this.mDisableAllUserInteractions = disable;
        }
    }

    public void setCalldisable(boolean calldisable) {
        if (!this.mdisableUI) {
            if (calldisable) {
                this.mShutterButton.setEnabled(false);
            } else {
                this.mShutterButton.setEnabled(true);
            }
            this.mDisableAllUserInteractions = calldisable;
        }
    }

    public void setmVideoShuttterBottondisable(boolean videodiable) {
        if (!this.mdisableUI) {
            if (videodiable) {
                this.mVideoShuttterBotton.setImageDrawable(this.mController.getAndroidContext().getResources().getDrawable(R.drawable.ic_video_record_pressed));
                this.mVideoShuttterBotton.setEnabled(false);
                return;
            }
            this.mVideoShuttterBotton.setImageDrawable(this.mController.getAndroidContext().getResources().getDrawable(R.drawable.btn_shutter_video_normal));
            this.mVideoShuttterBotton.setEnabled(true);
        }
    }

    public void onModeIdChanged(int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onModeIdChanged:id = ");
        stringBuilder.append(id);
        android.util.Log.d("20190110", stringBuilder.toString());
        onModeSelected(id);
        Log.e(TAG, "end mode selecting");
        BoostUtil.getInstance().releaseCpuLock();
    }

    public void onModeIdChanging() {
        BoostUtil.getInstance().acquireCpuLock();
        Log.v(TAG, "KPI start mode selecting");
        switchShutterSlidingAbility(false);
        this.mController.onModeSelecting();
    }

    public void onModeSelected(int modeIndex) {
        updateMode(modeIndex);
    }

    public void hideImageCover() {
        this.mModeTransitionView.hideImageCover();
    }

    public void showImageCover() {
        this.mModeTransitionView.setupModeCoverTileAnimationDone(this.mCameraModuleScreenShotProvider.getPreviewFrame(2), new OnTransAnimationListener[0]);
        this.mModeTransitionView.bringToFront();
    }

    private void updateMode(int modeIndex) {
        if (modeIndex == 0 && this.mController.getCameraProvider().getCurrentCameraId() == 0) {
            ModeTransitionView.sDelayedTime = 600;
            Log.d(TAG, "liugz..............switch rear btoken .............");
        }
        Log.w(TAG, "updateMode create hideCover runnable");
        this.mHideCoverRunnable = new Runnable() {
            public void run() {
                CameraAppUI.this.mModeTransitionView.hideImageCover();
            }
        };
        this.mShutterButton.setAlpha(1.0f);
        setModeCoverState(1);
        int lastIndex = this.mController.getCurrentModuleIndex();
        this.mController.onModeSelected(modeIndex);
        int currentIndex = this.mController.getCurrentModuleIndex();
        if (currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pano)) {
            this.mPreviewOverlay.setTouchEnabled(false);
        } else {
            this.mPreviewOverlay.setTouchEnabled(true);
        }
        filterBottomBarIcons(currentIndex, false);
        updateModeSpecificUIColors();
    }

    public void changeShutterButtonSavePanorama(boolean isInSave) {
        this.mShutterButton.setActivated(isInSave);
        this.mShutterButton.setClickable(isInSave ^ 1);
    }

    public void changeBottomBarInCapturePanorama() {
        this.mBottomBar.animateHidePeek();
        setModeSwitchUIVisibility(false);
        this.mBottomBar.setShutterButtonIcon(R.drawable.ic_panorama_capture);
        this.mBottomBar.overrideBottomBarColor(Integer.valueOf(0));
        hideModeOptions();
        setSwipeEnabled(false);
        setModeStripViewVisibility(false);
    }

    public void restoreBottomBarFinishPanorama() {
        this.mBottomBar.animateShowPeek();
        setModeSwitchUIVisibility(true);
        this.mBottomBar.overrideBottomBarColor(Integer.valueOf(0));
        setBottomBarShutterIcon(this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pano));
        showModeOptions();
        setSwipeEnabled(true);
        this.mBottomBar.overrideBottomBarColor(null);
        setModeStripViewVisibility(true);
    }

    private void filterBottomBarIcons(int currentIndex, boolean fullSize) {
        if (currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo)) {
            if (this.mIsCaptureIntent) {
                this.mBottomBar.animateHidePeek();
                this.mBottomBar.animateHideVideoShutter();
            } else {
                this.mBottomBar.showPeek();
                this.mBottomBar.animateHideVideoShutter();
            }
            this.mBottomBar.animateHideSegementRemove();
            this.mBottomBar.animateHideRemix();
            this.mBottomBar.animateHideVideoCapture();
            this.mBottomBar.animateHideVideoPause();
        } else if (currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_micro_video)) {
            if (!fullSize) {
                this.mBottomBar.animateHideSegementRemove();
                this.mBottomBar.animateHideRemix();
                this.mBottomBar.animateShowPeek();
                this.mBottomBar.animateHideVideoShutter();
                this.mBottomBar.animateHideVideoCapture();
                this.mBottomBar.animateHideVideoPause();
            }
        } else if (!(currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video) || currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video_capture))) {
            this.mBottomBar.animateHideVideoCapture();
            this.mBottomBar.animateHideVideoPause();
            this.mBottomBar.animateShowPeek();
            this.mBottomBar.animateHideVideoShutter();
            this.mBottomBar.animateHideSegementRemove();
            this.mBottomBar.animateHideRemix();
        }
        if (currentIndex != this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_micro_video)) {
            this.mMicroVideoProgressbar.setVisibility(8);
        }
        this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_panorama);
    }

    private void filterBottomBarIconsWithoutAnimation(int currentIndex) {
        if (currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo)) {
            if (this.mIsCaptureIntent) {
                this.mBottomBar.hidePeek();
                this.mBottomBar.hideVideoShutter();
            } else {
                this.mBottomBar.showPeek();
                this.mBottomBar.hideVideoShutter();
            }
            this.mBottomBar.hideSegementRemove();
            this.mBottomBar.hideRemix();
            this.mBottomBar.hideVideoCapture();
            this.mBottomBar.hideVideoPause();
        } else if (currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_micro_video)) {
            this.mBottomBar.showPeek();
            this.mBottomBar.hideVideoShutter();
            this.mBottomBar.hideSegementRemove();
            this.mBottomBar.hideRemix();
            this.mBottomBar.hideVideoCapture();
            this.mBottomBar.hideVideoPause();
        } else if (currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video) || currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video_capture)) {
            this.mBottomBar.showVideoPause();
            this.mBottomBar.showPeek();
            this.mBottomBar.hideVideoShutter();
            this.mBottomBar.hideSegementRemove();
            this.mBottomBar.hideRemix();
            if (this.mIsCaptureIntent) {
                android.util.Log.i("camera", "hideVideoCapture");
                this.mBottomBar.hidePeek();
                this.mBottomBar.hideVideoCapture();
            } else {
                android.util.Log.i("camera", "showVideoCapture");
                this.mBottomBar.showVideoCapture();
            }
        } else {
            this.mBottomBar.showPeek();
            this.mBottomBar.hideVideoShutter();
            this.mBottomBar.hideSegementRemove();
            this.mBottomBar.hideRemix();
            this.mBottomBar.hideVideoCapture();
            this.mBottomBar.hideVideoPause();
        }
        if (currentIndex != this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_micro_video)) {
            this.mMicroVideoProgressbar.setVisibility(8);
        }
        if (currentIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pano)) {
            this.mPreviewOverlay.setTouchEnabled(false);
        }
    }

    public void showMicroVideoEditButtons(boolean withAnimation) {
        if (withAnimation) {
            this.mBottomBar.animateHidePeek();
            this.mBottomBar.animateShowRemix();
            this.mBottomBar.animateShowSegmentRemove();
            return;
        }
        this.mBottomBar.hidePeek();
        this.mBottomBar.showRemix();
        this.mBottomBar.showSegmentRemove();
    }

    public void hideMicroVideoEditButtons(boolean withAnimation) {
        if (withAnimation) {
            this.mBottomBar.animateShowPeek();
            this.mBottomBar.animateHideRemix();
            this.mBottomBar.animateHideSegementRemove();
            return;
        }
        this.mBottomBar.showPeek();
        this.mBottomBar.hideRemix();
        this.mBottomBar.hideSegementRemove();
    }

    public void animateHidePeek() {
        this.mBottomBar.animateHidePeek();
    }

    public void updateHdrViewVisable(boolean show) {
        if (show) {
            this.mHdrView.setVisibility(0);
        } else {
            this.mHdrView.setVisibility(8);
        }
    }

    public void updatePromptViewVisable(boolean show) {
        if (show) {
            this.mPromptView.setVisibility(0);
        } else {
            this.mPromptView.setVisibility(8);
        }
    }

    public void setPromptMessage(String message) {
        this.mPromptView.setText(message);
    }

    public void showVideoCaptureButton(boolean animate) {
        if (animate) {
            if (this.mBottomBar != null) {
                this.mBottomBar.animateShowVideoCapture();
            }
        } else if (this.mCaptureButton != null) {
            this.mBottomBar.showVideoCapture();
        }
    }

    public void hideVideoCaptureButton(boolean animate) {
        if (animate) {
            if (this.mBottomBar != null) {
                this.mBottomBar.animateHideVideoCapture();
            }
        } else if (this.mCaptureButton != null) {
            this.mBottomBar.hideVideoCapture();
        }
    }

    public void showVideoPauseButton(boolean animate) {
        android.util.Log.i("camera", "showVideoPauseButton");
        if (animate) {
            if (this.mBottomBar != null) {
                this.mBottomBar.animateShowVideoPause();
            }
        } else if (this.mCaptureButton != null) {
            this.mBottomBar.showVideoPause();
        }
    }

    public void hideVideoPauseButton(boolean animate) {
        android.util.Log.i("camera", "hideVideoPauseButton");
        if (animate) {
            if (this.mBottomBar != null) {
                this.mBottomBar.animateHideVideoPause();
            }
        } else if (this.mCaptureButton != null) {
            this.mBottomBar.hideVideoPause();
        }
    }

    public void hideCaptureButton() {
        if (this.mBottomBar != null) {
            this.mBottomBar.hideVideoCapture();
        }
    }

    private void updateModeSpecificUIColors() {
        setBottomBarColorsForModeIndex(this.mController.getCurrentModuleIndex());
    }

    public void onSettingsSelected() {
        this.mController.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING, false);
        this.mController.onSettingsSelected();
    }

    public int getCurrentModeIndex() {
        return this.mController.getCurrentModuleIndex();
    }

    public void startPeekAnimation(Bitmap bitmap, boolean strong, String accessibilityString) {
    }

    public void startScalingPeekAnimation(Bitmap bitmap, boolean strong, String accessiblityString, OnCaptureStateListener listener) {
    }

    public void startPreCaptureAnimation(boolean shortFlash) {
        this.mCaptureOverlay.startFlashAnimation(shortFlash);
    }

    public void cancelPreCaptureAnimation() {
        this.mAnimationManager.cancelAnimations();
    }

    public void cancelPostCaptureAnimation() {
        this.mAnimationManager.cancelAnimations();
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mTextureView.getSurfaceTexture();
    }

    public SurfaceTexture getSurfaceTexture(boolean flag) {
        if (flag) {
            return this.mGlSurfaceView.getSurfaceTexture();
        }
        return this.mTextureView.getSurfaceTexture();
    }

    public void initSurfaceRender(CameraProxy camera) {
        this.mGlSurfaceView.initRender(camera);
        hideModeCover();
    }

    public int getSurfaceWidth() {
        return this.mSurfaceWidth;
    }

    public int getSurfaceHeight() {
        return this.mSurfaceHeight;
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.mSurface = surface;
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("KPI SurfaceTexture is available:width:");
        stringBuilder.append(width);
        stringBuilder.append(" ,height:");
        stringBuilder.append(height);
        Log.v(tag, stringBuilder.toString());
        if (this.mPreviewStatusListener != null) {
            this.mPreviewStatusListener.onSurfaceTextureAvailable(surface, width, height);
        }
        enableModeOptions();
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        this.mSurface = surface;
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        if (this.mPreviewStatusListener != null) {
            this.mPreviewStatusListener.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        this.mSurface = null;
        Log.v(TAG, "SurfaceTexture is destroyed");
        if (this.mPreviewStatusListener != null) {
            return this.mPreviewStatusListener.onSurfaceTextureDestroyed(surface);
        }
        return false;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.v(TAG, "KPI on surfaceTexture updated");
        this.mSurface = surface;
        if (this.mPreviewStatusListener != null) {
            this.mPreviewStatusListener.onSurfaceTextureUpdated(surface);
        }
        if (!PhotoModule.firstFrame) {
            PhotoModule.firstFrame = true;
            if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_OPTIMIZE_SWITCH, false) && (this.mController.getCurrentModuleController() instanceof PhotoModule)) {
                ((PhotoModule) this.mController.getCurrentModuleController()).updateFaceBeautyWhenFrameReady();
            }
        }
        if (this.mModeCoverState == 3) {
            Log.v(TAG, "hiding cover via onSurfaceTextureUpdated");
            CameraPerformanceTracker.onEvent(5);
            hideModeCover();
        }
    }

    public void showGridLines() {
        if (this.mGridLines != null) {
            this.mGridLines.setVisibility(0);
        }
    }

    public void hideGridLines() {
        if (this.mGridLines != null) {
            this.mGridLines.setVisibility(4);
        }
    }

    public ButtonCallback getGridLinesCallback() {
        return new ButtonCallback() {
            public void onStateChanged(int state) {
                if (Keys.areGridLinesOn(CameraAppUI.this.mController.getSettingsManager())) {
                    CameraAppUI.this.showGridLines();
                } else {
                    CameraAppUI.this.hideGridLines();
                }
            }
        };
    }

    public void showModeOptions() {
        enableModeOptions();
        this.mModeOptionsOverlay.setVisibility(0);
        if (this.mOnModeOptionsVisibilityChangedListener != null) {
            this.mOnModeOptionsVisibilityChangedListener.onModeOptionsVisibilityChanged(0);
        }
    }

    public void hideModeOptions() {
        this.mModeOptionsOverlay.setVisibility(4);
        if (this.mOnModeOptionsVisibilityChangedListener != null) {
            this.mOnModeOptionsVisibilityChangedListener.onModeOptionsVisibilityChanged(4);
        }
    }

    public void resetBottomControls(ModuleController module, int moduleIndex) {
        if (areBottomControlsUsed(module)) {
            setBottomBarShutterIcon(moduleIndex);
            this.mCaptureLayoutHelper.setShowBottomBar(true);
            return;
        }
        this.mCaptureLayoutHelper.setShowBottomBar(false);
    }

    public void resetAlpha(boolean alpha) {
        if (alpha) {
            this.mShutterButton.setAlpha(0.4f);
        } else {
            this.mShutterButton.setAlpha(1.0f);
        }
    }

    private boolean areBottomControlsUsed(ModuleController module) {
        if (module.isUsingBottomBar()) {
            showBottomBar();
            showModeOptions();
            return true;
        }
        hideBottomBar();
        hideModeOptions();
        return false;
    }

    public void showBottomBar() {
        this.mBottomBar.setVisibility(0);
    }

    public void hideBottomBar() {
        this.mBottomBar.setVisibility(4);
    }

    public void setBottomBarColor(int colorId) {
        this.mBottomBar.setBackgroundColor(colorId);
    }

    public void setBottomBarColorsForModeIndex(int index) {
        this.mBottomBar.setColorsForModeIndex(index);
    }

    public void setBottomBarShutterIcon(int modeIndex) {
        int shutterIconId = CameraUtil.getCameraShutterIconId(modeIndex, this.mController.getAndroidContext());
        if (this.mIsCaptureIntent) {
            this.mBottomBar.setShutterButtonIcon(shutterIconId, false);
        } else {
            this.mBottomBar.setShutterButtonIcon(shutterIconId);
        }
    }

    public void animateBottomBarToVideoStop(int shutterIconId) {
        this.mController.getButtonManager().setMoreEnterToggleButton(8);
        if (getCurrentModeIndex() == 14) {
            this.mController.getButtonManager().setEffectsEnterToggleButton(8);
        } else {
            this.mController.getButtonManager().setEffectsEnterToggleButton(4);
        }
        this.mBottomBar.animateToVideoStop(shutterIconId);
    }

    public void animateBottomBarToFullSize(int shutterIconId, BottomBarSizeListener listener, boolean isTL) {
        filterBottomBarIcons(this.mController.getCurrentModuleIndex(), true);
        this.mBottomBar.animateToFullSize(shutterIconId, listener, isTL);
    }

    public void setShutterButtonEnabled(boolean enabled) {
        setShutterButtonEnabled(enabled, true);
    }

    public void setShutterButtonEnabled(final boolean enabled, final boolean needChangeAlpha) {
        if (!this.mDisableAllUserInteractions) {
            if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
                this.mBottomBar.post(new Runnable() {
                    public void run() {
                        if (needChangeAlpha) {
                            CameraAppUI.this.mBottomBar.setShutterButtonEnabled(enabled);
                        } else {
                            CameraAppUI.this.mBottomBar.setShutterbuttonEnabledWithoutAppearenceChanged(enabled);
                        }
                    }
                });
            } else if (needChangeAlpha) {
                this.mBottomBar.setShutterButtonEnabled(enabled);
            } else {
                this.mBottomBar.setShutterbuttonEnabledWithoutAppearenceChanged(enabled);
            }
        }
    }

    public void setShutterButtonPress(final boolean press) {
        if (!this.mDisableAllUserInteractions) {
            this.mBottomBar.post(new Runnable() {
                public void run() {
                    CameraAppUI.this.mBottomBar.setShutterButtonPress(press);
                }
            });
        }
    }

    public void setShutterButtonLongClickable(final boolean enabled) {
        if (!this.mDisableAllUserInteractions) {
            this.mBottomBar.post(new Runnable() {
                public void run() {
                    CameraAppUI.this.mBottomBar.setShutterButtonLongClickable(enabled);
                }
            });
        }
    }

    public void setShutterButtonImportantToA11y(boolean important) {
        this.mBottomBar.setShutterButtonImportantToA11y(important);
    }

    public boolean isShutterButtonEnabled() {
        return this.mBottomBar.isShutterButtonEnabled();
    }

    public void setIndicatorBottomBarWrapperVisible(boolean visible) {
        int i = 4;
        this.mIndicatorBottomBarWrapper.setVisibility(visible ? 0 : 4);
        if (visible) {
            i = 0;
        }
        int vFlag = i;
        this.mIndicatorBottomBarWrapper.setVisibility(vFlag);
        this.mController.getButtonManager().setMOBExtraVisibility(vFlag);
    }

    public void setBottomBarVisible(boolean visible) {
        this.mBottomBar.setVisibility(visible ? 0 : 4);
    }

    public void addShutterListener(OnShutterButtonListener listener) {
        Tag tag = this.SHUTTER_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("add shutter listener");
        stringBuilder.append(listener);
        Log.w(tag, stringBuilder.toString());
        this.mShutterButton.addOnShutterButtonListener(listener);
    }

    public void removeShutterListener(OnShutterButtonListener listener) {
        Tag tag = this.SHUTTER_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("remove shutter listener");
        stringBuilder.append(listener);
        Log.w(tag, stringBuilder.toString());
        this.mShutterButton.removeOnShutterButtonListener(listener);
    }

    public void transitionToCapture() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        this.mBottomBar.transitionToCapture();
        this.mIntentReviewControls.hide();
        this.mBottomBar.setVisibility(0);
        if (this.mCaptureLayoutHelper.shouldOverlayBottomBar()) {
            this.mBottomBar.setIsBackgroundTransparent(false);
        }
        if (getCurrentModeIndex() == 15 || getCurrentModeIndex() == 14 || (getCurrentModeIndex() == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro) && (isArcSeekBarShow() || this.isCommShow))) {
            setModeStripViewVisibility(false);
        } else {
            setModeStripViewVisibility(true);
        }
        ((CameraActivity) this.mController).getWindow().getDecorView().setSystemUiVisibility(SHOW_NAVIGATION_VIEW);
    }

    public void transitionToCancel() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        this.mBottomBar.transitionToCancel();
        this.mIntentReviewControls.hide();
        this.mBottomBar.setVisibility(0);
        if (this.mCaptureLayoutHelper.shouldOverlayBottomBar()) {
            this.mBottomBar.setIsBackgroundTransparent(true);
        }
        setModeStripViewVisibility(false);
    }

    public void transitionToIntentCaptureLayout() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        this.mBottomBar.transitionToIntentCaptureLayout();
        this.mIntentReviewControls.hide();
        this.mModeOptionsOverlay.setVisibility(0);
        this.mBottomBar.setVisibility(0);
    }

    public void transitionToIntentReviewLayout() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        this.mBottomBar.transitionToIntentReviewLayout();
        this.mIntentReviewControls.show(moduleController.getBottomBarSpec().showCancel, moduleController.getBottomBarSpec().showDone, moduleController.getBottomBarSpec().showRetake, moduleController.getBottomBarSpec().showReview);
        this.mModeOptionsOverlay.setVisibility(8);
        this.mBottomBar.setVisibility(8);
    }

    public boolean isInIntentReview() {
        return this.mBottomBar.isInIntentReview();
    }

    public void onSettingChanged(SettingsManager settingsManager, String key) {
        if (key.equals(Keys.KEY_CAMERA_MOTION) && getCurrentModeIndex() != 0) {
            ModuleController moduleController = this.mController.getCurrentModuleController();
            applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        }
    }

    private void surfaceviewQueueRun(Runnable run) {
        if (this.mGlSurfaceView != null) {
            this.mGlSurfaceView.queueEvent(run);
        }
    }

    public int getFlashState() {
        return this.mFlashState;
    }

    public void applyModuleSpecs(HardwareSpec hardwareSpec, BottomBarUISpec bottomBarSpec) {
        BottomBarUISpec bottomBarUISpec = bottomBarSpec;
        if (bottomBarUISpec != null) {
            this.mBottomBarUISpec = bottomBarUISpec;
        }
        if (hardwareSpec != null && bottomBarUISpec != null) {
            boolean isMotionOn;
            Log.v(TAG, "Timon apply bottomSpec");
            ButtonManager buttonManager = this.mController.getButtonManager();
            SettingsManager settingsManager = this.mController.getSettingsManager();
            buttonManager.hideMoreOptionsWrapper();
            buttonManager.setEffectsCallBack(new EffectOnClick() {
                public void onItemClick(int pos, final String path) {
                    final Message message = Message.obtain();
                    SharedPreferences sharedPreferences = CameraAppUI.this.mController.getAndroidContext().getSharedPreferences("EffectClickTimesForToast", 0);
                    int clickTime = sharedPreferences.getInt("ClickTime", 5);
                    if (clickTime > 0) {
                        sharedPreferences.edit().putInt("ClickTime", clickTime - 1).apply();
                        ToastUtil.showToast(CameraAppUI.this.mController.getAndroidContext(), (int) R.string.firstboot_message, 1);
                    }
                    CameraAppUI.this.surfaceviewQueueRun(new Runnable() {
                        public void run() {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("1-mCurrSelect = ");
                            stringBuilder.append(CameraAppUI.this.mCurrSelect);
                            android.util.Log.d("GLMODE", stringBuilder.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("path = ");
                            stringBuilder.append(path);
                            android.util.Log.d("GLMODE", stringBuilder.toString());
                            if (TextUtils.isEmpty(CameraAppUI.this.mCurrSelect) || !path.equals(CameraAppUI.this.mCurrSelect)) {
                                BeaurifyJniSdk.preViewInstance().nativeChangePackage(path);
                                CameraAppUI.this.mCurrSelect = path;
                                CameraAppUI.this.setEffectEnable(true);
                            } else {
                                CameraAppUI.this.setEffectEnable(false);
                                if (path.equals(CameraAppUI.this.mCurrSelect)) {
                                    BeaurifyJniSdk.preViewInstance().nativeDisablePackage();
                                    CameraAppUI.this.mCurrSelect = "";
                                }
                            }
                            message.obj = CameraAppUI.this.mCurrSelect;
                            CameraAppUI.this.mMainHandler.sendMessage(message);
                        }
                    });
                }
            });
            buttonManager.setToInitialState();
            buttonManager.setBeautyCallBack(new BeautyStateListener() {
                public void onStateChanged(int state) {
                    CameraAppUI.this.mBeauState = state;
                }
            });
            if (!this.mIsCaptureIntent) {
                buttonManager.initializeSettingButton(new Runnable() {
                    public void run() {
                        CameraAppUI.this.onSettingsSelected();
                    }
                });
                if (bottomBarUISpec.hideSetting) {
                    buttonManager.hideSettings();
                } else {
                    buttonManager.showSettings();
                }
            }
            if (bottomBarUISpec.hideCamera || this.mController.getCameraProvider().getNumberOfCameras() <= 1 || !hardwareSpec.isFrontCameraSupported()) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("numberofcameras = ");
                stringBuilder.append(this.mController.getCameraProvider().getNumberOfCameras());
                Log.v(tag, stringBuilder.toString());
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isfrontcamerasupported = ");
                stringBuilder.append(hardwareSpec.isFrontCameraSupported());
                Log.v(tag, stringBuilder.toString());
                if (this.mController.getCameraProvider().getNumberOfCameras() > 1 && hardwareSpec.isFrontCameraSupported() && !bottomBarUISpec.hideCameraForced && !bottomBarUISpec.setCameraInvisible) {
                    buttonManager.initializeButton(16, null);
                } else if (bottomBarUISpec.setCameraInvisible) {
                    buttonManager.setButtonInvisible(3);
                } else {
                    buttonManager.hideButton(3);
                }
            } else if (bottomBarUISpec.enableCamera) {
                buttonManager.initializeButton(3, bottomBarUISpec.cameraCallback);
            } else {
                buttonManager.disableButton(3);
            }
            if (PhotoModule.class.getSimpleName().equals(bottomBarUISpec.moduleName) || LiveBokehModule.class.getSimpleName().equals(bottomBarUISpec.moduleName)) {
                if (bottomBarUISpec.showMotion) {
                    buttonManager.initializeMotionButton();
                } else {
                    buttonManager.hideButton(18);
                }
                isMotionOn = Keys.isMotionOn(settingsManager);
                int id = this.mController.getCameraProvider().getCurrentCameraId();
                boolean enable = Keys.isDepthOn(settingsManager);
                if (!isMotionOn && ((bottomBarUISpec.showBeautyButton || bottomBarUISpec.showEffectButton || !bottomBarUISpec.hideBolken) && (!enable || ((enable && ((id == 1 && getCurrentModeIndex() == 4) || getCurrentModeIndex() == 15)) || (enable && getCurrentModeIndex() == 0))))) {
                    buttonManager.initEffectsEnterToggleButton();
                    buttonManager.setEffectsEnterToggleButton(0);
                    if (bottomBarUISpec.showEffectButton) {
                        buttonManager.initializeEffectButton();
                    } else {
                        buttonManager.hideEffectLayout();
                        buttonManager.hideEffectButton(getCurrentModeIndex());
                    }
                    if (bottomBarUISpec.showBeautyButton) {
                        buttonManager.initializeBeautyButton(bottomBarUISpec.seekbarChangeListener);
                    } else {
                        buttonManager.hideBeautyButton();
                        buttonManager.hideBeautyLayout();
                    }
                    if (bottomBarUISpec.hideBolken) {
                        buttonManager.hideLiveBolkenButton();
                        buttonManager.hideLiveBolkenLayout();
                    } else if (this.mController.getCameraProvider().getCurrentCameraId() == 0 || this.mController.getCameraProvider().getCurrentCameraId() == 3) {
                        buttonManager.initLiveBolkenLayout(bottomBarUISpec.seekbarBolkenListener);
                        buttonManager.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                        buttonManager.showEffectsContainerWrapper();
                        buttonManager.showLiveBolkenButton();
                        buttonManager.showLiveBolkenLayout();
                    } else {
                        buttonManager.initLiveBolkenLayout(bottomBarUISpec.seekbarBolkenListener);
                        if (this.mBeautyEnable) {
                            buttonManager.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                            buttonManager.showLiveBolkenButton();
                            buttonManager.showEffectsContainerWrapper();
                            buttonManager.showLiveBolkenLayout();
                            buttonManager.hideBeautyLayout();
                        }
                    }
                } else if (isMotionOn && getCurrentModeIndex() == 0) {
                    buttonManager.initEffectsEnterToggleButton();
                    buttonManager.showLiveBolkenLayout();
                    buttonManager.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                    buttonManager.initLiveBolkenLayout(bottomBarUISpec.seekbarBolkenListener);
                    buttonManager.showLiveBolkenButton();
                    buttonManager.setEffectsEnterToggleButton(0);
                    buttonManager.hideBeautyButton();
                    buttonManager.hideBeautyLayout();
                } else {
                    buttonManager.initEffectsEnterToggleButton();
                    if (getCurrentModeIndex() == 0 && (this.mController.getCameraProvider().getCurrentCameraId() == 0 || this.mController.getCameraProvider().getCurrentCameraId() == 3)) {
                        buttonManager.showLiveBolkenLayout();
                        buttonManager.initLiveBolkenLayout(bottomBarUISpec.seekbarBolkenListener);
                        buttonManager.showLiveBolkenButton();
                    } else {
                        buttonManager.hideEffectsContainerWrapper();
                        buttonManager.setEffectsEnterToggleButton(4);
                    }
                }
                buttonManager.initializeDualSightButton();
                buttonManager.hideButtonLives();
            } else {
                buttonManager.hidePhotoModuleRelatedButtons();
                if (VideoModule.class.getSimpleName().equals(bottomBarUISpec.moduleName)) {
                    if (bottomBarUISpec.showBeautyButton || bottomBarUISpec.showEffectButton) {
                        buttonManager.initEffectsEnterToggleButton();
                        buttonManager.setEffectsEnterToggleButton(0);
                        if (bottomBarUISpec.showEffectButton) {
                            buttonManager.initializeEffectButton();
                        } else {
                            buttonManager.hideEffectLayout();
                            buttonManager.hideEffectButton(getCurrentModeIndex());
                        }
                        if (bottomBarUISpec.showBeautyButton) {
                            buttonManager.initializeBeautyButton(bottomBarUISpec.seekbarChangeListener);
                        } else {
                            buttonManager.hideBeautyButton();
                            buttonManager.hideBeautyLayout();
                        }
                    } else {
                        buttonManager.setEffectsEnterToggleButton(4);
                    }
                    if (bottomBarUISpec.hideBolken) {
                        buttonManager.hideLiveBolkenButton();
                        buttonManager.hideLiveBolkenLayout();
                    } else {
                        buttonManager.initLiveBolkenLayout(bottomBarUISpec.seekbarBolkenListener);
                        buttonManager.showLiveBolkenButton();
                    }
                }
            }
            buttonManager.initMoreEnterToggleButton();
            if (SquareModule.class.getSimpleName().equals(bottomBarUISpec.moduleName)) {
                buttonManager.hideMotionButtons();
                buttonManager.hideDualSightButton();
            }
            isMotionOn = this.mController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA);
            if (bottomBarUISpec.hideFlash || !isMotionOn) {
                buttonManager.hideButtons(0);
                buttonManager.hideButtons(1);
            } else if (!hardwareSpec.isFlashSupported()) {
                buttonManager.initializeFlashButton(0, bottomBarUISpec.flashCallback);
                if (SharedUtil.getIntValueByKey("currentBatteryStatus").intValue() == 0) {
                    buttonManager.enableButton(0);
                    buttonManager.enableButton(1);
                }
            } else if (bottomBarUISpec.enableFlash) {
                buttonManager.initializeFlashButton(0, bottomBarUISpec.flashCallback);
            } else if (bottomBarUISpec.enableTorchFlash) {
                buttonManager.initializeFlashButton(0, bottomBarUISpec.flashCallback);
                buttonManager.initializeButton(1, bottomBarUISpec.flashCallback);
            } else if (bottomBarUISpec.enableHdrPlusFlash) {
                buttonManager.initializeButton(2, bottomBarUISpec.flashCallback);
            } else {
                buttonManager.initializeButton(0, null);
                buttonManager.disableButton(0);
                buttonManager.disableButton(1);
            }
            if (bottomBarUISpec.hideLive) {
                buttonManager.hideButton(17);
            }
            if (bottomBarUISpec.hideHdr) {
                buttonManager.hideButtons(5);
            } else if (hardwareSpec.isHdrPlusSupported()) {
                if (bottomBarUISpec.enableHdr && Keys.isCameraBackFacing(settingsManager, SettingsManager.SCOPE_GLOBAL)) {
                    buttonManager.initializeButton(4, bottomBarUISpec.hdrCallback);
                } else {
                    buttonManager.disableButton(4);
                }
            } else if (!hardwareSpec.isHdrSupported()) {
                buttonManager.hideButton(4);
            } else if (bottomBarUISpec.enableHdr) {
                buttonManager.initializeHdrButton(5, bottomBarUISpec.hdrCallback);
            } else {
                buttonManager.disableButton(5);
            }
            if (bottomBarUISpec.hideLowlight) {
                buttonManager.hideButton(13);
            } else {
                buttonManager.hideButton(13);
            }
            if (bottomBarUISpec.hideGridLines) {
                buttonManager.hideButton(10);
                String moduleScope = this.mController.getModuleScope();
                if (Keys.areGridLinesOn(this.mController.getSettingsManager()) && (moduleScope.endsWith(NormalPhotoModule.AUTO_MODULE_STRING_ID) || moduleScope.endsWith(ManualModule.MANUAL_MODULE_STRING_ID) || moduleScope.endsWith(NormalVideoModule.NORMAL_VIDEO_MODULE_STRING_ID) || moduleScope.endsWith(TimeLapsedModule.TIME_SLPASE_VIDEO_MODULE_STRING_ID) || moduleScope.endsWith(SquareModule.SQUARE_MODULE_STRING_ID) || moduleScope.endsWith(LiveBokehModule.PHOTO_MODULE_STRING_ID))) {
                    showGridLines();
                } else {
                    hideGridLines();
                }
            } else if (bottomBarUISpec.enableGridLines) {
                buttonManager.initializeButton(10, bottomBarUISpec.gridLinesCallback != null ? bottomBarUISpec.gridLinesCallback : getGridLinesCallback());
            } else {
                buttonManager.disableButton(10);
                if (Keys.areGridLinesOn(this.mController.getSettingsManager())) {
                    showGridLines();
                } else {
                    hideGridLines();
                }
            }
            if (bottomBarUISpec.enableSelfTimer) {
                buttonManager.initializeButton(12, null);
            } else if (bottomBarUISpec.showSelfTimer) {
                buttonManager.disableButton(12);
            } else {
                buttonManager.hideButton(12);
            }
            if (bottomBarUISpec.enablePanoOrientation && PhotoSphereHelper.getPanoramaOrientationOptionArrayId() > 0) {
                buttonManager.initializePanoOrientationButtons(bottomBarUISpec.panoOrientationCallback);
            }
            boolean enableExposureCompensation = bottomBarUISpec.enableExposureCompensation && (!(bottomBarUISpec.minExposureCompensation == 0 && bottomBarUISpec.maxExposureCompensation == 0) && this.mController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED));
            if (enableExposureCompensation) {
                buttonManager.initializePushButton(11, null);
                buttonManager.setExposureCompensationParameters(bottomBarUISpec.minExposureCompensation, bottomBarUISpec.maxExposureCompensation, bottomBarUISpec.exposureCompensationStep);
                buttonManager.setExposureCompensationCallback(bottomBarUISpec.exposureCompensationSetCallback);
                buttonManager.updateExposureButtons();
            } else {
                buttonManager.hideButton(11);
                buttonManager.setExposureCompensationCallback(null);
            }
            if (bottomBarUISpec.showCancel) {
                buttonManager.initializePushButton(6, bottomBarUISpec.cancelCallback);
            }
            if (bottomBarUISpec.showDone) {
                buttonManager.initializePushButton(7, bottomBarUISpec.doneCallback);
            }
            if (bottomBarUISpec.showRetake) {
                buttonManager.initializePushButton(8, bottomBarUISpec.retakeCallback);
            }
            if (bottomBarUISpec.showReview) {
                buttonManager.initializePushButton(9, bottomBarUISpec.reviewCallback, R.drawable.ic_play);
            }
            if (bottomBarUISpec.showWrapperButton) {
                buttonManager.initializePushButton(14, null);
            } else {
                buttonManager.hideButton(14);
            }
            if (getCurrentModeIndex() == 16) {
                buttonManager.hideSettings();
                buttonManager.hideButton(0);
                buttonManager.hideButton(3);
                this.mBottomBar.hidePeek();
                this.mBottomBar.hideShutterButton();
            } else {
                if (!this.mIsCaptureIntent) {
                    this.mBottomBar.showPeek();
                }
                this.mBottomBar.showShutterButton();
            }
            if (getCurrentModeIndex() == 15 || getCurrentModeIndex() == 14) {
                buttonManager.hideSettings();
                buttonManager.hideDualSightButton();
                buttonManager.setMoreEnterToggleButton(4);
                buttonManager.initializeExitMoreButton();
                this.mBottomBar.setModeStripVisible(8);
                buttonManager.setExitMoreButtonVisible(0);
                buttonManager.setIndicatorText(bottomBarUISpec.moreName);
                buttonManager.setIndicatorTextVisible(0);
                setSwipeEnabled(false);
                if (getCurrentModeIndex() == 14) {
                    buttonManager.hideEffectsContainerWrapper();
                    buttonManager.setMoreEnterToggleButton(8);
                }
            } else {
                setSwipeEnabled(true);
                buttonManager.setExitMoreButtonVisible(8);
                int photoId = this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo);
                int videoId = this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video_capture);
                int proId = this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro);
                if ((this.mIsCaptureIntent && (getCurrentModeIndex() == photoId || getCurrentModeIndex() == videoId)) || (getCurrentModeIndex() == proId && (isArcSeekBarShow() || this.isCommShow))) {
                    buttonManager.setMoreEnterToggleButton(4);
                } else {
                    buttonManager.setMoreEnterToggleButton(0);
                }
                if (getCurrentModeIndex() == proId && (isArcSeekBarShow() || this.isCommShow)) {
                    this.mBottomBar.setModeStripVisible(8);
                } else {
                    this.mBottomBar.setModeStripVisible(0);
                }
                this.mController.getButtonManager().setIndicatorTextVisible(8);
                if (this.mIsCaptureIntent && getCurrentModeIndex() == photoId) {
                    buttonManager.setSettingButton(8);
                } else if (this.mIsCaptureIntent && getCurrentModeIndex() == videoId) {
                    buttonManager.setSettingButton(4);
                } else {
                    buttonManager.showSettings();
                }
            }
            if (this.mController.getCurrentModuleIndex() == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_livebokeh)) {
                this.mController.getButtonManager().hideButton(0);
            }
            if (bottomBarUISpec.showBeauty2) {
                buttonManager.showBeauty2Button();
            } else {
                buttonManager.hideBeauty2Button();
            }
            if (bottomBarUISpec.showEffect2) {
                buttonManager.showEffect2Button();
            } else {
                buttonManager.hideEffect2Button();
            }
        }
    }

    private void setLockState(LockState state) {
        switch (state) {
            case BLOCKING:
                lockPool(null);
                return;
            case BLOCK_FROM_MODE_SWITCHING:
                lockPool(this.mModeStripView);
                return;
            case IDLE:
                unlockPool();
                return;
            case BLOCK_FROM_SHUTTER:
                lockPool(this.mShutterButton);
                return;
            default:
                return;
        }
    }

    public synchronized void addLockableToListenerPool(Lockable lockable) {
        this.mListeningLockable.put(Integer.valueOf(lockable.hashCode()), lockable);
    }

    public synchronized void removeLockableFromListenerPool(Lockable lockable) {
        this.mListeningLockable.remove(Integer.valueOf(lockable.hashCode()));
    }

    private synchronized void lockPool(Lockable inUseLockable) {
        Log.v(TAG, "call lock pool with lockable ");
        for (Lockable lockable : this.mListeningLockable.values()) {
            if (lockable != inUseLockable) {
                lockable.lockSelf();
            }
        }
    }

    private synchronized void lockPool(int hash) {
        Log.v(TAG, "call lock pool with hash ");
        for (Lockable lockable : this.mListeningLockable.values()) {
            if (lockable.hashCode() != hash) {
                lockable.lockSelf();
            }
        }
    }

    private synchronized void unlockPool() {
        Log.v(TAG, "call unlock pool ");
        for (Lockable lockable : this.mListeningLockable.values()) {
            lockable.unLockSelf();
        }
    }

    public void showTutorial(AbstractTutorialOverlay tutorial, LayoutInflater inflater) {
    }

    public void showFilmstrip() {
    }

    public void hideFilmstrip() {
    }

    public void addManualModeListener(ManualGroup manualGroup) {
        addShutterListener(manualGroup);
        this.mOnModeOptionsVisibilityChangedListener = manualGroup;
    }

    public void removeManualModeListener(ManualGroup manualGroup) {
        removeShutterListener(manualGroup);
        this.mOnModeOptionsVisibilityChangedListener = null;
    }

    public int getNavigationHeight() {
        Resources resources = this.mController.getAndroidContext().getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public CaptureLayoutHelper getCaptureLayoutHelper() {
        return this.mCaptureLayoutHelper;
    }

    public void lockButtonOrientation() {
        this.mController.lockRotatableOrientation(this.mShutterButton.hashCode());
        this.mController.lockRotatableOrientation(this.mCaptureButton.hashCode());
        this.mController.lockRotatableOrientation(this.mSegmentRemoveButton.hashCode());
        this.mController.lockRotatableOrientation(this.mRemixButton.hashCode());
    }

    public void unlockButtonOrientation() {
        this.mController.unlockRotatableOrientation(this.mShutterButton.hashCode());
        this.mController.unlockRotatableOrientation(this.mCaptureButton.hashCode());
        this.mController.unlockRotatableOrientation(this.mSegmentRemoveButton.hashCode());
        this.mController.unlockRotatableOrientation(this.mRemixButton.hashCode());
    }

    public void showRotateButton() {
        if (this.mBottomBar != null) {
            this.mBottomBar.showRotateButton();
        }
    }

    public void hideRotateButton() {
        if (this.mBottomBar != null) {
            this.mBottomBar.hideRotateButton();
        }
    }

    public void setCameraSurfaceDevice(CameraProxy proxy) {
        this.mGlSurfaceView.setCameraDevice(proxy);
    }

    public ProArcSeekBarUI getProArcSeekBarUI() {
        if (this.mProArcSeekBarUI == null) {
            this.mController.getButtonManager().initMOBExtraLayout();
            this.mProArcSeekBarUI = new ProArcSeekBarUI(this.mController, this.mAppRootView);
            this.mProArcSeekBarUI.hideAllArcSeekBar(null, false);
        }
        return this.mProArcSeekBarUI;
    }

    public void slipUpShutterButton() {
        int proModeID = this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" [CameraAppUI] slipUpShutterButton lastModeID = ");
        stringBuilder.append(this.lastModeID);
        Log.d(tag, stringBuilder.toString());
        if (this.mController.getCurrentModuleIndex() != proModeID) {
            int currentModuleIndex = this.mController.getCurrentModuleIndex();
            this.lastModeID = currentModuleIndex;
            this.lastModeID = currentModuleIndex;
            if (!(this.lastModeID == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_square) || this.lastModeID == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pano) || this.lastModeID == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_time_lapse))) {
                onModeIdChanging();
                this.mModeStripView.setCurrentModeWithModeIndex(proModeID);
            }
            setNeedShowArc(true);
            return;
        }
        showArcSeekBar();
    }

    public void slipDownShutterButton() {
        Log.d(TAG, "slipDownShutterButton");
        if (this.mController.getCurrentModuleIndex() == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro)) {
            if (this.isCommShow) {
                updateComBarState(-1, false);
                this.isCommShow = false;
            } else if (isArcSeekBarShow()) {
                hideArcSeekBar();
            } else {
                if (this.isNeedShowArc) {
                    setNeedShowArc(false);
                }
                removeShowSeekBarMsg();
                if (this.lastModeID != -1) {
                    if (!(this.lastModeID == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_square) || this.lastModeID == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pano) || this.lastModeID == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_time_lapse))) {
                        onModeIdChanging();
                        this.mModeStripView.setCurrentModeWithModeIndex(this.lastModeID);
                    }
                    this.lastModeID = -1;
                }
            }
        }
    }

    public void resetRings() {
        if (getCurrentModeIndex() != this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro)) {
            return;
        }
        if (isArcSeekBarShow()) {
            setArcSeekBarShow(false);
            if (this.mProArcSeekBarUI != null) {
                this.mProArcSeekBarUI.hideAllArcSeekBar(null, false);
            }
            showArcSeekBar();
        } else if (this.isCommShow) {
            updateComBarState(-1, false);
            this.isCommShow = false;
        }
    }

    public void takePicture(Runnable handler) {
        this.mGlSurfaceView.takePicture(handler);
    }

    private int getDelayedTime() {
        int delayedTime = MediaPlayer2.MEDIA_INFO_TIMED_TEXT_ERROR;
        if (getCurrentModeIndex() == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_livebokeh) && this.mIsFirstLiveBokehModule) {
            delayedTime = 1800;
            this.mIsFirstLiveBokehModule = false;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("delayedTime = ");
        stringBuilder.append(delayedTime);
        Log.v(tag, stringBuilder.toString());
        return delayedTime;
    }

    public void showOrHideGLSurface(boolean surface) {
        sLastShowOrHideGLSurfaceTime = System.currentTimeMillis();
        if (!surface) {
            if (this.mIsFirstRun) {
                this.mGlSurfaceView.setVisibility(8);
                this.mTextureView.setVisibility(0);
            } else if (this.mGlSurfaceView.getVisibility() == 0) {
                freezeScreenUntilPreviewReady(true, new OnTransAnimationListener[0]);
                this.mTextureView.postDelayed(new Runnable() {
                    public void run() {
                        CameraAppUI.this.mGlSurfaceView.setVisibility(8);
                    }
                }, 400);
                this.mTextureView.postDelayed(new Runnable() {
                    public void run() {
                        CameraAppUI.this.mTextureView.setVisibility(0);
                    }
                }, 500);
            }
            this.mIsFirstRun = false;
        } else if (this.mGlSurfaceView.getVisibility() != 0) {
            ModeTransitionView.sDelayedTime = getDelayedTime();
            freezeScreenUntilPreviewReady(true, new OnTransAnimationListener[0]);
            if (this.mController instanceof CameraActivity) {
                if (this.mController.getCurrentModule().isSupportBeauty()) {
                    this.mTextureView.postDelayed(new Runnable() {
                        public void run() {
                            if (((CameraActivity) CameraAppUI.this.mController).getCurrentModule().isSupportBeauty()) {
                                CameraAppUI.this.mTextureView.setVisibility(8);
                                CameraAppUI.this.mGlSurfaceView.setVisibility(0);
                            } else {
                                CameraAppUI.this.mGlSurfaceView.setVisibility(8);
                                CameraAppUI.this.mTextureView.setVisibility(0);
                            }
                            Log.v(CameraAppUI.TAG, "showOrHideGLSurface set TextureView gone delay 1000ms");
                        }
                    }, 1000);
                    this.mGlSurfaceView.setVisibility(0);
                } else if (this.mTextureView.getVisibility() != 0) {
                    this.mTextureView.setVisibility(0);
                    this.mGlSurfaceView.setVisibility(8);
                }
            }
        } else if (this.mIsForceGlSurfaceChanged) {
            forceGlSurfaceChanged();
        } else {
            this.mGlSurfaceView.requestRender();
        }
        this.mIsForceGlSurfaceChanged = false;
    }

    public void freezeGlSurface() {
        this.mIsForceGlSurfaceChanged = true;
    }

    public void forceGlSurfaceChanged() {
        Log.d(TAG, "ModeTransitionView.sState function  ");
        if (ModeTransitionView.sState == 0) {
            this.mModeCoverState = 0;
            ModeTransitionView.sDelayedTime = MediaPlayer2.MEDIA_INFO_TIMED_TEXT_ERROR;
            freezeScreenUntilPreviewReady(true, new OnTransAnimationListener[0]);
            this.mGlSurfaceView.postDelayed(new Runnable() {
                public void run() {
                    CameraAppUI.this.mGlSurfaceView.surfaceChanged(null, 0, CameraAppUI.this.mGlSurfaceView.getWidth(), CameraAppUI.this.mGlSurfaceView.getHeight());
                }
            }, 10);
            return;
        }
        int time;
        int last = ((StereoModeStripView) this.mModeStripView).getLastModuleId();
        int curr = ((StereoModeStripView) this.mModeStripView).getFutureModuel();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("last ");
        stringBuilder.append(last);
        stringBuilder.append("   curr ");
        stringBuilder.append(curr);
        Log.d(tag, stringBuilder.toString());
        if (last == 0 && curr == 0) {
            ModeTransitionView.sDelayedTime = 200;
            time = 0;
        } else if (curr == 2 && last == 2) {
            ModeTransitionView.sDelayedTime = 300;
            time = 0;
        } else if ((curr == 2 && last == 4) || (last == 2 && curr == 4)) {
            ModeTransitionView.sDelayedTime = 200;
            time = 0;
        } else {
            ModeTransitionView.sDelayedTime = 800;
            time = 10;
        }
        this.mGlSurfaceView.postDelayed(new Runnable() {
            public void run() {
                CameraAppUI.this.mGlSurfaceView.surfaceChanged(null, 0, CameraAppUI.this.mGlSurfaceView.getWidth(), CameraAppUI.this.mGlSurfaceView.getHeight());
            }
        }, (long) time);
    }

    public onPreviewBytes getPreviewCallback() {
        return this.mGlSurfaceView.getPreviewCallBack();
    }

    public synchronized void showArcSeekBar() {
        if (this.mProArcSeekBarUI == null) {
            this.mController.getButtonManager().initMOBExtraLayout();
            this.mProArcSeekBarUI = new ProArcSeekBarUI(this.mController, this.mAppRootView);
        }
        this.mProArcSeekBarUI.hideAllArcSeekBar(null, false);
        if (isArcSeekBarShow()) {
            this.mProArcSeekBarUI.resetSeekBar();
        }
        if (this.isCommShow) {
            updateComBarState(-1, false);
            this.isCommShow = false;
        }
        this.mProArcSeekBarUI.showAllArcSeekBar(null, true);
        setArcSeekBarShow(true);
        this.mModeStripView.pause();
        setModeStripViewVisibility(false);
        this.mController.getButtonManager().setMoreEnterToggleButton(8);
        this.mController.getButtonManager().hideMoreOptionsWrapper();
    }

    private void hideArcSeekBar() {
        if (this.mProArcSeekBarUI != null) {
            this.mProArcSeekBarUI.hideAllArcSeekBar(null, true);
        }
        setArcSeekBarShow(false);
        this.mModeStripView.resume();
        setModeStripViewVisibility(true);
        this.mController.getButtonManager().setMoreEnterToggleButton(0);
    }

    public void updateComBarState(int arcMode, boolean isShow) {
        if (isShow) {
            this.mController.getButtonManager().setMoreEnterToggleButton(8);
            this.mController.getButtonManager().hideMoreOptionsWrapper();
            this.mModeStripView.pause();
            setModeStripViewVisibility(false);
            setStereoModeStripViewSlideable(false);
        } else {
            this.mModeStripView.resume();
            setModeStripViewVisibility(true);
            this.mController.getButtonManager().setMoreEnterToggleButton(0);
            setStereoModeStripViewSlideable(true);
        }
        this.isCommShow = isShow;
        if (isShow) {
            getProArcSeekBarUI();
        }
        if (this.mProArcSeekBarUI != null) {
            this.mProArcSeekBarUI.updateComBar(arcMode, isShow);
        }
    }

    public void setNeedShowArc(boolean isNeedShowArc) {
        this.isNeedShowArc = isNeedShowArc;
    }

    public void removeShowSeekBarMsg() {
        if (this.proHandler != null) {
            this.proHandler.removeMessages(1);
        }
    }

    private void setArcSeekBarShow(boolean isShow) {
        this.isArcSeekBarShow = isShow;
    }

    private boolean isArcSeekBarShow() {
        return this.isArcSeekBarShow;
    }

    public void switchShutterSlidingAbility(boolean isSliding) {
        if (this.mShutterButton != null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[CameraAppUI] switchShutterSlidingAbility isSliding = ");
            stringBuilder.append(isSliding);
            Log.d(tag, stringBuilder.toString());
            this.mShutterButton.switchSlidingAbility(isSliding);
        }
    }

    public void onCancelSelectionMenu() {
        this.mController.getButtonManager().toggleSelectionMenu();
        this.mController.getLockEventListener().onIdle();
    }

    public Surface getSurface() {
        return this.mGlSurfaceView.getSurface();
    }

    public void setSurfacePath(String path) {
        this.mGlSurfaceView.path = path;
    }

    public void setSurfaceWidth(int width) {
        this.mGlSurfaceView.mCameraPreviewWidth = width;
        this.mGlSurfaceView.setWidth(width);
    }

    public void setSurfaceHeight(int height) {
        this.mGlSurfaceView.mCameraPreviewHeight = height;
        this.mGlSurfaceView.setHeight(height);
    }

    public void setOrientation(int i) {
        this.mGlSurfaceView.setOrientation(i);
    }

    public void startVideoRecorder() {
        if (this.mGlSurfaceView != null) {
            this.mGlSurfaceView.startVideoRecorder();
        }
    }

    public void stopVideoRecorder() {
        if (this.mGlSurfaceView != null) {
            this.mGlSurfaceView.stopVideoRecorder();
        }
    }

    public void initVideopath(String path) {
        if (this.mGlSurfaceView != null) {
            this.mGlSurfaceView.initVideoPath(path);
        }
    }

    public void setBeautyEnable(boolean enable) {
        this.mBeautyEnable = enable;
    }

    public void setEffectEnable(boolean enable) {
        this.mEffectEnable = false;
    }

    public boolean getBeautyEnable() {
        return this.mBeautyEnable;
    }

    public boolean getEffectEnable() {
        return this.mEffectEnable;
    }

    public float getBeautySeek() {
        return this.mBeautySeek;
    }

    public void setBeautySeek(float seek) {
        this.mBeautySeek = seek;
    }

    public String getCurrSelect() {
        return this.mCurrSelect;
    }

    public int getCurrBeauState() {
        return this.mBeauState;
    }

    public void resetCurrMaskSelect() {
        this.mCurrSelect = "";
        BeaurifyJniSdk.preViewInstance().nativeDisablePackage();
        this.mController.getButtonManager().setAdapterSelectMask("");
    }

    public BottomBar getBottomBar() {
        return this.mBottomBar;
    }

    public void setTextureViewVisible(int visible) {
        if (this.mTextureView != null) {
            this.mTextureView.setVisibility(visible);
        }
    }

    public void playCameraSwitchAnimation(int playTime, final int holdTime) {
        this.mIsCameraSwitchAnimationRunning = true;
        this.mCameraSwitchAnimation.setImageBitmap(this.mCameraModuleScreenShotProvider.getPreviewFrame(0));
        this.mCameraSwitchAnimation.setVisibility(0);
        this.mCameraSwitchAnimationBackgroud.setVisibility(0);
        ObjectAnimator rotationYAnimator = ObjectAnimator.ofFloat(this.mCameraSwitchAnimation, "rotationY", new float[]{0.0f, 180.0f});
        rotationYAnimator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                CameraAppUI.this.mCameraSwitchAnimation.postDelayed(new Runnable() {
                    public void run() {
                        CameraAppUI.this.mCameraSwitchAnimation.setVisibility(8);
                        CameraAppUI.this.mCameraSwitchAnimationBackgroud.setVisibility(8);
                        CameraAppUI.this.mIsCameraSwitchAnimationRunning = false;
                    }
                }, (long) holdTime);
            }

            public void onAnimationCancel(Animator animation) {
                CameraAppUI.this.mCameraSwitchAnimation.setVisibility(8);
                CameraAppUI.this.mCameraSwitchAnimationBackgroud.setVisibility(8);
                CameraAppUI.this.mIsCameraSwitchAnimationRunning = false;
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        rotationYAnimator.setDuration((long) playTime);
        rotationYAnimator.start();
    }

    public void setStereoModeStripViewSlideable(boolean slideable) {
        ((StereoModeStripView) this.mModeStripView).isSlideable(slideable);
    }

    public Bitmap getPreviewBitmap() {
        if (this.mCameraModuleScreenShotProvider != null) {
            return this.mCameraModuleScreenShotProvider.getRawPreviewFrame(0);
        }
        return null;
    }

    public void addPeekThumbLockableToListenerPool() {
        if (this.mController != null && this.mPeekThumb != null) {
            this.mController.addLockableToListenerPool(this.mPeekThumb);
        }
    }

    public void removePeekThumbLockableFromListenerPool() {
        if (this.mController != null && this.mPeekThumb != null) {
            this.mController.removeLockableFromListenerPool(this.mPeekThumb);
            this.mPeekThumb.unLockSelf();
        }
    }
}
