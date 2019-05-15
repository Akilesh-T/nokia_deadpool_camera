package com.hmdglobal.app.camera.motion;

import android.graphics.RectF;
import android.util.Log;

public class YuvCropper {
    private static final boolean DEBUG = true;
    private static final String TAG = "YuvCropper";
    private int mCropHeight;
    private final int mCropLeft;
    private final int mCropTop;
    private int mCropWidth;
    private byte[] mData;
    private final int mHeight;
    private final int mWidth;
    private final int mYLength;

    public YuvCropper(int width, int height, RectF crop) {
        if (width <= 0 || height <= 0 || crop == null || crop.width() <= 0.0f || crop.height() <= 0.0f) {
            Log.d(TAG, "YuvCropper init error");
        }
        this.mWidth = width;
        this.mHeight = height;
        this.mYLength = this.mWidth * this.mHeight;
        this.mCropHeight = roundTo16((int) (crop.height() * ((float) this.mHeight)), this.mHeight);
        this.mCropWidth = roundTo16((int) (crop.width() * ((float) this.mWidth)), this.mWidth);
        this.mCropLeft = (((int) (crop.left * ((float) this.mWidth))) / 16) * 16;
        this.mCropTop = (((int) (crop.top * ((float) this.mHeight))) / 16) * 16;
        this.mData = new byte[(((this.mCropHeight * this.mCropWidth) * 3) / 2)];
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("crop size : ");
        stringBuilder.append(this.mCropWidth);
        stringBuilder.append("x");
        stringBuilder.append(this.mCropHeight);
        Log.d(str, stringBuilder.toString());
    }

    public byte[] crop(byte[] data) {
        if (data == null || data.length != (this.mYLength * 3) / 2) {
            Log.e(TAG, "invalid data");
            return this.mData;
        }
        processNV12(data);
        return this.mData;
    }

    private void processNV12(byte[] data) {
        int index = copy(data, this.mCropLeft + (this.mWidth * this.mCropTop), this.mWidth * (this.mCropTop + this.mCropHeight), this.mWidth, this.mCropWidth, 0);
        copy(data, (this.mYLength + ((this.mWidth * this.mCropTop) / 2)) + this.mCropLeft, this.mYLength + ((this.mWidth * (this.mCropTop + this.mCropHeight)) / 2), this.mWidth, this.mCropWidth, index);
    }

    private int copy(byte[] data, int start, int end, int oriStep, int newStep, int index) {
        int index2 = index;
        index = start;
        while (index < end) {
            System.arraycopy(data, index, this.mData, index2, newStep);
            index2 += newStep;
            index += oriStep;
        }
        return index2;
    }

    private int roundTo16(int size, int limit) {
        if (size >= limit) {
            return limit;
        }
        int m;
        float f = ((float) size) / 16.0f;
        int i = 16;
        if (f - ((float) ((int) f)) > 0.5f) {
            m = ((int) (((double) f) + 0.5d)) * 16;
        } else {
            m = ((int) f) * 16;
        }
        if (m >= 16) {
            i = m;
        }
        return i;
    }
}
