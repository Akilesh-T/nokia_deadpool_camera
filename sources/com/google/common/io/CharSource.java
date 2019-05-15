package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public abstract class CharSource {

    private static class CharSequenceCharSource extends CharSource {
        private static final Splitter LINE_SPLITTER = Splitter.on(Pattern.compile("\r\n|\n|\r"));
        private final CharSequence seq;

        protected CharSequenceCharSource(CharSequence seq) {
            this.seq = (CharSequence) Preconditions.checkNotNull(seq);
        }

        public Reader openStream() {
            return new CharSequenceReader(this.seq);
        }

        public String read() {
            return this.seq.toString();
        }

        public boolean isEmpty() {
            return this.seq.length() == 0;
        }

        private Iterable<String> lines() {
            return new Iterable<String>() {
                public Iterator<String> iterator() {
                    return new AbstractIterator<String>() {
                        Iterator<String> lines = CharSequenceCharSource.LINE_SPLITTER.split(CharSequenceCharSource.this.seq).iterator();

                        /* Access modifiers changed, original: protected */
                        public String computeNext() {
                            if (this.lines.hasNext()) {
                                String next = (String) this.lines.next();
                                if (this.lines.hasNext() || !next.isEmpty()) {
                                    return next;
                                }
                            }
                            return (String) endOfData();
                        }
                    };
                }
            };
        }

        public String readFirstLine() {
            Iterator<String> lines = lines().iterator();
            return lines.hasNext() ? (String) lines.next() : null;
        }

        public ImmutableList<String> readLines() {
            return ImmutableList.copyOf(lines());
        }

        public <T> T readLines(LineProcessor<T> processor) throws IOException {
            Iterator it = lines().iterator();
            while (it.hasNext() && processor.processLine((String) it.next())) {
            }
            return processor.getResult();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CharSource.wrap(");
            stringBuilder.append(Ascii.truncate(this.seq, 30, "..."));
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static final class ConcatenatedCharSource extends CharSource {
        private final Iterable<? extends CharSource> sources;

        ConcatenatedCharSource(Iterable<? extends CharSource> sources) {
            this.sources = (Iterable) Preconditions.checkNotNull(sources);
        }

        public Reader openStream() throws IOException {
            return new MultiReader(this.sources.iterator());
        }

        public boolean isEmpty() throws IOException {
            for (CharSource source : this.sources) {
                if (!source.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CharSource.concat(");
            stringBuilder.append(this.sources);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static final class EmptyCharSource extends CharSequenceCharSource {
        private static final EmptyCharSource INSTANCE = new EmptyCharSource();

        private EmptyCharSource() {
            super("");
        }

        public String toString() {
            return "CharSource.empty()";
        }
    }

    public abstract Reader openStream() throws IOException;

    protected CharSource() {
    }

    public BufferedReader openBufferedStream() throws IOException {
        Reader reader = openStream();
        if (reader instanceof BufferedReader) {
            return (BufferedReader) reader;
        }
        return new BufferedReader(reader);
    }

    public long copyTo(Appendable appendable) throws IOException {
        Preconditions.checkNotNull(appendable);
        Closer closer = Closer.create();
        try {
            long copy = CharStreams.copy((Reader) closer.register(openStream()), appendable);
            closer.close();
            return copy;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public long copyTo(CharSink sink) throws IOException {
        Preconditions.checkNotNull(sink);
        Closer closer = Closer.create();
        try {
            long copy = CharStreams.copy((Reader) closer.register(openStream()), (Writer) closer.register(sink.openStream()));
            closer.close();
            return copy;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public String read() throws IOException {
        Closer closer = Closer.create();
        try {
            String charStreams = CharStreams.toString((Reader) closer.register(openStream()));
            closer.close();
            return charStreams;
        } catch (Throwable th) {
            closer.close();
        }
    }

    @Nullable
    public String readFirstLine() throws IOException {
        Closer closer = Closer.create();
        try {
            String readLine = ((BufferedReader) closer.register(openBufferedStream())).readLine();
            closer.close();
            return readLine;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public ImmutableList<String> readLines() throws IOException {
        Closer closer = Closer.create();
        try {
            BufferedReader reader = (BufferedReader) closer.register(openBufferedStream());
            Collection result = Lists.newArrayList();
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    result.add(line);
                } else {
                    ImmutableList copyOf = ImmutableList.copyOf(result);
                    closer.close();
                    return copyOf;
                }
            }
        } catch (Throwable th) {
            closer.close();
        }
    }

    @Beta
    public <T> T readLines(LineProcessor<T> processor) throws IOException {
        Preconditions.checkNotNull(processor);
        Closer closer = Closer.create();
        try {
            Object readLines = CharStreams.readLines((Reader) closer.register(openStream()), processor);
            closer.close();
            return readLines;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public boolean isEmpty() throws IOException {
        Closer closer = Closer.create();
        try {
            boolean z = ((Reader) closer.register(openStream())).read() == -1;
            closer.close();
            return z;
        } catch (Throwable th) {
            closer.close();
        }
    }

    public static CharSource concat(Iterable<? extends CharSource> sources) {
        return new ConcatenatedCharSource(sources);
    }

    public static CharSource concat(Iterator<? extends CharSource> sources) {
        return concat(ImmutableList.copyOf((Iterator) sources));
    }

    public static CharSource concat(CharSource... sources) {
        return concat(ImmutableList.copyOf((Object[]) sources));
    }

    public static CharSource wrap(CharSequence charSequence) {
        return new CharSequenceCharSource(charSequence);
    }

    public static CharSource empty() {
        return EmptyCharSource.INSTANCE;
    }
}
