package com.android.grafika;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import com.android.grafika.gles.MediaMuxerWrapper;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoderCore implements IVideoEncoderCore {
    private static final int FRAME_RATE = 20;
    private static final int IFRAME_INTERVAL = 5;
    private static final String MIME_TYPE = "video/avc";
    private static final String TAG = "VideoEncoderCore";
    private static final boolean VERBOSE = false;
    private BufferInfo mBufferInfo = new BufferInfo();
    private MediaCodec mEncoder;
    private Surface mInputSurface;
    private MediaMuxerWrapper mMuxer;
    private boolean mMuxerStarted;
    private int mTrackIndex;
    private int m_framerate;
    long prevPresentationTimes;

    public VideoEncoderCore(int width, int height, int bitRate, File outputFile, MediaMuxerWrapper muxer) throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        this.m_framerate = bitRate;
        format.setInteger("color-format", 2130708361);
        format.setInteger("bitrate", bitRate);
        format.setInteger("frame-rate", 20);
        format.setInteger("i-frame-interval", 5);
        this.mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        this.mEncoder.configure(format, null, null, 1);
        this.mInputSurface = this.mEncoder.createInputSurface();
        this.mEncoder.start();
        this.mMuxer = muxer;
        this.mTrackIndex = -1;
        this.mMuxerStarted = false;
    }

    public Surface getInputSurface() {
        return this.mInputSurface;
    }

    public void release() {
        if (this.mEncoder != null) {
            this.mEncoder.stop();
            this.mEncoder.release();
            this.mEncoder = null;
        }
        if (this.mMuxer != null) {
            this.mMuxer.stop();
            this.mMuxer = null;
        }
    }

    public void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            this.mEncoder.signalEndOfInputStream();
        }
        ByteBuffer[] encoderOutputBuffers = this.mEncoder.getOutputBuffers();
        while (true) {
            int encoderStatus = this.mEncoder.dequeueOutputBuffer(this.mBufferInfo, 10000);
            StringBuilder stringBuilder;
            if (encoderStatus == -1) {
                if (!endOfStream) {
                    return;
                }
            } else if (encoderStatus == -3) {
                encoderOutputBuffers = this.mEncoder.getOutputBuffers();
            } else if (encoderStatus == -2) {
                if (this.mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = this.mEncoder.getOutputFormat();
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("encoder output format changed: ");
                stringBuilder.append(newFormat);
                Log.d(str, stringBuilder.toString());
                this.mTrackIndex = this.mMuxer.addTrack(newFormat);
                this.mMuxer.start();
                this.mMuxerStarted = true;
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
                    if (this.mBufferInfo.size != 0) {
                        if (this.mMuxerStarted) {
                            encodedData.position(this.mBufferInfo.offset);
                            encodedData.limit(this.mBufferInfo.offset + this.mBufferInfo.size);
                            this.mMuxer.writeSampleData(this.mTrackIndex, encodedData, this.mBufferInfo);
                        } else {
                            throw new RuntimeException("muxer hasn't started");
                        }
                    }
                    this.mEncoder.releaseOutputBuffer(encoderStatus, false);
                    if ((this.mBufferInfo.flags & 4) != 0) {
                        if (!endOfStream) {
                            Log.w(TAG, "reached end of stream unexpectedly");
                            return;
                        }
                        return;
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("encoderOutputBuffer ");
                    stringBuilder.append(encoderStatus);
                    stringBuilder.append(" was null");
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
        }
    }

    private long getPTSUs() {
        long result = System.nanoTime() / 1000;
        if (result < this.prevPresentationTimes) {
            return result + (this.prevPresentationTimes - result);
        }
        return result;
    }
}
