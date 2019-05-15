package com.google.common.hash;

import com.google.common.base.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

abstract class AbstractNonStreamingHashFunction implements HashFunction {

    private static final class ExposedByteArrayOutputStream extends ByteArrayOutputStream {
        ExposedByteArrayOutputStream(int expectedInputSize) {
            super(expectedInputSize);
        }

        /* Access modifiers changed, original: 0000 */
        public byte[] byteArray() {
            return this.buf;
        }

        /* Access modifiers changed, original: 0000 */
        public int length() {
            return this.count;
        }
    }

    private final class BufferingHasher extends AbstractHasher {
        static final int BOTTOM_BYTE = 255;
        final ExposedByteArrayOutputStream stream;

        BufferingHasher(int expectedInputSize) {
            this.stream = new ExposedByteArrayOutputStream(expectedInputSize);
        }

        public Hasher putByte(byte b) {
            this.stream.write(b);
            return this;
        }

        public Hasher putBytes(byte[] bytes) {
            try {
                this.stream.write(bytes);
                return this;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Hasher putBytes(byte[] bytes, int off, int len) {
            this.stream.write(bytes, off, len);
            return this;
        }

        public Hasher putShort(short s) {
            this.stream.write(s & 255);
            this.stream.write((s >>> 8) & 255);
            return this;
        }

        public Hasher putInt(int i) {
            this.stream.write(i & 255);
            this.stream.write((i >>> 8) & 255);
            this.stream.write((i >>> 16) & 255);
            this.stream.write((i >>> 24) & 255);
            return this;
        }

        public Hasher putLong(long l) {
            for (int i = 0; i < 64; i += 8) {
                this.stream.write((byte) ((int) ((l >>> i) & 255)));
            }
            return this;
        }

        public Hasher putChar(char c) {
            this.stream.write(c & 255);
            this.stream.write((c >>> 8) & 255);
            return this;
        }

        public <T> Hasher putObject(T instance, Funnel<? super T> funnel) {
            funnel.funnel(instance, this);
            return this;
        }

        public HashCode hash() {
            return AbstractNonStreamingHashFunction.this.hashBytes(this.stream.byteArray(), 0, this.stream.length());
        }
    }

    AbstractNonStreamingHashFunction() {
    }

    public Hasher newHasher() {
        return new BufferingHasher(32);
    }

    public Hasher newHasher(int expectedInputSize) {
        Preconditions.checkArgument(expectedInputSize >= 0);
        return new BufferingHasher(expectedInputSize);
    }

    public <T> HashCode hashObject(T instance, Funnel<? super T> funnel) {
        return newHasher().putObject(instance, funnel).hash();
    }

    public HashCode hashUnencodedChars(CharSequence input) {
        int len = input.length();
        Hasher hasher = newHasher(len * 2);
        for (int i = 0; i < len; i++) {
            hasher.putChar(input.charAt(i));
        }
        return hasher.hash();
    }

    public HashCode hashString(CharSequence input, Charset charset) {
        return hashBytes(input.toString().getBytes(charset));
    }

    public HashCode hashInt(int input) {
        return newHasher(4).putInt(input).hash();
    }

    public HashCode hashLong(long input) {
        return newHasher(8).putLong(input).hash();
    }

    public HashCode hashBytes(byte[] input) {
        return hashBytes(input, 0, input.length);
    }
}
