package com.hmdglobal.app.camera;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraProvider;
import com.hmdglobal.app.camera.app.CameraServices;
import com.hmdglobal.app.camera.module.ModuleController;

public abstract class CameraModule implements ModuleController {
    private final CameraProvider mCameraProvider;
    protected final CameraServices mServices;

    public abstract int getModuleId();

    public abstract String getPeekAccessibilityString();

    @Deprecated
    public abstract boolean onKeyDown(int i, KeyEvent keyEvent);

    @Deprecated
    public abstract boolean onKeyUp(int i, KeyEvent keyEvent);

    @Deprecated
    public abstract void onSingleTapUp(View view, int i, int i2);

    public CameraModule(AppController app) {
        this.mServices = app.getServices();
        this.mCameraProvider = app.getCameraProvider();
    }

    public boolean onBackPressed() {
        return false;
    }

    public void onPreviewVisibilityChanged(int visibility) {
    }

    /* Access modifiers changed, original: protected */
    public boolean getCameraPreviewState() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public CameraServices getServices() {
        return this.mServices;
    }

    /* Access modifiers changed, original: protected */
    public CameraProvider getCameraProvider() {
        return this.mCameraProvider;
    }

    /* Access modifiers changed, original: protected */
    public void requestBackCamera() {
        int backCameraId = this.mCameraProvider.getFirstBackCameraId();
        if (backCameraId != -1) {
            this.mCameraProvider.requestCamera(backCameraId);
        }
    }

    public void onPreviewInitialDataReceived() {
    }

    /* Access modifiers changed, original: protected */
    public void releaseBackCamera() {
        int backCameraId = this.mCameraProvider.getFirstBackCameraId();
        if (backCameraId != -1) {
            this.mCameraProvider.releaseCamera(backCameraId);
        }
    }

    public boolean isZslOn() {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void onMediaAction(Context context, Intent intent) {
    }

    public void preparePause() {
    }

    public boolean isSupportBeauty() {
        return false;
    }

    public void openOrCloseEffects(int state, int effects) {
    }

    public boolean onGLRenderEnable() {
        return false;
    }
}
