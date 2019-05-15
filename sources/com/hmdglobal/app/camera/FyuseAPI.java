package com.hmdglobal.app.camera;

public class FyuseAPI {
    public static final String COMMAND = "command";
    public static final String FYUSE_PACKAGE_NAME = "com.fyusion.sdk";
    public static final String FYUSE_SDK = "FyuseSDK";
    public static final String FYUSE_SDK_APPNAME = "com.fyusion.fyuse";
    public static final int VERSION = 2;
    public static final String VERSION_KEY = "version";

    public interface Action {
        public static final String DELETE_FILE = "deleteFile";
        public static final String LIST_DIRECTORY = "listDirectory";
        public static final String OPEN_VIEWER = ".FullScreenActivity";
        public static final String RESUME_CAMERA = ".Camera.CameraActivity";
        public static final String START_CAMERA = ".Camera.SDKCameraActivity";
    }
}
