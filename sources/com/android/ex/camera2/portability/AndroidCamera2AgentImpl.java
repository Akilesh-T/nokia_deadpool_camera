package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureRequest.Key;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaActionSound;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import com.android.ex.camera2.portability.CameraAgent.CameraAFCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraAFMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraFaceDetectionCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraFinalPreviewCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraGDCallBack;
import com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPanoramaCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPanoramaMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewDataCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewResultCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraShutterCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback;
import com.android.ex.camera2.portability.CameraAgent.CaptureCompleteCallBack;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.ex.camera2.utils.Camera2RequestSettingsSet;
import com.hmdglobal.app.camera.exif.ExifInterface.GpsMeasureMode;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.motion.YuvCropper;
import com.hmdglobal.app.camera.ui.camera2.ExtendedFace;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.morphoinc.utils.camera.CameraJNI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class AndroidCamera2AgentImpl extends CameraAgent {
    private static final int HEIGHR_SIZE = 720;
    private static final int HEIGHR_SIZE_FRONT = 720;
    public static Key<Long> ISO_EXP = new Key("org.codeaurora.qcamera3.iso_exp_priority.use_iso_exp_priority", Long.class);
    private static final String RATIO_18_9 = "2.11";
    public static Key<Integer> SELECT_PRIORITY = new Key("org.codeaurora.qcamera3.iso_exp_priority.select_priority", Integer.class);
    private static final Tag TAG = new Tag("AndCam2AgntImp");
    private static final int WITH_SIZE = 1280;
    private static final int WITH_SIZE_FRONT = 1280;
    public static final Key<Integer> bokeh_blur_level = new Key("org.codeaurora.qcamera3.bokeh.blurLevel", Integer.class);
    public static final Key<Boolean> bokeh_enable = new Key("org.codeaurora.qcamera3.bokeh.enable", Boolean.class);
    public static final Key<Float[]> bokeh_point = new Key("org.codeaurora.qcamera3.westalgo_dof.focus", Float[].class);
    private int HDR_COUNT = 5;
    private final float LOG_2 = ((float) Math.log(2.0d));
    private int countTimes = 0;
    private int currentModuleId = -1;
    private final List<String> mCameraDevices;
    private CameraFaceDetectionCallback mCameraFaceDetectionCallback;
    private CameraFinalPreviewCallback mCameraFinalPreviewCallback;
    private final Camera2Handler mCameraHandler;
    private final HandlerThread mCameraHandlerThread = new HandlerThread("Camera2 Handler Thread");
    private final CameraManager mCameraManager;
    private CameraPreviewResultCallback mCameraPreviewCompleteCallback;
    private final CameraStateHolder mCameraState;
    private CaptureCompleteCallBack mCaptureCompleteCallBack;
    private final DispatchThread mDispatchThread;
    private Range<Long> mEtRange;
    private List<EvInfo> mEvInfos = new ArrayList();
    private CameraExceptionHandler mExceptionHandler;
    private int mHdrCount = 0;
    private List<Float> mHdrEv;
    private boolean mIsAdjustEt = false;
    private boolean mIsDepthOn = false;
    private boolean mIsUseJpeg = false;
    private Range<Integer> mIsoRange;
    private boolean mLastDepthOn = false;
    private int mLastModuleId = 4;
    private TotalCaptureResult mLastPreviewResult;
    private MotionPictureHelper mMotionPictureHelper;
    private boolean mNeedHdrBurst = false;
    private final MediaActionSound mNoisemaker;
    private int mNumCameraDevices;
    private TotalCaptureResult mPreviewResult;
    private LinkedList<TotalCaptureResult> mRawResultQueue = new LinkedList();
    private YuvCropper mYuvCropper;
    private int mcurrentOrientation;
    private Characteristics mmCharacteristics;

    private static abstract class CaptureAvailableListener extends CaptureCallback implements OnImageAvailableListener {
        private CaptureAvailableListener() {
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {
        CompareSizesByArea() {
        }

        public int compare(Size lhs, Size rhs) {
            return Long.signum((((long) lhs.getWidth()) * ((long) lhs.getHeight())) - (((long) rhs.getWidth()) * ((long) rhs.getHeight())));
        }
    }

    private static class AndroidCamera2DeviceInfo implements CameraDeviceInfo {
        private final String[] mCameraIds;
        private final CameraManager mCameraManager;
        private final int mFirstBackCameraId;
        private final int mFirstFrontCameraId;
        private final int mNumberOfCameras;

        private static class AndroidCharacteristics2 extends Characteristics {
            private CameraCharacteristics mCameraInfo;

            AndroidCharacteristics2(CameraCharacteristics cameraInfo) {
                this.mCameraInfo = cameraInfo;
            }

            public Size[] getSupportRawSize() {
                int[] caps = (int[]) this.mCameraInfo.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                Arrays.sort(caps);
                if (Arrays.binarySearch(caps, 3) >= 0) {
                    return ((StreamConfigurationMap) this.mCameraInfo.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(32);
                }
                return null;
            }

            public int getSupportedHardwareLevel(int id) {
                return ((Integer) this.mCameraInfo.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue();
            }

            public boolean isFacingBack() {
                return ((Integer) this.mCameraInfo.get(CameraCharacteristics.LENS_FACING)).equals(Integer.valueOf(1));
            }

            public boolean isFacingFront() {
                return ((Integer) this.mCameraInfo.get(CameraCharacteristics.LENS_FACING)).equals(Integer.valueOf(0));
            }

            public int getSensorOrientation() {
                return ((Integer) this.mCameraInfo.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
            }

            public Matrix getPreviewTransform(int currentDisplayOrientation, RectF surfaceDimensions, RectF desiredBounds) {
                if (!Characteristics.orientationIsValid(currentDisplayOrientation)) {
                    return new Matrix();
                }
                float[] surfacePolygon = rotate(convertRectToPoly(surfaceDimensions), (2 * currentDisplayOrientation) / 90);
                float[] desiredPolygon = convertRectToPoly(desiredBounds);
                Matrix matrix = new Matrix();
                Matrix transform = matrix;
                matrix.setPolyToPoly(surfacePolygon, 0, desiredPolygon, 0, 4);
                return transform;
            }

            public boolean canDisableShutterSound() {
                return true;
            }

            private static float[] convertRectToPoly(RectF rf) {
                return new float[]{rf.left, rf.top, rf.right, rf.top, rf.right, rf.bottom, rf.left, rf.bottom};
            }

            private static float[] rotate(float[] arr, int times) {
                if (times < 0) {
                    times = (times % arr.length) + arr.length;
                }
                float[] res = new float[arr.length];
                for (int offset = 0; offset < arr.length; offset++) {
                    res[offset] = arr[(times + offset) % arr.length];
                }
                return res;
            }
        }

        public AndroidCamera2DeviceInfo(CameraManager cameraManager, String[] cameraIds, int numberOfCameras) {
            Tag access$000;
            StringBuilder stringBuilder;
            this.mCameraManager = cameraManager;
            this.mCameraIds = cameraIds;
            this.mNumberOfCameras = numberOfCameras;
            int firstBackId = -1;
            int firstFrontId = -1;
            for (int id = 0; id < cameraIds.length; id++) {
                try {
                    int lensDirection = ((Integer) cameraManager.getCameraCharacteristics(cameraIds[id]).get(CameraCharacteristics.LENS_FACING)).intValue();
                    if (firstBackId == -1 && lensDirection == 1) {
                        firstBackId = id;
                    }
                    if (firstFrontId == -1 && lensDirection == 0) {
                        firstFrontId = id;
                    }
                } catch (CameraAccessException ex) {
                    access$000 = AndroidCamera2AgentImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Couldn't get characteristics of camera '");
                    stringBuilder.append(id);
                    stringBuilder.append("'");
                    Log.w(access$000, stringBuilder.toString(), ex);
                } catch (Exception e) {
                    access$000 = AndroidCamera2AgentImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Couldn't get characteristics of camera '");
                    stringBuilder.append(id);
                    stringBuilder.append("'");
                    Log.w(access$000, stringBuilder.toString(), e);
                }
            }
            this.mFirstBackCameraId = firstBackId;
            this.mFirstFrontCameraId = firstFrontId;
        }

        public Characteristics getCharacteristics(int cameraId) {
            try {
                return new AndroidCharacteristics2(this.mCameraManager.getCameraCharacteristics(this.mCameraIds[cameraId]));
            } catch (Exception e) {
                return null;
            }
        }

        public int getNumberOfCameras() {
            return this.mNumberOfCameras;
        }

        public int getFirstBackCameraId() {
            return this.mFirstBackCameraId;
        }

        public int getFirstFrontCameraId() {
            return this.mFirstFrontCameraId;
        }
    }

    private class AndroidCamera2ProxyImpl extends CameraProxy {
        private static final int MAX_BURST_COUNT = 99;
        private boolean mBurstCaptureCanceled = true;
        private int mBurstHasCaptureCount;
        private final CameraDevice mCamera;
        private final AndroidCamera2AgentImpl mCameraAgent;
        private final int mCameraIndex;
        private final AndroidCamera2Capabilities mCapabilities;
        private final Characteristics mCharacteristics;
        private CameraSettings mLastSettings;
        private CaptureAvailableListener mPicListener;
        private boolean mShutterSoundEnabled;

        public AndroidCamera2ProxyImpl(AndroidCamera2AgentImpl agent, int cameraIndex, CameraDevice camera, Characteristics characteristics, CameraCharacteristics properties) {
            this.mCameraAgent = agent;
            this.mCameraIndex = cameraIndex;
            this.mCamera = camera;
            this.mCharacteristics = characteristics;
            AndroidCamera2AgentImpl.this.mmCharacteristics = characteristics;
            this.mCapabilities = new AndroidCamera2Capabilities(properties);
            this.mLastSettings = null;
            this.mShutterSoundEnabled = true;
        }

        public Camera getCamera() {
            return null;
        }

        public int getCameraId() {
            return this.mCameraIndex;
        }

        public void setModuleId(int id, int currentOrientation) {
            AndroidCamera2AgentImpl.this.currentModuleId = id;
            AndroidCamera2AgentImpl.this.mcurrentOrientation = currentOrientation;
        }

        public Characteristics getCharacteristics() {
            return this.mCharacteristics;
        }

        public CameraCapabilities getCapabilities() {
            return this.mCapabilities;
        }

        public CameraAgent getAgent() {
            return this.mCameraAgent;
        }

        private AndroidCamera2Capabilities getSpecializedCapabilities() {
            return this.mCapabilities;
        }

        public void setPreviewTexture(SurfaceTexture surfaceTexture) {
            getSettings().setSizesLocked(true);
            AndroidCamera2AgentImpl.this.mIsDepthOn = getSettings().isDepthOn();
            AndroidCamera2AgentImpl.this.mIsUseJpeg = getSettings().isUseJpeg();
            AndroidCamera2AgentImpl.this.mIsAdjustEt = getSettings().isAdjustEt();
            super.setPreviewTexture(surfaceTexture);
        }

        public void setPreviewTextureSync(SurfaceTexture surfaceTexture) {
            getSettings().setSizesLocked(true);
            super.setPreviewTexture(surfaceTexture);
        }

        public void setPreviewResultCallback(Handler handler, CameraPreviewResultCallback cb) {
            AndroidCamera2AgentImpl.this.mCameraPreviewCompleteCallback = cb;
        }

        public void setCaptureResultCallback(Handler handler, CaptureCompleteCallBack cb) {
            AndroidCamera2AgentImpl.this.mCaptureCompleteCallBack = cb;
        }

        public void setFinalPreviewCallback(CameraFinalPreviewCallback cb) {
            AndroidCamera2AgentImpl.this.mCameraFinalPreviewCallback = cb;
        }

        public void setPreviewDataCallback(Handler handler, final CameraPreviewDataCallback cb) {
            OnImageAvailableListener onImageAvailableListener;
            final CameraPreviewDataCallbackWithHandler cbWithHandler = new CameraPreviewDataCallbackWithHandler(cb, handler);
            if (cb == null) {
                onImageAvailableListener = null;
            } else {
                onImageAvailableListener = new OnImageAvailableListener() {
                    /*  JADX ERROR: JadxRuntimeException in pass: SSATransform
                        jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r4_8 ?) in PHI: PHI: (r4_4 ?) = (r4_5 ?), (r4_8 ?), (r4_4 ?) binds: {(r4_5 ?)=B:68:?, (r4_4 ?)=B:47:0x0121}
                        	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
                        	at jadx.core.dex.visitors.ssa.SSATransform.inlinePhiInsn(SSATransform.java:392)
                        	at jadx.core.dex.visitors.ssa.SSATransform.replacePhiWithMove(SSATransform.java:360)
                        	at jadx.core.dex.visitors.ssa.SSATransform.fixPhiWithSameArgs(SSATransform.java:300)
                        	at jadx.core.dex.visitors.ssa.SSATransform.fixUselessPhi(SSATransform.java:275)
                        	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:61)
                        	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
                        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
                        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
                        	at java.util.ArrayList.forEach(ArrayList.java:1255)
                        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
                        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
                        	at java.util.ArrayList.forEach(ArrayList.java:1255)
                        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
                        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
                        	at java.util.ArrayList.forEach(ArrayList.java:1255)
                        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
                        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
                        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
                        	at java.lang.Iterable.forEach(Iterable.java:75)
                        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
                        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
                        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
                        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
                        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
                        */
                    public void onImageAvailable(android.media.ImageReader r20) {
                        /*
                        r19 = this;
                        r1 = r19;
                        r2 = r20.acquireNextImage();
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = r0.getSettings();	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = r0.isMotionOn();	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        if (r0 == 0) goto L_0x0083;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = r0.mMotionPictureHelper;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        if (r0 == 0) goto L_0x0083;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = r0.mMotionPictureHelper;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r4 = 3;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = r0.getDataFromImage(r2, r4);	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r4 = r2.getTimestamp();	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = "1";	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = r7.mCameraHandler;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = r7.mCameraId;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = r6.equals(r7);	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        if (r6 == 0) goto L_0x0054;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = r6.mMotionPictureHelper;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = r2.getWidth();	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r8 = r2.getHeight();	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6.mirrorYUV420SP(r0, r7, r8);	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = r6.mYuvCropper;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        if (r6 == 0) goto L_0x0076;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = r6.mMotionPictureHelper;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = r7.mYuvCropper;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r7 = r7.crop(r0);	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6.onPreview(r7, r4);	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        goto L_0x0081;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6 = r6.mMotionPictureHelper;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r6.onPreview(r0, r4);	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        goto L_0x0117;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = r6;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        if (r0 == 0) goto L_0x0117;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = r2.getPlanes();	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r4 = r0;
                        r0 = 0;
                        r5 = 0;
                        r6 = r2.getWidth();	 Catch:{ Exception -> 0x010b }
                        r7 = r2.getHeight();	 Catch:{ Exception -> 0x010b }
                        r8 = r6 * r7;	 Catch:{ Exception -> 0x010b }
                        r8 = new byte[r8];	 Catch:{ Exception -> 0x010b }
                        r9 = r4[r0];	 Catch:{ Exception -> 0x010b }
                        r9 = r9.getBuffer();	 Catch:{ Exception -> 0x010b }
                        r10 = r4[r0];	 Catch:{ Exception -> 0x010b }
                        r10 = r10.getRowStride();	 Catch:{ Exception -> 0x010b }
                        r11 = r4[r0];	 Catch:{ Exception -> 0x010b }
                        r11 = r11.getPixelStride();	 Catch:{ Exception -> 0x010b }
                        if (r0 != 0) goto L_0x00b0;	 Catch:{ Exception -> 0x010b }
                        r12 = r6;	 Catch:{ Exception -> 0x010b }
                        goto L_0x00b2;	 Catch:{ Exception -> 0x010b }
                        r12 = r6 / 2;	 Catch:{ Exception -> 0x010b }
                        if (r0 != 0) goto L_0x00b6;	 Catch:{ Exception -> 0x010b }
                        r13 = r7;	 Catch:{ Exception -> 0x010b }
                        goto L_0x00b8;	 Catch:{ Exception -> 0x010b }
                        r13 = r7 / 2;	 Catch:{ Exception -> 0x010b }
                        r14 = 1;	 Catch:{ Exception -> 0x010b }
                        if (r11 != r14) goto L_0x00c6;	 Catch:{ Exception -> 0x010b }
                        if (r10 != r12) goto L_0x00c6;	 Catch:{ Exception -> 0x010b }
                        r14 = r12 * r13;	 Catch:{ Exception -> 0x010b }
                        r9.get(r8, r5, r14);	 Catch:{ Exception -> 0x010b }
                        r14 = r12 * r13;	 Catch:{ Exception -> 0x010b }
                        r5 = r5 + r14;	 Catch:{ Exception -> 0x010b }
                        goto L_0x0103;	 Catch:{ Exception -> 0x010b }
                        r14 = new byte[r10];	 Catch:{ Exception -> 0x010b }
                        r15 = 0;	 Catch:{ Exception -> 0x010b }
                        r16 = r5;	 Catch:{ Exception -> 0x010b }
                        r5 = r15;	 Catch:{ Exception -> 0x010b }
                        r3 = r13 + -1;	 Catch:{ Exception -> 0x010b }
                        if (r5 >= r3) goto L_0x00e6;	 Catch:{ Exception -> 0x010b }
                        r9.get(r14, r15, r10);	 Catch:{ Exception -> 0x010b }
                        r3 = r15;	 Catch:{ Exception -> 0x010b }
                        if (r3 >= r12) goto L_0x00e3;	 Catch:{ Exception -> 0x010b }
                        r17 = r16 + 1;	 Catch:{ Exception -> 0x010b }
                        r18 = r3 * r11;	 Catch:{ Exception -> 0x010b }
                        r18 = r14[r18];	 Catch:{ Exception -> 0x010b }
                        r8[r16] = r18;	 Catch:{ Exception -> 0x010b }
                        r3 = r3 + 1;	 Catch:{ Exception -> 0x010b }
                        r16 = r17;	 Catch:{ Exception -> 0x010b }
                        goto L_0x00d4;	 Catch:{ Exception -> 0x010b }
                        r5 = r5 + 1;	 Catch:{ Exception -> 0x010b }
                        goto L_0x00cc;	 Catch:{ Exception -> 0x010b }
                        r3 = r9.remaining();	 Catch:{ Exception -> 0x010b }
                        r3 = java.lang.Math.min(r10, r3);	 Catch:{ Exception -> 0x010b }
                        r9.get(r14, r15, r3);	 Catch:{ Exception -> 0x010b }
                        r5 = r16;	 Catch:{ Exception -> 0x010b }
                        r3 = r15;	 Catch:{ Exception -> 0x010b }
                        if (r3 >= r12) goto L_0x0103;	 Catch:{ Exception -> 0x010b }
                        r15 = r5 + 1;	 Catch:{ Exception -> 0x010b }
                        r16 = r3 * r11;	 Catch:{ Exception -> 0x010b }
                        r16 = r14[r16];	 Catch:{ Exception -> 0x010b }
                        r8[r5] = r16;	 Catch:{ Exception -> 0x010b }
                        r3 = r3 + 1;	 Catch:{ Exception -> 0x010b }
                        r5 = r15;	 Catch:{ Exception -> 0x010b }
                        r15 = r3;	 Catch:{ Exception -> 0x010b }
                        goto L_0x00f3;	 Catch:{ Exception -> 0x010b }
                        r3 = r6;	 Catch:{ Exception -> 0x010b }
                        r14 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Exception -> 0x010b }
                        r3.onPreviewFrame(r8, r14);	 Catch:{ Exception -> 0x010b }
                        goto L_0x0117;
                        r0 = move-exception;
                        r3 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r5 = r0.getMessage();	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        com.android.ex.camera2.portability.debug.Log.e(r3, r5);	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        if (r2 == 0) goto L_?;
                        r2.close();
                        return;
                        r0 = move-exception;
                        r3 = r0;
                        r4 = 0;
                        goto L_0x0127;
                        r0 = move-exception;
                        r3 = r0;
                        throw r3;	 Catch:{ Throwable -> 0x0121, all -> 0x011d, all -> 0x0124 }
                        r0 = move-exception;
                        r4 = r3;
                        r3 = r0;
                        if (r2 == 0) goto L_0x0138;
                        if (r4 == 0) goto L_0x0135;
                        r2.close();	 Catch:{ Throwable -> 0x012f }
                        goto L_0x0138;
                        r0 = move-exception;
                        r5 = r0;
                        r4.addSuppressed(r5);
                        goto L_0x0138;
                        r2.close();
                        throw r3;
                        */
                        throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.portability.AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$AnonymousClass1.onImageAvailable(android.media.ImageReader):void");
                    }
                };
            }
            AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new Runnable() {
                public void run() {
                    AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(107, new Object[]{cbWithHandler, onImageAvailableListener}).sendToTarget();
                }
            });
        }

        public void setOneShotPreviewCallback(Handler handler, CameraPreviewDataCallback cb) {
        }

        public void setPreviewDataCallbackWithBuffer(Handler handler, CameraPreviewDataCallback cb) {
        }

        public void addCallbackBuffer(byte[] callbackBuffer) {
        }

        public void autoFocus(final Handler handler, final CameraAFCallback cb) {
            try {
                AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new Runnable() {

                    /* renamed from: com.android.ex.camera2.portability.AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$3$2 */
                    class AnonymousClass2 implements Runnable {
                        final /* synthetic */ byte[] val$pixels;

                        AnonymousClass2(byte[] bArr) {
                            this.val$pixels = bArr;
                        }

                        public void run() {
                            AnonymousClass3.this.val$jpeg.onPictureTaken(this.val$pixels, AndroidCamera2ProxyImpl.this);
                        }
                    }

                    public void run() {
                        CameraAFCallback cbForward = null;
                        if (cb != null) {
                            cbForward = new CameraAFCallback() {
                                public void onAutoFocus(final boolean focused, final CameraProxy camera) {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            cb.onAutoFocus(focused, camera);
                                        }
                                    });
                                }
                            };
                        }
                        AndroidCamera2AgentImpl.this.mCameraState.waitForStates(48);
                        AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(CameraActions.AUTO_FOCUS, cbForward).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @TargetApi(16)
        public void setAutoFocusMoveCallback(final Handler handler, final CameraAFMoveCallback cb) {
            try {
                AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    public void run() {
                        CameraAFMoveCallback cbForward = null;
                        if (cb != null) {
                            cbForward = new CameraAFMoveCallback() {
                                public void onAutoFocusMoving(final boolean moving, final CameraProxy camera) {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            cb.onAutoFocusMoving(moving, camera);
                                        }
                                    });
                                }
                            };
                        }
                        AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_AUTO_FOCUS_MOVE_CALLBACK, cbForward).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void takePicture(final Handler handler, final CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback postview, final CameraPictureCallback jpeg) {
            CaptureAvailableListener picListener = new CaptureAvailableListener() {
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                }

                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    if (AndroidCamera2AgentImpl.this.mCaptureCompleteCallBack != null) {
                        AndroidCamera2AgentImpl.this.mCaptureCompleteCallBack.onCaptureCompleted(session, request, result);
                    }
                    AndroidCamera2AgentImpl.this.mRawResultQueue.add(result);
                }

                /* JADX WARNING: Missing block: B:25:0x0095, code skipped:
            if (r0 != null) goto L_0x0097;
     */
                /* JADX WARNING: Missing block: B:26:0x0097, code skipped:
            if (r1 != null) goto L_0x0099;
     */
                /* JADX WARNING: Missing block: B:28:?, code skipped:
            r0.close();
     */
                /* JADX WARNING: Missing block: B:29:0x009d, code skipped:
            r3 = move-exception;
     */
                /* JADX WARNING: Missing block: B:30:0x009e, code skipped:
            r1.addSuppressed(r3);
     */
                /* JADX WARNING: Missing block: B:31:0x00a2, code skipped:
            r0.close();
     */
                public void onImageAvailable(android.media.ImageReader r11) {
                    /*
                    r10 = this;
                    r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;
                    r1 = new java.lang.StringBuilder;
                    r1.<init>();
                    r2 = "Pro [takePicture]onImageAvailable + countTimes = ";
                    r1.append(r2);
                    r2 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                    r2 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;
                    r2 = r2.countTimes;
                    r1.append(r2);
                    r1 = r1.toString();
                    com.android.ex.camera2.portability.debug.Log.d(r0, r1);
                    r0 = r6;
                    if (r0 == 0) goto L_0x0030;
                L_0x0024:
                    r0 = r5;
                    r1 = r6;
                    r2 = new com.android.ex.camera2.portability.-$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$5$fw58LJkhw88CzZi1AZl513mNuvc;
                    r2.<init>(r10, r1);
                    r0.post(r2);
                L_0x0030:
                    r0 = r11.acquireNextImage();
                    r1 = 0;
                    r2 = r9;	 Catch:{ Throwable -> 0x0093 }
                    if (r2 == 0) goto L_0x008b;
                L_0x0039:
                    r2 = java.lang.Runtime.getRuntime();	 Catch:{ Throwable -> 0x0093 }
                    r2 = r2.maxMemory();	 Catch:{ Throwable -> 0x0093 }
                    r2 = (int) r2;	 Catch:{ Throwable -> 0x0093 }
                    r2 = r2 / 1024;
                    r2 = r2 / 1024;
                    r2 = (long) r2;	 Catch:{ Throwable -> 0x0093 }
                    r4 = java.lang.Runtime.getRuntime();	 Catch:{ Throwable -> 0x0093 }
                    r4 = r4.totalMemory();	 Catch:{ Throwable -> 0x0093 }
                    r4 = (int) r4;	 Catch:{ Throwable -> 0x0093 }
                    r4 = r4 / 1024;
                    r4 = r4 / 1024;
                    r4 = (long) r4;	 Catch:{ Throwable -> 0x0093 }
                    r6 = r2 - r4;
                    r8 = 100;
                    r6 = (r6 > r8 ? 1 : (r6 == r8 ? 0 : -1));
                    if (r6 >= 0) goto L_0x006c;
                L_0x005d:
                    r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Throwable -> 0x0093 }
                    r7 = "memory is low";
                    com.android.ex.camera2.portability.debug.Log.d(r6, r7);	 Catch:{ Throwable -> 0x0093 }
                    if (r0 == 0) goto L_0x006b;
                L_0x0068:
                    r0.close();
                L_0x006b:
                    return;
                L_0x006c:
                    r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ OutOfMemoryError -> 0x0081 }
                    r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ OutOfMemoryError -> 0x0081 }
                    r6 = r6.getBuffer(r0);	 Catch:{ OutOfMemoryError -> 0x0081 }
                    r7 = r5;	 Catch:{ OutOfMemoryError -> 0x0081 }
                    r8 = r9;	 Catch:{ OutOfMemoryError -> 0x0081 }
                    r9 = new com.android.ex.camera2.portability.-$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$5$Gz2HrQXLsH9mprEChwuFWvSNCxg;	 Catch:{ OutOfMemoryError -> 0x0081 }
                    r9.<init>(r10, r8, r6);	 Catch:{ OutOfMemoryError -> 0x0081 }
                    r7.post(r9);	 Catch:{ OutOfMemoryError -> 0x0081 }
                    goto L_0x008b;
                L_0x0081:
                    r6 = move-exception;
                    r7 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Throwable -> 0x0093 }
                    r8 = "take picture failed due to outOfMemoryError";
                    com.android.ex.camera2.portability.debug.Log.d(r7, r8);	 Catch:{ Throwable -> 0x0093 }
                L_0x008b:
                    if (r0 == 0) goto L_0x0090;
                L_0x008d:
                    r0.close();
                L_0x0090:
                    return;
                L_0x0091:
                    r2 = move-exception;
                    goto L_0x0095;
                L_0x0093:
                    r1 = move-exception;
                    throw r1;	 Catch:{ all -> 0x0091 }
                L_0x0095:
                    if (r0 == 0) goto L_0x00a5;
                L_0x0097:
                    if (r1 == 0) goto L_0x00a2;
                L_0x0099:
                    r0.close();	 Catch:{ Throwable -> 0x009d }
                    goto L_0x00a5;
                L_0x009d:
                    r3 = move-exception;
                    r1.addSuppressed(r3);
                    goto L_0x00a5;
                L_0x00a2:
                    r0.close();
                L_0x00a5:
                    throw r2;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.portability.AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$AnonymousClass5.onImageAvailable(android.media.ImageReader):void");
                }
            };
            try {
                if (AndroidCamera2AgentImpl.this.mCameraFinalPreviewCallback != null) {
                    AndroidCamera2AgentImpl.this.mCameraFinalPreviewCallback.onFinalPreviewReturn();
                }
                AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new -$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$T9Z_aZahj6zP0kcfRPhjBB1PwIM(this, picListener));
                AndroidCamera2AgentImpl.this.countTimes = AndroidCamera2AgentImpl.this.countTimes + 1;
                Tag access$000 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Pro CameraActions.CAPTURE_PHOTO + countTimes = ");
                stringBuilder.append(AndroidCamera2AgentImpl.this.countTimes);
                Log.d(access$000, stringBuilder.toString());
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public static /* synthetic */ void lambda$takePicture$0(AndroidCamera2ProxyImpl androidCamera2ProxyImpl, CaptureAvailableListener picListener) {
            AndroidCamera2AgentImpl.this.mCameraState.waitForStates(-16);
            AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(CameraActions.CAPTURE_PHOTO, picListener).sendToTarget();
        }

        public void takePictureWithoutWaiting(Handler handler, CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback postview, CameraPictureCallback jpeg) {
        }

        public void burstShot(final Handler handler, final CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback postview, final CameraPictureCallback jpeg) {
            if (handler != null && shutter != null && jpeg != null) {
                AndroidCamera2AgentImpl.this.mNeedHdrBurst = getSettings().needBurst();
                Tag access$000 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mNeedHdrBurst = ");
                stringBuilder.append(AndroidCamera2AgentImpl.this.mNeedHdrBurst);
                Log.d(access$000, stringBuilder.toString());
                if (AndroidCamera2AgentImpl.this.mNeedHdrBurst) {
                    AndroidCamera2AgentImpl.this.mHdrCount = 0;
                    AndroidCamera2AgentImpl.this.mLastPreviewResult = AndroidCamera2AgentImpl.this.mPreviewResult;
                    AndroidCamera2AgentImpl.this.mHdrEv = getSettings().getHdrEv();
                    AndroidCamera2AgentImpl.this.HDR_COUNT = AndroidCamera2AgentImpl.this.mHdrEv.size();
                    AndroidCamera2AgentImpl.this.mEvInfos.clear();
                }
                try {
                    CameraCharacteristics cameraCharacteristics = AndroidCamera2AgentImpl.this.mCameraManager.getCameraCharacteristics(this.mCamera.getId());
                    AndroidCamera2AgentImpl.this.mEtRange = (Range) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
                    AndroidCamera2AgentImpl.this.mIsoRange = (Range) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                final CaptureAvailableListener picListener = new CaptureAvailableListener() {
                    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                        if (shutter != null) {
                            handler.post(new Runnable() {
                                public void run() {
                                    if (AndroidCamera2ProxyImpl.this.mShutterSoundEnabled) {
                                        if (!AndroidCamera2AgentImpl.this.mNeedHdrBurst) {
                                            AndroidCamera2AgentImpl.this.mNoisemaker.play(0);
                                        } else if (AndroidCamera2AgentImpl.this.mHdrCount == 0) {
                                            AndroidCamera2AgentImpl.this.mNoisemaker.play(0);
                                        }
                                        AndroidCamera2AgentImpl.this.mHdrCount = AndroidCamera2AgentImpl.this.mHdrCount + 1;
                                    }
                                    shutter.onShutter(AndroidCamera2ProxyImpl.this);
                                }
                            });
                        }
                    }

                    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
                    }

                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        if (AndroidCamera2AgentImpl.this.mCaptureCompleteCallBack != null) {
                            AndroidCamera2AgentImpl.this.mCaptureCompleteCallBack.onCaptureCompleted(session, request, result);
                        }
                    }

                    /* JADX WARNING: Missing block: B:24:0x00d0, code skipped:
            if (r0 != null) goto L_0x00d2;
     */
                    /* JADX WARNING: Missing block: B:25:0x00d2, code skipped:
            if (r1 != null) goto L_0x00d4;
     */
                    /* JADX WARNING: Missing block: B:27:?, code skipped:
            r0.close();
     */
                    /* JADX WARNING: Missing block: B:28:0x00d8, code skipped:
            r3 = move-exception;
     */
                    /* JADX WARNING: Missing block: B:29:0x00d9, code skipped:
            r1.addSuppressed(r3);
     */
                    /* JADX WARNING: Missing block: B:30:0x00dd, code skipped:
            r0.close();
     */
                    public void onImageAvailable(android.media.ImageReader r6) {
                        /*
                        r5 = this;
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;
                        r0 = r0.mNeedHdrBurst;
                        if (r0 != 0) goto L_0x007f;
                    L_0x000a:
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r0 = r0.mPicListener;
                        if (r0 == 0) goto L_0x007f;
                    L_0x0012:
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r0 = r0.mBurstCaptureCanceled;
                        if (r0 != 0) goto L_0x007f;
                    L_0x001a:
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;
                        r1 = new java.lang.StringBuilder;
                        r1.<init>();
                        r2 = "mBurstHasCaptureCount = ";
                        r1.append(r2);
                        r2 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r2 = r2.mBurstHasCaptureCount;
                        r1.append(r2);
                        r1 = r1.toString();
                        com.android.ex.camera2.portability.debug.Log.d(r0, r1);
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r0 = r0.mBurstHasCaptureCount;
                        r1 = 99;
                        if (r0 >= r1) goto L_0x007f;
                    L_0x0042:
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;
                        r1 = new java.lang.StringBuilder;
                        r1.<init>();
                        r2 = "mBurstHasCaptureCount = ";
                        r1.append(r2);
                        r2 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r2 = r2.mBurstHasCaptureCount;
                        r1.append(r2);
                        r1 = r1.toString();
                        com.android.ex.camera2.portability.debug.Log.i(r0, r1);
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ RuntimeException -> 0x0071 }
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ RuntimeException -> 0x0071 }
                        r0 = r0.mDispatchThread;	 Catch:{ RuntimeException -> 0x0071 }
                        r1 = new com.android.ex.camera2.portability.-$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$6$8ofxkACzFx8FZ2Vyw1z4VkYnjL0;	 Catch:{ RuntimeException -> 0x0071 }
                        r1.<init>(r5);	 Catch:{ RuntimeException -> 0x0071 }
                        r0.runJob(r1);	 Catch:{ RuntimeException -> 0x0071 }
                        goto L_0x007f;
                    L_0x0071:
                        r0 = move-exception;
                        r1 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r1 = r1.mCameraAgent;
                        r1 = r1.getCameraExceptionHandler();
                        r1.onDispatchThreadException(r0);
                    L_0x007f:
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;
                        r1 = "burstShot onImageAvailable";
                        com.android.ex.camera2.portability.debug.Log.d(r0, r1);
                        r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r0 = r0.getSettings();
                        r1 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;
                        r1 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;
                        r1 = r1.mEvInfos;
                        r0.setEvInfo(r1);
                        r0 = r6.acquireNextImage();
                        r1 = 0;
                        r2 = r8;	 Catch:{ Throwable -> 0x00ce }
                        if (r2 == 0) goto L_0x00c6;
                    L_0x00a2:
                        r2 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Throwable -> 0x00ce }
                        r3 = "begin getNV21FromImage";
                        com.android.ex.camera2.portability.debug.Log.d(r2, r3);	 Catch:{ Throwable -> 0x00ce }
                        r2 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.this;	 Catch:{ Throwable -> 0x00ce }
                        r2 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Throwable -> 0x00ce }
                        r2 = r2.getBuffer(r0);	 Catch:{ Throwable -> 0x00ce }
                        r3 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Throwable -> 0x00ce }
                        r4 = "end getNV21FromImage";
                        com.android.ex.camera2.portability.debug.Log.d(r3, r4);	 Catch:{ Throwable -> 0x00ce }
                        r3 = r4;	 Catch:{ Throwable -> 0x00ce }
                        r4 = new com.android.ex.camera2.portability.AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$6$2;	 Catch:{ Throwable -> 0x00ce }
                        r4.<init>(r2);	 Catch:{ Throwable -> 0x00ce }
                        r3.post(r4);	 Catch:{ Throwable -> 0x00ce }
                    L_0x00c6:
                        if (r0 == 0) goto L_0x00cb;
                    L_0x00c8:
                        r0.close();
                    L_0x00cb:
                        return;
                    L_0x00cc:
                        r2 = move-exception;
                        goto L_0x00d0;
                    L_0x00ce:
                        r1 = move-exception;
                        throw r1;	 Catch:{ all -> 0x00cc }
                    L_0x00d0:
                        if (r0 == 0) goto L_0x00e0;
                    L_0x00d2:
                        if (r1 == 0) goto L_0x00dd;
                    L_0x00d4:
                        r0.close();	 Catch:{ Throwable -> 0x00d8 }
                        goto L_0x00e0;
                    L_0x00d8:
                        r3 = move-exception;
                        r1.addSuppressed(r3);
                        goto L_0x00e0;
                    L_0x00dd:
                        r0.close();
                    L_0x00e0:
                        throw r2;
                        */
                        throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.portability.AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$AnonymousClass6.onImageAvailable(android.media.ImageReader):void");
                    }

                    public static /* synthetic */ void lambda$onImageAvailable$0(AnonymousClass6 anonymousClass6) {
                        if (!AndroidCamera2ProxyImpl.this.mBurstCaptureCanceled) {
                            AndroidCamera2AgentImpl.this.mCameraState.waitForStates(-16);
                            AndroidCamera2ProxyImpl.this.mBurstHasCaptureCount = AndroidCamera2ProxyImpl.this.mBurstHasCaptureCount + 1;
                            AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(CameraActions.BURST_SHOT, AndroidCamera2ProxyImpl.this.mPicListener).sendToTarget();
                        }
                    }
                };
                try {
                    if (AndroidCamera2AgentImpl.this.mCameraFinalPreviewCallback != null) {
                        AndroidCamera2AgentImpl.this.mCameraFinalPreviewCallback.onFinalPreviewReturn();
                    }
                    this.mPicListener = picListener;
                    AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new Runnable() {
                        public void run() {
                            AndroidCamera2ProxyImpl.this.mBurstHasCaptureCount = 0;
                            AndroidCamera2ProxyImpl.this.mBurstHasCaptureCount = AndroidCamera2ProxyImpl.this.mBurstHasCaptureCount + 1;
                            AndroidCamera2ProxyImpl.this.mBurstCaptureCanceled = false;
                            AndroidCamera2AgentImpl.this.mCameraState.waitForStates(-16);
                            AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(CameraActions.BURST_SHOT, picListener).sendToTarget();
                        }
                    });
                } catch (RuntimeException ex) {
                    this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
                }
            }
        }

        public void startPreAllocBurstShot() {
        }

        public void stopPreAllocBurstShot() {
        }

        public void initExtCamera(Context context) {
        }

        public void setGestureCallback(Handler handler, CameraGDCallBack cb) {
        }

        public void startGestureDetection() {
        }

        public void stopGestureDetection() {
        }

        public void startRama(Handler handler, int num) {
        }

        public void stopRama(Handler handler, int isMerge) {
        }

        public void setRamaCallback(Handler handler, CameraPanoramaCallback cb) {
        }

        public void setRamaMoveCallback(Handler handler, CameraPanoramaMoveCallback cb) {
        }

        public void abortBurstShot() {
            this.mBurstCaptureCanceled = true;
        }

        public void setZoomChangeListener(OnZoomChangeListener listener) {
        }

        public void setFaceDetectionCallback(Handler handler, CameraFaceDetectionCallback callback) {
            AndroidCamera2AgentImpl.this.mCameraFaceDetectionCallback = callback;
        }

        public void startFaceDetection() {
        }

        public void stopFaceDetection() {
        }

        public void setParameters(Parameters params) {
        }

        public Parameters getParameters() {
            return null;
        }

        public CameraSettings getSettings() {
            if (this.mLastSettings == null) {
                this.mLastSettings = AndroidCamera2AgentImpl.this.mCameraHandler.buildSettings(this.mCapabilities);
            }
            return this.mLastSettings;
        }

        public boolean applySettings(CameraSettings settings) {
            if (settings == null) {
                Log.w(AndroidCamera2AgentImpl.TAG, "null parameters in applySettings()");
                return false;
            } else if (!(settings instanceof AndroidCamera2Settings)) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Provided settings not compatible with the backing framework API");
                return false;
            } else if (!applySettingsHelper(settings, -2)) {
                return false;
            } else {
                this.mLastSettings = settings;
                return true;
            }
        }

        public void enableShutterSound(boolean enable) {
            this.mShutterSoundEnabled = enable;
        }

        public String dumpDeviceSettings() {
            return null;
        }

        public Handler getCameraHandler() {
            return AndroidCamera2AgentImpl.this.getCameraHandler();
        }

        public DispatchThread getDispatchThread() {
            return AndroidCamera2AgentImpl.this.getDispatchThread();
        }

        public CameraStateHolder getCameraState() {
            return AndroidCamera2AgentImpl.this.mCameraState;
        }
    }

    private static class AndroidCamera2StateHolder extends CameraStateHolder {
        public static final int CAMERA_CONFIGURED = 4;
        public static final int CAMERA_EXPOSURE_LOCK = 64;
        public static final int CAMERA_FOCUS_LOCKED = 32;
        public static final int CAMERA_LONG_SHOT = 128;
        public static final int CAMERA_PREVIEW_ACTIVE = 16;
        public static final int CAMERA_PREVIEW_READY = 8;
        public static final int CAMERA_UNCONFIGURED = 2;
        public static final int CAMERA_UNOPENED = 1;

        public AndroidCamera2StateHolder() {
            this(1);
        }

        public AndroidCamera2StateHolder(int state) {
            super(state);
        }
    }

    private class Camera2Handler extends HistoryHandler {
        private Rect mActiveArray;
        private Handler mBackgroundHandler;
        private HandlerThread mBackgroundThread;
        private CameraDevice mCamera;
        private StateCallback mCameraDeviceStateCallback = new StateCallback() {
            public void onOpened(CameraDevice camera) {
                Camera2Handler.this.mCamera = camera;
                if (Camera2Handler.this.mOpenCallback != null) {
                    try {
                        CameraCharacteristics props = AndroidCamera2AgentImpl.this.mCameraManager.getCameraCharacteristics(Camera2Handler.this.mCameraId);
                        Characteristics characteristics = AndroidCamera2AgentImpl.this.getCameraDeviceInfo().getCharacteristics(Camera2Handler.this.mCameraIndex);
                        Camera2Handler.this.initFaceParams();
                        Camera2Handler.this.startBackgroundThread();
                        Camera2Handler.this.mCameraProxy = new AndroidCamera2ProxyImpl(AndroidCamera2AgentImpl.this, Camera2Handler.this.mCameraIndex, Camera2Handler.this.mCamera, characteristics, props);
                        Camera2Handler.this.mPersistentSettings = new Camera2RequestSettingsSet();
                        Camera2Handler.this.mActiveArray = (Rect) props.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                        Camera2Handler.this.mLegacyDevice = ((Integer) props.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue() == 2;
                        Camera2Handler.this.changeState(2);
                        Camera2Handler.this.mOpenCallback.onCameraOpened(Camera2Handler.this.mCameraProxy);
                    } catch (CameraAccessException e) {
                        Log.e(AndroidCamera2AgentImpl.TAG, "1530->agent2", new Exception());
                        Camera2Handler.this.mOpenCallback.onDeviceOpenFailure(Camera2Handler.this.mCameraIndex, Camera2Handler.this.generateHistoryString(Camera2Handler.this.mCameraIndex));
                    }
                }
            }

            public void onDisconnected(CameraDevice camera) {
                Tag access$000 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Camera device '");
                stringBuilder.append(Camera2Handler.this.mCameraIndex);
                stringBuilder.append("' was disconnected");
                Log.w(access$000, stringBuilder.toString());
            }

            public void onError(CameraDevice camera, int error) {
                Tag access$000 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Camera device '");
                stringBuilder.append(Camera2Handler.this.mCameraIndex);
                stringBuilder.append("' encountered error code '");
                stringBuilder.append(error);
                stringBuilder.append('\'');
                Log.e(access$000, stringBuilder.toString());
                Log.e(AndroidCamera2AgentImpl.TAG, "onError->Agent2", new Exception());
                if (Camera2Handler.this.mOpenCallback != null) {
                    Camera2Handler.this.mOpenCallback.onDeviceOpenFailure(Camera2Handler.this.mCameraIndex, Camera2Handler.this.generateHistoryString(Camera2Handler.this.mCameraIndex));
                }
            }
        };
        private String mCameraId;
        private int mCameraIndex;
        private CameraCaptureSession.StateCallback mCameraPreviewStateCallback = new CameraCaptureSession.StateCallback() {
            public void onConfigured(CameraCaptureSession session) {
                Camera2Handler.this.mSession = session;
                Camera2Handler.this.changeState(8);
                if (!GpsMeasureMode.MODE_3_DIMENSIONAL.equals(Camera2Handler.this.mCameraId)) {
                    Camera2Handler.this.initFaceDetect();
                }
            }

            public void onConfigureFailed(CameraCaptureSession session) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Failed to configure the camera for capture");
            }

            public void onActive(CameraCaptureSession session) {
                if (Camera2Handler.this.mOneshotPreviewingCallback != null) {
                    Camera2Handler.this.mOneshotPreviewingCallback.onPreviewStarted();
                    Camera2Handler.this.mOneshotPreviewingCallback = null;
                }
            }
        };
        private AndroidCamera2ProxyImpl mCameraProxy;
        private CameraResultStateCallback mCameraResultStateCallback = new CameraResultStateCallback() {
            private long mLastAeFrameNumber = -1;
            private long mLastAfFrameNumber = -1;
            private int mLastAfState = -1;

            public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult result) {
                monitorControlStates(result);
            }

            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                monitorControlStates(result);
                if (AndroidCamera2AgentImpl.this.mCameraPreviewCompleteCallback != null) {
                    AndroidCamera2AgentImpl.this.mCameraPreviewCompleteCallback.onCaptureComplete(session, request, result);
                }
                AndroidCamera2AgentImpl.this.mPreviewResult = result;
            }

            public void monitorControlStates(CaptureResult result) {
                if (AndroidCamera2AgentImpl.this.mCameraFaceDetectionCallback != null) {
                    Face[] faces = (Face[]) result.get(CaptureResult.STATISTICS_FACES);
                    ExtendedFace[] extendedFaces = AndroidCamera2AgentImpl.this.generateExtendedFaces(result, faces);
                    try {
                        if (AndroidCamera2AgentImpl.this.mCameraFaceDetectionCallback != null) {
                            AndroidCamera2AgentImpl.this.mCameraFaceDetectionCallback.onFaceDetection(Camera2Handler.this.mActiveArray, faces, extendedFaces, Camera2Handler.this.mCameraProxy);
                        }
                    } catch (Exception e) {
                    }
                }
                Integer afStateMaybe = (Integer) result.get(CaptureResult.CONTROL_AF_STATE);
                if (afStateMaybe != null) {
                    int afState = afStateMaybe.intValue();
                    if (result.getFrameNumber() > this.mLastAfFrameNumber) {
                        boolean afStateChanged = afState != this.mLastAfState;
                        this.mLastAfState = afState;
                        this.mLastAfFrameNumber = result.getFrameNumber();
                        switch (afState) {
                            case 0:
                            case 4:
                            case 5:
                                if (Camera2Handler.this.mOneshotAfCallback != null) {
                                    CameraAFCallback access$2200 = Camera2Handler.this.mOneshotAfCallback;
                                    boolean z = afState == 4 || afState == 0;
                                    access$2200.onAutoFocus(z, Camera2Handler.this.mCameraProxy);
                                    Camera2Handler.this.mOneshotAfCallback = null;
                                    break;
                                }
                                break;
                            case 1:
                            case 2:
                            case 6:
                                if (afStateChanged && Camera2Handler.this.mPassiveAfCallback != null) {
                                    Camera2Handler.this.mPassiveAfCallback.onAutoFocusMoving(afState == 1, Camera2Handler.this.mCameraProxy);
                                    break;
                                }
                        }
                    }
                }
                Integer aeStateMaybe = (Integer) result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeStateMaybe != null) {
                    int aeState = aeStateMaybe.intValue();
                    if (result.getFrameNumber() > this.mLastAeFrameNumber) {
                        Camera2Handler.this.mCurrentAeState = aeStateMaybe.intValue();
                        this.mLastAeFrameNumber = result.getFrameNumber();
                        if (aeState != 0) {
                            switch (aeState) {
                                case 2:
                                case 3:
                                case 4:
                                    break;
                                default:
                                    return;
                            }
                        }
                        if (Camera2Handler.this.mOneshotCaptureCallback != null) {
                            Camera2Handler.this.mCaptureReader.setOnImageAvailableListener(Camera2Handler.this.mOneshotCaptureCallback, Camera2Handler.this);
                            try {
                                Camera2Handler.this.mSession.capture(Camera2Handler.this.mPersistentSettings.createRequest(Camera2Handler.this.mCamera, 2, Camera2Handler.this.mCaptureReader.getSurface()), Camera2Handler.this.mOneshotCaptureCallback, Camera2Handler.this);
                                Log.d(AndroidCamera2AgentImpl.TAG, "Pro  [monitorControlStates]mSession.capture()");
                            } catch (CameraAccessException ex) {
                                Log.e(AndroidCamera2AgentImpl.TAG, "Unable to initiate capture", ex);
                            } catch (Throwable th) {
                                Camera2Handler.this.mOneshotCaptureCallback = null;
                            }
                            Camera2Handler.this.mOneshotCaptureCallback = null;
                        }
                    }
                }
            }

            public void resetState() {
                this.mLastAfState = -1;
                this.mLastAfFrameNumber = -1;
                this.mLastAeFrameNumber = -1;
            }

            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                Tag access$000 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Capture attempt failed with reason ");
                stringBuilder.append(failure.getReason());
                Log.e(access$000, stringBuilder.toString());
            }
        };
        private int mCancelAfPending = 0;
        private ImageReader mCaptureReader;
        private int mCurrentAeState = 0;
        private int mFaceDetectMode;
        private boolean mFaceDetectSupported = false;
        private int mHardwareSupportLevel = 2;
        private boolean mIsSaveDng;
        private boolean mLegacyDevice;
        private CameraAFCallback mOneshotAfCallback;
        private CaptureAvailableListener mOneshotCaptureCallback;
        private CameraStartPreviewCallback mOneshotPreviewingCallback;
        private CameraOpenCallback mOpenCallback;
        private CameraAFMoveCallback mPassiveAfCallback;
        private Camera2RequestSettingsSet mPersistentSettings;
        private Size mPhotoSize;
        private OnImageAvailableListener mPreviewDataAvailableListener;
        private CameraPreviewDataCallbackWithHandler mPreviewDataCallbackWithBuffer;
        private ImageReader mPreviewReader;
        private Size mPreviewSize;
        private Surface mPreviewSurface;
        private int mPreviewTemplate = 1;
        private SurfaceTexture mPreviewTexture;
        private ImageReader mRawImageReader;
        private CameraCaptureSession mSession;
        private boolean mShouldSaveDng;
        private Builder previewRequestBuilder;

        private abstract class CameraResultStateCallback extends CaptureCallback {
            public abstract void monitorControlStates(CaptureResult captureResult);

            public abstract void resetState();

            private CameraResultStateCallback() {
            }
        }

        Camera2Handler(Looper looper) {
            super(looper);
        }

        private void captureStillPicture(CaptureAvailableListener listener) throws CameraAccessException {
            Log.d(AndroidCamera2AgentImpl.TAG, "test_zzw  captureStillPicture in ");
            Builder captureBuilder = this.mCamera.createCaptureRequest(2);
            captureBuilder.addTarget(this.mCaptureReader.getSurface());
            try {
                Object[] switchValues = new Object[]{"org.codeaurora.qcamera3.westalgo_blur.switch", Integer.class};
                Object[] levelValues = new Object[]{"org.codeaurora.qcamera3.westalgo_blur.level", Integer.class};
                Object[] widthValues = new Object[]{"org.codeaurora.qcamera3.westalgo_blur.width", Integer.class};
                Object[] heightValues = new Object[]{"org.codeaurora.qcamera3.westalgo_blur.height", Integer.class};
                Constructor cons = Class.forName("android.hardware.camera2.CaptureRequest$Key").getDeclaredConstructor(new Class[]{String.class, Class.class});
                cons.setAccessible(true);
                Object switchObj = cons.newInstance(switchValues);
                Object levelObj = cons.newInstance(levelValues);
                Object widthObj = cons.newInstance(widthValues);
                Object heightObj = cons.newInstance(heightValues);
                captureBuilder.set((Key) switchObj, Integer.valueOf(1));
                captureBuilder.set((Key) levelObj, Integer.valueOf(CameraAgent.mLiveBolkenFrontLevel));
                captureBuilder.set((Key) widthObj, Integer.valueOf(this.mPreviewSize.width()));
                captureBuilder.set((Key) heightObj, Integer.valueOf(this.mPreviewSize.height()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(AndroidCamera2AgentImpl.this.mcurrentOrientation));
            try {
                this.mSession.stopRepeating();
                this.mSession.abortCaptures();
                this.mSession.capture(captureBuilder.build(), listener, null);
            } catch (CameraAccessException e2) {
                e2.printStackTrace();
            }
        }

        public void handleMessage(android.os.Message r18) {
            /*
            r17 = this;
            r1 = r17;
            r2 = r18;
            super.handleMessage(r18);
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;
            r3 = new java.lang.StringBuilder;
            r3.<init>();
            r4 = "handleMessage - action = '";
            r3.append(r4);
            r4 = r2.what;
            r4 = com.android.ex.camera2.portability.CameraActions.stringify(r4);
            r3.append(r4);
            r4 = "'";
            r3.append(r4);
            r3 = r3.toString();
            com.android.ex.camera2.portability.debug.Log.v(r0, r3);
            r0 = r2.what;
            r3 = r0;
            r4 = 32;
            r5 = 3;
            r6 = 8;
            r0 = 16;
            r7 = 2;
            r8 = 0;
            r9 = 0;
            r10 = 1;
            switch(r3) {
                case 1: goto L_0x05ff;
                case 2: goto L_0x057c;
                case 3: goto L_0x04fd;
                case 101: goto L_0x04f4;
                case 102: goto L_0x0408;
                case 103: goto L_0x03e7;
                case 107: goto L_0x0373;
                case 204: goto L_0x036a;
                case 301: goto L_0x0361;
                case 302: goto L_0x030d;
                case 303: goto L_0x0305;
                case 305: goto L_0x02fe;
                case 502: goto L_0x02e0;
                case 503: goto L_0x02d1;
                case 601: goto L_0x0091;
                case 602: goto L_0x0091;
                case 603: goto L_0x0077;
                case 701: goto L_0x006d;
                case 702: goto L_0x0068;
                case 703: goto L_0x004c;
                case 800: goto L_0x003f;
                default: goto L_0x003b;
            };
        L_0x003b:
            r0 = new java.lang.RuntimeException;	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e6;
        L_0x003f:
            r0 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r0 = (java.lang.Integer) r0;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.intValue();	 Catch:{ Exception -> 0x0700 }
            r1.setPreviewBolken(r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x004c:
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r4 = r4.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r4 = r4.getState();	 Catch:{ Exception -> 0x0700 }
            if (r4 >= r0) goto L_0x0063;
        L_0x0058:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r4 = "Refusing to stop preview at inappropriate time";
            com.android.ex.camera2.portability.debug.Log.w(r0, r4);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0063:
            r1.changeState(r6);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0068:
            r1.changeState(r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x006d:
            r0 = new android.graphics.SurfaceTexture;	 Catch:{ Exception -> 0x0700 }
            r0.<init>(r9);	 Catch:{ Exception -> 0x0700 }
            r1.setPreviewTexture(r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0077:
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r4 = r4.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r4 = r4.getState();	 Catch:{ Exception -> 0x0700 }
            r5 = 128; // 0x80 float:1.794E-43 double:6.32E-322;
            if (r4 >= r5) goto L_0x0087;
        L_0x0085:
            goto L_0x06e1;
        L_0x0087:
            r4 = r1.mSession;	 Catch:{ Exception -> 0x0700 }
            r4.abortCaptures();	 Catch:{ Exception -> 0x0700 }
            r1.changeState(r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0091:
            r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r6 = r6.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r6 = r6.getState();	 Catch:{ Exception -> 0x0700 }
            if (r6 >= r0) goto L_0x00a8;
        L_0x009d:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r4 = "Photos may only be taken when a preview is active";
            com.android.ex.camera2.portability.debug.Log.e(r0, r4);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x00a8:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.getState();	 Catch:{ Exception -> 0x0700 }
            if (r0 == r4) goto L_0x00bd;
        L_0x00b4:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r4 = "Taking a (likely blurry) photo without the lens locked";
            com.android.ex.camera2.portability.debug.Log.w(r0, r4);	 Catch:{ Exception -> 0x0700 }
        L_0x00bd:
            r0 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r0 = (com.android.ex.camera2.portability.AndroidCamera2AgentImpl.CaptureAvailableListener) r0;	 Catch:{ Exception -> 0x0700 }
            r4 = r0;
            r0 = r1.mLegacyDevice;	 Catch:{ Exception -> 0x0700 }
            if (r0 != 0) goto L_0x018d;
        L_0x00c6:
            r0 = 602; // 0x25a float:8.44E-43 double:2.974E-321;
            if (r3 != r0) goto L_0x018d;
        L_0x00ca:
            r0 = r1.mCaptureReader;	 Catch:{ Exception -> 0x0700 }
            r0.setOnImageAvailableListener(r4, r1);	 Catch:{ Exception -> 0x0700 }
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.mNeedHdrBurst;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x0168;
        L_0x00d7:
            r0 = new java.util.ArrayList;	 Catch:{ Exception -> 0x0700 }
            r0.<init>();	 Catch:{ Exception -> 0x0700 }
            r5 = r0;
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ CameraAccessException -> 0x015c }
            r6 = new java.lang.StringBuilder;	 Catch:{ CameraAccessException -> 0x015c }
            r6.<init>();	 Catch:{ CameraAccessException -> 0x015c }
            r11 = "HDR_COUNT = ";
            r6.append(r11);	 Catch:{ CameraAccessException -> 0x015c }
            r11 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ CameraAccessException -> 0x015c }
            r11 = r11.HDR_COUNT;	 Catch:{ CameraAccessException -> 0x015c }
            r6.append(r11);	 Catch:{ CameraAccessException -> 0x015c }
            r6 = r6.toString();	 Catch:{ CameraAccessException -> 0x015c }
            com.android.ex.camera2.portability.debug.Log.d(r0, r6);	 Catch:{ CameraAccessException -> 0x015c }
            r0 = r9;
        L_0x00fc:
            r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ CameraAccessException -> 0x015c }
            r6 = r6.HDR_COUNT;	 Catch:{ CameraAccessException -> 0x015c }
            if (r0 >= r6) goto L_0x0156;
        L_0x0104:
            r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ CameraAccessException -> 0x015c }
            r6 = r6.mHdrEv;	 Catch:{ CameraAccessException -> 0x015c }
            r6 = r6.get(r0);	 Catch:{ CameraAccessException -> 0x015c }
            r6 = (java.lang.Float) r6;	 Catch:{ CameraAccessException -> 0x015c }
            r6 = r6.floatValue();	 Catch:{ CameraAccessException -> 0x015c }
            r11 = 0;
            r6 = (r6 > r11 ? 1 : (r6 == r11 ? 0 : -1));
            if (r6 != 0) goto L_0x0132;
        L_0x0119:
            r6 = r1.mPersistentSettings;	 Catch:{ CameraAccessException -> 0x015c }
            r11 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x015c }
            r12 = new android.view.Surface[r10];	 Catch:{ CameraAccessException -> 0x015c }
            r13 = r1.mCaptureReader;	 Catch:{ CameraAccessException -> 0x015c }
            r13 = r13.getSurface();	 Catch:{ CameraAccessException -> 0x015c }
            r12[r9] = r13;	 Catch:{ CameraAccessException -> 0x015c }
            r6 = r6.createRequest(r11, r7, r12);	 Catch:{ CameraAccessException -> 0x015c }
            r1.setEvInfos(r0);	 Catch:{ CameraAccessException -> 0x015c }
            r5.add(r6);	 Catch:{ CameraAccessException -> 0x015c }
            goto L_0x0153;
        L_0x0132:
            r6 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x015c }
            r6 = r6.createCaptureRequest(r7);	 Catch:{ CameraAccessException -> 0x015c }
            r11 = r1.mCaptureReader;	 Catch:{ CameraAccessException -> 0x015c }
            r11 = r11.getSurface();	 Catch:{ CameraAccessException -> 0x015c }
            r6.addTarget(r11);	 Catch:{ CameraAccessException -> 0x015c }
            r11 = r1.mPreviewSurface;	 Catch:{ CameraAccessException -> 0x015c }
            r6.addTarget(r11);	 Catch:{ CameraAccessException -> 0x015c }
            r1.addBaseParameterToRequest(r6);	 Catch:{ CameraAccessException -> 0x015c }
            r1.setCaptureRequest(r6, r0);	 Catch:{ CameraAccessException -> 0x015c }
            r11 = r6.build();	 Catch:{ CameraAccessException -> 0x015c }
            r5.add(r11);	 Catch:{ CameraAccessException -> 0x015c }
        L_0x0153:
            r0 = r0 + 1;
            goto L_0x00fc;
        L_0x0156:
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x015c }
            r0.captureBurst(r5, r4, r1);	 Catch:{ CameraAccessException -> 0x015c }
            goto L_0x0166;
        L_0x015c:
            r0 = move-exception;
            r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r9 = "Could not access camera for still image capture.";
            com.android.ex.camera2.portability.debug.Log.e(r6, r9);	 Catch:{ Exception -> 0x0700 }
        L_0x0166:
            goto L_0x06e1;
        L_0x0168:
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x0181 }
            r5 = r1.mPersistentSettings;	 Catch:{ CameraAccessException -> 0x0181 }
            r6 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x0181 }
            r11 = new android.view.Surface[r10];	 Catch:{ CameraAccessException -> 0x0181 }
            r12 = r1.mCaptureReader;	 Catch:{ CameraAccessException -> 0x0181 }
            r12 = r12.getSurface();	 Catch:{ CameraAccessException -> 0x0181 }
            r11[r9] = r12;	 Catch:{ CameraAccessException -> 0x0181 }
            r5 = r5.createRequest(r6, r7, r11);	 Catch:{ CameraAccessException -> 0x0181 }
            r0.capture(r5, r4, r1);	 Catch:{ CameraAccessException -> 0x0181 }
            goto L_0x06e1;
        L_0x0181:
            r0 = move-exception;
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r6 = "Unable to initiate immediate capture";
            com.android.ex.camera2.portability.debug.Log.e(r5, r6, r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x018d:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r6 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0700 }
            r6.<init>();	 Catch:{ Exception -> 0x0700 }
            r11 = "Pro mLegacyDevice = ";
            r6.append(r11);	 Catch:{ Exception -> 0x0700 }
            r11 = r1.mLegacyDevice;	 Catch:{ Exception -> 0x0700 }
            r6.append(r11);	 Catch:{ Exception -> 0x0700 }
            r11 = "  mCurrentAeState = ";
            r6.append(r11);	 Catch:{ Exception -> 0x0700 }
            r11 = r1.mCurrentAeState;	 Catch:{ Exception -> 0x0700 }
            r6.append(r11);	 Catch:{ Exception -> 0x0700 }
            r11 = " AE_MODE = ";
            r6.append(r11);	 Catch:{ Exception -> 0x0700 }
            r11 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r12 = android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE;	 Catch:{ Exception -> 0x0700 }
            r11 = r11.get(r12);	 Catch:{ Exception -> 0x0700 }
            r6.append(r11);	 Catch:{ Exception -> 0x0700 }
            r11 = " FLASH_MODE = ";
            r6.append(r11);	 Catch:{ Exception -> 0x0700 }
            r11 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r12 = android.hardware.camera2.CaptureRequest.FLASH_MODE;	 Catch:{ Exception -> 0x0700 }
            r11 = r11.get(r12);	 Catch:{ Exception -> 0x0700 }
            r6.append(r11);	 Catch:{ Exception -> 0x0700 }
            r6 = r6.toString();	 Catch:{ Exception -> 0x0700 }
            com.android.ex.camera2.portability.debug.Log.d(r0, r6);	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mLegacyDevice;	 Catch:{ Exception -> 0x0700 }
            if (r0 != 0) goto L_0x0236;
        L_0x01d5:
            r0 = r1.mCurrentAeState;	 Catch:{ Exception -> 0x0700 }
            if (r0 != r7) goto L_0x01f6;
        L_0x01d9:
            r0 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r6 = android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE;	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.Integer.valueOf(r5);	 Catch:{ Exception -> 0x0700 }
            r0 = r0.matches(r6, r5);	 Catch:{ Exception -> 0x0700 }
            if (r0 != 0) goto L_0x01f6;
        L_0x01e7:
            r0 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r5 = android.hardware.camera2.CaptureRequest.FLASH_MODE;	 Catch:{ Exception -> 0x0700 }
            r6 = java.lang.Integer.valueOf(r10);	 Catch:{ Exception -> 0x0700 }
            r0 = r0.matches(r5, r6);	 Catch:{ Exception -> 0x0700 }
            if (r0 != 0) goto L_0x01f6;
        L_0x01f5:
            goto L_0x0236;
        L_0x01f6:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r5 = "Forcing pre-capture autoexposure convergence";
            com.android.ex.camera2.portability.debug.Log.i(r0, r5);	 Catch:{ Exception -> 0x0700 }
            r0 = new com.android.ex.camera2.portability.AndroidCamera2AgentImpl$Camera2Handler$2;	 Catch:{ Exception -> 0x0700 }
            r0.<init>(r4);	 Catch:{ Exception -> 0x0700 }
            r5 = r0;
            r0 = new com.android.ex.camera2.utils.Camera2RequestSettingsSet;	 Catch:{ Exception -> 0x0700 }
            r6 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r0.<init>(r6);	 Catch:{ Exception -> 0x0700 }
            r6 = r0;
            r0 = android.hardware.camera2.CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER;	 Catch:{ Exception -> 0x0700 }
            r11 = java.lang.Integer.valueOf(r10);	 Catch:{ Exception -> 0x0700 }
            r6.set(r0, r11);	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x022a }
            r11 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x022a }
            r12 = r1.mPreviewTemplate;	 Catch:{ CameraAccessException -> 0x022a }
            r13 = new android.view.Surface[r10];	 Catch:{ CameraAccessException -> 0x022a }
            r14 = r1.mPreviewSurface;	 Catch:{ CameraAccessException -> 0x022a }
            r13[r9] = r14;	 Catch:{ CameraAccessException -> 0x022a }
            r9 = r6.createRequest(r11, r12, r13);	 Catch:{ CameraAccessException -> 0x022a }
            r0.capture(r9, r5, r1);	 Catch:{ CameraAccessException -> 0x022a }
            goto L_0x0234;
        L_0x022a:
            r0 = move-exception;
            r9 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r11 = "Unable to run autoexposure and perform capture";
            com.android.ex.camera2.portability.debug.Log.e(r9, r11, r0);	 Catch:{ Exception -> 0x0700 }
        L_0x0234:
            goto L_0x06e1;
        L_0x0236:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r5 = "Skipping pre-capture autoexposure convergence";
            com.android.ex.camera2.portability.debug.Log.i(r0, r5);	 Catch:{ Exception -> 0x0700 }
            r0 = new com.android.ex.camera2.portability.-$$Lambda$AndroidCamera2AgentImpl$Camera2Handler$RwBTxUHV_ZNMMA8uo7LM8OEDehM;	 Catch:{ Exception -> 0x0700 }
            r0.<init>(r1);	 Catch:{ Exception -> 0x0700 }
            r5 = r0;
            r0 = r1.mRawImageReader;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x024e;
        L_0x0249:
            r0 = r1.mRawImageReader;	 Catch:{ Exception -> 0x0700 }
            r0.setOnImageAvailableListener(r5, r1);	 Catch:{ Exception -> 0x0700 }
        L_0x024e:
            r0 = r1.mCaptureReader;	 Catch:{ Exception -> 0x0700 }
            r0.setOnImageAvailableListener(r4, r1);	 Catch:{ Exception -> 0x0700 }
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ CameraAccessException -> 0x02c5 }
            r6 = new java.lang.StringBuilder;	 Catch:{ CameraAccessException -> 0x02c5 }
            r6.<init>();	 Catch:{ CameraAccessException -> 0x02c5 }
            r11 = "capture, mShouldSaveDng = ";
            r6.append(r11);	 Catch:{ CameraAccessException -> 0x02c5 }
            r11 = r1.mShouldSaveDng;	 Catch:{ CameraAccessException -> 0x02c5 }
            r6.append(r11);	 Catch:{ CameraAccessException -> 0x02c5 }
            r6 = r6.toString();	 Catch:{ CameraAccessException -> 0x02c5 }
            com.android.ex.camera2.portability.debug.Log.d(r0, r6);	 Catch:{ CameraAccessException -> 0x02c5 }
            r0 = r1.mRawImageReader;	 Catch:{ CameraAccessException -> 0x02c5 }
            if (r0 == 0) goto L_0x0295;
        L_0x0271:
            r0 = r1.mShouldSaveDng;	 Catch:{ CameraAccessException -> 0x02c5 }
            if (r0 == 0) goto L_0x0295;
        L_0x0275:
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x02c5 }
            r6 = r1.mPersistentSettings;	 Catch:{ CameraAccessException -> 0x02c5 }
            r11 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x02c5 }
            r12 = new android.view.Surface[r7];	 Catch:{ CameraAccessException -> 0x02c5 }
            r13 = r1.mCaptureReader;	 Catch:{ CameraAccessException -> 0x02c5 }
            r13 = r13.getSurface();	 Catch:{ CameraAccessException -> 0x02c5 }
            r12[r9] = r13;	 Catch:{ CameraAccessException -> 0x02c5 }
            r9 = r1.mRawImageReader;	 Catch:{ CameraAccessException -> 0x02c5 }
            r9 = r9.getSurface();	 Catch:{ CameraAccessException -> 0x02c5 }
            r12[r10] = r9;	 Catch:{ CameraAccessException -> 0x02c5 }
            r6 = r6.createRequest(r11, r7, r12);	 Catch:{ CameraAccessException -> 0x02c5 }
            r0.capture(r6, r4, r1);	 Catch:{ CameraAccessException -> 0x02c5 }
            goto L_0x02c4;
        L_0x0295:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ CameraAccessException -> 0x02c5 }
            r0 = r0.mmCharacteristics;	 Catch:{ CameraAccessException -> 0x02c5 }
            r0 = r0.isFacingFront();	 Catch:{ CameraAccessException -> 0x02c5 }
            if (r0 == 0) goto L_0x02ad;
        L_0x02a1:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ CameraAccessException -> 0x02c5 }
            r0 = r0.currentModuleId;	 Catch:{ CameraAccessException -> 0x02c5 }
            if (r0 != 0) goto L_0x02ad;
        L_0x02a9:
            r1.captureStillPicture(r4);	 Catch:{ CameraAccessException -> 0x02c5 }
            goto L_0x02c4;
        L_0x02ad:
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x02c5 }
            r6 = r1.mPersistentSettings;	 Catch:{ CameraAccessException -> 0x02c5 }
            r11 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x02c5 }
            r12 = new android.view.Surface[r10];	 Catch:{ CameraAccessException -> 0x02c5 }
            r13 = r1.mCaptureReader;	 Catch:{ CameraAccessException -> 0x02c5 }
            r13 = r13.getSurface();	 Catch:{ CameraAccessException -> 0x02c5 }
            r12[r9] = r13;	 Catch:{ CameraAccessException -> 0x02c5 }
            r6 = r6.createRequest(r11, r7, r12);	 Catch:{ CameraAccessException -> 0x02c5 }
            r0.capture(r6, r4, r1);	 Catch:{ CameraAccessException -> 0x02c5 }
        L_0x02c4:
            goto L_0x02cf;
        L_0x02c5:
            r0 = move-exception;
            r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r9 = "Unable to initiate immediate capture";
            com.android.ex.camera2.portability.debug.Log.e(r6, r9, r0);	 Catch:{ Exception -> 0x0700 }
        L_0x02cf:
            goto L_0x06e1;
        L_0x02d1:
            r0 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r4 = android.hardware.camera2.CaptureRequest.JPEG_ORIENTATION;	 Catch:{ Exception -> 0x0700 }
            r5 = r2.arg1;	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.Integer.valueOf(r5);	 Catch:{ Exception -> 0x0700 }
            r0.set(r4, r5);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x02e0:
            r0 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r4 = android.hardware.camera2.CaptureRequest.JPEG_ORIENTATION;	 Catch:{ Exception -> 0x0700 }
            r5 = r2.arg2;	 Catch:{ Exception -> 0x0700 }
            if (r5 <= 0) goto L_0x02f5;
        L_0x02e8:
            r5 = r1.mCameraProxy;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.getCharacteristics();	 Catch:{ Exception -> 0x0700 }
            r6 = r2.arg1;	 Catch:{ Exception -> 0x0700 }
            r9 = r5.getJpegOrientation(r6);	 Catch:{ Exception -> 0x0700 }
        L_0x02f5:
            r5 = java.lang.Integer.valueOf(r9);	 Catch:{ Exception -> 0x0700 }
            r0.set(r4, r5);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x02fe:
            r0 = r1.mCancelAfPending;	 Catch:{ Exception -> 0x0700 }
            r0 = r0 - r10;
            r1.mCancelAfPending = r0;	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0305:
            r0 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r0 = (com.android.ex.camera2.portability.CameraAgent.CameraAFMoveCallback) r0;	 Catch:{ Exception -> 0x0700 }
            r1.mPassiveAfCallback = r0;	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x030d:
            r5 = r1.mCancelAfPending;	 Catch:{ Exception -> 0x0700 }
            r5 = r5 + r10;
            r1.mCancelAfPending = r5;	 Catch:{ Exception -> 0x0700 }
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.getState();	 Catch:{ Exception -> 0x0700 }
            if (r5 >= r0) goto L_0x0329;
        L_0x031e:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r4 = "Ignoring attempt to release focus lock without preview";
            com.android.ex.camera2.portability.debug.Log.w(r0, r4);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0329:
            r1.changeState(r0);	 Catch:{ Exception -> 0x0700 }
            r0 = new com.android.ex.camera2.utils.Camera2RequestSettingsSet;	 Catch:{ Exception -> 0x0700 }
            r5 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r0.<init>(r5);	 Catch:{ Exception -> 0x0700 }
            r5 = r0;
            r0 = android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER;	 Catch:{ Exception -> 0x0700 }
            r6 = java.lang.Integer.valueOf(r7);	 Catch:{ Exception -> 0x0700 }
            r5.set(r0, r6);	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x0352 }
            r6 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x0352 }
            r11 = r1.mPreviewTemplate;	 Catch:{ CameraAccessException -> 0x0352 }
            r12 = new android.view.Surface[r10];	 Catch:{ CameraAccessException -> 0x0352 }
            r13 = r1.mPreviewSurface;	 Catch:{ CameraAccessException -> 0x0352 }
            r12[r9] = r13;	 Catch:{ CameraAccessException -> 0x0352 }
            r6 = r5.createRequest(r6, r11, r12);	 Catch:{ CameraAccessException -> 0x0352 }
            r0.capture(r6, r8, r1);	 Catch:{ CameraAccessException -> 0x0352 }
            goto L_0x06e1;
        L_0x0352:
            r0 = move-exception;
            r6 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r9 = "Unable to cancel autofocus";
            com.android.ex.camera2.portability.debug.Log.e(r6, r9, r0);	 Catch:{ Exception -> 0x0700 }
            r1.changeState(r4);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0361:
            r0 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r0 = (com.android.ex.camera2.portability.CameraAgent.CameraAFCallback) r0;	 Catch:{ Exception -> 0x0700 }
            r1.performAutoFocus(r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x036a:
            r0 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r0 = (com.android.ex.camera2.portability.AndroidCamera2Settings) r0;	 Catch:{ Exception -> 0x0700 }
            r1.applyToRequest(r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0373:
            r4 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r4 = (java.lang.Object[]) r4;	 Catch:{ Exception -> 0x0700 }
            r11 = r4[r9];	 Catch:{ Exception -> 0x0700 }
            r11 = (com.android.ex.camera2.portability.CameraAgent.CameraPreviewDataCallbackWithHandler) r11;	 Catch:{ Exception -> 0x0700 }
            r12 = r4[r10];	 Catch:{ Exception -> 0x0700 }
            r12 = (android.media.ImageReader.OnImageAvailableListener) r12;	 Catch:{ Exception -> 0x0700 }
            r1.mPreviewDataCallbackWithBuffer = r11;	 Catch:{ Exception -> 0x0700 }
            r1.mPreviewDataAvailableListener = r12;	 Catch:{ Exception -> 0x0700 }
            r13 = r1.mPreviewDataCallbackWithBuffer;	 Catch:{ Exception -> 0x0700 }
            if (r13 != 0) goto L_0x0389;
        L_0x0387:
            goto L_0x06e1;
        L_0x0389:
            r13 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r13 = r13.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r13 = r13.getState();	 Catch:{ Exception -> 0x0700 }
            if (r13 < r0) goto L_0x06e1;
        L_0x0395:
            r0 = r1.mPreviewReader;	 Catch:{ Exception -> 0x0700 }
            r13 = r11.handler;	 Catch:{ Exception -> 0x0700 }
            r0.setOnImageAvailableListener(r12, r13);	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mCameraIndex;	 Catch:{ CameraAccessException -> 0x03d8 }
            if (r0 != r5) goto L_0x03b8;
        L_0x03a0:
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x03d8 }
            r5 = r1.mPersistentSettings;	 Catch:{ CameraAccessException -> 0x03d8 }
            r13 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x03d8 }
            r14 = r1.mPreviewTemplate;	 Catch:{ CameraAccessException -> 0x03d8 }
            r15 = new android.view.Surface[r10];	 Catch:{ CameraAccessException -> 0x03d8 }
            r8 = r1.mPreviewSurface;	 Catch:{ CameraAccessException -> 0x03d8 }
            r15[r9] = r8;	 Catch:{ CameraAccessException -> 0x03d8 }
            r5 = r5.createRequest(r13, r14, r15);	 Catch:{ CameraAccessException -> 0x03d8 }
            r8 = r1.mCameraResultStateCallback;	 Catch:{ CameraAccessException -> 0x03d8 }
            r0.setRepeatingRequest(r5, r8, r1);	 Catch:{ CameraAccessException -> 0x03d8 }
            goto L_0x03e5;
        L_0x03b8:
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x03d8 }
            r5 = r1.mPersistentSettings;	 Catch:{ CameraAccessException -> 0x03d8 }
            r8 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x03d8 }
            r13 = r1.mPreviewTemplate;	 Catch:{ CameraAccessException -> 0x03d8 }
            r14 = new android.view.Surface[r7];	 Catch:{ CameraAccessException -> 0x03d8 }
            r15 = r1.mPreviewSurface;	 Catch:{ CameraAccessException -> 0x03d8 }
            r14[r9] = r15;	 Catch:{ CameraAccessException -> 0x03d8 }
            r9 = r1.mPreviewReader;	 Catch:{ CameraAccessException -> 0x03d8 }
            r9 = r9.getSurface();	 Catch:{ CameraAccessException -> 0x03d8 }
            r14[r10] = r9;	 Catch:{ CameraAccessException -> 0x03d8 }
            r5 = r5.createRequest(r8, r13, r14);	 Catch:{ CameraAccessException -> 0x03d8 }
            r8 = r1.mCameraResultStateCallback;	 Catch:{ CameraAccessException -> 0x03d8 }
            r0.setRepeatingRequest(r5, r8, r1);	 Catch:{ CameraAccessException -> 0x03d8 }
            goto L_0x03e5;
        L_0x03d8:
            r0 = move-exception;
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r8 = "Unable to start preview";
            com.android.ex.camera2.portability.debug.Log.w(r5, r8, r0);	 Catch:{ Exception -> 0x0700 }
            r1.changeState(r6);	 Catch:{ Exception -> 0x0700 }
        L_0x03e5:
            goto L_0x06e1;
        L_0x03e7:
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r4 = r4.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r4 = r4.getState();	 Catch:{ Exception -> 0x0700 }
            if (r4 >= r0) goto L_0x03fe;
        L_0x03f3:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r4 = "Refusing to stop preview at inappropriate time";
            com.android.ex.camera2.portability.debug.Log.w(r0, r4);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x03fe:
            r0 = r1.mSession;	 Catch:{ Exception -> 0x0700 }
            r0.stopRepeating();	 Catch:{ Exception -> 0x0700 }
            r1.changeState(r6);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0408:
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r4 = r4.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r4 = r4.getState();	 Catch:{ Exception -> 0x0700 }
            if (r4 == r6) goto L_0x043c;
        L_0x0414:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0700 }
            r4.<init>();	 Catch:{ Exception -> 0x0700 }
            r5 = "Refusing to start preview at inappropriate time";
            r4.append(r5);	 Catch:{ Exception -> 0x0700 }
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.getState();	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.Integer.toBinaryString(r5);	 Catch:{ Exception -> 0x0700 }
            r4.append(r5);	 Catch:{ Exception -> 0x0700 }
            r4 = r4.toString();	 Catch:{ Exception -> 0x0700 }
            com.android.ex.camera2.portability.debug.Log.w(r0, r4);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x043c:
            r4 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r4 = (com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback) r4;	 Catch:{ Exception -> 0x0700 }
            r1.mOneshotPreviewingCallback = r4;	 Catch:{ Exception -> 0x0700 }
            r1.changeState(r0);	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mPreviewDataCallbackWithBuffer;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x045e;
        L_0x0449:
            r0 = "3";
            r4 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.equals(r4);	 Catch:{ Exception -> 0x0700 }
            if (r0 != 0) goto L_0x045e;
        L_0x0453:
            r0 = r1.mPreviewReader;	 Catch:{ Exception -> 0x0700 }
            r4 = r1.mPreviewDataAvailableListener;	 Catch:{ Exception -> 0x0700 }
            r5 = r1.mPreviewDataCallbackWithBuffer;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.handler;	 Catch:{ Exception -> 0x0700 }
            r0.setOnImageAvailableListener(r4, r5);	 Catch:{ Exception -> 0x0700 }
        L_0x045e:
            r0 = "3";
            r4 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.equals(r4);	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x0488;
        L_0x0468:
            r0 = com.android.ex.camera2.portability.CameraAgent.mLiveBolkenRearLevel;	 Catch:{ Exception -> 0x0700 }
            if (r0 >= 0) goto L_0x046f;
        L_0x046c:
            r0 = 4;
            com.android.ex.camera2.portability.CameraAgent.mLiveBolkenRearLevel = r0;	 Catch:{ Exception -> 0x0700 }
        L_0x046f:
            r0 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.bokeh_blur_level;	 Catch:{ Exception -> 0x0700 }
            r5 = com.android.ex.camera2.portability.CameraAgent.mLiveBolkenRearLevel;	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.Integer.valueOf(r5);	 Catch:{ Exception -> 0x0700 }
            r0.set(r4, r5);	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.bokeh_enable;	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.Boolean.valueOf(r10);	 Catch:{ Exception -> 0x0700 }
            r0.set(r4, r5);	 Catch:{ Exception -> 0x0700 }
            goto L_0x049e;
        L_0x0488:
            r0 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.bokeh_blur_level;	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.Integer.valueOf(r9);	 Catch:{ Exception -> 0x0700 }
            r0.set(r4, r5);	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mPersistentSettings;	 Catch:{ Exception -> 0x0700 }
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.bokeh_enable;	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.Boolean.valueOf(r9);	 Catch:{ Exception -> 0x0700 }
            r0.set(r4, r5);	 Catch:{ Exception -> 0x0700 }
        L_0x049e:
            r0 = r1.mPreviewDataCallbackWithBuffer;	 Catch:{ CameraAccessException -> 0x04e5 }
            if (r0 == 0) goto L_0x04cc;
        L_0x04a2:
            r0 = "3";
            r4 = r1.mCameraId;	 Catch:{ CameraAccessException -> 0x04e5 }
            r0 = r0.equals(r4);	 Catch:{ CameraAccessException -> 0x04e5 }
            if (r0 != 0) goto L_0x04cc;
        L_0x04ac:
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x04e5 }
            r4 = r1.mPersistentSettings;	 Catch:{ CameraAccessException -> 0x04e5 }
            r5 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x04e5 }
            r8 = r1.mPreviewTemplate;	 Catch:{ CameraAccessException -> 0x04e5 }
            r11 = new android.view.Surface[r7];	 Catch:{ CameraAccessException -> 0x04e5 }
            r12 = r1.mPreviewSurface;	 Catch:{ CameraAccessException -> 0x04e5 }
            r11[r9] = r12;	 Catch:{ CameraAccessException -> 0x04e5 }
            r9 = r1.mPreviewReader;	 Catch:{ CameraAccessException -> 0x04e5 }
            r9 = r9.getSurface();	 Catch:{ CameraAccessException -> 0x04e5 }
            r11[r10] = r9;	 Catch:{ CameraAccessException -> 0x04e5 }
            r4 = r4.createRequest(r5, r8, r11);	 Catch:{ CameraAccessException -> 0x04e5 }
            r5 = r1.mCameraResultStateCallback;	 Catch:{ CameraAccessException -> 0x04e5 }
            r0.setRepeatingRequest(r4, r5, r1);	 Catch:{ CameraAccessException -> 0x04e5 }
            goto L_0x04e3;
        L_0x04cc:
            r0 = r1.mSession;	 Catch:{ CameraAccessException -> 0x04e5 }
            r4 = r1.mPersistentSettings;	 Catch:{ CameraAccessException -> 0x04e5 }
            r5 = r1.mCamera;	 Catch:{ CameraAccessException -> 0x04e5 }
            r8 = r1.mPreviewTemplate;	 Catch:{ CameraAccessException -> 0x04e5 }
            r11 = new android.view.Surface[r10];	 Catch:{ CameraAccessException -> 0x04e5 }
            r12 = r1.mPreviewSurface;	 Catch:{ CameraAccessException -> 0x04e5 }
            r11[r9] = r12;	 Catch:{ CameraAccessException -> 0x04e5 }
            r4 = r4.createRequest(r5, r8, r11);	 Catch:{ CameraAccessException -> 0x04e5 }
            r5 = r1.mCameraResultStateCallback;	 Catch:{ CameraAccessException -> 0x04e5 }
            r0.setRepeatingRequest(r4, r5, r1);	 Catch:{ CameraAccessException -> 0x04e5 }
        L_0x04e3:
            goto L_0x06e1;
        L_0x04e5:
            r0 = move-exception;
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r5 = "Unable to start preview";
            com.android.ex.camera2.portability.debug.Log.w(r4, r5, r0);	 Catch:{ Exception -> 0x0700 }
            r1.changeState(r6);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x04f4:
            r0 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r0 = (android.graphics.SurfaceTexture) r0;	 Catch:{ Exception -> 0x0700 }
            r1.setPreviewTexture(r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x04fd:
            r0 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r0 = (com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback) r0;	 Catch:{ Exception -> 0x0700 }
            r4 = r2.arg1;	 Catch:{ Exception -> 0x0700 }
            r1.mOpenCallback = r0;	 Catch:{ Exception -> 0x0700 }
            r1.mCameraIndex = r4;	 Catch:{ Exception -> 0x0700 }
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.mCameraDevices;	 Catch:{ Exception -> 0x0700 }
            r6 = r1.mCameraIndex;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.get(r6);	 Catch:{ Exception -> 0x0700 }
            r5 = (java.lang.String) r5;	 Catch:{ Exception -> 0x0700 }
            r1.mCameraId = r5;	 Catch:{ Exception -> 0x0700 }
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r6 = "Opening camera index %d (id %s) with camera2 API";
            r8 = new java.lang.Object[r7];	 Catch:{ Exception -> 0x0700 }
            r11 = java.lang.Integer.valueOf(r4);	 Catch:{ Exception -> 0x0700 }
            r8[r9] = r11;	 Catch:{ Exception -> 0x0700 }
            r9 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r8[r10] = r9;	 Catch:{ Exception -> 0x0700 }
            r6 = java.lang.String.format(r6, r8);	 Catch:{ Exception -> 0x0700 }
            com.android.ex.camera2.portability.debug.Log.i(r5, r6);	 Catch:{ Exception -> 0x0700 }
            r5 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            if (r5 != 0) goto L_0x053d;
        L_0x0534:
            r5 = r1.mOpenCallback;	 Catch:{ Exception -> 0x0700 }
            r6 = r2.arg1;	 Catch:{ Exception -> 0x0700 }
            r5.onCameraDisabled(r6);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x053d:
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.mCameraManager;	 Catch:{ Exception -> 0x0700 }
            r6 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.getCameraCharacteristics(r6);	 Catch:{ Exception -> 0x0700 }
            r6 = android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.get(r6);	 Catch:{ Exception -> 0x0700 }
            r5 = (java.lang.Integer) r5;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.intValue();	 Catch:{ Exception -> 0x0700 }
            r1.mHardwareSupportLevel = r5;	 Catch:{ Exception -> 0x0700 }
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.getState();	 Catch:{ Exception -> 0x0700 }
            if (r5 <= r10) goto L_0x056d;
        L_0x0563:
            r5 = new com.android.ex.camera2.portability.AndroidCamera2AgentImpl$Camera2Handler$1;	 Catch:{ Exception -> 0x0700 }
            r5.<init>();	 Catch:{ Exception -> 0x0700 }
            r1.post(r5);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x056d:
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r5 = r5.mCameraManager;	 Catch:{ Exception -> 0x0700 }
            r6 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r8 = r1.mCameraDeviceStateCallback;	 Catch:{ Exception -> 0x0700 }
            r5.openCamera(r6, r8, r1);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x057c:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.getState();	 Catch:{ Exception -> 0x0700 }
            if (r0 != r10) goto L_0x0593;
        L_0x0588:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r4 = "Ignoring release at inappropriate time";
            com.android.ex.camera2.portability.debug.Log.w(r0, r4);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x0593:
            r0 = r1.mSession;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x059d;
        L_0x0597:
            r17.closePreviewSession();	 Catch:{ Exception -> 0x0700 }
            r4 = 0;
            r1.mSession = r4;	 Catch:{ Exception -> 0x0700 }
        L_0x059d:
            r0 = r1.mCamera;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x05a9;
        L_0x05a1:
            r0 = r1.mCamera;	 Catch:{ Exception -> 0x0700 }
            r0.close();	 Catch:{ Exception -> 0x0700 }
            r4 = 0;
            r1.mCamera = r4;	 Catch:{ Exception -> 0x0700 }
        L_0x05a9:
            r4 = 0;
            r1.mCameraProxy = r4;	 Catch:{ Exception -> 0x0700 }
            r1.mPersistentSettings = r4;	 Catch:{ Exception -> 0x0700 }
            r1.mActiveArray = r4;	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mPreviewSurface;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x05bc;
        L_0x05b4:
            r0 = r1.mPreviewSurface;	 Catch:{ Exception -> 0x0700 }
            r0.release();	 Catch:{ Exception -> 0x0700 }
            r4 = 0;
            r1.mPreviewSurface = r4;	 Catch:{ Exception -> 0x0700 }
        L_0x05bc:
            r4 = 0;
            r1.mPreviewTexture = r4;	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mCaptureReader;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x05cb;
        L_0x05c3:
            r0 = r1.mCaptureReader;	 Catch:{ Exception -> 0x0700 }
            r0.close();	 Catch:{ Exception -> 0x0700 }
            r4 = 0;
            r1.mCaptureReader = r4;	 Catch:{ Exception -> 0x0700 }
        L_0x05cb:
            r0 = r1.mRawImageReader;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x05d7;
        L_0x05cf:
            r0 = r1.mRawImageReader;	 Catch:{ Exception -> 0x0700 }
            r0.close();	 Catch:{ Exception -> 0x0700 }
            r4 = 0;
            r1.mRawImageReader = r4;	 Catch:{ Exception -> 0x0700 }
        L_0x05d7:
            r0 = r1.mPreviewReader;	 Catch:{ Exception -> 0x0700 }
            if (r0 == 0) goto L_0x05e5;
        L_0x05db:
            r0 = r1.mPreviewReader;	 Catch:{ Exception -> 0x0700 }
            r0.close();	 Catch:{ Exception -> 0x0700 }
            r4 = 0;
            r1.mPreviewReader = r4;	 Catch:{ Exception -> 0x0700 }
            r1.mPreviewDataAvailableListener = r4;	 Catch:{ Exception -> 0x0700 }
        L_0x05e5:
            r4 = 0;
            r1.mPreviewSize = r4;	 Catch:{ Exception -> 0x0700 }
            r1.mPhotoSize = r4;	 Catch:{ Exception -> 0x0700 }
            r1.mIsSaveDng = r9;	 Catch:{ Exception -> 0x0700 }
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.mRawResultQueue;	 Catch:{ Exception -> 0x0700 }
            r0.clear();	 Catch:{ Exception -> 0x0700 }
            r1.mCameraIndex = r9;	 Catch:{ Exception -> 0x0700 }
            r4 = 0;
            r1.mCameraId = r4;	 Catch:{ Exception -> 0x0700 }
            r1.changeState(r10);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x05ff:
            r0 = r2.obj;	 Catch:{ Exception -> 0x0700 }
            r0 = (com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback) r0;	 Catch:{ Exception -> 0x0700 }
            r4 = r0;
            r0 = r2.arg1;	 Catch:{ Exception -> 0x0700 }
            r6 = r0;
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.mCameraState;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.getState();	 Catch:{ Exception -> 0x0700 }
            if (r0 <= r10) goto L_0x061d;
            r0 = r1.generateHistoryString(r6);	 Catch:{ Exception -> 0x0700 }
            r4.onDeviceOpenedAlready(r6, r0);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x061d:
            r1.mOpenCallback = r4;	 Catch:{ Exception -> 0x0700 }
            r1.mCameraIndex = r6;	 Catch:{ Exception -> 0x0700 }
            r0 = "masaisai";
            r8 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0700 }
            r8.<init>();	 Catch:{ Exception -> 0x0700 }
            r11 = "mCameraIndex = ";
            r8.append(r11);	 Catch:{ Exception -> 0x0700 }
            r11 = r1.mCameraIndex;	 Catch:{ Exception -> 0x0700 }
            r8.append(r11);	 Catch:{ Exception -> 0x0700 }
            r8 = r8.toString();	 Catch:{ Exception -> 0x0700 }
            android.util.Log.v(r0, r8);	 Catch:{ Exception -> 0x0700 }
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x065e }
            r0 = r0.mCameraDevices;	 Catch:{ Exception -> 0x065e }
            r8 = r1.mCameraIndex;	 Catch:{ Exception -> 0x065e }
            r0 = r0.get(r8);	 Catch:{ Exception -> 0x065e }
            r0 = (java.lang.String) r0;	 Catch:{ Exception -> 0x065e }
            r8 = "masaisai";
            r11 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x065e }
            r11.<init>();	 Catch:{ Exception -> 0x065e }
            r12 = "cameraId = ";
            r11.append(r12);	 Catch:{ Exception -> 0x065e }
            r11.append(r0);	 Catch:{ Exception -> 0x065e }
            r11 = r11.toString();	 Catch:{ Exception -> 0x065e }
            android.util.Log.v(r8, r11);	 Catch:{ Exception -> 0x065e }
            goto L_0x0662;
        L_0x065e:
            r0 = move-exception;
            r0.printStackTrace();	 Catch:{ Exception -> 0x0700 }
        L_0x0662:
            r0 = r1.mCameraIndex;	 Catch:{ Exception -> 0x0700 }
            if (r0 != r5) goto L_0x066b;
        L_0x0666:
            r0 = "3";
            r1.mCameraId = r0;	 Catch:{ Exception -> 0x0700 }
            goto L_0x067b;
        L_0x066b:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.mCameraDevices;	 Catch:{ Exception -> 0x0700 }
            r5 = r1.mCameraIndex;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.get(r5);	 Catch:{ Exception -> 0x0700 }
            r0 = (java.lang.String) r0;	 Catch:{ Exception -> 0x0700 }
            r1.mCameraId = r0;	 Catch:{ Exception -> 0x0700 }
        L_0x067b:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r5 = "Opening camera index %d (id %s) with camera2 API";
            r8 = new java.lang.Object[r7];	 Catch:{ Exception -> 0x0700 }
            r11 = java.lang.Integer.valueOf(r6);	 Catch:{ Exception -> 0x0700 }
            r8[r9] = r11;	 Catch:{ Exception -> 0x0700 }
            r11 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r8[r10] = r11;	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.String.format(r5, r8);	 Catch:{ Exception -> 0x0700 }
            com.android.ex.camera2.portability.debug.Log.i(r0, r5);	 Catch:{ Exception -> 0x0700 }
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ Exception -> 0x0700 }
            r5 = "Opening camera index %d (id %s) with camera2 API";
            r8 = new java.lang.Object[r7];	 Catch:{ Exception -> 0x0700 }
            r11 = java.lang.Integer.valueOf(r6);	 Catch:{ Exception -> 0x0700 }
            r8[r9] = r11;	 Catch:{ Exception -> 0x0700 }
            r9 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r8[r10] = r9;	 Catch:{ Exception -> 0x0700 }
            r5 = java.lang.String.format(r5, r8);	 Catch:{ Exception -> 0x0700 }
            com.android.ex.camera2.portability.debug.Log.i(r0, r5);	 Catch:{ Exception -> 0x0700 }
            r0 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            if (r0 != 0) goto L_0x06b9;
        L_0x06b1:
            r0 = r1.mOpenCallback;	 Catch:{ Exception -> 0x0700 }
            r5 = r2.arg1;	 Catch:{ Exception -> 0x0700 }
            r0.onCameraDisabled(r5);	 Catch:{ Exception -> 0x0700 }
            goto L_0x06e1;
        L_0x06b9:
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.mCameraManager;	 Catch:{ Exception -> 0x0700 }
            r5 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.getCameraCharacteristics(r5);	 Catch:{ Exception -> 0x0700 }
            r5 = android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.get(r5);	 Catch:{ Exception -> 0x0700 }
            r0 = (java.lang.Integer) r0;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.intValue();	 Catch:{ Exception -> 0x0700 }
            r1.mHardwareSupportLevel = r0;	 Catch:{ Exception -> 0x0700 }
            r0 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ Exception -> 0x0700 }
            r0 = r0.mCameraManager;	 Catch:{ Exception -> 0x0700 }
            r5 = r1.mCameraId;	 Catch:{ Exception -> 0x0700 }
            r8 = r1.mCameraDeviceStateCallback;	 Catch:{ Exception -> 0x0700 }
            r0.openCamera(r5, r8, r1);	 Catch:{ Exception -> 0x0700 }
        L_0x06e1:
            com.android.ex.camera2.portability.CameraAgent.WaitDoneBundle.unblockSyncWaiters(r18);
            goto L_0x0790;
        L_0x06e6:
            r4 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0700 }
            r4.<init>();	 Catch:{ Exception -> 0x0700 }
            r5 = "Unimplemented CameraProxy message=";
            r4.append(r5);	 Catch:{ Exception -> 0x0700 }
            r5 = r2.what;	 Catch:{ Exception -> 0x0700 }
            r4.append(r5);	 Catch:{ Exception -> 0x0700 }
            r4 = r4.toString();	 Catch:{ Exception -> 0x0700 }
            r0.<init>(r4);	 Catch:{ Exception -> 0x0700 }
            throw r0;	 Catch:{ Exception -> 0x0700 }
        L_0x06fd:
            r0 = move-exception;
            goto L_0x0791;
        L_0x0700:
            r0 = move-exception;
            if (r3 == r7) goto L_0x0719;
        L_0x0703:
            r4 = r1.mCamera;	 Catch:{ all -> 0x06fd }
            if (r4 == 0) goto L_0x0719;
        L_0x0707:
            r4 = r1.mCamera;	 Catch:{ all -> 0x06fd }
            r4.close();	 Catch:{ all -> 0x06fd }
            r4 = 0;
            r1.mCamera = r4;	 Catch:{ all -> 0x06fd }
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ all -> 0x06fd }
            r5 = "Open Camera Error";
            com.android.ex.camera2.portability.debug.Log.v(r4, r5, r0);	 Catch:{ all -> 0x06fd }
            goto L_0x0762;
        L_0x0719:
            r4 = r1.mCamera;	 Catch:{ all -> 0x06fd }
            if (r4 != 0) goto L_0x0762;
        L_0x071d:
            if (r3 != r10) goto L_0x073f;
        L_0x071f:
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ all -> 0x06fd }
            r5 = "1006Agent2";
            r6 = new java.lang.Exception;	 Catch:{ all -> 0x06fd }
            r6.<init>();	 Catch:{ all -> 0x06fd }
            com.android.ex.camera2.portability.debug.Log.e(r4, r5, r6);	 Catch:{ all -> 0x06fd }
            r4 = r1.mOpenCallback;	 Catch:{ all -> 0x06fd }
            if (r4 == 0) goto L_0x075e;
        L_0x0731:
            r4 = r1.mOpenCallback;	 Catch:{ all -> 0x06fd }
            r5 = r1.mCameraIndex;	 Catch:{ all -> 0x06fd }
            r6 = r1.mCameraIndex;	 Catch:{ all -> 0x06fd }
            r6 = r1.generateHistoryString(r6);	 Catch:{ all -> 0x06fd }
            r4.onDeviceOpenFailure(r5, r6);	 Catch:{ all -> 0x06fd }
            goto L_0x075e;
        L_0x073f:
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.TAG;	 Catch:{ all -> 0x06fd }
            r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x06fd }
            r5.<init>();	 Catch:{ all -> 0x06fd }
            r6 = "Cannot handle message ";
            r5.append(r6);	 Catch:{ all -> 0x06fd }
            r6 = r2.what;	 Catch:{ all -> 0x06fd }
            r5.append(r6);	 Catch:{ all -> 0x06fd }
            r6 = ", mCamera is null";
            r5.append(r6);	 Catch:{ all -> 0x06fd }
            r5 = r5.toString();	 Catch:{ all -> 0x06fd }
            com.android.ex.camera2.portability.debug.Log.w(r4, r5);	 Catch:{ all -> 0x06fd }
        L_0x075e:
            com.android.ex.camera2.portability.CameraAgent.WaitDoneBundle.unblockSyncWaiters(r18);
            return;
        L_0x0762:
            r4 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ all -> 0x06fd }
            r4 = r4.mExceptionHandler;	 Catch:{ all -> 0x06fd }
            if (r4 == 0) goto L_0x06e1;
        L_0x076a:
            r4 = r0 instanceof java.lang.RuntimeException;	 Catch:{ all -> 0x06fd }
            if (r4 == 0) goto L_0x06e1;
        L_0x076e:
            r4 = r1.mCameraId;	 Catch:{ all -> 0x06fd }
            r4 = java.lang.Integer.parseInt(r4);	 Catch:{ all -> 0x06fd }
            r4 = r1.generateHistoryString(r4);	 Catch:{ all -> 0x06fd }
            r5 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ all -> 0x06fd }
            r5 = r5.mExceptionHandler;	 Catch:{ all -> 0x06fd }
            r6 = r0;
            r6 = (java.lang.RuntimeException) r6;	 Catch:{ all -> 0x06fd }
            r7 = com.android.ex.camera2.portability.AndroidCamera2AgentImpl.this;	 Catch:{ all -> 0x06fd }
            r7 = r7.mCameraState;	 Catch:{ all -> 0x06fd }
            r7 = r7.getState();	 Catch:{ all -> 0x06fd }
            r5.onCameraException(r6, r4, r3, r7);	 Catch:{ all -> 0x06fd }
            goto L_0x06e1;
        L_0x0790:
            return;
        L_0x0791:
            com.android.ex.camera2.portability.CameraAgent.WaitDoneBundle.unblockSyncWaiters(r18);
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.portability.AndroidCamera2AgentImpl$Camera2Handler.handleMessage(android.os.Message):void");
        }

        public static /* synthetic */ void lambda$handleMessage$0(Camera2Handler camera2Handler, ImageReader reader) {
            Image image;
            try {
                image = reader.acquireLatestImage();
                Tag access$000 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("on raw image available, format:");
                stringBuilder.append(image.getFormat());
                Log.d(access$000, stringBuilder.toString());
                if (image.getFormat() == 32) {
                    File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("RAW_");
                    stringBuilder2.append(AndroidCamera2AgentImpl.generateTimestamp());
                    stringBuilder2.append(".dng");
                    File rawFile = new File(externalStoragePublicDirectory, stringBuilder2.toString());
                    if (AndroidCamera2AgentImpl.this.mRawResultQueue.size() > 0) {
                        AndroidCamera2AgentImpl.writeDngBytesAndClose(image, (TotalCaptureResult) AndroidCamera2AgentImpl.this.mRawResultQueue.removeFirst(), AndroidCamera2AgentImpl.this.mCameraManager.getCameraCharacteristics(camera2Handler.mCamera.getId()), rawFile);
                    } else {
                        Log.e(AndroidCamera2AgentImpl.TAG, "Can't get TotalCaptureResult for this raw image.");
                    }
                }
                if (image != null) {
                    image.close();
                }
            } catch (CameraAccessException e) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Can't get TotalCaptureResult for this raw image.");
            } catch (Throwable th) {
                r1.addSuppressed(th);
            }
        }

        public void setEvInfos(int index) {
            boolean prior_et;
            int gain;
            long tv;
            int ss = ((Integer) AndroidCamera2AgentImpl.this.mLastPreviewResult.get(TotalCaptureResult.SENSOR_SENSITIVITY)).intValue();
            Long et = (Long) AndroidCamera2AgentImpl.this.mLastPreviewResult.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME);
            float ev = ((Float) AndroidCamera2AgentImpl.this.mHdrEv.get(index)).floatValue();
            if (ev < 0.0f) {
                prior_et = true;
            } else {
                prior_et = false;
            }
            Tag access$000 = AndroidCamera2AgentImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("prior_et = ");
            stringBuilder.append(prior_et);
            stringBuilder.append(" ss = ");
            stringBuilder.append(ss);
            stringBuilder.append(" et = ");
            stringBuilder.append(et);
            stringBuilder.append(" ev = ");
            stringBuilder.append(ev);
            Log.d(access$000, stringBuilder.toString());
            if (prior_et) {
                double desired_gain = ((double) ss) * Math.exp((double) (AndroidCamera2AgentImpl.this.LOG_2 * ev));
                gain = ((Integer) AndroidCamera2AgentImpl.this.mIsoRange.clamp(Integer.valueOf((int) Math.rint(desired_gain)))).intValue();
                tv = ((Long) AndroidCamera2AgentImpl.this.mEtRange.clamp(Long.valueOf((long) Math.rint((((double) et.longValue()) * desired_gain) / ((double) gain))))).longValue();
            } else {
                double desired_tv = ((double) et.longValue()) * Math.exp((double) (AndroidCamera2AgentImpl.this.LOG_2 * ev));
                tv = ((Long) AndroidCamera2AgentImpl.this.mEtRange.clamp(Long.valueOf((long) Math.rint(desired_tv)))).longValue();
                gain = ((Integer) AndroidCamera2AgentImpl.this.mIsoRange.clamp(Integer.valueOf((int) Math.rint((((double) ss) * desired_tv) / ((double) tv))))).intValue();
            }
            AndroidCamera2AgentImpl.this.mEvInfos.add(new EvInfo(tv, gain));
            Tag access$0002 = AndroidCamera2AgentImpl.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setCaptureRequest et = ");
            stringBuilder2.append(tv);
            stringBuilder2.append(" iso = ");
            stringBuilder2.append(gain);
            Log.d(access$0002, stringBuilder2.toString());
        }

        public void setCaptureRequest(Builder builder, int index) {
            boolean prior_et;
            int gain;
            long tv;
            Builder builder2 = builder;
            int ss = ((Integer) AndroidCamera2AgentImpl.this.mLastPreviewResult.get(TotalCaptureResult.SENSOR_SENSITIVITY)).intValue();
            Long et = (Long) AndroidCamera2AgentImpl.this.mLastPreviewResult.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME);
            float ev = ((Float) AndroidCamera2AgentImpl.this.mHdrEv.get(index)).floatValue();
            if (ev < 0.0f) {
                prior_et = true;
            } else {
                prior_et = false;
            }
            Tag access$000 = AndroidCamera2AgentImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("prior_et = ");
            stringBuilder.append(prior_et);
            stringBuilder.append(" ss = ");
            stringBuilder.append(ss);
            stringBuilder.append(" et = ");
            stringBuilder.append(et);
            stringBuilder.append(" ev = ");
            stringBuilder.append(ev);
            Log.d(access$000, stringBuilder.toString());
            if (prior_et) {
                double desired_gain = ((double) ss) * Math.exp((double) (AndroidCamera2AgentImpl.this.LOG_2 * ev));
                gain = ((Integer) AndroidCamera2AgentImpl.this.mIsoRange.clamp(Integer.valueOf((int) Math.rint(desired_gain)))).intValue();
                tv = ((Long) AndroidCamera2AgentImpl.this.mEtRange.clamp(Long.valueOf((long) Math.rint((((double) et.longValue()) * desired_gain) / ((double) gain))))).longValue();
            } else {
                double desired_tv = ((double) et.longValue()) * Math.exp((double) (AndroidCamera2AgentImpl.this.LOG_2 * ev));
                tv = ((Long) AndroidCamera2AgentImpl.this.mEtRange.clamp(Long.valueOf((long) Math.rint(desired_tv)))).longValue();
                gain = ((Integer) AndroidCamera2AgentImpl.this.mIsoRange.clamp(Integer.valueOf((int) Math.rint((((double) ss) * desired_tv) / ((double) tv))))).intValue();
            }
            builder2.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(0));
            builder2.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(gain));
            builder2.set(CaptureRequest.SENSOR_EXPOSURE_TIME, Long.valueOf(tv));
            AndroidCamera2AgentImpl.this.mEvInfos.add(new EvInfo(tv, gain));
            Tag access$0002 = AndroidCamera2AgentImpl.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setCaptureRequest et = ");
            stringBuilder2.append(tv);
            stringBuilder2.append(" iso = ");
            stringBuilder2.append(gain);
            Log.d(access$0002, stringBuilder2.toString());
        }

        private void addBaseParameterToRequest(Builder builder) {
            builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, Integer.valueOf(0));
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, Integer.valueOf(0));
            builder.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, Integer.valueOf(0));
            builder.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(true));
            builder.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(true));
            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, Integer.valueOf(0));
            builder.set(CaptureRequest.CONTROL_AWB_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.FLASH_MODE, Integer.valueOf(0));
            builder.set(CaptureRequest.EDGE_MODE, Integer.valueOf(0));
            builder.set(CaptureRequest.NOISE_REDUCTION_MODE, Integer.valueOf(1));
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, null);
            builder.set(CaptureRequest.SCALER_CROP_REGION, (Rect) this.mPersistentSettings.get(CaptureRequest.SCALER_CROP_REGION));
        }

        public CameraSettings buildSettings(AndroidCamera2Capabilities caps) {
            try {
                return new AndroidCamera2Settings(this.mCamera, this.mPreviewTemplate, this.mActiveArray, this.mPreviewSize, this.mPhotoSize);
            } catch (CameraAccessException e) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Unable to query camera device to build settings representation");
                return null;
            } catch (NullPointerException ex) {
                Tag access$000 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(ex);
                stringBuilder.append("");
                Log.e(access$000, stringBuilder.toString());
                return null;
            }
        }

        private void applyToRequest(AndroidCamera2Settings settings) {
            this.mPersistentSettings.union(settings.getRequestSettings());
            this.mPreviewSize = settings.getCurrentPreviewSize();
            this.mPhotoSize = settings.getCurrentPhotoSize();
            Tag access$000 = AndroidCamera2AgentImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mPhotoSize size: ");
            stringBuilder.append(this.mPhotoSize);
            Log.d(access$000, stringBuilder.toString());
            this.mIsSaveDng = settings.isSaveDngEnabled();
            AndroidCamera2AgentImpl.this.mMotionPictureHelper = MotionPictureHelper.getHelper();
            this.mShouldSaveDng = settings.shouldSaveDng();
            access$000 = AndroidCamera2AgentImpl.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mShouldSaveDng = ");
            stringBuilder.append(this.mShouldSaveDng);
            Log.d(access$000, stringBuilder.toString());
            this.mPersistentSettings.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, Integer.valueOf(2));
            if (AndroidCamera2AgentImpl.this.mCameraState.getState() >= 16) {
                try {
                    if (!GpsMeasureMode.MODE_3_DIMENSIONAL.equals(this.mCameraId)) {
                        if (AndroidCamera2AgentImpl.this.currentModuleId != 0) {
                            this.mSession.setRepeatingRequest(this.mPersistentSettings.createRequest(this.mCamera, this.mPreviewTemplate, this.mPreviewSurface, this.mPreviewReader.getSurface()), this.mCameraResultStateCallback, this);
                            return;
                        }
                    }
                    this.mSession.setRepeatingRequest(this.mPersistentSettings.createRequest(this.mCamera, this.mPreviewTemplate, this.mPreviewSurface), this.mCameraResultStateCallback, this);
                } catch (CameraAccessException ex) {
                    Log.e(AndroidCamera2AgentImpl.TAG, "Failed to apply updated request settings", ex);
                }
            } else if (AndroidCamera2AgentImpl.this.mCameraState.getState() < 8) {
                changeState(4);
            }
        }

        private void performAutoFocus(final CameraAFCallback callback) {
            if (this.mCancelAfPending > 0) {
                Tag access$000 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage - Ignored AUTO_FOCUS because there was ");
                stringBuilder.append(this.mCancelAfPending);
                stringBuilder.append(" pending CANCEL_AUTO_FOCUS messages");
                Log.v(access$000, stringBuilder.toString());
            } else if (AndroidCamera2AgentImpl.this.mCameraState.getState() < 16) {
                Log.w(AndroidCamera2AgentImpl.TAG, "Ignoring attempt to autofocus without preview");
            } else {
                CaptureCallback deferredCallbackSetter = new CaptureCallback() {
                    private boolean mAlreadyDispatched = false;

                    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult result) {
                        checkAfState(result);
                    }

                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        checkAfState(result);
                    }

                    private void checkAfState(CaptureResult result) {
                        if (result.get(CaptureResult.CONTROL_AF_STATE) != null && !this.mAlreadyDispatched) {
                            this.mAlreadyDispatched = true;
                            Camera2Handler.this.mOneshotAfCallback = callback;
                            Camera2Handler.this.mCameraResultStateCallback.monitorControlStates(result);
                        }
                    }

                    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                        Tag access$000 = AndroidCamera2AgentImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Focusing failed with reason ");
                        stringBuilder.append(failure.getReason());
                        Log.e(access$000, stringBuilder.toString());
                        callback.onAutoFocus(false, Camera2Handler.this.mCameraProxy);
                    }
                };
                changeState(32);
                Camera2RequestSettingsSet trigger = new Camera2RequestSettingsSet(this.mPersistentSettings);
                trigger.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(1));
                try {
                    this.mSession.capture(trigger.createRequest(this.mCamera, this.mPreviewTemplate, this.mPreviewSurface), deferredCallbackSetter, this);
                } catch (CameraAccessException ex) {
                    Log.e(AndroidCamera2AgentImpl.TAG, "Unable to lock autofocus", ex);
                    changeState(16);
                }
            }
        }

        private void initFaceParams() {
            CameraCharacteristics props = null;
            try {
                props = AndroidCamera2AgentImpl.this.mCameraManager.getCameraCharacteristics(this.mCameraId);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            int[] FD = (int[]) props.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
            int maxFD = ((Integer) props.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT)).intValue();
            if (FD.length > 0) {
                List<Integer> fdList = new ArrayList();
                for (int FaceD : FD) {
                    fdList.add(Integer.valueOf(FaceD));
                    Tag access$000 = AndroidCamera2AgentImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setUpCameraOutputs: FD type:");
                    stringBuilder.append(Integer.toString(FaceD));
                    Log.e(access$000, stringBuilder.toString());
                }
                Tag access$0002 = AndroidCamera2AgentImpl.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setUpCameraOutputs: FD count");
                stringBuilder2.append(Integer.toString(maxFD));
                Log.e(access$0002, stringBuilder2.toString());
                if (maxFD > 0) {
                    this.mFaceDetectSupported = true;
                    this.mFaceDetectMode = ((Integer) Collections.max(fdList)).intValue();
                }
            }
        }

        private void setPreviewBolken(int bolken) {
            if (AndroidCamera2AgentImpl.this.mmCharacteristics.isFacingFront() && AndroidCamera2AgentImpl.this.currentModuleId == 0 && bolken >= 0 && CustomUtil.getInstance().isPanther()) {
                CameraAgent.mLiveBolkenFrontLevel = bolken;
                try {
                    Class[] params = new Class[]{String.class, Class.class};
                    Object[] switchValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.switch", Integer.class};
                    Object[] levelValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.level", Integer.class};
                    Object[] widthValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.width", Integer.class};
                    Object[] heightValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.height", Integer.class};
                    Constructor cons = Class.forName("android.hardware.camera2.CaptureRequest$Key").getDeclaredConstructor(params);
                    cons.setAccessible(true);
                    Object switchObj = cons.newInstance(switchValues);
                    Object levelObj = cons.newInstance(levelValues);
                    Object widthObj = cons.newInstance(widthValues);
                    Object heightObj = cons.newInstance(heightValues);
                    this.mPersistentSettings.set((Key) switchObj, Integer.valueOf(1));
                    this.mPersistentSettings.set((Key) levelObj, Integer.valueOf(CameraAgent.mLiveBolkenFrontLevel));
                    this.mPersistentSettings.set((Key) widthObj, Integer.valueOf(this.mPreviewSize.width()));
                    this.mPersistentSettings.set((Key) heightObj, Integer.valueOf(this.mPreviewSize.height()));
                    this.mPersistentSettings.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(AndroidCamera2AgentImpl.this.mcurrentOrientation));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    this.mSession.setRepeatingRequest(this.mPersistentSettings.createRequest(this.mCamera, this.mPreviewTemplate, this.mPreviewSurface, this.mPreviewReader.getSurface()), this.mCameraResultStateCallback, this.mBackgroundHandler);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            } else if (AndroidCamera2AgentImpl.this.mmCharacteristics.isFacingFront() && bolken < 0 && CustomUtil.getInstance().isPanther()) {
                try {
                    Object[] switchValues2 = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.switch", Integer.class};
                    Object[] levelValues2 = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.level", Integer.class};
                    Constructor cons2 = Class.forName("android.hardware.camera2.CaptureRequest$Key").getDeclaredConstructor(new Class[]{String.class, Class.class});
                    cons2.setAccessible(true);
                    Object switchObj2 = cons2.newInstance(switchValues2);
                    Object levelObj2 = cons2.newInstance(levelValues2);
                    this.mPersistentSettings.set((Key) switchObj2, Integer.valueOf(0));
                    this.mPersistentSettings.set((Key) levelObj2, Integer.valueOf(0));
                } catch (Exception e22) {
                    e22.printStackTrace();
                }
                try {
                    this.mSession.setRepeatingRequest(this.mPersistentSettings.createRequest(this.mCamera, this.mPreviewTemplate, this.mPreviewSurface, this.mPreviewReader.getSurface()), this.mCameraResultStateCallback, this.mBackgroundHandler);
                } catch (Exception e222) {
                    e222.printStackTrace();
                }
            } else if (GpsMeasureMode.MODE_3_DIMENSIONAL.equals(this.mCameraId) && !AndroidCamera2AgentImpl.this.mmCharacteristics.isFacingFront() && CustomUtil.getInstance().isPanther()) {
                if (bolken >= 0) {
                    CameraAgent.mLiveBolkenRearLevel = bolken;
                }
                try {
                    this.mPersistentSettings.set(AndroidCamera2AgentImpl.bokeh_blur_level, Integer.valueOf(bolken));
                    this.mPersistentSettings.set(AndroidCamera2AgentImpl.bokeh_enable, Boolean.valueOf(true));
                } catch (Exception e2222) {
                    e2222.printStackTrace();
                }
                try {
                    this.mSession.setRepeatingRequest(this.mPersistentSettings.createRequest(this.mCamera, this.mPreviewTemplate, this.mPreviewSurface), this.mCameraResultStateCallback, this.mBackgroundHandler);
                } catch (Exception e22222) {
                    e22222.printStackTrace();
                }
            }
        }

        private void setPreviewTexture(SurfaceTexture surfaceTexture) {
            if (AndroidCamera2AgentImpl.this.mCameraState.getState() < 4) {
                Log.w(AndroidCamera2AgentImpl.TAG, "Ignoring texture setting at inappropriate time");
                return;
            }
            Tag access$000 = AndroidCamera2AgentImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mIsDepthOn = ");
            stringBuilder.append(AndroidCamera2AgentImpl.this.mIsDepthOn);
            stringBuilder.append(" mLastDepthOn = ");
            stringBuilder.append(AndroidCamera2AgentImpl.this.mLastDepthOn);
            Log.d(access$000, stringBuilder.toString());
            access$000 = AndroidCamera2AgentImpl.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("currentModuleId = ");
            stringBuilder.append(AndroidCamera2AgentImpl.this.currentModuleId);
            stringBuilder.append(" mLastModuleId = ");
            stringBuilder.append(AndroidCamera2AgentImpl.this.mLastModuleId);
            Log.d(access$000, stringBuilder.toString());
            access$000 = AndroidCamera2AgentImpl.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mIsAdjustEt = ");
            stringBuilder.append(AndroidCamera2AgentImpl.this.mIsAdjustEt);
            Log.d(access$000, stringBuilder.toString());
            if (surfaceTexture == this.mPreviewTexture && AndroidCamera2AgentImpl.this.mIsDepthOn == AndroidCamera2AgentImpl.this.mLastDepthOn && AndroidCamera2AgentImpl.this.currentModuleId == AndroidCamera2AgentImpl.this.mLastModuleId && !AndroidCamera2AgentImpl.this.mIsAdjustEt) {
                Log.i(AndroidCamera2AgentImpl.TAG, "Optimizing out redundant preview texture setting");
            } else if (surfaceTexture != null) {
                AndroidCamera2AgentImpl.this.mLastDepthOn = AndroidCamera2AgentImpl.this.mIsDepthOn;
                AndroidCamera2AgentImpl.this.mLastModuleId = AndroidCamera2AgentImpl.this.currentModuleId;
                if (this.mSession != null) {
                    closePreviewSession();
                }
                this.mPreviewTexture = surfaceTexture;
                if (AndroidCamera2AgentImpl.this.currentModuleId != 0) {
                    surfaceTexture.setDefaultBufferSize(this.mPreviewSize.width(), this.mPreviewSize.height());
                } else if (GpsMeasureMode.MODE_3_DIMENSIONAL.equals(this.mCameraId)) {
                    surfaceTexture.setDefaultBufferSize(this.mPreviewSize.width(), this.mPreviewSize.height());
                } else {
                    surfaceTexture.setDefaultBufferSize(this.mPreviewSize.width(), this.mPreviewSize.height());
                }
                if (this.mPreviewSurface != null) {
                    this.mPreviewSurface.release();
                }
                this.mPreviewSurface = new Surface(surfaceTexture);
                if (this.mCaptureReader != null) {
                    this.mCaptureReader.close();
                }
                if (this.mPreviewReader != null) {
                    this.mPreviewReader.close();
                }
                if (this.mRawImageReader != null) {
                    this.mRawImageReader.close();
                }
                AndroidCamera2AgentImpl.this.mRawResultQueue.clear();
                access$000 = AndroidCamera2AgentImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mPhotoSize.width() = ");
                stringBuilder.append(this.mPhotoSize.width());
                stringBuilder.append("mPhotoSize.height() = ");
                stringBuilder.append(this.mPhotoSize.height());
                Log.d(access$000, stringBuilder.toString());
                if (GpsMeasureMode.MODE_3_DIMENSIONAL.equals(this.mCameraId) || AndroidCamera2AgentImpl.this.currentModuleId == 0 || AndroidCamera2AgentImpl.this.mIsUseJpeg) {
                    this.mCaptureReader = ImageReader.newInstance(this.mPhotoSize.width(), this.mPhotoSize.height(), 256, 1);
                } else {
                    this.mCaptureReader = ImageReader.newInstance(this.mPhotoSize.width(), this.mPhotoSize.height(), 35, 1);
                }
                String ratio = new DecimalFormat("#.00").format((((double) this.mPreviewSize.width()) / 1.0d) / ((double) this.mPreviewSize.height()));
                AndroidCamera2AgentImpl.this.mYuvCropper = null;
                if (AndroidCamera2AgentImpl.this.currentModuleId == 0) {
                    if (GpsMeasureMode.MODE_3_DIMENSIONAL.equals(this.mCameraId)) {
                        this.mPreviewReader = ImageReader.newInstance(this.mPreviewSize.width(), this.mPreviewSize.height(), 35, 1);
                    } else {
                        this.mPreviewReader = ImageReader.newInstance(this.mPreviewSize.width(), this.mPreviewSize.height(), 35, 1);
                    }
                } else if (AndroidCamera2AgentImpl.RATIO_18_9.equals(ratio)) {
                    Log.d(AndroidCamera2AgentImpl.TAG, "mPreviewReader 18 : 9");
                    this.mPreviewReader = ImageReader.newInstance(1280, MotionPictureHelper.FRAME_HEIGHT_9, 35, 1);
                    AndroidCamera2AgentImpl.this.mYuvCropper = new YuvCropper(1280, MotionPictureHelper.FRAME_HEIGHT_9, new RectF(0.0f, 0.08f, 1.0f, 0.92f));
                } else {
                    this.mPreviewReader = ImageReader.newInstance(this.mPreviewSize.width(), this.mPreviewSize.height(), 35, 1);
                }
                initPreview();
                try {
                    if (this.mIsSaveDng) {
                        Size[] supportRawSize = AndroidCamera2AgentImpl.this.getCameraDeviceInfo().getCharacteristics(this.mCameraIndex).getSupportRawSize();
                        if (supportRawSize != null) {
                            Size largestRaw = (Size) Collections.max(Arrays.asList(supportRawSize), new CompareSizesByArea());
                            this.mRawImageReader = ImageReader.newInstance(largestRaw.getWidth(), largestRaw.getHeight(), 32, 1);
                        } else {
                            Log.e(AndroidCamera2AgentImpl.TAG, "Device not support raw output!");
                            this.mRawImageReader = null;
                        }
                    } else {
                        this.mRawImageReader = null;
                    }
                    if (this.mRawImageReader != null) {
                        this.mCamera.createCaptureSession(Arrays.asList(new Surface[]{this.mPreviewSurface, this.mCaptureReader.getSurface(), this.mRawImageReader.getSurface(), this.mPreviewReader.getSurface()}), this.mCameraPreviewStateCallback, this);
                    } else if (GpsMeasureMode.MODE_3_DIMENSIONAL.equals(this.mCameraId)) {
                        this.mCamera.createCaptureSession(Arrays.asList(new Surface[]{this.mPreviewSurface, this.mCaptureReader.getSurface()}), this.mCameraPreviewStateCallback, this);
                    } else {
                        this.mCamera.createCaptureSession(Arrays.asList(new Surface[]{this.mPreviewSurface, this.mCaptureReader.getSurface(), this.mPreviewReader.getSurface()}), this.mCameraPreviewStateCallback, this);
                    }
                } catch (CameraAccessException ex) {
                    Log.e(AndroidCamera2AgentImpl.TAG, "Failed to create camera capture session", ex);
                }
            }
        }

        private void closePreviewSession() {
            try {
                this.mSession.abortCaptures();
                this.mSession = null;
            } catch (CameraAccessException ex) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Failed to close existing camera capture session", ex);
            } catch (IllegalStateException ex2) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Failed to close session with IllegalState", ex2);
            }
            changeState(4);
        }

        private void changeState(int newState) {
            if (AndroidCamera2AgentImpl.this.mCameraState.getState() != newState) {
                AndroidCamera2AgentImpl.this.mCameraState.setState(newState);
                if (newState < 16) {
                    this.mCurrentAeState = 0;
                    this.mCameraResultStateCallback.resetState();
                }
            }
        }

        private void setFaceDetect(Builder builder, int faceDetectMode) {
            if (this.mFaceDetectSupported) {
                builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, Integer.valueOf(faceDetectMode));
            }
        }

        private void startBackgroundThread() {
            this.mBackgroundThread = new HandlerThread("CameraBackground");
            this.mBackgroundThread.start();
            this.mBackgroundHandler = new Handler(this.mBackgroundThread.getLooper());
        }

        private void initPreview() {
            try {
                this.previewRequestBuilder = this.mCamera.createCaptureRequest(1);
                this.previewRequestBuilder.addTarget(this.mPreviewSurface);
                if (!GpsMeasureMode.MODE_3_DIMENSIONAL.equals(this.mCameraId)) {
                    this.previewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, Integer.valueOf(2));
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            Object[] switchValues;
            Object[] heightValues;
            if (AndroidCamera2AgentImpl.this.mmCharacteristics.isFacingFront() && AndroidCamera2AgentImpl.this.currentModuleId == 0 && !GpsMeasureMode.MODE_3_DIMENSIONAL.equals(this.mCameraId) && CustomUtil.getInstance().isPanther()) {
                try {
                    switchValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.switch", Integer.class};
                    Object[] levelValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.level", Integer.class};
                    Object[] widthValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.width", Integer.class};
                    heightValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.height", Integer.class};
                    Constructor cons = Class.forName("android.hardware.camera2.CaptureRequest$Key").getDeclaredConstructor(new Class[]{String.class, Class.class});
                    cons.setAccessible(true);
                    Object switchObj = cons.newInstance(switchValues);
                    Object levelObj = cons.newInstance(levelValues);
                    Object widthObj = cons.newInstance(widthValues);
                    Object heightObj = cons.newInstance(heightValues);
                    this.mPersistentSettings.set((Key) switchObj, Integer.valueOf(1));
                    this.mPersistentSettings.set((Key) levelObj, Integer.valueOf(CameraAgent.mLiveBolkenFrontLevel));
                    this.mPersistentSettings.set((Key) widthObj, Integer.valueOf(this.mPreviewSize.width()));
                    this.mPersistentSettings.set((Key) heightObj, Integer.valueOf(this.mPreviewSize.height()));
                    this.mPersistentSettings.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(AndroidCamera2AgentImpl.this.mcurrentOrientation));
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            } else if (AndroidCamera2AgentImpl.this.mmCharacteristics.isFacingFront() && AndroidCamera2AgentImpl.this.currentModuleId != 0 && CustomUtil.getInstance().isPanther()) {
                try {
                    switchValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.switch", Integer.class};
                    heightValues = new Object[]{"org.codeaurora.qcamera3.westalgo_rt_blur.level", Integer.class};
                    Constructor cons2 = Class.forName("android.hardware.camera2.CaptureRequest$Key").getDeclaredConstructor(new Class[]{String.class, Class.class});
                    cons2.setAccessible(true);
                    Object switchObj2 = cons2.newInstance(switchValues);
                    Object levelObj2 = cons2.newInstance(heightValues);
                    this.mPersistentSettings.set((Key) switchObj2, Integer.valueOf(0));
                    this.mPersistentSettings.set((Key) levelObj2, Integer.valueOf(0));
                } catch (Exception e22) {
                    e22.printStackTrace();
                }
            }
        }

        private void initFaceDetect() {
            setFaceDetect(this.previewRequestBuilder, this.mFaceDetectMode);
            this.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, Integer.valueOf(((Integer) this.mPersistentSettings.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE)).intValue()));
            try {
                this.mSession.setRepeatingRequest(this.previewRequestBuilder.build(), new CaptureCallback() {
                    private void process(CaptureResult result) {
                        if (AndroidCamera2AgentImpl.this.mCameraFaceDetectionCallback != null) {
                            Face[] faces = (Face[]) result.get(CaptureResult.STATISTICS_FACES);
                            ExtendedFace[] extendedFaces = AndroidCamera2AgentImpl.this.generateExtendedFaces(result, faces);
                            try {
                                if (AndroidCamera2AgentImpl.this.mCameraFaceDetectionCallback != null) {
                                    AndroidCamera2AgentImpl.this.mCameraFaceDetectionCallback.onFaceDetection(Camera2Handler.this.mActiveArray, faces, extendedFaces, null);
                                }
                            } catch (Exception e) {
                            }
                        }
                    }

                    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber);
                    }

                    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                        process(partialResult);
                    }

                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                        process(result);
                    }
                }, this.mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static String generateTimestamp() {
        return new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(new Date());
    }

    private static void writeDngBytesAndClose(Image image, TotalCaptureResult captureResult, CameraCharacteristics characteristics, File dngFile) {
        Throwable th;
        Throwable th2;
        DngCreator dngCreator;
        try {
            dngCreator = new DngCreator(characteristics, captureResult);
            FileOutputStream outputStream = new FileOutputStream(dngFile);
            try {
                dngCreator.writeImage(outputStream, image);
                outputStream.close();
                image.close();
                $closeResource(null, outputStream);
                $closeResource(null, dngCreator);
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Successfully stored DNG file: ");
                stringBuilder.append(dngFile.getAbsolutePath());
                Log.i(tag, stringBuilder.toString());
                return;
            } catch (Throwable th22) {
                Throwable th3 = th22;
                th22 = th;
                th = th3;
            }
            $closeResource(th22, outputStream);
            throw th;
        } catch (IOException e) {
            Log.e(TAG, "Could not store DNG file", e);
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "Negative ISO value", e2);
        } catch (Throwable th4) {
            $closeResource(r1, dngCreator);
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    AndroidCamera2AgentImpl(Context context) {
        this.mCameraHandlerThread.start();
        this.mCameraHandler = new Camera2Handler(this.mCameraHandlerThread.getLooper());
        this.mExceptionHandler = new CameraExceptionHandler(this.mCameraHandler);
        this.mCameraState = new AndroidCamera2StateHolder();
        this.mDispatchThread = new DispatchThread(this.mCameraHandler, this.mCameraHandlerThread);
        this.mDispatchThread.start();
        this.mCameraManager = (CameraManager) context.getSystemService("camera");
        this.mNoisemaker = new MediaActionSound();
        this.mNoisemaker.load(0);
        this.mNumCameraDevices = 0;
        this.mCameraDevices = new ArrayList();
        updateCameraDevices();
    }

    public void openCamera(Handler handler, int cameraId, CameraOpenCallback callback) {
        super.openCamera(handler, cameraId, callback);
    }

    private boolean updateCameraDevices() {
        try {
            int index;
            String[] currentCameraDevices = this.mCameraManager.getCameraIdList();
            Set<String> currentSet = new HashSet(Arrays.asList(currentCameraDevices));
            for (index = 0; index < this.mCameraDevices.size(); index++) {
                if (!currentSet.contains(this.mCameraDevices.get(index))) {
                    this.mCameraDevices.set(index, null);
                    this.mNumCameraDevices--;
                }
            }
            currentSet.removeAll(this.mCameraDevices);
            for (String device : currentCameraDevices) {
                if (currentSet.contains(device)) {
                    this.mCameraDevices.add(device);
                    this.mNumCameraDevices++;
                }
            }
            return true;
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not get device listing from camera subsystem", ex);
            return false;
        }
    }

    public void recycle() {
    }

    public CameraDeviceInfo getCameraDeviceInfo() {
        updateCameraDevices();
        return new AndroidCamera2DeviceInfo(this.mCameraManager, (String[]) this.mCameraDevices.toArray(new String[0]), this.mNumCameraDevices);
    }

    /* Access modifiers changed, original: protected */
    public Handler getCameraHandler() {
        return this.mCameraHandler;
    }

    /* Access modifiers changed, original: protected */
    public DispatchThread getDispatchThread() {
        return this.mDispatchThread;
    }

    /* Access modifiers changed, original: protected */
    public CameraStateHolder getCameraState() {
        return this.mCameraState;
    }

    /* Access modifiers changed, original: protected */
    public CameraExceptionHandler getCameraExceptionHandler() {
        return this.mExceptionHandler;
    }

    public void setCameraExceptionHandler(CameraExceptionHandler exceptionHandler) {
        this.mExceptionHandler = exceptionHandler;
    }

    @Nullable
    private ExtendedFace[] generateExtendedFaces(@NonNull CaptureResult captureResult, Face[] face) {
        if (face == null) {
            return null;
        }
        ExtendedFace[] extendedFaces = new ExtendedFace[face.length];
        for (int i = 0; i < extendedFaces.length; i++) {
            extendedFaces[i] = new ExtendedFace(i);
        }
        return extendedFaces;
    }

    private byte[] getBuffer(Image image) {
        byte[] out = new byte[(((image.getWidth() * image.getHeight()) * 3) / 2)];
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getbuffer image format : ");
        stringBuilder.append(image.getFormat());
        Log.d(tag, stringBuilder.toString());
        if (image.getFormat() == 35) {
            CameraJNI.renderByteArray(out, image);
            return out;
        }
        ByteBuffer jpegBuffer = image.getPlanes()[0].getBuffer();
        out = new byte[jpegBuffer.remaining()];
        jpegBuffer.get(out);
        return out;
    }
}
