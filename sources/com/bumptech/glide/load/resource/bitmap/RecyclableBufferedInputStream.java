package com.bumptech.glide.load.resource.bitmap;

import android.util.Log;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RecyclableBufferedInputStream extends FilterInputStream {
    private static final String TAG = "BufferedIs";
    private volatile byte[] buf;
    private int count;
    private int marklimit;
    private int markpos = -1;
    private int pos;

    public static class InvalidMarkException extends RuntimeException {
        private static final long serialVersionUID = -4338378848813561757L;

        public InvalidMarkException(String detailMessage) {
            super(detailMessage);
        }
    }

    public RecyclableBufferedInputStream(InputStream in, byte[] buffer) {
        super(in);
        if (buffer == null || buffer.length == 0) {
            throw new IllegalArgumentException("buffer is null or empty");
        }
        this.buf = buffer;
    }

    public synchronized int available() throws IOException {
        InputStream localIn;
        localIn = this.in;
        if (this.buf == null || localIn == null) {
            throw streamClosed();
        }
        return (this.count - this.pos) + localIn.available();
    }

    private static IOException streamClosed() throws IOException {
        throw new IOException("BufferedInputStream is closed");
    }

    public synchronized void fixMarkLimit() {
        this.marklimit = this.buf.length;
    }

    public void close() throws IOException {
        this.buf = null;
        InputStream localIn = this.in;
        this.in = null;
        if (localIn != null) {
            localIn.close();
        }
    }

    private int fillbuf(InputStream localIn, byte[] localBuf) throws IOException {
        int result;
        if (this.markpos == -1 || this.pos - this.markpos >= this.marklimit) {
            result = localIn.read(localBuf);
            if (result > 0) {
                this.markpos = -1;
                this.pos = 0;
                this.count = result;
            }
            return result;
        }
        if (this.markpos == 0 && this.marklimit > localBuf.length && this.count == localBuf.length) {
            result = localBuf.length * 2;
            if (result > this.marklimit) {
                result = this.marklimit;
            }
            if (Log.isLoggable(TAG, 3)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("allocate buffer of length: ");
                stringBuilder.append(result);
                Log.d(str, stringBuilder.toString());
            }
            byte[] newbuf = new byte[result];
            System.arraycopy(localBuf, 0, newbuf, 0, localBuf.length);
            this.buf = newbuf;
            localBuf = newbuf;
        } else if (this.markpos > 0) {
            System.arraycopy(localBuf, this.markpos, localBuf, 0, localBuf.length - this.markpos);
        }
        this.pos -= this.markpos;
        this.markpos = 0;
        this.count = 0;
        result = localIn.read(localBuf, this.pos, localBuf.length - this.pos);
        this.count = result <= 0 ? this.pos : this.pos + result;
        return result;
    }

    public synchronized void mark(int readlimit) {
        this.marklimit = Math.max(this.marklimit, readlimit);
        this.markpos = this.pos;
    }

    public boolean markSupported() {
        return true;
    }

    public synchronized int read() throws IOException {
        byte[] localBuf = this.buf;
        InputStream localIn = this.in;
        if (localBuf == null || localIn == null) {
            throw streamClosed();
        } else if (this.pos >= this.count && fillbuf(localIn, localBuf) == -1) {
            return -1;
        } else {
            if (localBuf != this.buf) {
                localBuf = this.buf;
                if (localBuf == null) {
                    throw streamClosed();
                }
            }
            if (this.count - this.pos <= 0) {
                return -1;
            }
            int i = this.pos;
            this.pos = i + 1;
            return localBuf[i] & 255;
        }
    }

    /* JADX WARNING: Missing block: B:23:0x003a, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:35:0x0050, code skipped:
            return r4;
     */
    /* JADX WARNING: Missing block: B:42:0x005d, code skipped:
            return r4;
     */
    public synchronized int read(byte[] r6, int r7, int r8) throws java.io.IOException {
        /*
        r5 = this;
        monitor-enter(r5);
        r0 = r5.buf;	 Catch:{ all -> 0x00a0 }
        if (r0 == 0) goto L_0x009b;
    L_0x0005:
        if (r8 != 0) goto L_0x000a;
    L_0x0007:
        r1 = 0;
        monitor-exit(r5);
        return r1;
    L_0x000a:
        r1 = r5.in;	 Catch:{ all -> 0x00a0 }
        if (r1 == 0) goto L_0x0096;
    L_0x000e:
        r2 = r5.pos;	 Catch:{ all -> 0x00a0 }
        r3 = r5.count;	 Catch:{ all -> 0x00a0 }
        if (r2 >= r3) goto L_0x003b;
    L_0x0014:
        r2 = r5.count;	 Catch:{ all -> 0x00a0 }
        r3 = r5.pos;	 Catch:{ all -> 0x00a0 }
        r2 = r2 - r3;
        if (r2 < r8) goto L_0x001d;
    L_0x001b:
        r2 = r8;
        goto L_0x0022;
    L_0x001d:
        r2 = r5.count;	 Catch:{ all -> 0x00a0 }
        r3 = r5.pos;	 Catch:{ all -> 0x00a0 }
        r2 = r2 - r3;
    L_0x0022:
        r3 = r5.pos;	 Catch:{ all -> 0x00a0 }
        java.lang.System.arraycopy(r0, r3, r6, r7, r2);	 Catch:{ all -> 0x00a0 }
        r3 = r5.pos;	 Catch:{ all -> 0x00a0 }
        r3 = r3 + r2;
        r5.pos = r3;	 Catch:{ all -> 0x00a0 }
        if (r2 == r8) goto L_0x0039;
    L_0x002e:
        r3 = r1.available();	 Catch:{ all -> 0x00a0 }
        if (r3 != 0) goto L_0x0035;
    L_0x0034:
        goto L_0x0039;
    L_0x0035:
        r7 = r7 + r2;
        r2 = r8 - r2;
        goto L_0x003c;
    L_0x0039:
        monitor-exit(r5);
        return r2;
    L_0x003b:
        r2 = r8;
    L_0x003c:
        r3 = r5.markpos;	 Catch:{ all -> 0x00a0 }
        r4 = -1;
        if (r3 != r4) goto L_0x0051;
    L_0x0041:
        r3 = r0.length;	 Catch:{ all -> 0x00a0 }
        if (r2 < r3) goto L_0x0051;
    L_0x0044:
        r3 = r1.read(r6, r7, r2);	 Catch:{ all -> 0x00a0 }
        if (r3 != r4) goto L_0x0085;
    L_0x004a:
        if (r2 != r8) goto L_0x004d;
    L_0x004c:
        goto L_0x004f;
    L_0x004d:
        r4 = r8 - r2;
    L_0x004f:
        monitor-exit(r5);
        return r4;
    L_0x0051:
        r3 = r5.fillbuf(r1, r0);	 Catch:{ all -> 0x00a0 }
        if (r3 != r4) goto L_0x005e;
    L_0x0057:
        if (r2 != r8) goto L_0x005a;
    L_0x0059:
        goto L_0x005c;
    L_0x005a:
        r4 = r8 - r2;
    L_0x005c:
        monitor-exit(r5);
        return r4;
    L_0x005e:
        r3 = r5.buf;	 Catch:{ all -> 0x00a0 }
        if (r0 == r3) goto L_0x006d;
    L_0x0062:
        r3 = r5.buf;	 Catch:{ all -> 0x00a0 }
        r0 = r3;
        if (r0 == 0) goto L_0x0068;
    L_0x0067:
        goto L_0x006d;
    L_0x0068:
        r3 = streamClosed();	 Catch:{ all -> 0x00a0 }
        throw r3;	 Catch:{ all -> 0x00a0 }
    L_0x006d:
        r3 = r5.count;	 Catch:{ all -> 0x00a0 }
        r4 = r5.pos;	 Catch:{ all -> 0x00a0 }
        r3 = r3 - r4;
        if (r3 < r2) goto L_0x0076;
    L_0x0074:
        r3 = r2;
        goto L_0x007b;
    L_0x0076:
        r3 = r5.count;	 Catch:{ all -> 0x00a0 }
        r4 = r5.pos;	 Catch:{ all -> 0x00a0 }
        r3 = r3 - r4;
    L_0x007b:
        r4 = r5.pos;	 Catch:{ all -> 0x00a0 }
        java.lang.System.arraycopy(r0, r4, r6, r7, r3);	 Catch:{ all -> 0x00a0 }
        r4 = r5.pos;	 Catch:{ all -> 0x00a0 }
        r4 = r4 + r3;
        r5.pos = r4;	 Catch:{ all -> 0x00a0 }
    L_0x0085:
        r2 = r2 - r3;
        if (r2 != 0) goto L_0x008a;
    L_0x0088:
        monitor-exit(r5);
        return r8;
    L_0x008a:
        r4 = r1.available();	 Catch:{ all -> 0x00a0 }
        if (r4 != 0) goto L_0x0094;
    L_0x0090:
        r4 = r8 - r2;
        monitor-exit(r5);
        return r4;
    L_0x0094:
        r7 = r7 + r3;
        goto L_0x003c;
    L_0x0096:
        r2 = streamClosed();	 Catch:{ all -> 0x00a0 }
        throw r2;	 Catch:{ all -> 0x00a0 }
    L_0x009b:
        r1 = streamClosed();	 Catch:{ all -> 0x00a0 }
        throw r1;	 Catch:{ all -> 0x00a0 }
    L_0x00a0:
        r6 = move-exception;
        monitor-exit(r5);
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.load.resource.bitmap.RecyclableBufferedInputStream.read(byte[], int, int):int");
    }

    public synchronized void reset() throws IOException {
        if (this.buf == null) {
            throw new IOException("Stream is closed");
        } else if (-1 != this.markpos) {
            this.pos = this.markpos;
        } else {
            throw new InvalidMarkException("Mark has been invalidated");
        }
    }

    public synchronized long skip(long byteCount) throws IOException {
        byte[] localBuf = this.buf;
        InputStream localIn = this.in;
        if (localBuf == null) {
            throw streamClosed();
        } else if (byteCount < 1) {
            return 0;
        } else {
            if (localIn == null) {
                throw streamClosed();
            } else if (((long) (this.count - this.pos)) >= byteCount) {
                this.pos = (int) (((long) this.pos) + byteCount);
                return byteCount;
            } else {
                long read = (long) (this.count - this.pos);
                this.pos = this.count;
                if (this.markpos == -1 || byteCount > ((long) this.marklimit)) {
                    return localIn.skip(byteCount - read) + read;
                } else if (fillbuf(localIn, localBuf) == -1) {
                    return read;
                } else {
                    if (((long) (this.count - this.pos)) >= byteCount - read) {
                        this.pos = (int) (((long) this.pos) + (byteCount - read));
                        return byteCount;
                    }
                    long read2 = (((long) this.count) + read) - ((long) this.pos);
                    this.pos = this.count;
                    return read2;
                }
            }
        }
    }
}
