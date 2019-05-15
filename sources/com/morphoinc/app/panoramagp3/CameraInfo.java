package com.morphoinc.app.panoramagp3;

import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.util.SizeF;
import com.morphoinc.app.LogFilter;
import java.util.ArrayList;

public class CameraInfo {
    public static final Range<Double> CAPTURE_GAIN_RANGE = new Range(Double.valueOf(0.98d), Double.valueOf(1.02d));
    private static final String LOG_TAG = "Camera2App";
    public static final int OPEN_STATE_CLOSE = 0;
    public static final int OPEN_STATE_OPENED = 2;
    public static final int OPEN_STATE_OPEN_REQUEST = 1;
    public static final Range<Double> PREVIEW_GAIN_RANGE = new Range(Double.valueOf(0.5d), Double.valueOf(2.0d));
    private Rect mActiveArray;
    private Range<Integer> mAeCompensationRange = new Range(Integer.valueOf(0), Integer.valueOf(0));
    private Rational mAeCompensationStep = new Rational(1, 1);
    private String mCameraId;
    private CameraCaptureSession mCaptureSession;
    private Size mCaptureSize;
    private Range<Long> mExposureTimeRange = new Range(Long.valueOf(1), Long.valueOf(1));
    private float mFocalLength;
    private int mHardwareLevel;
    private boolean mIsEnabledZsl;
    private Integer mMaxAnalogSensitivity = Integer.valueOf(1);
    private Long mMaxFrameDuration = Long.valueOf(1);
    private CameraDevice mOpenCameraDevice;
    private int mOpenState;
    private int mOrientation;
    private SizeF mPhysicalSize;
    private Size mPixelArraySize;
    private Size mPreviewSize;
    private Range<Integer> mSensitivityRange = new Range(Integer.valueOf(0), Integer.valueOf(0));
    private final ArrayList<Range<Integer>> mTargetFpsRanges = new ArrayList();

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public int getHardwareLevel() {
        return this.mHardwareLevel;
    }

    public void setHardwareLevel(int level) {
        this.mHardwareLevel = level;
    }

    public String getCameraId() {
        return this.mCameraId;
    }

    public void setCameraId(String id) {
        this.mCameraId = id;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CameraId='");
        stringBuilder.append(this.mCameraId);
        stringBuilder.append("'");
        LogFilter.i(str, stringBuilder.toString());
    }

    public CameraDevice getOpenCameraDevice() throws CameraAccessException {
        if (this.mOpenCameraDevice != null) {
            return this.mOpenCameraDevice;
        }
        throw new CameraAccessException(2);
    }

    public void setOpenCameraDevice(CameraDevice device) {
        this.mOpenCameraDevice = device;
        if (this.mOpenCameraDevice != null) {
            setOpenState(2);
        }
    }

    public int getPreviewWidth() {
        return this.mPreviewSize.getWidth();
    }

    public void setPreviewWidth(int width) {
        setPreviewSize(width, this.mPreviewSize.getHeight());
    }

    public int getPreviewHeight() {
        return this.mPreviewSize.getHeight();
    }

    public void setPreviewHeight(int hight) {
        setPreviewSize(this.mPreviewSize.getWidth(), hight);
    }

    public void setPreviewSize(int width, int height) {
        this.mPreviewSize = new Size(width, height);
    }

    public int getCaptureWidth() {
        return this.mCaptureSize.getWidth();
    }

    public void setCaptureWidth(int width) {
        setCaptureSize(width, this.mCaptureSize.getHeight());
    }

    public int getCaptureHeight() {
        return this.mCaptureSize.getHeight();
    }

    public void setCaptureHeight(int hight) {
        setCaptureSize(this.mCaptureSize.getWidth(), hight);
    }

    public void setCaptureSize(int width, int height) {
        this.mCaptureSize = new Size(width, height);
    }

    public CameraCaptureSession getCaptureSession() throws CameraAccessException {
        if (this.mCaptureSession != null) {
            return this.mCaptureSession;
        }
        throw new CameraAccessException(2);
    }

    public void setCaptureSession(CameraCaptureSession session) {
        this.mCaptureSession = session;
    }

    public ArrayList<Range<Integer>> getTargetFpsRanges() {
        return this.mTargetFpsRanges;
    }

    public void setTargetFpsRanges(Range<Integer>[] ranges) {
        this.mTargetFpsRanges.clear();
        for (Range<Integer> range : ranges) {
            this.mTargetFpsRanges.add(new Range((Integer) range.getLower(), (Integer) range.getUpper()));
        }
    }

    public CameraInfo() {
        clearCameraId();
        this.mHardwareLevel = 2;
        this.mOpenCameraDevice = null;
        this.mCaptureSession = null;
        setPreviewSize(0, 0);
        setCaptureSize(0, 0);
        this.mFocalLength = 0.0f;
        setPhysicalSize(0.0f, 0.0f);
        setPixelArraySize(0, 0);
        setActiveArraySize(0, 0, 0, 0);
        this.mOpenState = 0;
        this.mIsEnabledZsl = false;
    }

    public boolean isCameraEnabled() {
        return this.mCameraId.isEmpty() ^ 1;
    }

    private void clearCameraId() {
        this.mCameraId = "";
    }

    public void setFocalLength(float focalLength) {
        this.mFocalLength = focalLength;
    }

    public float getFocalLength() {
        return this.mFocalLength;
    }

    public void setPhysicalSize(float width, float height) {
        this.mPhysicalSize = new SizeF(width, height);
    }

    public float getPhysicalWidth() {
        return this.mPhysicalSize.getWidth();
    }

    public float getPhysicalHeight() {
        return this.mPhysicalSize.getHeight();
    }

    public void setPixelArraySize(int width, int height) {
        this.mPixelArraySize = new Size(width, height);
    }

    public int getPixelArrayWidth() {
        return this.mPixelArraySize.getWidth();
    }

    public int getPixelArrayHeight() {
        return this.mPixelArraySize.getHeight();
    }

    public void setActiveArraySize(int left, int top, int right, int bottom) {
        this.mActiveArray = new Rect(left, top, right, bottom);
    }

    public int getActiveArrayWidth() {
        return this.mActiveArray.width();
    }

    public int getActiveArrayHeight() {
        return this.mActiveArray.height();
    }

    public int getActiveArrayLeft() {
        return this.mActiveArray.left;
    }

    public int getActiveArrayRight() {
        return this.mActiveArray.right;
    }

    public int getActiveArrayTop() {
        return this.mActiveArray.top;
    }

    public int getActiveArrayBottom() {
        return this.mActiveArray.bottom;
    }

    public void onCloseCamera(CameraDevice camera) {
        if (this.mCaptureSession != null) {
            this.mCaptureSession.close();
            this.mCaptureSession = null;
        }
        if (this.mOpenCameraDevice != null && this.mOpenCameraDevice == camera) {
            this.mOpenCameraDevice.close();
            this.mOpenCameraDevice = null;
        }
        setOpenState(0);
    }

    public void onCloseCamera() {
        onCloseCamera(this.mOpenCameraDevice);
    }

    public void abortCaptures() {
        if (this.mCaptureSession != null) {
            try {
                this.mCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void setExposureTimeRange(Long lower, Long upper) {
        this.mExposureTimeRange = new Range(lower, upper);
    }

    public final Range<Long> getExposureTimeRange() {
        return this.mExposureTimeRange;
    }

    public Long getExposureTimeRangeLength() {
        return Long.valueOf(((Long) this.mExposureTimeRange.getUpper()).longValue() - ((Long) this.mExposureTimeRange.getLower()).longValue());
    }

    public Long getExposureTimeMin() {
        return (Long) this.mExposureTimeRange.getLower();
    }

    public Long getExposureTimeMax() {
        return (Long) this.mExposureTimeRange.getUpper();
    }

    public boolean containsExposureTime(Long value) {
        return this.mExposureTimeRange.contains(value);
    }

    public Long clampExposureTime(Long value) {
        return (Long) this.mExposureTimeRange.clamp(value);
    }

    public void setMaxFrameDuration(Long duration) {
        this.mMaxFrameDuration = duration;
    }

    public Long getMaxFrameDuration() {
        return this.mMaxFrameDuration;
    }

    public boolean containsFrameDuration(Long value) {
        return value.longValue() <= this.mMaxFrameDuration.longValue();
    }

    public void setSensitivityRange(Integer lower, Integer upper) {
        this.mSensitivityRange = new Range(lower, upper);
    }

    public final Range<Integer> getSensitivityRange() {
        return this.mSensitivityRange;
    }

    public int getSensitivityRangeLength() {
        return ((Integer) this.mSensitivityRange.getUpper()).intValue() - ((Integer) this.mSensitivityRange.getLower()).intValue();
    }

    public int getSensitivityMin() {
        return ((Integer) this.mSensitivityRange.getLower()).intValue();
    }

    public int getSensitivityMax() {
        return ((Integer) this.mSensitivityRange.getUpper()).intValue();
    }

    public boolean containsSensitivity(Integer value) {
        return this.mSensitivityRange.contains(value);
    }

    public int clampSensitivityRange(int value) {
        return ((Integer) this.mSensitivityRange.clamp(Integer.valueOf(value))).intValue();
    }

    public void setMaxAnalogSensitivity(Integer sensitivity) {
        this.mMaxAnalogSensitivity = sensitivity;
    }

    public int getMaxAnalogSensitivity() {
        return this.mMaxAnalogSensitivity.intValue();
    }

    public void setAeCompensationStep(Rational step) {
        this.mAeCompensationStep = new Rational(step.getNumerator(), step.getDenominator());
    }

    public double getAeCompensationStep() {
        return this.mAeCompensationStep.doubleValue();
    }

    public int getAeCompensationNumerator() {
        return this.mAeCompensationStep.getNumerator();
    }

    public int getAeCompensationDenominator() {
        return this.mAeCompensationStep.getDenominator();
    }

    public void setAeCompensationRange(Integer lower, Integer upper) {
        this.mAeCompensationRange = new Range(lower, upper);
    }

    public int getAeCompensationMin() {
        return ((Integer) this.mAeCompensationRange.getLower()).intValue();
    }

    public int getAeCompensationMax() {
        return ((Integer) this.mAeCompensationRange.getUpper()).intValue();
    }

    public void setOpenState(int state) {
        this.mOpenState = state;
    }

    public int getOpenState() {
        return this.mOpenState;
    }

    public void setEnabledZsl(boolean enabled) {
        this.mIsEnabledZsl = enabled;
    }

    public boolean isEnabledZsl() {
        return this.mIsEnabledZsl;
    }
}
