package com.hmdglobal.app.camera.beauty.cameragl;

import android.content.Context;
import android.opengl.GLES20;
import com.hmdglobal.app.camera.R;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ICameraOESMatrix {
    protected int mDisplayHeight;
    protected int mDisplayWidth;
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
    protected int mOutputHeight;
    protected int mOutputWidth;
    private float[] mTextureTransformMatrix;
    private int mTextureTransformMatrixLocation;
    private final String mVertexShader;
    private final float[] texturePoint = new float[]{0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
    private final float[] vertexPoint = new float[]{-1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f};

    public ICameraOESMatrix(Context context) {
        this.mVertexShader = OpenglUtil.loadFromRawFile(context, R.raw.camera_vertex);
        this.mFragmentShader = OpenglUtil.loadFromRawFile(context, R.raw.fragment_shader);
        this.mGLCubeBuffer = ByteBuffer.allocateDirect(this.vertexPoint.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mGLCubeBuffer.put(this.vertexPoint).position(0);
        this.mGLTextureBuffer = ByteBuffer.allocateDirect(OpenglUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public void init(boolean isFontCamera) {
        this.mGLTextureBuffer.put(OpenglUtil.TEXTURE_NO_ROTATION).position(0);
        onInit();
        this.mIsInitialized = true;
    }

    /* Access modifiers changed, original: protected */
    public void onInit() {
        this.mGLProgId = OpenglUtil.loadProgram(this.mVertexShader, this.mFragmentShader);
        this.mGLAttribPosition = GLES20.glGetAttribLocation(this.mGLProgId, "position");
        this.mGLUniformTexture = GLES20.glGetUniformLocation(this.mGLProgId, "inputImageTexture");
        this.mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(this.mGLProgId, "inputTextureCoordinate");
        this.mTextureTransformMatrixLocation = GLES20.glGetUniformLocation(this.mGLProgId, "textureTransform");
        this.mIsInitialized = true;
    }

    public void setTextureTransformMatrix(float[] mtx) {
        this.mTextureTransformMatrix = mtx;
    }

    public void onDisplaySizeChanged(int width, int height) {
        this.mDisplayWidth = width;
        this.mDisplayHeight = height;
    }

    public void onOutputSizeChanged(int width, int height) {
        this.mOutputWidth = width;
        this.mOutputHeight = height;
    }

    public final void destroy() {
        this.mIsInitialized = false;
        GLES20.glDeleteProgram(this.mGLProgId);
    }

    public void onDrawPicture(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        GLES20.glUseProgram(this.mGLProgId);
        if (this.mIsInitialized) {
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
            GLES20.glBindTexture(3553, 0);
        }
    }

    public int onDrawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        GLES20.glUseProgram(this.mGLProgId);
        if (!this.mIsInitialized) {
            return -1;
        }
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttribPosition, 2, 5126, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttribTextureCoordinate, 2, 5126, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(this.mTextureTransformMatrixLocation, 1, false, this.mTextureTransformMatrix, 0);
        if (textureId != -1) {
            GLES20.glActiveTexture(33984);
            GLES20.glBindTexture(3553, textureId);
            GLES20.glUniform1i(this.mGLUniformTexture, 0);
        }
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDisableVertexAttribArray(this.mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(this.mGLAttribTextureCoordinate);
        GLES20.glBindTexture(3553, 0);
        return 1;
    }

    public int onDrawToTexture(int textureId) {
        if (this.mFrameBuffers == null) {
            return -1;
        }
        GLES20.glBindFramebuffer(36160, this.mFrameBuffers[0]);
        GLES20.glViewport(0, 0, this.mOutputWidth, this.mOutputHeight);
        GLES20.glUseProgram(this.mGLProgId);
        if (!this.mIsInitialized) {
            return -1;
        }
        this.mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttribPosition, 2, 5126, false, 0, this.mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribPosition);
        this.mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mGLAttribTextureCoordinate, 2, 5126, false, 0, this.mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(this.mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(this.mTextureTransformMatrixLocation, 1, false, this.mTextureTransformMatrix, 0);
        if (textureId != -1) {
            GLES20.glActiveTexture(33984);
            GLES20.glBindTexture(36197, textureId);
            GLES20.glUniform1i(this.mGLUniformTexture, 0);
        }
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDisableVertexAttribArray(this.mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(this.mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(36197, 0);
        GLES20.glBindFramebuffer(36160, 0);
        return this.mFrameBufferTextures[0];
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

    public static int getOESTexture() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(36197, texture[0]);
        GLES20.glTexParameterf(36197, 10241, 9729.0f);
        GLES20.glTexParameterf(36197, 10240, 9729.0f);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        return texture[0];
    }

    public static int get2DTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(3553, texture[0]);
        GLES20.glTexParameterf(3553, 10241, 9729.0f);
        GLES20.glTexParameterf(3553, 10240, 9729.0f);
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
        return texture[0];
    }
}
