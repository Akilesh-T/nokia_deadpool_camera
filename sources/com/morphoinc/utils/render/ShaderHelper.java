package com.morphoinc.utils.render;

import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ShaderHelper {
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int GL_TEXTURE_EXTERNAL_OES = 36197;
    public static final int MODE_USING_ExtImg = 1;
    public static final int MODE_USING_Tex2D = 0;
    private static final String TAG = "ShaderHelper";
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 20;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private static final String mFragmentShaderExtImg = "#extension GL_OES_EGL_image_external : require\nprecision mediump float;\nvarying vec2 vTextureCoord;\nuniform samplerExternalOES sTexture;\nvoid main() {\n  gl_FragColor = texture2D(sTexture, vTextureCoord).rgba; \n}\n";
    private static final String mFragmentShaderTex2D = "precision mediump float;\nvarying vec2 vTextureCoord;\nuniform sampler2D sTexture;\nvoid main() {\n  gl_FragColor = texture2D(sTexture, vTextureCoord).rgba; \n}\n";
    private static final float[] mTriangleVerticesDataBase = new float[]{-1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f};
    private static final String mVertexShader = "uniform mat4 uMVPMatrix;\nuniform mat4 uSTMatrix;\nattribute vec4 aPosition;\nattribute vec4 aTextureCoord;\nvarying vec2 vTextureCoord;\nvoid main() {\n  gl_Position = uMVPMatrix * aPosition;\n  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n}\n";
    public boolean mInitialized;
    public final float[] mMVPMatrix = new float[16];
    public int mMode;
    public int mProgram;
    public final float[] mSTMatrix = new float[16];
    private FloatBuffer mTriangleVertices;
    private float[] mTriangleVerticesData = new float[20];
    public int maPositionHandle;
    public int maTextureHandle;
    public int muMVPMatrixHandle;
    public int muSTMatrixHandle;

    public static class TextureFBO {
        public final int[] mFrameBuffer = new int[1];
        public final int mHeight;
        public final int[] mTexture = new int[1];
        public final int mWidth;

        public TextureFBO(int w, int h) {
            this.mWidth = w;
            this.mHeight = h;
            GLES20.glGenFramebuffers(1, this.mFrameBuffer, 0);
            GLES20.glGenTextures(1, this.mTexture, 0);
            GLES20.glBindTexture(3553, this.mTexture[0]);
            GLES20.glTexImage2D(3553, 0, 6408, this.mWidth, this.mHeight, 0, 6408, 5121, null);
            GLES20.glTexParameteri(3553, 10242, 33071);
            GLES20.glTexParameteri(3553, 10243, 33071);
            GLES20.glTexParameteri(3553, 10240, 9729);
            GLES20.glTexParameteri(3553, 10241, 9729);
            GLES20.glBindTexture(3553, 0);
            GLES20.glBindFramebuffer(36160, this.mFrameBuffer[0]);
            GLES20.glFramebufferTexture2D(36160, 36064, 3553, this.mTexture[0], 0);
            GLES20.glBindFramebuffer(36160, 0);
        }

        public void release() {
            GLES20.glDeleteFramebuffers(1, this.mFrameBuffer, 0);
            GLES20.glDeleteTextures(1, this.mTexture, 0);
        }
    }

    public void initShader(int mode) {
        initShaderWithTexCoord(mode, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    public void initShaderWithTexCoord(int mode, float ltx, float lty, float rbx, float rby) {
        if (!this.mInitialized) {
            this.mMode = mode;
            this.mProgram = createProgram(mVertexShader, mode == 0 ? mFragmentShaderTex2D : mFragmentShaderExtImg);
            if (this.mProgram == 0) {
                Log.d(TAG, "Error createProgram for FBO shader");
                return;
            }
            this.maPositionHandle = GLES20.glGetAttribLocation(this.mProgram, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (this.maPositionHandle != -1) {
                this.maTextureHandle = GLES20.glGetAttribLocation(this.mProgram, "aTextureCoord");
                checkGlError("glGetAttribLocation aTextureCoord");
                if (this.maTextureHandle != -1) {
                    this.muMVPMatrixHandle = GLES20.glGetUniformLocation(this.mProgram, "uMVPMatrix");
                    checkGlError("glGetUniformLocation uMVPMatrix");
                    if (this.muMVPMatrixHandle != -1) {
                        this.muSTMatrixHandle = GLES20.glGetUniformLocation(this.mProgram, "uSTMatrix");
                        checkGlError("glGetUniformLocation uSTMatrix");
                        if (this.muSTMatrixHandle != -1) {
                            Matrix.setIdentityM(this.mSTMatrix, 0);
                            this.mInitialized = true;
                            this.mTriangleVerticesData = (float[]) mTriangleVerticesDataBase.clone();
                            this.mTriangleVertices = ByteBuffer.allocateDirect(this.mTriangleVerticesData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                            modifyVerticesUV(ltx, lty, rbx, rby);
                            return;
                        }
                        throw new RuntimeException("Could not get attrib location for uSTMatrix");
                    }
                    throw new RuntimeException("Could not get attrib location for uMVPMatrix");
                }
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
    }

    public void modifyVerticesUV(float ltx, float lty, float rbx, float rby) {
        this.mTriangleVerticesData[3] = ltx;
        this.mTriangleVerticesData[4] = lty;
        this.mTriangleVerticesData[8] = rbx;
        this.mTriangleVerticesData[9] = lty;
        this.mTriangleVerticesData[13] = ltx;
        this.mTriangleVerticesData[14] = rby;
        this.mTriangleVerticesData[18] = rbx;
        this.mTriangleVerticesData[19] = rby;
        this.mTriangleVertices.position(0);
        this.mTriangleVertices.put(this.mTriangleVerticesData).position(0);
    }

    public void finalizeShader() {
        if (this.mInitialized) {
            GLES20.glDeleteProgram(this.mProgram);
            this.mTriangleVertices = null;
            this.mInitialized = false;
        }
    }

    public void renderTexture(int idTex, Rect view_port) {
        if (view_port != null) {
            GLES20.glViewport(view_port.left, view_port.top, view_port.right - view_port.left, view_port.bottom - view_port.top);
        }
        GLES20.glClear(16640);
        GLES20.glUseProgram(this.mProgram);
        checkGlError("glUseProgram");
        GLES20.glActiveTexture(33984);
        checkGlError("glActiveTexture");
        GLES20.glBindTexture(this.mMode == 0 ? 3553 : GL_TEXTURE_EXTERNAL_OES, idTex);
        checkGlError("glBindTexture");
        this.mTriangleVertices.position(0);
        GLES20.glVertexAttribPointer(this.maPositionHandle, 3, 5126, false, 20, this.mTriangleVertices);
        checkGlError("glVertexAttribPointer maPositionHandleTex2D");
        GLES20.glEnableVertexAttribArray(this.maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandleTex2D");
        this.mTriangleVertices.position(3);
        GLES20.glVertexAttribPointer(this.maTextureHandle, 2, 5126, false, 20, this.mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandleTex2D");
        GLES20.glEnableVertexAttribArray(this.maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandleTex2D");
        Matrix.setIdentityM(this.mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(this.muMVPMatrixHandle, 1, false, this.mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(this.muSTMatrixHandle, 1, false, this.mSTMatrix, 0);
        GLES20.glDrawArrays(5, 0, 4);
        checkGlError("glDrawArrays");
    }

    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader == 0) {
            return shader;
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, 35713, compiled, 0);
        if (compiled[0] != 0) {
            return shader;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not compile shader ");
        stringBuilder.append(shaderType);
        stringBuilder.append(":");
        Log.e(str, stringBuilder.toString());
        Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
        GLES20.glDeleteShader(shader);
        return 0;
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(35633, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(35632, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
            if (linkStatus[0] != 1) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private static void checkGlError(String op) {
        int glGetError = GLES20.glGetError();
        int error = glGetError;
        if (glGetError != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(op);
            stringBuilder.append(": glError ");
            stringBuilder.append(error);
            Log.e(TAG, stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(op);
            stringBuilder2.append(": glError ");
            stringBuilder2.append(error);
            throw new RuntimeException(stringBuilder2.toString());
        }
    }

    public static void activateFBO(TextureFBO tex_fbo) {
        GLES20.glBindFramebuffer(36160, tex_fbo.mFrameBuffer[0]);
        checkGlError("glBindFramebuffer");
        GLES20.glClear(16640);
        GLES20.glViewport(0, 0, tex_fbo.mWidth, tex_fbo.mHeight);
    }

    public static void inactivateFBO() {
        GLES20.glBindFramebuffer(36160, 0);
    }
}
