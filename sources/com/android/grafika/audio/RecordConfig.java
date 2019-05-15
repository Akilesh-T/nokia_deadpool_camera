package com.android.grafika.audio;

import com.hmdglobal.app.camera.motion.MotionPictureHelper;

public class RecordConfig {
    private int backCameraDirectionMode;
    private int bitRate;
    private int defaultCamera;
    private int frontCameraDirectionMode;
    private boolean printDetailMsg;
    private int renderingMode;
    private String saveVideoPath = null;
    private boolean square = false;
    private Size targetVideoSize;
    private int videoBufferQueueNum;
    private int videoFPS;
    private int videoGOP;

    private RecordConfig() {
    }

    public static RecordConfig obtain() {
        RecordConfig res = new RecordConfig();
        res.setRenderingMode(2);
        res.setTargetVideoSize(new Size(MotionPictureHelper.FRAME_WIDTH_4, MotionPictureHelper.FRAME_HEIGHT_3));
        res.setVideoFPS(25);
        res.setVideoGOP(1);
        res.setVideoBufferQueueNum(5);
        res.setBitRate(2000000);
        res.setPrintDetailMsg(false);
        res.setDefaultCamera(0);
        res.setBackCameraDirectionMode(16);
        res.setFrontCameraDirectionMode(16);
        return res;
    }

    public void setDefaultCamera(int defaultCamera) {
        this.defaultCamera = defaultCamera;
    }

    public void setFrontCameraDirectionMode(int frontCameraDirectionMode) {
        this.frontCameraDirectionMode = frontCameraDirectionMode;
    }

    public void setBackCameraDirectionMode(int backCameraDirectionMode) {
        this.backCameraDirectionMode = backCameraDirectionMode;
    }

    public void setRenderingMode(int renderingMode) {
        this.renderingMode = renderingMode;
    }

    public void setPrintDetailMsg(boolean printDetailMsg) {
        this.printDetailMsg = printDetailMsg;
    }

    public void setTargetVideoSize(Size videoSize) {
        this.targetVideoSize = videoSize;
    }

    public void setVideoBufferQueueNum(int num) {
        this.videoBufferQueueNum = num;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public int getVideoFPS() {
        return this.videoFPS;
    }

    public void setVideoFPS(int videoFPS) {
        this.videoFPS = videoFPS;
    }

    public int getVideoGOP() {
        return this.videoGOP;
    }

    public void setVideoGOP(int videoGOP) {
        this.videoGOP = videoGOP;
    }

    public int getVideoBufferQueueNum() {
        return this.videoBufferQueueNum;
    }

    public int getBitRate() {
        return this.bitRate;
    }

    public Size getTargetVideoSize() {
        return this.targetVideoSize;
    }

    public int getDefaultCamera() {
        return this.defaultCamera;
    }

    public int getBackCameraDirectionMode() {
        return this.backCameraDirectionMode;
    }

    public int getFrontCameraDirectionMode() {
        return this.frontCameraDirectionMode;
    }

    public int getRenderingMode() {
        return this.renderingMode;
    }

    public boolean isPrintDetailMsg() {
        return this.printDetailMsg;
    }

    public void setSquare(boolean enable) {
        this.square = enable;
    }

    public boolean isSquare() {
        return this.square;
    }

    public boolean isSaveVideoEnable() {
        return true;
    }

    public void setSaveVideoPath(String path) {
        this.saveVideoPath = path;
    }

    public String getSaveVideoPath() {
        return this.saveVideoPath;
    }
}
