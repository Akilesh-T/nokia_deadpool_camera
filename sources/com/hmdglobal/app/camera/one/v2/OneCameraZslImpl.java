package com.hmdglobal.app.camera.one.v2;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CaptureResult.Key;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CameraProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.util.Pools.SynchronizedPool;
import android.view.Surface;
import com.hmdglobal.app.camera.CaptureModuleUtil;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.exif.ExifTag;
import com.hmdglobal.app.camera.exif.Rational;
import com.hmdglobal.app.camera.one.AbstractOneCamera;
import com.hmdglobal.app.camera.one.OneCamera.CaptureReadyCallback;
import com.hmdglobal.app.camera.one.OneCamera.CloseCallback;
import com.hmdglobal.app.camera.one.OneCamera.PhotoCaptureParameters;
import com.hmdglobal.app.camera.one.OneCamera.PhotoCaptureParameters.Flash;
import com.hmdglobal.app.camera.one.Settings3A;
import com.hmdglobal.app.camera.one.v2.ImageCaptureManager.CaptureReadyListener;
import com.hmdglobal.app.camera.one.v2.ImageCaptureManager.CapturedImageConstraint;
import com.hmdglobal.app.camera.one.v2.ImageCaptureManager.ImageCaptureListener;
import com.hmdglobal.app.camera.one.v2.ImageCaptureManager.MetadataChangeListener;
import com.hmdglobal.app.camera.session.CaptureSession;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.ConjunctionListenerMux;
import com.hmdglobal.app.camera.util.ConjunctionListenerMux.OutputChangeListener;
import com.hmdglobal.app.camera.util.JpegUtilNative;
import com.hmdglobal.app.camera.util.Size;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@TargetApi(21)
public class OneCameraZslImpl extends AbstractOneCamera {
    private static final String FOCUS_RESUME_CALLBACK_TOKEN = "RESUME_CONTINUOUS_AF";
    private static final int JPEG_QUALITY = CameraProfile.getJpegEncodingQualityParameter(2);
    private static final int MAX_CAPTURE_IMAGES = 10;
    private static final Tag TAG = new Tag("OneCameraZslImpl2");
    private static final boolean ZSL_ENABLED = true;
    private static final int sCaptureImageFormat = 35;
    MeteringRectangle[] ZERO_WEIGHT_3A_REGION = AutoFocusHelper.getZeroWeightRegion();
    private MeteringRectangle[] mAERegions = this.ZERO_WEIGHT_3A_REGION;
    private MeteringRectangle[] mAFRegions = this.ZERO_WEIGHT_3A_REGION;
    private final Handler mCameraHandler;
    private final Handler mCameraListenerHandler;
    private final HandlerThread mCameraListenerThread;
    private final HandlerThread mCameraThread;
    private final ImageReader mCaptureImageReader;
    private ImageCaptureManager mCaptureManager;
    private CameraCaptureSession mCaptureSession;
    private final CameraCharacteristics mCharacteristics;
    private CloseCallback mCloseCallback = null;
    private Rect mCropRegion;
    private final CameraDevice mDevice;
    private final float mFullSizeAspectRatio;
    private final ThreadPoolExecutor mImageSaverThreadPool;
    private volatile boolean mIsClosed = false;
    private final SynchronizedPool<ByteBuffer> mJpegByteBufferPool = new SynchronizedPool(64);
    private final AtomicLong mLastCapturedImageTimestamp = new AtomicLong(0);
    private MediaActionSound mMediaActionSound = new MediaActionSound();
    private Surface mPreviewSurface;
    private final ConjunctionListenerMux<ReadyStateRequirement> mReadyStateManager = new ConjunctionListenerMux(ReadyStateRequirement.class, new OutputChangeListener() {
        public void onOutputChange(boolean state) {
            OneCameraZslImpl.this.broadcastReadyState(state);
        }
    });
    private float mZoomValue = 1.0f;

    private enum ReadyStateRequirement {
        CAPTURE_MANAGER_READY,
        CAPTURE_NOT_IN_PROGRESS
    }

    private enum RequestTag {
        EXPLICIT_CAPTURE
    }

    private class ImageCaptureTask implements ImageCaptureListener {
        private final PhotoCaptureParameters mParams;
        private final CaptureSession mSession;

        public ImageCaptureTask(PhotoCaptureParameters parameters, CaptureSession session) {
            this.mParams = parameters;
            this.mSession = session;
        }

        public void onImageCaptured(Image image, TotalCaptureResult captureResult) {
            long timestamp = ((Long) captureResult.get(CaptureResult.SENSOR_TIMESTAMP)).longValue();
            synchronized (OneCameraZslImpl.this.mLastCapturedImageTimestamp) {
                if (timestamp > OneCameraZslImpl.this.mLastCapturedImageTimestamp.get()) {
                    OneCameraZslImpl.this.mLastCapturedImageTimestamp.set(timestamp);
                    OneCameraZslImpl.this.mReadyStateManager.setInput(ReadyStateRequirement.CAPTURE_NOT_IN_PROGRESS, true);
                    this.mSession.startEmpty();
                    OneCameraZslImpl.this.savePicture(image, this.mParams, this.mSession);
                    this.mParams.callback.onPictureTaken(this.mSession);
                    Tag access$400 = OneCameraZslImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Image saved.  Frame number = ");
                    stringBuilder.append(captureResult.getFrameNumber());
                    Log.v(access$400, stringBuilder.toString());
                    return;
                }
            }
        }
    }

    OneCameraZslImpl(CameraDevice device, CameraCharacteristics characteristics, Size pictureSize) {
        Log.v(TAG, "Creating new OneCameraZslImpl");
        this.mDevice = device;
        this.mCharacteristics = characteristics;
        this.mFullSizeAspectRatio = calculateFullSizeAspectRatio(characteristics);
        this.mCameraThread = new HandlerThread("OneCamera2");
        this.mCameraThread.setPriority(10);
        this.mCameraThread.start();
        this.mCameraHandler = new Handler(this.mCameraThread.getLooper());
        this.mCameraListenerThread = new HandlerThread("OneCamera2-Listener");
        this.mCameraListenerThread.start();
        this.mCameraListenerHandler = new Handler(this.mCameraListenerThread.getLooper());
        int numEncodingCores = CameraUtil.getNumCpuCores();
        this.mImageSaverThreadPool = new ThreadPoolExecutor(numEncodingCores, numEncodingCores, 10, TimeUnit.SECONDS, new LinkedBlockingQueue());
        this.mCaptureManager = new ImageCaptureManager(10, this.mCameraListenerHandler, this.mImageSaverThreadPool);
        this.mCaptureManager.setCaptureReadyListener(new CaptureReadyListener() {
            public void onReadyStateChange(boolean capturePossible) {
                OneCameraZslImpl.this.mReadyStateManager.setInput(ReadyStateRequirement.CAPTURE_MANAGER_READY, capturePossible);
            }
        });
        this.mCaptureManager.addMetadataChangeListener(CaptureResult.CONTROL_AF_STATE, new MetadataChangeListener() {
            public void onImageMetadataChange(Key<?> key, Object oldValue, Object newValue, CaptureResult result) {
                OneCameraZslImpl.this.mFocusStateListener.onFocusStatusUpdate(AutoFocusHelper.stateFromCamera2State(((Integer) result.get(CaptureResult.CONTROL_AF_STATE)).intValue()), result.getFrameNumber());
            }
        });
        if (pictureSize == null) {
            pictureSize = getDefaultPictureSize();
        }
        this.mCaptureImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), 35, 10);
        this.mCaptureImageReader.setOnImageAvailableListener(this.mCaptureManager, this.mCameraHandler);
        this.mMediaActionSound.load(0);
    }

    public Size getDefaultPictureSize() {
        android.util.Size[] supportedSizes = ((StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(35);
        int i = 0;
        android.util.Size largestSupportedSize = supportedSizes[0];
        long largestSupportedSizePixels = (long) (largestSupportedSize.getWidth() * largestSupportedSize.getHeight());
        while (i < supportedSizes.length) {
            long numPixels = (long) (supportedSizes[i].getWidth() * supportedSizes[i].getHeight());
            if (numPixels > largestSupportedSizePixels) {
                largestSupportedSize = supportedSizes[i];
                largestSupportedSizePixels = numPixels;
            }
            i++;
        }
        return new Size(largestSupportedSize.getWidth(), largestSupportedSize.getHeight());
    }

    private void onShutterInvokeUI(PhotoCaptureParameters params) {
        params.callback.onQuickExpose();
        this.mMediaActionSound.play(0);
    }

    public void takePicture(final PhotoCaptureParameters params, CaptureSession session) {
        params.checkSanity();
        this.mReadyStateManager.setInput(ReadyStateRequirement.CAPTURE_NOT_IN_PROGRESS, false);
        ArrayList<CapturedImageConstraint> zslConstraints = new ArrayList();
        zslConstraints.add(new CapturedImageConstraint() {
            /* JADX WARNING: Missing block: B:30:0x00a4, code skipped:
            return false;
     */
            public boolean satisfiesConstraint(android.hardware.camera2.TotalCaptureResult r13) {
                /*
                r12 = this;
                r0 = android.hardware.camera2.CaptureResult.SENSOR_TIMESTAMP;
                r0 = r13.get(r0);
                r0 = (java.lang.Long) r0;
                r1 = android.hardware.camera2.CaptureResult.LENS_STATE;
                r1 = r13.get(r1);
                r1 = (java.lang.Integer) r1;
                r2 = android.hardware.camera2.CaptureResult.FLASH_STATE;
                r2 = r13.get(r2);
                r2 = (java.lang.Integer) r2;
                r3 = android.hardware.camera2.CaptureResult.FLASH_MODE;
                r3 = r13.get(r3);
                r3 = (java.lang.Integer) r3;
                r4 = android.hardware.camera2.CaptureResult.CONTROL_AE_STATE;
                r4 = r13.get(r4);
                r4 = (java.lang.Integer) r4;
                r5 = android.hardware.camera2.CaptureResult.CONTROL_AF_STATE;
                r5 = r13.get(r5);
                r5 = (java.lang.Integer) r5;
                r6 = android.hardware.camera2.CaptureResult.CONTROL_AWB_STATE;
                r6 = r13.get(r6);
                r6 = (java.lang.Integer) r6;
                r7 = r0.longValue();
                r9 = com.hmdglobal.app.camera.one.v2.OneCameraZslImpl.this;
                r9 = r9.mLastCapturedImageTimestamp;
                r9 = r9.get();
                r7 = (r7 > r9 ? 1 : (r7 == r9 ? 0 : -1));
                r8 = 0;
                if (r7 > 0) goto L_0x004c;
            L_0x004b:
                return r8;
            L_0x004c:
                r7 = r1.intValue();
                r9 = 1;
                if (r7 != r9) goto L_0x0054;
            L_0x0053:
                return r8;
            L_0x0054:
                r7 = r4.intValue();
                if (r7 == r9) goto L_0x00a5;
            L_0x005a:
                r7 = r4.intValue();
                r10 = 5;
                if (r7 != r10) goto L_0x0062;
            L_0x0061:
                goto L_0x00a5;
            L_0x0062:
                r7 = com.hmdglobal.app.camera.one.v2.OneCameraZslImpl.AnonymousClass11.$SwitchMap$com$hmdglobal$app$camera$one$OneCamera$PhotoCaptureParameters$Flash;
                r10 = r8;
                r10 = r10.flashMode;
                r10 = r10.ordinal();
                r7 = r7[r10];
                r10 = 3;
                switch(r7) {
                    case 1: goto L_0x008e;
                    case 2: goto L_0x0081;
                    case 3: goto L_0x0073;
                    default: goto L_0x0072;
                };
            L_0x0072:
                goto L_0x008f;
            L_0x0073:
                r7 = r4.intValue();
                r11 = 4;
                if (r7 != r11) goto L_0x008f;
            L_0x007a:
                r7 = r2.intValue();
                if (r7 == r10) goto L_0x008f;
            L_0x0080:
                return r8;
            L_0x0081:
                r7 = r2.intValue();
                if (r7 != r10) goto L_0x008d;
            L_0x0087:
                r7 = r3.intValue();
                if (r7 == r9) goto L_0x008f;
            L_0x008d:
                return r8;
            L_0x008f:
                r7 = r5.intValue();
                if (r7 == r10) goto L_0x00a4;
            L_0x0095:
                r7 = r5.intValue();
                if (r7 != r9) goto L_0x009c;
            L_0x009b:
                goto L_0x00a4;
            L_0x009c:
                r7 = r6.intValue();
                if (r7 != r9) goto L_0x00a3;
            L_0x00a2:
                return r8;
            L_0x00a3:
                return r9;
            L_0x00a4:
                return r8;
            L_0x00a5:
                return r8;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.one.v2.OneCameraZslImpl$AnonymousClass4.satisfiesConstraint(android.hardware.camera2.TotalCaptureResult):boolean");
            }
        });
        ArrayList<CapturedImageConstraint> singleCaptureConstraint = new ArrayList();
        singleCaptureConstraint.add(new CapturedImageConstraint() {
            public boolean satisfiesConstraint(TotalCaptureResult captureResult) {
                return captureResult.getRequest().getTag() == RequestTag.EXPLICIT_CAPTURE;
            }
        });
        if (!true) {
            throw new UnsupportedOperationException("Non-ZSL capture not yet supported");
        } else if (this.mCaptureManager.tryCaptureExistingImage(new ImageCaptureTask(params, session), zslConstraints)) {
            Log.v(TAG, "Saving previous frame");
            onShutterInvokeUI(params);
        } else {
            Log.v(TAG, "No good image Available.  Capturing next available good image.");
            if (params.flashMode == Flash.ON || params.flashMode == Flash.AUTO) {
                this.mCaptureManager.captureNextImage(new ImageCaptureTask(params, session), singleCaptureConstraint);
                this.mCaptureManager.addMetadataChangeListener(CaptureResult.CONTROL_AE_STATE, new MetadataChangeListener() {
                    public void onImageMetadataChange(Key<?> key, Object oldValue, Object newValue, CaptureResult result) {
                        Log.v(OneCameraZslImpl.TAG, "AE State Changed");
                        if (oldValue.equals(Integer.valueOf(5))) {
                            OneCameraZslImpl.this.mCaptureManager.removeMetadataChangeListener(key, this);
                            OneCameraZslImpl.this.sendSingleRequest(params);
                            OneCameraZslImpl.this.onShutterInvokeUI(params);
                        }
                    }
                });
                sendAutoExposureTriggerRequest(params.flashMode);
                return;
            }
            this.mCaptureManager.captureNextImage(new ImageCaptureTask(params, session), zslConstraints);
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
        this.mCaptureManager.close();
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

    private void savePicture(Image image, final PhotoCaptureParameters captureParams, CaptureSession session) {
        int heading = captureParams.heading;
        int width = image.getWidth();
        int height = image.getHeight();
        ExifInterface exif = new ExifInterface();
        exif.setTag(exif.buildTag(ExifInterface.TAG_PIXEL_X_DIMENSION, Integer.valueOf(width)));
        exif.setTag(exif.buildTag(ExifInterface.TAG_PIXEL_Y_DIMENSION, Integer.valueOf(height)));
        if (heading >= 0) {
            ExifTag directionRefTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
            ExifTag directionTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational((long) heading, 1));
            exif.setTag(directionRefTag);
            exif.setTag(directionTag);
        }
        try {
            session.saveAndFinish(acquireJpegBytes(image), width, height, 0, exif, new OnMediaSavedListener() {
                public void onMediaSaved(Uri uri) {
                    captureParams.callback.onPictureSaved(uri);
                }
            });
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void setupAsync(final Surface previewSurface, final CaptureReadyCallback listener) {
        this.mCameraHandler.post(new Runnable() {
            public void run() {
                OneCameraZslImpl.this.setup(previewSurface, listener);
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
                    OneCameraZslImpl.this.mCaptureSession = session;
                    OneCameraZslImpl.this.mAFRegions = OneCameraZslImpl.this.ZERO_WEIGHT_3A_REGION;
                    OneCameraZslImpl.this.mAERegions = OneCameraZslImpl.this.ZERO_WEIGHT_3A_REGION;
                    OneCameraZslImpl.this.mZoomValue = 1.0f;
                    OneCameraZslImpl.this.mCropRegion = OneCameraZslImpl.this.cropRegionForZoom(OneCameraZslImpl.this.mZoomValue);
                    if (OneCameraZslImpl.this.sendRepeatingCaptureRequest()) {
                        OneCameraZslImpl.this.mReadyStateManager.setInput(ReadyStateRequirement.CAPTURE_NOT_IN_PROGRESS, true);
                        OneCameraZslImpl.this.mReadyStateManager.notifyListeners();
                        listener.onReadyForCapture();
                        return;
                    }
                    listener.onSetupFailed();
                }

                public void onClosed(CameraCaptureSession session) {
                    super.onClosed(session);
                    if (OneCameraZslImpl.this.mCloseCallback != null) {
                        OneCameraZslImpl.this.mCloseCallback.onCameraClosed();
                    }
                }
            }, this.mCameraHandler);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not set up capture session", ex);
            listener.onSetupFailed();
        }
    }

    private void addRegionsToCaptureRequestBuilder(Builder builder) {
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, this.mAERegions);
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, this.mAFRegions);
        builder.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRegion);
    }

    private void addFlashToCaptureRequestBuilder(Builder builder, Flash flashMode) {
        switch (flashMode) {
            case OFF:
                builder.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(1));
                builder.set(CaptureRequest.FLASH_MODE, Integer.valueOf(0));
                return;
            case ON:
                builder.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(3));
                builder.set(CaptureRequest.FLASH_MODE, Integer.valueOf(1));
                return;
            case AUTO:
                builder.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(2));
                return;
            default:
                return;
        }
    }

    private boolean sendRepeatingCaptureRequest() {
        Log.v(TAG, "sendRepeatingCaptureRequest()");
        try {
            Builder builder = this.mDevice.createCaptureRequest(5);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(4));
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
            builder.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.FLASH_MODE, Integer.valueOf(0));
            addRegionsToCaptureRequestBuilder(builder);
            this.mCaptureSession.setRepeatingRequest(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return true;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute zero-shutter-lag repeating request.", e);
            return false;
        }
    }

    private boolean sendSingleRequest(PhotoCaptureParameters params) {
        Log.v(TAG, "sendSingleRequest()");
        try {
            Builder builder = this.mDevice.createCaptureRequest(2);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            addFlashToCaptureRequestBuilder(builder, params.flashMode);
            addRegionsToCaptureRequestBuilder(builder);
            builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
            builder.setTag(RequestTag.EXPLICIT_CAPTURE);
            this.mCaptureSession.capture(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return true;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute single still capture request.", e);
            return false;
        }
    }

    private boolean sendAutoExposureTriggerRequest(Flash flashMode) {
        Log.v(TAG, "sendAutoExposureTriggerRequest()");
        try {
            Builder builder = this.mDevice.createCaptureRequest(5);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, Integer.valueOf(1));
            addRegionsToCaptureRequestBuilder(builder);
            addFlashToCaptureRequestBuilder(builder, flashMode);
            this.mCaptureSession.capture(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return true;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute auto exposure trigger request.", e);
            return false;
        }
    }

    private boolean sendAutoFocusTriggerRequest() {
        Log.v(TAG, "sendAutoFocusTriggerRequest()");
        try {
            Builder builder = this.mDevice.createCaptureRequest(5);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            addRegionsToCaptureRequestBuilder(builder);
            builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(1));
            this.mCaptureSession.capture(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return true;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute auto focus trigger request.", e);
            return false;
        }
    }

    private boolean sendAutoFocusHoldRequest() {
        Log.v(TAG, "sendAutoFocusHoldRequest()");
        try {
            Builder builder = this.mDevice.createCaptureRequest(5);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
            addRegionsToCaptureRequestBuilder(builder);
            this.mCaptureSession.setRepeatingRequest(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return true;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute auto focus hold request.", e);
            return false;
        }
    }

    private static float calculateFullSizeAspectRatio(CameraCharacteristics characteristics) {
        Rect activeArraySize = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        return ((float) activeArraySize.width()) / ((float) activeArraySize.height());
    }

    private byte[] acquireJpegBytes(Image img) {
        byte[] imageBytes;
        if (img.getFormat() == 256) {
            ByteBuffer buffer = img.getPlanes()[0].getBuffer();
            imageBytes = new byte[buffer.remaining()];
            buffer.get(imageBytes);
            buffer.rewind();
            return imageBytes;
        } else if (img.getFormat() == 35) {
            ByteBuffer buffer2 = (ByteBuffer) this.mJpegByteBufferPool.acquire();
            if (buffer2 == null) {
                buffer2 = ByteBuffer.allocateDirect((img.getWidth() * img.getHeight()) * 3);
            }
            int numBytes = JpegUtilNative.compressJpegFromYUV420Image(img, buffer2, JPEG_QUALITY);
            if (numBytes >= 0) {
                buffer2.limit(numBytes);
                imageBytes = new byte[buffer2.remaining()];
                buffer2.get(imageBytes);
                buffer2.clear();
                this.mJpegByteBufferPool.release(buffer2);
                return imageBytes;
            }
            throw new RuntimeException("Error compressing jpeg.");
        } else {
            throw new RuntimeException("Unsupported image format.");
        }
    }

    private void startAFCycle() {
        this.mCameraHandler.removeCallbacksAndMessages(FOCUS_RESUME_CALLBACK_TOKEN);
        sendAutoFocusTriggerRequest();
        sendAutoFocusHoldRequest();
        this.mCameraHandler.postAtTime(new Runnable() {
            public void run() {
                OneCameraZslImpl.this.mAERegions = OneCameraZslImpl.this.ZERO_WEIGHT_3A_REGION;
                OneCameraZslImpl.this.mAFRegions = OneCameraZslImpl.this.ZERO_WEIGHT_3A_REGION;
                OneCameraZslImpl.this.sendRepeatingCaptureRequest();
            }
        }, FOCUS_RESUME_CALLBACK_TOKEN, SystemClock.uptimeMillis() + ((long) Settings3A.getFocusHoldMillis()));
    }

    public void triggerFocusAndMeterAtPoint(float nx, float ny) {
        int sensorOrientation = ((Integer) this.mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        this.mAERegions = AutoFocusHelper.aeRegionsForNormalizedCoord(nx, ny, this.mCropRegion, sensorOrientation);
        this.mAFRegions = AutoFocusHelper.afRegionsForNormalizedCoord(nx, ny, this.mCropRegion, sensorOrientation);
        startAFCycle();
    }

    public Size pickPreviewSize(Size pictureSize, Context context) {
        if (pictureSize == null) {
            pictureSize = getDefaultPictureSize();
        }
        return CaptureModuleUtil.getOptimalPreviewSize(context, getSupportedSizes(), (double) (((float) pictureSize.getWidth()) / ((float) pictureSize.getHeight())));
    }

    public float getMaxZoom() {
        return ((Float) this.mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)).floatValue();
    }

    public void setZoom(float zoom) {
        this.mZoomValue = zoom;
        this.mCropRegion = cropRegionForZoom(zoom);
        sendRepeatingCaptureRequest();
    }

    private Rect cropRegionForZoom(float zoom) {
        return AutoFocusHelper.cropRegionForZoom(this.mCharacteristics, zoom);
    }
}
