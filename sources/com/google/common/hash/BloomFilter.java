package com.google.common.hash;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.primitives.SignedBytes;
import com.google.common.primitives.UnsignedBytes;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import javax.annotation.Nullable;

@Beta
public final class BloomFilter<T> implements Predicate<T>, Serializable {
    private static final Strategy DEFAULT_STRATEGY = BloomFilterStrategies.MURMUR128_MITZ_64;
    private final BitArray bits;
    private final Funnel<? super T> funnel;
    private final int numHashFunctions;
    private final Strategy strategy;

    private static class SerialForm<T> implements Serializable {
        private static final long serialVersionUID = 1;
        final long[] data;
        final Funnel<? super T> funnel;
        final int numHashFunctions;
        final Strategy strategy;

        SerialForm(BloomFilter<T> bf) {
            this.data = bf.bits.data;
            this.numHashFunctions = bf.numHashFunctions;
            this.funnel = bf.funnel;
            this.strategy = bf.strategy;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return new BloomFilter(new BitArray(this.data), this.numHashFunctions, this.funnel, this.strategy);
        }
    }

    interface Strategy extends Serializable {
        <T> boolean mightContain(T t, Funnel<? super T> funnel, int i, BitArray bitArray);

        int ordinal();

        <T> boolean put(T t, Funnel<? super T> funnel, int i, BitArray bitArray);
    }

    private BloomFilter(BitArray bits, int numHashFunctions, Funnel<? super T> funnel, Strategy strategy) {
        Preconditions.checkArgument(numHashFunctions > 0, "numHashFunctions (%s) must be > 0", Integer.valueOf(numHashFunctions));
        Preconditions.checkArgument(numHashFunctions <= 255, "numHashFunctions (%s) must be <= 255", Integer.valueOf(numHashFunctions));
        this.bits = (BitArray) Preconditions.checkNotNull(bits);
        this.numHashFunctions = numHashFunctions;
        this.funnel = (Funnel) Preconditions.checkNotNull(funnel);
        this.strategy = (Strategy) Preconditions.checkNotNull(strategy);
    }

    public BloomFilter<T> copy() {
        return new BloomFilter(this.bits.copy(), this.numHashFunctions, this.funnel, this.strategy);
    }

    public boolean mightContain(T object) {
        return this.strategy.mightContain(object, this.funnel, this.numHashFunctions, this.bits);
    }

    @Deprecated
    public boolean apply(T input) {
        return mightContain(input);
    }

    public boolean put(T object) {
        return this.strategy.put(object, this.funnel, this.numHashFunctions, this.bits);
    }

    public double expectedFpp() {
        return Math.pow(((double) this.bits.bitCount()) / ((double) bitSize()), (double) this.numHashFunctions);
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public long bitSize() {
        return this.bits.bitSize();
    }

    public boolean isCompatible(BloomFilter<T> that) {
        Preconditions.checkNotNull(that);
        return this != that && this.numHashFunctions == that.numHashFunctions && bitSize() == that.bitSize() && this.strategy.equals(that.strategy) && this.funnel.equals(that.funnel);
    }

    public void putAll(BloomFilter<T> that) {
        Preconditions.checkNotNull(that);
        Preconditions.checkArgument(this != that, "Cannot combine a BloomFilter with itself.");
        Preconditions.checkArgument(this.numHashFunctions == that.numHashFunctions, "BloomFilters must have the same number of hash functions (%s != %s)", Integer.valueOf(this.numHashFunctions), Integer.valueOf(that.numHashFunctions));
        Preconditions.checkArgument(bitSize() == that.bitSize(), "BloomFilters must have the same size underlying bit arrays (%s != %s)", Long.valueOf(bitSize()), Long.valueOf(that.bitSize()));
        Preconditions.checkArgument(this.strategy.equals(that.strategy), "BloomFilters must have equal strategies (%s != %s)", this.strategy, that.strategy);
        Preconditions.checkArgument(this.funnel.equals(that.funnel), "BloomFilters must have equal funnels (%s != %s)", this.funnel, that.funnel);
        this.bits.putAll(that.bits);
    }

    public boolean equals(@Nullable Object object) {
        boolean z = true;
        if (object == this) {
            return true;
        }
        if (!(object instanceof BloomFilter)) {
            return false;
        }
        BloomFilter<?> that = (BloomFilter) object;
        if (!(this.numHashFunctions == that.numHashFunctions && this.funnel.equals(that.funnel) && this.bits.equals(that.bits) && this.strategy.equals(that.strategy))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hashCode(Integer.valueOf(this.numHashFunctions), this.funnel, this.strategy, this.bits);
    }

    public static <T> BloomFilter<T> create(Funnel<? super T> funnel, int expectedInsertions, double fpp) {
        return create(funnel, expectedInsertions, fpp, DEFAULT_STRATEGY);
    }

    @VisibleForTesting
    static <T> BloomFilter<T> create(Funnel<? super T> funnel, int expectedInsertions, double fpp, Strategy strategy) {
        Preconditions.checkNotNull(funnel);
        Preconditions.checkArgument(expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", Integer.valueOf(expectedInsertions));
        Preconditions.checkArgument(fpp > Camera2ParamsFragment.TARGET_EV, "False positive probability (%s) must be > 0.0", Double.valueOf(fpp));
        Preconditions.checkArgument(fpp < 1.0d, "False positive probability (%s) must be < 1.0", Double.valueOf(fpp));
        Preconditions.checkNotNull(strategy);
        if (expectedInsertions == 0) {
            expectedInsertions = 1;
        }
        long numBits = optimalNumOfBits((long) expectedInsertions, fpp);
        try {
            return new BloomFilter(new BitArray(numBits), optimalNumOfHashFunctions((long) expectedInsertions, numBits), funnel, strategy);
        } catch (IllegalArgumentException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not create BloomFilter of ");
            stringBuilder.append(numBits);
            stringBuilder.append(" bits");
            throw new IllegalArgumentException(stringBuilder.toString(), e);
        }
    }

    public static <T> BloomFilter<T> create(Funnel<? super T> funnel, int expectedInsertions) {
        return create(funnel, expectedInsertions, 0.03d);
    }

    @VisibleForTesting
    static int optimalNumOfHashFunctions(long n, long m) {
        return Math.max(1, (int) Math.round((((double) m) / ((double) n)) * Math.log(2.0d)));
    }

    @VisibleForTesting
    static long optimalNumOfBits(long n, double p) {
        if (p == Camera2ParamsFragment.TARGET_EV) {
            p = Double.MIN_VALUE;
        }
        return (long) ((((double) (-n)) * Math.log(p)) / (Math.log(2.0d) * Math.log(2.0d)));
    }

    private Object writeReplace() {
        return new SerialForm(this);
    }

    public void writeTo(OutputStream out) throws IOException {
        DataOutputStream dout = new DataOutputStream(out);
        dout.writeByte(SignedBytes.checkedCast((long) this.strategy.ordinal()));
        dout.writeByte(UnsignedBytes.checkedCast((long) this.numHashFunctions));
        dout.writeInt(this.bits.data.length);
        for (long value : this.bits.data) {
            dout.writeLong(value);
        }
    }

    public static <T> BloomFilter<T> readFrom(InputStream in, Funnel<T> funnel) throws IOException {
        Preconditions.checkNotNull(in, "InputStream");
        Preconditions.checkNotNull(funnel, "Funnel");
        try {
            DataInputStream din = new DataInputStream(in);
            byte strategyOrdinal = din.readByte();
            int numHashFunctions = UnsignedBytes.toInt(din.readByte());
            int dataLength = din.readInt();
            Strategy strategy = BloomFilterStrategies.values()[strategyOrdinal];
            long[] data = new long[dataLength];
            for (int i = 0; i < data.length; i++) {
                data[i] = din.readLong();
            }
            return new BloomFilter(new BitArray(data), numHashFunctions, funnel, strategy);
        } catch (RuntimeException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to deserialize BloomFilter from InputStream. strategyOrdinal: ");
            stringBuilder.append(-1);
            stringBuilder.append(" numHashFunctions: ");
            stringBuilder.append(-1);
            stringBuilder.append(" dataLength: ");
            stringBuilder.append(-1);
            IOException ioException = new IOException(stringBuilder.toString());
            ioException.initCause(e);
            throw ioException;
        }
    }
}
