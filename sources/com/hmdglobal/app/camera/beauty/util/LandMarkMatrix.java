package com.hmdglobal.app.camera.beauty.util;

import android.opengl.GLES20;
import com.hmdglobal.app.camera.beauty.cameragl.OpenglUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class LandMarkMatrix {
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(this.points.length * 4).order(ByteOrder.nativeOrder());
    private boolean inited;
    private final String mFragmentShader = "precision mediump float;\nvoid main()\n{\n\tgl_FragColor = vec4(1.0,0.0,0.0, 1.0);\n}";
    protected int[] mFrameBuffers = null;
    private int mGLAttribPosition;
    private int mGLProgId;
    private final String mVertexShader = "attribute vec2 aPosition;\nvoid main()\n{\n  gl_Position = vec4(aPosition,1.0,1.0);\n  gl_PointSize = 10.0;\n}";
    private volatile int[] points = new int[162];

    public void setPoints(int[] points) {
        this.points = Arrays.copyOf(points, points.length);
    }

    public void drawLandMark(int textureId, int width, int height) {
        if (this.points != null) {
            checkInit();
            GLES20.glBindFramebuffer(36160, this.mFrameBuffers[0]);
            GLES20.glFramebufferTexture2D(36160, 36064, 3553, textureId, 0);
            GLES20.glUseProgram(this.mGLProgId);
            float[] points = getLandMarks(width, height);
            this.byteBuffer.asFloatBuffer().put(points);
            GLES20.glEnableVertexAttribArray(this.mGLAttribPosition);
            GLES20.glVertexAttribPointer(this.mGLAttribPosition, 2, 5126, false, 0, this.byteBuffer);
            GLES20.glDrawArrays(0, 0, points.length / 2);
            GLES20.glUseProgram(0);
            GLES20.glBindFramebuffer(36160, 0);
        }
    }

    private void checkInit() {
        if (!this.inited) {
            this.inited = true;
            this.mGLProgId = OpenglUtil.loadProgram("attribute vec2 aPosition;\nvoid main()\n{\n  gl_Position = vec4(aPosition,1.0,1.0);\n  gl_PointSize = 10.0;\n}", "precision mediump float;\nvoid main()\n{\n\tgl_FragColor = vec4(1.0,0.0,0.0, 1.0);\n}");
            this.mGLAttribPosition = GLES20.glGetAttribLocation(this.mGLProgId, "aPosition");
            this.mFrameBuffers = new int[1];
            GLES20.glGenFramebuffers(1, this.mFrameBuffers, 0);
        }
    }

    private float[] getLandMarks(int width, int height) {
        float[] result = new float[162];
        for (int i = 0; i < this.points.length; i += 2) {
            result[i] = ((((float) this.points[i]) / ((float) width)) * 2.0f) - 1.0f;
            result[i + 1] = ((((float) this.points[i + 1]) / ((float) height)) * 2.0f) - 1.0f;
        }
        return result;
    }
}
