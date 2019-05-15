package com.hmdglobal.app.camera;

import android.content.Context;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.external.ExtendKey;
import com.hmdglobal.app.camera.MasksRecyclerAdapter.onItemClick;
import com.hmdglobal.app.camera.MultiToggleImageButton.OnStateChangeListener;
import com.hmdglobal.app.camera.MultiToggleImageButton.OnTouchListener;
import com.hmdglobal.app.camera.MultiToggleImageButton.OnUnhandledClickListener;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec.ExposureCompensationSetCallback;
import com.hmdglobal.app.camera.app.CameraAppUI.EffectOnClick;
import com.hmdglobal.app.camera.app.CameraController;
import com.hmdglobal.app.camera.beauty.cameragl.CameraSurfaceView;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsManager.OnSettingChangedListener;
import com.hmdglobal.app.camera.ui.BottomBarModeOptionsWrapperExtra;
import com.hmdglobal.app.camera.ui.CameraSelectionMenu;
import com.hmdglobal.app.camera.ui.CustomSeekBar;
import com.hmdglobal.app.camera.ui.EffectsContainerWrapper;
import com.hmdglobal.app.camera.ui.ModeTransitionView.OnTransAnimationListener;
import com.hmdglobal.app.camera.ui.MoreOptionsUI;
import com.hmdglobal.app.camera.ui.MoreOptionsWrapper;
import com.hmdglobal.app.camera.ui.PreviewOverlay;
import com.hmdglobal.app.camera.ui.RadioOptions;
import com.hmdglobal.app.camera.ui.RadioOptions.OnOptionClickListener;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;
import com.hmdglobal.app.camera.ui.RotatableButton;
import com.hmdglobal.app.camera.ui.RotateLayout;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.FileUtil;
import com.hmdglobal.app.camera.util.LockUtils;
import com.hmdglobal.app.camera.util.LockUtils.Lock;
import com.hmdglobal.app.camera.util.LockUtils.LockType;
import com.hmdglobal.app.camera.widget.ModeOptions;
import com.hmdglobal.app.camera.widget.ModeOptionsOverlay;
import java.util.HashMap;
import java.util.Map;

public class ButtonManager implements OnSettingChangedListener {
    public static final int BUTTON_CAMERA = 3;
    public static final int BUTTON_CANCEL = 6;
    public static final int BUTTON_COUNTDOWN = 12;
    public static final int BUTTON_DONE = 7;
    public static final int BUTTON_EXPOSURE_COMPENSATION = 11;
    public static final int BUTTON_FLASH = 0;
    public static final int BUTTON_GRID_LINES = 10;
    public static final int BUTTON_HDR = 5;
    public static final int BUTTON_HDR_PLUS = 4;
    public static final int BUTTON_HDR_PLUS_FLASH = 2;
    public static final int BUTTON_LIVE = 17;
    public static final int BUTTON_LOWLIGHT = 13;
    public static final int BUTTON_MOTION = 18;
    public static final int BUTTON_RETAKE = 8;
    public static final int BUTTON_REVIEW = 9;
    public static final int BUTTON_SETTING = 15;
    public static final int BUTTON_SWITCH = 16;
    public static final int BUTTON_TORCH = 1;
    public static final int BUTTON_WRAPPER = 14;
    private static final int EFFECT_MODULE_BEAUTY = 3;
    public static final int EFFECT_MODULE_BOKEN = 2;
    private static final int EFFECT_MODULE_MASK = 1;
    public static final int MASK_NAVIGATOR_HEIGHT = 100;
    public static final int MASK_TOTAL_HEIGHT = 220;
    private static final int MIN_CLICK_DELAY_TIME = 700;
    private static final int MIN_CLICK_DELAY_TIME_BOKEH = 1000;
    public static final int OFF = 0;
    public static final int ON = 1;
    private static final String PHOTO_MODULE_STRING_ID = "ButtonManager";
    private static final Tag TAG = new Tag(PHOTO_MODULE_STRING_ID);
    private static int sGcamIndex;
    private long lastClickTime = -1;
    private final AppController mAppController;
    private LinearLayout mBeautifulLayout;
    private BeautyStateListener mBeautyCallBack;
    public boolean mBeautyEnable = true;
    private int mBeautySeek = 0;
    private CustomSeekBar mBeautySeekbar;
    private LinearLayout mBolkenLayout;
    private CustomSeekBar mBolkenSeekbar;
    private MultiToggleImageButton mButtonBeauty;
    private RotatableButton mButtonBeauty2;
    private MultiToggleImageButton mButtonBolken;
    private MultiToggleImageButton mButtonCamera;
    private ImageButton mButtonCancel;
    private MultiToggleImageButton mButtonCountdown;
    private ImageButton mButtonDone;
    private MultiToggleImageButton mButtonDualSight;
    private MultiToggleImageButton mButtonEffect;
    private RotateLayout mButtonEffect2;
    private ImageButton mButtonExposureCompensation;
    private MultiToggleImageButton mButtonFlash;
    private RotatableButton mButtonGoogleLens;
    private MultiToggleImageButton mButtonGridlines;
    private MultiToggleImageButton mButtonHdr;
    private int mButtonId;
    private MultiToggleImageButton mButtonLowlight;
    private MultiToggleImageButton mButtonMotion;
    private ImageButton mButtonRetake;
    private RotatableButton mButtonSetting;
    private MultiToggleImageButton mButtonliveBroadcast;
    private CameraSelectionMenu mCameraSelectionMenu;
    private CameraSurfaceView mCameraSurfaceView;
    private MasksRecyclerAdapter mEasyRecyclerAdapter;
    private EffectOnClick mEffectCallBack;
    private EffectsContainerWrapper mEffectsContainerWrapper;
    private MultiToggleImageButton mEffectsEnterToggleButton;
    private MultiToggleImageButton mExitMoreButton;
    private ImageButton mExposure0;
    private float mExposureCompensationStep;
    private ImageButton mExposureN1;
    private ImageButton mExposureN2;
    private ImageButton mExposureP1;
    private ImageButton mExposureP2;
    private Integer mFlashState0;
    private Integer mFlashState1;
    private ButtonCallback mHDRCallback;
    private OnStateChangeListener mHDRListener = new OnStateChangeListener() {
        public void stateChanged(View view, int state) {
            ButtonManager.this.mAppController.getLockEventListener().onIdle();
            ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, state);
            if ("on".equals(ButtonManager.this.mSettingsManager.getValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, state))) {
                ButtonManager.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_LOWLIGHT, false);
                ButtonManager.this.mSettingsManager.set(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, ButtonManager.this.mAppController.getAndroidContext().getString(R.string.pref_camera_flashmode_off));
            }
            if (ButtonManager.this.mHDRCallback != null) {
                ButtonManager.this.mHDRCallback.onStateChanged(state);
            }
            if (ButtonManager.this.mHelpTipsManager != null) {
                ButtonManager.this.mHelpTipsManager.notifyEventFinshed();
            }
        }
    };
    private Integer mHdrState;
    private HelpTipsManager mHelpTipsManager;
    private TextView mIndicatorText;
    private boolean mIsHide = true;
    private long mLastOnClickTime;
    private ButtonStatusListener mListener;
    private Map<Integer, Lock> mLockMap = new HashMap();
    private BottomBarModeOptionsWrapperExtra mMOBExtraLayout;
    private FrameLayout mMaskLayout;
    private RecyclerView mMaskRecycler;
    private int mMaxExposureCompensation;
    private int mMinExposureCompensation;
    private ModeOptions mModeOptions;
    private View mModeOptionsButtons;
    private RadioOptions mModeOptionsExposure;
    private ModeOptionsOverlay mModeOptionsOverlay;
    private RadioOptions mModeOptionsPano;
    private MultiToggleImageButton mMoreEnterToggleButton;
    private MoreOptionsWrapper mMoreOptionsWrapper;
    private Integer mMotionState;
    private final SettingsManager mSettingsManager;
    private OnTransAnimationListener mTransitionView = new OnTransAnimationListener() {
        public void onAnimationDone() {
            ButtonManager.this.mAppController.getLockEventListener().onIdle();
            ButtonManager.this.mAppController.getCameraAppUI().hideImageCover();
            ButtonManager.this.mAppController.getCameraAppUI().setModeCoverState(0);
        }
    };
    private ImageButton mWrapperButton;

    public interface BeautyStateListener {
        void onStateChanged(int i);
    }

    public interface ButtonCallback {
        void onStateChanged(int i);
    }

    public enum ButtonId {
        BUTTON_FLASH,
        BUTTON_TORCH,
        BUTTON_HDR_PLUS_FLASH,
        BUTTON_CAMERA,
        BUTTON_HDR_PLUS,
        BUTTON_HDR,
        BUTTON_CANCEL,
        BUTTON_DONE,
        BUTTON_RETAKE,
        BUTTON_REVIEW,
        BUTTON_GRID_LINES,
        BUTTON_EXPOSURE_COMPENSATION,
        BUTTON_COUNTDOWN,
        BUTTON_LOW_LIGHT,
        BUTTON_WRAPPER,
        BUTTON_SETTING,
        BUTTON_SWITCH,
        BUTTON_LIVE,
        BUTTON_MOTION
    }

    public interface ButtonStatusListener {
        void onButtonEnabledChanged(ButtonManager buttonManager, int i);

        void onButtonVisibilityChanged(ButtonManager buttonManager, int i);
    }

    public interface ExtendButtonCallback extends ButtonCallback {
        void onUnhandledClick();
    }

    public int getLastBeautySeekProgress() {
        return this.mBeautySeek;
    }

    public void setLastBeautySeekProgress(int progress) {
        this.mBeautySeek = progress;
    }

    public synchronized boolean isFastClick() {
        long curClickTime = System.currentTimeMillis();
        int delayTime = 700;
        if (this.mAppController.getCurrentModuleIndex() == 0) {
            delayTime = 1000;
        }
        if (curClickTime - this.lastClickTime <= ((long) delayTime)) {
            this.lastClickTime = curClickTime;
            return true;
        }
        this.lastClickTime = curClickTime;
        return false;
    }

    public ButtonManager(AppController app) {
        this.mAppController = app;
        sGcamIndex = app.getAndroidContext().getResources().getInteger(R.integer.camera_mode_gcam);
        this.mSettingsManager = app.getSettingsManager();
        this.mSettingsManager.addListener(this);
    }

    public void registerOnSharedPreferenceChangeListener() {
        if (this.mSettingsManager != null) {
            this.mSettingsManager.addListener(this);
        }
    }

    private void initAdapter(Context context) {
        if (this.mMaskRecycler != null) {
            this.mMaskRecycler.setVisibility(0);
            return;
        }
        this.mMaskRecycler = new RecyclerView(context);
        this.mMaskRecycler.setLayoutParams(new LayoutParams(-1, -2));
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(0);
        this.mMaskRecycler.setLayoutManager(linearLayoutManager);
        this.mEasyRecyclerAdapter = new MasksRecyclerAdapter(context, FileUtil.mAllModels);
        this.mMaskRecycler.setAdapter(this.mEasyRecyclerAdapter);
        if (this.mMaskLayout != null) {
            this.mMaskLayout.addView(this.mMaskRecycler);
        }
        this.mEasyRecyclerAdapter.setCallback(new onItemClick() {
            public void onClick(int pos, String position) {
                if (ButtonManager.this.mEffectCallBack != null) {
                    ButtonManager.this.mEffectCallBack.onItemClick(pos, position);
                }
            }
        });
        this.mMaskRecycler.requestFocus();
        this.mMaskRecycler.bringToFront();
        this.mMaskRecycler.setClickable(true);
        if (this.mMaskLayout != null) {
            this.mMaskLayout.requestLayout();
        }
    }

    public void setAdapterSelectMask(String path) {
        if (this.mEasyRecyclerAdapter != null) {
            this.mEasyRecyclerAdapter.setSelect(path);
        }
    }

    public void setEffectsCallBack(EffectOnClick callBack) {
        this.mEffectCallBack = callBack;
    }

    public void setBeautyCallBack(BeautyStateListener callBack) {
        this.mBeautyCallBack = callBack;
    }

    public void load(View root) {
        getButtonsReferences(root);
    }

    public void setListener(ButtonStatusListener listener) {
        this.mListener = listener;
    }

    private void getButtonsReferences(View root) {
        this.mButtonSetting = (RotatableButton) root.findViewById(R.id.menu_setting_button);
        this.mButtonMotion = (MultiToggleImageButton) root.findViewById(R.id.motion_toggle_button);
        this.mMoreEnterToggleButton = (MultiToggleImageButton) root.findViewById(R.id.more_enter_toggle_button);
        this.mEffectsEnterToggleButton = (MultiToggleImageButton) root.findViewById(R.id.effects_enter_toggle_button);
        this.mEffectsEnterToggleButton.setState(0);
        this.mExitMoreButton = (MultiToggleImageButton) root.findViewById(R.id.more_fun_close_imgbtn);
        this.mButtonDualSight = (MultiToggleImageButton) root.findViewById(R.id.dual_sight_toggle_button);
        this.mButtonliveBroadcast = (MultiToggleImageButton) root.findViewById(R.id.live_broadcast_button);
        this.mButtonCamera = (MultiToggleImageButton) root.findViewById(R.id.camera_toggle_button);
        this.mButtonFlash = (MultiToggleImageButton) root.findViewById(R.id.flash_toggle_button);
        this.mButtonHdr = (MultiToggleImageButton) root.findViewById(R.id.hdr_plus_toggle_button);
        this.mButtonGridlines = (MultiToggleImageButton) root.findViewById(R.id.grid_lines_toggle_button);
        this.mButtonCancel = (ImageButton) root.findViewById(R.id.cancel_button);
        this.mButtonDone = (ImageButton) root.findViewById(R.id.done_button);
        this.mButtonRetake = (ImageButton) root.findViewById(R.id.retake_button);
        this.mButtonLowlight = (MultiToggleImageButton) root.findViewById(R.id.lowlight_toggle_button);
        this.mWrapperButton = (ImageButton) root.findViewById(R.id.wrapper_button);
        this.mButtonExposureCompensation = (ImageButton) root.findViewById(R.id.exposure_button);
        this.mExposureN2 = (ImageButton) root.findViewById(R.id.exposure_n2);
        this.mExposureN1 = (ImageButton) root.findViewById(R.id.exposure_n1);
        this.mExposure0 = (ImageButton) root.findViewById(R.id.exposure_0);
        this.mExposureP1 = (ImageButton) root.findViewById(R.id.exposure_p1);
        this.mExposureP2 = (ImageButton) root.findViewById(R.id.exposure_p2);
        this.mModeOptionsExposure = (RadioOptions) root.findViewById(R.id.mode_options_exposure);
        this.mModeOptionsPano = (RadioOptions) root.findViewById(R.id.mode_options_pano);
        this.mModeOptionsButtons = root.findViewById(R.id.mode_options_buttons);
        this.mIndicatorText = (TextView) root.findViewById(R.id.indicator_text);
        this.mCameraSurfaceView = (CameraSurfaceView) root.findViewById(R.id.preview_content_gl);
        this.mCameraSelectionMenu = (CameraSelectionMenu) root.findViewById(R.id.mode_options_flash_selection);
        this.mCameraSelectionMenu.setButtonManager(this);
        this.mModeOptionsOverlay = (ModeOptionsOverlay) root.findViewById(R.id.mode_options_overlay);
        this.mModeOptions = (ModeOptions) root.findViewById(R.id.mode_options);
        this.mButtonCountdown = (MultiToggleImageButton) root.findViewById(R.id.countdown_toggle_button);
        this.mButtonEffect2 = (RotateLayout) root.findViewById(R.id.effect_toggle_button_2);
        this.mButtonBeauty2 = (RotatableButton) root.findViewById(R.id.beauty_toggle_button_2);
        ((PreviewOverlay) root.findViewById(R.id.preview_overlay)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            }
        });
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonMotion, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonDualSight, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonliveBroadcast, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonCountdown, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonSetting, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonCamera, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonFlash, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonHdr, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonGridlines, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonLowlight, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonEffect2, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonBeauty2, true));
        this.mAppController.addLockableToListenerPool(this.mButtonMotion);
        this.mAppController.addLockableToListenerPool(this.mButtonDualSight);
        this.mAppController.addLockableToListenerPool(this.mButtonliveBroadcast);
        this.mAppController.addLockableToListenerPool(this.mButtonSetting);
        this.mAppController.addLockableToListenerPool(this.mButtonCamera);
        this.mAppController.addLockableToListenerPool(this.mButtonFlash);
        this.mAppController.addLockableToListenerPool(this.mButtonHdr);
        this.mAppController.addLockableToListenerPool(this.mButtonGridlines);
        this.mAppController.addLockableToListenerPool(this.mButtonLowlight);
        this.mAppController.addLockableToListenerPool(this.mButtonCountdown);
        this.mAppController.addLockableToListenerPool(this.mMoreEnterToggleButton);
        this.mAppController.addLockableToListenerPool(this.mEffectsEnterToggleButton);
        this.mAppController.addLockableToListenerPool(this.mExitMoreButton);
    }

    public boolean tryToCancelMaskWrapper() {
        if (getEffectsEnterWrapperVisible() != 0) {
            return false;
        }
        if (this.mAppController.getCameraAppUI().getCurrentModeIndex() != 0) {
            hideEffectsContainerWrapper();
            if (this.mAppController.getCameraAppUI().getBeautySeek() == 0.0f) {
                this.mAppController.openOrCloseEffects(-1, -1, this.mAppController.getCurrentModuleIndex());
            } else {
                animationToEffectWrapper();
            }
            return true;
        } else if (this.mAppController.getCameraProvider().getCurrentCameraId() != 1) {
            return false;
        } else {
            hideEffectsContainerWrapper();
            if (this.mAppController.getCameraAppUI().getBeautySeek() == 0.0f) {
                this.mAppController.openOrCloseEffects(-1, -1, this.mAppController.getCurrentModuleIndex());
            }
            return true;
        }
    }

    private void initEffectsRelatedViews(View root) {
        this.mButtonEffect = (MultiToggleImageButton) root.findViewById(R.id.effect_toggle_button);
        this.mBeautifulLayout = (LinearLayout) root.findViewById(R.id.beautiful_layout);
        this.mBeautifulLayout.setVisibility(8);
        this.mBeautySeekbar = (CustomSeekBar) root.findViewById(R.id.seekbarbeauty);
        this.mButtonBeauty = (MultiToggleImageButton) root.findViewById(R.id.beauty_toggle_button);
        this.mBolkenLayout = (LinearLayout) root.findViewById(R.id.live_bolken_layout);
        this.mBolkenSeekbar = (CustomSeekBar) root.findViewById(R.id.seekbar_bolken);
        this.mBolkenSeekbar.setMaxProgress(9);
        this.mButtonBolken = (MultiToggleImageButton) root.findViewById(R.id.live_bolken_toggle_state_button);
        this.mMaskLayout = (FrameLayout) root.findViewById(R.id.mask_layout);
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonEffect, true));
        this.mAppController.addRotatableToListenerPool(new RotateEntity(this.mButtonBeauty, true));
        this.mAppController.addLockableToListenerPool(this.mButtonEffect);
        this.mAppController.addLockableToListenerPool(this.mButtonBeauty);
        if (this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID) == 1) {
            setLiveBokehLevelSeeker(CameraAgent.mLiveBolkenFrontLevel);
        } else {
            setLiveBokehLevelSeeker(CameraAgent.mLiveBolkenRearLevel);
        }
        initAccordingToSpecs();
    }

    private void initAccordingToSpecs() {
        BottomBarUISpec bottomBarSpec = this.mAppController.getCurrentModuleController().getBottomBarSpec();
        if (bottomBarSpec != null) {
            if ((PhotoModule.class.getSimpleName().equals(bottomBarSpec.moduleName) || SquareModule.class.getSimpleName().equals(bottomBarSpec.moduleName) || LiveBokehModule.class.getSimpleName().equals(bottomBarSpec.moduleName)) && (bottomBarSpec.showBeautyButton || bottomBarSpec.showEffectButton || !bottomBarSpec.hideBolken)) {
                if (bottomBarSpec.showEffectButton) {
                    initializeEffectButton();
                } else {
                    hideEffectLayout();
                    hideEffectButton(this.mAppController.getCurrentModuleIndex());
                }
                if (bottomBarSpec.showBeautyButton) {
                    initializeBeautyButton(bottomBarSpec.seekbarChangeListener);
                } else {
                    hideBeautyButton();
                    hideBeautyLayout();
                }
                if (bottomBarSpec.hideBolken) {
                    hideLiveBolkenButton();
                    hideLiveBolkenLayout();
                } else {
                    initLiveBolkenLayout(bottomBarSpec.seekbarBolkenListener);
                    showLiveBolkenButton();
                    if (this.mAppController.getCameraProvider().getCurrentCameraId() == 0 || this.mAppController.getCameraProvider().getCurrentCameraId() == 3) {
                        showLiveBolkenLayout();
                    }
                }
            }
        }
    }

    public void onSettingChanged(SettingsManager settingsManager, String key) {
        MultiToggleImageButton button = null;
        int index = 0;
        if (key.equals(Keys.KEY_FLASH_MODE)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            button = getButtonOrError(0);
        } else if (key.equals(Keys.KEY_VIDEOCAMERA_FLASH_MODE)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE);
            button = getButtonOrError(1);
        } else if (key.equals(Keys.KEY_HDR_PLUS_FLASH_MODE)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getModuleScope(), Keys.KEY_HDR_PLUS_FLASH_MODE);
            button = getButtonOrError(2);
        } else if (key.equals(Keys.KEY_CAMERA_ID)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID);
            button = getButtonOrError(3);
        } else if (key.equals(Keys.KEY_CAMERA_HDR_PLUS)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS);
            button = getButtonOrError(4);
        } else if (key.equals(Keys.KEY_CAMERA_HDR)) {
            ModuleController mc = this.mAppController.getCurrentModuleController();
            if (!(mc instanceof PhotoModule) || !((PhotoModule) mc).isInBurstshot()) {
                index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR);
                button = getButtonOrError(5);
            } else {
                return;
            }
        } else if (key.equals(Keys.KEY_CAMERA_GRID_LINES)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_GRID_LINES);
            button = getButtonOrError(10);
        } else if (key.equals(Keys.KEY_CAMERA_PANO_ORIENTATION)) {
            updatePanoButtons();
        } else if (key.equals(Keys.KEY_EXPOSURE)) {
            updateExposureButtons();
        } else if (key.equals(Keys.KEY_COUNTDOWN_DURATION)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_COUNTDOWN_DURATION);
            button = getButtonOrError(12);
        } else if (key.equals(Keys.KEY_CAMERA_LOWLIGHT)) {
            index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_LOWLIGHT);
            button = getButtonOrError(13);
        }
        if (!(button == null || button.getState() == index)) {
            button.setState(Math.max(index, 0), false);
        }
    }

    public MultiToggleImageButton getButtonOrError(int buttonId) {
        switch (buttonId) {
            case 0:
                if (this.mButtonFlash != null) {
                    return this.mButtonFlash;
                }
                throw new IllegalStateException("Flash button could not be found.");
            case 1:
                if (this.mButtonFlash != null) {
                    return this.mButtonFlash;
                }
                throw new IllegalStateException("Torch button could not be found.");
            case 2:
                if (this.mButtonFlash != null) {
                    return this.mButtonFlash;
                }
                throw new IllegalStateException("Hdr plus torch button could not be found.");
            case 3:
                if (this.mButtonCamera != null) {
                    return this.mButtonCamera;
                }
                throw new IllegalStateException("Camera button could not be found.");
            case 4:
                if (this.mButtonHdr != null) {
                    return this.mButtonHdr;
                }
                throw new IllegalStateException("Hdr plus button could not be found.");
            case 5:
                if (this.mButtonHdr != null) {
                    return this.mButtonHdr;
                }
                throw new IllegalStateException("Hdr button could not be found.");
            case 10:
                if (this.mButtonGridlines != null) {
                    return this.mButtonGridlines;
                }
                throw new IllegalStateException("Grid lines button could not be found.");
            case 12:
                if (this.mButtonCountdown != null) {
                    return this.mButtonCountdown;
                }
                throw new IllegalStateException("Countdown button could not be found.");
            case 13:
                if (this.mButtonLowlight != null) {
                    return this.mButtonLowlight;
                }
                throw new IllegalStateException("Hdr button could not be found.");
            case 16:
                if (this.mButtonCamera != null) {
                    return this.mButtonCamera;
                }
                throw new IllegalStateException("Camera button could not be found.");
            case 17:
                if (this.mButtonliveBroadcast != null) {
                    return this.mButtonliveBroadcast;
                }
                throw new IllegalStateException("Countdown button could not be found.");
            case 18:
                if (this.mButtonMotion != null) {
                    return this.mButtonMotion;
                }
                throw new IllegalStateException("Countdown button could not be found.");
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("button not known by id=");
                stringBuilder.append(buttonId);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private ImageButton getImageButtonOrError(int buttonId) {
        if (buttonId != 11) {
            if (buttonId != 14) {
                switch (buttonId) {
                    case 6:
                        if (this.mButtonCancel != null) {
                            return this.mButtonCancel;
                        }
                        throw new IllegalStateException("Cancel button could not be found.");
                    case 7:
                        if (this.mButtonDone != null) {
                            return this.mButtonDone;
                        }
                        throw new IllegalStateException("Done button could not be found.");
                    case 8:
                        if (this.mButtonRetake != null) {
                            return this.mButtonRetake;
                        }
                        throw new IllegalStateException("Retake button could not be found.");
                    case 9:
                        if (this.mButtonRetake != null) {
                            return this.mButtonRetake;
                        }
                        throw new IllegalStateException("Review button could not be found.");
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("button not known by id=");
                        stringBuilder.append(buttonId);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            } else if (this.mWrapperButton != null) {
                return this.mWrapperButton;
            } else {
                throw new IllegalStateException("Wrapper button could not be found.");
            }
        } else if (this.mButtonExposureCompensation != null) {
            return this.mButtonExposureCompensation;
        } else {
            throw new IllegalStateException("Exposure Compensation button could not be found.");
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0024, code skipped:
            if (r7.mButtonMotion != null) goto L_0x002e;
     */
    /* JADX WARNING: Missing block: B:12:0x002d, code skipped:
            throw new java.lang.IllegalStateException("Countdown button could not be found.");
     */
    /* JADX WARNING: Missing block: B:13:0x002e, code skipped:
            initializeMotionButton();
     */
    /* JADX WARNING: Missing block: B:35:0x00c9, code skipped:
            r3 = com.hmdglobal.app.camera.beauty.util.SharedUtil.getIntValueByKey("currentBatteryStatus").intValue();
     */
    /* JADX WARNING: Missing block: B:36:0x00d3, code skipped:
            if (r8 == 0) goto L_0x00db;
     */
    /* JADX WARNING: Missing block: B:37:0x00d5, code skipped:
            if (r8 == 1) goto L_0x00db;
     */
    /* JADX WARNING: Missing block: B:38:0x00d7, code skipped:
            enableButton(r8);
     */
    /* JADX WARNING: Missing block: B:39:0x00db, code skipped:
            if (r3 != 0) goto L_?;
     */
    /* JADX WARNING: Missing block: B:40:0x00dd, code skipped:
            enableButton(0);
            enableButton(1);
     */
    /* JADX WARNING: Missing block: B:41:0x00e4, code skipped:
            r2 = new java.lang.StringBuilder();
            r2.append("button not known by id=");
            r2.append(r8);
     */
    /* JADX WARNING: Missing block: B:42:0x00fa, code skipped:
            throw new java.lang.IllegalArgumentException(r2.toString());
     */
    /* JADX WARNING: Missing block: B:43:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:44:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            return;
     */
    public void initializeButton(int r8, com.hmdglobal.app.camera.ButtonManager.ButtonCallback r9) {
        /*
        r7 = this;
        r0 = r7.getButtonOrError(r8);
        r1 = 1;
        r2 = 0;
        if (r8 == 0) goto L_0x000d;
    L_0x0008:
        if (r8 == r1) goto L_0x000d;
    L_0x000a:
        r0.setVisibility(r2);
    L_0x000d:
        r3 = 2130837513; // 0x7f020009 float:1.7279982E38 double:1.052773612E-314;
        r4 = 2130837521; // 0x7f020011 float:1.7279998E38 double:1.052773616E-314;
        r5 = 2130837522; // 0x7f020012 float:1.728E38 double:1.0527736165E-314;
        switch(r8) {
            case 0: goto L_0x00c5;
            case 1: goto L_0x00be;
            case 2: goto L_0x00ba;
            case 3: goto L_0x0098;
            case 4: goto L_0x0091;
            case 5: goto L_0x008a;
            case 6: goto L_0x0019;
            case 7: goto L_0x0019;
            case 8: goto L_0x0019;
            case 9: goto L_0x0019;
            case 10: goto L_0x0083;
            case 11: goto L_0x0019;
            case 12: goto L_0x0066;
            case 13: goto L_0x005f;
            case 14: goto L_0x0019;
            case 15: goto L_0x0019;
            case 16: goto L_0x003b;
            case 17: goto L_0x001b;
            case 18: goto L_0x0022;
            default: goto L_0x0019;
        };
    L_0x0019:
        goto L_0x00e4;
    L_0x001b:
        r1 = r7.mButtonliveBroadcast;
        if (r1 == 0) goto L_0x0033;
    L_0x001f:
        r7.initializeLivebroadcastButton();
    L_0x0022:
        r1 = r7.mButtonMotion;
        if (r1 != 0) goto L_0x002e;
    L_0x0026:
        r1 = new java.lang.IllegalStateException;
        r2 = "Countdown button could not be found.";
        r1.<init>(r2);
        throw r1;
    L_0x002e:
        r7.initializeMotionButton();
        goto L_0x00e4;
    L_0x0033:
        r1 = new java.lang.IllegalStateException;
        r2 = "Countdown button could not be found.";
        r1.<init>(r2);
        throw r1;
    L_0x003b:
        r3 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r6 = "def_camera_vdf_switch_camera_icons_customize";
        r3 = r3.getBoolean(r6, r2);
        if (r3 == 0) goto L_0x0053;
    L_0x0047:
        r3 = TAG;
        r4 = "switch 3";
        com.hmdglobal.app.camera.debug.Log.d(r3, r4);
        r7.initializeSwitchButton(r0, r9, r5);
        goto L_0x00c9;
    L_0x0053:
        r3 = TAG;
        r5 = "switch 4";
        com.hmdglobal.app.camera.debug.Log.d(r3, r5);
        r7.initializeSwitchButton(r0, r9, r4);
        goto L_0x00c9;
    L_0x005f:
        r3 = 2130837565; // 0x7f02003d float:1.7280088E38 double:1.0527736377E-314;
        r7.initializeLowlightButton(r0, r9, r3);
        goto L_0x00c9;
    L_0x0066:
        r3 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r4 = "def_camera_vdf_count_timer";
        r3 = r3.getBoolean(r4, r2);
        if (r3 == 0) goto L_0x007c;
    L_0x0072:
        r3 = 2130837643; // 0x7f02008b float:1.7280246E38 double:1.0527736763E-314;
        r4 = 2130837642; // 0x7f02008a float:1.7280244E38 double:1.052773676E-314;
        r7.initializeCountdownButton(r0, r9, r3, r4);
        goto L_0x00c9;
    L_0x007c:
        r3 = 2130837547; // 0x7f02002b float:1.7280051E38 double:1.052773629E-314;
        r7.initializeCountdownButton(r0, r9, r3);
        goto L_0x00c9;
    L_0x0083:
        r3 = 2130837555; // 0x7f020033 float:1.7280067E38 double:1.052773633E-314;
        r7.initializeGridLinesButton(r0, r9, r3);
        goto L_0x00c9;
    L_0x008a:
        r3 = 2130837558; // 0x7f020036 float:1.7280073E38 double:1.0527736343E-314;
        r7.initializeHdrButton(r0, r9, r3);
        goto L_0x00c9;
    L_0x0091:
        r3 = 2130837613; // 0x7f02006d float:1.7280185E38 double:1.0527736614E-314;
        r7.initializeHdrPlusButton(r0, r9, r3);
        goto L_0x00c9;
    L_0x0098:
        r3 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r6 = "def_camera_vdf_switch_camera_icons_customize";
        r3 = r3.getBoolean(r6, r2);
        if (r3 == 0) goto L_0x00af;
    L_0x00a4:
        r3 = TAG;
        r4 = "switch 1";
        com.hmdglobal.app.camera.debug.Log.d(r3, r4);
        r7.initializeCameraButton(r0, r9, r5);
        goto L_0x00c9;
    L_0x00af:
        r3 = TAG;
        r5 = "switch 2";
        com.hmdglobal.app.camera.debug.Log.d(r3, r5);
        r7.initializeCameraButton(r0, r9, r4);
        goto L_0x00c9;
    L_0x00ba:
        r7.initializeHdrPlusFlashButton(r0, r9, r3);
        goto L_0x00c9;
    L_0x00be:
        r3 = 2130837647; // 0x7f02008f float:1.7280254E38 double:1.0527736782E-314;
        r7.initializeTorchButton(r0, r9, r3);
        goto L_0x00c9;
    L_0x00c5:
        r7.initializeFlashButton(r0, r9, r3);
    L_0x00c9:
        r3 = "currentBatteryStatus";
        r3 = com.hmdglobal.app.camera.beauty.util.SharedUtil.getIntValueByKey(r3);
        r3 = r3.intValue();
        if (r8 == 0) goto L_0x00db;
    L_0x00d5:
        if (r8 == r1) goto L_0x00db;
    L_0x00d7:
        r7.enableButton(r8);
        goto L_0x00e3;
    L_0x00db:
        if (r3 != 0) goto L_0x00e3;
    L_0x00dd:
        r7.enableButton(r2);
        r7.enableButton(r1);
    L_0x00e3:
        return;
    L_0x00e4:
        r1 = new java.lang.IllegalArgumentException;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "button not known by id=";
        r2.append(r3);
        r2.append(r8);
        r2 = r2.toString();
        r1.<init>(r2);
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ButtonManager.initializeButton(int, com.hmdglobal.app.camera.ButtonManager$ButtonCallback):void");
    }

    public void showEffect2Button() {
        if (this.mButtonEffect2 != null) {
            this.mButtonEffect2.setVisibility(0);
            this.mButtonEffect2.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (ButtonManager.this.mEffectsContainerWrapper.getVisibility() == 8) {
                        ButtonManager.this.hideMoreOptionsWrapper();
                        boolean isGlMode = ButtonManager.this.mAppController.getCameraAppUI().getBeautyEnable() || ButtonManager.this.mAppController.getCameraAppUI().getEffectEnable();
                        ButtonManager.this.hideLiveBolkenLayout();
                        ButtonManager.this.hideLiveBolkenButton();
                        if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 4 || ButtonManager.this.mAppController.getCurrentModuleIndex() == 15) {
                            if (isGlMode) {
                                if (ButtonManager.this.mBeautifulLayout.getVisibility() == 0 && ButtonManager.this.mMaskLayout.getVisibility() == 0) {
                                    ButtonManager.this.hideEffectLayout();
                                } else if (ButtonManager.this.mAppController.getCameraAppUI().getBeautyEnable()) {
                                    ButtonManager.this.hideEffectLayout();
                                    ButtonManager.this.showBeautyLayout();
                                    ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                } else {
                                    ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                    ButtonManager.this.showEffectLayout();
                                    ButtonManager.this.hideBeautyLayout();
                                }
                                ButtonManager.this.showEffectsContainerWrapper();
                                ButtonManager.this.animationToEffectWrapper();
                            }
                        } else if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 5 && ButtonManager.this.mAppController.getCameraAppUI().getEffectEnable()) {
                            ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                            ButtonManager.this.showEffectLayout();
                            ButtonManager.this.hideBeautyLayout();
                            ButtonManager.this.showEffectsContainerWrapper();
                            ButtonManager.this.animationToEffectWrapper();
                        }
                    }
                }
            });
        }
    }

    public void hideEffect2Button() {
        if (this.mButtonEffect2 != null) {
            this.mButtonEffect2.setVisibility(8);
        }
    }

    public void showBeauty2Button() {
        if (this.mButtonBeauty2 != null) {
            this.mButtonBeauty2.setVisibility(0);
            this.mButtonBeauty2.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (ButtonManager.this.mEffectsContainerWrapper.getVisibility() == 8) {
                        ButtonManager.this.hideMoreOptionsWrapper();
                        boolean z = true;
                        if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 0) {
                            ButtonManager.this.hideEffectButton(ButtonManager.this.mAppController.getCurrentModuleIndex());
                            ButtonManager.this.hideEffectLayout();
                            ButtonManager.this.showBeautyLayout();
                            ButtonManager.this.hideLiveBolkenLayout();
                            ButtonManager.this.showEffectsContainerWrapper();
                            if (ButtonManager.this.mAppController.getCameraAppUI().getBeautyEnable()) {
                                ButtonManager.this.animationToEffectWrapper();
                            } else {
                                ButtonManager.this.mAppController.openOrCloseEffects(1, R.id.effect_toggle_button, ButtonManager.this.mAppController.getCurrentModuleIndex());
                            }
                            ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                            return;
                        }
                        if (!(ButtonManager.this.mAppController.getCameraAppUI().getBeautyEnable() || ButtonManager.this.mAppController.getCameraAppUI().getEffectEnable())) {
                            z = false;
                        }
                        boolean isGlMode = z;
                        ButtonManager.this.hideLiveBolkenLayout();
                        ButtonManager.this.hideLiveBolkenButton();
                        if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 4 || ButtonManager.this.mAppController.getCurrentModuleIndex() == 15) {
                            if (isGlMode) {
                                if (ButtonManager.this.mBeautifulLayout.getVisibility() == 0 && ButtonManager.this.mMaskLayout.getVisibility() == 0) {
                                    ButtonManager.this.hideEffectLayout();
                                } else if (ButtonManager.this.mAppController.getCameraAppUI().getBeautyEnable()) {
                                    ButtonManager.this.hideEffectLayout();
                                    ButtonManager.this.showBeautyLayout();
                                    ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                } else {
                                    ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                    ButtonManager.this.showBeautyLayout();
                                    ButtonManager.this.hideEffectLayout();
                                }
                                ButtonManager.this.showEffectsContainerWrapper();
                                ButtonManager.this.animationToEffectWrapper();
                            }
                        } else if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 5 && ButtonManager.this.mAppController.getCameraAppUI().getEffectEnable()) {
                            ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                            ButtonManager.this.showBeautyLayout();
                            ButtonManager.this.hideEffectLayout();
                            ButtonManager.this.showEffectsContainerWrapper();
                            ButtonManager.this.animationToEffectWrapper();
                        }
                    }
                }
            });
        }
    }

    public void hideBeauty2Button() {
        if (this.mButtonBeauty2 != null) {
            this.mButtonBeauty2.setVisibility(8);
        }
    }

    public void initializeMotionButton() {
        if (this.mButtonMotion != null) {
            int i = 0;
            this.mButtonMotion.setVisibility(0);
            this.mButtonMotion.setSelectionModeClickListener(new -$$Lambda$ButtonManager$6BX942zzgMW-U--AyAD0x32E6_g(this));
            int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MOTION);
            MultiToggleImageButton multiToggleImageButton = this.mButtonMotion;
            if (index >= 0) {
                i = index;
            }
            multiToggleImageButton.setState(i, true);
            this.mButtonMotion.setOnStateChangeListener(new OnStateChangeListener() {
                public void stateChanged(View view, int state) {
                    ButtonManager.this.mAppController.getLockEventListener().onIdle();
                    ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MOTION, state);
                    String value = ButtonManager.this.mSettingsManager.getValueByIndex(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_CAMERA_MOTION, state);
                    if ("1".equals(value)) {
                        ButtonManager.this.hideButtons(5);
                    } else if ("0".equals(value)) {
                        ButtonManager.this.showButtons(5);
                    }
                }
            });
            this.mButtonMotion.setOnTouchListener(new OnTouchListener() {
                public void onTouchDown() {
                    ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(ButtonManager.this.mButtonMotion.hashCode());
                }

                public void onTouchUp() {
                    ButtonManager.this.mAppController.getLockEventListener().onIdle();
                }
            });
        }
    }

    public static /* synthetic */ void lambda$initializeMotionButton$0(ButtonManager buttonManager, View v) {
        buttonManager.mCameraSelectionMenu.bindButtonAndIcons(buttonManager.mAppController, buttonManager.mButtonMotion, R.array.camera_motion_selection_icons, R.array.camera_motion_selection_icons_texts);
        buttonManager.toggleSelectionMenu();
    }

    /* Access modifiers changed, original: protected */
    public boolean isDepthEnabled() {
        return Keys.isDepthOn(this.mAppController.getSettingsManager()) && this.mAppController.getCurrentCameraId() == 0 && this.mAppController.getCurrentModuleIndex() == 4;
    }

    /* Access modifiers changed, original: protected */
    public boolean isBeautyShow() {
        return this.mAppController.getCameraAppUI().getBeautySeek() > 0.0f && !isDepthEnabled();
    }

    /* Access modifiers changed, original: protected */
    public boolean isMaskSelected() {
        return (this.mAppController.getCameraAppUI().getCurrSelect() == null || this.mAppController.getCameraAppUI().getCurrSelect() == "" || isDepthEnabled()) ? false : true;
    }

    public void initializeEffectButton() {
        if (this.mButtonEffect != null) {
            if (this.mButtonEffect.getState() != 1) {
                if (TextUtils.isEmpty(this.mAppController.getCameraAppUI().getCurrSelect())) {
                    this.mButtonEffect.setState(0);
                } else {
                    this.mButtonEffect.setState(1);
                }
            }
            this.mButtonEffect.setVisibility(0);
            this.mButtonEffect.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    long time = System.currentTimeMillis();
                    if (time - ButtonManager.this.mLastOnClickTime >= 300) {
                        long intervalTime = time - CameraAppUI.sLastShowOrHideGLSurfaceTime;
                        if (intervalTime < 2500) {
                            Log.d(ButtonManager.TAG, "IntervalTime is too short");
                            return;
                        }
                        ButtonManager.this.mLastOnClickTime = time;
                        Tag access$900 = ButtonManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("IntervalTime is enough , intervalTime = ");
                        stringBuilder.append(intervalTime);
                        Log.d(access$900, stringBuilder.toString());
                        ButtonManager.this.showEffectSubContainerByType(1);
                        if (ButtonManager.this.mAppController.getCurrentModuleIndex() != 4) {
                            ButtonManager.this.mAppController.getCurrentModuleIndex();
                        }
                        if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 4 || ButtonManager.this.mAppController.getCurrentModuleIndex() == 15) {
                            if (ButtonManager.this.isBeautyShow()) {
                                ButtonManager.this.showBeauty2Button();
                            } else {
                                ButtonManager.this.hideBeauty2Button();
                            }
                            if (ButtonManager.this.isMaskSelected()) {
                                ButtonManager.this.showEffect2Button();
                            } else {
                                ButtonManager.this.hideEffect2Button();
                            }
                        }
                        if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 5) {
                            if (ButtonManager.this.isMaskSelected()) {
                                ButtonManager.this.showEffect2Button();
                            } else {
                                ButtonManager.this.hideEffect2Button();
                            }
                        }
                    }
                }
            });
        }
    }

    public void initializeBeautyButton(OnSeekBarChangeListener listener) {
        if (this.mButtonBeauty != null) {
            if (this.mButtonBeauty.getState() != 1) {
                if (this.mAppController.getCameraAppUI().getBeautySeek() > 0.0f) {
                    this.mButtonBeauty.setState(1);
                } else {
                    this.mButtonBeauty.setState(0);
                }
            }
            this.mButtonBeauty.setVisibility(0);
            this.mBeautySeekbar.setOnSeekbarChangeListener(listener);
            this.mButtonBeauty.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    long time = System.currentTimeMillis();
                    if (time - ButtonManager.this.mLastOnClickTime >= 300) {
                        long intervalTime = time - CameraAppUI.sLastShowOrHideGLSurfaceTime;
                        if (intervalTime < 2500) {
                            Log.d(ButtonManager.TAG, "IntervalTime is too short");
                            return;
                        }
                        ButtonManager.this.mLastOnClickTime = time;
                        Tag access$900 = ButtonManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("IntervalTime is enough , intervalTime = ");
                        stringBuilder.append(intervalTime);
                        Log.d(access$900, stringBuilder.toString());
                        ButtonManager.this.showEffectSubContainerByType(3);
                        if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 4 || ButtonManager.this.mAppController.getCurrentModuleIndex() == 15) {
                            if (ButtonManager.this.isBeautyShow()) {
                                ButtonManager.this.showBeauty2Button();
                            } else {
                                ButtonManager.this.hideBeauty2Button();
                            }
                            if (ButtonManager.this.isMaskSelected()) {
                                ButtonManager.this.showEffect2Button();
                            } else {
                                ButtonManager.this.hideEffect2Button();
                            }
                        }
                        if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 0) {
                            if (ButtonManager.this.isBeautyShow()) {
                                ButtonManager.this.showBeauty2Button();
                            } else {
                                ButtonManager.this.hideBeauty2Button();
                            }
                        }
                    }
                }
            });
        }
    }

    public void updateMOBExtraWithHeight(int height) {
        if (this.mMOBExtraLayout != null) {
            this.mMOBExtraLayout.onResetHeight(height);
            this.mMOBExtraLayout.requestLayout();
            this.mMOBExtraLayout.postInvalidate();
        }
    }

    public void requestMOBExtraLayout() {
        if (this.mMOBExtraLayout != null) {
            this.mMOBExtraLayout.requestLayout();
        }
    }

    public void setMOBExtraVisibility(int visibility) {
        if (this.mMOBExtraLayout != null) {
            this.mMOBExtraLayout.setVisibility(visibility);
        }
    }

    public void initMOBExtraLayout() {
        if (this.mMOBExtraLayout == null) {
            this.mMOBExtraLayout = (BottomBarModeOptionsWrapperExtra) ((ViewStub) this.mAppController.getModuleLayoutRoot().findViewById(R.id.mob_extra_viewstub)).inflate();
            this.mMOBExtraLayout.setCaptureLayoutHelper(this.mAppController.getCameraAppUI().getCaptureLayoutHelper());
            this.mMOBExtraLayout.setMoreEnterToggleButton(this.mMoreEnterToggleButton);
            this.mMOBExtraLayout.onResetHeight(MASK_TOTAL_HEIGHT);
        }
    }

    public void initLiveBolkenLayout(OnSeekBarChangeListener listener) {
        if (this.mButtonBolken != null) {
            if (this.mButtonBolken.getState() != 1) {
                this.mButtonBolken.setState(0);
            }
            this.mButtonBolken.setVisibility(0);
            this.mBolkenSeekbar.setOnSeekbarChangeListener(listener);
            this.mButtonBolken.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    ButtonManager.this.showEffectSubContainerByType(2);
                }
            });
        }
    }

    public void setBeautyState(int state) {
        this.mButtonBeauty.setState(state);
    }

    public void setLiveBokehState(int state) {
        this.mButtonBolken.setState(state);
    }

    public void showEffectSubContainerByType(int type) {
        switch (type) {
            case 1:
                hideBeautyLayout();
                hideLiveBolkenLayout();
                if (this.mMaskLayout == null || this.mMaskLayout.getVisibility() != 0) {
                    updateMOBExtraWithHeight(MASK_TOTAL_HEIGHT);
                    showEffectLayout();
                    return;
                }
                this.mAppController.getCameraAppUI().resetCurrMaskSelect();
                this.mAppController.getCameraAppUI().setEffectEnable(false);
                return;
            case 2:
                hideBeautyLayout();
                hideEffectLayout();
                if (this.mBolkenLayout == null || this.mBolkenLayout.getVisibility() != 0) {
                    updateMOBExtraWithHeight(MASK_TOTAL_HEIGHT);
                    showLiveBolkenLayout();
                    return;
                }
                return;
            case 3:
                hideEffectLayout();
                hideLiveBolkenLayout();
                if (this.mBeautifulLayout == null || this.mBeautifulLayout.getVisibility() != 0) {
                    updateMOBExtraWithHeight(MASK_TOTAL_HEIGHT);
                    showBeautyLayout();
                    if (this.mBeautySeekbar != null) {
                        this.mBeautySeekbar.setProgress(getLastBeautySeekProgress());
                        return;
                    }
                    return;
                }
                if (this.mBeautySeekbar.getProgress() > 0) {
                    setLastBeautySeekProgress(this.mBeautySeekbar.getProgress());
                }
                if (this.mBeautySeekbar.getProgress() > 0) {
                    setBeautyState(0);
                    this.mAppController.getCameraAppUI().setBeautyEnable(false);
                    if (this.mBeautySeekbar != null) {
                        this.mBeautySeekbar.setProgress(0);
                        return;
                    }
                    return;
                }
                if (this.mBeautySeekbar.getProgress() == 0) {
                    setBeautyState(0);
                } else {
                    setBeautyState(1);
                }
                this.mAppController.getCameraAppUI().setBeautyEnable(true);
                if (this.mBeautySeekbar != null) {
                    this.mBeautySeekbar.setProgress(getLastBeautySeekProgress());
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void initEffectsEnterToggleButton() {
        if (this.mEffectsEnterToggleButton != null) {
            this.mEffectsEnterToggleButton.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    if (ButtonManager.this.mEffectsContainerWrapper == null) {
                        ButtonManager.this.initMOBExtraLayout();
                        ButtonManager.this.mEffectsContainerWrapper = (EffectsContainerWrapper) ButtonManager.this.mMOBExtraLayout.findViewById(R.id.effect_layout_wrapper);
                        ButtonManager.this.initEffectsRelatedViews(ButtonManager.this.mAppController.getModuleLayoutRoot());
                    }
                    long time = System.currentTimeMillis();
                    if (time - ButtonManager.this.mLastOnClickTime >= 500) {
                        long intervalTime = time - CameraAppUI.sLastShowOrHideGLSurfaceTime;
                        if (intervalTime < 2500) {
                            Log.d(ButtonManager.TAG, "IntervalTime is too short");
                            return;
                        }
                        ButtonManager.this.mLastOnClickTime = time;
                        Tag access$900 = ButtonManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("IntervalTime is enough , intervalTime = ");
                        stringBuilder.append(intervalTime);
                        Log.d(access$900, stringBuilder.toString());
                        if (ButtonManager.this.mAppController.getCurrentModuleController().isCameraOpened()) {
                            if (ButtonManager.this.mEffectsContainerWrapper.getVisibility() == 0) {
                                ButtonManager.this.hideEffectsContainerWrapper();
                                ButtonManager.this.setModuleEffect();
                            } else {
                                ButtonManager.this.hideMoreOptionsWrapper();
                                if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 0) {
                                    ButtonManager.this.showLiveBolkenButton();
                                    ButtonManager.this.hideEffectButton(ButtonManager.this.mAppController.getCurrentModuleIndex());
                                    ButtonManager.this.hideEffectLayout();
                                    if (ButtonManager.this.mBeautifulLayout.getVisibility() == 0 && ButtonManager.this.mBolkenLayout.getVisibility() == 0) {
                                        ButtonManager.this.showBeautyLayout();
                                        ButtonManager.this.hideLiveBolkenLayout();
                                        ButtonManager.this.showEffectsContainerWrapper();
                                    } else {
                                        ButtonManager.this.hideBeautyLayout();
                                        ButtonManager.this.showEffectsContainerWrapper();
                                        ButtonManager.this.showLiveBolkenLayout();
                                    }
                                    if (ButtonManager.this.mAppController.getCameraAppUI().getBeautyEnable()) {
                                        ButtonManager.this.animationToEffectWrapper();
                                    } else {
                                        ButtonManager.this.mAppController.openOrCloseEffects(1, R.id.effect_toggle_button, ButtonManager.this.mAppController.getCurrentModuleIndex());
                                    }
                                    ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                } else {
                                    boolean isGlMode = ButtonManager.this.mAppController.getCameraAppUI().getBeautyEnable() || ButtonManager.this.mAppController.getCameraAppUI().getEffectEnable();
                                    ButtonManager.this.hideLiveBolkenLayout();
                                    ButtonManager.this.hideLiveBolkenButton();
                                    if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 4 || ButtonManager.this.mAppController.getCurrentModuleIndex() == 15) {
                                        if (isGlMode) {
                                            if (ButtonManager.this.mBeautifulLayout.getVisibility() == 0 && ButtonManager.this.mMaskLayout.getVisibility() == 0) {
                                                ButtonManager.this.hideEffectLayout();
                                            } else if (ButtonManager.this.mAppController.getCameraAppUI().getBeautyEnable()) {
                                                ButtonManager.this.hideEffectLayout();
                                                ButtonManager.this.showBeautyLayout();
                                                ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                            } else {
                                                ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                                ButtonManager.this.showBeautyLayout();
                                                ButtonManager.this.hideEffectLayout();
                                            }
                                            ButtonManager.this.showEffectsContainerWrapper();
                                            ButtonManager.this.animationToEffectWrapper();
                                        } else {
                                            ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                            ButtonManager.this.showBeautyLayout();
                                            ButtonManager.this.hideEffectLayout();
                                            ButtonManager.this.showEffectsContainerWrapper();
                                            ButtonManager.this.mAppController.openOrCloseEffects(1, R.id.effect_toggle_button, ButtonManager.this.mAppController.getCurrentModuleIndex());
                                        }
                                    } else if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 5) {
                                        if (ButtonManager.this.mAppController.getCameraAppUI().getEffectEnable()) {
                                            ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                            ButtonManager.this.showBeautyLayout();
                                            ButtonManager.this.hideEffectLayout();
                                            ButtonManager.this.showEffectsContainerWrapper();
                                            ButtonManager.this.animationToEffectWrapper();
                                        } else {
                                            ButtonManager.this.updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
                                            ButtonManager.this.showBeautyLayout();
                                            ButtonManager.this.hideEffectLayout();
                                            ButtonManager.this.showEffectsContainerWrapper();
                                            ButtonManager.this.mAppController.openOrCloseEffects(1, R.id.effect_toggle_button, ButtonManager.this.mAppController.getCurrentModuleIndex());
                                        }
                                    }
                                }
                            }
                            if (ButtonManager.this.mAppController.getCurrentModuleIndex() != 4) {
                                ButtonManager.this.mAppController.getCurrentModuleIndex();
                            }
                            if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 4 || ButtonManager.this.mAppController.getCurrentModuleIndex() == 15) {
                                if (ButtonManager.this.isBeautyShow()) {
                                    ButtonManager.this.showBeauty2Button();
                                } else {
                                    ButtonManager.this.hideBeauty2Button();
                                }
                                if (ButtonManager.this.isMaskSelected()) {
                                    ButtonManager.this.showEffect2Button();
                                } else {
                                    ButtonManager.this.hideEffect2Button();
                                }
                            }
                            if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 0) {
                                if (ButtonManager.this.isBeautyShow() && ButtonManager.this.mAppController.getCurrentCameraId() == 1) {
                                    ButtonManager.this.showBeauty2Button();
                                } else {
                                    ButtonManager.this.hideBeauty2Button();
                                }
                            }
                            if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 5) {
                                if (ButtonManager.this.isMaskSelected()) {
                                    ButtonManager.this.showEffect2Button();
                                } else {
                                    ButtonManager.this.hideEffect2Button();
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void animationToEffectWrapper() {
        this.mAppController.getCameraAppUI().freezeScreenUntilPreviewReady(true, this.mTransitionView);
    }

    private void setModuleEffect() {
        boolean beauty = this.mAppController.getCameraAppUI().getBeautySeek() == 0.0f;
        boolean effect = TextUtils.isEmpty(this.mAppController.getCameraAppUI().getCurrSelect());
        int currentModuleIndex = this.mAppController.getCurrentModuleIndex();
        if (currentModuleIndex != 0) {
            if (currentModuleIndex != 15) {
                switch (currentModuleIndex) {
                    case 4:
                        if (beauty && effect) {
                            this.mAppController.getCameraAppUI().setBeautyEnable(false);
                            this.mAppController.getCameraAppUI().setEffectEnable(false);
                            this.mAppController.openOrCloseEffects(0, R.id.effect_toggle_button, this.mAppController.getCurrentModuleIndex());
                            return;
                        }
                        animationToEffectWrapper();
                        return;
                    case 5:
                        if (effect) {
                            this.mAppController.getCameraAppUI().setEffectEnable(false);
                            this.mAppController.openOrCloseEffects(0, R.id.effect_toggle_button, this.mAppController.getCurrentModuleIndex());
                            return;
                        }
                        animationToEffectWrapper();
                        return;
                    default:
                        return;
                }
            } else if (beauty && effect) {
                this.mAppController.getCameraAppUI().setBeautyEnable(false);
                this.mAppController.getCameraAppUI().setEffectEnable(false);
                this.mAppController.openOrCloseEffects(0, R.id.effect_toggle_button, this.mAppController.getCurrentModuleIndex());
            } else {
                animationToEffectWrapper();
            }
        } else if (beauty) {
            this.mAppController.getCameraAppUI().setBeautyEnable(false);
            this.mAppController.openOrCloseEffects(0, R.id.effect_toggle_button, this.mAppController.getCurrentModuleIndex());
        } else {
            animationToEffectWrapper();
        }
    }

    public void initMoreEnterToggleButton() {
        if (this.mMoreEnterToggleButton != null) {
            this.mMoreEnterToggleButton.setState(0);
            this.mMoreEnterToggleButton.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    if (ButtonManager.this.mMoreOptionsWrapper == null) {
                        ButtonManager.this.initMOBExtraLayout();
                        MoreOptionsUI moreOptionsUI = new MoreOptionsUI((CameraActivity) ButtonManager.this.mAppController, ButtonManager.this.mAppController.getModuleLayoutRoot());
                        ButtonManager.this.mMoreOptionsWrapper = (MoreOptionsWrapper) ButtonManager.this.mMOBExtraLayout.findViewById(R.id.more_layout_wrapper);
                    }
                    if (ButtonManager.this.mMoreOptionsWrapper.getVisibility() == 0) {
                        ButtonManager.this.hideMoreOptionsWrapper();
                        return;
                    }
                    ButtonManager.this.hideEffectsContainerWrapper();
                    ButtonManager.this.showMoreOptionsWrapper();
                }
            });
        }
    }

    public int getEffectsEnterWrapperVisible() {
        if (this.mEffectsContainerWrapper != null) {
            return this.mEffectsContainerWrapper.getVisibility();
        }
        return 8;
    }

    public int getMoreEnterWrapperVisible() {
        if (this.mMoreOptionsWrapper != null) {
            return this.mMoreOptionsWrapper.getVisibility();
        }
        return 8;
    }

    public void setEffectsEnterToggleButton(int visible) {
        if (this.mEffectsEnterToggleButton != null) {
            this.mEffectsEnterToggleButton.setVisibility(visible);
        }
    }

    public void setMoreEnterToggleButton(int visible) {
        if (this.mMoreEnterToggleButton != null) {
            this.mMoreEnterToggleButton.setVisibility(visible);
        }
    }

    public void setSettingButton(int visible) {
        if (this.mButtonSetting != null) {
            this.mButtonSetting.setVisibility(visible);
        }
    }

    public void showBeautyButton() {
        if (this.mButtonBeauty != null) {
            this.mButtonBeauty.setVisibility(0);
        }
    }

    public void showBeautyLayout() {
        if (this.mBeautifulLayout != null) {
            if (this.mBeautySeekbar.getProgress() > 0) {
                this.mButtonBeauty.setState(1);
            } else {
                this.mButtonBeauty.setState(0);
            }
            this.mBeautifulLayout.setVisibility(0);
            this.mBeautifulLayout.requestLayout();
        }
    }

    public void hideBeautyLayout() {
        if (this.mBeautifulLayout != null) {
            if (this.mButtonBeauty.getVisibility() == 0) {
                this.mButtonBeauty.setState(0);
            }
            this.mBeautifulLayout.setVisibility(8);
        }
    }

    public void showEffectsContainerWrapper() {
        if (this.mEffectsContainerWrapper == null) {
            initMOBExtraLayout();
            this.mEffectsContainerWrapper = (EffectsContainerWrapper) this.mMOBExtraLayout.findViewById(R.id.effect_layout_wrapper);
            initEffectsRelatedViews(this.mAppController.getModuleLayoutRoot());
        }
        if (this.mEffectsContainerWrapper == null) {
            return;
        }
        if (8 == this.mEffectsContainerWrapper.getVisibility() || this.mEffectsContainerWrapper.getVisibility() == 4) {
            this.mEffectsEnterToggleButton.setState(1);
            Animation ta = new TranslateAnimation(0.0f, 0.0f, (float) this.mEffectsContainerWrapper.getHeight(), 0.0f);
            boolean isGlMode = this.mAppController.getCameraAppUI().getBeautyEnable() || this.mAppController.getCameraAppUI().getEffectEnable();
            int duration = isGlMode ? 600 : 700;
            ta.setDuration((long) duration);
            Animation aa = new AlphaAnimation(0.0f, 1.0f);
            aa.setDuration((long) duration);
            AnimationSet as = new AnimationSet(true);
            as.addAnimation(ta);
            as.addAnimation(aa);
            this.mEffectsContainerWrapper.startAnimation(as);
            this.mEffectsContainerWrapper.setVisibility(0);
        }
    }

    public void hideEffectsContainerWrapper() {
        if (this.mEffectsContainerWrapper != null && 8 != this.mEffectsContainerWrapper.getVisibility()) {
            this.mEffectsEnterToggleButton.setState(0);
            Animation ta = new TranslateAnimation(0.0f, 0.0f, 0.0f, (float) this.mEffectsContainerWrapper.getHeight());
            ta.setDuration(600);
            Animation aa = new AlphaAnimation(1.0f, 0.0f);
            aa.setDuration(600);
            aa.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    ButtonManager.this.mEffectsContainerWrapper.setVisibility(8);
                }

                public void onAnimationRepeat(Animation animation) {
                }
            });
            AnimationSet as = new AnimationSet(true);
            as.addAnimation(ta);
            as.addAnimation(aa);
            this.mEffectsContainerWrapper.startAnimation(as);
        }
    }

    public void setEffectWrapperVisible(int visible) {
        if (this.mEffectsContainerWrapper != null) {
            if (visible == 0) {
                this.mEffectsEnterToggleButton.setState(1);
            } else {
                this.mEffectsEnterToggleButton.setState(0);
            }
            this.mEffectsContainerWrapper.setVisibility(visible);
        }
    }

    public boolean isMoreOptionsWrapperShow() {
        boolean z = false;
        if (this.mMoreOptionsWrapper == null) {
            return false;
        }
        if (this.mMoreOptionsWrapper.getVisibility() == 0) {
            z = true;
        }
        return z;
    }

    public void showMoreOptionsWrapper() {
        if (this.mMoreOptionsWrapper != null) {
            this.mMoreEnterToggleButton.setState(1);
            this.mMoreOptionsWrapper.setVisibility(0);
        }
    }

    public void hideMoreOptionsWrapper() {
        if (this.mMoreOptionsWrapper != null) {
            this.mMoreEnterToggleButton.setState(0);
            this.mMoreOptionsWrapper.setVisibility(8);
        }
    }

    public void hideBeautyButton() {
        if (this.mButtonBeauty != null) {
            this.mButtonBeauty.setVisibility(4);
        }
    }

    public void hideEffectButton(int id) {
        if (this.mButtonEffect == null) {
            return;
        }
        if (id == 4 || id == 15) {
            this.mButtonEffect.setVisibility(8);
        } else {
            this.mButtonEffect.setVisibility(8);
        }
    }

    public void showEffectButton() {
        if (this.mButtonEffect != null) {
            this.mButtonEffect.setVisibility(0);
        }
    }

    public void hideMotionButtons() {
        if (this.mButtonMotion != null) {
            this.mButtonMotion.setVisibility(8);
        }
    }

    public void initializeLivebroadcastButton() {
        if (this.mButtonliveBroadcast != null) {
            this.mButtonliveBroadcast.setState(0);
            this.mButtonliveBroadcast.setVisibility(0);
            this.mButtonliveBroadcast.setSelectionModeClickListener(new -$$Lambda$ButtonManager$yzDTQVHFpRinkdGw8vaPI74PBeE(this));
        }
    }

    public static /* synthetic */ void lambda$initializeLivebroadcastButton$1(ButtonManager buttonManager, View v) {
        buttonManager.mCameraSelectionMenu.bindButtonAndIcons(buttonManager.mAppController, buttonManager.mButtonliveBroadcast, R.array.live_icons, R.array.live_texts);
        buttonManager.toggleSelectionMenu();
    }

    public void initializeDualSightButton() {
        if (this.mButtonDualSight != null) {
            this.mButtonDualSight.setState(0);
            this.mButtonDualSight.setVisibility(8);
            this.mButtonDualSight.setSelectionModeClickListener(new -$$Lambda$ButtonManager$JSrzANwv_tBVXZ927QFMAEj7rK8(this));
        }
    }

    public static /* synthetic */ void lambda$initializeDualSightButton$2(ButtonManager buttonManager, View v) {
        buttonManager.mCameraSelectionMenu.bindButtonAndIcons(buttonManager.mAppController, buttonManager.mButtonDualSight, R.array.camera_dual_sight_selection_icons, R.array.camera_dual_sight_selection_texts);
        buttonManager.toggleSelectionMenu();
    }

    public void hideDualSightButton() {
        if (this.mButtonDualSight != null) {
            this.mButtonDualSight.setVisibility(8);
        }
    }

    public void hidePhotoModuleRelatedButtons() {
        this.mButtonMotion.setVisibility(8);
        if (this.mButtonEffect != null) {
            this.mButtonEffect.setVisibility(8);
        }
        if (this.mButtonBeauty != null) {
            this.mButtonBeauty.setVisibility(8);
        }
        this.mButtonDualSight.setVisibility(8);
        this.mButtonliveBroadcast.setVisibility(8);
    }

    public void initializePushButton(int buttonId, OnClickListener cb, int imageId) {
        ImageButton button = getImageButtonOrError(buttonId);
        button.setOnClickListener(cb);
        button.setImageResource(imageId);
        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (this.mListener != null) {
                this.mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, Integer.valueOf(buttonId));
        if (button.getVisibility() != 0) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void initializePushButton(int buttonId, OnClickListener cb) {
        ImageButton button = getImageButtonOrError(buttonId);
        if (cb != null) {
            button.setOnClickListener(cb);
        }
        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (this.mListener != null) {
                this.mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, Integer.valueOf(buttonId));
        if (button.getVisibility() != 0) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void hideButtonLives() {
        this.mButtonliveBroadcast.setVisibility(8);
    }

    public void disableButton(int buttonId) {
        MultiToggleImageButton button = getButtonOrError(buttonId);
        if (buttonId == 4) {
            initializeHdrPlusButtonIcons(button, R.array.pref_camera_hdr_plus_icons);
        } else if (buttonId == 5) {
            initializeHdrButtonIcons(button, R.array.hdr_icons);
        }
        if (button.isEnabled()) {
            button.setEnabled(false);
            if (this.mListener != null) {
                this.mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, null);
        if (button.getVisibility() != 0) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public int disableButtonWithLock(int buttonId) {
        Lock lock;
        if (this.mLockMap.containsKey(Integer.valueOf(buttonId))) {
            lock = (Lock) this.mLockMap.get(Integer.valueOf(buttonId));
        } else {
            lock = LockUtils.getInstance().generateMultiLock(LockType.MULTILOCK);
            this.mLockMap.put(Integer.valueOf(buttonId), lock);
        }
        return lock.aquireLock().intValue();
    }

    public void enableButtonWithToken(int buttonId, int token) {
        if (this.mLockMap.containsKey(Integer.valueOf(buttonId))) {
            Lock lock = (Lock) this.mLockMap.get(Integer.valueOf(buttonId));
            lock.unlockWithToken(Integer.valueOf(token));
            if (!lock.isLocked()) {
                enableButton(buttonId);
            }
            return;
        }
        enableButton(buttonId);
    }

    public void enableButton(int buttonId) {
        boolean etAuto = this.mAppController.getSettingsManager().getBoolean(this.mAppController.getCameraScope(), Keys.KEY_PRO_CURRENT_ET_AUTO_STATES);
        if ((buttonId == 0 || buttonId == 1) && !etAuto) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Pro enableButton etAuto = ");
            stringBuilder.append(etAuto);
            Log.d(tag, stringBuilder.toString());
            return;
        }
        MultiToggleImageButton button = getButtonOrError(buttonId);
        if (!button.isEnabled()) {
            button.setEnabled(true);
            if (this.mListener != null) {
                this.mListener.onButtonEnabledChanged(this, buttonId);
            }
        }
        button.setTag(R.string.tag_enabled_id, Integer.valueOf(buttonId));
        if (button.getVisibility() != 0) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void disableButtonClick(int buttonId) {
        MultiToggleImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            button.setClickEnabled(false);
        }
    }

    public void enableButtonClick(int buttonId) {
        MultiToggleImageButton button = getButtonOrError(buttonId);
        if (button instanceof MultiToggleImageButton) {
            button.setClickEnabled(true);
        }
    }

    public void hideButton(int buttonId) {
        View button;
        if (buttonId == 0) {
            this.mButtonId = buttonId;
        }
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (button.getVisibility() == 0) {
            button.setVisibility(8);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public boolean isFlashButtonHidden() {
        return this.mButtonId == 0;
    }

    public void hideButtons(int buttonId) {
        if (buttonId == 0) {
            this.mButtonId = buttonId;
        }
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (getButtonOrError(buttonId).getVisibility() == 0) {
            getButtonOrError(buttonId).setVisibility(8);
            if (buttonId == 0) {
                this.mAppController.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_HIDDEN, true);
                this.mButtonFlash.overrideImageIds(R.array.camera_flashmode_icons);
                this.mButtonFlash.overrideContentDescriptions(R.array.camera_flash_descriptions);
                this.mFlashState0 = Integer.valueOf(this.mAppController.getSettingsManager().getIndexOfCurrentValue(getCameraScopes(0), Keys.KEY_FLASH_MODE));
                this.mAppController.getSettingsManager().set(getCameraScopes(0), Keys.KEY_FLASH_MODE_BEFORE_BEAUTY, this.mFlashState0.intValue());
                this.mFlashState1 = Integer.valueOf(this.mAppController.getSettingsManager().getIndexOfCurrentValue(getCameraScopes(1), Keys.KEY_FLASH_MODE));
                this.mAppController.getSettingsManager().set(getCameraScopes(1), Keys.KEY_FLASH_MODE_BEFORE_BEAUTY, this.mFlashState1.intValue());
                this.mSettingsManager.setValueByIndex(getCameraScopes(0), Keys.KEY_FLASH_MODE, 0);
                this.mSettingsManager.setValueByIndex(getCameraScopes(1), Keys.KEY_FLASH_MODE, 0);
            } else if (buttonId == 5) {
                this.mAppController.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HDR_HIDDEN, true);
                initializeHdrButtonIcons(this.mButtonHdr, R.array.hdr_icons);
                this.mHdrState = Integer.valueOf(this.mAppController.getSettingsManager().getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR));
                this.mAppController.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HDR_MODE_BEFORE_BEAUTY, this.mHdrState.intValue());
                this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, 0);
            }
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public String getCameraScopes(int cameraId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CameraActivity.CAMERA_SCOPE_PREFIX);
        stringBuilder.append(Integer.toString(cameraId));
        return stringBuilder.toString();
    }

    public void showButtons(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (button.getVisibility() == 8) {
            if (buttonId == 0) {
                this.mButtonFlash.overrideImageIds(R.array.camera_flashmode_icons);
                this.mButtonFlash.overrideContentDescriptions(R.array.camera_flash_descriptions);
                if (this.mAppController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_HIDDEN, false)) {
                    this.mFlashState0 = this.mAppController.getSettingsManager().getInteger(getCameraScopes(0), Keys.KEY_FLASH_MODE_BEFORE_BEAUTY);
                    this.mAppController.getSettingsManager().setValueByIndex(getCameraScopes(0), Keys.KEY_FLASH_MODE, this.mFlashState0.intValue());
                    this.mFlashState1 = this.mAppController.getSettingsManager().getInteger(getCameraScopes(1), Keys.KEY_FLASH_MODE_BEFORE_BEAUTY);
                    this.mAppController.getSettingsManager().setValueByIndex(getCameraScopes(1), Keys.KEY_FLASH_MODE, this.mFlashState1.intValue());
                    getButtonOrError(0).setState(this.mAppController.getSettingsManager().getInteger(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE_BEFORE_BEAUTY).intValue(), false);
                }
                button.setVisibility(0);
            } else if (buttonId == 5) {
                initializeHdrButtonIcons(this.mButtonHdr, R.array.hdr_icons);
                if (this.mAppController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HDR_HIDDEN, false)) {
                    int hdrState = this.mAppController.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HDR_MODE_BEFORE_BEAUTY).intValue();
                    this.mAppController.getSettingsManager().setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, hdrState);
                    getButtonOrError(5).setState(hdrState, false);
                }
                button.setVisibility(0);
            } else {
                button.setVisibility(0);
            }
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void showButton(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (button.getVisibility() == 8) {
            button.setVisibility(0);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void tryCancelSelectionMenu() {
        if (this.mCameraSelectionMenu.getVisibility() == 0) {
            this.mCameraSelectionMenu.onMenuCanceled();
            toggleSelectionMenu();
        }
        if (getMoreEnterWrapperVisible() == 0) {
            hideMoreOptionsWrapper();
        }
    }

    public void toggleSelectionMenu() {
        if (isMoreOptionsWrapperShow()) {
            hideMoreOptionsWrapper();
            this.mAppController.getLockEventListener().onIdle();
            return;
        }
        MultiToggleImageButton stateButton;
        if (this.mCameraSelectionMenu.getVisibility() == 0) {
            this.mCameraSelectionMenu.setVisibility(8);
            this.mModeOptionsButtons.setVisibility(0);
            this.mModeOptionsOverlay.requestLayout();
            this.mModeOptions.requestLayout();
            this.mModeOptionsButtons.requestLayout();
            this.mCameraSelectionMenu.onMenuClosed();
            stateButton = this.mCameraSelectionMenu.getBindButton();
            if (stateButton != null && stateButton.getId() == R.id.beauty_toggle_button) {
                this.mAppController.openOrCloseEffects(stateButton.getState(), R.id.beauty_toggle_button, this.mAppController.getCurrentModuleIndex());
                if (stateButton.getState() == 1) {
                    showBeautyLayout();
                } else {
                    hideBeautyLayout();
                }
            }
            if (stateButton != null) {
                stateButton.getId();
            }
        } else {
            stateButton = this.mCameraSelectionMenu.getBindButton();
            if ((stateButton.getId() == R.id.hdr_plus_toggle_button || stateButton.getId() == R.id.motion_toggle_button || stateButton.getId() == R.id.flash_toggle_button) && this.mCameraSelectionMenu.getVisibility() == 4) {
                this.mCameraSelectionMenu.setVisibility(8);
                this.mModeOptionsButtons.setVisibility(0);
                this.mModeOptionsOverlay.requestLayout();
                this.mModeOptions.requestLayout();
                this.mModeOptionsButtons.requestLayout();
                this.mCameraSelectionMenu.onMenuClosed();
            } else {
                this.mCameraSelectionMenu.setVisibility(0);
                this.mModeOptionsButtons.setVisibility(8);
                this.mModeOptionsOverlay.requestLayout();
                this.mModeOptions.requestLayout();
                this.mCameraSelectionMenu.requestLayout();
                LockUtils.getInstance().generateMultiLock(LockType.MULTILOCK).ignoreLock(true);
            }
        }
    }

    public void showEffectLayout() {
        if (this.mMaskLayout != null && this.mButtonEffect != null) {
            this.mButtonEffect.setState(1);
            this.mMaskLayout.setVisibility(0);
            initAdapter(this.mMaskLayout.getContext());
        }
    }

    public void initEffectById(int id) {
        if (id != 0) {
            if (id == 4) {
                if (this.mAppController.getCameraAppUI().getEffectEnable()) {
                    showEffectLayout();
                    hideBeautyLayout();
                    hideLiveBolkenLayout();
                } else {
                    hideEffectLayout();
                    showBeautyLayout();
                    hideLiveBolkenLayout();
                }
                updateMOBExtraWithHeight(MASK_TOTAL_HEIGHT);
            }
        } else if (this.mAppController.getCameraAppUI().getBeautyEnable()) {
            showBeautyLayout();
            hideEffectLayout();
            hideLiveBolkenLayout();
            updateMOBExtraWithHeight(MASK_TOTAL_HEIGHT);
        }
    }

    public void hideEffectLayout() {
        if (this.mMaskLayout != null && this.mButtonEffect != null) {
            if (this.mButtonEffect.getVisibility() == 0) {
                if (TextUtils.isEmpty(this.mAppController.getCameraAppUI().getCurrSelect())) {
                    this.mButtonEffect.setState(0);
                } else {
                    this.mButtonEffect.setState(1);
                }
            }
            this.mMaskLayout.setVisibility(8);
        }
    }

    public void setButtonInvisible(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        if (button.getVisibility() == 0) {
            button.setVisibility(4);
            if (this.mListener != null) {
                this.mListener.onButtonVisibilityChanged(this, buttonId);
            }
        }
    }

    public void hideSettings() {
        if (this.mButtonSetting != null && this.mButtonSetting.getVisibility() != 8) {
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                this.mButtonSetting.setVisibility(8);
            } else {
                this.mButtonSetting.post(new Runnable() {
                    public void run() {
                        ButtonManager.this.mButtonSetting.setVisibility(8);
                    }
                });
            }
        }
    }

    public void showSettings() {
        if (this.mButtonSetting != null && this.mButtonSetting.getVisibility() != 0) {
            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                this.mButtonSetting.setVisibility(0);
            } else {
                this.mButtonSetting.post(new Runnable() {
                    public void run() {
                        ButtonManager.this.mButtonSetting.setVisibility(0);
                    }
                });
            }
        }
    }

    public void setToInitialState() {
        this.mModeOptions.setMainBar(0);
    }

    public void setExposureCompensationCallback(final ExposureCompensationSetCallback cb) {
        if (cb == null) {
            this.mModeOptionsExposure.setOnOptionClickListener(null);
        } else {
            this.mModeOptionsExposure.setOnOptionClickListener(new OnOptionClickListener() {
                public void onOptionClicked(View v) {
                    int comp = Integer.parseInt((String) v.getTag());
                    if (ButtonManager.this.mExposureCompensationStep != 0.0f) {
                        cb.setExposure(Math.round(((float) comp) / ButtonManager.this.mExposureCompensationStep));
                    }
                }
            });
        }
    }

    public void setExposureCompensationParameters(int min, int max, float step) {
        this.mMaxExposureCompensation = max;
        this.mMinExposureCompensation = min;
        this.mExposureCompensationStep = step;
        boolean z = false;
        setVisible(this.mExposureN2, Math.round(((float) min) * step) <= -2);
        setVisible(this.mExposureN1, Math.round(((float) min) * step) <= -1);
        setVisible(this.mExposureP1, Math.round(((float) max) * step) >= 1);
        ImageButton imageButton = this.mExposureP2;
        if (Math.round(((float) max) * step) >= 2) {
            z = true;
        }
        setVisible(imageButton, z);
        updateExposureButtons();
    }

    private static void setVisible(View v, boolean visible) {
        if (visible) {
            v.setVisibility(0);
        } else {
            v.setVisibility(4);
        }
    }

    public float getExposureCompensationStep() {
        return this.mExposureCompensationStep;
    }

    public boolean isEnabled(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        Integer enabledId = (Integer) button.getTag(R.string.tag_enabled_id);
        boolean z = false;
        if (enabledId == null) {
            return false;
        }
        if (enabledId.intValue() == buttonId && button.isEnabled()) {
            z = true;
        }
        return z;
    }

    public boolean isVisible(int buttonId) {
        View button;
        try {
            button = getButtonOrError(buttonId);
        } catch (IllegalArgumentException e) {
            button = getImageButtonOrError(buttonId);
        }
        return button.getVisibility() == 0;
    }

    public void initializeSettingButton(final Runnable runnable) {
        if (this.mButtonSetting.getVisibility() == 8) {
            this.mButtonSetting.setVisibility(0);
        }
        this.mButtonSetting.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (ButtonManager.this.isMoreOptionsWrapperShow()) {
                    ButtonManager.this.hideMoreOptionsWrapper();
                    return;
                }
                if (ButtonManager.this.mHelpTipsManager != null) {
                    ButtonManager.this.mHelpTipsManager.clickSettingResponse();
                }
                runnable.run();
                ButtonManager.this.mAppController.getCameraAppUI().slipDownShutterButton();
            }
        });
    }

    public void initializeExitMoreButton() {
        if (this.mExitMoreButton.getVisibility() == 8) {
            this.mExitMoreButton.setVisibility(0);
        }
        this.mExitMoreButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                int moduleIndex = ButtonManager.this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SQUARE_RETURN_TO_INDEX).intValue();
                if (ButtonManager.this.mAppController.getCurrentModuleIndex() == 15) {
                    ButtonManager.this.mAppController.getCameraAppUI().freezeGlSurface();
                }
                ButtonManager.this.mAppController.getCameraAppUI().onModeIdChanged(moduleIndex);
            }
        });
    }

    public void setExitMoreButtonVisible(int visible) {
        this.mExitMoreButton.setVisibility(visible);
    }

    public void initializeFlashButton(int buttonId, ButtonCallback cb) {
        if (this.mButtonFlash.getVisibility() != 0) {
            this.mButtonFlash.overrideImageIds(R.array.camera_flashmode_icons);
            this.mButtonFlash.overrideContentDescriptions(R.array.camera_flash_descriptions);
            if (this.mAppController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_HIDDEN, false)) {
                this.mAppController.getSettingsManager().setValueByIndex(getCameraScopes(0), Keys.KEY_FLASH_MODE, this.mAppController.getSettingsManager().getInteger(getCameraScopes(0), Keys.KEY_FLASH_MODE_BEFORE_BEAUTY).intValue());
                this.mAppController.getSettingsManager().setValueByIndex(getCameraScopes(1), Keys.KEY_FLASH_MODE, this.mAppController.getSettingsManager().getInteger(getCameraScopes(1), Keys.KEY_FLASH_MODE_BEFORE_BEAUTY).intValue());
                getButtonOrError(0).setState(this.mAppController.getSettingsManager().getInteger(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE_BEFORE_BEAUTY).intValue(), false);
            }
        }
        if (this.mAppController.getCurrentModuleIndex() != 5) {
            enableButton(0);
        }
        initializeFlashButton(this.mButtonFlash, cb, R.array.camera_flashmode_icons);
    }

    /* Access modifiers changed, original: protected */
    public void initializeFlashButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.camera_flash_descriptions);
        button.setSelectionModeClickListener(new OnClickListener() {
            public void onClick(View v) {
                ButtonManager.this.mCameraSelectionMenu.bindButtonAndIcons(ButtonManager.this.mAppController, button, R.array.camera_flashmode_selection_icons, R.array.camera_flashmode_selection_texts);
                ButtonManager.this.toggleSelectionMenu();
            }
        });
        int index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
        button.setState(index >= 0 ? index : 0, false, false);
        button.setOnStateChangeListener(new OnStateChangeListener() {
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, state);
                if ("on".equals(ButtonManager.this.mSettingsManager.getValueByIndex(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, state))) {
                    ButtonManager.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, ExtendKey.FLIP_MODE_OFF);
                    ButtonManager.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_LOWLIGHT, false);
                }
                if (cb != null) {
                    cb.onStateChanged(state);
                }
                if (ButtonManager.this.mHelpTipsManager != null) {
                    ButtonManager.this.mHelpTipsManager.notifyEventFinshed();
                }
            }
        });
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
        button.setOnUnhandledClickListener(new OnUnhandledClickListener() {
            public void unhandledClick() {
                if (cb != null && (cb instanceof ExtendButtonCallback)) {
                    ((ExtendButtonCallback) cb).onUnhandledClick();
                }
            }
        });
    }

    private void initializeTorchButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.video_flash_descriptions);
        button.setSelectionModeClickListener(new OnClickListener() {
            public void onClick(View v) {
                ButtonManager.this.mCameraSelectionMenu.bindButtonAndIcons(ButtonManager.this.mAppController, button, R.array.video_flashmode_selection_icons, R.array.video_flashmode_selection_texts);
                ButtonManager.this.toggleSelectionMenu();
            }
        });
        int index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE);
        button.setState(index >= 0 ? index : 0, false, false);
        button.setOnStateChangeListener(new OnStateChangeListener() {
            public void stateChanged(View view, int state) {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
                ButtonManager.this.mSettingsManager.setValueByIndex(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    private void initializeHdrPlusFlashButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.hdr_plus_flash_descriptions);
        button.setSelectionModeClickListener(new OnClickListener() {
            public void onClick(View v) {
                ButtonManager.this.mCameraSelectionMenu.bindButtonAndIcons(ButtonManager.this.mAppController, button, R.array.camera_flashmode_selection_icons, R.array.camera_flashmode_selection_texts);
                ButtonManager.this.toggleSelectionMenu();
            }
        });
        int index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getModuleScope(), Keys.KEY_HDR_PLUS_FLASH_MODE);
        button.setState(index >= 0 ? index : 0, false);
        button.setOnStateChangeListener(new OnStateChangeListener() {
            public void stateChanged(View view, int state) {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
                ButtonManager.this.mSettingsManager.setValueByIndex(ButtonManager.this.mAppController.getModuleScope(), Keys.KEY_HDR_PLUS_FLASH_MODE, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    private void initializeCameraButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("index = ");
        stringBuilder.append(index);
        Log.d(tag, stringBuilder.toString());
        button.setState(index >= 0 ? index : 0, false, false);
        button.setSelectionModeClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (ButtonManager.this.isMoreOptionsWrapperShow()) {
                    ButtonManager.this.hideMoreOptionsWrapper();
                    ButtonManager.this.mAppController.getLockEventListener().onIdle();
                } else if (ButtonManager.this.isFastClick()) {
                    ButtonManager.this.mAppController.getLockEventListener().onIdle();
                } else if (!ButtonManager.this.mBeautyEnable) {
                } else {
                    if (((CameraController) ButtonManager.this.mAppController.getCameraProvider()).isCameraOpenSuccess()) {
                        int cameraId;
                        if (ButtonManager.this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue() == 0) {
                            cameraId = 1;
                        } else {
                            cameraId = 0;
                        }
                        if (ButtonManager.this.mAppController.getCameraAppUI().getCameraGLSurfaceView().getVisibility() == 0) {
                            if (ButtonManager.this.mAppController.getCameraAppUI().getCameraGLSurfaceView().isCameraDrawable()) {
                                ButtonManager.this.mAppController.getCameraAppUI().getCameraGLSurfaceView().setDrawEnable(false);
                            } else {
                                return;
                            }
                        }
                        ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, cameraId);
                        Log.d(ButtonManager.TAG, "set button false");
                        if (cb != null) {
                            cb.onStateChanged(cameraId);
                        }
                        HelpTipsManager helpTipsManager = ButtonManager.this.mAppController.getHelpTipsManager();
                        if (helpTipsManager != null) {
                            helpTipsManager.openGestureHelpTip(cameraId);
                        }
                        ButtonManager.this.mBeautyEnable = true;
                        return;
                    }
                    ButtonManager.this.mAppController.getLockEventListener().onIdle();
                }
            }
        });
        button.setOnCancelSelectionMenu(new OnCancelSelectionMenuListener() {
            public void onCancelSelectionMenu() {
                ButtonManager.this.toggleSelectionMenu();
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    private void initializeSwitchButton(final MultiToggleImageButton button, ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID);
        button.setState(index >= 0 ? index : 0, false, false);
        button.setOnStateChangeListener(new OnStateChangeListener() {
            public void stateChanged(View view, int state) {
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, state);
                int cameraId = ButtonManager.this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
                ButtonManager.this.mAppController.getLockEventListener().onSwitching();
                ButtonManager.this.mAppController.switchToMode(ButtonManager.this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo), false);
                HelpTipsManager helpTipsManager = ButtonManager.this.mAppController.getHelpTipsManager();
                if (helpTipsManager != null) {
                    helpTipsManager.openGestureHelpTip(cameraId);
                }
            }
        });
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    private void initializeHdrPlusButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        initializeHdrPlusButtonIcons(button, resIdImages);
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS);
        button.setState(index >= 0 ? index : 0, false);
        button.setOnStateChangeListener(new OnStateChangeListener() {
            public void stateChanged(View view, int state) {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    private void initializeHdrPlusButtonIcons(MultiToggleImageButton button, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.hdr_plus_descriptions);
    }

    public void initializeHdrButton(int buttonId, ButtonCallback cb) {
        showButtons(5);
        this.mButtonHdr.setVisibility(0);
        initializeHdrButton(this.mButtonHdr, cb, R.array.hdr_icons);
    }

    public void initializeHdrButton(final MultiToggleImageButton button, ButtonCallback cb, int resIdImages) {
        this.mHDRCallback = cb;
        initializeHdrButtonIcons(button, resIdImages);
        button.setSelectionModeClickListener(new OnClickListener() {
            public void onClick(View v) {
                ButtonManager.this.mCameraSelectionMenu.bindButtonAndIcons(ButtonManager.this.mAppController, button, R.array.pref_camera_hdr_icons, R.array.pref_camera_hdr_texts);
                ButtonManager.this.toggleSelectionMenu();
            }
        });
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR);
        button.setState(index >= 0 ? index : 0, false);
        this.mHDRListener.stateChanged(button, index);
        button.setOnStateChangeListener(this.mHDRListener);
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    private void initializeHdrButtonIcons(MultiToggleImageButton button, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.hdr_descriptions);
    }

    private void initializeMotionButtonIcons(MultiToggleImageButton button, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.camera_motion_selection_icons_texts);
    }

    private void initializeCountdownButton(MultiToggleImageButton button, ButtonCallback cb, int resIdImages, int resIdContentDescriptions) {
        if (resIdContentDescriptions > 0) {
            button.overrideContentDescriptions(resIdContentDescriptions);
        }
        initializeCountdownButton(button, cb, resIdImages);
    }

    private void initializeCountdownButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        if (!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_VDF_COUNT_TIMER, false)) {
            int[] possibleTimer = this.mAppController.getAndroidContext().getResources().getIntArray(R.array.pref_countdown_duration);
            if (this.mAppController.getCurrentCameraId() == 0 || possibleTimer.length <= 2) {
                this.mSettingsManager.setDefaults(Keys.KEY_COUNTDOWN_DURATION, 0, possibleTimer);
            } else {
                android.util.Log.i("camera", "so for front camera set the second value as default");
                this.mSettingsManager.setDefaults(Keys.KEY_COUNTDOWN_DURATION, 0, possibleTimer);
            }
        }
        int index = this.mSettingsManager.getIndexOfCurrentValue(this.mAppController.getCameraScope(), Keys.KEY_COUNTDOWN_DURATION);
        button.setState(index >= 0 ? index : 0, false);
        button.setSelectionModeClickListener(new -$$Lambda$ButtonManager$SrXPkBNhjBTEIxBj45Eg7rEYaz4(this, button));
        button.setOnStateChangeListener(new OnStateChangeListener() {
            public void stateChanged(View view, int state) {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
                ButtonManager.this.mSettingsManager.setValueByIndex(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_COUNTDOWN_DURATION, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
                if (ButtonManager.this.mHelpTipsManager != null) {
                    ButtonManager.this.mHelpTipsManager.notifyEventFinshed();
                }
            }
        });
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    public static /* synthetic */ void lambda$initializeCountdownButton$3(ButtonManager buttonManager, MultiToggleImageButton button, View v) {
        buttonManager.mCameraSelectionMenu.bindButtonAndIcons(buttonManager.mAppController, button, R.array.countdown_duration_icons, R.array.countdown_duration_texts);
        buttonManager.toggleSelectionMenu();
    }

    private void initializeLowlightButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_LOWLIGHT);
        button.setState(index >= 0 ? index : 0, false);
        button.setOnStateChangeListener(new OnStateChangeListener() {
            public void stateChanged(View view, int state) {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_LOWLIGHT, state);
                if (SettingsManager.convertToBoolean(ButtonManager.this.mSettingsManager.getValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_LOWLIGHT, state))) {
                    ButtonManager.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, ExtendKey.FLIP_MODE_OFF);
                    ButtonManager.this.mSettingsManager.set(ButtonManager.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, ButtonManager.this.mAppController.getAndroidContext().getString(R.string.pref_camera_flashmode_off));
                }
                if (cb != null) {
                    cb.onStateChanged(state);
                }
                if (ButtonManager.this.mHelpTipsManager != null) {
                    ButtonManager.this.mHelpTipsManager.notifyEventFinshed();
                }
            }
        });
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    public void updateExposureButtons() {
        int compValue = this.mSettingsManager.getInteger(this.mAppController.getCameraScope(), Keys.KEY_EXPOSURE).intValue();
        if (this.mExposureCompensationStep != 0.0f) {
            this.mModeOptionsExposure.setSelectedOptionByTag(String.valueOf(Math.round(((float) compValue) * this.mExposureCompensationStep)));
        }
    }

    private void initializeGridLinesButton(final MultiToggleImageButton button, final ButtonCallback cb, int resIdImages) {
        if (resIdImages > 0) {
            button.overrideImageIds(resIdImages);
        }
        button.overrideContentDescriptions(R.array.grid_lines_descriptions);
        button.setOnStateChangeListener(new OnStateChangeListener() {
            public void stateChanged(View view, int state) {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
                ButtonManager.this.mSettingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_GRID_LINES, state);
                if (cb != null) {
                    cb.onStateChanged(state);
                }
            }
        });
        int index = this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_GRID_LINES);
        button.setState(index >= 0 ? index : 0, true);
        button.setOnTouchListener(new OnTouchListener() {
            public void onTouchDown() {
                ButtonManager.this.mAppController.getLockEventListener().onMenuClicked(button.hashCode());
            }

            public void onTouchUp() {
                ButtonManager.this.mAppController.getLockEventListener().onIdle();
            }
        });
    }

    public boolean isPanoEnabled() {
        return this.mModeOptions.getMainBar() == 1;
    }

    /* JADX WARNING: Failed to extract finally block: empty outs */
    /* JADX WARNING: Missing block: B:36:?, code skipped:
            return;
     */
    public void initializePanoOrientationButtons(final com.hmdglobal.app.camera.ButtonManager.ButtonCallback r15) {
        /*
        r14 = this;
        r0 = com.hmdglobal.app.camera.util.PhotoSphereHelper.getPanoramaOrientationOptionArrayId();
        r1 = com.hmdglobal.app.camera.util.PhotoSphereHelper.getPanoramaOrientationDescriptions();
        if (r0 <= 0) goto L_0x00b8;
    L_0x000a:
        r2 = 0;
        r3 = 0;
        r4 = r14.mModeOptions;	 Catch:{ all -> 0x00ac }
        r5 = 1;
        r4.setMainBar(r5);	 Catch:{ all -> 0x00ac }
        r4 = r14.mAppController;	 Catch:{ all -> 0x00ac }
        r4 = r4.getAndroidContext();	 Catch:{ all -> 0x00ac }
        r4 = r4.getResources();	 Catch:{ all -> 0x00ac }
        r4 = r4.obtainTypedArray(r0);	 Catch:{ all -> 0x00ac }
        r2 = r4;
        r4 = r14.mAppController;	 Catch:{ all -> 0x00ac }
        r4 = r4.getAndroidContext();	 Catch:{ all -> 0x00ac }
        r4 = r4.getResources();	 Catch:{ all -> 0x00ac }
        r4 = r4.obtainTypedArray(r1);	 Catch:{ all -> 0x00ac }
        r3 = r4;
        r4 = r14.mModeOptionsPano;	 Catch:{ all -> 0x00ac }
        r4.removeAllViews();	 Catch:{ all -> 0x00ac }
        r4 = r14.mModeOptionsPano;	 Catch:{ all -> 0x00ac }
        r4 = r4.getOrientation();	 Catch:{ all -> 0x00ac }
        r6 = 0;
        if (r4 != 0) goto L_0x0040;
    L_0x003e:
        r4 = r5;
        goto L_0x0041;
    L_0x0040:
        r4 = r6;
    L_0x0041:
        r7 = r2.length();	 Catch:{ all -> 0x00ac }
        r8 = r6;
    L_0x0046:
        if (r8 >= r7) goto L_0x008f;
    L_0x0048:
        if (r4 == 0) goto L_0x004c;
    L_0x004a:
        r9 = r8;
        goto L_0x004f;
    L_0x004c:
        r9 = r7 - r8;
        r9 = r9 - r5;
    L_0x004f:
        r10 = r2.getResourceId(r9, r6);	 Catch:{ all -> 0x00ac }
        if (r10 <= 0) goto L_0x008c;
    L_0x0055:
        r11 = r14.mAppController;	 Catch:{ all -> 0x00ac }
        r11 = r11.getAndroidContext();	 Catch:{ all -> 0x00ac }
        r11 = android.view.LayoutInflater.from(r11);	 Catch:{ all -> 0x00ac }
        r12 = 2131361888; // 0x7f0a0060 float:1.8343541E38 double:1.0530326877E-314;
        r13 = r14.mModeOptionsPano;	 Catch:{ all -> 0x00ac }
        r11 = r11.inflate(r12, r13, r6);	 Catch:{ all -> 0x00ac }
        r11 = (android.widget.ImageButton) r11;	 Catch:{ all -> 0x00ac }
        r11.setImageResource(r10);	 Catch:{ all -> 0x00ac }
        r12 = java.lang.String.valueOf(r9);	 Catch:{ all -> 0x00ac }
        r11.setTag(r12);	 Catch:{ all -> 0x00ac }
        r12 = r14.mModeOptionsPano;	 Catch:{ all -> 0x00ac }
        r12.addView(r11);	 Catch:{ all -> 0x00ac }
        r12 = r3.getResourceId(r9, r6);	 Catch:{ all -> 0x00ac }
        if (r12 <= 0) goto L_0x008c;
    L_0x007f:
        r13 = r14.mAppController;	 Catch:{ all -> 0x00ac }
        r13 = r13.getAndroidContext();	 Catch:{ all -> 0x00ac }
        r13 = r13.getString(r12);	 Catch:{ all -> 0x00ac }
        r11.setContentDescription(r13);	 Catch:{ all -> 0x00ac }
    L_0x008c:
        r8 = r8 + 1;
        goto L_0x0046;
    L_0x008f:
        r5 = r14.mModeOptionsPano;	 Catch:{ all -> 0x00ac }
        r5.updateListeners();	 Catch:{ all -> 0x00ac }
        r5 = r14.mModeOptionsPano;	 Catch:{ all -> 0x00ac }
        r6 = new com.hmdglobal.app.camera.ButtonManager$45;	 Catch:{ all -> 0x00ac }
        r6.<init>(r15);	 Catch:{ all -> 0x00ac }
        r5.setOnOptionClickListener(r6);	 Catch:{ all -> 0x00ac }
        r14.updatePanoButtons();	 Catch:{ all -> 0x00ac }
        if (r2 == 0) goto L_0x00a6;
    L_0x00a3:
        r2.recycle();
    L_0x00a6:
        if (r3 == 0) goto L_0x00b8;
    L_0x00a8:
        r3.recycle();
        goto L_0x00b8;
    L_0x00ac:
        r4 = move-exception;
        if (r2 == 0) goto L_0x00b2;
    L_0x00af:
        r2.recycle();
    L_0x00b2:
        if (r3 == 0) goto L_0x00b7;
    L_0x00b4:
        r3.recycle();
    L_0x00b7:
        throw r4;
    L_0x00b8:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ButtonManager.initializePanoOrientationButtons(com.hmdglobal.app.camera.ButtonManager$ButtonCallback):void");
    }

    public void setSeekbarProgress(int length) {
        if (this.mBeautySeekbar != null) {
            this.mBeautySeekbar.setProgress(length);
        }
    }

    private void updatePanoButtons() {
        this.mModeOptionsPano.setSelectedOptionByTag(String.valueOf(this.mSettingsManager.getIndexOfCurrentValue(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_PANO_ORIENTATION)));
    }

    public void setHelpTipListener(HelpTipsManager manager) {
        this.mHelpTipsManager = manager;
    }

    public void hideLiveBolkenButton() {
        if (this.mButtonBolken != null) {
            this.mButtonBolken.setVisibility(8);
        }
    }

    public void showLiveBolkenButton() {
        if (this.mButtonBolken != null) {
            this.mButtonBolken.setVisibility(0);
        }
    }

    public void showLiveBolkenLayout() {
        if (this.mBolkenLayout != null && this.mButtonBolken != null) {
            this.mButtonBolken.setState(1);
            this.mBolkenLayout.setVisibility(0);
            this.mBolkenLayout.requestLayout();
        }
    }

    public void hideLiveBolkenLayout() {
        if (this.mBolkenLayout != null) {
            if (this.mButtonBolken != null && this.mButtonBolken.getVisibility() == 0) {
                this.mButtonBolken.setState(0);
            }
            this.mBolkenLayout.setVisibility(8);
        }
    }

    public void setIndicatorTextVisible(int visible) {
        this.mIndicatorText.setVisibility(visible);
    }

    public void setIndicatorText(String text) {
        this.mIndicatorText.setText(text);
    }

    public void setLiveBokehLevelSeeker(int progress) {
        if (this.mBolkenSeekbar != null) {
            this.mBolkenSeekbar.setProgress(9 - (progress + 1));
        }
    }
}
