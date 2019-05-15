package com.hmdglobal.app.camera.beauty.video;

import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.grafika.gles.EglCore;
import com.hmdglobal.app.camera.beauty.cameragl.CameraRender.onVideoStopListener;
import com.hmdglobal.app.camera.beauty.cameragl.OpenglUtil;
import com.hmdglobal.app.camera.beauty.cameragl.RecordImageMatrix;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class TextureMovieEncoder implements Runnable {
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_PAUSE = 6;
    private static final int MSG_QUIT = 5;
    private static final int MSG_RESUME = 7;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final String TAG = "TextureMovieEncoder";
    private static final boolean VERBOSE = false;
    private long baseTimeStamp = -1;
    private FloatBuffer gLCubeBuffer;
    private FloatBuffer gLTextureBuffer;
    private Context mContext;
    private EglCore mEglCore;
    private volatile EncoderHandler mHandler;
    private WindowSurface mInputWindowSurface;
    private onVideoStopListener mListener;
    private boolean mReady;
    private Object mReadyFence = new Object();
    private RecordImageMatrix mRecordMatrix;
    private boolean mRunning;
    private FloatBuffer mTextureBuffer;
    private int mTextureId;
    private FloatBuffer mVertexBuffer;
    private VideoEncoderCore mVideoEncoder;
    private String mVideoPath = "";
    long onceDelayTime;
    long pauseDelayTime;

    public static class EncoderConfig {
        final int angle;
        final boolean isFront;
        final int mBitRate;
        final EGLContext mEglContext;
        final int mHeight;
        final int mWidth;
        final String path;

        public EncoderConfig(String path, int width, int height, int bitRate, boolean isFront, EGLContext sharedEglContext, CameraInfo info, int a) {
            this.path = path;
            this.mWidth = width;
            this.isFront = isFront;
            this.mHeight = height;
            this.mBitRate = bitRate;
            this.mEglContext = sharedEglContext;
            this.angle = a;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EncoderConfig: ");
            stringBuilder.append(this.mWidth);
            stringBuilder.append("x");
            stringBuilder.append(this.mHeight);
            stringBuilder.append(" @");
            stringBuilder.append(this.mBitRate);
            stringBuilder.append(" to '");
            stringBuilder.append(this.path);
            stringBuilder.append("' ctxt=");
            stringBuilder.append(this.mEglContext);
            return stringBuilder.toString();
        }
    }

    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            this.mWeakEncoder = new WeakReference(encoder);
        }

        public void stopVideo() {
            removeMessages(2);
            removeMessages(4);
            sendEmptyMessage(1);
        }

        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;
            TextureMovieEncoder encoder = (TextureMovieEncoder) this.mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TextureMovieEncoder.TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }
            switch (what) {
                case 0:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case 1:
                    encoder.handleStopRecording();
                    break;
                case 2:
                    encoder.handleFrameAvailable((float[]) obj, (((long) inputMessage.arg1) << 32) | (((long) inputMessage.arg2) & 4294967295L));
                    break;
                case 3:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case 4:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case 5:
                    Looper.myLooper().quit();
                    break;
                case 6:
                    encoder.handlePauseRecording();
                    break;
                case 7:
                    encoder.handleResumeRecording();
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unhandled msg what=");
                    stringBuilder.append(what);
                    throw new RuntimeException(stringBuilder.toString());
            }
        }
    }

    public TextureMovieEncoder(Context context) {
        this.mContext = context;
    }

    public void startRecording(EncoderConfig config) {
        Log.d(TAG, "Encoder: startRecording()");
        synchronized (this.mReadyFence) {
            if (this.mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            this.mRunning = true;
            new Thread(this, TAG).start();
            while (!this.mReady) {
                try {
                    this.mReadyFence.wait();
                } catch (InterruptedException e) {
                }
            }
            this.mHandler.sendMessage(this.mHandler.obtainMessage(0, config));
        }
    }

    public void stopRecording() {
        this.mHandler.stopVideo();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5));
    }

    public void pauseRecording() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(6));
    }

    public void resumeRecording() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7));
    }

    public boolean isRecording() {
        boolean z;
        synchronized (this.mReadyFence) {
            z = this.mRunning;
        }
        return z;
    }

    public void updateSharedContext(EGLContext sharedContext) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, sharedContext));
    }

    /* JADX WARNING: Missing block: B:8:0x000a, code skipped:
            r0 = new float[16];
            r9.getTransformMatrix(r0);
            r1 = r9.getTimestamp();
     */
    /* JADX WARNING: Missing block: B:9:0x0019, code skipped:
            if (r1 != 0) goto L_0x0023;
     */
    /* JADX WARNING: Missing block: B:10:0x001b, code skipped:
            android.util.Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
     */
    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:12:0x0023, code skipped:
            r8.mHandler.sendMessage(r8.mHandler.obtainMessage(2, (int) (r1 >> 32), (int) r1, r0));
     */
    /* JADX WARNING: Missing block: B:13:0x0035, code skipped:
            return;
     */
    public void frameAvailable(android.graphics.SurfaceTexture r9) {
        /*
        r8 = this;
        r0 = r8.mReadyFence;
        monitor-enter(r0);
        r1 = r8.mReady;	 Catch:{ all -> 0x0036 }
        if (r1 != 0) goto L_0x0009;
    L_0x0007:
        monitor-exit(r0);	 Catch:{ all -> 0x0036 }
        return;
    L_0x0009:
        monitor-exit(r0);	 Catch:{ all -> 0x0036 }
        r0 = 16;
        r0 = new float[r0];
        r9.getTransformMatrix(r0);
        r1 = r9.getTimestamp();
        r3 = 0;
        r3 = (r1 > r3 ? 1 : (r1 == r3 ? 0 : -1));
        if (r3 != 0) goto L_0x0023;
    L_0x001b:
        r3 = "TextureMovieEncoder";
        r4 = "HEY: got SurfaceTexture with timestamp of zero";
        android.util.Log.w(r3, r4);
        return;
    L_0x0023:
        r3 = r8.mHandler;
        r4 = r8.mHandler;
        r5 = 2;
        r6 = 32;
        r6 = r1 >> r6;
        r6 = (int) r6;
        r7 = (int) r1;
        r4 = r4.obtainMessage(r5, r6, r7, r0);
        r3.sendMessage(r4);
        return;
    L_0x0036:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x0036 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.beauty.video.TextureMovieEncoder.frameAvailable(android.graphics.SurfaceTexture):void");
    }

    public void setTextureId(int id) {
        synchronized (this.mReadyFence) {
            if (this.mReady) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(3, id, 0, null));
                return;
            }
        }
    }

    public void run() {
        Looper.prepare();
        synchronized (this.mReadyFence) {
            this.mHandler = new EncoderHandler(this);
            this.mReady = true;
            this.mReadyFence.notify();
        }
        Looper.loop();
        Log.d(TAG, "Encoder thread exiting");
        synchronized (this.mReadyFence) {
            this.mRunning = false;
            this.mReady = false;
            this.mHandler = null;
        }
    }

    private void handleStartRecording(EncoderConfig config) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleStartRecording ");
        stringBuilder.append(config);
        Log.d(str, stringBuilder.toString());
        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate, config.path, config.isFront, config.angle);
    }

    private void handleFrameAvailable(float[] transform, long timestampNanos) {
        this.mVideoEncoder.drainEncoder(false);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("---setTextureId==");
        stringBuilder.append(this.mTextureId);
        Log.e("hero", stringBuilder.toString());
        if (this.baseTimeStamp == -1) {
            this.baseTimeStamp = System.nanoTime();
            this.mVideoEncoder.startRecord();
        }
        long nano = System.nanoTime();
        long time = (nano - this.baseTimeStamp) - this.pauseDelayTime;
        this.mRecordMatrix.onDrawFrame(this.mTextureId, this.mVertexBuffer, this.mTextureBuffer);
        PrintStream printStream = System.out;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("TimeStampVideo=");
        stringBuilder2.append(time);
        stringBuilder2.append(";nanoTime=");
        stringBuilder2.append(nano);
        stringBuilder2.append(";baseTimeStamp=");
        stringBuilder2.append(this.baseTimeStamp);
        stringBuilder2.append(";pauseDelay=");
        stringBuilder2.append(this.pauseDelayTime);
        printStream.println(stringBuilder2.toString());
        this.mInputWindowSurface.setPresentationTime(time);
        this.mInputWindowSurface.swapBuffers();
    }

    private void handlePauseRecording() {
        this.onceDelayTime = System.nanoTime();
        this.mVideoEncoder.pauseRecording();
    }

    private void handleResumeRecording() {
        this.onceDelayTime = System.nanoTime() - this.onceDelayTime;
        this.pauseDelayTime += this.onceDelayTime;
        this.mVideoEncoder.resumeRecording();
    }

    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        this.mVideoEncoder.stopAudRecord();
        this.mVideoEncoder.drainEncoder(true);
        releaseEncoder();
        if (this.mListener != null) {
            this.mListener.onVideoStoped(this.mVideoPath);
        }
    }

    private void handleSetTexture(int id) {
        this.mTextureId = id;
    }

    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleUpdatedSharedContext ");
        stringBuilder.append(newSharedContext);
        Log.d(str, stringBuilder.toString());
        this.mInputWindowSurface.releaseEglSurface();
        this.mEglCore.release();
        this.mEglCore = new EglCore(newSharedContext, 1);
        this.mInputWindowSurface.recreate(this.mEglCore);
        this.mInputWindowSurface.makeCurrent();
    }

    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate, String path, boolean isFrontCamera, int angle) {
        try {
            float[] textureCords;
            this.mVideoPath = path;
            this.mVideoEncoder = new VideoEncoderCore(width, height, bitRate, path, angle);
            this.mEglCore = new EglCore(sharedContext, 1);
            this.mInputWindowSurface = new WindowSurface(this.mEglCore, this.mVideoEncoder.getInputSurface(), true);
            this.mInputWindowSurface.makeCurrent();
            this.mRecordMatrix = new RecordImageMatrix(this.mContext);
            this.mRecordMatrix.init();
            this.mRecordMatrix.initCameraFrameBuffer(width, height);
            this.mVertexBuffer = ByteBuffer.allocateDirect(OpenglUtil.CUBE.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            this.mVertexBuffer.put(OpenglUtil.CUBE).position(0);
            this.mTextureBuffer = ByteBuffer.allocateDirect(OpenglUtil.TEXTURE_NO_ROTATION.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            this.mTextureBuffer.put(OpenglUtil.TEXTURE_NO_ROTATION).position(0);
            if (isFrontCamera) {
                textureCords = OpenglUtil.TEXTURE_ROTATED_FRONT;
            } else {
                textureCords = OpenglUtil.TEXTURE_ROTATED_BACK;
            }
            this.mTextureBuffer.clear();
            this.mTextureBuffer.put(textureCords).position(0);
            this.baseTimeStamp = -1;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void releaseEncoder() {
        this.mVideoEncoder.release();
        if (this.mInputWindowSurface != null) {
            this.mInputWindowSurface.release();
            this.mInputWindowSurface = null;
        }
        if (this.mEglCore != null) {
            this.mEglCore.release();
            this.mEglCore = null;
        }
    }

    public void setVideoSavedListener(onVideoStopListener listener) {
        this.mListener = listener;
    }

    public void setTextureBuffer(FloatBuffer gLTextureBuffer) {
        this.gLTextureBuffer = gLTextureBuffer;
    }

    public void setCubeBuffer(FloatBuffer gLCubeBuffer) {
        this.gLCubeBuffer = gLCubeBuffer;
    }
}
