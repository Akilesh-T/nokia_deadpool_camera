package com.hmdglobal.app.camera.beauty.cameragl;

import android.opengl.GLES20;

public class OESTexture {
    private int mTextureHandle;

    public int getTextureId() {
        return this.mTextureHandle;
    }

    public void init() {
        int[] mTextureHandles = new int[1];
        GLES20.glGenTextures(1, mTextureHandles, 0);
        this.mTextureHandle = mTextureHandles[0];
        GLES20.glBindTexture(36197, mTextureHandles[0]);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        GLES20.glTexParameteri(36197, 10241, 9729);
        GLES20.glTexParameteri(36197, 10240, 9729);
    }
}
