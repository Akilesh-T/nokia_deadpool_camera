package com.hmdglobal.app.camera.one.v2;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import com.hmdglobal.app.camera.SoundPlayer;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.one.OneCamera.Facing;
import com.hmdglobal.app.camera.one.OneCamera.OpenCallback;
import com.hmdglobal.app.camera.one.OneCameraManager;
import com.hmdglobal.app.camera.util.Size;

public class OneCameraManagerImpl extends OneCameraManager {
    private static final Tag TAG = new Tag("OneCameraMgrImpl2");
    private final CameraManager mCameraManager;
    private final Context mContext;
    private final DisplayMetrics mDisplayMetrics;
    private final int mMaxMemoryMB;
    private final SoundPlayer mSoundPlayer;

    public OneCameraManagerImpl(Context context, CameraManager cameraManager, int maxMemoryMB, DisplayMetrics displayMetrics, SoundPlayer soundPlayer) {
        this.mContext = context;
        this.mCameraManager = cameraManager;
        this.mMaxMemoryMB = maxMemoryMB;
        this.mDisplayMetrics = displayMetrics;
        this.mSoundPlayer = soundPlayer;
    }

    public void open(Facing facing, final boolean useHdr, final Size pictureSize, final OpenCallback openCallback, Handler handler) {
        Tag tag;
        StringBuilder stringBuilder;
        try {
            String cameraId = getCameraId(facing);
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Opening Camera ID ");
            stringBuilder.append(cameraId);
            Log.i(tag, stringBuilder.toString());
            this.mCameraManager.openCamera(cameraId, new StateCallback() {
                private boolean isFirstCallback = true;

                public void onDisconnected(CameraDevice device) {
                    if (this.isFirstCallback) {
                        this.isFirstCallback = false;
                        device.close();
                        openCallback.onCameraClosed();
                    }
                }

                public void onClosed(CameraDevice device) {
                    if (this.isFirstCallback) {
                        this.isFirstCallback = false;
                        openCallback.onCameraClosed();
                    }
                }

                public void onError(CameraDevice device, int error) {
                    if (this.isFirstCallback) {
                        this.isFirstCallback = false;
                        device.close();
                        openCallback.onFailure();
                    }
                }

                public void onOpened(CameraDevice device) {
                    if (this.isFirstCallback) {
                        this.isFirstCallback = false;
                        try {
                            CameraDevice cameraDevice = device;
                            openCallback.onCameraOpened(OneCameraCreator.create(OneCameraManagerImpl.this.mContext, useHdr, cameraDevice, OneCameraManagerImpl.this.mCameraManager.getCameraCharacteristics(device.getId()), pictureSize, OneCameraManagerImpl.this.mMaxMemoryMB, OneCameraManagerImpl.this.mDisplayMetrics, OneCameraManagerImpl.this.mSoundPlayer));
                        } catch (CameraAccessException e) {
                            Log.d(OneCameraManagerImpl.TAG, "Could not get camera characteristics");
                            openCallback.onFailure();
                        }
                    }
                }
            }, handler);
        } catch (CameraAccessException ex) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not open camera. ");
            stringBuilder.append(ex.getMessage());
            Log.e(tag, stringBuilder.toString());
            handler.post(new Runnable() {
                public void run() {
                    openCallback.onFailure();
                }
            });
        } catch (UnsupportedOperationException ex2) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not open camera. ");
            stringBuilder.append(ex2.getMessage());
            Log.e(tag, stringBuilder.toString());
            handler.post(new Runnable() {
                public void run() {
                    openCallback.onFailure();
                }
            });
        }
    }

    public boolean hasCameraFacing(Facing facing) {
        return getFirstCameraFacing(facing == Facing.FRONT ? 0 : 1) != null;
    }

    private String getCameraId(Facing facing) {
        if (facing == Facing.FRONT) {
            return getFirstFrontCameraId();
        }
        return getFirstBackCameraId();
    }

    public String getFirstBackCameraId() {
        Log.d(TAG, "Getting First BACK Camera");
        String cameraId = getFirstCameraFacing(1);
        if (cameraId != null) {
            return cameraId;
        }
        throw new RuntimeException("No back-facing camera found.");
    }

    public String getFirstFrontCameraId() {
        Log.d(TAG, "Getting First FRONT Camera");
        String cameraId = getFirstCameraFacing(null);
        if (cameraId != null) {
            return cameraId;
        }
        throw new RuntimeException("No front-facing camera found.");
    }

    private String getFirstCameraFacing(int facing) {
        try {
            for (String cameraId : this.mCameraManager.getCameraIdList()) {
                if (((Integer) this.mCameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)).intValue() == facing) {
                    return cameraId;
                }
            }
            return null;
        } catch (CameraAccessException ex) {
            throw new RuntimeException("Unable to get camera ID", ex);
        }
    }
}
