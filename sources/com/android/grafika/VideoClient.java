package com.android.grafika;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.util.Log;
import com.android.grafika.TextureMovieEncoder.EncoderConfig;
import com.android.grafika.audio.AudioClient;
import com.android.grafika.audio.BaseSoftAudioFilter;
import com.android.grafika.audio.MediaMakerConfig;
import com.android.grafika.gles.MediaMuxerWrapper;
import java.io.IOException;

public class VideoClient {
    private Object SyncOp = new Object();
    private boolean isFront = false;
    private AudioClient mAudioClient;
    private MediaMuxerWrapper mMuxer = null;
    private int mRenderH = 1920;
    private int mRenderW = 1080;
    private TextureMovieEncoder mVideoEncoder;

    public VideoClient(Context context) {
        this.mVideoEncoder = new TextureMovieEncoder(context);
    }

    public void initAudioPath(String path) {
        MediaMakerConfig mediaMakerConfig = new MediaMakerConfig();
        mediaMakerConfig.saveVideoEnable = true;
        mediaMakerConfig.saveVideoPath = path;
        this.mAudioClient = new AudioClient(mediaMakerConfig);
        this.mAudioClient.prepare(null);
        this.mAudioClient.setSoftAudioFilter(new BaseSoftAudioFilter());
    }

    public void updateSharedContext(EGLContext context) {
        this.mVideoEncoder.updateSharedContext(context);
    }

    public void setTextureId(int textureId) {
        this.mVideoEncoder.setTextureId(textureId);
    }

    public void frameAvailable(SurfaceTexture texture) {
        this.mVideoEncoder.frameAvailable(texture);
    }

    public void startRecording(EncoderConfig config) {
        synchronized (this.SyncOp) {
            prepareMuxer(config.mOutputFile.getPath());
            config.mediaMuxerWrapper = this.mMuxer;
            this.mVideoEncoder.startRecording(config);
            this.mAudioClient.startRecording(this.mMuxer);
            Log.d("", "RecorderClient,startRecording()");
        }
    }

    public void stopRecording() {
        synchronized (this.SyncOp) {
            this.mVideoEncoder.stopRecording();
            this.mAudioClient.stopRecording();
            Log.d("", "RecorderClient,stopRecording()");
        }
    }

    public void destroy() {
        synchronized (this.SyncOp) {
            this.mAudioClient = null;
            this.mVideoEncoder = null;
            Log.d("", "RecorderClient,destroy()");
        }
    }

    private void prepareMuxer(String path) {
        try {
            this.mMuxer = new MediaMuxerWrapper(path);
            this.mMuxer.setTrackCount(2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRecording() {
        return this.mVideoEncoder.isRecording();
    }

    public void stopRecorder() {
        this.mVideoEncoder.stopRecording();
    }
}
