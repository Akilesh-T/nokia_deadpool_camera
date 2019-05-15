package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Supplier;
import java.util.concurrent.atomic.AtomicLong;

@GwtCompatible(emulated = true)
final class LongAddables {
    private static final Supplier<LongAddable> SUPPLIER;

    private static final class PureJavaLongAddable extends AtomicLong implements LongAddable {
        private PureJavaLongAddable() {
        }

        /* synthetic */ PureJavaLongAddable(AnonymousClass1 x0) {
            this();
        }

        public void increment() {
            getAndIncrement();
        }

        public void add(long x) {
            getAndAdd(x);
        }

        public long sum() {
            return get();
        }
    }

    LongAddables() {
    }

    static {
        Supplier<LongAddable> supplier;
        try {
            LongAdder longAdder = new LongAdder();
            supplier = new Supplier<LongAddable>() {
                public LongAddable get() {
                    return new LongAdder();
                }
            };
        } catch (Throwable th) {
            supplier = new Supplier<LongAddable>() {
                public LongAddable get() {
                    return new PureJavaLongAddable();
                }
            };
        }
        SUPPLIER = supplier;
    }

    public static LongAddable create() {
        return (LongAddable) SUPPLIER.get();
    }
}
