package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@GwtCompatible(emulated = true)
final class LongAdder extends Striped64 implements Serializable, LongAddable {
    private static final long serialVersionUID = 7249069246863182397L;

    /* Access modifiers changed, original: final */
    public final long fn(long v, long x) {
        return v + x;
    }

    public void add(long x) {
        Cell[] cellArr = this.cells;
        Cell[] as = cellArr;
        if (cellArr == null) {
            long j = this.base;
            if (casBase(j, j + x)) {
                return;
            }
        }
        boolean uncontended = true;
        int[] iArr = (int[]) threadHashCode.get();
        int[] hc = iArr;
        if (!(iArr == null || as == null)) {
            int length = as.length;
            int n = length;
            if (length >= 1) {
                Cell cell = as[(n - 1) & hc[0]];
                Cell a = cell;
                if (cell != null) {
                    long j2 = a.value;
                    boolean cas = a.cas(j2, j2 + x);
                    uncontended = cas;
                    if (cas) {
                        return;
                    }
                }
            }
        }
        retryUpdate(x, hc, uncontended);
    }

    public void increment() {
        add(1);
    }

    public void decrement() {
        add(-1);
    }

    public long sum() {
        long sum = this.base;
        Cell[] as = this.cells;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    sum += a.value;
                }
            }
        }
        return sum;
    }

    public void reset() {
        internalReset(0);
    }

    public long sumThenReset() {
        long sum = this.base;
        Cell[] as = this.cells;
        this.base = 0;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    sum += a.value;
                    a.value = 0;
                }
            }
        }
        return sum;
    }

    public String toString() {
        return Long.toString(sum());
    }

    public long longValue() {
        return sum();
    }

    public int intValue() {
        return (int) sum();
    }

    public float floatValue() {
        return (float) sum();
    }

    public double doubleValue() {
        return (double) sum();
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeLong(sum());
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.busy = 0;
        this.cells = null;
        this.base = s.readLong();
    }
}
