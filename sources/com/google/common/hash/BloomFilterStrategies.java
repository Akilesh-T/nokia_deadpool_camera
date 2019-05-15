package com.google.common.hash;

import com.google.common.base.Preconditions;
import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.math.RoundingMode;
import java.util.Arrays;

enum BloomFilterStrategies implements Strategy {
    MURMUR128_MITZ_32 {
        public <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits) {
            long bitSize = bits.bitSize();
            long hash64 = Hashing.murmur3_128().hashObject(object, funnel).asLong();
            int hash1 = (int) hash64;
            int hash2 = (int) (hash64 >>> 32);
            boolean bitsChanged = false;
            for (int i = 1; i <= numHashFunctions; i++) {
                int combinedHash = (i * hash2) + hash1;
                if (combinedHash < 0) {
                    combinedHash = ~combinedHash;
                }
                bitsChanged |= bits.set(((long) combinedHash) % bitSize);
            }
            return bitsChanged;
        }

        public <T> boolean mightContain(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits) {
            long bitSize = bits.bitSize();
            long hash64 = Hashing.murmur3_128().hashObject(object, funnel).asLong();
            int hash1 = (int) hash64;
            int hash2 = (int) (hash64 >>> 32);
            for (int i = 1; i <= numHashFunctions; i++) {
                int combinedHash = (i * hash2) + hash1;
                if (combinedHash < 0) {
                    combinedHash = ~combinedHash;
                }
                if (!bits.get(((long) combinedHash) % bitSize)) {
                    return false;
                }
            }
            return true;
        }
    },
    MURMUR128_MITZ_64 {
        public <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits) {
            byte[] bytes;
            long bitSize = bits.bitSize();
            byte[] bytes2 = Hashing.murmur3_128().hashObject(object, funnel).getBytesInternal();
            long hash1 = lowerEight(bytes2);
            long hash2 = upperEight(bytes2);
            boolean bitsChanged = false;
            long combinedHash = hash1;
            int i = 0;
            while (i < numHashFunctions) {
                bytes = bytes2;
                bitsChanged |= bits.set((Long.MAX_VALUE & combinedHash) % bitSize);
                combinedHash += hash2;
                i++;
                bytes2 = bytes;
                T t = object;
            }
            BitArray bitArray = bits;
            bytes = bytes2;
            return bitsChanged;
        }

        public <T> boolean mightContain(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits) {
            long combinedHash;
            long bitSize = bits.bitSize();
            byte[] bytes = Hashing.murmur3_128().hashObject(object, funnel).getBytesInternal();
            long hash1 = lowerEight(bytes);
            long hash2 = upperEight(bytes);
            long combinedHash2 = hash1;
            int i = 0;
            while (i < numHashFunctions) {
                combinedHash = combinedHash2;
                if (!bits.get((Long.MAX_VALUE & combinedHash2) % bitSize)) {
                    return false;
                }
                i++;
                combinedHash2 = combinedHash + hash2;
            }
            combinedHash = combinedHash2;
            BitArray bitArray = bits;
            return true;
        }

        private long lowerEight(byte[] bytes) {
            return Longs.fromBytes(bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
        }

        private long upperEight(byte[] bytes) {
            return Longs.fromBytes(bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8]);
        }
    };

    static final class BitArray {
        long bitCount;
        final long[] data;

        BitArray(long bits) {
            this(new long[Ints.checkedCast(LongMath.divide(bits, 64, RoundingMode.CEILING))]);
        }

        BitArray(long[] data) {
            int i = 0;
            Preconditions.checkArgument(data.length > 0, "data length is zero!");
            this.data = data;
            long bitCount = 0;
            while (i < data.length) {
                bitCount += (long) Long.bitCount(data[i]);
                i++;
            }
            this.bitCount = bitCount;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean set(long index) {
            if (get(index)) {
                return false;
            }
            long[] jArr = this.data;
            int i = (int) (index >>> 6);
            jArr[i] = jArr[i] | (1 << ((int) index));
            this.bitCount++;
            return true;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean get(long index) {
            return (this.data[(int) (index >>> 6)] & (1 << ((int) index))) != 0;
        }

        /* Access modifiers changed, original: 0000 */
        public long bitSize() {
            return ((long) this.data.length) * 64;
        }

        /* Access modifiers changed, original: 0000 */
        public long bitCount() {
            return this.bitCount;
        }

        /* Access modifiers changed, original: 0000 */
        public BitArray copy() {
            return new BitArray((long[]) this.data.clone());
        }

        /* Access modifiers changed, original: 0000 */
        public void putAll(BitArray array) {
            int i = 0;
            Preconditions.checkArgument(this.data.length == array.data.length, "BitArrays must be of equal length (%s != %s)", Integer.valueOf(this.data.length), Integer.valueOf(array.data.length));
            this.bitCount = 0;
            while (true) {
                int i2 = i;
                if (i2 < this.data.length) {
                    long[] jArr = this.data;
                    jArr[i2] = jArr[i2] | array.data[i2];
                    this.bitCount += (long) Long.bitCount(this.data[i2]);
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }

        public boolean equals(Object o) {
            if (!(o instanceof BitArray)) {
                return false;
            }
            return Arrays.equals(this.data, ((BitArray) o).data);
        }

        public int hashCode() {
            return Arrays.hashCode(this.data);
        }
    }
}
