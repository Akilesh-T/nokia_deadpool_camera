package com.google.common.hash;

import com.google.common.primitives.UnsignedBytes;
import java.io.Serializable;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

final class Murmur3_32HashFunction extends AbstractStreamingHashFunction implements Serializable {
    private static final int C1 = -862048943;
    private static final int C2 = 461845907;
    private static final long serialVersionUID = 0;
    private final int seed;

    private static final class Murmur3_32Hasher extends AbstractStreamingHasher {
        private static final int CHUNK_SIZE = 4;
        private int h1;
        private int length = 0;

        Murmur3_32Hasher(int seed) {
            super(4);
            this.h1 = seed;
        }

        /* Access modifiers changed, original: protected */
        public void process(ByteBuffer bb) {
            this.h1 = Murmur3_32HashFunction.mixH1(this.h1, Murmur3_32HashFunction.mixK1(bb.getInt()));
            this.length += 4;
        }

        /* Access modifiers changed, original: protected */
        public void processRemaining(ByteBuffer bb) {
            this.length += bb.remaining();
            int k1 = 0;
            int i = 0;
            while (bb.hasRemaining()) {
                k1 ^= UnsignedBytes.toInt(bb.get()) << i;
                i += 8;
            }
            this.h1 ^= Murmur3_32HashFunction.mixK1(k1);
        }

        public HashCode makeHash() {
            return Murmur3_32HashFunction.fmix(this.h1, this.length);
        }
    }

    Murmur3_32HashFunction(int seed) {
        this.seed = seed;
    }

    public int bits() {
        return 32;
    }

    public Hasher newHasher() {
        return new Murmur3_32Hasher(this.seed);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hashing.murmur3_32(");
        stringBuilder.append(this.seed);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public boolean equals(@Nullable Object object) {
        boolean z = false;
        if (!(object instanceof Murmur3_32HashFunction)) {
            return false;
        }
        if (this.seed == ((Murmur3_32HashFunction) object).seed) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return getClass().hashCode() ^ this.seed;
    }

    public HashCode hashInt(int input) {
        return fmix(mixH1(this.seed, mixK1(input)), 4);
    }

    public HashCode hashLong(long input) {
        int high = (int) (input >>> 32);
        return fmix(mixH1(mixH1(this.seed, mixK1((int) input)), mixK1(high)), 8);
    }

    public HashCode hashUnencodedChars(CharSequence input) {
        int h1 = this.seed;
        for (int i = 1; i < input.length(); i += 2) {
            h1 = mixH1(h1, mixK1(input.charAt(i - 1) | (input.charAt(i) << 16)));
        }
        if ((input.length() & 1) == 1) {
            h1 ^= mixK1(input.charAt(input.length() - 1));
        }
        return fmix(h1, 2 * input.length());
    }

    private static int mixK1(int k1) {
        return Integer.rotateLeft(k1 * C1, 15) * C2;
    }

    private static int mixH1(int h1, int k1) {
        return (Integer.rotateLeft(h1 ^ k1, 13) * 5) - 430675100;
    }

    private static HashCode fmix(int h1, int length) {
        h1 ^= length;
        h1 = (h1 ^ (h1 >>> 16)) * -2048144789;
        h1 = (h1 ^ (h1 >>> 13)) * -1028477387;
        return HashCode.fromInt(h1 ^ (h1 >>> 16));
    }
}
