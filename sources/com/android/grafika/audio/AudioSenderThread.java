package com.android.grafika.audio;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.util.Log;
import com.android.grafika.gles.MediaMuxerWrapper;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class AudioSenderThread extends Thread {
    private static final long WAIT_TIME = 5000;
    private MediaCodec dstAudioEncoder;
    private BufferInfo eInfo;
    protected boolean mMuxerStarted;
    protected int mTrackIndex;
    protected WeakReference<MediaMuxerWrapper> mWeakMuxer;
    private long prevOutputPTSUs;
    private boolean shouldQuit;
    private long startTime;

    AudioSenderThread(String name, MediaCodec encoder, MediaMuxerWrapper muxer) {
        super(name);
        this.startTime = 0;
        this.shouldQuit = false;
        this.mMuxerStarted = false;
        this.prevOutputPTSUs = 0;
        this.eInfo = new BufferInfo();
        this.startTime = 0;
        this.dstAudioEncoder = encoder;
        this.mWeakMuxer = new WeakReference(muxer);
    }

    /* Access modifiers changed, original: 0000 */
    public void quit() {
        this.shouldQuit = true;
        interrupt();
        if (this.mMuxerStarted) {
            MediaMuxerWrapper muxer = this.mWeakMuxer != null ? (MediaMuxerWrapper) this.mWeakMuxer.get() : null;
            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (Exception e) {
                    Log.e("AudioSenderThread", "failed stopping muxer", e);
                }
            }
        }
    }

    public void run() {
        MediaMuxerWrapper muxer = this.mWeakMuxer != null ? (MediaMuxerWrapper) this.mWeakMuxer.get() : null;
        boolean isMuxerEnable = muxer != null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("muxer enable:");
        stringBuilder.append(isMuxerEnable);
        Log.w("AudioSenderThread", stringBuilder.toString());
        while (!this.shouldQuit) {
            int eobIndex = this.dstAudioEncoder.dequeueOutputBuffer(this.eInfo, WAIT_TIME);
            StringBuilder stringBuilder2;
            switch (eobIndex) {
                case -3:
                    Log.d("", "AudioSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case -2:
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("AudioSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:");
                    stringBuilder2.append(this.dstAudioEncoder.getOutputFormat().toString());
                    Log.d("", stringBuilder2.toString());
                    if (!isMuxerEnable) {
                        break;
                    }
                    this.mTrackIndex = muxer.addTrack(this.dstAudioEncoder.getOutputFormat());
                    muxer.start();
                    this.mMuxerStarted = true;
                    break;
                case -1:
                    break;
                default:
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("AudioSenderThread,MediaCode,eobIndex=");
                    stringBuilder2.append(eobIndex);
                    Log.d("", stringBuilder2.toString());
                    if (this.startTime == 0) {
                        this.startTime = this.eInfo.presentationTimeUs / 1000;
                    }
                    if (!(this.eInfo.flags == 2 || this.eInfo.size == 0)) {
                        ByteBuffer realData = this.dstAudioEncoder.getOutputBuffers()[eobIndex];
                        realData.position(this.eInfo.offset);
                        realData.limit(this.eInfo.offset + this.eInfo.size);
                        if (isMuxerEnable && this.mMuxerStarted) {
                            this.eInfo.presentationTimeUs = getPTSUs();
                            muxer.writeSampleData(this.mTrackIndex, realData, this.eInfo);
                            this.prevOutputPTSUs = this.eInfo.presentationTimeUs;
                        }
                    }
                    this.dstAudioEncoder.releaseOutputBuffer(eobIndex, false);
                    break;
            }
        }
        this.eInfo = null;
    }

    /* Access modifiers changed, original: protected */
    public long getPTSUs() {
        long result = System.nanoTime() / 1000;
        if (result < this.prevOutputPTSUs) {
            return result + (this.prevOutputPTSUs - result);
        }
        return result;
    }
}
