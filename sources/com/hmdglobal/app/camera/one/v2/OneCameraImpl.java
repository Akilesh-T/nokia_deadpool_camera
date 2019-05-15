package com.hmdglobal.app.camera.one.v2;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Surface;
import com.hmdglobal.app.camera.CaptureModuleUtil;
import com.hmdglobal.app.camera.Exif;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.debug.DebugPropertyHelper;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.exif.ExifTag;
import com.hmdglobal.app.camera.exif.Rational;
import com.hmdglobal.app.camera.one.AbstractOneCamera;
import com.hmdglobal.app.camera.one.OneCamera.AutoFocusState;
import com.hmdglobal.app.camera.one.OneCamera.CaptureReadyCallback;
import com.hmdglobal.app.camera.one.OneCamera.CloseCallback;
import com.hmdglobal.app.camera.one.OneCamera.PhotoCaptureParameters;
import com.hmdglobal.app.camera.one.OneCamera.PictureCallback;
import com.hmdglobal.app.camera.one.Settings3A;
import com.hmdglobal.app.camera.session.CaptureSession;
import com.hmdglobal.app.camera.util.CaptureDataSerializer;
import com.hmdglobal.app.camera.util.JpegUtilNative;
import com.hmdglobal.app.camera.util.Size;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OneCameraImpl extends AbstractOneCamera {
    private static final boolean DEBUG_FOCUS_LOG = DebugPropertyHelper.showFrameDebugLog();
    private static final boolean DEBUG_WRITE_CAPTURE_DATA = DebugPropertyHelper.writeCaptureData();
    private static final int FOCUS_HOLD_MILLIS = Settings3A.getFocusHoldMillis();
    private static final Byte JPEG_QUALITY = Byte.valueOf((byte) 90);
    private static final Tag TAG = new Tag("OneCameraImpl2");
    private static final int sCaptureImageFormat = 35;
    MeteringRectangle[] ZERO_WEIGHT_3A_REGION = AutoFocusHelper.getZeroWeightRegion();
    private MeteringRectangle[] mAERegions = this.ZERO_WEIGHT_3A_REGION;
    private MeteringRectangle[] mAFRegions = this.ZERO_WEIGHT_3A_REGION;
    private final CaptureCallback mAutoFocusStateListener = new CaptureCallback() {
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            if (request.getTag() == RequestTag.CAPTURE && OneCameraImpl.this.mLastPictureCallback != null) {
                OneCameraImpl.this.mLastPictureCallback.onQuickExpose();
            }
        }

        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            OneCameraImpl.this.autofocusStateChangeDispatcher(partialResult);
            super.onCaptureProgressed(session, request, partialResult);
        }

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            OneCameraImpl.this.autofocusStateChangeDispatcher(result);
            if (result.get(CaptureResult.CONTROL_AF_STATE) == null) {
                AutoFocusHelper.checkControlAfState(result);
            }
            if (OneCameraImpl.DEBUG_FOCUS_LOG) {
                AutoFocusHelper.logExtraFocusInfo(result);
            }
            super.onCaptureCompleted(session, request, result);
        }
    };
    private final Handler mCameraHandler;
    private final HandlerThread mCameraThread;
    OnImageAvailableListener mCaptureImageListener = new OnImageAvailableListener() {
        public void onImageAvailable(ImageReader reader) {
            InFlightCapture capture = (InFlightCapture) OneCameraImpl.this.mCaptureQueue.remove();
            capture.session.startEmpty();
            try {
                OneCameraImpl.this.savePicture(OneCameraImpl.acquireJpegBytesAndClose(reader), capture.parameters, capture.session);
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
            OneCameraImpl.this.broadcastReadyState(true);
            capture.parameters.callback.onPictureTaken(capture.session);
        }
    };
    private final ImageReader mCaptureImageReader;
    private final LinkedList<InFlightCapture> mCaptureQueue = new LinkedList();
    private CameraCaptureSession mCaptureSession;
    private final CameraCharacteristics mCharacteristics;
    private CloseCallback mCloseCallback = null;
    private int mControlAFMode = 4;
    private Rect mCropRegion;
    private final CameraDevice mDevice;
    private final float mFullSizeAspectRatio;
    private volatile boolean mIsClosed = false;
    private long mLastControlAfStateFrameNumber = 0;
    private PictureCallback mLastPictureCallback = null;
    private AutoFocusState mLastResultAFState = AutoFocusState.INACTIVE;
    private Surface mPreviewSurface;
    private final Runnable mReturnToContinuousAFRunnable = new Runnable() {
        public void run() {
            OneCameraImpl.this.mAFRegions = OneCameraImpl.this.ZERO_WEIGHT_3A_REGION;
            OneCameraImpl.this.mAERegions = OneCameraImpl.this.ZERO_WEIGHT_3A_REGION;
            OneCameraImpl.this.mControlAFMode = 4;
            OneCameraImpl.this.repeatingPreview(null);
        }
    };
    private Runnable mTakePictureRunnable;
    private long mTakePictureStartMillis;
    private boolean mTakePictureWhenLensIsStopped = false;
    private float mZoomValue = 1.0f;

    private static class InFlightCapture {
        final PhotoCaptureParameters parameters;
        final CaptureSession session;

        public InFlightCapture(PhotoCaptureParameters parameters, CaptureSession session) {
            this.parameters = parameters;
            this.session = session;
        }
    }

    public enum RequestTag {
        PRESHOT_TRIGGERED_AF,
        CAPTURE,
        TAP_TO_FOCUS
    }

    OneCameraImpl(CameraDevice device, CameraCharacteristics characteristics, Size pictureSize) {
        this.mDevice = device;
        this.mCharacteristics = characteristics;
        this.mFullSizeAspectRatio = calculateFullSizeAspectRatio(characteristics);
        this.mCameraThread = new HandlerThread("OneCamera2");
        this.mCameraThread.start();
        this.mCameraHandler = new Handler(this.mCameraThread.getLooper());
        this.mCaptureImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), 35, 2);
        this.mCaptureImageReader.setOnImageAvailableListener(this.mCaptureImageListener, this.mCameraHandler);
        Log.d(TAG, "New Camera2 based OneCameraImpl created.");
    }

    public void takePicture(final PhotoCaptureParameters params, final CaptureSession session) {
        if (!this.mTakePictureWhenLensIsStopped) {
            broadcastReadyState(false);
            this.mTakePictureRunnable = new Runnable() {
                public void run() {
                    OneCameraImpl.this.takePictureNow(params, session);
                }
            };
            this.mLastPictureCallback = params.callback;
            this.mTakePictureStartMillis = SystemClock.uptimeMillis();
            if (this.mLastResultAFState == AutoFocusState.ACTIVE_SCAN) {
                Log.v(TAG, "Waiting until scan is done before taking shot.");
                this.mTakePictureWhenLensIsStopped = true;
            } else {
                takePictureNow(params, session);
            }
        }
    }

    public void takePictureNow(PhotoCaptureParameters params, CaptureSession session) {
        long dt = SystemClock.uptimeMillis() - this.mTakePictureStartMillis;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Taking shot with extra AF delay of ");
        stringBuilder.append(dt);
        stringBuilder.append(" ms.");
        Log.v(tag, stringBuilder.toString());
        params.checkSanity();
        try {
            Builder builder = this.mDevice.createCaptureRequest(2);
            builder.setTag(RequestTag.CAPTURE);
            addBaselineCaptureKeysToRequest(builder);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            CaptureRequest request = builder.build();
            if (DEBUG_WRITE_CAPTURE_DATA) {
                String debugDataDir = AbstractOneCamera.makeDebugDir(params.debugDataFolder, "normal_capture_debug");
                Tag tag2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Writing capture data to: ");
                stringBuilder2.append(debugDataDir);
                Log.i(tag2, stringBuilder2.toString());
                CaptureDataSerializer.toFile("Normal Capture", request, new File(debugDataDir, "capture.txt"));
            }
            this.mCaptureSession.capture(request, this.mAutoFocusStateListener, this.mCameraHandler);
            this.mCaptureQueue.add(new InFlightCapture(params, session));
        } catch (CameraAccessException e) {
            Log.e(TAG, "Could not access camera for still image capture.");
            broadcastReadyState(true);
            params.callback.onPictureTakenFailed();
        }
    }

    public void startPreview(Surface previewSurface, CaptureReadyCallback listener) {
        this.mPreviewSurface = previewSurface;
        setupAsync(this.mPreviewSurface, listener);
    }

    public void setViewfinderSize(int width, int height) {
        throw new RuntimeException("Not implemented yet.");
    }

    public boolean isFlashSupported(boolean enhanced) {
        throw new RuntimeException("Not implemented yet.");
    }

    public boolean isSupportingEnhancedMode() {
        throw new RuntimeException("Not implemented yet.");
    }

    public void close(CloseCallback closeCallback) {
        if (this.mIsClosed) {
            Log.w(TAG, "Camera is already closed.");
            return;
        }
        try {
            this.mCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            Log.e(TAG, "Could not abort captures in progress.");
        }
        this.mIsClosed = true;
        this.mCloseCallback = closeCallback;
        this.mCameraThread.quitSafely();
        this.mDevice.close();
    }

    public Size[] getSupportedSizes() {
        return Size.convert(((StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(35));
    }

    public float getFullSizeAspectRatio() {
        return this.mFullSizeAspectRatio;
    }

    public boolean isFrontFacing() {
        return ((Integer) this.mCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0;
    }

    public boolean isBackFacing() {
        return ((Integer) this.mCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 1;
    }

    private void savePicture(byte[] jpegData, PhotoCaptureParameters captureParams, CaptureSession session) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        byte[] bArr;
        int width;
        IOException e;
        int i;
        final PhotoCaptureParameters photoCaptureParameters = captureParams;
        int heading = photoCaptureParameters.heading;
        int height = 0;
        int rotation = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface();
            bArr = jpegData;
            try {
                exif.readExif(bArr);
                Integer w = exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                int width2 = w == null ? 0 : w.intValue();
                try {
                    Integer h = exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                    height = h == null ? 0 : h.intValue();
                    rotation = Exif.getOrientation(exif);
                    if (heading >= 0) {
                        ExifTag directionRefTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
                        width = width2;
                        try {
                            heading = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational((long) heading, 1));
                            exif.setTag(directionRefTag);
                            exif.setTag(heading);
                        } catch (IOException e2) {
                            e = e2;
                        }
                    } else {
                        width = width2;
                    }
                } catch (IOException e3) {
                    e = e3;
                    i = heading;
                    width = width2;
                    Log.w(TAG, "Could not read exif from gcam jpeg", e);
                    exif = null;
                    session.saveAndFinish(bArr, width, height, rotation, exif, new OnMediaSavedListener() {
                        public void onMediaSaved(Uri uri) {
                            photoCaptureParameters.callback.onPictureSaved(uri);
                        }
                    });
                }
            } catch (IOException e4) {
                e = e4;
                i = heading;
                width = 0;
                Log.w(TAG, "Could not read exif from gcam jpeg", e);
                exif = null;
                session.saveAndFinish(bArr, width, height, rotation, exif, /* anonymous class already generated */);
            }
        } catch (IOException e5) {
            e = e5;
            bArr = jpegData;
            i = heading;
            width = 0;
            Log.w(TAG, "Could not read exif from gcam jpeg", e);
            exif = null;
            session.saveAndFinish(bArr, width, height, rotation, exif, /* anonymous class already generated */);
        }
        session.saveAndFinish(bArr, width, height, rotation, exif, /* anonymous class already generated */);
    }

    private void setupAsync(final Surface previewSurface, final CaptureReadyCallback listener) {
        this.mCameraHandler.post(new Runnable() {
            public void run() {
                OneCameraImpl.this.setup(previewSurface, listener);
            }
        });
    }

    private void setup(Surface previewSurface, final CaptureReadyCallback listener) {
        try {
            if (this.mCaptureSession != null) {
                this.mCaptureSession.abortCaptures();
                this.mCaptureSession = null;
            }
            List<Surface> outputSurfaces = new ArrayList(2);
            outputSurfaces.add(previewSurface);
            outputSurfaces.add(this.mCaptureImageReader.getSurface());
            this.mDevice.createCaptureSession(outputSurfaces, new StateCallback() {
                public void onConfigureFailed(CameraCaptureSession session) {
                    listener.onSetupFailed();
                }

                public void onConfigured(CameraCaptureSession session) {
                    OneCameraImpl.this.mCaptureSession = session;
                    OneCameraImpl.this.mAFRegions = OneCameraImpl.this.ZERO_WEIGHT_3A_REGION;
                    OneCameraImpl.this.mAERegions = OneCameraImpl.this.ZERO_WEIGHT_3A_REGION;
                    OneCameraImpl.this.mZoomValue = 1.0f;
                    OneCameraImpl.this.mCropRegion = OneCameraImpl.this.cropRegionForZoom(OneCameraImpl.this.mZoomValue);
                    if (OneCameraImpl.this.repeatingPreview(null)) {
                        listener.onReadyForCapture();
                    } else {
                        listener.onSetupFailed();
                    }
                }

                public void onClosed(CameraCaptureSession session) {
                    super.onClosed(session);
                    if (OneCameraImpl.this.mCloseCallback != null) {
                        OneCameraImpl.this.mCloseCallback.onCameraClosed();
                    }
                }
            }, this.mCameraHandler);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not set up capture session", ex);
            listener.onSetupFailed();
        }
    }

    private void addBaselineCaptureKeysToRequest(Builder builder) {
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, this.mAFRegions);
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, this.mAERegions);
        builder.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRegion);
        builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mControlAFMode));
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
        builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, Integer.valueOf(2));
        builder.set(CaptureRequest.CONTROL_SCENE_MODE, Integer.valueOf(1));
    }

    private boolean repeatingPreview(Object tag) {
        try {
            Builder builder = this.mDevice.createCaptureRequest(1);
            builder.addTarget(this.mPreviewSurface);
            builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            addBaselineCaptureKeysToRequest(builder);
            this.mCaptureSession.setRepeatingRequest(builder.build(), this.mAutoFocusStateListener, this.mCameraHandler);
            Log.v(TAG, String.format("Sent repeating Preview request, zoom = %.2f", new Object[]{Float.valueOf(this.mZoomValue)}));
            return true;
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not access camera setting up preview.", ex);
            return false;
        }
    }

    private void sendAutoFocusTriggerCaptureRequest(Object tag) {
        try {
            Builder builder = this.mDevice.createCaptureRequest(1);
            builder.addTarget(this.mPreviewSurface);
            builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            this.mControlAFMode = 1;
            addBaselineCaptureKeysToRequest(builder);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(1));
            builder.setTag(tag);
            this.mCaptureSession.capture(builder.build(), this.mAutoFocusStateListener, this.mCameraHandler);
            repeatingPreview(tag);
            resumeContinuousAFAfterDelay(FOCUS_HOLD_MILLIS);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not execute preview request.", ex);
        }
    }

    private void resumeContinuousAFAfterDelay(int millis) {
        this.mCameraHandler.removeCallbacks(this.mReturnToContinuousAFRunnable);
        this.mCameraHandler.postDelayed(this.mReturnToContinuousAFRunnable, (long) millis);
    }

    private void autofocusStateChangeDispatcher(CaptureResult result) {
        if (result.getFrameNumber() >= this.mLastControlAfStateFrameNumber && result.get(CaptureResult.CONTROL_AF_STATE) != null) {
            this.mLastControlAfStateFrameNumber = result.getFrameNumber();
            AutoFocusState resultAFState = AutoFocusHelper.stateFromCamera2State(((Integer) result.get(CaptureResult.CONTROL_AF_STATE)).intValue());
            boolean lensIsStopped = resultAFState == AutoFocusState.ACTIVE_FOCUSED || resultAFState == AutoFocusState.ACTIVE_UNFOCUSED || resultAFState == AutoFocusState.PASSIVE_FOCUSED || resultAFState == AutoFocusState.PASSIVE_UNFOCUSED;
            if (this.mTakePictureWhenLensIsStopped && lensIsStopped) {
                this.mCameraHandler.post(this.mTakePictureRunnable);
                this.mTakePictureWhenLensIsStopped = false;
            }
            if (!(resultAFState == this.mLastResultAFState || this.mFocusStateListener == null)) {
                this.mFocusStateListener.onFocusStatusUpdate(resultAFState, result.getFrameNumber());
            }
            this.mLastResultAFState = resultAFState;
        }
    }

    public void triggerFocusAndMeterAtPoint(float nx, float ny) {
        int sensorOrientation = ((Integer) this.mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        this.mAERegions = AutoFocusHelper.aeRegionsForNormalizedCoord(nx, ny, this.mCropRegion, sensorOrientation);
        this.mAFRegions = AutoFocusHelper.afRegionsForNormalizedCoord(nx, ny, this.mCropRegion, sensorOrientation);
        sendAutoFocusTriggerCaptureRequest(RequestTag.TAP_TO_FOCUS);
    }

    public float getMaxZoom() {
        return ((Float) this.mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)).floatValue();
    }

    public void setZoom(float zoom) {
        this.mZoomValue = zoom;
        this.mCropRegion = cropRegionForZoom(zoom);
        repeatingPreview(null);
    }

    public Size pickPreviewSize(Size pictureSize, Context context) {
        return CaptureModuleUtil.getOptimalPreviewSize(context, getSupportedSizes(), (double) (((float) pictureSize.getWidth()) / ((float) pictureSize.getHeight())));
    }

    private Rect cropRegionForZoom(float zoom) {
        return AutoFocusHelper.cropRegionForZoom(this.mCharacteristics, zoom);
    }

    private static float calculateFullSizeAspectRatio(CameraCharacteristics characteristics) {
        Rect activeArraySize = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        return ((float) activeArraySize.width()) / ((float) activeArraySize.height());
    }

    private static byte[] acquireJpegBytesAndClose(ImageReader reader) {
        ByteBuffer buffer;
        int numBytes;
        Image img = reader.acquireLatestImage();
        if (img.getFormat() == 256) {
            buffer = img.getPlanes()[0].getBuffer();
        } else if (img.getFormat() == 35) {
            buffer = ByteBuffer.allocateDirect((img.getWidth() * img.getHeight()) * 3);
            Log.v(TAG, "Compressing JPEG with software encoder.");
            numBytes = JpegUtilNative.compressJpegFromYUV420Image(img, buffer, JPEG_QUALITY.byteValue());
            if (numBytes >= 0) {
                buffer.limit(numBytes);
            } else {
                throw new RuntimeException("Error compressing jpeg.");
            }
        } else {
            throw new RuntimeException("Unsupported image format.");
        }
        numBytes = new byte[buffer.remaining()];
        buffer.get(numBytes);
        buffer.rewind();
        img.close();
        return numBytes;
    }
}
