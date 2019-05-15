package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

@Beta
public final class CharStreams {
    private static final int BUF_SIZE = 2048;

    private static final class NullWriter extends Writer {
        private static final NullWriter INSTANCE = new NullWriter();

        private NullWriter() {
        }

        public void write(int c) {
        }

        public void write(char[] cbuf) {
            Preconditions.checkNotNull(cbuf);
        }

        public void write(char[] cbuf, int off, int len) {
            Preconditions.checkPositionIndexes(off, off + len, cbuf.length);
        }

        public void write(String str) {
            Preconditions.checkNotNull(str);
        }

        public void write(String str, int off, int len) {
            Preconditions.checkPositionIndexes(off, off + len, str.length());
        }

        public Writer append(CharSequence csq) {
            Preconditions.checkNotNull(csq);
            return this;
        }

        public Writer append(CharSequence csq, int start, int end) {
            Preconditions.checkPositionIndexes(start, end, csq.length());
            return this;
        }

        public Writer append(char c) {
            return this;
        }

        public void flush() {
        }

        public void close() {
        }

        public String toString() {
            return "CharStreams.nullWriter()";
        }
    }

    private CharStreams() {
    }

    public static long copy(Readable from, Appendable to) throws IOException {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        CharBuffer buf = CharBuffer.allocate(2048);
        long total = 0;
        while (from.read(buf) != -1) {
            buf.flip();
            to.append(buf);
            total += (long) buf.remaining();
            buf.clear();
        }
        return total;
    }

    public static String toString(Readable r) throws IOException {
        return toStringBuilder(r).toString();
    }

    private static StringBuilder toStringBuilder(Readable r) throws IOException {
        StringBuilder sb = new StringBuilder();
        copy(r, sb);
        return sb;
    }

    public static List<String> readLines(Readable r) throws IOException {
        List<String> result = new ArrayList();
        LineReader lineReader = new LineReader(r);
        while (true) {
            String readLine = lineReader.readLine();
            String line = readLine;
            if (readLine == null) {
                return result;
            }
            result.add(line);
        }
    }

    public static <T> T readLines(Readable readable, LineProcessor<T> processor) throws IOException {
        Preconditions.checkNotNull(readable);
        Preconditions.checkNotNull(processor);
        LineReader lineReader = new LineReader(readable);
        String line;
        do {
            String readLine = lineReader.readLine();
            line = readLine;
            if (readLine == null) {
                break;
            }
        } while (processor.processLine(line));
        return processor.getResult();
    }

    public static void skipFully(Reader reader, long n) throws IOException {
        Preconditions.checkNotNull(reader);
        while (n > 0) {
            long amt = reader.skip(n);
            if (amt != 0) {
                n -= amt;
            } else if (reader.read() != -1) {
                n--;
            } else {
                throw new EOFException();
            }
        }
    }

    public static Writer nullWriter() {
        return NullWriter.INSTANCE;
    }

    public static Writer asWriter(Appendable target) {
        if (target instanceof Writer) {
            return (Writer) target;
        }
        return new AppendableWriter(target);
    }

    static Reader asReader(final Readable readable) {
        Preconditions.checkNotNull(readable);
        if (readable instanceof Reader) {
            return (Reader) readable;
        }
        return new Reader() {
            public int read(char[] cbuf, int off, int len) throws IOException {
                return read(CharBuffer.wrap(cbuf, off, len));
            }

            public int read(CharBuffer target) throws IOException {
                return readable.read(target);
            }

            public void close() throws IOException {
                if (readable instanceof Closeable) {
                    ((Closeable) readable).close();
                }
            }
        };
    }
}
