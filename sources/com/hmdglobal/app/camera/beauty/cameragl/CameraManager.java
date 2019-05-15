package com.hmdglobal.app.camera.beauty.cameragl;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.Size;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraManager implements PreviewCallback {
    public static final int CAMERA_HAL_API_VERSION_1_0 = 256;
    private static final String PHONES_HONGMI = "2014813";
    private static final String PHONES_HUAWEI_P7 = "HUAWEI P7-L09";
    public int Angle;
    public int cameraHeight = 760;
    public int cameraWidth = 920;
    private WeakReference<Activity> mActivity;
    private PreviewCallback mCallBack;
    public Camera mCamera;
    private Object mCameraOpenLock = new Object();
    private int mCamraId = 1;
    private byte[] mDataCache;
    private Handler mHandler;
    private boolean mUsHAL1 = false;
    private HandlerThread mWorkerThread;

    public CameraManager(Activity activity) {
        this.mActivity = new WeakReference(activity);
    }

    public synchronized void switchCamera(CameraRender render) {
        closeCamera();
        this.mCamraId = (this.mCamraId + 1) % 2;
        render.mCameraChange = true;
        if (!getIsFront()) {
            autoFocus();
        }
    }

    public boolean isFrontCam() {
        return this.mCamraId == 1;
    }

    public int getCameraId() {
        return this.mCamraId;
    }

    public boolean getIsFront() {
        if (this.mCamraId == 1) {
            return true;
        }
        return false;
    }

    public void setCameraId(int id) {
        this.mCamraId = id;
    }

    public void setAngel(int cameraId) {
        if (cameraId == 3) {
            cameraId = 0;
        }
        this.mCamraId = cameraId;
        if (cameraId == 0) {
            this.Angle = 90;
        } else {
            this.Angle = MediaProviderUtils.ROTATION_270;
        }
    }

    public void calculateBestSize(CameraProxy devices) {
        List<Size> allSupportedSize = devices.getCapabilities().getSupportedPreviewSizes();
        ArrayList<Size> widthLargerSize = new ArrayList();
        for (Size tmpSize : allSupportedSize) {
            if (tmpSize.width() > tmpSize.height()) {
                widthLargerSize.add(tmpSize);
            }
        }
        Collections.sort(widthLargerSize, new Comparator<Size>(1920, 1080) {
            public int compare(Size lhs, Size rhs) {
                return Math.abs((lhs.width() * lhs.height()) - (1920 * 1080)) - Math.abs((rhs.width() * rhs.height()) - (1920 * 1080));
            }
        });
        this.cameraWidth = ((Size) widthLargerSize.get(0)).width();
        this.cameraHeight = ((Size) widthLargerSize.get(0)).height();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cameraWidth  =  ");
        stringBuilder.append(this.cameraWidth);
        stringBuilder.append(" cameraHeight  ");
        stringBuilder.append(this.cameraHeight);
        Log.e("----------->", stringBuilder.toString());
    }

    private Camera.Size calBestPreviewSize(Parameters camPara, final int width, final int height) {
        List<Camera.Size> allSupportedSize = camPara.getSupportedPreviewSizes();
        ArrayList<Camera.Size> widthLargerSize = new ArrayList();
        for (Camera.Size tmpSize : allSupportedSize) {
            if (tmpSize.width > tmpSize.height) {
                widthLargerSize.add(tmpSize);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calBestPreviewSize ");
            stringBuilder.append(tmpSize.width);
            stringBuilder.append(", ");
            stringBuilder.append(tmpSize.height);
            Log.e("CameraManager", stringBuilder.toString());
        }
        Collections.sort(widthLargerSize, new Comparator<Camera.Size>() {
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return Math.abs((lhs.width * lhs.height) - (width * height)) - Math.abs((rhs.width * rhs.height) - (width * height));
            }
        });
        return (Camera.Size) widthLargerSize.get(0);
    }

    public void startPreview(SurfaceTexture surfaceTexture) {
        try {
            this.mCamera.setPreviewTexture(surfaceTexture);
            this.mCamera.startPreview();
            onPreviewStarted(this.mCamera);
            this.mCamera.setPreviewCallbackWithBuffer(this);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void onPreviewStarted(Camera camera) {
        Camera.Size s = camera.getParameters().getPreviewSize();
        int wishedBufferSize = ((s.height * s.width) * 3) / 2;
        camera.addCallbackBuffer(new byte[wishedBufferSize]);
        camera.addCallbackBuffer(new byte[wishedBufferSize]);
        camera.addCallbackBuffer(new byte[wishedBufferSize]);
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        if (this.mDataCache == null || this.mDataCache.length != data.length) {
            this.mDataCache = new byte[data.length];
        }
        System.arraycopy(data, 0, this.mDataCache, 0, data.length);
        camera.addCallbackBuffer(data);
        if (this.mCallBack != null) {
            this.mCallBack.onPreviewFrame(this.mDataCache, camera);
        }
    }

    public synchronized void autoFocus() {
        if (this.mCamera != null) {
            try {
                this.mCamera.cancelAutoFocus();
                Parameters parameters = this.mCamera.getParameters();
                parameters.setFocusMode("auto");
                this.mCamera.setParameters(parameters);
                this.mCamera.autoFocus(null);
            } catch (Exception e) {
            }
        }
    }

    public boolean actionDetect(PreviewCallback mActivity) {
        this.mCallBack = mActivity;
        return true;
    }

    public synchronized void closeCamera() {
        if (this.mCamera != null) {
            this.mCamera.setPreviewCallback(null);
            this.mCamera.stopPreview();
            this.mCamera.release();
        }
        this.mCamera = null;
        if (this.mWorkerThread != null) {
            this.mWorkerThread.quit();
            this.mWorkerThread = null;
        }
    }

    private int getAngle() {
        CameraInfo info = new CameraInfo();
        if (this.mCamraId == 3) {
            this.mCamraId = 0;
        }
        Camera.getCameraInfo(this.mCamraId, info);
        return info.orientation;
    }

    private int getAngleGoogle() {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(this.mCamraId, info);
        if (this.mActivity.get() == null) {
            return 90;
        }
        int rotateAngle;
        int degrees = 0;
        switch (((Activity) this.mActivity.get()).getWindowManager().getDefaultDisplay().getRotation()) {
            case 0:
                degrees = 0;
                break;
            case 1:
                degrees = 90;
                break;
            case 2:
                degrees = MediaProviderUtils.ROTATION_180;
                break;
            case 3:
                degrees = MediaProviderUtils.ROTATION_270;
                break;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("xie getAngle: origin onPreviewFrame");
        stringBuilder.append(degrees);
        stringBuilder.append("orient");
        stringBuilder.append(info.orientation);
        Log.e("xie", stringBuilder.toString());
        if (info.facing == 1) {
            rotateAngle = (360 - ((info.orientation + degrees) % 360)) % 360;
        } else {
            rotateAngle = ((info.orientation - degrees) + 360) % 360;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("xie getAngle: process");
        stringBuilder.append(rotateAngle);
        Log.e("xie", stringBuilder.toString());
        return rotateAngle;
    }
}
