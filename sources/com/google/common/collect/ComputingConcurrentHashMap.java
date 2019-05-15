package com.google.common.collect;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

class ComputingConcurrentHashMap<K, V> extends MapMakerInternalMap<K, V> {
    private static final long serialVersionUID = 4;
    final Function<? super K, ? extends V> computingFunction;

    private static final class ComputationExceptionReference<K, V> implements ValueReference<K, V> {
        final Throwable t;

        ComputationExceptionReference(Throwable t) {
            this.t = t;
        }

        public V get() {
            return null;
        }

        public ReferenceEntry<K, V> getEntry() {
            return null;
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            return this;
        }

        public boolean isComputingReference() {
            return false;
        }

        public V waitForValue() throws ExecutionException {
            throw new ExecutionException(this.t);
        }

        public void clear(ValueReference<K, V> valueReference) {
        }
    }

    private static final class ComputedReference<K, V> implements ValueReference<K, V> {
        final V value;

        ComputedReference(@Nullable V value) {
            this.value = value;
        }

        public V get() {
            return this.value;
        }

        public ReferenceEntry<K, V> getEntry() {
            return null;
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            return this;
        }

        public boolean isComputingReference() {
            return false;
        }

        public V waitForValue() {
            return get();
        }

        public void clear(ValueReference<K, V> valueReference) {
        }
    }

    static final class ComputingSegment<K, V> extends Segment<K, V> {
        ComputingSegment(MapMakerInternalMap<K, V> map, int initialCapacity, int maxSegmentSize) {
            super(map, initialCapacity, maxSegmentSize);
        }

        /* Access modifiers changed, original: 0000 */
        /* JADX WARNING: Removed duplicated region for block: B:41:0x00a8 A:{Catch:{ all -> 0x00ed, all -> 0x00f5 }} */
        /* JADX WARNING: Removed duplicated region for block: B:64:0x00c8 A:{SYNTHETIC} */
        /* JADX WARNING: Missing block: B:11:0x001e, code skipped:
            if (r0.getValueReference().isComputingReference() == false) goto L_0x0020;
     */
        public V getOrCompute(K r12, int r13, com.google.common.base.Function<? super K, ? extends V> r14) throws java.util.concurrent.ExecutionException {
            /*
            r11 = this;
        L_0x0000:
            r0 = r11.getEntry(r12, r13);	 Catch:{ all -> 0x00f5 }
            if (r0 == 0) goto L_0x0014;
        L_0x0006:
            r1 = r11.getLiveValue(r0);	 Catch:{ all -> 0x00f5 }
            if (r1 == 0) goto L_0x0014;
        L_0x000c:
            r11.recordRead(r0);	 Catch:{ all -> 0x00f5 }
            r11.postReadCleanup();
            return r1;
        L_0x0014:
            if (r0 == 0) goto L_0x0020;
        L_0x0016:
            r1 = r0.getValueReference();	 Catch:{ all -> 0x00f5 }
            r1 = r1.isComputingReference();	 Catch:{ all -> 0x00f5 }
            if (r1 != 0) goto L_0x00d0;
        L_0x0020:
            r1 = 1;
            r2 = 0;
            r11.lock();	 Catch:{ all -> 0x00f5 }
            r11.preWriteCleanup();	 Catch:{ all -> 0x00ed }
            r3 = r11.count;	 Catch:{ all -> 0x00ed }
            r3 = r3 + -1;
            r4 = r11.table;	 Catch:{ all -> 0x00ed }
            r5 = r4.length();	 Catch:{ all -> 0x00ed }
            r5 = r5 + -1;
            r5 = r5 & r13;
            r6 = r4.get(r5);	 Catch:{ all -> 0x00ed }
            r6 = (com.google.common.collect.MapMakerInternalMap.ReferenceEntry) r6;	 Catch:{ all -> 0x00ed }
            r0 = r6;
        L_0x003c:
            if (r0 == 0) goto L_0x00a6;
        L_0x003e:
            r7 = r0.getKey();	 Catch:{ all -> 0x00ed }
            r8 = r0.getHash();	 Catch:{ all -> 0x00ed }
            if (r8 != r13) goto L_0x00a0;
        L_0x0048:
            if (r7 == 0) goto L_0x00a0;
        L_0x004a:
            r8 = r11.map;	 Catch:{ all -> 0x00ed }
            r8 = r8.keyEquivalence;	 Catch:{ all -> 0x00ed }
            r8 = r8.equivalent(r12, r7);	 Catch:{ all -> 0x00ed }
            if (r8 == 0) goto L_0x00a0;
        L_0x0054:
            r8 = r0.getValueReference();	 Catch:{ all -> 0x00ed }
            r9 = r8.isComputingReference();	 Catch:{ all -> 0x00ed }
            if (r9 == 0) goto L_0x0060;
        L_0x005e:
            r1 = 0;
            goto L_0x00a6;
        L_0x0060:
            r9 = r0.getValueReference();	 Catch:{ all -> 0x00ed }
            r9 = r9.get();	 Catch:{ all -> 0x00ed }
            if (r9 != 0) goto L_0x0070;
        L_0x006a:
            r10 = com.google.common.collect.MapMaker.RemovalCause.COLLECTED;	 Catch:{ all -> 0x00ed }
            r11.enqueueNotification(r7, r13, r9, r10);	 Catch:{ all -> 0x00ed }
            goto L_0x0085;
        L_0x0070:
            r10 = r11.map;	 Catch:{ all -> 0x00ed }
            r10 = r10.expires();	 Catch:{ all -> 0x00ed }
            if (r10 == 0) goto L_0x0092;
        L_0x0078:
            r10 = r11.map;	 Catch:{ all -> 0x00ed }
            r10 = r10.isExpired(r0);	 Catch:{ all -> 0x00ed }
            if (r10 == 0) goto L_0x0092;
        L_0x0080:
            r10 = com.google.common.collect.MapMaker.RemovalCause.EXPIRED;	 Catch:{ all -> 0x00ed }
            r11.enqueueNotification(r7, r13, r9, r10);	 Catch:{ all -> 0x00ed }
        L_0x0085:
            r10 = r11.evictionQueue;	 Catch:{ all -> 0x00ed }
            r10.remove(r0);	 Catch:{ all -> 0x00ed }
            r10 = r11.expirationQueue;	 Catch:{ all -> 0x00ed }
            r10.remove(r0);	 Catch:{ all -> 0x00ed }
            r11.count = r3;	 Catch:{ all -> 0x00ed }
            goto L_0x00a6;
        L_0x0092:
            r11.recordLockedRead(r0);	 Catch:{ all -> 0x00ed }
            r11.unlock();	 Catch:{ all -> 0x00f5 }
            r11.postWriteCleanup();	 Catch:{ all -> 0x00f5 }
            r11.postReadCleanup();
            return r9;
        L_0x00a0:
            r7 = r0.getNext();	 Catch:{ all -> 0x00ed }
            r0 = r7;
            goto L_0x003c;
        L_0x00a6:
            if (r1 == 0) goto L_0x00bf;
        L_0x00a8:
            r7 = new com.google.common.collect.ComputingConcurrentHashMap$ComputingValueReference;	 Catch:{ all -> 0x00ed }
            r7.<init>(r14);	 Catch:{ all -> 0x00ed }
            r2 = r7;
            if (r0 != 0) goto L_0x00bc;
        L_0x00b0:
            r7 = r11.newEntry(r12, r13, r6);	 Catch:{ all -> 0x00ed }
            r0 = r7;
            r0.setValueReference(r2);	 Catch:{ all -> 0x00ed }
            r4.set(r5, r0);	 Catch:{ all -> 0x00ed }
            goto L_0x00bf;
        L_0x00bc:
            r0.setValueReference(r2);	 Catch:{ all -> 0x00ed }
        L_0x00bf:
            r11.unlock();	 Catch:{ all -> 0x00f5 }
            r11.postWriteCleanup();	 Catch:{ all -> 0x00f5 }
            if (r1 == 0) goto L_0x00d0;
        L_0x00c8:
            r3 = r11.compute(r12, r13, r0, r2);	 Catch:{ all -> 0x00f5 }
            r11.postReadCleanup();
            return r3;
        L_0x00d0:
            r1 = java.lang.Thread.holdsLock(r0);	 Catch:{ all -> 0x00f5 }
            r1 = r1 ^ 1;
            r2 = "Recursive computation";
            com.google.common.base.Preconditions.checkState(r1, r2);	 Catch:{ all -> 0x00f5 }
            r1 = r0.getValueReference();	 Catch:{ all -> 0x00f5 }
            r1 = r1.waitForValue();	 Catch:{ all -> 0x00f5 }
            if (r1 == 0) goto L_0x0000;
        L_0x00e5:
            r11.recordRead(r0);	 Catch:{ all -> 0x00f5 }
            r11.postReadCleanup();
            return r1;
        L_0x00ed:
            r3 = move-exception;
            r11.unlock();	 Catch:{ all -> 0x00f5 }
            r11.postWriteCleanup();	 Catch:{ all -> 0x00f5 }
            throw r3;	 Catch:{ all -> 0x00f5 }
        L_0x00f5:
            r0 = move-exception;
            r11.postReadCleanup();
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.collect.ComputingConcurrentHashMap$ComputingSegment.getOrCompute(java.lang.Object, int, com.google.common.base.Function):java.lang.Object");
        }

        /* Access modifiers changed, original: 0000 */
        public V compute(K key, int hash, ReferenceEntry<K, V> e, ComputingValueReference<K, V> computingValueReference) throws ExecutionException {
            V value = null;
            long start = System.nanoTime();
            long end = 0;
            try {
                synchronized (e) {
                    value = computingValueReference.compute(key, hash);
                    end = System.nanoTime();
                }
                if (value != null) {
                    if (put(key, hash, value, true) != null) {
                        enqueueNotification(key, hash, value, RemovalCause.REPLACED);
                    }
                }
                if (end == 0) {
                    end = System.nanoTime();
                }
                if (value == null) {
                    clearValue(key, hash, computingValueReference);
                }
                return value;
            } catch (Throwable th) {
                if (end == 0) {
                    end = System.nanoTime();
                }
                if (value == null) {
                    clearValue(key, hash, computingValueReference);
                }
            }
        }
    }

    private static final class ComputingValueReference<K, V> implements ValueReference<K, V> {
        @GuardedBy("ComputingValueReference.this")
        volatile ValueReference<K, V> computedReference = MapMakerInternalMap.unset();
        final Function<? super K, ? extends V> computingFunction;

        public ComputingValueReference(Function<? super K, ? extends V> computingFunction) {
            this.computingFunction = computingFunction;
        }

        public V get() {
            return null;
        }

        public ReferenceEntry<K, V> getEntry() {
            return null;
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, @Nullable V v, ReferenceEntry<K, V> referenceEntry) {
            return this;
        }

        public boolean isComputingReference() {
            return true;
        }

        /* JADX WARNING: Missing block: B:23:0x0024, code skipped:
            r0 = th;
     */
        public V waitForValue() throws java.util.concurrent.ExecutionException {
            /*
            r4 = this;
            r0 = r4.computedReference;
            r1 = com.google.common.collect.MapMakerInternalMap.UNSET;
            if (r0 != r1) goto L_0x0034;
        L_0x0006:
            r0 = 0;
            monitor-enter(r4);	 Catch:{ all -> 0x0026 }
            r1 = r0;
        L_0x0009:
            r0 = r4.computedReference;	 Catch:{ all -> 0x0021 }
            r2 = com.google.common.collect.MapMakerInternalMap.UNSET;	 Catch:{ all -> 0x0021 }
            if (r0 != r2) goto L_0x0016;
        L_0x000f:
            r4.wait();	 Catch:{ InterruptedException -> 0x0013 }
        L_0x0012:
            goto L_0x0009;
        L_0x0013:
            r0 = move-exception;
            r1 = 1;
            goto L_0x0012;
        L_0x0016:
            monitor-exit(r4);	 Catch:{ all -> 0x0021 }
            if (r1 == 0) goto L_0x0034;
        L_0x0019:
            r0 = java.lang.Thread.currentThread();
            r0.interrupt();
            goto L_0x0034;
        L_0x0021:
            r0 = move-exception;
            monitor-exit(r4);	 Catch:{ all -> 0x0021 }
            throw r0;	 Catch:{ all -> 0x0024 }
        L_0x0024:
            r0 = move-exception;
            goto L_0x002a;
        L_0x0026:
            r1 = move-exception;
            r3 = r1;
            r1 = r0;
            r0 = r3;
        L_0x002a:
            if (r1 == 0) goto L_0x0033;
        L_0x002c:
            r2 = java.lang.Thread.currentThread();
            r2.interrupt();
        L_0x0033:
            throw r0;
        L_0x0034:
            r0 = r4.computedReference;
            r0 = r0.waitForValue();
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.collect.ComputingConcurrentHashMap$ComputingValueReference.waitForValue():java.lang.Object");
        }

        public void clear(ValueReference<K, V> newValue) {
            setValueReference(newValue);
        }

        /* Access modifiers changed, original: 0000 */
        public V compute(K key, int hash) throws ExecutionException {
            try {
                V value = this.computingFunction.apply(key);
                setValueReference(new ComputedReference(value));
                return value;
            } catch (Throwable t) {
                setValueReference(new ComputationExceptionReference(t));
                ExecutionException executionException = new ExecutionException(t);
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void setValueReference(ValueReference<K, V> valueReference) {
            synchronized (this) {
                if (this.computedReference == MapMakerInternalMap.UNSET) {
                    this.computedReference = valueReference;
                    notifyAll();
                }
            }
        }
    }

    static final class ComputingSerializationProxy<K, V> extends AbstractSerializationProxy<K, V> {
        private static final long serialVersionUID = 4;
        final Function<? super K, ? extends V> computingFunction;

        ComputingSerializationProxy(Strength keyStrength, Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize, int concurrencyLevel, RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> delegate, Function<? super K, ? extends V> computingFunction) {
            super(keyStrength, valueStrength, keyEquivalence, valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize, concurrencyLevel, removalListener, delegate);
            this.computingFunction = computingFunction;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            writeMapTo(out);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.delegate = readMapMaker(in).makeComputingMap(this.computingFunction);
            readEntries(in);
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return this.delegate;
        }
    }

    ComputingConcurrentHashMap(MapMaker builder, Function<? super K, ? extends V> computingFunction) {
        super(builder);
        this.computingFunction = (Function) Preconditions.checkNotNull(computingFunction);
    }

    /* Access modifiers changed, original: 0000 */
    public Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
        return new ComputingSegment(this, initialCapacity, maxSegmentSize);
    }

    /* Access modifiers changed, original: 0000 */
    public ComputingSegment<K, V> segmentFor(int hash) {
        return (ComputingSegment) super.segmentFor(hash);
    }

    /* Access modifiers changed, original: 0000 */
    public V getOrCompute(K key) throws ExecutionException {
        int hash = hash(Preconditions.checkNotNull(key));
        return segmentFor(hash).getOrCompute(key, hash, this.computingFunction);
    }

    /* Access modifiers changed, original: 0000 */
    public Object writeReplace() {
        return new ComputingSerializationProxy(this.keyStrength, this.valueStrength, this.keyEquivalence, this.valueEquivalence, this.expireAfterWriteNanos, this.expireAfterAccessNanos, this.maximumSize, this.concurrencyLevel, this.removalListener, this, this.computingFunction);
    }
}
