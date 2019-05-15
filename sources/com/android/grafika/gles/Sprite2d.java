package com.android.grafika.gles;

import android.opengl.Matrix;
import com.android.ex.camera2.portability.Size;

public class Sprite2d {
    private static final String TAG = "Grafika";
    private float mAngle;
    private float[] mColor;
    private Drawable2d mDrawable;
    private boolean mMatrixReady;
    private float[] mModelViewMatrix;
    private float mPosX;
    private float mPosY;
    private float mScaleX;
    private float mScaleY;
    private float[] mScratchMatrix = new float[16];
    private int mTextureId;

    public Sprite2d(Drawable2d drawable) {
        this.mDrawable = drawable;
        this.mColor = new float[4];
        this.mColor[3] = 1.0f;
        this.mTextureId = -1;
        this.mModelViewMatrix = new float[16];
        this.mMatrixReady = false;
    }

    private void recomputeMatrix() {
        float[] modelView = this.mModelViewMatrix;
        Matrix.setIdentityM(modelView, 0);
        Matrix.translateM(modelView, 0, this.mPosX, this.mPosY, 0.0f);
        if (this.mAngle != 0.0f) {
            Matrix.rotateM(modelView, 0, this.mAngle, 0.0f, 0.0f, 1.0f);
        }
        Matrix.scaleM(modelView, 0, this.mScaleX, this.mScaleY, 1.0f);
        this.mMatrixReady = true;
    }

    public float getScaleX() {
        return this.mScaleX;
    }

    public float getScaleY() {
        return this.mScaleY;
    }

    public void setScale(float scaleX, float scaleY) {
        this.mScaleX = scaleX;
        this.mScaleY = scaleY;
        this.mMatrixReady = false;
    }

    public float getRotation() {
        return this.mAngle;
    }

    public void setRotation(float angle) {
        while (angle >= 360.0f) {
            angle -= 360.0f;
        }
        while (angle <= -360.0f) {
            angle += 360.0f;
        }
        this.mAngle = angle;
        this.mMatrixReady = false;
    }

    public float getPositionX() {
        return this.mPosX;
    }

    public float getPositionY() {
        return this.mPosY;
    }

    public void setPosition(float posX, float posY) {
        this.mPosX = posX;
        this.mPosY = posY;
        this.mMatrixReady = false;
    }

    public float[] getModelViewMatrix() {
        if (!this.mMatrixReady) {
            recomputeMatrix();
        }
        return this.mModelViewMatrix;
    }

    public void setColor(float red, float green, float blue) {
        this.mColor[0] = red;
        this.mColor[1] = green;
        this.mColor[2] = blue;
    }

    public void setTexture(int textureId) {
        this.mTextureId = textureId;
    }

    public float[] getColor() {
        return this.mColor;
    }

    public void draw(FlatShadedProgram program, float[] projectionMatrix) {
        Matrix.multiplyMM(this.mScratchMatrix, 0, projectionMatrix, 0, getModelViewMatrix(), 0);
        program.draw(this.mScratchMatrix, this.mColor, this.mDrawable.getVertexArray(), 0, this.mDrawable.getVertexCount(), this.mDrawable.getCoordsPerVertex(), this.mDrawable.getVertexStride());
    }

    public void draw(Texture2dProgram program, float[] projectionMatrix) {
        Matrix.multiplyMM(this.mScratchMatrix, 0, projectionMatrix, 0, getModelViewMatrix(), 0);
        Texture2dProgram texture2dProgram = program;
        texture2dProgram.draw(this.mScratchMatrix, this.mDrawable.getVertexArray(), 0, this.mDrawable.getVertexCount(), this.mDrawable.getCoordsPerVertex(), this.mDrawable.getVertexStride(), GlUtil.IDENTITY_MATRIX, this.mDrawable.getTexCoordArray(), this.mTextureId, this.mDrawable.getTexCoordStride());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Sprite2d pos=");
        stringBuilder.append(this.mPosX);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(this.mPosY);
        stringBuilder.append(" scale=");
        stringBuilder.append(this.mScaleX);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(this.mScaleY);
        stringBuilder.append(" angle=");
        stringBuilder.append(this.mAngle);
        stringBuilder.append(" color={");
        stringBuilder.append(this.mColor[0]);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(this.mColor[1]);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(this.mColor[2]);
        stringBuilder.append("} drawable=");
        stringBuilder.append(this.mDrawable);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
