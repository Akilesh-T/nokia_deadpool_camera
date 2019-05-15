package com.morphoinc.utils.VideoRec;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;
import java.io.IOException;
import java.nio.ByteBuffer;

abstract class BaseVideoRecoder {
    static final int IFRAME_INTERVAL = 1;
    static final String MIME_TYPE = "video/avc";
    private static final String TAG = "MORPHO";
    private static final boolean VERBOSE = false;
    private int mBitRate = -1;
    private BufferInfo mBufferInfo;
    private int mColorFormat = -1;
    MediaCodec mEncoder;
    private final float mFrameRate;
    private int mHeight = -1;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted;
    private final String mOutputPath;
    Surface mSurface;
    private int mTrackIndex;
    private int mWidth = -1;

    public abstract void end();

    BaseVideoRecoder(int width, int height, int bitrate, float frame_rate, int color_format, String output_path) {
        this.mWidth = width;
        this.mHeight = height;
        this.mBitRate = bitrate;
        this.mFrameRate = frame_rate;
        this.mColorFormat = color_format;
        this.mOutputPath = output_path;
        prepareEncoder();
    }

    public String getOutputFilePath() {
        return this.mOutputPath;
    }

    private void prepareEncoder() {
        this.mBufferInfo = new BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, this.mWidth, this.mHeight);
        format.setInteger("color-format", this.mColorFormat);
        format.setInteger("bitrate", this.mBitRate);
        format.setFloat("frame-rate", this.mFrameRate);
        format.setInteger("i-frame-interval", 1);
        try {
            this.mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            this.mEncoder.configure(format, null, null, 1);
            if (this.mColorFormat == 2130708361) {
                this.mSurface = this.mEncoder.createInputSurface();
            }
            this.mEncoder.start();
            try {
                this.mMuxer = new MediaMuxer(this.mOutputPath, 0);
                this.mTrackIndex = -1;
                this.mMuxerStarted = false;
            } catch (IOException ioe) {
                throw new RuntimeException("MediaMuxer creation failed", ioe);
            }
        } catch (IOException ioe2) {
            throw new RuntimeException("Encoder creation failed", ioe2);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void drainEncoder(boolean endOfStream) {
        int indexInput;
        if (endOfStream) {
            if (this.mColorFormat == 2130708361) {
                this.mEncoder.signalEndOfInputStream();
            } else {
                do {
                    indexInput = this.mEncoder.dequeueInputBuffer(0);
                    if (indexInput >= 0) {
                        this.mEncoder.queueInputBuffer(indexInput, 0, 0, 0, 4);
                    }
                    int indexOutput = this.mEncoder.dequeueOutputBuffer(this.mBufferInfo, -1);
                    if (indexOutput >= 0) {
                        try {
                            ByteBuffer buffer = this.mEncoder.getOutputBuffer(indexOutput);
                            buffer.position(buffer.position() + this.mBufferInfo.offset);
                            buffer.limit(buffer.position() + this.mBufferInfo.size);
                            this.mMuxer.writeSampleData(this.mTrackIndex, buffer, this.mBufferInfo);
                            buffer.clear();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        this.mEncoder.releaseOutputBuffer(indexOutput, false);
                    }
                } while ((this.mBufferInfo.flags & 4) == 0);
                return;
            }
        }
        while (true) {
            indexInput = this.mEncoder.dequeueOutputBuffer(this.mBufferInfo, 1000);
            StringBuilder stringBuilder;
            if (indexInput == -1) {
                if (!endOfStream) {
                    break;
                }
            } else if (indexInput == -3) {
                continue;
            } else if (indexInput == -2) {
                if (this.mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = this.mEncoder.getOutputFormat();
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("encoder output format changed: ");
                stringBuilder2.append(newFormat);
                Log.d(str, stringBuilder2.toString());
                this.mTrackIndex = this.mMuxer.addTrack(newFormat);
                this.mMuxer.start();
                this.mMuxerStarted = true;
            } else if (indexInput < 0) {
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected result from encoder.dequeueOutputBuffer: ");
                stringBuilder.append(indexInput);
                Log.w(str2, stringBuilder.toString());
            } else {
                ByteBuffer encodedData = this.mEncoder.getOutputBuffer(indexInput);
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
                    this.mEncoder.releaseOutputBuffer(indexInput, false);
                    if ((this.mBufferInfo.flags & 4) != 0) {
                        if (!endOfStream) {
                            Log.w(TAG, "reached end of stream unexpectedly");
                        }
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("encoderOutputBuffer ");
                    stringBuilder.append(indexInput);
                    stringBuilder.append(" was null");
                    throw new RuntimeException(stringBuilder.toString());
                }
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void releaseEncoder() {
        if (this.mEncoder != null) {
            this.mEncoder.stop();
            this.mEncoder.release();
            this.mEncoder = null;
        }
        if (this.mMuxer != null) {
            try {
                this.mMuxer.stop();
                this.mMuxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.mMuxer = null;
        }
    }
}
