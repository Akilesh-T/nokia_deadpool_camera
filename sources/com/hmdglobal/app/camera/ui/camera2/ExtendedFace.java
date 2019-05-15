package com.hmdglobal.app.camera.ui.camera2;

public class ExtendedFace {
    private int mBlinkDetected = 0;
    private int mGazeAngle = 0;
    private int mGazeDirection = 0;
    private int mId;
    private int mLeftrightGaze = 0;
    private int mLeyeBlink = 0;
    private int mReyeBlink = 0;
    private int mRollDirection = 0;
    private int mSmileConfidence = 0;
    private int mSmileDegree = 0;
    private int mTopbottomGaze = 0;

    public ExtendedFace(int id) {
        this.mId = id;
    }

    public int getBlinkDetected() {
        return this.mBlinkDetected;
    }

    public int getLeyeBlink() {
        return this.mLeyeBlink;
    }

    public int getReyeBlink() {
        return this.mReyeBlink;
    }

    public int getSmileDegree() {
        return this.mSmileDegree;
    }

    public int getSmileConfidence() {
        return this.mSmileConfidence;
    }

    public int getLeftrightGaze() {
        return this.mLeftrightGaze;
    }

    public int getTopbottomGaze() {
        return this.mTopbottomGaze;
    }

    public int getGazeDirection() {
        return this.mGazeDirection;
    }

    public int getRollDirection() {
        return this.mRollDirection;
    }

    public void setBlinkDetected(int blinkDetected) {
        this.mBlinkDetected = blinkDetected;
    }

    public void setBlinkDegree(byte left, byte right) {
        this.mLeyeBlink = left;
        this.mReyeBlink = right;
    }

    public void setSmileDegree(byte smileDegree) {
        this.mSmileDegree = smileDegree;
    }

    public void setGazeDirection(int topbottomGaze, int leftrightGaze, int rollDirection) {
        this.mTopbottomGaze = topbottomGaze;
        this.mLeftrightGaze = leftrightGaze;
        this.mRollDirection = rollDirection;
    }

    public void setGazeAngle(byte gazeAngle) {
        this.mGazeAngle = gazeAngle;
    }

    public void setSmileConfidence(int smileConfidence) {
        this.mSmileConfidence = smileConfidence;
    }
}
