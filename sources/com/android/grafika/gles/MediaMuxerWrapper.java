package com.android.grafika.gles;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.io.IOException;

public class MediaMuxerWrapper {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaMuxerWrapper";
    private int mEncoderCount = 0;
    private boolean mIsStarted = false;
    private final MediaMuxer mMediaMuxer;
    private int mStartedCount = 0;

    public MediaMuxerWrapper(String outputPath) throws IOException {
        this.mMediaMuxer = new MediaMuxer(outputPath, 0);
        this.mEncoderCount = 0;
        this.mStartedCount = 0;
        this.mIsStarted = false;
        this.mEncoderCount = 2;
    }

    public void setTrackCount(int count) {
        this.mEncoderCount = count;
    }

    public synchronized boolean start() {
        this.mStartedCount++;
        if (this.mEncoderCount > 0 && this.mStartedCount == this.mEncoderCount) {
            this.mMediaMuxer.start();
            this.mIsStarted = true;
            notifyAll();
        }
        return this.mIsStarted;
    }

    /* JADX WARNING: Missing block: B:14:0x002a, code skipped:
            return;
     */
    public synchronized void stop() {
        /*
        r2 = this;
        monitor-enter(r2);
        r0 = r2.mIsStarted;	 Catch:{ all -> 0x002b }
        if (r0 != 0) goto L_0x000e;
    L_0x0005:
        r0 = "";
        r1 = "not started";
        android.util.Log.d(r0, r1);	 Catch:{ all -> 0x002b }
        monitor-exit(r2);
        return;
    L_0x000e:
        r0 = r2.mStartedCount;	 Catch:{ all -> 0x002b }
        r0 = r0 + -1;
        r2.mStartedCount = r0;	 Catch:{ all -> 0x002b }
        r0 = r2.mEncoderCount;	 Catch:{ all -> 0x002b }
        if (r0 <= 0) goto L_0x0029;
    L_0x0018:
        r0 = r2.mStartedCount;	 Catch:{ all -> 0x002b }
        if (r0 > 0) goto L_0x0029;
    L_0x001c:
        r0 = r2.mMediaMuxer;	 Catch:{ all -> 0x002b }
        r0.stop();	 Catch:{ all -> 0x002b }
        r0 = r2.mMediaMuxer;	 Catch:{ all -> 0x002b }
        r0.release();	 Catch:{ all -> 0x002b }
        r0 = 0;
        r2.mIsStarted = r0;	 Catch:{ all -> 0x002b }
    L_0x0029:
        monitor-exit(r2);
        return;
    L_0x002b:
        r0 = move-exception;
        monitor-exit(r2);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.grafika.gles.MediaMuxerWrapper.stop():void");
    }

    public synchronized int addTrack(MediaFormat format) {
        if (this.mIsStarted) {
            throw new IllegalStateException("muxer already started");
        }
        return this.mMediaMuxer.addTrack(format);
    }

    /* JADX WARNING: Missing block: B:11:0x0011, code skipped:
            return;
     */
    public synchronized void writeSampleData(int r2, java.nio.ByteBuffer r3, android.media.MediaCodec.BufferInfo r4) {
        /*
        r1 = this;
        monitor-enter(r1);
        r0 = r1.mIsStarted;	 Catch:{ all -> 0x0012 }
        if (r0 != 0) goto L_0x0007;
    L_0x0005:
        monitor-exit(r1);
        return;
    L_0x0007:
        r0 = r1.mStartedCount;	 Catch:{ all -> 0x0012 }
        if (r0 <= 0) goto L_0x0010;
    L_0x000b:
        r0 = r1.mMediaMuxer;	 Catch:{ all -> 0x0012 }
        r0.writeSampleData(r2, r3, r4);	 Catch:{ all -> 0x0012 }
    L_0x0010:
        monitor-exit(r1);
        return;
    L_0x0012:
        r2 = move-exception;
        monitor-exit(r1);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.grafika.gles.MediaMuxerWrapper.writeSampleData(int, java.nio.ByteBuffer, android.media.MediaCodec$BufferInfo):void");
    }
}
