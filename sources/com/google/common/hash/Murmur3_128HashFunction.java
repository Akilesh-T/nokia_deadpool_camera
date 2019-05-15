package com.google.common.hash;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.annotation.Nullable;

final class Murmur3_128HashFunction extends AbstractStreamingHashFunction implements Serializable {
    private static final long serialVersionUID = 0;
    private final int seed;

    private static final class Murmur3_128Hasher extends AbstractStreamingHasher {
        private static final long C1 = -8663945395140668459L;
        private static final long C2 = 5545529020109919103L;
        private static final int CHUNK_SIZE = 16;
        private long h1;
        private long h2;
        private int length = 0;

        Murmur3_128Hasher(int seed) {
            super(16);
            this.h1 = (long) seed;
            this.h2 = (long) seed;
        }

        /* Access modifiers changed, original: protected */
        public void process(ByteBuffer bb) {
            bmix64(bb.getLong(), bb.getLong());
            this.length += 16;
        }

        private void bmix64(long k1, long k2) {
            this.h1 ^= mixK1(k1);
            this.h1 = Long.rotateLeft(this.h1, 27);
            this.h1 += this.h2;
            this.h1 = (this.h1 * 5) + 1390208809;
            this.h2 ^= mixK2(k2);
            this.h2 = Long.rotateLeft(this.h2, 31);
            this.h2 += this.h1;
            this.h2 = (this.h2 * 5) + 944331445;
        }

        /* Access modifiers changed, original: protected */
        /* JADX WARNING: Missing block: B:5:0x0035, code skipped:
            r2 = r2 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(13))) << 40);
     */
        /* JADX WARNING: Missing block: B:6:0x0043, code skipped:
            r2 = r2 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(12))) << 32);
     */
        /* JADX WARNING: Missing block: B:7:0x0051, code skipped:
            r2 = r2 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(11))) << 24);
     */
        /* JADX WARNING: Missing block: B:8:0x005f, code skipped:
            r2 = r2 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(10))) << 16);
     */
        /* JADX WARNING: Missing block: B:9:0x006d, code skipped:
            r2 = r2 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(9))) << 8);
     */
        /* JADX WARNING: Missing block: B:10:0x007a, code skipped:
            r2 = r2 ^ ((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(8)));
     */
        /* JADX WARNING: Missing block: B:11:0x0084, code skipped:
            r0 = 0 ^ r14.getLong();
     */
        /* JADX WARNING: Missing block: B:13:0x0096, code skipped:
            r0 = r0 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(5))) << 40);
     */
        /* JADX WARNING: Missing block: B:14:0x00a3, code skipped:
            r0 = r0 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(4))) << 32);
     */
        /* JADX WARNING: Missing block: B:15:0x00b0, code skipped:
            r0 = r0 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(3))) << 24);
     */
        /* JADX WARNING: Missing block: B:16:0x00bd, code skipped:
            r0 = r0 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(2))) << 16);
     */
        /* JADX WARNING: Missing block: B:17:0x00ca, code skipped:
            r0 = r0 ^ (((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(1))) << 8);
     */
        /* JADX WARNING: Missing block: B:18:0x00d6, code skipped:
            r0 = r0 ^ ((long) com.google.common.primitives.UnsignedBytes.toInt(r14.get(0)));
     */
        /* JADX WARNING: Missing block: B:19:0x00e2, code skipped:
            r13.h1 ^= mixK1(r0);
            r13.h2 ^= mixK2(r2);
     */
        /* JADX WARNING: Missing block: B:20:0x00f4, code skipped:
            return;
     */
        public void processRemaining(java.nio.ByteBuffer r14) {
            /*
            r13 = this;
            r0 = 0;
            r2 = 0;
            r4 = r13.length;
            r5 = r14.remaining();
            r4 = r4 + r5;
            r13.length = r4;
            r4 = r14.remaining();
            r5 = 16;
            r6 = 24;
            r7 = 32;
            r8 = 40;
            r9 = 48;
            r10 = 8;
            switch(r4) {
                case 1: goto L_0x00d6;
                case 2: goto L_0x00ca;
                case 3: goto L_0x00bd;
                case 4: goto L_0x00b0;
                case 5: goto L_0x00a3;
                case 6: goto L_0x0096;
                case 7: goto L_0x008a;
                case 8: goto L_0x0084;
                case 9: goto L_0x007a;
                case 10: goto L_0x006d;
                case 11: goto L_0x005f;
                case 12: goto L_0x0051;
                case 13: goto L_0x0043;
                case 14: goto L_0x0035;
                case 15: goto L_0x0028;
                default: goto L_0x0020;
            };
        L_0x0020:
            r4 = new java.lang.AssertionError;
            r5 = "Should never get here.";
            r4.<init>(r5);
            throw r4;
        L_0x0028:
            r4 = 14;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r11 = (long) r4;
            r11 = r11 << r9;
            r2 = r2 ^ r11;
        L_0x0035:
            r4 = 13;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r11 = (long) r4;
            r8 = r11 << r8;
            r2 = r2 ^ r8;
        L_0x0043:
            r4 = 12;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r8 = (long) r4;
            r7 = r8 << r7;
            r2 = r2 ^ r7;
        L_0x0051:
            r4 = 11;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r7 = (long) r4;
            r6 = r7 << r6;
            r2 = r2 ^ r6;
        L_0x005f:
            r4 = 10;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r6 = (long) r4;
            r4 = r6 << r5;
            r2 = r2 ^ r4;
        L_0x006d:
            r4 = 9;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r4 = (long) r4;
            r4 = r4 << r10;
            r2 = r2 ^ r4;
        L_0x007a:
            r4 = r14.get(r10);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r4 = (long) r4;
            r2 = r2 ^ r4;
        L_0x0084:
            r4 = r14.getLong();
            r0 = r0 ^ r4;
            goto L_0x00e2;
        L_0x008a:
            r4 = 6;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r11 = (long) r4;
            r11 = r11 << r9;
            r0 = r0 ^ r11;
        L_0x0096:
            r4 = 5;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r11 = (long) r4;
            r8 = r11 << r8;
            r0 = r0 ^ r8;
        L_0x00a3:
            r4 = 4;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r8 = (long) r4;
            r7 = r8 << r7;
            r0 = r0 ^ r7;
        L_0x00b0:
            r4 = 3;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r7 = (long) r4;
            r6 = r7 << r6;
            r0 = r0 ^ r6;
        L_0x00bd:
            r4 = 2;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r6 = (long) r4;
            r4 = r6 << r5;
            r0 = r0 ^ r4;
        L_0x00ca:
            r4 = 1;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r4 = (long) r4;
            r4 = r4 << r10;
            r0 = r0 ^ r4;
        L_0x00d6:
            r4 = 0;
            r4 = r14.get(r4);
            r4 = com.google.common.primitives.UnsignedBytes.toInt(r4);
            r4 = (long) r4;
            r0 = r0 ^ r4;
        L_0x00e2:
            r4 = r13.h1;
            r6 = mixK1(r0);
            r4 = r4 ^ r6;
            r13.h1 = r4;
            r4 = r13.h2;
            r6 = mixK2(r2);
            r4 = r4 ^ r6;
            r13.h2 = r4;
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.hash.Murmur3_128HashFunction$Murmur3_128Hasher.processRemaining(java.nio.ByteBuffer):void");
        }

        public HashCode makeHash() {
            this.h1 ^= (long) this.length;
            this.h2 ^= (long) this.length;
            this.h1 += this.h2;
            this.h2 += this.h1;
            this.h1 = fmix64(this.h1);
            this.h2 = fmix64(this.h2);
            this.h1 += this.h2;
            this.h2 += this.h1;
            return HashCode.fromBytesNoCopy(ByteBuffer.wrap(new byte[16]).order(ByteOrder.LITTLE_ENDIAN).putLong(this.h1).putLong(this.h2).array());
        }

        private static long fmix64(long k) {
            k = (k ^ (k >>> 33)) * -49064778989728563L;
            k = (k ^ (k >>> 33)) * -4265267296055464877L;
            return k ^ (k >>> 33);
        }

        private static long mixK1(long k1) {
            return Long.rotateLeft(k1 * C1, 31) * C2;
        }

        private static long mixK2(long k2) {
            return Long.rotateLeft(k2 * C2, 33) * C1;
        }
    }

    Murmur3_128HashFunction(int seed) {
        this.seed = seed;
    }

    public int bits() {
        return 128;
    }

    public Hasher newHasher() {
        return new Murmur3_128Hasher(this.seed);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Hashing.murmur3_128(");
        stringBuilder.append(this.seed);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public boolean equals(@Nullable Object object) {
        boolean z = false;
        if (!(object instanceof Murmur3_128HashFunction)) {
            return false;
        }
        if (this.seed == ((Murmur3_128HashFunction) object).seed) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return getClass().hashCode() ^ this.seed;
    }
}
