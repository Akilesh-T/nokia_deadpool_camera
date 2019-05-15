package com.hmdglobal.app.camera.module;

import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.ShutterButton.OnShutterButtonListener;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.hardware.HardwareSpec;
import com.hmdglobal.app.camera.settings.SettingsManager;

public interface ModuleController extends OnShutterButtonListener {
    public static final int VISIBILITY_COVERED = 1;
    public static final int VISIBILITY_HIDDEN = 2;
    public static final int VISIBILITY_VISIBLE = 0;

    void destroy();

    BottomBarUISpec getBottomBarSpec();

    HardwareSpec getHardwareSpec();

    String getModuleStringIdentifier();

    void hardResetSettings(SettingsManager settingsManager);

    void init(CameraActivity cameraActivity, boolean z, boolean z2);

    boolean isCameraOpened();

    boolean isPaused();

    boolean isUsingBottomBar();

    boolean onBackPressed();

    void onCameraAvailable(CameraProxy cameraProxy);

    void onLayoutOrientationChanged(boolean z);

    void onOrientationChanged(int i);

    void onPreviewVisibilityChanged(int i);

    void pause() throws InterruptedException;

    void preparePause();

    void resume();
}
