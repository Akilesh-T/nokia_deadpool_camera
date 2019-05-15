package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.RoundingMode;
import java.util.Arrays;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
@Beta
public abstract class BaseEncoding {
    private static final BaseEncoding BASE16 = new StandardBaseEncoding("base16()", "0123456789ABCDEF", null);
    private static final BaseEncoding BASE32 = new StandardBaseEncoding("base32()", "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567", Character.valueOf('='));
    private static final BaseEncoding BASE32_HEX = new StandardBaseEncoding("base32Hex()", "0123456789ABCDEFGHIJKLMNOPQRSTUV", Character.valueOf('='));
    private static final BaseEncoding BASE64 = new StandardBaseEncoding("base64()", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/", Character.valueOf('='));
    private static final BaseEncoding BASE64_URL = new StandardBaseEncoding("base64Url()", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_", Character.valueOf('='));

    public static final class DecodingException extends IOException {
        DecodingException(String message) {
            super(message);
        }

        DecodingException(Throwable cause) {
            super(cause);
        }
    }

    static final class SeparatedBaseEncoding extends BaseEncoding {
        private final int afterEveryChars;
        private final BaseEncoding delegate;
        private final String separator;
        private final CharMatcher separatorChars;

        SeparatedBaseEncoding(BaseEncoding delegate, String separator, int afterEveryChars) {
            this.delegate = (BaseEncoding) Preconditions.checkNotNull(delegate);
            this.separator = (String) Preconditions.checkNotNull(separator);
            this.afterEveryChars = afterEveryChars;
            Preconditions.checkArgument(afterEveryChars > 0, "Cannot add a separator after every %s chars", Integer.valueOf(afterEveryChars));
            this.separatorChars = CharMatcher.anyOf(separator).precomputed();
        }

        /* Access modifiers changed, original: 0000 */
        public CharMatcher padding() {
            return this.delegate.padding();
        }

        /* Access modifiers changed, original: 0000 */
        public int maxEncodedSize(int bytes) {
            int unseparatedSize = this.delegate.maxEncodedSize(bytes);
            return (this.separator.length() * IntMath.divide(Math.max(0, unseparatedSize - 1), this.afterEveryChars, RoundingMode.FLOOR)) + unseparatedSize;
        }

        /* Access modifiers changed, original: 0000 */
        public ByteOutput encodingStream(CharOutput output) {
            return this.delegate.encodingStream(BaseEncoding.separatingOutput(output, this.separator, this.afterEveryChars));
        }

        /* Access modifiers changed, original: 0000 */
        public int maxDecodedSize(int chars) {
            return this.delegate.maxDecodedSize(chars);
        }

        /* Access modifiers changed, original: 0000 */
        public ByteInput decodingStream(CharInput input) {
            return this.delegate.decodingStream(BaseEncoding.ignoringInput(input, this.separatorChars));
        }

        public BaseEncoding omitPadding() {
            return this.delegate.omitPadding().withSeparator(this.separator, this.afterEveryChars);
        }

        public BaseEncoding withPadChar(char padChar) {
            return this.delegate.withPadChar(padChar).withSeparator(this.separator, this.afterEveryChars);
        }

        public BaseEncoding withSeparator(String separator, int afterEveryChars) {
            throw new UnsupportedOperationException("Already have a separator");
        }

        public BaseEncoding upperCase() {
            return this.delegate.upperCase().withSeparator(this.separator, this.afterEveryChars);
        }

        public BaseEncoding lowerCase() {
            return this.delegate.lowerCase().withSeparator(this.separator, this.afterEveryChars);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.delegate.toString());
            stringBuilder.append(".withSeparator(\"");
            stringBuilder.append(this.separator);
            stringBuilder.append("\", ");
            stringBuilder.append(this.afterEveryChars);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    static final class StandardBaseEncoding extends BaseEncoding {
        private final Alphabet alphabet;
        private transient BaseEncoding lowerCase;
        @Nullable
        private final Character paddingChar;
        private transient BaseEncoding upperCase;

        StandardBaseEncoding(String name, String alphabetChars, @Nullable Character paddingChar) {
            this(new Alphabet(name, alphabetChars.toCharArray()), paddingChar);
        }

        StandardBaseEncoding(Alphabet alphabet, @Nullable Character paddingChar) {
            this.alphabet = (Alphabet) Preconditions.checkNotNull(alphabet);
            boolean z = paddingChar == null || !alphabet.matches(paddingChar.charValue());
            Preconditions.checkArgument(z, "Padding character %s was already in alphabet", paddingChar);
            this.paddingChar = paddingChar;
        }

        /* Access modifiers changed, original: 0000 */
        public CharMatcher padding() {
            return this.paddingChar == null ? CharMatcher.NONE : CharMatcher.is(this.paddingChar.charValue());
        }

        /* Access modifiers changed, original: 0000 */
        public int maxEncodedSize(int bytes) {
            return this.alphabet.charsPerChunk * IntMath.divide(bytes, this.alphabet.bytesPerChunk, RoundingMode.CEILING);
        }

        /* Access modifiers changed, original: 0000 */
        public ByteOutput encodingStream(final CharOutput out) {
            Preconditions.checkNotNull(out);
            return new ByteOutput() {
                int bitBuffer = 0;
                int bitBufferLength = 0;
                int writtenChars = 0;

                public void write(byte b) throws IOException {
                    this.bitBuffer <<= 8;
                    this.bitBuffer |= b & 255;
                    this.bitBufferLength += 8;
                    while (this.bitBufferLength >= StandardBaseEncoding.this.alphabet.bitsPerChar) {
                        out.write(StandardBaseEncoding.this.alphabet.encode((this.bitBuffer >> (this.bitBufferLength - StandardBaseEncoding.this.alphabet.bitsPerChar)) & StandardBaseEncoding.this.alphabet.mask));
                        this.writtenChars++;
                        this.bitBufferLength -= StandardBaseEncoding.this.alphabet.bitsPerChar;
                    }
                }

                public void flush() throws IOException {
                    out.flush();
                }

                public void close() throws IOException {
                    if (this.bitBufferLength > 0) {
                        out.write(StandardBaseEncoding.this.alphabet.encode((this.bitBuffer << (StandardBaseEncoding.this.alphabet.bitsPerChar - this.bitBufferLength)) & StandardBaseEncoding.this.alphabet.mask));
                        this.writtenChars++;
                        if (StandardBaseEncoding.this.paddingChar != null) {
                            while (this.writtenChars % StandardBaseEncoding.this.alphabet.charsPerChunk != 0) {
                                out.write(StandardBaseEncoding.this.paddingChar.charValue());
                                this.writtenChars++;
                            }
                        }
                    }
                    out.close();
                }
            };
        }

        /* Access modifiers changed, original: 0000 */
        public int maxDecodedSize(int chars) {
            return (int) (((((long) this.alphabet.bitsPerChar) * ((long) chars)) + 7) / 8);
        }

        /* Access modifiers changed, original: 0000 */
        public ByteInput decodingStream(final CharInput reader) {
            Preconditions.checkNotNull(reader);
            return new ByteInput() {
                int bitBuffer = 0;
                int bitBufferLength = 0;
                boolean hitPadding = false;
                final CharMatcher paddingMatcher = StandardBaseEncoding.this.padding();
                int readChars = 0;

                public int read() throws IOException {
                    StringBuilder stringBuilder;
                    while (true) {
                        int readChar = reader.read();
                        if (readChar != -1) {
                            this.readChars++;
                            char ch = (char) readChar;
                            if (this.paddingMatcher.matches(ch)) {
                                if (this.hitPadding || (this.readChars != 1 && StandardBaseEncoding.this.alphabet.isValidPaddingStartPosition(this.readChars - 1))) {
                                    this.hitPadding = true;
                                }
                            } else if (this.hitPadding) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Expected padding character but found '");
                                stringBuilder.append(ch);
                                stringBuilder.append("' at index ");
                                stringBuilder.append(this.readChars);
                                throw new DecodingException(stringBuilder.toString());
                            } else {
                                this.bitBuffer <<= StandardBaseEncoding.this.alphabet.bitsPerChar;
                                this.bitBuffer |= StandardBaseEncoding.this.alphabet.decode(ch);
                                this.bitBufferLength += StandardBaseEncoding.this.alphabet.bitsPerChar;
                                if (this.bitBufferLength >= 8) {
                                    this.bitBufferLength -= 8;
                                    return (this.bitBuffer >> this.bitBufferLength) & 255;
                                }
                            }
                        } else if (this.hitPadding || StandardBaseEncoding.this.alphabet.isValidPaddingStartPosition(this.readChars)) {
                            return -1;
                        } else {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Invalid input length ");
                            stringBuilder2.append(this.readChars);
                            throw new DecodingException(stringBuilder2.toString());
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Padding cannot start at index ");
                    stringBuilder.append(this.readChars);
                    throw new DecodingException(stringBuilder.toString());
                }

                public void close() throws IOException {
                    reader.close();
                }
            };
        }

        public BaseEncoding omitPadding() {
            return this.paddingChar == null ? this : new StandardBaseEncoding(this.alphabet, null);
        }

        public BaseEncoding withPadChar(char padChar) {
            if (8 % this.alphabet.bitsPerChar == 0 || (this.paddingChar != null && this.paddingChar.charValue() == padChar)) {
                return this;
            }
            return new StandardBaseEncoding(this.alphabet, Character.valueOf(padChar));
        }

        public BaseEncoding withSeparator(String separator, int afterEveryChars) {
            Preconditions.checkNotNull(separator);
            Preconditions.checkArgument(padding().or(this.alphabet).matchesNoneOf(separator), "Separator cannot contain alphabet or padding characters");
            return new SeparatedBaseEncoding(this, separator, afterEveryChars);
        }

        public BaseEncoding upperCase() {
            BaseEncoding result = this.upperCase;
            if (result != null) {
                return result;
            }
            Alphabet upper = this.alphabet.upperCase();
            BaseEncoding standardBaseEncoding = upper == this.alphabet ? this : new StandardBaseEncoding(upper, this.paddingChar);
            this.upperCase = standardBaseEncoding;
            return standardBaseEncoding;
        }

        public BaseEncoding lowerCase() {
            BaseEncoding result = this.lowerCase;
            if (result != null) {
                return result;
            }
            Alphabet lower = this.alphabet.lowerCase();
            BaseEncoding standardBaseEncoding = lower == this.alphabet ? this : new StandardBaseEncoding(lower, this.paddingChar);
            this.lowerCase = standardBaseEncoding;
            return standardBaseEncoding;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder("BaseEncoding.");
            builder.append(this.alphabet.toString());
            if (8 % this.alphabet.bitsPerChar != 0) {
                if (this.paddingChar == null) {
                    builder.append(".omitPadding()");
                } else {
                    builder.append(".withPadChar(");
                    builder.append(this.paddingChar);
                    builder.append(')');
                }
            }
            return builder.toString();
        }
    }

    private static final class Alphabet extends CharMatcher {
        final int bitsPerChar;
        final int bytesPerChunk;
        private final char[] chars;
        final int charsPerChunk;
        private final byte[] decodabet;
        final int mask;
        private final String name;
        private final boolean[] validPadding;

        Alphabet(String name, char[] chars) {
            this.name = (String) Preconditions.checkNotNull(name);
            this.chars = (char[]) Preconditions.checkNotNull(chars);
            try {
                this.bitsPerChar = IntMath.log2(chars.length, RoundingMode.UNNECESSARY);
                int gcd = Math.min(8, Integer.lowestOneBit(this.bitsPerChar));
                this.charsPerChunk = 8 / gcd;
                this.bytesPerChunk = this.bitsPerChar / gcd;
                this.mask = chars.length - 1;
                byte[] decodabet = new byte[128];
                Arrays.fill(decodabet, (byte) -1);
                int i = 0;
                for (int i2 = 0; i2 < chars.length; i2++) {
                    char c = chars[i2];
                    Preconditions.checkArgument(CharMatcher.ASCII.matches(c), "Non-ASCII character: %s", Character.valueOf(c));
                    Preconditions.checkArgument(decodabet[c] == (byte) -1, "Duplicate character: %s", Character.valueOf(c));
                    decodabet[c] = (byte) i2;
                }
                this.decodabet = decodabet;
                boolean[] validPadding = new boolean[this.charsPerChunk];
                while (i < this.bytesPerChunk) {
                    validPadding[IntMath.divide(i * 8, this.bitsPerChar, RoundingMode.CEILING)] = true;
                    i++;
                }
                this.validPadding = validPadding;
            } catch (ArithmeticException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal alphabet length ");
                stringBuilder.append(chars.length);
                throw new IllegalArgumentException(stringBuilder.toString(), e);
            }
        }

        /* Access modifiers changed, original: 0000 */
        public char encode(int bits) {
            return this.chars[bits];
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isValidPaddingStartPosition(int index) {
            return this.validPadding[index % this.charsPerChunk];
        }

        /* Access modifiers changed, original: 0000 */
        public int decode(char ch) throws IOException {
            if (ch <= Ascii.MAX && this.decodabet[ch] != (byte) -1) {
                return this.decodabet[ch];
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unrecognized character: ");
            stringBuilder.append(ch);
            throw new DecodingException(stringBuilder.toString());
        }

        private boolean hasLowerCase() {
            for (char c : this.chars) {
                if (Ascii.isLowerCase(c)) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasUpperCase() {
            for (char c : this.chars) {
                if (Ascii.isUpperCase(c)) {
                    return true;
                }
            }
            return false;
        }

        /* Access modifiers changed, original: 0000 */
        public Alphabet upperCase() {
            if (!hasLowerCase()) {
                return this;
            }
            Preconditions.checkState(hasUpperCase() ^ 1, "Cannot call upperCase() on a mixed-case alphabet");
            char[] upperCased = new char[this.chars.length];
            for (int i = 0; i < this.chars.length; i++) {
                upperCased[i] = Ascii.toUpperCase(this.chars[i]);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.name);
            stringBuilder.append(".upperCase()");
            return new Alphabet(stringBuilder.toString(), upperCased);
        }

        /* Access modifiers changed, original: 0000 */
        public Alphabet lowerCase() {
            if (!hasUpperCase()) {
                return this;
            }
            Preconditions.checkState(hasLowerCase() ^ 1, "Cannot call lowerCase() on a mixed-case alphabet");
            char[] lowerCased = new char[this.chars.length];
            for (int i = 0; i < this.chars.length; i++) {
                lowerCased[i] = Ascii.toLowerCase(this.chars[i]);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.name);
            stringBuilder.append(".lowerCase()");
            return new Alphabet(stringBuilder.toString(), lowerCased);
        }

        public boolean matches(char c) {
            return CharMatcher.ASCII.matches(c) && this.decodabet[c] != (byte) -1;
        }

        public String toString() {
            return this.name;
        }
    }

    public abstract ByteInput decodingStream(CharInput charInput);

    public abstract ByteOutput encodingStream(CharOutput charOutput);

    @CheckReturnValue
    public abstract BaseEncoding lowerCase();

    public abstract int maxDecodedSize(int i);

    public abstract int maxEncodedSize(int i);

    @CheckReturnValue
    public abstract BaseEncoding omitPadding();

    public abstract CharMatcher padding();

    @CheckReturnValue
    public abstract BaseEncoding upperCase();

    @CheckReturnValue
    public abstract BaseEncoding withPadChar(char c);

    @CheckReturnValue
    public abstract BaseEncoding withSeparator(String str, int i);

    BaseEncoding() {
    }

    public String encode(byte[] bytes) {
        return encode((byte[]) Preconditions.checkNotNull(bytes), 0, bytes.length);
    }

    public final String encode(byte[] bytes, int off, int len) {
        Preconditions.checkNotNull(bytes);
        Preconditions.checkPositionIndexes(off, off + len, bytes.length);
        CharOutput result = GwtWorkarounds.stringBuilderOutput(maxEncodedSize(len));
        ByteOutput byteOutput = encodingStream(result);
        int i = 0;
        while (i < len) {
            try {
                byteOutput.write(bytes[off + i]);
                i++;
            } catch (IOException e) {
                throw new AssertionError("impossible");
            }
        }
        byteOutput.close();
        return result.toString();
    }

    @GwtIncompatible("Writer,OutputStream")
    public final OutputStream encodingStream(Writer writer) {
        return GwtWorkarounds.asOutputStream(encodingStream(GwtWorkarounds.asCharOutput(writer)));
    }

    @GwtIncompatible("ByteSink,CharSink")
    public final ByteSink encodingSink(final CharSink encodedSink) {
        Preconditions.checkNotNull(encodedSink);
        return new ByteSink() {
            public OutputStream openStream() throws IOException {
                return BaseEncoding.this.encodingStream(encodedSink.openStream());
            }
        };
    }

    private static byte[] extract(byte[] result, int length) {
        if (length == result.length) {
            return result;
        }
        byte[] trunc = new byte[length];
        System.arraycopy(result, 0, trunc, 0, length);
        return trunc;
    }

    public final byte[] decode(CharSequence chars) {
        try {
            return decodeChecked(chars);
        } catch (DecodingException badInput) {
            throw new IllegalArgumentException(badInput);
        }
    }

    /* Access modifiers changed, original: final */
    public final byte[] decodeChecked(CharSequence chars) throws DecodingException {
        DecodingException badInput;
        chars = padding().trimTrailingFrom(chars);
        ByteInput decodedInput = decodingStream(GwtWorkarounds.asCharInput(chars));
        byte[] tmp = new byte[maxDecodedSize(chars.length())];
        int index = 0;
        int index2;
        try {
            int i = decodedInput.read();
            while (i != -1) {
                index2 = index + 1;
                try {
                    tmp[index] = (byte) i;
                    i = decodedInput.read();
                    index = index2;
                } catch (DecodingException e) {
                    badInput = e;
                    throw badInput;
                } catch (IOException e2) {
                    index = e2;
                    throw new AssertionError(index);
                }
            }
            return extract(tmp, index);
        } catch (DecodingException e3) {
            index2 = 0;
            badInput = e3;
            throw badInput;
        } catch (IOException e4) {
            index2 = 0;
            index = e4;
            throw new AssertionError(index);
        }
    }

    @GwtIncompatible("Reader,InputStream")
    public final InputStream decodingStream(Reader reader) {
        return GwtWorkarounds.asInputStream(decodingStream(GwtWorkarounds.asCharInput(reader)));
    }

    @GwtIncompatible("ByteSource,CharSource")
    public final ByteSource decodingSource(final CharSource encodedSource) {
        Preconditions.checkNotNull(encodedSource);
        return new ByteSource() {
            public InputStream openStream() throws IOException {
                return BaseEncoding.this.decodingStream(encodedSource.openStream());
            }
        };
    }

    public static BaseEncoding base64() {
        return BASE64;
    }

    public static BaseEncoding base64Url() {
        return BASE64_URL;
    }

    public static BaseEncoding base32() {
        return BASE32;
    }

    public static BaseEncoding base32Hex() {
        return BASE32_HEX;
    }

    public static BaseEncoding base16() {
        return BASE16;
    }

    static CharInput ignoringInput(final CharInput delegate, final CharMatcher toIgnore) {
        Preconditions.checkNotNull(delegate);
        Preconditions.checkNotNull(toIgnore);
        return new CharInput() {
            public int read() throws IOException {
                int readChar;
                do {
                    readChar = delegate.read();
                    if (readChar == -1) {
                        break;
                    }
                } while (toIgnore.matches((char) readChar));
                return readChar;
            }

            public void close() throws IOException {
                delegate.close();
            }
        };
    }

    static CharOutput separatingOutput(final CharOutput delegate, final String separator, final int afterEveryChars) {
        Preconditions.checkNotNull(delegate);
        Preconditions.checkNotNull(separator);
        Preconditions.checkArgument(afterEveryChars > 0);
        return new CharOutput() {
            int charsUntilSeparator = afterEveryChars;

            public void write(char c) throws IOException {
                if (this.charsUntilSeparator == 0) {
                    for (int i = 0; i < separator.length(); i++) {
                        delegate.write(separator.charAt(i));
                    }
                    this.charsUntilSeparator = afterEveryChars;
                }
                delegate.write(c);
                this.charsUntilSeparator--;
            }

            public void flush() throws IOException {
                delegate.flush();
            }

            public void close() throws IOException {
                delegate.close();
            }
        };
    }
}
