package com.morphoinc.app.panoramagp3;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureRequest.Key;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.os.EnvironmentCompat;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.view.TextureView;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.camera_states.CameraState;
import com.morphoinc.app.camera_states.IMorphoPanoramaGP3Callback;
import com.morphoinc.app.camera_states.MorphoPanoramaGP3CameraState;
import com.morphoinc.app.camera_states.MorphoPanoramaGP3CameraState.CameraStartupInfo;
import com.morphoinc.app.camera_states.PreviewState;
import com.morphoinc.app.camera_states.TvRequestParams;
import com.morphoinc.app.panoramagp3.MorphoCameraBase.IMorphoCameraListener;
import com.morphoinc.utils.os.BuildUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MorphoCamera extends MorphoCameraBase {
    private static final String LOG_TAG = "MorphoCamera2";
    private final CameraMode[] HARDWARE_LEVEL = new CameraMode[]{new CameraMode(1, "full"), new CameraMode(2, "legacy"), new CameraMode(0, "limited"), new CameraMode(3, "level_3")};
    private final CameraMode[] TIMESTAMP_SOURCE = new CameraMode[]{new CameraMode(1, "real time"), new CameraMode(0, EnvironmentCompat.MEDIA_UNKNOWN)};
    private HandlerThread mBackgroundHandlerThread = null;
    private final CameraInfo mCameraInfo;
    private final CameraManager mCameraManager;
    private MorphoPanoramaGP3CameraState mCameraState;
    private final CaptureCallback mCaptureCallback = new CaptureCallback() {
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            MorphoCamera.this.mCameraState.onProgressed(partialResult);
        }

        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            MorphoCamera.this.mCameraState.onCaptureCompleted(request, result);
            MorphoCamera.this.mListener.onCaptureCompleted(request, result);
        }

        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
            MorphoCamera.this.mCameraState.onCaptureSequenceCompleted(sequenceId);
        }

        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            String str = MorphoCamera.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CameraCaptureSession.CaptureCallback.onCaptureFailed >Reason:");
            stringBuilder.append(failure.getReason());
            LogFilter.w(str, stringBuilder.toString());
        }
    };
    private ImageReader mImageReader;
    private ImageReader mImageReaderIdling;
    private final IMorphoCameraListener mListener;
    private final OnImageAvailableListener mPreviewAvailableListener = new OnImageAvailableListener() {
        public void onImageAvailable(ImageReader reader) {
            Image image;
            if (MorphoCamera.this.mListener.onPreviewImageAvailable()) {
                if (!MorphoCamera.this.mCameraState.camera2Params().tvAll()) {
                    image = reader.acquireNextImage();
                    if (image != null) {
                        image.close();
                    }
                }
                return;
            }
            image = reader.acquireNextImage();
            if (image != null) {
                image.close();
            }
        }
    };
    private Surface mPreviewSurface;
    private final StateCallback mSessionCallback = new StateCallback() {
        public void onConfigured(@NonNull CameraCaptureSession session) {
            MorphoCamera.this.mCameraInfo.setCaptureSession(session);
            if (MorphoCamera.this.mCameraState != null) {
                MorphoCamera.this.mCameraState = new PreviewState();
            }
            MorphoCamera.this.mCameraState.onStart();
        }

        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            MorphoCamera.this.mCameraInfo.onCloseCamera();
            MorphoCamera.this.mPreviewSurface = null;
            MorphoCamera.this.mCameraState.setPreviewSurface(null);
        }
    };
    private final Comparator<Size> mSizeComparator = new Comparator<Size>() {
        public int compare(Size lhs, Size rhs) {
            int result = rhs.getWidth() - lhs.getWidth();
            if (result == 0) {
                return rhs.getHeight() - lhs.getHeight();
            }
            return result;
        }
    };
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        public void onOpened(@NonNull CameraDevice camera) {
            MorphoCamera.this.mCameraInfo.setOpenCameraDevice(camera);
            SurfaceTexture texture = null;
            if (MorphoCamera.this.mTextureView != null) {
                texture = MorphoCamera.this.mTextureView.getSurfaceTexture();
            }
            if (texture == null) {
                LogFilter.w(MorphoCamera.LOG_TAG, "CameraDevice.StateCallback.onOpened SurfaceTexture is null!!");
                camera.close();
                MorphoCamera.this.mCameraInfo.setOpenState(0);
            } else if (MorphoCamera.this.mImageReader == null) {
                LogFilter.w(MorphoCamera.LOG_TAG, "CameraDevice.StateCallback.onOpened ImageReader is null!!");
                camera.close();
                MorphoCamera.this.mCameraInfo.setOpenState(0);
            } else {
                if (BuildUtil.isHuaweiP9()) {
                    texture.setDefaultBufferSize(1920, 1440);
                } else {
                    texture.setDefaultBufferSize(MorphoCamera.this.mCameraInfo.getPreviewWidth(), MorphoCamera.this.mCameraInfo.getPreviewHeight());
                }
                MorphoCamera.this.mPreviewSurface = new Surface(texture);
                try {
                    MorphoCamera.this.mCameraInfo.getOpenCameraDevice().createCaptureSession(Arrays.asList(new Surface[]{MorphoCamera.this.mPreviewSurface, MorphoCamera.this.mImageReader.getSurface(), MorphoCamera.this.mImageReaderIdling.getSurface()}), MorphoCamera.this.mSessionCallback, MorphoCamera.this.mCameraState.backgroundHandler());
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    MorphoCamera.this.mCameraInfo.onCloseCamera(camera);
                    MorphoCamera.this.mPreviewSurface = null;
                }
                MorphoCamera.this.mCameraState.setPreviewSurface(MorphoCamera.this.mPreviewSurface);
                MorphoCamera.this.mListener.onOpened();
            }
        }

        public void onDisconnected(@NonNull CameraDevice camera) {
            MorphoCamera.this.mCameraInfo.onCloseCamera(camera);
            MorphoCamera.this.mPreviewSurface = null;
            MorphoCamera.this.mCameraState.setPreviewSurface(null);
        }

        public void onError(@NonNull CameraDevice camera, int error) {
            LogFilter.e(MorphoCamera.LOG_TAG, String.format(Locale.US, "CameraDevice.StateCallback.onError (%d)", new Object[]{Integer.valueOf(error)}));
        }
    };
    private final OnImageAvailableListener mTakePictureAvailableListener = new OnImageAvailableListener() {
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if (image == null) {
                LogFilter.w(MorphoCamera.LOG_TAG, "ImageReader#acquireNextImage() is null.");
                return;
            }
            Camera2Image captureImage = new Camera2Image(image);
            TotalCaptureResult totalCaptureResult = MorphoCamera.this.mCameraState.getCameraStartupInfo().totalCaptureResult;
            Integer sensitivity = (Integer) totalCaptureResult.get(TotalCaptureResult.SENSOR_SENSITIVITY);
            Long exposureTime = (Long) totalCaptureResult.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME);
            Long rollingShutterSkew = (Long) totalCaptureResult.get(TotalCaptureResult.SENSOR_ROLLING_SHUTTER_SKEW);
            Long timestamp = (Long) totalCaptureResult.get(TotalCaptureResult.SENSOR_TIMESTAMP);
            if (sensitivity != null) {
                captureImage.setSensitivity(sensitivity.intValue());
            }
            if (exposureTime != null) {
                captureImage.setExposureTime(exposureTime.longValue());
            }
            if (rollingShutterSkew != null) {
                captureImage.setRollingShutterSkew(rollingShutterSkew.longValue());
            }
            if (timestamp != null) {
                captureImage.setSensorTimeStamp(timestamp.longValue());
            }
            if (!MorphoCamera.this.mListener.onPictureTaken(captureImage)) {
                captureImage.close();
            }
        }
    };
    private TextureView mTextureView = null;
    private long mTvTargetExposureTime;
    private int mTvTargetSensorSensitivity;

    private class CameraMode {
        public int key;
        public String str;

        CameraMode(int key, String str) {
            this.key = key;
            this.str = str;
        }
    }

    public CameraInfo cameraInfo() {
        return this.mCameraInfo;
    }

    public final CameraState cameraState() {
        return this.mCameraState;
    }

    public int burstRemaining() {
        return this.mCameraState.getCameraStartupInfo().burstRemaining;
    }

    public void setBurstRemaining(int value) {
        this.mCameraState.setBurstRemaining(value);
    }

    public void setMorphoPanoramaGP3Interface(IMorphoPanoramaGP3Callback callback) {
        this.mCameraState.setMorphoPanoramaGP3Interface(callback);
    }

    public void setCamera2Params(Camera2ParamsFragment fragment) {
        this.mCameraState.setCamera2Params(fragment);
    }

    public MorphoCamera(IMorphoCameraListener listener, Activity parent, int camera_id, int capture_mode) {
        this.mCameraManager = (CameraManager) parent.getSystemService("camera");
        this.mCameraInfo = new CameraInfo();
        this.mCameraInfo.setCameraId(String.valueOf(camera_id));
        if (listener == null) {
            listener = nullMorphoCameraListener;
        }
        this.mListener = listener;
        this.mCameraState = new MorphoPanoramaGP3CameraState();
        MorphoPanoramaGP3CameraState.initialize(capture_mode);
        this.mBackgroundHandlerThread = new HandlerThread("BackgroundHandlerThread");
        this.mBackgroundHandlerThread.start();
        this.mCameraState.setBackgroundHandlerThread(this.mBackgroundHandlerThread);
    }

    public void exit() {
        if (this.mBackgroundHandlerThread != null) {
            this.mBackgroundHandlerThread.quit();
            this.mBackgroundHandlerThread = null;
        }
        this.mCameraState.setBackgroundHandlerThread(null);
    }

    public void pause() {
        this.mCameraState.setPreviewSurface(null);
        if (BuildUtil.isSamsung()) {
            try {
                Builder builder = this.mCameraInfo.getOpenCameraDevice().createCaptureRequest(2);
                builder.addTarget(this.mPreviewSurface);
                this.mCameraInfo.getCaptureSession().capture(builder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        this.mCameraInfo.onCloseCamera();
        this.mPreviewSurface = null;
        if (this.mImageReader != null) {
            this.mImageReader.close();
            this.mImageReader = null;
        }
        if (this.mImageReaderIdling != null) {
            this.mImageReaderIdling.close();
            this.mImageReaderIdling = null;
        }
        this.mCameraState.setImageReader(null);
        this.mCameraState.setImageReaderIdling(null);
    }

    public void resume(Size captureSize, Size previewSize) {
        this.mCameraInfo.setCaptureSize(captureSize.getWidth(), captureSize.getHeight());
        this.mCameraInfo.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        Handler backgroundHandler = this.mCameraState.backgroundHandler();
        this.mImageReader = ImageReader.newInstance(this.mCameraInfo.getCaptureWidth(), this.mCameraInfo.getCaptureHeight(), 35, 20);
        this.mImageReader.setOnImageAvailableListener(this.mPreviewAvailableListener, backgroundHandler);
        this.mImageReaderIdling = ImageReader.newInstance(320, ((int) ((((float) 320) * ((float) this.mCameraInfo.getCaptureHeight())) / ((float) this.mCameraInfo.getCaptureWidth()))) & -2, 35, 3);
        this.mImageReaderIdling.setOnImageAvailableListener(this.mPreviewAvailableListener, backgroundHandler);
        this.mCameraState.setCameraInfo(this.mCameraInfo);
        this.mCameraState.setCaptureCallback(this.mCaptureCallback);
        this.mCameraState.setImageReader(this.mImageReader);
        this.mCameraState.setImageReaderIdling(this.mImageReaderIdling);
        this.mCameraState.setOnPreviewImageAvailableListener(this.mPreviewAvailableListener);
        this.mCameraState.setOnTakePictureImageAvailableListener(this.mTakePictureAvailableListener);
    }

    public String[] getAllCameras() {
        String[] cameras = null;
        try {
            int i;
            String[] id_list = this.mCameraManager.getCameraIdList();
            cameras = new String[id_list.length];
            int i2 = 0;
            int other = 0;
            int front = 0;
            int back = 0;
            for (i = 0; i < id_list.length; i++) {
                Integer lens_facing = (Integer) this.mCameraManager.getCameraCharacteristics(id_list[i]).get(CameraCharacteristics.LENS_FACING);
                Object[] objArr;
                if (lens_facing != null) {
                    switch (lens_facing.intValue()) {
                        case 0:
                            objArr = new Object[1];
                            front++;
                            objArr[0] = Integer.valueOf(front);
                            cameras[i] = String.format(Locale.US, "Front %d", objArr);
                            break;
                        case 1:
                            objArr = new Object[1];
                            back++;
                            objArr[0] = Integer.valueOf(back);
                            cameras[i] = String.format(Locale.US, "Back %d", objArr);
                            break;
                        default:
                            Locale locale = Locale.US;
                            objArr = new Object[1];
                            other++;
                            objArr[0] = Integer.valueOf(other);
                            cameras[i] = String.format(locale, "Other %d", objArr);
                            break;
                    }
                }
                objArr = new Object[1];
                other++;
                objArr[0] = Integer.valueOf(other);
                cameras[i] = String.format(Locale.US, "Other %d", objArr);
            }
            if (other == 1) {
                for (i = 0; i < cameras.length; i++) {
                    if (cameras[i].contains("Other")) {
                        cameras[i] = cameras[i].replace(" 1", "");
                    }
                }
            }
            if (front == 1) {
                for (i = 0; i < cameras.length; i++) {
                    if (cameras[i].contains("Front")) {
                        cameras[i] = cameras[i].replace(" 1", "");
                    }
                }
            }
            if (back == 1) {
                while (true) {
                    i = i2;
                    if (i < cameras.length) {
                        if (cameras[i].contains("Back")) {
                            cameras[i] = cameras[i].replace(" 1", "");
                        }
                        i2 = i + 1;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameras;
    }

    public boolean isFrontCamera(int camera_id) {
        boolean z = false;
        boolean ret = false;
        try {
            Integer lens_facing = (Integer) this.mCameraManager.getCameraCharacteristics(String.valueOf(camera_id)).get(CameraCharacteristics.LENS_FACING);
            if (lens_facing != null && lens_facing.intValue() == 0) {
                z = true;
            }
            return z;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return ret;
        }
    }

    public boolean openCamera(TextureView textureView) {
        this.mTextureView = textureView;
        CameraStartupInfo cameraStartupInfo = this.mCameraState.getCameraStartupInfo();
        CameraManager cameraManager = this.mCameraManager;
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId());
            Integer hw_level = (Integer) characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            Integer orientation = (Integer) characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Integer num;
            Integer num2;
            CameraCharacteristics cameraCharacteristics;
            if (hw_level == null) {
                num = hw_level;
                num2 = orientation;
            } else if (orientation == null) {
                cameraCharacteristics = characteristics;
                num = hw_level;
                num2 = orientation;
            } else {
                int[] values;
                boolean aeModeOn;
                boolean aeModeOff;
                boolean aeModeOff2;
                int aeModeOn2;
                int length;
                int mode;
                int afAuto;
                boolean isAvailableYuvReprocessing;
                this.mCameraInfo.setHardwareLevel(hw_level.intValue());
                this.mCameraInfo.setOrientation(orientation.intValue());
                float[] focalLengths = (float[]) characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                SizeF physicalSize = (SizeF) characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                Size pixelArraySize = (Size) characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                Rect activeRect = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                Camera2ParamsFragment fragment = this.mCameraState.camera2Params();
                if (isOverLevelFull()) {
                    Range<Long> exposureTimeRange = (Range) characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                    Long maxFrameDuration = (Long) characteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);
                    Range<Integer> sensitivityRange = (Range) characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                    Integer maxAnalogSensitivity = (Integer) characteristics.get(CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY);
                    if (exposureTimeRange != null) {
                        this.mCameraInfo.setExposureTimeRange((Long) exposureTimeRange.getLower(), (Long) exposureTimeRange.getUpper());
                    } else {
                        num2 = orientation;
                    }
                    if (maxFrameDuration != null) {
                        this.mCameraInfo.setMaxFrameDuration(maxFrameDuration);
                    }
                    if (sensitivityRange != null) {
                        this.mCameraInfo.setSensitivityRange((Integer) sensitivityRange.getLower(), (Integer) sensitivityRange.getUpper());
                    }
                    if (maxAnalogSensitivity != null) {
                        this.mCameraInfo.setMaxAnalogSensitivity(maxAnalogSensitivity);
                    }
                    Range<Integer> aeRange = (Range) characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                    Rational hw_level2 = (Rational) characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
                    if (aeRange != null) {
                        this.mCameraInfo.setAeCompensationRange((Integer) aeRange.getLower(), (Integer) aeRange.getUpper());
                    } else {
                        Range<Integer> range = sensitivityRange;
                    }
                    if (hw_level2 != null) {
                        this.mCameraInfo.setAeCompensationStep(hw_level2);
                    }
                    cameraStartupInfo.available_ae_mode = true;
                    cameraStartupInfo.available_af_mode = true;
                    cameraStartupInfo.available_scene_mode_sports = true;
                    cameraStartupInfo.available_antibanding_mode = true;
                    cameraStartupInfo.available_image_quality_settings = true;
                    Camera2ParamsFragment camera2ParamsFragment = fragment;
                } else {
                    boolean afContinuous;
                    boolean afContinuous2;
                    boolean smSports;
                    boolean antibandingOff;
                    boolean antibanding60Hz;
                    boolean antibanding50Hz;
                    num2 = orientation;
                    values = (int[]) characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
                    aeModeOn = false;
                    if (values != null) {
                        aeModeOff = false;
                        aeModeOff2 = false;
                        for (int mode2 : values) {
                            aeModeOff2 |= mode2 == 1 ? 1 : 0;
                            aeModeOff |= mode2 == 0 ? 1 : 0;
                        }
                        aeModeOn = aeModeOff2;
                    } else {
                        aeModeOff = false;
                    }
                    aeModeOff2 = aeModeOn && aeModeOff;
                    cameraStartupInfo.available_ae_mode = aeModeOff2;
                    values = (int[]) characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
                    aeModeOff2 = false;
                    int[] values2;
                    boolean aeModeOn3;
                    if (values != null) {
                        mode2 = values.length;
                        afContinuous = false;
                        afContinuous2 = false;
                        afAuto = 0;
                        while (afAuto < mode2) {
                            values2 = values;
                            aeModeOn3 = aeModeOn;
                            values = values[afAuto];
                            afContinuous2 |= values == true ? 1 : 0;
                            afContinuous |= values == 4 ? 1 : 0;
                            afAuto++;
                            values = values2;
                            aeModeOn = aeModeOn3;
                        }
                        aeModeOn3 = aeModeOn;
                        aeModeOff2 = afContinuous2;
                    } else {
                        values2 = values;
                        aeModeOn3 = aeModeOn;
                        afContinuous = false;
                    }
                    boolean z = aeModeOff2 && afContinuous;
                    cameraStartupInfo.available_af_mode = z;
                    values = (int[]) characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
                    boolean z2;
                    if (values != null) {
                        length = values.length;
                        smSports = false;
                        aeModeOn2 = 0;
                        while (aeModeOn2 < length) {
                            smSports |= values[aeModeOn2] == 13 ? 1 : 0;
                            aeModeOn2++;
                            values = values;
                            aeModeOff2 = aeModeOff2;
                        }
                        z2 = aeModeOff2;
                    } else {
                        int[] iArr = values;
                        z2 = aeModeOff2;
                        smSports = false;
                    }
                    cameraStartupInfo.available_scene_mode_sports = smSports;
                    values = (int[]) characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES);
                    aeModeOff2 = false;
                    if (values != null) {
                        antibandingOff = false;
                        antibanding60Hz = false;
                        antibanding50Hz = false;
                        afContinuous2 = false;
                        afAuto = 0;
                        for (aeModeOn2 = values.length; afAuto < aeModeOn2; aeModeOn2 = aeModeOn2) {
                            int[] values3 = values;
                            values = values[afAuto];
                            antibandingOff |= values == null ? 1 : 0;
                            afContinuous2 |= values == 3 ? 1 : 0;
                            antibanding50Hz |= values == 1 ? 1 : 0;
                            antibanding60Hz |= values == 2 ? 1 : 0;
                            afAuto++;
                            values = values3;
                        }
                        aeModeOff2 = afContinuous2;
                    } else {
                        antibandingOff = false;
                        antibanding60Hz = false;
                        antibanding50Hz = false;
                    }
                    z = antibandingOff && aeModeOff2 && antibanding50Hz && antibanding60Hz;
                    cameraStartupInfo.available_antibanding_mode = z;
                    LogFilter.i(LOG_TAG, String.format(Locale.US, "AVAILABLE AE_MODE(%s), AF_MODE(%s), SM_SPORTS(%s), ANTIBANDING_MODE(%s)", new Object[]{Boolean.toString(cameraStartupInfo.available_ae_mode), Boolean.toString(cameraStartupInfo.available_af_mode), Boolean.toString(cameraStartupInfo.available_scene_mode_sports), Boolean.toString(cameraStartupInfo.available_antibanding_mode)}));
                }
                this.mCameraInfo.setTargetFpsRanges((Range[]) characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES));
                LogFilter.i(LOG_TAG, "CameraCharacteristics ======= ========= ==========");
                LogFilter.i(LOG_TAG, String.format(Locale.US, "INFO_SUPPORTED_HARDWARE_LEVEL=%d", new Object[]{Integer.valueOf(this.mCameraInfo.getHardwareLevel())}));
                LogFilter.i(LOG_TAG, String.format(Locale.US, "SENSOR_ORIENTATION=%d", new Object[]{Integer.valueOf(this.mCameraInfo.getOrientation())}));
                values = (int[]) characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                aeModeOn = false;
                int[] keys;
                if (values != null) {
                    length = values.length;
                    isAvailableYuvReprocessing = false;
                    aeModeOff2 = false;
                    aeModeOn2 = 0;
                    while (aeModeOn2 < length) {
                        int key = values[aeModeOn2];
                        String str = LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        keys = values;
                        stringBuilder.append("REQUEST_AVAILABLE_CAPABILITIES : ");
                        stringBuilder.append(key);
                        LogFilter.i(str, stringBuilder.toString());
                        if (key == 4) {
                            aeModeOff2 = true;
                        }
                        if (key == 7) {
                            isAvailableYuvReprocessing = true;
                        }
                        aeModeOn2++;
                        values = keys;
                    }
                    aeModeOn = aeModeOff2;
                } else {
                    keys = values;
                    isAvailableYuvReprocessing = false;
                }
                CameraInfo cameraInfo = this.mCameraInfo;
                aeModeOff2 = aeModeOn && isAvailableYuvReprocessing;
                cameraInfo.setEnabledZsl(aeModeOff2);
                values = (int[]) characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
                int[] keys2;
                if (values != null) {
                    length = values.length;
                    aeModeOff = false;
                    afAuto = 0;
                    while (afAuto < length) {
                        mode2 = values[afAuto];
                        String str2 = LOG_TAG;
                        cameraCharacteristics = characteristics;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        keys2 = values;
                        stringBuilder2.append("LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION : ");
                        stringBuilder2.append(mode2);
                        LogFilter.i(str2, stringBuilder2.toString());
                        if (mode2 == 1) {
                            aeModeOff = true;
                        }
                        afAuto++;
                        characteristics = cameraCharacteristics;
                        values = keys2;
                    }
                    keys2 = values;
                } else {
                    cameraCharacteristics = characteristics;
                    keys2 = values;
                    aeModeOff = false;
                }
                cameraStartupInfo.available_ois_mode = aeModeOff;
                LogFilter.v(LOG_TAG, "TARGET_FPS_RANGES");
                Iterator it = this.mCameraInfo.getTargetFpsRanges().iterator();
                while (it.hasNext()) {
                    LogFilter.v(LOG_TAG, ((Range) it.next()).toString());
                }
                LogFilter.v(LOG_TAG, String.format(Locale.US, "AE Step=%f (%d/%d), Range(%d, %d)", new Object[]{Double.valueOf(this.mCameraInfo.getAeCompensationStep()), Integer.valueOf(this.mCameraInfo.getAeCompensationNumerator()), Integer.valueOf(this.mCameraInfo.getAeCompensationDenominator()), Integer.valueOf(this.mCameraInfo.getAeCompensationMin()), Integer.valueOf(this.mCameraInfo.getAeCompensationMax())}));
                LogFilter.i(LOG_TAG, "========= ========= ========= ========= ==========");
                if (focalLengths != null) {
                    this.mCameraInfo.setFocalLength(focalLengths[0]);
                }
                if (physicalSize != null) {
                    this.mCameraInfo.setPhysicalSize(physicalSize.getWidth(), physicalSize.getHeight());
                }
                if (pixelArraySize != null) {
                    this.mCameraInfo.setPixelArraySize(pixelArraySize.getWidth(), pixelArraySize.getHeight());
                }
                if (activeRect != null) {
                    this.mCameraInfo.setActiveArraySize(activeRect.left, activeRect.top, activeRect.right, activeRect.bottom);
                }
                cameraManager.openCamera(this.mCameraInfo.getCameraId(), this.mStateCallback, this.mCameraState.backgroundHandler());
                this.mCameraInfo.setOpenState(1);
                return true;
            }
            return false;
        } catch (CameraAccessException | SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void takePicture() {
        try {
            this.mCameraInfo.getCaptureSession().capture(this.mCameraState.getCameraStartupInfo().captureRequest, this.mCaptureCallback, this.mCameraState.backgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePictureBurst() {
        try {
            this.mCameraState.setBurstRemaining(600);
            LogFilter.i(LOG_TAG, "captureBurst");
            this.mCameraInfo.getCaptureSession().captureBurst(this.mCameraState.getCameraStartupInfo().burstRequestList, this.mCaptureCallback, this.mCameraState.backgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePictureZSL() {
        try {
            Builder request = this.mCameraInfo.getOpenCameraDevice().createCaptureRequest(5);
            request.addTarget(this.mPreviewSurface);
            request.addTarget(this.mImageReader.getSurface());
            request.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(false));
            request.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(false));
            request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            this.mCameraInfo.getCaptureSession().capture(request.build(), this.mCaptureCallback, this.mCameraState.backgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void updateCameraState(CameraState newState) {
        if (newState instanceof MorphoPanoramaGP3CameraState) {
            this.mCameraState = (MorphoPanoramaGP3CameraState) newState;
            return;
        }
        LogFilter.w(LOG_TAG, "#updateCameraState, argument is invalid.");
        setDefaultCameraState();
    }

    public void setDefaultCameraState() {
        this.mCameraState = new MorphoPanoramaGP3CameraState();
    }

    public void updateTvValue() {
        Camera2ParamsFragment fragment = this.mCameraState.camera2Params();
        this.mTvTargetSensorSensitivity = fragment.sensorSensitivity();
        this.mTvTargetExposureTime = fragment.exposureTime().longValue();
    }

    public void calculateNewRequest(double gain) {
        TvRequestParams newRequestParams = PreviewState.calculateTvParam(gain, this.mCameraState, false);
        if (newRequestParams.isValid) {
            Float lensApertureSize = (Float) this.mCameraState.getCameraStartupInfo().totalCaptureResult.get(TotalCaptureResult.LENS_APERTURE);
            Camera2ParamsFragment fragment = this.mCameraState.camera2Params();
            this.mTvTargetSensorSensitivity = newRequestParams.sensorSensitivity;
            this.mTvTargetExposureTime = newRequestParams.exposureTimeNs;
            int sensorSensitivity = calculateSensorSensitivity(newRequestParams.sensorSensitivity);
            long exposureTimeNs = calculateExposureTime(newRequestParams.exposureTimeNs);
            fragment.setSensorSensitivity(sensorSensitivity);
            fragment.setExposureTime(exposureTimeNs);
            fragment.setCalculatedEv(PreviewState.calculateEv(lensApertureSize, Integer.valueOf(sensorSensitivity), Long.valueOf(exposureTimeNs)));
        }
    }

    public void calculateNewRequestTvSimple(double gain) {
        TvRequestParams newRequestParams = PreviewState.calculateTvSimpleParam(gain, this.mCameraState, false);
        if (newRequestParams.isValid) {
            Float lensApertureSize = (Float) this.mCameraState.getCameraStartupInfo().totalCaptureResult.get(TotalCaptureResult.LENS_APERTURE);
            Camera2ParamsFragment fragment = this.mCameraState.camera2Params();
            this.mTvTargetSensorSensitivity = newRequestParams.sensorSensitivity;
            this.mTvTargetExposureTime = newRequestParams.exposureTimeNs;
            int sensorSensitivity = calculateSensorSensitivity(newRequestParams.sensorSensitivity);
            long exposureTimeNs = calculateExposureTime(newRequestParams.exposureTimeNs);
            fragment.setSensorSensitivity(sensorSensitivity);
            fragment.setExposureTime(exposureTimeNs);
            fragment.setCalculatedEv(PreviewState.calculateEv(lensApertureSize, Integer.valueOf(sensorSensitivity), Long.valueOf(exposureTimeNs)));
        }
    }

    public void startTakePictureNewRequest(int capture_mode) {
        CameraStartupInfo cameraStartupInfo = this.mCameraState.getCameraStartupInfo();
        Builder currentRequestBuilder = cameraStartupInfo.currentRequestBuilder;
        Builder currentBurstRequestBuilder = cameraStartupInfo.currentBurstRequestBuilder;
        Camera2ParamsFragment fragment = this.mCameraState.camera2Params();
        currentRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(false));
        currentRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(false));
        int sensorSensitivity = fragment.sensorSensitivity();
        Long exposureTime = fragment.exposureTime();
        Long frameDuration = fragment.frameDuration();
        currentRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(sensorSensitivity));
        currentRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
        currentRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);
        if (capture_mode == 3 || capture_mode == 4) {
            currentBurstRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(false));
            currentBurstRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(false));
            currentBurstRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(sensorSensitivity));
            currentBurstRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
            currentBurstRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);
            CaptureRequest req = currentBurstRequestBuilder.build();
            cameraStartupInfo.burstRequestList.clear();
            for (int i = 0; i < 1; i++) {
                cameraStartupInfo.burstRequestList.add(req);
            }
        }
        try {
            Handler backgroundHandler = this.mCameraState.backgroundHandler();
            cameraStartupInfo.captureRequest = currentRequestBuilder.build();
            CameraCaptureSession session = this.mCameraInfo.getCaptureSession();
            synchronized (CameraConstants.CameraSynchronizedObject) {
                switch (capture_mode) {
                    case 1:
                        session.capture(cameraStartupInfo.captureRequest, this.mCaptureCallback, backgroundHandler);
                        break;
                    case 2:
                        session.capture(cameraStartupInfo.captureRequest, this.mCaptureCallback, backgroundHandler);
                        break;
                    case 3:
                        this.mCameraState.setBurstRemaining(0);
                        session.capture(cameraStartupInfo.captureRequest, this.mCaptureCallback, backgroundHandler);
                        break;
                    case 4:
                        session.stopRepeating();
                        session.setRepeatingBurst(cameraStartupInfo.burstRequestList, this.mCaptureCallback, backgroundHandler);
                        break;
                    default:
                        session.setRepeatingRequest(cameraStartupInfo.captureRequest, this.mCaptureCallback, backgroundHandler);
                        break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public int calculateSensorSensitivity() {
        return calculateSensorSensitivity(this.mTvTargetSensorSensitivity);
    }

    private int calculateSensorSensitivity(int src) {
        int sensorSensitivity = src;
        Camera2ParamsFragment fragment = this.mCameraState.camera2Params();
        if (sensorSensitivity - fragment.sensorSensitivity() > 5) {
            return Math.min(fragment.sensorSensitivity() + 5, this.mTvTargetSensorSensitivity);
        }
        if (sensorSensitivity - fragment.sensorSensitivity() < -5) {
            return Math.max(fragment.sensorSensitivity() - 5, this.mTvTargetSensorSensitivity);
        }
        return sensorSensitivity;
    }

    public long calculateExposureTime() {
        return calculateExposureTime(this.mTvTargetExposureTime);
    }

    private long calculateExposureTime(long src) {
        long exposureTime = src;
        Camera2ParamsFragment fragment = this.mCameraState.camera2Params();
        if (exposureTime - fragment.exposureTime().longValue() > CameraConstants.TV_EXPOSURE_TIME_DIFF) {
            return Math.min(fragment.exposureTime().longValue() + CameraConstants.TV_EXPOSURE_TIME_DIFF, this.mTvTargetExposureTime);
        }
        if (exposureTime - fragment.exposureTime().longValue() < -500000) {
            return Math.max(fragment.exposureTime().longValue() - CameraConstants.TV_EXPOSURE_TIME_DIFF, this.mTvTargetExposureTime);
        }
        return exposureTime;
    }

    public boolean isTvValueSame(int sensorSensitivity, long exposureTime) {
        return sensorSensitivity == this.mTvTargetSensorSensitivity && exposureTime == this.mTvTargetExposureTime;
    }

    public int[] getSupportedPreviewSizes() {
        if (!this.mCameraInfo.isCameraEnabled()) {
            return new int[0];
        }
        try {
            StreamConfigurationMap map = (StreamConfigurationMap) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return new int[0];
            }
            List<Size> previewSizes = Arrays.asList(map.getOutputSizes(35));
            Collections.sort(previewSizes, this.mSizeComparator);
            int[] sizes = new int[(previewSizes.size() * 2)];
            int index = 0;
            for (Size size : previewSizes) {
                int index2 = index + 1;
                sizes[index] = size.getWidth();
                index = index2 + 1;
                sizes[index2] = size.getHeight();
            }
            return sizes;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return new int[0];
        }
    }

    private String getCameraModeStringFromKey(CameraMode[] mode, int key) {
        String str = EnvironmentCompat.MEDIA_UNKNOWN;
        for (CameraMode aMode : mode) {
            if (key == aMode.key) {
                return aMode.str;
            }
        }
        return str;
    }

    public String getHardwareLevel() {
        String hardware_level = EnvironmentCompat.MEDIA_UNKNOWN;
        try {
            return getCameraModeStringFromKey(this.HARDWARE_LEVEL, ((Integer) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue());
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return hardware_level;
        }
    }

    public String getTimestampSource() {
        String timestamp_source = EnvironmentCompat.MEDIA_UNKNOWN;
        try {
            return getCameraModeStringFromKey(this.TIMESTAMP_SOURCE, ((Integer) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE)).intValue());
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return timestamp_source;
        }
    }

    public String[] getAvailableColorCorrectionMode() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        return new String[]{"FAST", "HIGH_QUALITY"};
    }

    public String[] getAvailableColorCorrectionModeValues() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        return new String[]{String.valueOf(1), String.valueOf(2)};
    }

    public String[] getColorCorrectionModeDefaultValues() {
        return getImageQualitySettingsDefaultValues(CaptureRequest.COLOR_CORRECTION_MODE);
    }

    public String[] getAvailableEdgeMode() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        String[] val = null;
        try {
            int[] keys = (int[]) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES);
            if (keys != null) {
                ArrayList<String> array = new ArrayList();
                for (int key : keys) {
                    switch (key) {
                        case 0:
                            array.add("OFF");
                            break;
                        case 1:
                            array.add("FAST");
                            break;
                        case 2:
                            array.add("HIGH_QUALITY");
                            break;
                        default:
                            break;
                    }
                }
                val = (String[]) array.toArray(new String[array.size()]);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return val;
    }

    public String[] getAvailableEdgeModeValues() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        String[] val = null;
        try {
            int[] keys = (int[]) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES);
            if (keys != null) {
                ArrayList<String> array = new ArrayList();
                for (int key : keys) {
                    switch (key) {
                        case 0:
                        case 1:
                        case 2:
                            array.add(String.valueOf(key));
                            break;
                        default:
                            break;
                    }
                }
                val = (String[]) array.toArray(new String[array.size()]);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return val;
    }

    public String[] getEdgeModeDefaultValues() {
        return getImageQualitySettingsDefaultValues(CaptureRequest.EDGE_MODE);
    }

    public String[] getAvailableNoiseReductionMode() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        String[] val = null;
        try {
            int[] keys = (int[]) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);
            if (keys != null) {
                ArrayList<String> array = new ArrayList();
                for (int key : keys) {
                    switch (key) {
                        case 0:
                            array.add("OFF");
                            break;
                        case 1:
                            array.add("FAST");
                            break;
                        case 2:
                            array.add("HIGH_QUALITY");
                            break;
                        default:
                            break;
                    }
                }
                val = (String[]) array.toArray(new String[array.size()]);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return val;
    }

    public String[] getAvailableNoiseReductionModeValues() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        String[] val = null;
        try {
            int[] keys = (int[]) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);
            if (keys != null) {
                ArrayList<String> array = new ArrayList();
                for (int key : keys) {
                    switch (key) {
                        case 0:
                        case 1:
                        case 2:
                            array.add(String.valueOf(key));
                            break;
                        default:
                            break;
                    }
                }
                val = (String[]) array.toArray(new String[array.size()]);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return val;
    }

    public String[] getNoiseReductionModeDefaultValues() {
        return getImageQualitySettingsDefaultValues(CaptureRequest.NOISE_REDUCTION_MODE);
    }

    public String[] getAvailableShadingMode() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        return new String[]{"OFF", "FAST"};
    }

    public String[] getAvailableShadingModeValues() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        return new String[]{String.valueOf(0), String.valueOf(1)};
    }

    public String[] getShadingModeDefaultValues() {
        return getImageQualitySettingsDefaultValues(CaptureRequest.SHADING_MODE);
    }

    public String[] getAvailableTonemapMode() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        String[] val = null;
        try {
            int[] keys = (int[]) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES);
            if (keys != null) {
                ArrayList<String> array = new ArrayList();
                for (int key : keys) {
                    switch (key) {
                        case 1:
                            array.add("FAST");
                            break;
                        case 2:
                            array.add("HIGH_QUALITY");
                            break;
                        default:
                            break;
                    }
                }
                val = (String[]) array.toArray(new String[array.size()]);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return val;
    }

    public String[] getAvailableTonemapModeValues() {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        String[] val = null;
        try {
            int[] keys = (int[]) this.mCameraManager.getCameraCharacteristics(this.mCameraInfo.getCameraId()).get(CameraCharacteristics.TONEMAP_AVAILABLE_TONE_MAP_MODES);
            if (keys != null) {
                ArrayList<String> array = new ArrayList();
                for (int key : keys) {
                    switch (key) {
                        case 1:
                        case 2:
                            array.add(String.valueOf(key));
                            break;
                        default:
                            break;
                    }
                }
                val = (String[]) array.toArray(new String[array.size()]);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return val;
    }

    public String[] getTonemapModeDefaultValues() {
        return getImageQualitySettingsDefaultValues(CaptureRequest.TONEMAP_MODE);
    }

    private String[] getImageQualitySettingsDefaultValues(Key<Integer> mode) {
        if (!this.mCameraState.getCameraStartupInfo().available_image_quality_settings) {
            return null;
        }
        String[] val = new String[3];
        try {
            Integer v = (Integer) this.mCameraInfo.getOpenCameraDevice().createCaptureRequest(1).get(mode);
            if (v == null) {
                return null;
            }
            val[0] = String.valueOf(v);
            try {
                v = (Integer) this.mCameraInfo.getOpenCameraDevice().createCaptureRequest(2).get(mode);
                if (v != null) {
                    val[1] = String.valueOf(v);
                } else {
                    val[1] = val[0];
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return null;
            } catch (CameraAccessException e2) {
                e2.printStackTrace();
                return null;
            } catch (Exception e3) {
                val[1] = val[0];
            }
            try {
                v = (Integer) this.mCameraInfo.getOpenCameraDevice().createCaptureRequest(5).get(mode);
                if (v != null) {
                    val[2] = String.valueOf(v);
                } else {
                    val[2] = val[0];
                }
                this.mCameraInfo.setEnabledZsl(true);
            } catch (IllegalStateException e4) {
                e4.printStackTrace();
                return null;
            } catch (CameraAccessException e22) {
                e22.printStackTrace();
                return null;
            } catch (Exception e5) {
                val[2] = val[0];
                this.mCameraInfo.setEnabledZsl(false);
            }
            String mode_str = "";
            if (CaptureRequest.COLOR_CORRECTION_MODE.equals(mode)) {
                mode_str = "COLOR_CORRECTION_MODE";
            } else if (CaptureRequest.EDGE_MODE.equals(mode)) {
                mode_str = "EDGE_MODE";
            } else if (CaptureRequest.NOISE_REDUCTION_MODE.equals(mode)) {
                mode_str = "NOISE_REDUCTION_MODE";
            } else if (CaptureRequest.SHADING_MODE.equals(mode)) {
                mode_str = "SHADING_MODE";
            } else if (CaptureRequest.TONEMAP_MODE.equals(mode)) {
                mode_str = "TONEMAP_MODE";
            }
            LogFilter.i(LOG_TAG, String.format(Locale.US, "getImageQualitySettingsDefaultValues %s[PREVIEW] : %s", new Object[]{mode_str, val[0]}));
            LogFilter.i(LOG_TAG, String.format(Locale.US, "getImageQualitySettingsDefaultValues %s[STILL]   : %s", new Object[]{mode_str, val[1]}));
            LogFilter.i(LOG_TAG, String.format(Locale.US, "getImageQualitySettingsDefaultValues %s[ZSL]     : %s", new Object[]{mode_str, val[2]}));
            return val;
        } catch (IllegalStateException e6) {
            e6.printStackTrace();
            return null;
        } catch (CameraAccessException e7) {
            e7.printStackTrace();
            return null;
        } catch (Exception e8) {
            return null;
        }
    }

    public boolean isOverLevelFull() {
        int level = this.mCameraInfo.getHardwareLevel();
        return (level == 2 || level == 0) ? false : true;
    }

    public boolean isAvailableOis() {
        return this.mCameraState.getCameraStartupInfo().available_ois_mode;
    }
}
