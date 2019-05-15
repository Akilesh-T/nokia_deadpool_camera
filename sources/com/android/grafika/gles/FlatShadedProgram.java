package com.android.grafika.gles;

import android.opengl.GLES20;
import android.util.Log;
import java.nio.FloatBuffer;

public class FlatShadedProgram {
    private static final String FRAGMENT_SHADER = "precision mediump float;uniform vec4 uColor;void main() {    gl_FragColor = uColor;}";
    private static final String TAG = "Grafika";
    private static final String VERTEX_SHADER = "uniform mat4 uMVPMatrix;attribute vec4 aPosition;void main() {    gl_Position = uMVPMatrix * aPosition;}";
    private int mProgramHandle;
    private int maPositionLoc;
    private int muColorLoc;
    private int muMVPMatrixLoc;

    public FlatShadedProgram() {
        this.mProgramHandle = -1;
        this.muColorLoc = -1;
        this.muMVPMatrixLoc = -1;
        this.maPositionLoc = -1;
        this.mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (this.mProgramHandle != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Created program ");
            stringBuilder.append(this.mProgramHandle);
            Log.d("Grafika", stringBuilder.toString());
            this.maPositionLoc = GLES20.glGetAttribLocation(this.mProgramHandle, "aPosition");
            GlUtil.checkLocation(this.maPositionLoc, "aPosition");
            this.muMVPMatrixLoc = GLES20.glGetUniformLocation(this.mProgramHandle, "uMVPMatrix");
            GlUtil.checkLocation(this.muMVPMatrixLoc, "uMVPMatrix");
            this.muColorLoc = GLES20.glGetUniformLocation(this.mProgramHandle, "uColor");
            GlUtil.checkLocation(this.muColorLoc, "uColor");
            return;
        }
        throw new RuntimeException("Unable to create program");
    }

    public void release() {
        GLES20.glDeleteProgram(this.mProgramHandle);
        this.mProgramHandle = -1;
    }

    public void draw(float[] mvpMatrix, float[] color, FloatBuffer vertexBuffer, int firstVertex, int vertexCount, int coordsPerVertex, int vertexStride) {
        GlUtil.checkGlError("draw start");
        GLES20.glUseProgram(this.mProgramHandle);
        GlUtil.checkGlError("glUseProgram");
        GLES20.glUniformMatrix4fv(this.muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");
        GLES20.glUniform4fv(this.muColorLoc, 1, color, 0);
        GlUtil.checkGlError("glUniform4fv ");
        GLES20.glEnableVertexAttribArray(this.maPositionLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");
        GLES20.glVertexAttribPointer(this.maPositionLoc, coordsPerVertex, 5126, false, vertexStride, vertexBuffer);
        GlUtil.checkGlError("glVertexAttribPointer");
        GLES20.glDrawArrays(5, firstVertex, vertexCount);
        GlUtil.checkGlError("glDrawArrays");
        GLES20.glDisableVertexAttribArray(this.maPositionLoc);
        GLES20.glUseProgram(0);
    }
}
