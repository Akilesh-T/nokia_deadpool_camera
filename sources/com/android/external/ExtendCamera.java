package com.android.external;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ExtendCamera {
    private static final String TAG = "ExtendCamera";
    private static Camera mCamera;
    private static ExtendCamera sInstance;
    private HashMap<String, Method> mCameraMethodMap = new HashMap();

    public static synchronized ExtendCamera getInstance(Camera camera) {
        ExtendCamera extendCamera;
        synchronized (ExtendCamera.class) {
            if (sInstance == null || camera != mCamera) {
                sInstance = new ExtendCamera(camera);
            }
            extendCamera = sInstance;
        }
        return extendCamera;
    }

    private ExtendCamera(Camera camera) {
        mCamera = camera;
        for (Method method : Camera.class.getMethods()) {
            this.mCameraMethodMap.put(method.getName(), method);
        }
    }

    private boolean isSupport(String method) {
        return this.mCameraMethodMap.containsKey(method);
    }

    private void set(String method, Object value) {
        if (method != null) {
            Method setMethod = (Method) this.mCameraMethodMap.get(method);
            if (setMethod != null) {
                setMethod.setAccessible(true);
                if (value != null) {
                    try {
                        setMethod.invoke(mCamera, new Object[]{value});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    setMethod.invoke(mCamera, null);
                }
            }
        }
    }

    public boolean needTakePicturePerShotDuringBurst() {
        return isSupport("setLongshot");
    }

    public void setLongshot(boolean enable, Parameters param) {
        if (isSupport("setLongshot")) {
            set("setLongshot", Boolean.valueOf(enable));
        } else {
            ExtendParameters.getInstance(param).setBurstShot(enable);
        }
    }

    public void cancelPreAllocBurst() {
        String cancelPreAllocMethodName = "cancelPicture";
        if (isSupport(cancelPreAllocMethodName)) {
            set(cancelPreAllocMethodName, null);
        }
    }
}
