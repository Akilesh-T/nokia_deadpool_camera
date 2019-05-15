package com.hmdglobal.app.camera.remote;

public interface RemoteShutterListener {
    void onModuleExit();

    void onModuleReady(RemoteCameraModule remoteCameraModule, int i);

    void onPictureTaken(byte[] bArr);
}
