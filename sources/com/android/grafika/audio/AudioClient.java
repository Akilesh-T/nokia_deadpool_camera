package com.android.grafika.audio;

import android.media.AudioRecord;
import android.util.Log;
import com.android.grafika.gles.MediaMuxerWrapper;

public class AudioClient {
    private byte[] audioBuffer;
    private AudioRecord audioRecord;
    private AudioRecordThread audioRecordThread;
    MediaMakerConfig mediaMakerConfig;
    private AudioCore softAudioCore;
    private final Object syncOp = new Object();

    class AudioRecordThread extends Thread {
        private boolean isRunning;

        AudioRecordThread() {
            this.isRunning = true;
            this.isRunning = true;
        }

        public void quit() {
            this.isRunning = false;
        }

        public void run() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AudioRecordThread,tid=");
            stringBuilder.append(Thread.currentThread().getId());
            Log.d("", stringBuilder.toString());
            while (this.isRunning) {
                Log.e("+++++++++++++++", "audio  ---------------------------->  ");
                int size = AudioClient.this.audioRecord.read(AudioClient.this.audioBuffer, 0, AudioClient.this.audioBuffer.length);
                if (this.isRunning && AudioClient.this.softAudioCore != null && size > 0) {
                    AudioClient.this.softAudioCore.queueAudio(AudioClient.this.audioBuffer);
                }
            }
        }
    }

    public AudioClient(MediaMakerConfig parameters) {
        this.mediaMakerConfig = parameters;
    }

    public boolean prepare(RecordConfig recordConfig) {
        synchronized (this.syncOp) {
            this.mediaMakerConfig.audioBufferQueueNum = 5;
            this.softAudioCore = new AudioCore(this.mediaMakerConfig);
            if (this.softAudioCore.prepare(recordConfig)) {
                this.mediaMakerConfig.audioRecoderFormat = 2;
                this.mediaMakerConfig.audioRecoderChannelConfig = 16;
                this.mediaMakerConfig.audioRecoderSliceSize = this.mediaMakerConfig.mediacodecAACSampleRate / 10;
                this.mediaMakerConfig.audioRecoderBufferSize = this.mediaMakerConfig.audioRecoderSliceSize * 2;
                this.mediaMakerConfig.audioRecoderSource = 0;
                this.mediaMakerConfig.audioRecoderSampleRate = this.mediaMakerConfig.mediacodecAACSampleRate;
                prepareAudio();
                return true;
            }
            Log.e("", "AudioClient,prepare");
            return false;
        }
    }

    public boolean startRecording(MediaMuxerWrapper muxer) {
        synchronized (this.syncOp) {
            this.softAudioCore.startRecording(muxer);
            this.audioRecord.startRecording();
            this.audioRecordThread = new AudioRecordThread();
            this.audioRecordThread.start();
            Log.d("", "AudioClient,start()");
        }
        return true;
    }

    public boolean stopRecording() {
        synchronized (this.syncOp) {
            if (this.audioRecordThread != null) {
                this.audioRecordThread.quit();
                try {
                    this.audioRecordThread.join();
                } catch (InterruptedException e) {
                }
                this.audioRecordThread = null;
            }
            this.softAudioCore.stop();
            this.audioRecord.stop();
        }
        return true;
    }

    public boolean destroy() {
        synchronized (this.syncOp) {
            this.audioRecord.release();
        }
        return true;
    }

    public void setSoftAudioFilter(BaseSoftAudioFilter baseSoftAudioFilter) {
        this.softAudioCore.setAudioFilter(baseSoftAudioFilter);
    }

    public BaseSoftAudioFilter acquireSoftAudioFilter() {
        return this.softAudioCore.acquireAudioFilter();
    }

    public void releaseSoftAudioFilter() {
        this.softAudioCore.releaseAudioFilter();
    }

    private boolean prepareAudio() {
        this.audioRecord = new AudioRecord(this.mediaMakerConfig.audioRecoderSource, this.mediaMakerConfig.audioRecoderSampleRate, this.mediaMakerConfig.audioRecoderChannelConfig, this.mediaMakerConfig.audioRecoderFormat, AudioRecord.getMinBufferSize(this.mediaMakerConfig.audioRecoderSampleRate, this.mediaMakerConfig.audioRecoderChannelConfig, this.mediaMakerConfig.audioRecoderFormat) * 5);
        this.audioBuffer = new byte[this.mediaMakerConfig.audioRecoderBufferSize];
        if (1 != this.audioRecord.getState()) {
            Log.e("", "audioRecord.getState()!=AudioRecord.STATE_INITIALIZED!");
            return false;
        } else if (this.audioRecord.setPositionNotificationPeriod(this.mediaMakerConfig.audioRecoderSliceSize) == 0) {
            return true;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(");
            stringBuilder.append(this.mediaMakerConfig.audioRecoderSliceSize);
            stringBuilder.append(")");
            Log.e("", stringBuilder.toString());
            return false;
        }
    }
}
