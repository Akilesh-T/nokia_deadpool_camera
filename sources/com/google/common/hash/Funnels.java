package com.google.common.hash;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import javax.annotation.Nullable;

@Beta
public final class Funnels {

    private static class SinkAsStream extends OutputStream {
        final PrimitiveSink sink;

        SinkAsStream(PrimitiveSink sink) {
            this.sink = (PrimitiveSink) Preconditions.checkNotNull(sink);
        }

        public void write(int b) {
            this.sink.putByte((byte) b);
        }

        public void write(byte[] bytes) {
            this.sink.putBytes(bytes);
        }

        public void write(byte[] bytes, int off, int len) {
            this.sink.putBytes(bytes, off, len);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Funnels.asOutputStream(");
            stringBuilder.append(this.sink);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private enum ByteArrayFunnel implements Funnel<byte[]> {
        INSTANCE;

        public void funnel(byte[] from, PrimitiveSink into) {
            into.putBytes(from);
        }

        public String toString() {
            return "Funnels.byteArrayFunnel()";
        }
    }

    private enum IntegerFunnel implements Funnel<Integer> {
        INSTANCE;

        public void funnel(Integer from, PrimitiveSink into) {
            into.putInt(from.intValue());
        }

        public String toString() {
            return "Funnels.integerFunnel()";
        }
    }

    private enum LongFunnel implements Funnel<Long> {
        INSTANCE;

        public void funnel(Long from, PrimitiveSink into) {
            into.putLong(from.longValue());
        }

        public String toString() {
            return "Funnels.longFunnel()";
        }
    }

    private static class SequentialFunnel<E> implements Funnel<Iterable<? extends E>>, Serializable {
        private final Funnel<E> elementFunnel;

        SequentialFunnel(Funnel<E> elementFunnel) {
            this.elementFunnel = (Funnel) Preconditions.checkNotNull(elementFunnel);
        }

        public void funnel(Iterable<? extends E> from, PrimitiveSink into) {
            for (E e : from) {
                this.elementFunnel.funnel(e, into);
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Funnels.sequentialFunnel(");
            stringBuilder.append(this.elementFunnel);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }

        public boolean equals(@Nullable Object o) {
            if (!(o instanceof SequentialFunnel)) {
                return false;
            }
            return this.elementFunnel.equals(((SequentialFunnel) o).elementFunnel);
        }

        public int hashCode() {
            return SequentialFunnel.class.hashCode() ^ this.elementFunnel.hashCode();
        }
    }

    private static class StringCharsetFunnel implements Funnel<CharSequence>, Serializable {
        private final Charset charset;

        private static class SerializedForm implements Serializable {
            private static final long serialVersionUID = 0;
            private final String charsetCanonicalName;

            SerializedForm(Charset charset) {
                this.charsetCanonicalName = charset.name();
            }

            private Object readResolve() {
                return Funnels.stringFunnel(Charset.forName(this.charsetCanonicalName));
            }
        }

        StringCharsetFunnel(Charset charset) {
            this.charset = (Charset) Preconditions.checkNotNull(charset);
        }

        public void funnel(CharSequence from, PrimitiveSink into) {
            into.putString(from, this.charset);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Funnels.stringFunnel(");
            stringBuilder.append(this.charset.name());
            stringBuilder.append(")");
            return stringBuilder.toString();
        }

        public boolean equals(@Nullable Object o) {
            if (!(o instanceof StringCharsetFunnel)) {
                return false;
            }
            return this.charset.equals(((StringCharsetFunnel) o).charset);
        }

        public int hashCode() {
            return StringCharsetFunnel.class.hashCode() ^ this.charset.hashCode();
        }

        /* Access modifiers changed, original: 0000 */
        public Object writeReplace() {
            return new SerializedForm(this.charset);
        }
    }

    private enum UnencodedCharsFunnel implements Funnel<CharSequence> {
        INSTANCE;

        public void funnel(CharSequence from, PrimitiveSink into) {
            into.putUnencodedChars(from);
        }

        public String toString() {
            return "Funnels.unencodedCharsFunnel()";
        }
    }

    private Funnels() {
    }

    public static Funnel<byte[]> byteArrayFunnel() {
        return ByteArrayFunnel.INSTANCE;
    }

    public static Funnel<CharSequence> unencodedCharsFunnel() {
        return UnencodedCharsFunnel.INSTANCE;
    }

    public static Funnel<CharSequence> stringFunnel(Charset charset) {
        return new StringCharsetFunnel(charset);
    }

    public static Funnel<Integer> integerFunnel() {
        return IntegerFunnel.INSTANCE;
    }

    public static <E> Funnel<Iterable<? extends E>> sequentialFunnel(Funnel<E> elementFunnel) {
        return new SequentialFunnel(elementFunnel);
    }

    public static Funnel<Long> longFunnel() {
        return LongFunnel.INSTANCE;
    }

    public static OutputStream asOutputStream(PrimitiveSink sink) {
        return new SinkAsStream(sink);
    }
}
