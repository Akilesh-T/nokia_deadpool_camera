package com.hmdglobal.app.camera.encoder;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.encoder.Muxer.MuxerBuffer;
import java.io.File;
import java.io.IOException;

public class MicroVideoMuxer extends Muxer {
    private static final int MSG_START_MUXER = 0;
    private static final int MSG_STOP_MUXER = 1;
    private static final int MSG_WRITE_DATA = 2;
    private Tag TAG = new Tag("MicroVideoMuxer");
    private MuxerHandler mHandler;
    private MediaMuxer mMuxer;
    private String mPath;
    private boolean mStarted;

    private class MuxerHandler extends Handler {
        public MuxerHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (MicroVideoMuxer.this.mMuxer != null) {
                        MicroVideoMuxer.this.mMuxer.start();
                        break;
                    }
                    break;
                case 1:
                    if (MicroVideoMuxer.this.mMuxer != null) {
                        MicroVideoMuxer.this.mMuxer.stop();
                        MicroVideoMuxer.this.mMuxer.release();
                        MicroVideoMuxer.this.mMuxer = null;
                    }
                    MicroVideoMuxer.this.mStarted = false;
                    break;
                case 2:
                    MuxerBuffer buffer = msg.obj;
                    buffer.bufferData.position(buffer.bufferInfo.offset);
                    buffer.bufferData.limit(buffer.bufferInfo.offset + buffer.bufferInfo.size);
                    Tag access$200 = MicroVideoMuxer.this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("write data track for ");
                    stringBuilder.append(buffer.track);
                    stringBuilder.append(" bufferData is ");
                    stringBuilder.append(buffer.bufferData.toString());
                    Log.w(access$200, stringBuilder.toString());
                    MicroVideoMuxer.this.mMuxer.writeSampleData(buffer.track, buffer.bufferData, buffer.bufferInfo);
                    break;
            }
            super.handleMessage(msg);
        }
    }

    public MicroVideoMuxer() {
        HandlerThread thread = new HandlerThread("VideoMuxer");
        thread.start();
        this.mHandler = new MuxerHandler(thread.getLooper());
    }

    public void setOrientationHint(int degree) {
        this.mMuxer.setOrientationHint(degree);
    }

    public void prepareMuxer(String path) throws IOException {
        this.mPath = path;
        this.mMuxer = new MediaMuxer(path, 0);
    }

    public void startMuxer() {
        this.mStarted = true;
        if (this.mMuxer != null) {
            this.mMuxer.start();
        }
    }

    public void stopMuxer() {
        if (this.mMuxer != null) {
            this.mMuxer.stop();
            this.mMuxer.release();
            this.mMuxer = null;
        }
        this.mStarted = false;
        File file = new File(this.mPath);
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stop muxer file size is ");
        stringBuilder.append(file.length());
        Log.w(tag, stringBuilder.toString());
    }

    public void writeData(MuxerBuffer buffer) {
        this.mMuxer.writeSampleData(buffer.track, buffer.bufferData, buffer.bufferInfo);
        File file = new File(this.mPath);
    }

    public int addTrack(MediaFormat format) {
        return this.mMuxer.addTrack(format);
    }
}
