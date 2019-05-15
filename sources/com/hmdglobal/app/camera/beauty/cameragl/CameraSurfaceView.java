package com.hmdglobal.app.camera.beauty.cameragl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.beauty.cameragl.CameraRender.onPreviewBytes;
import com.hmdglobal.app.camera.beauty.cameragl.CameraRender.onVideoStopListener;
import com.hmdglobal.app.camera.beauty.component.SensorEventUtil;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;

public class CameraSurfaceView extends GLSurfaceView {
    private CameraProxy mCameraDevice;
    private CameraManager mCameraManager;
    public int mCameraPreviewHeight;
    public int mCameraPreviewWidth;
    private Context mContext;
    private boolean mRecordingEnabled = false;
    public String path;
    private CameraRender render;
    private RequestRenderListener requestRenderListener = new RequestRenderListener() {
        public void startRequestRender() {
            CameraSurfaceView.this.requestRender();
        }

        public void runOnRenderThread(Runnable runnable) {
            CameraSurfaceView.this.queueEvent(runnable);
        }

        public boolean isFront() {
            CameraSurfaceView.this.mCameraManager.setCameraId(((CameraActivity) CameraSurfaceView.this.getContext()).getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue());
            return CameraSurfaceView.this.mCameraManager.getIsFront();
        }
    };
    private SensorEventUtil sensorUtil;

    public interface RequestRenderListener {
        boolean isFront();

        void runOnRenderThread(Runnable runnable);

        void startRequestRender();
    }

    public void initVideoPath(String path) {
        if (this.render != null) {
            this.render.initVideoPath(path);
        }
    }

    public void setWidth(int w) {
        this.mCameraManager.cameraWidth = w;
    }

    public void setHeight(int h) {
        this.mCameraManager.cameraHeight = h;
    }

    public void switchCamera(int id) {
        this.render.switchCamera(id);
    }

    public boolean isCameraDrawable() {
        return this.render.isDrawable();
    }

    public CameraSurfaceView(Context context) {
        super(context);
        setVisibility(0);
        this.mContext = context;
        int cameraId = ((CameraActivity) context).getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
        this.mCameraManager = new CameraManager((CameraActivity) context);
        this.mCameraManager.setCameraId(cameraId);
        this.mCameraManager.setAngel(cameraId);
        this.sensorUtil = new SensorEventUtil((CameraActivity) context);
        init();
    }

    public void initRender(CameraProxy devices) {
        this.mCameraDevice = devices;
        setCameraDevice(devices);
    }

    public void setCameraDevice(CameraProxy devices) {
    }

    public void setDrawEnable(boolean need) {
        this.render.setDrawEnable(need);
    }

    public Surface getSurface() {
        return getHolder().getSurface();
    }

    public void onPreviewStarted() {
        this.render.setCameraDevice(this.mCameraDevice);
    }

    public onPreviewBytes getPreviewCallBack() {
        return this.render.getApi2CameraDeviceCallback();
    }

    public void setOrientation(int cameraId) {
        this.render.mCameraChange = true;
        this.mCameraManager.setAngel(cameraId);
    }

    public void takePicture(Runnable handler) {
        this.render.takePicture(this.mCameraManager.getIsFront(), handler);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setVisibility(0);
        this.mContext = context;
        this.mCameraManager = new CameraManager((CameraActivity) context);
        this.sensorUtil = new SensorEventUtil((CameraActivity) context);
        init();
    }

    public void startVideoRecorder() {
        this.mRecordingEnabled = true;
        queueEvent(new Runnable() {
            public void run() {
                CameraSurfaceView.this.render.changeRecordingState(CameraSurfaceView.this.mRecordingEnabled);
            }
        });
    }

    public void stopVideoRecorder() {
        this.mRecordingEnabled = false;
        queueEvent(new Runnable() {
            public void run() {
                CameraSurfaceView.this.render.changeRecordingState(CameraSurfaceView.this.mRecordingEnabled);
            }
        });
    }

    public void setAngle(int a) {
        this.render.setAngle(a);
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.render.getSurfaceTexture();
    }

    private void init() {
        this.render = new CameraRender(this.mContext, this.mCameraManager, this.sensorUtil);
        this.render.setRequestRenderListener(this.requestRenderListener);
        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(3);
        setRenderer(this.render);
        setRenderMode(1);
    }

    public void setVideoSavedListener(onVideoStopListener listener) {
        this.render.setVideoStopListener(listener);
    }

    /* Access modifiers changed, original: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroyRender();
    }

    private void destroyRender() {
        if (this.render != null) {
            this.render.deleteTextures(this);
            this.render.onDestroy();
        }
    }

    public void onPause() {
        super.onPause();
        destroyRender();
    }

    public void onResume() {
        super.onResume();
    }
}
