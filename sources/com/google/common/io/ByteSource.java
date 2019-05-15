package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

public abstract class ByteSource {
    private static final int BUF_SIZE = 4096;
    private static final byte[] countBuffer = new byte[4096];

    private final class AsCharSource extends CharSource {
        private final Charset charset;

        private AsCharSource(Charset charset) {
            this.charset = (Charset) Preconditions.checkNotNull(charset);
        }

        public Reader openStream() throws IOException {
            return new InputStreamReader(ByteSource.this.openStream(), this.charset);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ByteSource.this.toString());
            stringBuilder.append(".asCharSource(");
            stringBuilder.append(this.charset);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static class ByteArrayByteSource extends ByteSource {
        protected final byte[] bytes;

        protected ByteArrayByteSource(byte[] bytes) {
            this.bytes = (byte[]) Preconditions.checkNotNull(bytes);
        }

        public InputStream openStream() {
            return new ByteArrayInputStream(this.bytes);
        }

        public InputStream openBufferedStream() throws IOException {
            return openStream();
        }

        public boolean isEmpty() {
            return this.bytes.length == 0;
        }

        public long size() {
            return (long) this.bytes.length;
        }

        public byte[] read() {
            return (byte[]) this.bytes.clone();
        }

        public long copyTo(OutputStream output) throws IOException {
            output.write(this.bytes);
            return (long) this.bytes.length;
        }

        public <T> T read(ByteProcessor<T> processor) throws IOException {
            processor.processBytes(this.bytes, 0, this.bytes.length);
            return processor.getResult();
        }

        public HashCode hash(HashFunction hashFunction) throws IOException {
            return hashFunction.hashBytes(this.bytes);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ByteSource.wrap(");
            stringBuilder.append(Ascii.truncate(BaseEncoding.base16().encode(this.bytes), 30, "..."));
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static final class ConcatenatedByteSource extends ByteSource {
        private final Iterable<? extends ByteSource> sources;

        ConcatenatedByteSource(Iterable<? extends ByteSource> sources) {
            this.sources = (Iterable) Preconditions.checkNotNull(sources);
        }

        public InputStream openStream() throws IOException {
            return new MultiInputStream(this.sources.iterator());
        }

        public boolean isEmpty() throws IOException {
            for (ByteSource source : this.sources) {
                if (!source.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public long size() throws IOException {
            long result = 0;
            for (ByteSource source : this.sources) {
                result += source.size();
            }
            return result;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ByteSource.concat(");
            stringBuilder.append(this.sources);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private final class SlicedByteSource extends ByteSource {
        private final long length;
        private final long offset;

        private SlicedByteSource(long offset, long length) {
            Preconditions.checkArgument(offset >= 0, "offset (%s) may not be negative", Long.valueOf(offset));
            Preconditions.checkArgument(length >= 0, "length (%s) may not be negative", Long.valueOf(length));
            this.offset = offset;
            this.length = length;
        }

        public InputStream openStream() throws IOException {
            return sliceStream(ByteSource.this.openStream());
        }

        public InputStream openBufferedStream() throws IOException {
            return sliceStream(ByteSource.this.openBufferedStream());
        }

        private InputStream sliceStream(InputStream in) throws IOException {
            Closer closer;
            if (this.offset > 0) {
                try {
                    ByteStreams.skipFully(in, this.offset);
                } catch (Throwable th) {
                    closer.close();
                }
            }
            return ByteStreams.limit(in, this.length);
        }

        public ByteSource slice(long offset, long length) {
            Preconditions.checkArgument(offset >= 0, "offset (%s) may not be negative", Long.valueOf(offset));
            Preconditions.checkArgument(length >= 0, "length (%s) may not be negative", Long.valueOf(length));
            return ByteSource.this.slice(this.offset + offset, Math.min(length, this.length - offset));
        }

        public boolean isEmpty() throws IOException {
            return this.length == 0 || super.isEmpty();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(ByteSource.this.toString());
            stringBuilder.append(".slice(");
            stringBuilder.append(this.offset);
            stringBuilder.append(", ");
            stringBuilder.append(this.length);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static final class EmptyByteSource extends ByteArrayByteSource {
        private static final EmptyByteSource INSTANCE = new EmptyByteSource();

        private EmptyByteSource() {
            super(new byte[0]);
        }

        public CharSource asCharSource(Charset charset) {
            Preconditions.checkNotNull(charset);
            return CharSource.empty();
        }

        public byte[] read() {
            return this.bytes;
        }

        public String toString() {
            return "ByteSource.empty()";
        }
    }

    public abstract InputStream openStream() throws IOException;

    protected ByteSource() {
    }

    public CharSource asCharSource(Charset charset) {
        return new AsCharSource(charset);
    }

    public InputStream openBufferedStream() throws IOException {
        InputStream in = openStream();
        if (in instanceof BufferedInputStream) {
            return (BufferedInputStream) in;
        }
        return new BufferedInputStream(in);
    }

    public ByteSource slice(long offset, long length) {
        return new SlicedByteSource(offset, length);
    }

    public boolean isEmpty() throws IOException {
        Closer closer = Closer.create();
        try {
            boolean z = ((InputStream) closer.register(openStream())).read() == -1;
            closer.close();
            return z;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public long size() throws IOException {
        Closer closer = Closer.create();
        long countBySkipping;
        try {
            countBySkipping = countBySkipping((InputStream) closer.register(openStream()));
            closer.close();
            return countBySkipping;
        } catch (IOException e) {
            closer.close();
            closer = Closer.create();
            countBySkipping = countByReading((InputStream) closer.register(openStream()));
            closer.close();
            return countBySkipping;
        } catch (Throwable e2) {
            try {
                RuntimeException rethrow = closer.rethrow(e2);
            } catch (Throwable th) {
                closer.close();
            }
        }
    }

    private long countBySkipping(InputStream in) throws IOException {
        long count = 0;
        while (true) {
            long skipped = in.skip((long) Math.min(in.available(), ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED));
            if (skipped > 0) {
                count += skipped;
            } else if (in.read() == -1) {
                return count;
            } else {
                if (count == 0 && in.available() == 0) {
                    throw new IOException();
                }
                count++;
            }
        }
    }

    private long countByReading(InputStream in) throws IOException {
        long count = 0;
        while (true) {
            long read = (long) in.read(countBuffer);
            long read2 = read;
            if (read == -1) {
                return count;
            }
            count += read2;
        }
    }

    public long copyTo(OutputStream output) throws IOException {
        Preconditions.checkNotNull(output);
        Closer closer = Closer.create();
        try {
            long copy = ByteStreams.copy((InputStream) closer.register(openStream()), output);
            closer.close();
            return copy;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public long copyTo(ByteSink sink) throws IOException {
        Preconditions.checkNotNull(sink);
        Closer closer = Closer.create();
        try {
            long copy = ByteStreams.copy((InputStream) closer.register(openStream()), (OutputStream) closer.register(sink.openStream()));
            closer.close();
            return copy;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public byte[] read() throws IOException {
        Closer closer = Closer.create();
        try {
            byte[] toByteArray = ByteStreams.toByteArray((InputStream) closer.register(openStream()));
            closer.close();
            return toByteArray;
        } catch (Throwable th) {
            closer.close();
        }
    }

    @Beta
    public <T> T read(ByteProcessor<T> processor) throws IOException {
        Preconditions.checkNotNull(processor);
        Closer closer = Closer.create();
        try {
            Object readBytes = ByteStreams.readBytes((InputStream) closer.register(openStream()), processor);
            closer.close();
            return readBytes;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public HashCode hash(HashFunction hashFunction) throws IOException {
        Hasher hasher = hashFunction.newHasher();
        copyTo(Funnels.asOutputStream(hasher));
        return hasher.hash();
    }

    public boolean contentEquals(ByteSource other) throws IOException {
        Preconditions.checkNotNull(other);
        byte[] buf1 = new byte[4096];
        byte[] buf2 = new byte[4096];
        Closer closer = Closer.create();
        try {
            InputStream in1 = (InputStream) closer.register(openStream());
            InputStream in2 = (InputStream) closer.register(other.openStream());
            while (true) {
                int read1 = ByteStreams.read(in1, buf1, 0, 4096);
                if (read1 != ByteStreams.read(in2, buf2, 0, 4096) || !Arrays.equals(buf1, buf2)) {
                    closer.close();
                } else if (read1 != 4096) {
                    closer.close();
                    return true;
                }
            }
            closer.close();
            return false;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public static ByteSource concat(Iterable<? extends ByteSource> sources) {
        return new ConcatenatedByteSource(sources);
    }

    public static ByteSource concat(Iterator<? extends ByteSource> sources) {
        return concat(ImmutableList.copyOf((Iterator) sources));
    }

    public static ByteSource concat(ByteSource... sources) {
        return concat(ImmutableList.copyOf((Object[]) sources));
    }

    public static ByteSource wrap(byte[] b) {
        return new ByteArrayByteSource(b);
    }

    public static ByteSource empty() {
        return EmptyByteSource.INSTANCE;
    }
}
