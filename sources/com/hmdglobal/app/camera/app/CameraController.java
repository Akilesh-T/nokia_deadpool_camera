package com.hmdglobal.app.camera.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewDataCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraExceptionHandler;
import com.android.ex.camera2.portability.CameraSettings.BoostParameters;
import com.hmdglobal.app.camera.CameraDisabledException;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.PictureSizePerso;

public class CameraController implements CameraOpenCallback, CameraProvider {
    private static final int EMPTY_REQUEST = -1;
    private static final Tag TAG = new Tag("CameraController");
    private final Handler mCallbackHandler;
    private CameraOpenCallback mCallbackReceiver;
    private final CameraAgent mCameraAgent;
    private final CameraAgent mCameraAgentNg;
    private CameraProxy mCameraProxy;
    private boolean mCameraReleased = false;
    private final Context mContext;
    private CameraDeviceInfo mInfo;
    private boolean mIsBoostPreview = false;
    private boolean mIsBoostedFromCreate = false;
    private boolean mIsCameraOpen = false;
    private int mRequestingCameraId = -1;
    private boolean mUsingNewApi = false;
    private boolean manualModeOn = false;
    private boolean squareModeOn = false;

    public CameraController(Context context, CameraOpenCallback callbackReceiver, Handler handler, CameraAgent cameraManager, CameraAgent cameraManagerNg) {
        this.mContext = context;
        this.mCallbackReceiver = callbackReceiver;
        this.mCallbackHandler = handler;
        this.mCameraAgent = cameraManager;
        this.mCameraAgentNg = cameraManagerNg != cameraManager ? cameraManagerNg : null;
        this.mInfo = this.mCameraAgent.getCameraDeviceInfo();
        if (this.mInfo == null && this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onDeviceOpenFailure(-1, "GETTING_CAMERA_INFO");
        }
        this.mCameraReleased = false;
    }

    public void setSquareModeOn(boolean squaremode) {
        this.squareModeOn = squaremode;
    }

    public void setManualModeOn(boolean maualmode) {
        this.manualModeOn = maualmode;
    }

    public void setCameraExceptionHandler(CameraExceptionHandler exceptionHandler) {
        this.mCameraAgent.setCameraExceptionHandler(exceptionHandler);
        if (this.mCameraAgentNg != null) {
            this.mCameraAgentNg.setCameraExceptionHandler(exceptionHandler);
        }
    }

    public Characteristics getCharacteristics(int cameraId) {
        if (this.mInfo == null) {
            return null;
        }
        return this.mInfo.getCharacteristics(cameraId);
    }

    public int getCurrentCameraId() {
        if (this.mCameraProxy != null) {
            return this.mCameraProxy.getCameraId();
        }
        Log.v(TAG, "getCurrentCameraId without an open camera... returning requested id");
        return this.mRequestingCameraId;
    }

    public int getNumberOfCameras() {
        if (this.mInfo == null) {
            return 0;
        }
        return this.mInfo.getNumberOfCameras();
    }

    public int getFirstBackCameraId() {
        if (this.mInfo == null) {
            return -1;
        }
        return this.mInfo.getFirstBackCameraId();
    }

    public int getFirstFrontCameraId() {
        if (this.mInfo == null) {
            return -1;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getFirstFrontCameraId = ");
        stringBuilder.append(this.mInfo.getFirstFrontCameraId());
        Log.v(tag, stringBuilder.toString());
        return this.mInfo.getFirstFrontCameraId();
    }

    public boolean isFrontFacingCamera(int id) {
        if (this.mInfo == null) {
            return false;
        }
        if (id < this.mInfo.getNumberOfCameras() && this.mInfo.getCharacteristics(id) != null) {
            return this.mInfo.getCharacteristics(id).isFacingFront();
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera info not available:");
        stringBuilder.append(id);
        Log.e(tag, stringBuilder.toString());
        return false;
    }

    public boolean isBackFacingCamera(int id) {
        if (this.mInfo == null) {
            return false;
        }
        if (id < this.mInfo.getNumberOfCameras() && this.mInfo.getCharacteristics(id) != null) {
            return this.mInfo.getCharacteristics(id).isFacingBack();
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera info not available:");
        stringBuilder.append(id);
        Log.e(tag, stringBuilder.toString());
        return false;
    }

    public void onCameraOpened(CameraProxy camera) {
        Log.v(TAG, "onCameraOpened");
        if (this.mRequestingCameraId == camera.getCameraId()) {
            this.mCameraProxy = camera;
            this.mRequestingCameraId = -1;
            PictureSizePerso perso = PictureSizePerso.getInstance();
            perso.init(this.mContext, camera.getCapabilities().getSupportedPhotoSizes(), 0);
            perso.init(this.mContext, camera.getCapabilities().getSupportedPhotoSizes(), 1);
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("support size:");
            stringBuilder.append(camera.getCapabilities().getSupportedPhotoSizes());
            Log.d(tag, stringBuilder.toString());
            if (this.mCallbackReceiver != null) {
                this.mCallbackReceiver.onCameraOpened(camera);
            }
            this.mIsCameraOpen = true;
        }
    }

    public void onCameraOpenedBoost(CameraProxy camera) {
    }

    public void onCameraDisabled(int cameraId) {
        if (this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onCameraDisabled(cameraId);
        }
    }

    public void onDeviceOpenFailure(int cameraId, String info) {
        if (this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onDeviceOpenFailure(cameraId, info);
        }
        if (this.mContext != null) {
            String ACTION_CRASH_REPORT_FOR_SALE_MODE = "android.intent.action.CRASH_REPORT_FOR_SALE_MODE";
            String EXTRA_FOR_SALE_MODE = "extra_process_name_and_type";
            Intent intent = new Intent("android.intent.action.CRASH_REPORT_FOR_SALE_MODE");
            intent.putExtra("extra_process_name_and_type", info);
            this.mContext.sendBroadcast(intent);
        }
    }

    public void onDeviceOpenedAlready(int cameraId, String info) {
        if (this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onDeviceOpenedAlready(cameraId, info);
        }
    }

    public void onReconnectionFailure(CameraAgent mgr, String info) {
        if (this.mCallbackReceiver != null) {
            this.mCallbackReceiver.onReconnectionFailure(mgr, info);
        }
    }

    public void onCameraRequested() {
        this.mCameraReleased = false;
    }

    public void onCameraClosed() {
        this.mCameraReleased = true;
    }

    public boolean isReleased() {
        return this.mCameraReleased;
    }

    public boolean isCameraRequestBoosted() {
        return this.mIsBoostedFromCreate;
    }

    public boolean isBoostPreview() {
        return this.mIsBoostPreview;
    }

    public boolean isCameraOpenSuccess() {
        return this.mIsCameraOpen;
    }

    public Context getCallbackContext() {
        return this.mContext;
    }

    public BoostParameters getBoostParam() {
        return null;
    }

    public void requestCamera(int id) {
        requestCamera(id, false);
    }

    /* JADX WARNING: Missing block: B:42:0x00c0, code skipped:
            return;
     */
    public void requestCamera(int r10, boolean r11, boolean r12, com.android.ex.camera2.portability.CameraSettings.BoostParameters r13) {
        /*
        r9 = this;
        r0 = 0;
        r9.mIsCameraOpen = r0;
        r1 = TAG;
        r2 = "requestCamera";
        com.hmdglobal.app.camera.debug.Log.v(r1, r2);
        r9.mIsBoostedFromCreate = r12;
        r9.mIsBoostPreview = r12;
        r1 = r9.mRequestingCameraId;
        r2 = -1;
        if (r1 != r2) goto L_0x00c0;
    L_0x0013:
        r1 = r9.mRequestingCameraId;
        if (r1 != r10) goto L_0x0019;
    L_0x0017:
        goto L_0x00c0;
    L_0x0019:
        r1 = r9.mInfo;
        if (r1 != 0) goto L_0x001e;
    L_0x001d:
        return;
    L_0x001e:
        r1 = r9.mContext;
        r2 = "android.permission.CAMERA";
        r1 = com.hmdglobal.app.camera.util.PermissionsUtil.isPermissionGranted(r1, r2);
        if (r1 != 0) goto L_0x0029;
    L_0x0028:
        return;
    L_0x0029:
        r9.mRequestingCameraId = r10;
        r1 = r9.mCameraAgentNg;
        if (r1 == 0) goto L_0x0033;
    L_0x002f:
        if (r11 == 0) goto L_0x0033;
    L_0x0031:
        r1 = 1;
        goto L_0x0034;
    L_0x0033:
        r1 = r0;
    L_0x0034:
        r11 = r1;
        if (r11 == 0) goto L_0x003a;
    L_0x0037:
        r1 = r9.mCameraAgentNg;
        goto L_0x003c;
    L_0x003a:
        r1 = r9.mCameraAgent;
    L_0x003c:
        r9.onCameraRequested();
        r2 = r9.mCameraProxy;
        if (r2 != 0) goto L_0x005a;
    L_0x0043:
        if (r12 == 0) goto L_0x0052;
    L_0x0045:
        r2 = r9.mContext;
        r5 = r9.mCallbackHandler;
        r3 = r1;
        r4 = r10;
        r6 = r9;
        r7 = r13;
        checkAndBoostOpenCamera(r2, r3, r4, r5, r6, r7);
        goto L_0x00b7;
    L_0x0052:
        r0 = r9.mContext;
        r2 = r9.mCallbackHandler;
        checkAndOpenCamera(r0, r1, r10, r2, r9);
        goto L_0x00b7;
    L_0x005a:
        r2 = r9.mCameraProxy;
        r2 = r2.getCameraId();
        if (r2 != r10) goto L_0x0081;
    L_0x0062:
        r2 = r9.mUsingNewApi;
        if (r2 != r11) goto L_0x0081;
    L_0x0066:
        r2 = r9.squareModeOn;
        if (r2 != 0) goto L_0x0081;
    L_0x006a:
        r2 = r9.manualModeOn;
        if (r2 == 0) goto L_0x006f;
    L_0x006e:
        goto L_0x0081;
    L_0x006f:
        r0 = TAG;
        r2 = "reconnecting to use the existing camera";
        com.hmdglobal.app.camera.debug.Log.v(r0, r2);
        r0 = r9.mCameraProxy;
        r2 = r9.mCallbackHandler;
        r0.reconnect(r2, r9);
        r0 = 0;
        r9.mCameraProxy = r0;
        goto L_0x00b7;
    L_0x0081:
        r2 = r9.mContext;
        r8 = com.hmdglobal.app.camera.util.GservicesHelper.useCamera2ApiThroughPortabilityLayer(r2);
        r2 = TAG;
        r3 = "different camera already opened, closing then reopening";
        com.hmdglobal.app.camera.debug.Log.v(r2, r3);
        r2 = r9.mUsingNewApi;
        if (r2 == 0) goto L_0x009a;
    L_0x0092:
        r2 = r9.mCameraAgentNg;
        r3 = r9.mCameraProxy;
        r2.closeCamera(r3, r0);
        goto L_0x00a1;
    L_0x009a:
        r0 = r9.mCameraAgent;
        r2 = r9.mCameraProxy;
        r0.closeCamera(r2, r8);
    L_0x00a1:
        if (r12 == 0) goto L_0x00af;
    L_0x00a3:
        r2 = r9.mContext;
        r5 = r9.mCallbackHandler;
        r3 = r1;
        r4 = r10;
        r6 = r9;
        r7 = r13;
        checkAndBoostOpenCamera(r2, r3, r4, r5, r6, r7);
        goto L_0x00b6;
    L_0x00af:
        r0 = r9.mContext;
        r2 = r9.mCallbackHandler;
        checkAndOpenCamera(r0, r1, r10, r2, r9);
    L_0x00b7:
        r9.mUsingNewApi = r11;
        r0 = r1.getCameraDeviceInfo();
        r9.mInfo = r0;
        return;
    L_0x00c0:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.app.CameraController.requestCamera(int, boolean, boolean, com.android.ex.camera2.portability.CameraSettings$BoostParameters):void");
    }

    public void requestCamera(int id, boolean useNewApi) {
        requestCamera(id, useNewApi, false, null);
    }

    public void boostSetPreviewTexture(SurfaceTexture surfaceTexture) {
        this.mCameraAgent.setPreviewTexture(surfaceTexture);
    }

    public void boostApplySettings(BoostParameters settings) {
        this.mIsBoostPreview = true;
        this.mCameraAgent.applySettings(settings);
    }

    public void boostStartPreview(CameraStartPreviewCallback callback) {
        this.mIsBoostPreview = true;
        this.mCameraAgent.startPreviewAsync(this.mCallbackHandler, callback);
    }

    public void clearBoostPreview() {
        this.mIsBoostPreview = false;
    }

    public boolean waitingForCamera() {
        return this.mRequestingCameraId != -1;
    }

    public void releaseCamera(int id) {
        this.mIsBoostPreview = false;
        this.mIsBoostedFromCreate = false;
        if (this.mCameraProxy == null) {
            if (this.mRequestingCameraId == -1) {
                Log.w(TAG, "Trying to release the camera before requesting");
            }
            this.mRequestingCameraId = -1;
        } else if (this.mCameraProxy.getCameraId() == id) {
            this.mRequestingCameraId = -1;
        } else {
            throw new IllegalStateException("Trying to release an unopened camera.");
        }
    }

    public void removeCallbackReceiver() {
        this.mCallbackReceiver = null;
    }

    public void closeCamera(boolean synced) {
        Log.v(TAG, "Closing camera");
        this.mIsBoostedFromCreate = false;
        this.mCameraProxy = null;
        onCameraClosed();
        if (this.mUsingNewApi) {
            this.mCameraAgentNg.closeCamera(this.mCameraProxy, synced);
        } else {
            this.mCameraAgent.closeCamera(this.mCameraProxy, synced);
        }
        this.mRequestingCameraId = -1;
        this.mUsingNewApi = false;
    }

    private static void checkAndOpenCamera(Context context, CameraAgent cameraManager, final int cameraId, Handler handler, final CameraOpenCallback cb) {
        Log.v(TAG, "checkAndOpenCamera");
        try {
            CameraUtil.throwIfCameraDisabled(context);
            cameraManager.openCamera(handler, cameraId, cb);
        } catch (CameraDisabledException e) {
            handler.post(new Runnable() {
                public void run() {
                    cb.onCameraDisabled(cameraId);
                }
            });
        }
    }

    private static void checkAndBoostOpenCamera(Context context, CameraAgent cameraManager, final int cameraId, Handler handler, final CameraOpenCallback cb, BoostParameters parameters) {
        Log.v(TAG, "checkAndOpenCamera");
        try {
            CameraUtil.throwIfCameraDisabled(context);
            cameraManager.openCameraBoost(handler, cameraId, cb, parameters);
        } catch (CameraDisabledException e) {
            handler.post(new Runnable() {
                public void run() {
                    cb.onCameraDisabled(cameraId);
                }
            });
        }
    }

    public void setOneShotPreviewCallback(Handler handler, CameraPreviewDataCallback cb) {
        this.mCameraProxy.setOneShotPreviewCallback(handler, cb);
    }

    public int getSupportedHardwareLevel(int id) {
        if (this.mInfo == null) {
            return 2;
        }
        return this.mInfo.getCharacteristics(id).getSupportedHardwareLevel(id);
    }
}
