package com.android.grafika.audio;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import com.android.grafika.gles.MediaMuxerWrapper;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AudioCore {
    private BaseSoftAudioFilter audioFilter;
    private AudioFilterHandler audioFilterHandler;
    private HandlerThread audioFilterHandlerThread;
    private AudioSenderThread audioSenderThread;
    private MediaCodec dstAudioEncoder;
    private MediaFormat dstAudioFormat;
    private AudioBuff filteredAudioBuff;
    private int lastAudioQueueBuffIndex;
    private Lock lockAudioFilter = null;
    MediaMakerConfig mediaMakerConfig;
    private AudioBuff orignAudioBuff;
    private AudioBuff[] orignAudioBuffs;
    private final Object syncOp = new Object();

    private class AudioFilterHandler extends Handler {
        public static final int FILTER_LOCK_TOLERATION = 3;
        public static final int WHAT_INCOMING_BUFF = 1;
        private int sequenceNum = 0;

        AudioFilterHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Message message = msg;
            if (message.what == 1) {
                this.sequenceNum++;
                int targetIndex = message.arg1;
                long nowTimeMs = SystemClock.uptimeMillis();
                System.arraycopy(AudioCore.this.orignAudioBuffs[targetIndex].buff, 0, AudioCore.this.orignAudioBuff.buff, 0, AudioCore.this.orignAudioBuff.buff.length);
                AudioCore.this.orignAudioBuffs[targetIndex].isReadyToFill = true;
                boolean filtered = false;
                if (lockAudioFilter()) {
                    filtered = AudioCore.this.audioFilter.onFrame(AudioCore.this.orignAudioBuff.buff, AudioCore.this.filteredAudioBuff.buff, nowTimeMs, this.sequenceNum);
                    unlockAudioFilter();
                } else {
                    System.arraycopy(AudioCore.this.orignAudioBuffs[targetIndex].buff, 0, AudioCore.this.orignAudioBuff.buff, 0, AudioCore.this.orignAudioBuff.buff.length);
                    AudioCore.this.orignAudioBuffs[targetIndex].isReadyToFill = true;
                }
                int eibIndex = AudioCore.this.dstAudioEncoder.dequeueInputBuffer(-1);
                if (eibIndex >= 0) {
                    ByteBuffer dstAudioEncoderIBuffer = AudioCore.this.dstAudioEncoder.getInputBuffers()[eibIndex];
                    dstAudioEncoderIBuffer.position(0);
                    dstAudioEncoderIBuffer.put((filtered ? AudioCore.this.filteredAudioBuff : AudioCore.this.orignAudioBuff).buff, 0, AudioCore.this.orignAudioBuff.buff.length);
                    AudioCore.this.dstAudioEncoder.queueInputBuffer(eibIndex, 0, AudioCore.this.orignAudioBuff.buff.length, nowTimeMs * 1000, 0);
                } else {
                    Log.d("", "dstAudioEncoder.dequeueInputBuffer(-1)<0");
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AudioFilterHandler,ProcessTime:");
                stringBuilder.append(System.currentTimeMillis() - nowTimeMs);
                Log.d("", stringBuilder.toString());
            }
        }

        private boolean lockAudioFilter() {
            try {
                if (!AudioCore.this.lockAudioFilter.tryLock(3, TimeUnit.MILLISECONDS)) {
                    return false;
                }
                if (AudioCore.this.audioFilter != null) {
                    return true;
                }
                AudioCore.this.lockAudioFilter.unlock();
                return false;
            } catch (InterruptedException e) {
                return false;
            }
        }

        private void unlockAudioFilter() {
            AudioCore.this.lockAudioFilter.unlock();
        }
    }

    public AudioCore(MediaMakerConfig parameters) {
        this.mediaMakerConfig = parameters;
        this.lockAudioFilter = new ReentrantLock(false);
    }

    public void queueAudio(byte[] rawAudioFrame) {
        int targetIndex = (this.lastAudioQueueBuffIndex + 1) % this.orignAudioBuffs.length;
        if (this.orignAudioBuffs[targetIndex].isReadyToFill) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("queueAudio,accept ,targetIndex");
            stringBuilder.append(targetIndex);
            Log.d("", stringBuilder.toString());
            System.arraycopy(rawAudioFrame, 0, this.orignAudioBuffs[targetIndex].buff, 0, this.mediaMakerConfig.audioRecoderBufferSize);
            this.orignAudioBuffs[targetIndex].isReadyToFill = false;
            this.lastAudioQueueBuffIndex = targetIndex;
            this.audioFilterHandler.sendMessage(this.audioFilterHandler.obtainMessage(1, targetIndex, 0));
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("queueAudio,abandon,targetIndex");
        stringBuilder2.append(targetIndex);
        Log.d("", stringBuilder2.toString());
    }

    public boolean prepare(RecordConfig resConfig) {
        synchronized (this.syncOp) {
            this.mediaMakerConfig.mediacodecAACProfile = 2;
            this.mediaMakerConfig.mediacodecAACSampleRate = 44100;
            this.mediaMakerConfig.mediacodecAACChannelCount = 1;
            this.mediaMakerConfig.mediacodecAACBitRate = 32768;
            this.mediaMakerConfig.mediacodecAACMaxInputSize = 8820;
            this.dstAudioFormat = new MediaFormat();
            this.dstAudioEncoder = MediaCodecHelper.createAudioMediaCodec(this.mediaMakerConfig, this.dstAudioFormat);
            int i = 0;
            if (this.dstAudioEncoder == null) {
                Log.e("", "create Audio MediaCodec failed");
                return false;
            }
            int audioQueueNum = this.mediaMakerConfig.audioBufferQueueNum;
            int orignAudioBuffSize = this.mediaMakerConfig.mediacodecAACSampleRate / 5;
            this.orignAudioBuffs = new AudioBuff[audioQueueNum];
            while (i < audioQueueNum) {
                this.orignAudioBuffs[i] = new AudioBuff(2, orignAudioBuffSize);
                i++;
            }
            this.orignAudioBuff = new AudioBuff(2, orignAudioBuffSize);
            this.filteredAudioBuff = new AudioBuff(2, orignAudioBuffSize);
            return true;
        }
    }

    public void startRecording(MediaMuxerWrapper muxer) {
        synchronized (this.syncOp) {
            try {
                for (AudioBuff buff : this.orignAudioBuffs) {
                    buff.isReadyToFill = true;
                }
                if (this.dstAudioEncoder == null) {
                    this.dstAudioEncoder = MediaCodec.createEncoderByType(this.dstAudioFormat.getString("mime"));
                }
                this.dstAudioEncoder.configure(this.dstAudioFormat, null, null, 1);
                this.dstAudioEncoder.start();
                this.lastAudioQueueBuffIndex = 0;
                this.audioFilterHandlerThread = new HandlerThread("audioFilterHandlerThread");
                this.audioSenderThread = new AudioSenderThread("AudioSenderThread", this.dstAudioEncoder, muxer);
                this.audioFilterHandlerThread.start();
                this.audioSenderThread.start();
                this.audioFilterHandler = new AudioFilterHandler(this.audioFilterHandlerThread.getLooper());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        synchronized (this.syncOp) {
            this.audioFilterHandler.removeCallbacksAndMessages(null);
            if (this.audioFilterHandlerThread != null) {
                this.audioFilterHandlerThread.quit();
            }
            try {
                if (this.audioFilterHandlerThread != null) {
                    this.audioFilterHandlerThread.join();
                }
                if (this.audioSenderThread != null) {
                    this.audioSenderThread.quit();
                    this.audioSenderThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.audioFilterHandlerThread = null;
            this.audioSenderThread = null;
            if (this.dstAudioEncoder != null) {
                this.dstAudioEncoder.stop();
                this.dstAudioEncoder.release();
                this.dstAudioEncoder = null;
            }
        }
    }

    public BaseSoftAudioFilter acquireAudioFilter() {
        this.lockAudioFilter.lock();
        return this.audioFilter;
    }

    public void releaseAudioFilter() {
        this.lockAudioFilter.unlock();
    }

    public void setAudioFilter(BaseSoftAudioFilter baseSoftAudioFilter) {
        this.lockAudioFilter.lock();
        if (this.audioFilter != null) {
            this.audioFilter.onDestroy();
        }
        this.audioFilter = baseSoftAudioFilter;
        if (this.audioFilter != null) {
            this.audioFilter.onInit(this.mediaMakerConfig.mediacodecAACSampleRate / 5);
        }
        this.lockAudioFilter.unlock();
    }

    public void destroy() {
        synchronized (this.syncOp) {
            this.lockAudioFilter.lock();
            if (this.audioFilter != null) {
                this.audioFilter.onDestroy();
            }
            this.lockAudioFilter.unlock();
        }
    }
}
