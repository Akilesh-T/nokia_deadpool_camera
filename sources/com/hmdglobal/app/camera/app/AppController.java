package com.hmdglobal.app.camera.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.ButtonManager;
import com.hmdglobal.app.camera.HelpTipsManager;
import com.hmdglobal.app.camera.SoundPlayer;
import com.hmdglobal.app.camera.app.CameraAppUI.LockEventListener;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.one.OneCameraManager;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.AbstractTutorialOverlay;
import com.hmdglobal.app.camera.ui.Lockable;
import com.hmdglobal.app.camera.ui.ModeTransitionView.OnTransAnimationListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;

public interface AppController {
    public static final boolean ENABLE_BLUR_TRANS = true;
    public static final int NOTIFY_NEW_MEDIA_ACTION_ANIMATION = 1;
    public static final int NOTIFY_NEW_MEDIA_ACTION_NONE = 0;
    public static final int NOTIFY_NEW_MEDIA_ACTION_OPTIMIZECAPTURE = 4;
    public static final int NOTIFY_NEW_MEDIA_ACTION_UPDATETHUMB = 2;
    public static final int NOTIFY_NEW_MEDIA_DEFALT_ACTION = 3;

    public interface ShutterEventsListener {
        void onShutterClicked();

        void onShutterLongPressed();

        void onShutterPressed();

        void onShutterReleased();
    }

    void addLockableToListenerPool(Lockable lockable);

    void addPreviewAreaSizeChangedListener(PreviewAreaChangedListener previewAreaChangedListener);

    void addRotatableToListenerPool(RotateEntity rotateEntity);

    void cancelPostCaptureAnimation();

    void cancelPreCaptureAnimation();

    void enableKeepScreenOn(boolean z);

    void freezeScreenUntilPreviewReady();

    void freezeScreenUntilPreviewReady(OnTransAnimationListener onTransAnimationListener);

    void freezeScreenUntilWithoutBlur();

    void freezeScreenWithoutBlurUntilAnimationDone(OnTransAnimationListener onTransAnimationListener);

    Context getAndroidContext();

    ButtonManager getButtonManager();

    CameraAppUI getCameraAppUI();

    OneCameraManager getCameraManager();

    CameraProvider getCameraProvider();

    String getCameraScope();

    int getCurrentCameraId();

    ModuleController getCurrentModuleController();

    int getCurrentModuleIndex();

    RectF getFullscreenRect();

    HelpTipsManager getHelpTipsManager();

    LocationManager getLocationManager();

    LockEventListener getLockEventListener();

    FrameLayout getModuleLayoutRoot();

    ModuleManager getModuleManager();

    String getModuleScope();

    OrientationManager getOrientationManager();

    int getPreferredChildModeIndex(int i);

    SurfaceTexture getPreviewBuffer();

    int getQuickSwitchToModuleId(int i);

    CameraServices getServices();

    SettingsManager getSettingsManager();

    SoundPlayer getSoundPlayer();

    int getSupportedHardwarelevel(int i);

    void intentReviewCancel();

    void intentReviewDone();

    void intentReviewPlay();

    void intentReviewRetake();

    boolean isAutoRotateScreen();

    boolean isPaused();

    boolean isReversibleWorking();

    boolean isShutterEnabled();

    void launchActivityByIntent(Intent intent);

    Integer lockModuleSelection();

    void lockOrientation();

    void lockRotatableOrientation(int i);

    void notifyNewMedia(Uri uri);

    void notifyNewMedia(Uri uri, int i);

    void onModeSelected(int i);

    void onModeSelecting();

    void onModeSelecting(boolean z);

    void onModeSelecting(boolean z, OnTransAnimationListener onTransAnimationListener);

    boolean onPeekThumbClicked(Uri uri);

    void onPreviewReadyToStart();

    void onPreviewStarted();

    void onSettingsSelected();

    void onVideoRecordingStarted();

    void onVideoRecordingStop();

    void openContextMenu(View view);

    void openOrCloseEffects(int i, int i2, int i3);

    void registerForContextMenu(View view);

    void removeLockableFromListenerPool(Lockable lockable);

    void removePreviewAreaSizeChangedListener(PreviewAreaChangedListener previewAreaChangedListener);

    void removeRotatableFromListenerPool(int i);

    void setPreviewStatusListener(PreviewStatusListener previewStatusListener);

    void setShutterButtonLongClickable(boolean z);

    void setShutterEnabled(boolean z);

    void setShutterEnabledWithNormalAppearence(boolean z);

    void setShutterEventsListener(ShutterEventsListener shutterEventsListener);

    void setShutterPress(boolean z);

    void setupOneShotPreviewListener();

    void showErrorAndFinish(int i);

    void showTutorial(AbstractTutorialOverlay abstractTutorialOverlay);

    void startPostCaptureAnimation();

    void startPostCaptureAnimation(Bitmap bitmap);

    void startPreCaptureAnimation();

    void startPreCaptureAnimation(boolean z);

    void switchToMode(int i);

    void switchToMode(int i, boolean z);

    boolean unlockModuleSelection(Integer num);

    void unlockOrientation();

    void unlockRotatableOrientation(int i);

    void updatePreviewAspectRatio(float f);

    void updatePreviewTransform(Matrix matrix);

    void updatePreviewTransformFullscreen(Matrix matrix, float f);
}
