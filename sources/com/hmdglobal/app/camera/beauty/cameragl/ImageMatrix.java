package com.hmdglobal.app.camera.beauty.cameragl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import com.hmdglobal.app.camera.R;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

public class ImageMatrix {
    private final String mFragmentShader;
    protected int mGLAttribPosition;
    protected int mGLAttribTextureCoordinate;
    protected FloatBuffer mGLCubeBuffer;
    protected int mGLProgId;
    protected FloatBuffer mGLTextureBuffer;
    protected int mGLUniformTexture;
    protected boolean mIsInitialized;
    private final LinkedList<Runnable> mRunOnDraw = new LinkedList();
    protected int mSurfaceHeight;
    protected int mSurfaceWidth;
    protected int mTableTextureID = -1;
    private final String mVertexShader;

    public ImageMatrix(Context context) {
        this.mVertexShader = OpenglUtil.loadFromRawFile(context, R.raw.image_vertex);
        this.mFragmentShader = OpenglUtil.loadFromRawFile(context, R.raw.image_fragment);
        this.mGLCubeBuffer = ByteBuffer.allocateDirect(OpenglUtil.CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mGLCubeBuffer.put(OpenglUtil.CUBE).position(0);
        this.mGLTextureBuffer = ByteBuffer.allocateDirect(OpenglUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mGLTextureBuffer.put(OpenglUtil.getRotation(0, false, true)).position(0);
    }

    public void init() {
        onInit();
    }

    private void onInit() {
        this.mGLProgId = OpenglUtil.loadProgram(this.mVertexShader, this.mFragmentShader);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("The program ID for image is ");
        stringBuilder.append(this.mGLProgId);
        Log.d("fenghx", stringBuilder.toString());
        this.mGLAttribPosition = GLES20.glGetAttribLocation(this.mGLProgId, "position");
        this.mGLUniformTexture = GLES20.glGetUniformLocation(this.mGLProgId, "inputImageTexture");
        this.mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(this.mGLProgId, "inputTextureCoordinate");
        this.mIsInitialized = true;
    }

    /* Access modifiers changed, original: protected */
    public void runOnDraw(Runnable runnable) {
        synchronized (this.mRunOnDraw) {
            this.mRunOnDraw.addLast(runnable);
        }
    }

    /* Access modifiers changed, original: protected */
    public void runPendingOnDrawTasks() {
        synchronized (this.mRunOnDraw) {
            while (!this.mRunOnDraw.isEmpty()) {
                ((Runnable) this.mRunOnDraw.removeFirst()).run();
            }
        }
    }

    public final void destroy() {
        this.mIsInitialized = false;
        GLES20.glDeleteProgram(this.mGLProgId);
    }

    public int onDrawFrame(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        GLES20.glUseProgram(this.mGLProgId);
        runPendingOnDrawTasks();
        if (!this.mIsInitialized) {
            return -1;
        }
        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttribPosition, 2, 5126, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttribTextureCoordinate, 2, 5126, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribTextureCoordinate);
        if (textureId != -1) {
            GLES20.glActiveTexture(33984);
            GLES20.glBindTexture(3553, textureId);
            GLES20.glUniform1i(this.mGLUniformTexture, 0);
        }
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDisableVertexAttribArray(this.mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(this.mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, 0);
        return 1;
    }
}
