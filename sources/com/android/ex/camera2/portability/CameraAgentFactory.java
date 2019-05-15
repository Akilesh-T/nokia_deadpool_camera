package com.android.ex.camera2.portability;

import android.content.Context;
import android.os.Build.VERSION;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.ex.camera2.portability.util.SystemProperties;

public class CameraAgentFactory {
    private static final String API_LEVEL_OVERRIDE_API1 = "1";
    private static final String API_LEVEL_OVERRIDE_API2 = "2";
    private static final String API_LEVEL_OVERRIDE_DEFAULT = "0";
    private static final String API_LEVEL_OVERRIDE_KEY = "camera2.portability.force_api";
    private static final String API_LEVEL_OVERRIDE_VALUE = SystemProperties.get(API_LEVEL_OVERRIDE_KEY, "0");
    private static final int FIRST_SDK_WITH_API_2 = 21;
    private static final Tag TAG = new Tag("CamAgntFact");
    private static CameraAgent sAndroidCamera2Agent;
    private static int sAndroidCamera2AgentClientCount;
    private static CameraAgent sAndroidCameraAgent;
    private static int sAndroidCameraAgentClientCount;

    public enum CameraApi {
        AUTO,
        API_1,
        API_2
    }

    private static CameraApi highestSupportedApi() {
        if (VERSION.SDK_INT >= 21 || VERSION.CODENAME.equals("L")) {
            return CameraApi.API_2;
        }
        return CameraApi.API_1;
    }

    private static CameraApi validateApiChoice(CameraApi choice) {
        if (API_LEVEL_OVERRIDE_VALUE.equals("1")) {
            Log.d(TAG, "API level overridden by system property: forced to 1");
            return CameraApi.API_1;
        } else if (API_LEVEL_OVERRIDE_VALUE.equals("2")) {
            Log.d(TAG, "API level overridden by system property: forced to 2");
            return CameraApi.API_2;
        } else {
            if (choice == null) {
                Log.w(TAG, "null API level request, so assuming AUTO");
                choice = CameraApi.AUTO;
            }
            if (choice == CameraApi.AUTO) {
                choice = highestSupportedApi();
            }
            return choice;
        }
    }

    public static synchronized CameraAgent getAndroidCameraAgent(Context context, CameraApi api) {
        synchronized (CameraAgentFactory.class) {
            CameraAgent cameraAgent;
            if (validateApiChoice(api) == CameraApi.API_1) {
                Log.v(TAG, "request androidCameraAgent instance");
                if (sAndroidCameraAgent == null) {
                    sAndroidCameraAgent = new AndroidCameraAgentImpl();
                    sAndroidCameraAgentClientCount = 1;
                    Log.v(TAG, "create androidCameraAgent instance");
                } else {
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("androidCameraAgent instance is recycled ?");
                    stringBuilder.append(((AndroidCameraAgentImpl) sAndroidCameraAgent).isRecycled());
                    Log.v(tag, stringBuilder.toString());
                    sAndroidCameraAgentClientCount++;
                }
                cameraAgent = sAndroidCameraAgent;
                return cameraAgent;
            } else if (highestSupportedApi() != CameraApi.API_1) {
                if (sAndroidCamera2Agent == null) {
                    sAndroidCamera2Agent = new AndroidCamera2AgentImpl(context);
                    sAndroidCamera2AgentClientCount = 1;
                } else {
                    sAndroidCamera2AgentClientCount++;
                }
                cameraAgent = sAndroidCamera2Agent;
                return cameraAgent;
            } else {
                throw new UnsupportedOperationException("Camera API_2 unavailable on this device");
            }
        }
    }

    public static synchronized void recycle(CameraApi api) {
        synchronized (CameraAgentFactory.class) {
            int i;
            if (validateApiChoice(api) == CameraApi.API_1) {
                i = sAndroidCameraAgentClientCount - 1;
                sAndroidCameraAgentClientCount = i;
                if (i == 0 && sAndroidCameraAgent != null) {
                    sAndroidCameraAgent.recycle();
                    Log.v(TAG, "androidCameraAgent instance recycled");
                    sAndroidCameraAgent = null;
                    Log.v(TAG, "androidCameraAgent instance became null");
                }
            } else if (highestSupportedApi() != CameraApi.API_1) {
                i = sAndroidCamera2AgentClientCount - 1;
                sAndroidCamera2AgentClientCount = i;
                if (i == 0 && sAndroidCamera2Agent != null) {
                    sAndroidCamera2Agent.recycle();
                    sAndroidCamera2Agent = null;
                }
            } else {
                throw new UnsupportedOperationException("Camera API_2 unavailable on this device");
            }
        }
    }
}
