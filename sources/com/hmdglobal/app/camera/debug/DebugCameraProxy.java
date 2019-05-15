package com.hmdglobal.app.camera.debug;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.view.SurfaceHolder;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraAFCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraAFMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraFaceDetectionCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraFinalPreviewCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraGDCallBack;
import com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPanoramaCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPanoramaMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewDataCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewResultCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraShutterCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback;
import com.android.ex.camera2.portability.CameraAgent.CaptureCompleteCallBack;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.CameraStateHolder;
import com.android.ex.camera2.portability.DispatchThread;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class DebugCameraProxy extends CameraProxy {
    private final CameraProxy mProxy;
    private final Tag mTag;

    public DebugCameraProxy(Tag tag, CameraProxy proxy) {
        this.mTag = tag;
        this.mProxy = proxy;
    }

    public Camera getCamera() {
        log("getCamera");
        return this.mProxy.getCamera();
    }

    public int getCameraId() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCameraId: ");
        stringBuilder.append(this.mProxy.getCameraId());
        log(stringBuilder.toString());
        return this.mProxy.getCameraId();
    }

    public void setModuleId(int id, int oriention) {
    }

    public Characteristics getCharacteristics() {
        log("getCharacteristics");
        return this.mProxy.getCharacteristics();
    }

    public CameraAgent getAgent() {
        log("getAgent");
        return this.mProxy.getAgent();
    }

    public CameraCapabilities getCapabilities() {
        log("getCapabilities");
        return this.mProxy.getCapabilities();
    }

    public void reconnect(Handler handler, CameraOpenCallback cb) {
        log("reconnect");
        this.mProxy.reconnect(handler, cb);
    }

    public void unlock() {
        log("unlock");
        this.mProxy.unlock();
    }

    public void lock() {
        log("lock");
        this.mProxy.lock();
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        log("setPreviewTexture");
        this.mProxy.setPreviewTexture(surfaceTexture);
    }

    public void setPreviewTextureSync(SurfaceTexture surfaceTexture) {
        log("setPreviewTextureSync");
        this.mProxy.setPreviewTextureSync(surfaceTexture);
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        log("setPreviewDisplay");
        this.mProxy.setPreviewDisplay(surfaceHolder);
    }

    public void startPreview() {
        log("startPreview");
        this.mProxy.startPreview();
    }

    public void startPreviewWithCallback(Handler h, CameraStartPreviewCallback cb) {
        log("startPreviewWithCallback");
        this.mProxy.startPreviewWithCallback(h, cb);
    }

    public void stopPreview() {
        log("stopPreview");
        this.mProxy.stopPreview();
    }

    public void setPreviewDataCallback(Handler handler, CameraPreviewDataCallback cb) {
        log("setPreviewDataCallback");
        this.mProxy.setPreviewDataCallback(handler, cb);
    }

    public void setPreviewResultCallback(Handler handler, CameraPreviewResultCallback cb) {
        log("setPreviewDataCallback");
        this.mProxy.setPreviewResultCallback(handler, cb);
    }

    public void setCaptureResultCallback(Handler handler, CaptureCompleteCallBack cb) {
        log("setPreviewDataCallback");
        this.mProxy.setCaptureResultCallback(handler, cb);
    }

    public void setFinalPreviewCallback(CameraFinalPreviewCallback cb) {
        log("setFinalPreviewCallback");
        this.mProxy.setFinalPreviewCallback(cb);
    }

    public void setOneShotPreviewCallback(Handler handler, CameraPreviewDataCallback cb) {
        log("setOneShotPreviewCallback");
        this.mProxy.setOneShotPreviewCallback(handler, cb);
    }

    public void setPreviewDataCallbackWithBuffer(Handler handler, CameraPreviewDataCallback cb) {
        log("setPreviewDataCallbackWithBuffer");
        this.mProxy.setPreviewDataCallbackWithBuffer(handler, cb);
    }

    public void addCallbackBuffer(byte[] callbackBuffer) {
        log("addCallbackBuffer");
        this.mProxy.addCallbackBuffer(callbackBuffer);
    }

    public void autoFocus(Handler handler, CameraAFCallback cb) {
        log("autoFocus");
        this.mProxy.autoFocus(handler, cb);
    }

    public void cancelAutoFocus() {
        log("cancelAutoFocus");
        this.mProxy.cancelAutoFocus();
    }

    public void setAutoFocusMoveCallback(Handler handler, CameraAFMoveCallback cb) {
        log("setAutoFocusMoveCallback");
        this.mProxy.setAutoFocusMoveCallback(handler, cb);
    }

    public void takePicture(Handler handler, CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback postview, CameraPictureCallback jpeg) {
        log("takePicture");
        this.mProxy.takePicture(handler, shutter, raw, postview, jpeg);
    }

    public void takePictureWithoutWaiting(Handler handler, CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback postview, CameraPictureCallback jpeg) {
        this.mProxy.takePicture(handler, shutter, raw, postview, jpeg);
    }

    public void setDisplayOrientation(int degrees) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPostGestureRotation:");
        stringBuilder.append(degrees);
        log(stringBuilder.toString());
        this.mProxy.setDisplayOrientation(degrees);
    }

    public void setZoomChangeListener(OnZoomChangeListener listener) {
        log("setZoomChangeListener");
        this.mProxy.setZoomChangeListener(listener);
    }

    public void setFaceDetectionCallback(Handler handler, CameraFaceDetectionCallback callback) {
        log("setFaceDetectionCallback");
        this.mProxy.setFaceDetectionCallback(handler, callback);
    }

    public void startFaceDetection() {
        log("startFaceDetection");
        this.mProxy.startFaceDetection();
    }

    public void stopFaceDetection() {
        log("stopFaceDetection");
        this.mProxy.stopFaceDetection();
    }

    public void setParameters(Parameters params) {
        log("setParameters");
        this.mProxy.setParameters(params);
    }

    public Parameters getParameters() {
        log("getParameters");
        return this.mProxy.getParameters();
    }

    public CameraSettings getSettings() {
        log("getSettings");
        return this.mProxy.getSettings();
    }

    public boolean applySettings(CameraSettings settings) {
        log("applySettings");
        return this.mProxy.applySettings(settings);
    }

    public void refreshSettings() {
        log("refreshParameters");
        this.mProxy.refreshSettings();
    }

    public void enableShutterSound(boolean enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableShutterSound:");
        stringBuilder.append(enable);
        log(stringBuilder.toString());
        this.mProxy.enableShutterSound(enable);
    }

    public String dumpDeviceSettings() {
        log("dumpDeviceSettings");
        return this.mProxy.dumpDeviceSettings();
    }

    public Handler getCameraHandler() {
        return this.mProxy.getCameraHandler();
    }

    public DispatchThread getDispatchThread() {
        return this.mProxy.getDispatchThread();
    }

    public CameraStateHolder getCameraState() {
        return this.mProxy.getCameraState();
    }

    private void log(String msg) {
        Log.v(this.mTag, msg);
    }

    public void burstShot(Handler handler, CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback postview, CameraPictureCallback jpeg) {
        log("burstShot");
        this.mProxy.burstShot(handler, shutter, raw, postview, jpeg);
    }

    public void startPreAllocBurstShot() {
    }

    public void stopPreAllocBurstShot() {
    }

    public void initExtCamera(Context context) {
        this.mProxy.initExtCamera(context);
    }

    public void setGestureCallback(Handler handler, CameraGDCallBack cb) {
        this.mProxy.setGestureCallback(handler, cb);
    }

    public void startGestureDetection() {
        this.mProxy.startGestureDetection();
    }

    public void stopGestureDetection() {
        this.mProxy.stopGestureDetection();
    }

    public void startRama(Handler handler, int num) {
        this.mProxy.startRama(handler, num);
    }

    public void stopRama(Handler handler, int isMerge) {
        this.mProxy.stopRama(handler, isMerge);
    }

    public void setRamaCallback(Handler handler, CameraPanoramaCallback cb) {
        this.mProxy.setRamaCallback(handler, cb);
    }

    public void setRamaMoveCallback(Handler handler, CameraPanoramaMoveCallback cb) {
        this.mProxy.setRamaMoveCallback(handler, cb);
    }

    public void abortBurstShot() {
    }
}
