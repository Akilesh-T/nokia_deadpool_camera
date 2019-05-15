package com.hmdglobal.app.camera.util;

import android.os.Handler;
import android.util.Pair;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.security.InvalidParameterException;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

public class ConcurrentSharedRingBuffer<E> {
    private static final Tag TAG = new Tag("CncrrntShrdRingBuf");
    private final Semaphore mCapacitySemaphore;
    private boolean mClosed = false;
    private TreeMap<Long, Pinnable<E>> mElements;
    private final Object mLock = new Object();
    private final Semaphore mPinSemaphore;
    private Handler mPinStateHandler = null;
    private PinStateListener mPinStateListener = null;
    private final Object mSwapLock = new Object();
    private TreeMap<Long, Pinnable<E>> mUnpinnedElements;

    public interface PinStateListener {
        void onPinStateChange(boolean z);
    }

    private static class Pinnable<E> {
        private E mElement;
        private int mPins = 0;

        public Pinnable(E element) {
            this.mElement = element;
        }

        public E getElement() {
            return this.mElement;
        }

        private boolean isPinned() {
            return this.mPins > 0;
        }
    }

    public interface Selector<E> {
        boolean select(E e);
    }

    public interface SwapTask<E> {
        E create();

        E swap(E e);

        void update(E e);
    }

    public ConcurrentSharedRingBuffer(int capacity) {
        if (capacity > 0) {
            this.mElements = new TreeMap();
            this.mUnpinnedElements = new TreeMap();
            this.mCapacitySemaphore = new Semaphore(capacity);
            this.mPinSemaphore = new Semaphore(-1);
            return;
        }
        throw new IllegalArgumentException("Capacity must be positive.");
    }

    public void setListener(Handler handler, PinStateListener listener) {
        synchronized (this.mLock) {
            this.mPinStateHandler = handler;
            this.mPinStateListener = listener;
        }
    }

    /* JADX WARNING: Missing block: B:17:0x001e, code skipped:
            if (r1 == null) goto L_0x0029;
     */
    /* JADX WARNING: Missing block: B:19:?, code skipped:
            r12.update(r1.getElement());
     */
    /* JADX WARNING: Missing block: B:21:0x0028, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:23:0x002f, code skipped:
            if (r9.mCapacitySemaphore.tryAcquire() == false) goto L_0x006c;
     */
    /* JADX WARNING: Missing block: B:24:0x0031, code skipped:
            r3 = new com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Pinnable(r12.create());
            r5 = r9.mLock;
     */
    /* JADX WARNING: Missing block: B:25:0x003c, code skipped:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:28:0x003f, code skipped:
            if (r9.mClosed == false) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:29:0x0041, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:32:0x0043, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:34:?, code skipped:
            r9.mElements.put(java.lang.Long.valueOf(r10), r3);
            r9.mUnpinnedElements.put(java.lang.Long.valueOf(r10), r3);
            r9.mPinSemaphore.release();
     */
    /* JADX WARNING: Missing block: B:35:0x0061, code skipped:
            if (r9.mPinSemaphore.availablePermits() != 1) goto L_0x0066;
     */
    /* JADX WARNING: Missing block: B:36:0x0063, code skipped:
            notifyPinStateChange(true);
     */
    /* JADX WARNING: Missing block: B:37:0x0066, code skipped:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:40:0x0068, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:46:0x006c, code skipped:
            r3 = r9.mLock;
     */
    /* JADX WARNING: Missing block: B:47:0x006e, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:50:0x0071, code skipped:
            if (r9.mClosed == false) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:51:0x0073, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:54:0x0075, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:56:?, code skipped:
            r5 = r9.mUnpinnedElements.pollFirstEntry();
     */
    /* JADX WARNING: Missing block: B:57:0x007c, code skipped:
            if (r5 == null) goto L_0x00de;
     */
    /* JADX WARNING: Missing block: B:58:0x007e, code skipped:
            r6 = (com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Pinnable) r5.getValue();
            r9.mElements.remove(r5.getKey());
     */
    /* JADX WARNING: Missing block: B:59:0x008d, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:61:?, code skipped:
            com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Pinnable.access$002(r6, r12.swap(com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Pinnable.access$000(r6)));
     */
    /* JADX WARNING: Missing block: B:63:?, code skipped:
            r3 = r9.mLock;
     */
    /* JADX WARNING: Missing block: B:64:0x009b, code skipped:
            monitor-enter(r3);
     */
    /* JADX WARNING: Missing block: B:67:0x009e, code skipped:
            if (r9.mClosed == false) goto L_0x00a3;
     */
    /* JADX WARNING: Missing block: B:68:0x00a0, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:71:0x00a2, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:73:?, code skipped:
            r9.mElements.put(java.lang.Long.valueOf(r10), r6);
            r9.mUnpinnedElements.put(java.lang.Long.valueOf(r10), r6);
     */
    /* JADX WARNING: Missing block: B:74:0x00b5, code skipped:
            monitor-exit(r3);
     */
    /* JADX WARNING: Missing block: B:77:0x00b8, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:78:0x00b9, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:83:0x00bc, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:85:0x00bf, code skipped:
            monitor-enter(r9.mLock);
     */
    /* JADX WARNING: Missing block: B:88:0x00c2, code skipped:
            if (r9.mClosed != false) goto L_0x00c4;
     */
    /* JADX WARNING: Missing block: B:92:0x00c6, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:94:?, code skipped:
            r9.mElements.put(java.lang.Long.valueOf(r10), r6);
            r9.mUnpinnedElements.put(java.lang.Long.valueOf(r10), r6);
     */
    /* JADX WARNING: Missing block: B:97:0x00db, code skipped:
            r2 = th;
     */
    /* JADX WARNING: Missing block: B:101:0x00e5, code skipped:
            throw new java.lang.RuntimeException("No unpinned element available.");
     */
    public boolean swapLeast(long r10, com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.SwapTask<E> r12) {
        /*
        r9 = this;
        r0 = r9.mSwapLock;
        monitor-enter(r0);
        r1 = 0;
        r2 = r9.mLock;	 Catch:{ all -> 0x00ec }
        monitor-enter(r2);	 Catch:{ all -> 0x00ec }
        r3 = r9.mClosed;	 Catch:{ all -> 0x00e9 }
        r4 = 0;
        if (r3 == 0) goto L_0x000f;
    L_0x000c:
        monitor-exit(r2);	 Catch:{ all -> 0x00e9 }
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        return r4;
    L_0x000f:
        r3 = r9.mElements;	 Catch:{ all -> 0x00e9 }
        r5 = java.lang.Long.valueOf(r10);	 Catch:{ all -> 0x00e9 }
        r3 = r3.get(r5);	 Catch:{ all -> 0x00e9 }
        r3 = (com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Pinnable) r3;	 Catch:{ all -> 0x00e9 }
        r1 = r3;
        monitor-exit(r2);	 Catch:{ all -> 0x00e9 }
        r2 = 1;
        if (r1 == 0) goto L_0x0029;
    L_0x0020:
        r3 = r1.getElement();	 Catch:{ all -> 0x00ec }
        r12.update(r3);	 Catch:{ all -> 0x00ec }
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        return r2;
    L_0x0029:
        r3 = r9.mCapacitySemaphore;	 Catch:{ all -> 0x00ec }
        r3 = r3.tryAcquire();	 Catch:{ all -> 0x00ec }
        if (r3 == 0) goto L_0x006c;
    L_0x0031:
        r3 = new com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer$Pinnable;	 Catch:{ all -> 0x00ec }
        r5 = r12.create();	 Catch:{ all -> 0x00ec }
        r3.<init>(r5);	 Catch:{ all -> 0x00ec }
        r5 = r9.mLock;	 Catch:{ all -> 0x00ec }
        monitor-enter(r5);	 Catch:{ all -> 0x00ec }
        r6 = r9.mClosed;	 Catch:{ all -> 0x0069 }
        if (r6 == 0) goto L_0x0044;
    L_0x0041:
        monitor-exit(r5);	 Catch:{ all -> 0x0069 }
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        return r4;
    L_0x0044:
        r4 = r9.mElements;	 Catch:{ all -> 0x0069 }
        r6 = java.lang.Long.valueOf(r10);	 Catch:{ all -> 0x0069 }
        r4.put(r6, r3);	 Catch:{ all -> 0x0069 }
        r4 = r9.mUnpinnedElements;	 Catch:{ all -> 0x0069 }
        r6 = java.lang.Long.valueOf(r10);	 Catch:{ all -> 0x0069 }
        r4.put(r6, r3);	 Catch:{ all -> 0x0069 }
        r4 = r9.mPinSemaphore;	 Catch:{ all -> 0x0069 }
        r4.release();	 Catch:{ all -> 0x0069 }
        r4 = r9.mPinSemaphore;	 Catch:{ all -> 0x0069 }
        r4 = r4.availablePermits();	 Catch:{ all -> 0x0069 }
        if (r4 != r2) goto L_0x0066;
    L_0x0063:
        r9.notifyPinStateChange(r2);	 Catch:{ all -> 0x0069 }
    L_0x0066:
        monitor-exit(r5);	 Catch:{ all -> 0x0069 }
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        return r2;
    L_0x0069:
        r2 = move-exception;
        monitor-exit(r5);	 Catch:{ all -> 0x0069 }
        throw r2;	 Catch:{ all -> 0x00ec }
    L_0x006c:
        r3 = r9.mLock;	 Catch:{ all -> 0x00ec }
        monitor-enter(r3);	 Catch:{ all -> 0x00ec }
        r5 = r9.mClosed;	 Catch:{ all -> 0x00e6 }
        if (r5 == 0) goto L_0x0076;
    L_0x0073:
        monitor-exit(r3);	 Catch:{ all -> 0x00e6 }
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        return r4;
    L_0x0076:
        r5 = r9.mUnpinnedElements;	 Catch:{ all -> 0x00e6 }
        r5 = r5.pollFirstEntry();	 Catch:{ all -> 0x00e6 }
        if (r5 == 0) goto L_0x00de;
    L_0x007e:
        r6 = r5.getValue();	 Catch:{ all -> 0x00e6 }
        r6 = (com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Pinnable) r6;	 Catch:{ all -> 0x00e6 }
        r7 = r9.mElements;	 Catch:{ all -> 0x00e6 }
        r8 = r5.getKey();	 Catch:{ all -> 0x00e6 }
        r7.remove(r8);	 Catch:{ all -> 0x00e6 }
        monitor-exit(r3);	 Catch:{ all -> 0x00e6 }
        r3 = r6.mElement;	 Catch:{ all -> 0x00bc }
        r3 = r12.swap(r3);	 Catch:{ all -> 0x00bc }
        r6.mElement = r3;	 Catch:{ all -> 0x00bc }
        r3 = r9.mLock;	 Catch:{ all -> 0x00ec }
        monitor-enter(r3);	 Catch:{ all -> 0x00ec }
        r5 = r9.mClosed;	 Catch:{ all -> 0x00b9 }
        if (r5 == 0) goto L_0x00a3;
    L_0x00a0:
        monitor-exit(r3);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        return r4;
    L_0x00a3:
        r4 = r9.mElements;	 Catch:{ all -> 0x00b9 }
        r5 = java.lang.Long.valueOf(r10);	 Catch:{ all -> 0x00b9 }
        r4.put(r5, r6);	 Catch:{ all -> 0x00b9 }
        r4 = r9.mUnpinnedElements;	 Catch:{ all -> 0x00b9 }
        r5 = java.lang.Long.valueOf(r10);	 Catch:{ all -> 0x00b9 }
        r4.put(r5, r6);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r3);	 Catch:{ all -> 0x00b9 }
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        return r2;
    L_0x00b9:
        r2 = move-exception;
        monitor-exit(r3);	 Catch:{ all -> 0x00b9 }
    L_0x00bb:
        throw r2;	 Catch:{ all -> 0x00ec }
    L_0x00bc:
        r2 = move-exception;
        r3 = r9.mLock;	 Catch:{ all -> 0x00ec }
        monitor-enter(r3);	 Catch:{ all -> 0x00ec }
        r5 = r9.mClosed;	 Catch:{ all -> 0x00db }
        if (r5 == 0) goto L_0x00c7;
    L_0x00c4:
        monitor-exit(r3);	 Catch:{ all -> 0x00db }
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        return r4;
    L_0x00c7:
        r4 = r9.mElements;	 Catch:{ all -> 0x00db }
        r5 = java.lang.Long.valueOf(r10);	 Catch:{ all -> 0x00db }
        r4.put(r5, r6);	 Catch:{ all -> 0x00db }
        r4 = r9.mUnpinnedElements;	 Catch:{ all -> 0x00db }
        r5 = java.lang.Long.valueOf(r10);	 Catch:{ all -> 0x00db }
        r4.put(r5, r6);	 Catch:{ all -> 0x00db }
        monitor-exit(r3);	 Catch:{ all -> 0x00db }
        goto L_0x00bb;
    L_0x00db:
        r2 = move-exception;
        monitor-exit(r3);	 Catch:{ all -> 0x00db }
        goto L_0x00bb;
    L_0x00de:
        r2 = new java.lang.RuntimeException;	 Catch:{ all -> 0x00e6 }
        r4 = "No unpinned element available.";
        r2.<init>(r4);	 Catch:{ all -> 0x00e6 }
        throw r2;	 Catch:{ all -> 0x00e6 }
    L_0x00e6:
        r2 = move-exception;
        monitor-exit(r3);	 Catch:{ all -> 0x00e6 }
        throw r2;	 Catch:{ all -> 0x00ec }
    L_0x00e9:
        r3 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x00e9 }
        throw r3;	 Catch:{ all -> 0x00ec }
    L_0x00ec:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x00ec }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.swapLeast(long, com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer$SwapTask):boolean");
    }

    /* JADX WARNING: Missing block: B:26:0x0053, code skipped:
            if (r0 == false) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:27:0x0055, code skipped:
            notifyPinStateChange(false);
     */
    /* JADX WARNING: Missing block: B:29:0x0064, code skipped:
            return android.util.Pair.create(java.lang.Long.valueOf(r7), r1.getElement());
     */
    public android.util.Pair<java.lang.Long, E> tryPin(long r7) {
        /*
        r6 = this;
        r0 = 0;
        r1 = 0;
        r2 = r6.mLock;
        monitor-enter(r2);
        r3 = r6.mClosed;	 Catch:{ all -> 0x0067 }
        r4 = 0;
        if (r3 == 0) goto L_0x000c;
    L_0x000a:
        monitor-exit(r2);	 Catch:{ all -> 0x0067 }
        return r4;
    L_0x000c:
        r3 = r6.mElements;	 Catch:{ all -> 0x0067 }
        r3 = r3.isEmpty();	 Catch:{ all -> 0x0067 }
        if (r3 == 0) goto L_0x0016;
    L_0x0014:
        monitor-exit(r2);	 Catch:{ all -> 0x0067 }
        return r4;
    L_0x0016:
        r3 = r6.mElements;	 Catch:{ all -> 0x0067 }
        r5 = java.lang.Long.valueOf(r7);	 Catch:{ all -> 0x0067 }
        r3 = r3.get(r5);	 Catch:{ all -> 0x0067 }
        r3 = (com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Pinnable) r3;	 Catch:{ all -> 0x0067 }
        r1 = r3;
        if (r1 != 0) goto L_0x0027;
    L_0x0025:
        monitor-exit(r2);	 Catch:{ all -> 0x0067 }
        return r4;
    L_0x0027:
        r3 = r1.isPinned();	 Catch:{ all -> 0x0067 }
        r5 = 0;
        if (r3 == 0) goto L_0x0032;
    L_0x002e:
        r1.mPins = r1.mPins + 1;	 Catch:{ all -> 0x0067 }
        goto L_0x0052;
    L_0x0032:
        r3 = r6.mPinSemaphore;	 Catch:{ all -> 0x0067 }
        r3 = r3.tryAcquire();	 Catch:{ all -> 0x0067 }
        if (r3 == 0) goto L_0x0065;
    L_0x003a:
        r3 = r6.mUnpinnedElements;	 Catch:{ all -> 0x0067 }
        r4 = java.lang.Long.valueOf(r7);	 Catch:{ all -> 0x0067 }
        r3.remove(r4);	 Catch:{ all -> 0x0067 }
        r1.mPins = r1.mPins + 1;	 Catch:{ all -> 0x0067 }
        r3 = r6.mPinSemaphore;	 Catch:{ all -> 0x0067 }
        r3 = r3.availablePermits();	 Catch:{ all -> 0x0067 }
        if (r3 > 0) goto L_0x0050;
    L_0x004e:
        r3 = 1;
        goto L_0x0051;
    L_0x0050:
        r3 = r5;
    L_0x0051:
        r0 = r3;
    L_0x0052:
        monitor-exit(r2);	 Catch:{ all -> 0x0067 }
        if (r0 == 0) goto L_0x0058;
    L_0x0055:
        r6.notifyPinStateChange(r5);
    L_0x0058:
        r2 = java.lang.Long.valueOf(r7);
        r3 = r1.getElement();
        r2 = android.util.Pair.create(r2, r3);
        return r2;
    L_0x0065:
        monitor-exit(r2);	 Catch:{ all -> 0x0067 }
        return r4;
    L_0x0067:
        r3 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x0067 }
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.tryPin(long):android.util.Pair");
    }

    public void release(long key) {
        synchronized (this.mLock) {
            Pinnable<E> element = (Pinnable) this.mElements.get(Long.valueOf(key));
            if (element == null) {
                throw new InvalidParameterException("No entry found for the given key.");
            } else if (element.isPinned()) {
                element.mPins = element.mPins - 1;
                if (!element.isPinned()) {
                    this.mUnpinnedElements.put(Long.valueOf(key), element);
                    this.mPinSemaphore.release();
                    if (this.mPinSemaphore.availablePermits() == 1) {
                        notifyPinStateChange(true);
                    }
                }
            } else {
                throw new IllegalArgumentException("Calling release() with unpinned element.");
            }
        }
    }

    public Pair<Long, E> tryPinGreatest() {
        synchronized (this.mLock) {
            if (this.mClosed) {
                return null;
            } else if (this.mElements.isEmpty()) {
                return null;
            } else {
                Pair tryPin = tryPin(((Long) this.mElements.lastKey()).longValue());
                return tryPin;
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0023, code skipped:
            java.util.Collections.sort(r0);
            r1 = r0.size() - 1;
     */
    /* JADX WARNING: Missing block: B:14:0x002c, code skipped:
            if (r1 < 0) goto L_0x0068;
     */
    /* JADX WARNING: Missing block: B:15:0x002e, code skipped:
            r2 = tryPin(((java.lang.Long) r0.get(r1)).longValue());
     */
    /* JADX WARNING: Missing block: B:16:0x003c, code skipped:
            if (r2 == null) goto L_0x0065;
     */
    /* JADX WARNING: Missing block: B:21:0x0046, code skipped:
            if (r8.select(r2.second) == false) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:22:0x0048, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:23:0x0049, code skipped:
            release(((java.lang.Long) r2.first).longValue());
     */
    /* JADX WARNING: Missing block: B:25:0x0056, code skipped:
            if (false != false) goto L_0x0058;
     */
    /* JADX WARNING: Missing block: B:26:0x0058, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:27:0x0059, code skipped:
            release(((java.lang.Long) r2.first).longValue());
     */
    /* JADX WARNING: Missing block: B:29:0x0065, code skipped:
            r1 = r1 - 1;
     */
    /* JADX WARNING: Missing block: B:30:0x0068, code skipped:
            return null;
     */
    public android.util.Pair<java.lang.Long, E> tryPinGreatestSelected(com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.Selector<E> r8) {
        /*
        r7 = this;
        r0 = new java.util.ArrayList;
        r0.<init>();
        r1 = r7.mLock;
        monitor-enter(r1);
        r2 = r7.mClosed;	 Catch:{ all -> 0x0069 }
        r3 = 0;
        if (r2 == 0) goto L_0x000f;
    L_0x000d:
        monitor-exit(r1);	 Catch:{ all -> 0x0069 }
        return r3;
    L_0x000f:
        r2 = r7.mElements;	 Catch:{ all -> 0x0069 }
        r2 = r2.isEmpty();	 Catch:{ all -> 0x0069 }
        if (r2 == 0) goto L_0x0019;
    L_0x0017:
        monitor-exit(r1);	 Catch:{ all -> 0x0069 }
        return r3;
    L_0x0019:
        r2 = r7.mElements;	 Catch:{ all -> 0x0069 }
        r2 = r2.keySet();	 Catch:{ all -> 0x0069 }
        r0.addAll(r2);	 Catch:{ all -> 0x0069 }
        monitor-exit(r1);	 Catch:{ all -> 0x0069 }
        java.util.Collections.sort(r0);
        r1 = r0.size();
        r1 = r1 + -1;
    L_0x002c:
        if (r1 < 0) goto L_0x0068;
    L_0x002e:
        r2 = r0.get(r1);
        r2 = (java.lang.Long) r2;
        r4 = r2.longValue();
        r2 = r7.tryPin(r4);
        if (r2 == 0) goto L_0x0065;
    L_0x003e:
        r4 = 0;
        r5 = r2.second;	 Catch:{ all -> 0x0055 }
        r5 = r8.select(r5);	 Catch:{ all -> 0x0055 }
        r4 = r5;
        if (r4 == 0) goto L_0x0049;
    L_0x0048:
        return r2;
    L_0x0049:
        r5 = r2.first;
        r5 = (java.lang.Long) r5;
        r5 = r5.longValue();
        r7.release(r5);
        goto L_0x0065;
    L_0x0055:
        r3 = move-exception;
        if (r4 == 0) goto L_0x0059;
    L_0x0058:
        return r2;
    L_0x0059:
        r5 = r2.first;
        r5 = (java.lang.Long) r5;
        r5 = r5.longValue();
        r7.release(r5);
        throw r3;
    L_0x0065:
        r1 = r1 + -1;
        goto L_0x002c;
    L_0x0068:
        return r3;
    L_0x0069:
        r2 = move-exception;
        monitor-exit(r1);	 Catch:{ all -> 0x0069 }
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer.tryPinGreatestSelected(com.hmdglobal.app.camera.util.ConcurrentSharedRingBuffer$Selector):android.util.Pair");
    }

    public void close(Task<E> task) throws InterruptedException {
        int numPinnedElements;
        synchronized (this.mSwapLock) {
            synchronized (this.mLock) {
                this.mClosed = true;
                numPinnedElements = this.mElements.size() - this.mUnpinnedElements.size();
            }
        }
        notifyPinStateChange(false);
        if (numPinnedElements > 0) {
            this.mPinSemaphore.acquire(numPinnedElements);
        }
        for (Pinnable<E> element : this.mElements.values()) {
            task.run(element.mElement);
        }
        this.mUnpinnedElements.clear();
        this.mElements.clear();
    }

    private void notifyPinStateChange(final boolean pinsAvailable) {
        synchronized (this.mLock) {
            if (this.mPinStateHandler != null) {
                final PinStateListener listener = this.mPinStateListener;
                this.mPinStateHandler.post(new Runnable() {
                    public void run() {
                        listener.onPinStateChange(pinsAvailable);
                    }
                });
            }
        }
    }
}
