package com.morphoinc.app.panoramagp3;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.camera_states.Camera1PreviewState;
import com.morphoinc.app.camera_states.Camera1State;
import com.morphoinc.app.camera_states.CameraState;
import com.morphoinc.app.camera_states.IMorphoPanoramaGP3Callback;
import com.morphoinc.app.panoramagp3.MorphoCameraBase.IMorphoCameraListener;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MorphoCamera1 extends MorphoCameraBase {
    private static final String LOG_TAG = "MorphoCamera1";
    private Camera mCamera = null;
    private final int mCameraId;
    private final CameraInfo mCameraInfo = new CameraInfo();
    private CameraState mCameraState = new CameraState();
    private int mDisplayOrientation = 0;
    private IMorphoPanoramaGP3Callback mGP3Callback;
    private final IMorphoCameraListener mListener;
    private boolean mResumed = false;
    private final Comparator<Size> mSizeComparator = new Comparator<Size>() {
        public int compare(Size lhs, Size rhs) {
            int result = rhs.width - lhs.height;
            if (result == 0) {
                return rhs.height - lhs.height;
            }
            return result;
        }
    };
    private SurfaceHolder mSurfaceHolder = null;
    public float viewAngleH;
    public float viewAngleV;

    public CameraInfo cameraInfo() {
        return this.mCameraInfo;
    }

    public final CameraState cameraState() {
        return this.mCameraState;
    }

    public int burstRemaining() {
        return 0;
    }

    public void setBurstRemaining(int value) {
    }

    public void setMorphoPanoramaGP3Interface(IMorphoPanoramaGP3Callback callback) {
        this.mGP3Callback = callback;
    }

    public MorphoCamera1(IMorphoCameraListener listener, int camera_id) {
        if (listener == null) {
            listener = nullMorphoCameraListener;
        }
        this.mListener = listener;
        this.mCameraId = camera_id;
    }

    public void exit() {
    }

    public void pause() {
        releaseCamera();
        this.mResumed = false;
    }

    public void resume(android.util.Size captureSize, android.util.Size previewSize) {
        this.mCameraInfo.setCaptureSize(captureSize.getWidth(), captureSize.getHeight());
        this.mCameraInfo.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        this.mResumed = true;
        startPreviewLocal(this.mDisplayOrientation);
    }

    public String[] getAllCameras() {
        int id;
        int numberOfCameras = Camera.getNumberOfCameras();
        String[] cameras = new String[numberOfCameras];
        int other = 0;
        CameraInfo cameraInfo = new CameraInfo();
        int i = 0;
        int front = 0;
        int back = 0;
        for (id = 0; id < numberOfCameras; id++) {
            Camera.getCameraInfo(id, cameraInfo);
            Object[] objArr;
            switch (cameraInfo.facing) {
                case 0:
                    objArr = new Object[1];
                    back++;
                    objArr[0] = Integer.valueOf(back);
                    cameras[id] = String.format(Locale.US, "Back %d", objArr);
                    break;
                case 1:
                    objArr = new Object[1];
                    front++;
                    objArr[0] = Integer.valueOf(front);
                    cameras[id] = String.format(Locale.US, "Front %d", objArr);
                    break;
                default:
                    objArr = new Object[1];
                    other++;
                    objArr[0] = Integer.valueOf(other);
                    cameras[id] = String.format(Locale.US, "Other %d", objArr);
                    break;
            }
        }
        if (other == 1) {
            for (id = 0; id < cameras.length; id++) {
                if (cameras[id].contains("Other")) {
                    cameras[id] = cameras[id].replace(" 1", "");
                }
            }
        }
        if (front == 1) {
            for (id = 0; id < cameras.length; id++) {
                if (cameras[id].contains("Front")) {
                    cameras[id] = cameras[id].replace(" 1", "");
                }
            }
        }
        if (back == 1) {
            while (true) {
                id = i;
                if (id < cameras.length) {
                    if (cameras[id].contains("Back")) {
                        cameras[id] = cameras[id].replace(" 1", "");
                    }
                    i = id + 1;
                }
            }
        }
        return cameras;
    }

    public boolean isFrontCamera(int camera_id) {
        return false;
    }

    private void releaseCamera() {
        if (this.mCamera != null) {
            this.mCamera.setPreviewCallback(null);
            this.mCamera.release();
            this.mCamera = null;
        }
    }

    public boolean openCamera(SurfaceHolder holder) {
        releaseCamera();
        this.mCamera = open(this.mCameraId);
        if (this.mCamera == null) {
            return false;
        }
        try {
            this.mCamera.setPreviewDisplay(holder);
            this.mSurfaceHolder = holder;
            Parameters parameters = this.mCamera.getParameters();
            this.viewAngleH = parameters.getHorizontalViewAngle();
            this.viewAngleV = parameters.getVerticalViewAngle();
            this.mCameraInfo.setFocalLength(parameters.getFocalLength());
            this.mListener.onOpened();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updateCameraState(CameraState newState) {
        if (newState instanceof Camera1State) {
            this.mCameraState = newState;
            return;
        }
        LogFilter.w(LOG_TAG, "#updateCameraState, argument is invalid.");
        setDefaultCameraState();
    }

    public void setDefaultCameraState() {
        this.mCameraState = new Camera1State(this.mCamera, this.mCameraInfo, this.mGP3Callback, this.mListener);
    }

    public int[] getSupportedPreviewSizes() {
        Camera camera = this.mCamera;
        if (camera == null) {
            camera = open(this.mCameraId);
            if (camera == null) {
                return new int[0];
            }
        }
        try {
            List<Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
            Collections.sort(previewSizes, this.mSizeComparator);
            int[] sizes = new int[(previewSizes.size() * 2)];
            int index = 0;
            for (Size size : previewSizes) {
                int index2 = index + 1;
                sizes[index] = size.width;
                index = index2 + 1;
                sizes[index2] = size.height;
            }
            return sizes;
        } finally {
            if (this.mCamera == null) {
                camera.release();
            }
        }
    }

    public void startPreview(int displayOrientation) {
        startPreviewLocal(displayOrientation);
        this.mDisplayOrientation = displayOrientation;
    }

    private void startPreviewLocal(int displayOrientation) {
        if (this.mResumed && this.mSurfaceHolder != null) {
            int degrees;
            if (this.mCamera == null) {
                openCamera(this.mSurfaceHolder);
            }
            Parameters parameters = this.mCamera.getParameters();
            int format = parameters.getPreviewFormat();
            LogFilter.i(LOG_TAG, String.format(Locale.US, "format:%d, displayOrientation:%d", new Object[]{Integer.valueOf(format), Integer.valueOf(displayOrientation)}));
            CameraInfo info = getCameraInfo(this.mCameraId);
            if (info.facing == 1) {
                degrees = (360 - ((info.orientation + displayOrientation) % 360)) % 360;
            } else {
                degrees = ((info.orientation - displayOrientation) + 360) % 360;
            }
            this.mCamera.setDisplayOrientation(degrees);
            parameters.setPreviewSize(this.mCameraInfo.getCaptureWidth(), this.mCameraInfo.getCaptureHeight());
            this.mCamera.setParameters(parameters);
            this.mCameraState = new Camera1PreviewState(this.mCamera, this.mCameraInfo, this.mGP3Callback, this.mListener);
            this.mCameraState.onStart();
        }
    }

    public void takePicture() {
    }

    public void takePictureBurst() {
    }

    public void takePictureZSL() {
    }

    public android.util.Size getMaxPictureSize() {
        Camera camera = this.mCamera;
        if (camera == null) {
            camera = open(this.mCameraId);
            if (camera == null) {
                return new android.util.Size(320, 240);
            }
        }
        try {
            List<Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();
            android.util.Size isEmpty = pictureSizes.isEmpty();
            if (isEmpty == true) {
                isEmpty = new android.util.Size(320, 240);
                return isEmpty;
            }
            Collections.sort(pictureSizes, this.mSizeComparator);
            isEmpty = ((Size) pictureSizes.get(0)).width;
            android.util.Size size = new android.util.Size(isEmpty, ((Size) pictureSizes.get(0)).height);
            if (this.mCamera == null) {
                camera.release();
            }
            return size;
        } finally {
            if (this.mCamera == null) {
                camera.release();
            }
        }
    }

    public static CameraInfo getCameraInfo(int camera_id) {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(camera_id, cameraInfo);
        return cameraInfo;
    }

    public static Camera open(int camera_id) {
        return Camera.open(camera_id);
    }
}
