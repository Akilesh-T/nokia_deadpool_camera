package com.hmdglobal.app.camera.beauty.video;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class VideoEncoderCore {
    private static final int FRAME_RATE = 30;
    private static final int IFRAME_INTERVAL = 1;
    private static final String MIME_TYPE = "video/avc";
    private static final int MSG_AUDIO_STEP = 2;
    private static final int MSG_PAUSE = 4;
    private static final int MSG_QUIT = 3;
    private static final int MSG_RESUME = 5;
    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final String TAG = "VideoEncoderCore";
    private static final boolean VERBOSE = false;
    private int audioFormat = 2;
    private String audioMime = "audio/mp4a-latm";
    private int audioRate = 96000;
    private AudioEncoder audioRecorder;
    private Thread audioThread;
    private int bufferSize;
    private int channelConfig = 12;
    private int channelCount = 2;
    private Object lock = new Object();
    private MediaCodec mAudioEnc;
    private int mAudioTrackIndex;
    private BufferInfo mBufferInfo;
    private Surface mInputSurface;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted;
    private AudioRecord mRecorder;
    private boolean mStopState = false;
    private MediaCodec mVideoEncoder;
    private int mVideoTrackIndex;
    private int sampleRate = 48000;

    class AudioEncoder implements Runnable {
        private long baseTimeStamp = -1;
        private boolean cancelFlag = false;
        private boolean isReady;
        private boolean isRecording = true;
        AudioHandler mHandler;
        private Object mReadyFence = new Object();
        private long oncePauseTime;
        private long pauseDelayTime;
        private boolean pausing = false;

        AudioEncoder() {
        }

        public void run() {
            Looper.prepare();
            this.mHandler = new AudioHandler(this);
            synchronized (this.mReadyFence) {
                this.isReady = true;
                this.mReadyFence.notify();
            }
            Looper.loop();
            synchronized (this.mReadyFence) {
                this.isReady = false;
                this.mHandler = null;
            }
        }

        public void startRecord() {
            synchronized (this.mReadyFence) {
                if (!this.isReady) {
                    try {
                        this.mReadyFence.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                this.mHandler.sendEmptyMessage(0);
            }
        }

        public void pause() {
            this.mHandler.sendEmptyMessage(4);
        }

        public void resume() {
            this.mHandler.sendEmptyMessage(5);
        }

        public void stopRecord() {
            this.mHandler.sendEmptyMessage(1);
        }

        public void handleStartRecord() {
            this.baseTimeStamp = System.nanoTime();
            this.mHandler.sendEmptyMessage(2);
        }

        public void handleAudioStep() {
            try {
                if (!this.cancelFlag) {
                    if (this.pausing) {
                        if (this.isRecording) {
                            this.mHandler.sendEmptyMessage(2);
                            return;
                        }
                        drainEncoder();
                        this.mHandler.sendEmptyMessage(3);
                    } else if (this.isRecording) {
                        audioStep();
                        this.mHandler.sendEmptyMessage(2);
                    } else {
                        drainEncoder();
                        this.mHandler.sendEmptyMessage(3);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void drainEncoder() throws IOException {
            while (!audioStep()) {
            }
        }

        public void handleAudioPause() {
            this.pausing = true;
            this.oncePauseTime = System.nanoTime();
        }

        public void handleAudioResume() {
            this.oncePauseTime = System.nanoTime() - this.oncePauseTime;
            this.pauseDelayTime += this.oncePauseTime;
            this.pausing = false;
        }

        public void handleStopRecord() {
            this.isRecording = false;
        }

        private boolean audioStep() throws IOException {
            int index = VideoEncoderCore.this.mAudioEnc.dequeueInputBuffer(0);
            if (index >= 0) {
                ByteBuffer buffer = getInputBuffer(VideoEncoderCore.this.mAudioEnc, index);
                buffer.clear();
                int length = VideoEncoderCore.this.mRecorder.read(buffer, VideoEncoderCore.this.bufferSize);
                if (length > 0) {
                    if (this.baseTimeStamp != -1) {
                        long nano = System.nanoTime();
                        long time = ((nano - this.baseTimeStamp) - this.pauseDelayTime) / 1000;
                        PrintStream printStream = System.out;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("TimeStampAudio=");
                        stringBuilder.append(time);
                        stringBuilder.append(";nanoTime=");
                        stringBuilder.append(nano);
                        stringBuilder.append(";baseTimeStamp=");
                        stringBuilder.append(this.baseTimeStamp);
                        stringBuilder.append(";pauseDelay=");
                        stringBuilder.append(this.pauseDelayTime);
                        printStream.println(stringBuilder.toString());
                        VideoEncoderCore.this.mAudioEnc.queueInputBuffer(index, 0, length, time, this.isRecording ? 0 : 4);
                    } else {
                        VideoEncoderCore.this.mAudioEnc.queueInputBuffer(index, 0, length, 0, this.isRecording ? 0 : 4);
                    }
                }
            }
            BufferInfo mInfo = new BufferInfo();
            while (true) {
                BufferInfo mInfo2 = mInfo;
                int outIndex = VideoEncoderCore.this.mAudioEnc.dequeueOutputBuffer(mInfo2, 0);
                if (outIndex >= 0) {
                    if ((mInfo2.flags & 4) != 0) {
                        Log.e(VideoEncoderCore.TAG, "audio end");
                        VideoEncoderCore.this.mAudioEnc.releaseOutputBuffer(outIndex, false);
                        return true;
                    }
                    ByteBuffer buffer2 = getOutputBuffer(VideoEncoderCore.this.mAudioEnc, outIndex);
                    buffer2.position(mInfo2.offset);
                    if (VideoEncoderCore.this.mMuxerStarted && mInfo2.presentationTimeUs > 0) {
                        try {
                            if (!VideoEncoderCore.this.mStopState) {
                                VideoEncoderCore.this.mMuxer.writeSampleData(VideoEncoderCore.this.mAudioTrackIndex, buffer2, mInfo2);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    VideoEncoderCore.this.mAudioEnc.releaseOutputBuffer(outIndex, false);
                } else if (outIndex != -1 && outIndex == -2) {
                    synchronized (VideoEncoderCore.this.lock) {
                        VideoEncoderCore.this.mAudioTrackIndex = VideoEncoderCore.this.mMuxer.addTrack(VideoEncoderCore.this.mAudioEnc.getOutputFormat());
                        String str = VideoEncoderCore.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("add audio track-->");
                        stringBuilder2.append(VideoEncoderCore.this.mAudioTrackIndex);
                        Log.e(str, stringBuilder2.toString());
                        if (VideoEncoderCore.this.mAudioTrackIndex >= 0 && VideoEncoderCore.this.mVideoTrackIndex >= 0) {
                            VideoEncoderCore.this.mMuxer.start();
                            VideoEncoderCore.this.mMuxerStarted = true;
                        }
                    }
                }
                if (outIndex < 0) {
                    return false;
                }
                mInfo = mInfo2;
            }
        }

        private ByteBuffer getInputBuffer(MediaCodec codec, int index) {
            if (VERSION.SDK_INT >= 21) {
                return codec.getInputBuffer(index);
            }
            return codec.getInputBuffers()[index];
        }

        private ByteBuffer getOutputBuffer(MediaCodec codec, int index) {
            if (VERSION.SDK_INT >= 21) {
                return codec.getOutputBuffer(index);
            }
            return codec.getOutputBuffers()[index];
        }
    }

    static class AudioHandler extends Handler {
        private WeakReference<AudioEncoder> encoderWeakReference;

        public AudioHandler(AudioEncoder encoder) {
            this.encoderWeakReference = new WeakReference(encoder);
        }

        public void handleMessage(Message msg) {
            int what = msg.what;
            AudioEncoder audioEncoder = (AudioEncoder) this.encoderWeakReference.get();
            if (audioEncoder != null) {
                switch (what) {
                    case 0:
                        audioEncoder.handleStartRecord();
                        break;
                    case 1:
                        audioEncoder.handleStopRecord();
                        break;
                    case 2:
                        audioEncoder.handleAudioStep();
                        break;
                    case 3:
                        Looper.myLooper().quit();
                        break;
                    case 4:
                        audioEncoder.handleAudioPause();
                        break;
                    case 5:
                        audioEncoder.handleAudioResume();
                        break;
                }
            }
        }
    }

    public VideoEncoderCore(int width, int height, int bitRate, String path, int angle) throws IOException {
        MediaFormat aFormat = MediaFormat.createAudioFormat(this.audioMime, this.sampleRate, this.channelCount);
        aFormat.setInteger("aac-profile", 2);
        aFormat.setInteger("bitrate", this.audioRate);
        this.mAudioEnc = MediaCodec.createEncoderByType(this.audioMime);
        this.mAudioEnc.configure(aFormat, null, null, 1);
        this.bufferSize = AudioRecord.getMinBufferSize(this.sampleRate, this.channelConfig, this.audioFormat);
        this.mRecorder = new AudioRecord(1, this.sampleRate, this.channelConfig, this.audioFormat, this.bufferSize);
        this.mAudioEnc.start();
        this.mRecorder.startRecording();
        this.mBufferInfo = new BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger("color-format", 2130708361);
        format.setInteger("bitrate", bitRate);
        format.setInteger("frame-rate", 30);
        format.setInteger("i-frame-interval", 1);
        this.mVideoEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        this.mVideoEncoder.configure(format, null, null, 1);
        this.mInputSurface = this.mVideoEncoder.createInputSurface();
        this.mVideoEncoder.start();
        this.mMuxer = new MediaMuxer(path, 0);
        this.mMuxer.setOrientationHint(angle);
        this.mVideoTrackIndex = -1;
        this.mAudioTrackIndex = -1;
        this.mMuxerStarted = false;
        this.audioRecorder = new AudioEncoder();
        this.audioThread = new Thread(this.audioRecorder);
        this.audioThread.start();
    }

    public Surface getInputSurface() {
        return this.mInputSurface;
    }

    public void release() {
        if (this.mVideoEncoder != null) {
            this.mVideoEncoder.stop();
            this.mVideoEncoder.release();
            this.mVideoEncoder = null;
        }
        if (this.mAudioEnc != null) {
            this.mAudioEnc.stop();
            this.mAudioEnc.release();
            this.mAudioEnc = null;
        }
        if (this.mRecorder != null) {
            this.mRecorder.stop();
            this.mRecorder.release();
            this.mRecorder = null;
        }
        if (this.mMuxer != null) {
            this.mMuxer.stop();
            this.mMuxer.release();
            this.mMuxer = null;
        }
    }

    public void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            this.mVideoEncoder.signalEndOfInputStream();
        }
        ByteBuffer[] encoderOutputBuffers = this.mVideoEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = this.mVideoEncoder.dequeueOutputBuffer(this.mBufferInfo, 10000);
            if (encoderStatus == -1) {
                if (!endOfStream) {
                    return;
                }
            } else if (encoderStatus == -3) {
                encoderOutputBuffers = this.mVideoEncoder.getOutputBuffers();
            } else if (encoderStatus == -2) {
                synchronized (this.lock) {
                    if (this.mMuxerStarted) {
                        throw new RuntimeException("format changed twice");
                    }
                    MediaFormat newFormat = this.mVideoEncoder.getOutputFormat();
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("encoder output format changed: ");
                    stringBuilder.append(newFormat);
                    Log.d(str, stringBuilder.toString());
                    this.mVideoTrackIndex = this.mMuxer.addTrack(newFormat);
                    if (this.mVideoTrackIndex >= 0 && this.mAudioTrackIndex >= 0) {
                        this.mMuxer.start();
                        this.mMuxerStarted = true;
                    }
                }
            } else if (encoderStatus < 0) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unexpected result from encoder.dequeueOutputBuffer: ");
                stringBuilder2.append(encoderStatus);
                Log.w(str2, stringBuilder2.toString());
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData != null) {
                    if ((this.mBufferInfo.flags & 2) != 0) {
                        this.mBufferInfo.size = 0;
                    }
                    if (!(this.mBufferInfo.size == 0 || !this.mMuxerStarted || this.mStopState)) {
                        this.mMuxer.writeSampleData(this.mVideoTrackIndex, encodedData, this.mBufferInfo);
                    }
                    this.mVideoEncoder.releaseOutputBuffer(encoderStatus, false);
                    if ((this.mBufferInfo.flags & 4) != 0) {
                        if (!endOfStream) {
                            Log.w(TAG, "reached end of stream unexpectedly");
                            return;
                        }
                        return;
                    }
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("encoderOutputBuffer ");
                    stringBuilder3.append(encoderStatus);
                    stringBuilder3.append(" was null");
                    throw new RuntimeException(stringBuilder3.toString());
                }
            }
        }
    }

    public void stopAudRecord() {
        this.mStopState = true;
        this.audioRecorder.stopRecord();
        if (this.audioThread != null) {
            try {
                this.audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void startRecord() {
        this.mStopState = false;
        this.audioRecorder.startRecord();
    }

    public void pauseRecording() {
        this.audioRecorder.pause();
    }

    public void resumeRecording() {
        this.audioRecorder.resume();
    }
}
