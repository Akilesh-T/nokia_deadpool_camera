package com.hmdglobal.app.camera.encoder;

import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class Muxer {

    public static class MuxerBuffer {
        public ByteBuffer bufferData;
        public BufferInfo bufferInfo;
        public int track;

        public MuxerBuffer(int track, ByteBuffer buffer, BufferInfo info) {
            this.track = track;
            this.bufferData = buffer;
            this.bufferInfo = info;
        }
    }

    public abstract int addTrack(MediaFormat mediaFormat);

    public abstract void prepareMuxer(String str) throws IOException;

    public abstract void setOrientationHint(int i);

    public abstract void startMuxer();

    public abstract void stopMuxer();

    public abstract void writeData(MuxerBuffer muxerBuffer);
}
