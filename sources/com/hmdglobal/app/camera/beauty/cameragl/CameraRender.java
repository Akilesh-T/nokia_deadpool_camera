package com.hmdglobal.app.camera.beauty.cameragl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.beauty.cameragl.CameraSurfaceView.RequestRenderListener;
import com.hmdglobal.app.camera.beauty.component.SensorEventUtil;
import com.hmdglobal.app.camera.beauty.util.CaptureUtil;
import com.hmdglobal.app.camera.beauty.util.CaptureUtil.GLExecutor;
import com.hmdglobal.app.camera.beauty.util.ConUtil;
import com.hmdglobal.app.camera.beauty.util.LandMarkMatrix;
import com.hmdglobal.app.camera.beauty.util.NV21Matrix;
import com.hmdglobal.app.camera.beauty.util.NoDoubleClickUtil;
import com.hmdglobal.app.camera.beauty.util.Util;
import com.hmdglobal.app.camera.beauty.video.TextureMovieEncoder;
import com.hmdglobal.app.camera.beauty.video.TextureMovieEncoder.EncoderConfig;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.megvii.beautify.jni.BeaurifyJniSdk;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRender implements Renderer, OnFrameAvailableListener, PreviewCallback {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int FACE_DETECT_BALANCE_PARAM = 8;
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static boolean USE_OES_TEXTURE = true;
    public static Object mLock = new Object();
    private int angle;
    private int current_out_tex_idx = 0;
    private boolean drawCamera = false;
    private int[] frontPoints = new int[162];
    byte[] halTestData;
    private Handler handler = new Handler();
    private boolean isProcessing;
    private boolean isReleased = false;
    private TexureToBufferHelper mBufferhelper;
    public boolean mCameraChange = false;
    private CameraProxy mCameraDevice;
    private CameraManager mCameraManager;
    private ICameraMatrix mCameraMatrix;
    private ICameraOESMatrix mCameraOESMatrix;
    private CaptureUtil mCaptureUtil;
    private Context mContext;
    private byte[] mDataCache;
    private int mFrontTexture;
    private int mHeight;
    private ImageMatrix mImageMatrix;
    private LandMarkMatrix mLandMarkMatrix = new LandMarkMatrix();
    private int mOES_Texture;
    private int[] mOutTextureId;
    private RecordImageMatrix mRecordMatrix;
    private boolean mRecordingEnabled = false;
    private int mRecordingStatus;
    public int mRenderH = MotionPictureHelper.FRAME_HEIGHT_9;
    public int mRenderW = 1280;
    private SurfaceTexture mSurfaceTexture;
    private final FloatBuffer mTextureBuffer;
    private final FloatBuffer mVertexBuffer;
    private String mVideoPath;
    private int mWidth;
    private volatile RenderMessageBean messageBean = RenderMessageBean.getInstance();
    private onPreviewBytes monPreviewBytes = new onPreviewBytes() {
        public void onPreviewData(byte[] bytes, Camera camera) {
            CameraRender.this.onPreviewFrame(bytes, camera);
        }
    };
    float[] mtx = new float[16];
    private boolean needDraw = true;
    private boolean needShow = false;
    private NV21Matrix nv21Matrix = new NV21Matrix();
    private RequestRenderListener requestRenderListener;
    int rotation = 0;
    private SensorEventUtil sensorUtil;
    float[] textureCords;
    private ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();
    private TextureMovieEncoder videoEncoder;
    private byte[] yuv;

    public interface TakePictureCallBack {
        void onTakPicture(Buffer buffer);
    }

    public interface onPreviewBytes {
        void onPreviewData(byte[] bArr, Camera camera);
    }

    public interface onVideoStopListener {
        void onVideoStoped(String str);
    }

    public void changeRecordingState(boolean isRecording) {
        this.mRecordingEnabled = isRecording;
    }

    public void switchCamera(int camera) {
        this.needDraw = false;
        this.mCameraChange = true;
        this.mCameraManager.setAngel(camera);
    }

    public void setDrawEnable(boolean enable) {
        this.needDraw = enable;
    }

    public boolean isDrawable() {
        return this.needDraw;
    }

    public CameraRender(Context context, CameraManager cameraManager, SensorEventUtil sensorUtil) {
        this.mContext = context;
        this.mCameraManager = cameraManager;
        this.mCameraMatrix = new ICameraMatrix(context);
        this.mImageMatrix = new ImageMatrix(context);
        this.mCameraOESMatrix = new ICameraOESMatrix(context);
        this.mRecordMatrix = new RecordImageMatrix(context);
        initPreview();
        this.sensorUtil = sensorUtil;
        this.mCaptureUtil = new CaptureUtil(context, new GLExecutor() {
            public void runOnRenderThread(Runnable runnable) {
                if (CameraRender.this.requestRenderListener != null) {
                    CameraRender.this.requestRenderListener.runOnRenderThread(runnable);
                }
            }
        });
        this.mVertexBuffer = ByteBuffer.allocateDirect(OpenglUtil.CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mVertexBuffer.put(OpenglUtil.CUBE).position(0);
        this.mTextureBuffer = ByteBuffer.allocateDirect(OpenglUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mTextureBuffer.put(OpenglUtil.TEXTURE_NO_ROTATION).position(0);
        this.mBufferhelper = new TexureToBufferHelper();
    }

    public onPreviewBytes getApi2CameraDeviceCallback() {
        return this.monPreviewBytes;
    }

    public void setCameraDevice(CameraProxy devices) {
        this.mCameraDevice = devices;
        Message message2 = new Message();
        message2.what = 107;
        message2.obj = this;
        this.mCameraDevice.getCameraHandler().sendMessage(message2);
    }

    public void initPreview() {
        ICameraMatrix iCameraMatrix;
        if (isOESMode()) {
            iCameraMatrix = this.mCameraMatrix;
            this.mOES_Texture = ICameraMatrix.getOESTexture();
            if (this.mSurfaceTexture != null) {
                this.mSurfaceTexture.release();
            }
            this.mSurfaceTexture = new SurfaceTexture(this.mOES_Texture);
            return;
        }
        iCameraMatrix = this.mCameraMatrix;
        this.mFrontTexture = ICameraMatrix.get2DTextureID();
        GLES20.glBindTexture(3553, this.mFrontTexture);
        GLES30.glTexImage2D(3553, 0, 6408, this.mRenderW, this.mRenderH, 0, 6408, 5121, null);
        if (this.mSurfaceTexture != null) {
            this.mSurfaceTexture.release();
        }
        this.mSurfaceTexture = new SurfaceTexture(10);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        if (this.mRecordingEnabled) {
            this.mRecordingStatus = 2;
        } else {
            this.mRecordingStatus = 0;
        }
    }

    private boolean isOESMode() {
        return true;
    }

    private void swapIndex() {
        if (this.current_out_tex_idx == 0) {
            this.current_out_tex_idx = 1;
        } else {
            this.current_out_tex_idx = 0;
        }
    }

    public void initVideoPath(String path) {
        this.mVideoPath = path;
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (!NoDoubleClickUtil.isDoubleChanged()) {
            float ratio = ((float) this.mCameraManager.cameraWidth) / ((float) this.mCameraManager.cameraHeight);
            int width2 = width;
            int height2 = height;
            if (width2 > height2) {
                int temp = width2;
                width2 = height2;
                height2 = temp;
            }
            float temp2 = ((float) width2) * ratio;
            if (temp2 < ((float) height2)) {
                if (ratio == 1.0f) {
                    if (CustomUtil.getInstance().isPanther()) {
                        height2 = width2 + 522;
                    } else {
                        height2 = width2 + 517;
                    }
                } else if (ratio == 1.3333334f) {
                    height2 = (((int) temp2) + (((int) (((float) height2) - temp2)) / 2)) + Const.CODE_C1_DLW;
                } else if (ratio != 2.0f) {
                    height2 = ((int) temp2) + (((int) (((float) height2) - temp2)) / 2);
                }
            }
            if (this.mOutTextureId != null) {
                if (this.mWidth != width2 || this.mHeight != height2 || this.mCameraChange) {
                    onDestroy();
                    if (this.mCameraChange) {
                        this.mCameraChange = false;
                    }
                } else {
                    return;
                }
            }
            if (this.mSurfaceTexture == null) {
                initPreview();
            }
            this.mRenderH = this.mCameraManager.cameraHeight;
            this.mRenderW = this.mCameraManager.cameraWidth;
            this.mWidth = width2;
            this.mHeight = height2;
            this.messageBean.width = (long) this.mCameraManager.cameraWidth;
            this.messageBean.height = (long) this.mCameraManager.cameraHeight;
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            if (this.mCameraManager.getIsFront()) {
                this.textureCords = OpenglUtil.TEXTURE_ROTATED_FRONT;
            } else {
                this.textureCords = OpenglUtil.TEXTURE_ROTATED_BACK;
            }
            this.mTextureBuffer.clear();
            this.mTextureBuffer.put(this.textureCords).position(0);
            Matrix.setIdentityM(this.mtx, 0);
            if (isOESMode()) {
                this.mSurfaceTexture.updateTexImage();
                this.mCameraOESMatrix.init(this.requestRenderListener.isFront());
                this.mCameraOESMatrix.initCameraFrameBuffer(this.mRenderW, this.mRenderH);
                this.mCameraOESMatrix.onOutputSizeChanged(this.mRenderW, this.mRenderH);
                this.mCameraOESMatrix.setTextureTransformMatrix(this.mtx);
            }
            this.mRecordMatrix.init();
            this.mRecordMatrix.initCameraFrameBuffer(this.mRenderH, this.mRenderW);
            this.mImageMatrix.init();
            this.mOutTextureId = OpenglUtil.initTextureID(this.mRenderW, this.mRenderH);
            BeaurifyJniSdk.preViewInstance().nativeCreateBeautyHandle(this.mContext, this.mRenderW, this.mRenderH, this.mCameraManager.Angle, 0, ConUtil.getFileContent(this.mContext, R.raw.mgbeautify_1_2_4_model), ConUtil.getFileContent(this.mContext, R.raw.detect_model), null);
            BeaurifyJniSdk.preViewInstance().nativeUseFastFilter(false);
            GLES20.glViewport(0, 0, width2, height2);
            changeImageDisplaySize(width2, height2);
            this.mBufferhelper.onOutputSizeChanged(this.mRenderW, this.mRenderH);
            initBeautyParam();
        }
    }

    public void setAngle(int a) {
        this.angle = a;
    }

    public void onDrawFrame(GL10 gl) {
        if (this.mImageMatrix != null) {
            drawNormalFrame(gl);
            StringBuilder stringBuilder;
            if (this.mRecordingEnabled) {
                switch (this.mRecordingStatus) {
                    case 0:
                        int bitrate = 12000000;
                        if (this.mRenderH == 1080) {
                            bitrate = 17000000;
                        }
                        this.videoEncoder = new TextureMovieEncoder(this.mContext);
                        this.videoEncoder.startRecording(new EncoderConfig(this.mVideoPath, this.mRenderH, this.mRenderW, bitrate, this.mCameraManager.isFrontCam(), EGL14.eglGetCurrentContext(), null, this.angle));
                        this.mRecordingStatus = 1;
                        break;
                    case 1:
                        break;
                    case 2:
                        this.videoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                        this.videoEncoder.resumeRecording();
                        this.mRecordingStatus = 1;
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown status ");
                        stringBuilder.append(this.mRecordingStatus);
                        throw new RuntimeException(stringBuilder.toString());
                }
            }
            switch (this.mRecordingStatus) {
                case 0:
                    break;
                case 1:
                case 2:
                    this.videoEncoder.stopRecording();
                    this.mRecordingStatus = 0;
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unknown status ");
                    stringBuilder.append(this.mRecordingStatus);
                    throw new RuntimeException(stringBuilder.toString());
            }
            if (this.mRecordingStatus == 1) {
                GLES20.glBindFramebuffer(36160, 0);
                this.videoEncoder.setTextureId(this.mOutTextureId[this.current_out_tex_idx]);
                this.videoEncoder.frameAvailable(this.mSurfaceTexture);
            }
        }
    }

    public void drawNormalFrame(GL10 gl) {
        int textureID;
        this.mSurfaceTexture.updateTexImage();
        if (Util.isFiltered) {
            Util.isFiltered = false;
            if (TextUtils.isEmpty(Util.filterPath)) {
                BeaurifyJniSdk.preViewInstance().nativeRemoveFilter();
            } else {
                BeaurifyJniSdk.preViewInstance().nativeSetFilter(Util.filterPath);
            }
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(16640);
        if (isOESMode()) {
            textureID = this.mCameraOESMatrix.onDrawToTexture(this.mOES_Texture);
        } else {
            textureID = this.mCameraMatrix.onDrawToTexture(this.mFrontTexture);
        }
        if (Util.isDebugingLandMark) {
            this.mLandMarkMatrix.drawLandMark(textureID, this.mRenderW, this.mRenderH);
        }
        BeaurifyJniSdk.preViewInstance().nativeProcessTexture(textureID, this.mOutTextureId[this.current_out_tex_idx]);
        swapIndex();
        int frameTexture = this.mOutTextureId[this.current_out_tex_idx];
        GLES20.glBindFramebuffer(36160, 0);
        GLES20.glViewport(0, 0, this.mWidth, this.mHeight);
        this.mImageMatrix.onDrawFrame(frameTexture, this.mVertexBuffer, this.mTextureBuffer);
        if (isOESMode()) {
            this.mSurfaceTexture.updateTexImage();
        }
    }

    private void updateTexture(byte[] data) {
        if (!isOESMode()) {
            this.nv21Matrix.setOutputSize(this.mRenderW, this.mRenderH);
            this.nv21Matrix.renderNv21(data, this.mFrontTexture, this.mCameraManager.cameraWidth, this.mCameraManager.cameraHeight, this.requestRenderListener.isFront());
        }
    }

    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
    }

    public void setRequestRenderListener(RequestRenderListener requestRenderListener) {
        this.requestRenderListener = requestRenderListener;
    }

    private void changeImageDisplaySize(int width, int height) {
        int outputWidth = width;
        int outputHeight = height;
        if (this.mCameraManager.Angle == MediaProviderUtils.ROTATION_270 || this.mCameraManager.Angle == 90) {
            outputWidth = height;
            outputHeight = width;
        }
        float ratioMax = Math.max(((float) outputWidth) / ((float) this.mCameraManager.cameraWidth), ((float) outputHeight) / ((float) this.mCameraManager.cameraHeight));
        int imageWidthNew = Math.round(((float) this.mCameraManager.cameraWidth) * ratioMax);
        int imageHeightNew = Math.round(((float) this.mCameraManager.cameraHeight) * ratioMax);
        float ratioWidth = ((float) imageWidthNew) / ((float) outputWidth);
        float ratioHeight = ((float) imageHeightNew) / ((float) outputHeight);
        if (this.mCameraManager.Angle == MediaProviderUtils.ROTATION_270 || this.mCameraManager.Angle == 90) {
            ratioWidth = ((float) imageHeightNew) / ((float) outputHeight);
            ratioHeight = ((float) imageWidthNew) / ((float) outputWidth);
        }
        float offset_width = (float) (0.0f - (1.0d / ((double) ratioWidth)));
        float offset_height = (float) (1.0d - (1.0d / ((double) ratioHeight)));
        float[] cube = new float[]{(OpenglUtil.CUBE[0] / ratioHeight) + offset_height, (OpenglUtil.CUBE[1] / ratioWidth) + offset_width, (OpenglUtil.CUBE[2] / ratioHeight) + offset_height, (OpenglUtil.CUBE[3] / ratioWidth) + offset_width, (OpenglUtil.CUBE[4] / ratioHeight) + offset_height, (OpenglUtil.CUBE[5] / ratioWidth) + offset_width, (OpenglUtil.CUBE[6] / ratioHeight) + offset_height, (OpenglUtil.CUBE[7] / ratioWidth) + offset_width};
        this.mVertexBuffer.clear();
        this.mVertexBuffer.put(cube).position(0);
    }

    public void sceenAutoFit(int screenW, int screenH, int cameraW, int cameraH, int angle) {
        if (angle == 90 || angle == MediaProviderUtils.ROTATION_270) {
            int temp = cameraW;
            cameraW = cameraH;
            cameraH = temp;
        }
        float dxRatio = 0.0f;
        float dyRatio = 0.0f;
        if (cameraW * screenH > screenW * cameraH) {
            dxRatio = (((float) screenW) - (((float) cameraW) * (((float) screenH) / ((float) cameraH)))) / ((float) screenW);
        } else {
            dyRatio = (((float) screenH) - (((float) cameraH) * (((float) screenW) / ((float) cameraW)))) / ((float) screenH);
        }
        float[] cube = new float[]{OpenglUtil.CUBE[0] + dxRatio, OpenglUtil.CUBE[1] + dyRatio, OpenglUtil.CUBE[2] - dxRatio, OpenglUtil.CUBE[3] + dyRatio, OpenglUtil.CUBE[4] + dxRatio, OpenglUtil.CUBE[5] - dyRatio, OpenglUtil.CUBE[6] - dxRatio, OpenglUtil.CUBE[7] - dyRatio};
        this.mVertexBuffer.clear();
        this.mVertexBuffer.put(cube).position(0);
    }

    public byte[] scaleUV420(byte[] data, int width, int height, int scale, int[] sz) {
        int i;
        int i2 = width;
        int i3 = height;
        int w = i2 / scale;
        int h = i3 / scale;
        if (w % 2 == 1) {
            w++;
        }
        if (h % 2 == 1) {
            h++;
        }
        sz[0] = w;
        sz[1] = h;
        if (this.yuv == null || this.yuv.length != (w * h) + ((w * h) / 2)) {
            this.yuv = new byte[((w * h) + ((w * h) / 2))];
        }
        int i4 = 0;
        int k = 0;
        while (k < i3) {
            i = i4;
            i4 = 0;
            while (i4 < i2) {
                this.yuv[i] = data[(k * i2) + i4];
                i++;
                i4 += scale;
            }
            k += scale;
            i4 = i;
        }
        k = i2 * i3;
        i = w * h;
        i4 = 0;
        while (i4 < i3 / 2) {
            int i5 = i;
            i = 0;
            while (i < i2) {
                if (i5 < this.yuv.length) {
                    this.yuv[i5] = data[((i4 * i2) + k) + i];
                    i5++;
                }
                if (i5 < this.yuv.length) {
                    this.yuv[i5] = data[(((i4 * i2) + k) + i) + 1];
                    i5++;
                }
                i += 2 * scale;
            }
            i4 += scale;
            i = i5;
        }
        return this.yuv;
    }

    public void takePicture(boolean isFrontCam, Runnable callback) {
        this.mCaptureUtil.takePicture(this.mOutTextureId[0], this.mRenderW, this.mRenderH, this.mCameraManager.cameraHeight, this.mCameraManager.cameraWidth, isFrontCam, callback, this.mDataCache);
    }

    private void reverseBuf(ByteBuffer buf, int width, int height) {
        long ts = System.currentTimeMillis();
        int i = 0;
        byte[] tmp = new byte[(width * 4)];
        while (true) {
            int i2 = i + 1;
            if (i < height / 2) {
                buf.get(tmp);
                System.arraycopy(buf.array(), buf.limit() - buf.position(), buf.array(), buf.position() - (width * 4), width * 4);
                System.arraycopy(tmp, 0, buf.array(), buf.limit() - buf.position(), width * 4);
                i = i2;
            } else {
                buf.rewind();
                return;
            }
        }
    }

    public Buffer readToBuffer() {
        Buffer buf = ByteBuffer.allocateDirect((this.mWidth * this.mHeight) * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, this.mWidth, this.mHeight, 6408, 5121, buf);
        reverseBuf(buf, this.mWidth, this.mHeight);
        return buf;
    }

    public Bitmap getBitmap(Buffer buf) {
        Bitmap bmp = Bitmap.createBitmap(this.mWidth, this.mHeight, Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(buf);
        return bmp;
    }

    public void save(Bitmap bmp, String filename) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filename);
            bmp.compress(CompressFormat.PNG, 100, out);
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
        }
    }

    private void dealwithFrameTestHAL(byte[] data) {
        if (this.halTestData == null || this.halTestData.length != data.length) {
            this.halTestData = new byte[data.length];
        }
        if (!this.isProcessing) {
            this.isProcessing = true;
            Future<Integer> future = this.threadPoolExecutor.submit(new Callable<Integer>() {
                public Integer call() throws Exception {
                    if (CameraRender.this.isReleased) {
                        return Integer.valueOf(0);
                    }
                    if (CameraRender.this.requestRenderListener != null) {
                        CameraRender.this.requestRenderListener.runOnRenderThread(new Runnable() {
                            public void run() {
                                CameraRender.this.updateTexture(CameraRender.this.halTestData);
                            }
                        });
                    }
                    CameraRender.this.requestRenderListener.startRequestRender();
                    CameraRender.this.isProcessing = false;
                    return Integer.valueOf(0);
                }
            });
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mSurfaceTexture;
    }

    public int getTextureId() {
        return this.mOES_Texture;
    }

    private void dealwithFrame(final byte[] data) {
        if (this.requestRenderListener != null) {
            this.requestRenderListener.runOnRenderThread(new Runnable() {
                public void run() {
                    CameraRender.this.updateTexture(data);
                }
            });
        }
        onDectBeauty(data, ((double) this.mRenderW) / ((double) this.mCameraManager.cameraWidth), ((double) this.mRenderH) / ((double) this.mCameraManager.cameraHeight), this.mCameraManager.cameraWidth, this.mCameraManager.cameraHeight);
        if (Util.isDebugingLandMark) {
            BeaurifyJniSdk.preViewInstance().nativeGetPoints(this.frontPoints);
            this.mLandMarkMatrix.setPoints(this.frontPoints);
        }
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        if (this.mDataCache == null || this.mDataCache.length != data.length) {
            this.mDataCache = new byte[data.length];
        }
        System.arraycopy(data, 0, this.mDataCache, 0, data.length);
        if (camera != null) {
            camera.addCallbackBuffer(this.mDataCache);
        }
        if (Util.isTestHAL) {
            dealwithFrameTestHAL(this.mDataCache);
        } else {
            dealwithFrame(this.mDataCache);
        }
        this.requestRenderListener.startRequestRender();
        if (!this.needDraw) {
            this.handler.postDelayed(new Runnable() {
                public void run() {
                    CameraRender.this.needDraw = true;
                }
            }, 300);
        }
    }

    private void onDectBeauty(byte[] data, double xScale, double yScale, int cameraWidth, int cameraHeight) {
        int orientation = this.sensorUtil.orientation;
        if (orientation == 0) {
            this.rotation = this.mCameraManager.Angle;
        } else if (orientation == 1) {
            this.rotation = 0;
        } else if (orientation == 2) {
            this.rotation = MediaProviderUtils.ROTATION_180;
        } else if (orientation == 3) {
            this.rotation = 360 - this.mCameraManager.Angle;
        }
        BeaurifyJniSdk.preViewInstance().nativeDetectFaceOrientation(data, xScale, yScale, cameraWidth, cameraHeight, 0, this.rotation);
    }

    public void deleteTextures(GLSurfaceView mGlSurfaceView) {
        mGlSurfaceView.queueEvent(new Runnable() {
            public void run() {
                if (CameraRender.this.mCameraMatrix != null) {
                    CameraRender.this.mCameraMatrix.destroyFramebuffers();
                    CameraRender.this.mCameraMatrix.destroy();
                }
                if (CameraRender.this.mImageMatrix != null) {
                    CameraRender.this.mImageMatrix.destroy();
                }
                if (CameraRender.this.mBufferhelper != null) {
                    CameraRender.this.mBufferhelper.destroy();
                }
                if (CameraRender.this.mOutTextureId != null) {
                    GLES20.glDeleteTextures(2, CameraRender.this.mOutTextureId, 0);
                    CameraRender.this.mOutTextureId = null;
                }
                CameraRender.this.mCameraMatrix = null;
                CameraRender.this.mImageMatrix = null;
                CameraRender.this.mBufferhelper = null;
            }
        });
    }

    public void deleteTextures2_onGL() {
        if (this.mCameraMatrix != null) {
            this.mCameraMatrix.destroyFramebuffers();
            this.mCameraMatrix.destroy();
        }
        if (this.mImageMatrix != null) {
            this.mImageMatrix.destroy();
        }
        if (this.mBufferhelper != null) {
            this.mBufferhelper.destroy();
        }
        if (this.mOutTextureId != null) {
            GLES20.glDeleteTextures(2, this.mOutTextureId, 0);
            this.mOutTextureId = null;
        }
    }

    public void onDestroy() {
        deleteTextures2_onGL();
        try {
            BeaurifyJniSdk.preViewInstance().nativeReleaseResources();
        } catch (Exception e) {
        }
        try {
            this.threadPoolExecutor.submit(new Callable<Integer>() {
                public Integer call() throws Exception {
                    CameraRender.this.isReleased = true;
                    return Integer.valueOf(0);
                }
            }).get();
        } catch (Throwable th) {
        }
    }

    private void initBeautyParam() {
        if (this.requestRenderListener.isFront()) {
            BeaurifyJniSdk.preViewInstance().nativeSetStickerParam(1.0f);
        } else {
            BeaurifyJniSdk.preViewInstance().nativeSetStickerParam(0.0f);
        }
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(3, Util.CURRENT_MG_BEAUTIFY_DENOISE);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(4, Util.CURRENT_MG_BEAUTIFY_BRIGHTNESS);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(1, Util.CURRENT_MG_BEAUTIFY_BRIGHTEN_EYE);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(2, Util.CURRENT_MG_BEAUTIFY_TOOTH);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(5, Util.CURRENT_MG_BEAUTIFY_ADD_PINK);
    }

    public void setVideoStopListener(onVideoStopListener listener) {
        this.videoEncoder.setVideoSavedListener(listener);
    }
}
