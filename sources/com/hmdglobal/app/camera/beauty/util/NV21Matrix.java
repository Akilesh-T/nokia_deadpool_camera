package com.hmdglobal.app.camera.beauty.util;

import android.opengl.GLES20;
import android.util.Log;
import com.hmdglobal.app.camera.beauty.cameragl.OpenglUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NV21Matrix {
    public static final int NO_TEXTURE = -1;
    private int attr_pos;
    private int attr_tex;
    private int height;
    private boolean inited;
    private String mFragmentShader = "precision mediump float;\nvarying vec2 v_texCoord;\nuniform sampler2D y_texture;\nuniform sampler2D uv_texture;\nvoid main (void){\n   float r, g, b, y, u, v;\n   y = texture2D(y_texture, v_texCoord).r;\n   u = texture2D(uv_texture, v_texCoord).a - 0.5;\n   v = texture2D(uv_texture, v_texCoord).r - 0.5;\n   r = y + 1.370705*v;\n   g = y - 0.337633*u - 0.698001*v;\n   b = y + 1.732446*u;\n   gl_FragColor = vec4(r, g, b, 1.0);\n}\n";
    protected int[] mFrameBuffers = null;
    private int mGLProgId;
    private int mOutputHeight;
    private int mOutputWidth;
    String mVertexShader = "attribute vec2 a_position;                         \nattribute vec2 a_texCoord;                         \nvarying vec2 v_texCoord;                           \nvoid main(){                                       \n   gl_Position = vec4(a_position, 1, 1);                       \n   v_texCoord = a_texCoord;                        \n}                                                  \n";
    private ByteBuffer texBuffer;
    private final float[] texturePoint = new float[]{0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};
    private ByteBuffer uvBuffer;
    private int uvTexture = -1;
    private int uvloc;
    private ByteBuffer vertexBuffer;
    private final float[] vertexPoint = new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    private int width;
    private ByteBuffer yBuffer;
    private int yTexture = -1;
    private int yloc;

    public void destroy() {
        GLES20.glDeleteFramebuffers(1, this.mFrameBuffers, 0);
        unloadTexture(this.yTexture);
        unloadTexture(this.uvTexture);
    }

    public void setOutputSize(int width, int height) {
        this.mOutputWidth = width;
        this.mOutputHeight = height;
    }

    public void renderNv21(byte[] nv21Data, int textureId, int width, int height, boolean isFrontCamera) {
        byte[] bArr = nv21Data;
        int i = width;
        int i2 = height;
        if (i == 0 || i2 == 0) {
            int i3 = textureId;
            boolean z = isFrontCamera;
            return;
        }
        checkInit(i, i2, isFrontCamera);
        GLES20.glViewport(0, 0, this.mOutputWidth, this.mOutputHeight);
        GLES20.glBindFramebuffer(36160, this.mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(36160, 36064, 3553, textureId, 0);
        GLES20.glUseProgram(this.mGLProgId);
        GLES20.glUniform1i(this.yloc, 0);
        GLES20.glUniform1i(this.uvloc, 1);
        GLES20.glEnableVertexAttribArray(this.attr_pos);
        GLES20.glEnableVertexAttribArray(this.attr_tex);
        GLES20.glVertexAttribPointer(this.attr_pos, 2, 5126, false, 0, this.vertexBuffer);
        GLES20.glVertexAttribPointer(this.attr_tex, 2, 5126, false, 0, this.texBuffer);
        GLES20.glActiveTexture(33984);
        this.yBuffer = ByteBuffer.wrap(bArr, 0, i * i2);
        this.uvBuffer.put(bArr, i * i2, (i * i2) / 2);
        this.uvBuffer.position(0);
        this.yTexture = loadTexture(this.yBuffer, i, i2, 6409, this.yTexture);
        this.yBuffer = null;
        GLES20.glActiveTexture(33985);
        this.uvTexture = loadTexture(this.uvBuffer, i / 2, i2 / 2, 6410, this.uvTexture);
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDisableVertexAttribArray(this.attr_pos);
        GLES20.glDisableVertexAttribArray(this.attr_tex);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, 0);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, 0);
        GLES20.glUseProgram(0);
    }

    private void checkError() {
        if (GLES20.glGetError() != 0) {
            Log.e("wangshuai", "error", new Exception());
        }
    }

    public static int unloadTexture(int texture) {
        if (texture != -1) {
            GLES20.glDeleteTextures(1, new int[]{texture}, 0);
        }
        return -1;
    }

    public static int loadTexture(ByteBuffer data, int width, int height, int type, int usedTexId) {
        int usedTexId2;
        int i = usedTexId;
        if (i == -1) {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);
            usedTexId2 = textures[0];
            GLES20.glBindTexture(3553, textures[0]);
            GLES20.glTexParameterf(3553, 10241, 9729.0f);
            GLES20.glTexParameterf(3553, 10240, 9729.0f);
            GLES20.glTexParameterf(3553, 10242, 33071.0f);
            GLES20.glTexParameterf(3553, 10243, 33071.0f);
            GLES20.glTexImage2D(3553, 0, type, width, height, 0, type, 5121, null);
        } else {
            usedTexId2 = i;
        }
        GLES20.glBindTexture(3553, usedTexId2);
        GLES20.glTexSubImage2D(3553, 0, 0, 0, width, height, type, 5121, data);
        return usedTexId2;
    }

    private void checkInit(int width, int height, boolean isFrontCamera) {
        if (!this.inited) {
            this.inited = true;
            this.width = width;
            this.height = height;
            this.mGLProgId = OpenglUtil.loadProgram(this.mVertexShader, this.mFragmentShader);
            this.mFrameBuffers = new int[1];
            GLES20.glGenFramebuffers(1, this.mFrameBuffers, 0);
            this.yTexture = -1;
            this.uvTexture = -1;
            this.attr_pos = GLES20.glGetAttribLocation(this.mGLProgId, "a_position");
            this.attr_tex = GLES20.glGetAttribLocation(this.mGLProgId, "a_texCoord");
            this.yloc = GLES20.glGetUniformLocation(this.mGLProgId, "y_texture");
            this.uvloc = GLES20.glGetUniformLocation(this.mGLProgId, "uv_texture");
            this.yBuffer = ByteBuffer.allocateDirect(width * height);
            this.uvBuffer = ByteBuffer.allocateDirect((width * height) / 2);
            this.yBuffer.order(ByteOrder.nativeOrder());
            this.uvBuffer.order(ByteOrder.nativeOrder());
            this.vertexBuffer = ByteBuffer.allocateDirect(this.vertexPoint.length * 4);
            this.vertexBuffer.order(ByteOrder.nativeOrder());
            this.vertexBuffer.asFloatBuffer().put(this.vertexPoint);
            this.vertexBuffer.position(0);
            this.texBuffer = ByteBuffer.allocateDirect(this.texturePoint.length * 4);
            this.texBuffer.order(ByteOrder.nativeOrder());
            this.texBuffer.asFloatBuffer().put(this.texturePoint);
            this.texBuffer.position(0);
        } else if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            this.yTexture = unloadTexture(this.yTexture);
            this.uvTexture = unloadTexture(this.uvTexture);
            this.yBuffer = ByteBuffer.allocateDirect(width * height);
            this.uvBuffer = ByteBuffer.allocateDirect((width * height) / 2);
            this.yBuffer.order(ByteOrder.nativeOrder());
            this.uvBuffer.order(ByteOrder.nativeOrder());
        }
    }
}
