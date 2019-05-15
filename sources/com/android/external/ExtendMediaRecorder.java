package com.android.external;

import android.media.MediaRecorder;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ExtendMediaRecorder {
    private static final String TAG = "ExtendMediaRecorder";
    private static MediaRecorder mMediaRecorder;
    private static ExtendMediaRecorder sInstance;
    private HashMap<String, Method> mMediaRecorderMethodMap = new HashMap();

    public static synchronized ExtendMediaRecorder getInstance(MediaRecorder recorder) {
        ExtendMediaRecorder extendMediaRecorder;
        synchronized (ExtendMediaRecorder.class) {
            if (sInstance == null || recorder != mMediaRecorder) {
                sInstance = new ExtendMediaRecorder(recorder);
            }
            extendMediaRecorder = sInstance;
        }
        return extendMediaRecorder;
    }

    private ExtendMediaRecorder(MediaRecorder recorder) {
        mMediaRecorder = recorder;
        for (Method method : MediaRecorder.class.getMethods()) {
            this.mMediaRecorderMethodMap.put(method.getName(), method);
        }
    }

    private Object invoke(String methodName, Object... params) {
        Method method = (Method) this.mMediaRecorderMethodMap.get(methodName);
        if (method != null) {
            try {
                method.setAccessible(true);
                return method.invoke(mMediaRecorder, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void pause() {
        if (this.mMediaRecorderMethodMap.containsKey("pause")) {
            invoke("pause", new Object[0]);
        } else if (this.mMediaRecorderMethodMap.containsKey("setParametersExtra")) {
            invoke("setParametersExtra", "media-param-pause=1");
        } else {
            Log.d(TAG, "pause failed!");
        }
    }
}
