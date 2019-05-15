package com.google.common.hash;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedInts;
import java.io.Serializable;
import java.security.MessageDigest;
import javax.annotation.Nullable;

@Beta
public abstract class HashCode {
    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    private static final class BytesHashCode extends HashCode implements Serializable {
        private static final long serialVersionUID = 0;
        final byte[] bytes;

        BytesHashCode(byte[] bytes) {
            this.bytes = (byte[]) Preconditions.checkNotNull(bytes);
        }

        public int bits() {
            return this.bytes.length * 8;
        }

        public byte[] asBytes() {
            return (byte[]) this.bytes.clone();
        }

        public int asInt() {
            Preconditions.checkState(this.bytes.length >= 4, "HashCode#asInt() requires >= 4 bytes (it only has %s bytes).", Integer.valueOf(this.bytes.length));
            return (((this.bytes[0] & 255) | ((this.bytes[1] & 255) << 8)) | ((this.bytes[2] & 255) << 16)) | ((this.bytes[3] & 255) << 24);
        }

        public long asLong() {
            Preconditions.checkState(this.bytes.length >= 8, "HashCode#asLong() requires >= 8 bytes (it only has %s bytes).", Integer.valueOf(this.bytes.length));
            return padToLong();
        }

        public long padToLong() {
            long retVal = (long) (this.bytes[0] & 255);
            for (int i = 1; i < Math.min(this.bytes.length, 8); i++) {
                retVal |= (((long) this.bytes[i]) & 255) << (i * 8);
            }
            return retVal;
        }

        /* Access modifiers changed, original: 0000 */
        public void writeBytesToImpl(byte[] dest, int offset, int maxLength) {
            System.arraycopy(this.bytes, 0, dest, offset, maxLength);
        }

        /* Access modifiers changed, original: 0000 */
        public byte[] getBytesInternal() {
            return this.bytes;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean equalsSameBits(HashCode that) {
            return MessageDigest.isEqual(this.bytes, that.getBytesInternal());
        }
    }

    private static final class IntHashCode extends HashCode implements Serializable {
        private static final long serialVersionUID = 0;
        final int hash;

        IntHashCode(int hash) {
            this.hash = hash;
        }

        public int bits() {
            return 32;
        }

        public byte[] asBytes() {
            return new byte[]{(byte) this.hash, (byte) (this.hash >> 8), (byte) (this.hash >> 16), (byte) (this.hash >> 24)};
        }

        public int asInt() {
            return this.hash;
        }

        public long asLong() {
            throw new IllegalStateException("this HashCode only has 32 bits; cannot create a long");
        }

        public long padToLong() {
            return UnsignedInts.toLong(this.hash);
        }

        /* Access modifiers changed, original: 0000 */
        public void writeBytesToImpl(byte[] dest, int offset, int maxLength) {
            for (int i = 0; i < maxLength; i++) {
                dest[offset + i] = (byte) (this.hash >> (i * 8));
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean equalsSameBits(HashCode that) {
            return this.hash == that.asInt();
        }
    }

    private static final class LongHashCode extends HashCode implements Serializable {
        private static final long serialVersionUID = 0;
        final long hash;

        LongHashCode(long hash) {
            this.hash = hash;
        }

        public int bits() {
            return 64;
        }

        public byte[] asBytes() {
            return new byte[]{(byte) ((int) this.hash), (byte) ((int) (this.hash >> 8)), (byte) ((int) (this.hash >> 16)), (byte) ((int) (this.hash >> 24)), (byte) ((int) (this.hash >> 32)), (byte) ((int) (this.hash >> 40)), (byte) ((int) (this.hash >> 48)), (byte) ((int) (this.hash >> 56))};
        }

        public int asInt() {
            return (int) this.hash;
        }

        public long asLong() {
            return this.hash;
        }

        public long padToLong() {
            return this.hash;
        }

        /* Access modifiers changed, original: 0000 */
        public void writeBytesToImpl(byte[] dest, int offset, int maxLength) {
            for (int i = 0; i < maxLength; i++) {
                dest[offset + i] = (byte) ((int) (this.hash >> (i * 8)));
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean equalsSameBits(HashCode that) {
            return this.hash == that.asLong();
        }
    }

    public abstract byte[] asBytes();

    public abstract int asInt();

    public abstract long asLong();

    public abstract int bits();

    public abstract boolean equalsSameBits(HashCode hashCode);

    public abstract long padToLong();

    public abstract void writeBytesToImpl(byte[] bArr, int i, int i2);

    HashCode() {
    }

    public int writeBytesTo(byte[] dest, int offset, int maxLength) {
        maxLength = Ints.min(maxLength, bits() / 8);
        Preconditions.checkPositionIndexes(offset, offset + maxLength, dest.length);
        writeBytesToImpl(dest, offset, maxLength);
        return maxLength;
    }

    /* Access modifiers changed, original: 0000 */
    public byte[] getBytesInternal() {
        return asBytes();
    }

    public static HashCode fromInt(int hash) {
        return new IntHashCode(hash);
    }

    public static HashCode fromLong(long hash) {
        return new LongHashCode(hash);
    }

    public static HashCode fromBytes(byte[] bytes) {
        boolean z = true;
        if (bytes.length < 1) {
            z = false;
        }
        Preconditions.checkArgument(z, "A HashCode must contain at least 1 byte.");
        return fromBytesNoCopy((byte[]) bytes.clone());
    }

    static HashCode fromBytesNoCopy(byte[] bytes) {
        return new BytesHashCode(bytes);
    }

    public static HashCode fromString(String string) {
        int i = 0;
        Preconditions.checkArgument(string.length() >= 2, "input string (%s) must have at least 2 characters", string);
        Preconditions.checkArgument(string.length() % 2 == 0, "input string (%s) must have an even number of characters", string);
        byte[] bytes = new byte[(string.length() / 2)];
        while (i < string.length()) {
            bytes[i / 2] = (byte) ((decode(string.charAt(i)) << 4) + decode(string.charAt(i + 1)));
            i += 2;
        }
        return fromBytesNoCopy(bytes);
    }

    private static int decode(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - 48;
        }
        if (ch >= 'a' && ch <= 'f') {
            return (ch - 97) + 10;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Illegal hexadecimal character: ");
        stringBuilder.append(ch);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public final boolean equals(@Nullable Object object) {
        boolean z = false;
        if (!(object instanceof HashCode)) {
            return false;
        }
        HashCode that = (HashCode) object;
        if (bits() == that.bits() && equalsSameBits(that)) {
            z = true;
        }
        return z;
    }

    public final int hashCode() {
        if (bits() >= 32) {
            return asInt();
        }
        byte[] bytes = asBytes();
        int val = bytes[0] & 255;
        for (int i = 1; i < bytes.length; i++) {
            val |= (bytes[i] & 255) << (i * 8);
        }
        return val;
    }

    public final String toString() {
        byte[] bytes = asBytes();
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(hexDigits[(b >> 4) & 15]);
            sb.append(hexDigits[b & 15]);
        }
        return sb.toString();
    }
}
