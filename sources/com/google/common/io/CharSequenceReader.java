package com.google.common.io;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

final class CharSequenceReader extends Reader {
    private int mark;
    private int pos;
    private CharSequence seq;

    public CharSequenceReader(CharSequence seq) {
        this.seq = (CharSequence) Preconditions.checkNotNull(seq);
    }

    private void checkOpen() throws IOException {
        if (this.seq == null) {
            throw new IOException("reader closed");
        }
    }

    private boolean hasRemaining() {
        return remaining() > 0;
    }

    private int remaining() {
        return this.seq.length() - this.pos;
    }

    public synchronized int read(CharBuffer target) throws IOException {
        Preconditions.checkNotNull(target);
        checkOpen();
        if (!hasRemaining()) {
            return -1;
        }
        int charsToRead = Math.min(target.remaining(), remaining());
        for (int i = 0; i < charsToRead; i++) {
            CharSequence charSequence = this.seq;
            int i2 = this.pos;
            this.pos = i2 + 1;
            target.put(charSequence.charAt(i2));
        }
        return charsToRead;
    }

    public synchronized int read() throws IOException {
        int charAt;
        checkOpen();
        if (hasRemaining()) {
            CharSequence charSequence = this.seq;
            int i = this.pos;
            this.pos = i + 1;
            charAt = charSequence.charAt(i);
        } else {
            charAt = -1;
        }
        return charAt;
    }

    public synchronized int read(char[] cbuf, int off, int len) throws IOException {
        Preconditions.checkPositionIndexes(off, off + len, cbuf.length);
        checkOpen();
        if (!hasRemaining()) {
            return -1;
        }
        int charsToRead = Math.min(len, remaining());
        for (int i = 0; i < charsToRead; i++) {
            int i2 = off + i;
            CharSequence charSequence = this.seq;
            int i3 = this.pos;
            this.pos = i3 + 1;
            cbuf[i2] = charSequence.charAt(i3);
        }
        return charsToRead;
    }

    public synchronized long skip(long n) throws IOException {
        int charsToSkip;
        Preconditions.checkArgument(n >= 0, "n (%s) may not be negative", Long.valueOf(n));
        checkOpen();
        charsToSkip = (int) Math.min((long) remaining(), n);
        this.pos += charsToSkip;
        return (long) charsToSkip;
    }

    public synchronized boolean ready() throws IOException {
        checkOpen();
        return true;
    }

    public boolean markSupported() {
        return true;
    }

    public synchronized void mark(int readAheadLimit) throws IOException {
        Preconditions.checkArgument(readAheadLimit >= 0, "readAheadLimit (%s) may not be negative", Integer.valueOf(readAheadLimit));
        checkOpen();
        this.mark = this.pos;
    }

    public synchronized void reset() throws IOException {
        checkOpen();
        this.pos = this.mark;
    }

    public synchronized void close() throws IOException {
        this.seq = null;
    }
}
