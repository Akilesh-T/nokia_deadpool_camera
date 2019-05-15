package com.google.common.cache;

import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

abstract class Striped64 extends Number {
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final AtomicLongFieldUpdater<Striped64> baseUpdater = AtomicLongFieldUpdater.newUpdater(Striped64.class, "base");
    private static final AtomicIntegerFieldUpdater<Striped64> busyUpdater = AtomicIntegerFieldUpdater.newUpdater(Striped64.class, "busy");
    static final Random rng = new Random();
    static final ThreadLocal<int[]> threadHashCode = new ThreadLocal();
    volatile transient long base;
    volatile transient int busy;
    volatile transient Cell[] cells;

    static final class Cell {
        private static final AtomicLongFieldUpdater<Cell> valueUpdater = AtomicLongFieldUpdater.newUpdater(Cell.class, "value");
        volatile long p0;
        volatile long p1;
        volatile long p2;
        volatile long p3;
        volatile long p4;
        volatile long p5;
        volatile long p6;
        volatile long q0;
        volatile long q1;
        volatile long q2;
        volatile long q3;
        volatile long q4;
        volatile long q5;
        volatile long q6;
        volatile long value;

        Cell(long x) {
            this.value = x;
        }

        /* Access modifiers changed, original: final */
        public final boolean cas(long cmp, long val) {
            return valueUpdater.compareAndSet(this, cmp, val);
        }
    }

    public abstract long fn(long j, long j2);

    Striped64() {
    }

    /* Access modifiers changed, original: final */
    public final boolean casBase(long cmp, long val) {
        return baseUpdater.compareAndSet(this, cmp, val);
    }

    /* Access modifiers changed, original: final */
    public final boolean casBusy() {
        return busyUpdater.compareAndSet(this, 0, 1);
    }

    /* Access modifiers changed, original: final */
    public final void retryUpdate(long x, int[] hc, boolean wasUncontended) {
        int r;
        int[] hc2;
        Throwable th;
        long j = x;
        int i = 0;
        if (hc == null) {
            int i2 = 1;
            int[] iArr = new int[1];
            int[] hc3 = iArr;
            threadHashCode.set(iArr);
            r = rng.nextInt();
            if (r != 0) {
                i2 = r;
            }
            hc3[0] = i2;
            r = i2;
            hc2 = hc3;
        } else {
            r = hc[0];
            hc2 = hc;
        }
        boolean wasUncontended2 = wasUncontended;
        boolean collide = false;
        while (true) {
            int i3;
            boolean collide2 = collide;
            Cell[] cellArr = this.cells;
            Cell[] as = cellArr;
            if (cellArr != null) {
                int length = as.length;
                int n = length;
                if (length > 0) {
                    Cell cell = as[(n - 1) & r];
                    Cell a = cell;
                    if (cell == null) {
                        if (this.busy == 0) {
                            Cell r2 = new Cell(j);
                            if (this.busy == 0 && casBusy()) {
                                boolean created = i;
                                try {
                                    cellArr = this.cells;
                                    Cell[] rs = cellArr;
                                    if (cellArr != null) {
                                        length = rs.length;
                                        int m = length;
                                        if (length > 0) {
                                            length = (m - 1) & r;
                                            int j2 = length;
                                            if (rs[length] == null) {
                                                rs[j2] = r2;
                                                created = true;
                                            }
                                        }
                                    }
                                    this.busy = i;
                                    if (created) {
                                        i3 = r;
                                        return;
                                    }
                                    collide = collide2;
                                } catch (Throwable th2) {
                                    this.busy = i;
                                }
                            }
                        }
                        collide2 = false;
                    } else if (wasUncontended2) {
                        long j3 = a.value;
                        i3 = r;
                        if (!a.cas(j3, fn(j3, j))) {
                            if (n >= NCPU || this.cells != as) {
                                collide2 = false;
                                length = i3 ^ (i3 << 13);
                                length ^= length >>> 17;
                                length ^= length << 5;
                                i = 0;
                                hc2[0] = length;
                                r = length;
                                collide = collide2;
                            } else {
                                if (!collide2) {
                                    collide2 = true;
                                } else if (this.busy == 0 && casBusy()) {
                                    try {
                                        if (this.cells == as) {
                                            cellArr = new Cell[(n << 1)];
                                            i = 0;
                                            try {
                                                System.arraycopy(as, 0, cellArr, 0, n);
                                                this.cells = cellArr;
                                            } catch (Throwable th3) {
                                                th = th3;
                                                this.busy = i;
                                                throw th;
                                            }
                                        }
                                        i = 0;
                                        this.busy = 0;
                                        collide = false;
                                        r = i3;
                                    } catch (Throwable th4) {
                                        th = th4;
                                        i = 0;
                                    }
                                }
                                length = i3 ^ (i3 << 13);
                                length ^= length >>> 17;
                                length ^= length << 5;
                                i = 0;
                                hc2[0] = length;
                                r = length;
                                collide = collide2;
                            }
                        } else {
                            return;
                        }
                    } else {
                        wasUncontended2 = true;
                    }
                    i3 = r;
                    length = i3 ^ (i3 << 13);
                    length ^= length >>> 17;
                    length ^= length << 5;
                    i = 0;
                    hc2[0] = length;
                    r = length;
                    collide = collide2;
                }
            }
            i3 = r;
            if (this.busy == 0 && this.cells == as && casBusy()) {
                boolean init = false;
                try {
                    if (this.cells == as) {
                        cellArr = new Cell[2];
                        cellArr[i3 & 1] = new Cell(j);
                        this.cells = cellArr;
                        init = true;
                    }
                    this.busy = 0;
                    if (!init) {
                        i = 0;
                    } else {
                        return;
                    }
                } catch (Throwable th5) {
                    this.busy = 0;
                }
            } else {
                i = 0;
                long j4 = this.base;
                if (casBase(j4, fn(j4, j))) {
                    return;
                }
            }
            collide = collide2;
            r = i3;
        }
    }

    /* Access modifiers changed, original: final */
    public final void internalReset(long initialValue) {
        Cell[] as = this.cells;
        this.base = initialValue;
        if (as != null) {
            for (Cell a : as) {
                if (a != null) {
                    a.value = initialValue;
                }
            }
        }
    }
}
