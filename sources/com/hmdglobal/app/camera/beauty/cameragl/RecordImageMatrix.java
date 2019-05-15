package com.hmdglobal.app.camera.beauty.cameragl;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;
import com.hmdglobal.app.camera.R;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

public class RecordImageMatrix {
    private final String mFragmentShader;
    protected int[] mFrameBufferTextures = null;
    protected int[] mFrameBuffers = null;
    private int mFrameHeight = -1;
    private int mFrameWidth = -1;
    protected int mGLAttribPosition;
    protected int mGLAttribTextureCoordinate;
    protected FloatBuffer mGLCubeBuffer;
    protected int mGLProgId;
    protected FloatBuffer mGLTextureBuffer;
    protected int mGLUniformTexture;
    protected boolean mIsInitialized;
    private final LinkedList<Runnable> mRunOnDraw = new LinkedList();
    private final String mVertexShader;

    public RecordImageMatrix(Context context) {
        this.mVertexShader = OpenglUtil.loadFromRawFile(context, R.raw.image_vertex);
        this.mFragmentShader = OpenglUtil.loadFromRawFile(context, R.raw.image_fragment);
        this.mGLCubeBuffer = ByteBuffer.allocateDirect(OpenglUtil.CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mGLCubeBuffer.put(OpenglUtil.CUBE).position(0);
        this.mGLTextureBuffer = ByteBuffer.allocateDirect(OpenglUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mGLTextureBuffer.put(OpenglUtil.getRotation(0, false, true)).position(0);
    }

    public void init() {
        onInit();
        this.mIsInitialized = true;
    }

    public void destroyFramebuffers() {
        if (this.mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(2, this.mFrameBufferTextures, 0);
            this.mFrameBufferTextures = null;
        }
        if (this.mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(2, this.mFrameBuffers, 0);
            this.mFrameBuffers = null;
        }
        this.mFrameWidth = -1;
        this.mFrameHeight = -1;
    }

    public void initCameraFrameBuffer(int width, int height) {
        if (!(this.mFrameBuffers == null || (this.mFrameWidth == width && this.mFrameHeight == height))) {
            destroyFramebuffers();
        }
        if (this.mFrameBuffers == null) {
            this.mFrameWidth = width;
            this.mFrameHeight = height;
            this.mFrameBuffers = new int[2];
            this.mFrameBufferTextures = new int[2];
            GLES20.glGenFramebuffers(2, this.mFrameBuffers, 0);
            GLES20.glGenTextures(2, this.mFrameBufferTextures, 0);
            bindFrameBuffer(this.mFrameBufferTextures[0], this.mFrameBuffers[0], width, height);
            bindFrameBuffer(this.mFrameBufferTextures[1], this.mFrameBuffers[1], height, width);
        }
    }

    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
        GLES20.glBindTexture(3553, textureId);
        GLES20.glTexImage2D(3553, 0, 6408, width, height, 0, 6408, 5121, null);
        GLES20.glTexParameterf(3553, 10240, 9729.0f);
        GLES20.glTexParameterf(3553, 10241, 9729.0f);
        GLES20.glTexParameterf(3553, 10242, 33071.0f);
        GLES20.glTexParameterf(3553, 10243, 33071.0f);
        GLES20.glBindFramebuffer(36160, frameBuffer);
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, textureId, 0);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, 0);
    }

    /* Access modifiers changed, original: protected */
    public void onInit() {
        this.mGLProgId = OpenglUtil.loadProgram(this.mVertexShader, this.mFragmentShader);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("The program ID for image is ");
        stringBuilder.append(this.mGLProgId);
        Log.d("fenghx", stringBuilder.toString());
        this.mGLAttribPosition = GLES20.glGetAttribLocation(this.mGLProgId, "position");
        this.mGLUniformTexture = GLES20.glGetUniformLocation(this.mGLProgId, "inputImageTexture");
        this.mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(this.mGLProgId, "inputTextureCoordinate");
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
        if (this.mFrameBuffers == null) {
            return -1;
        }
        GLES20.glBindFramebuffer(36160, 0);
        GLES20.glViewport(0, 0, this.mFrameWidth, this.mFrameHeight);
        GLES20.glUseProgram(this.mGLProgId);
        if (!this.mIsInitialized) {
            return -1;
        }
        GLES20.glVertexAttribPointer(this.mGLAttribPosition, 2, 5126, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribPosition);
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
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, 0);
        return this.mFrameBufferTextures[0];
    }
}
