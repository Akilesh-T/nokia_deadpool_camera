package com.bumptech.glide.request;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.util.Util;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RequestFutureTarget<T, R> implements FutureTarget<R>, Runnable {
    private static final Waiter DEFAULT_WAITER = new Waiter();
    private final boolean assertBackgroundThread;
    private Exception exception;
    private boolean exceptionReceived;
    private final int height;
    private boolean isCancelled;
    private final Handler mainHandler;
    private Request request;
    private R resource;
    private boolean resultReceived;
    private final Waiter waiter;
    private final int width;

    static class Waiter {
        Waiter() {
        }

        public void waitForTimeout(Object toWaitOn, long timeoutMillis) throws InterruptedException {
            toWaitOn.wait(timeoutMillis);
        }

        public void notifyAll(Object toNotify) {
            toNotify.notifyAll();
        }
    }

    public RequestFutureTarget(Handler mainHandler, int width, int height) {
        this(mainHandler, width, height, true, DEFAULT_WAITER);
    }

    RequestFutureTarget(Handler mainHandler, int width, int height, boolean assertBackgroundThread, Waiter waiter) {
        this.mainHandler = mainHandler;
        this.width = width;
        this.height = height;
        this.assertBackgroundThread = assertBackgroundThread;
        this.waiter = waiter;
    }

    /* JADX WARNING: Missing block: B:12:0x001a, code skipped:
            return r0;
     */
    public synchronized boolean cancel(boolean r3) {
        /*
        r2 = this;
        monitor-enter(r2);
        r0 = r2.isCancelled;	 Catch:{ all -> 0x001b }
        r1 = 1;
        if (r0 == 0) goto L_0x0008;
    L_0x0006:
        monitor-exit(r2);
        return r1;
    L_0x0008:
        r0 = r2.isDone();	 Catch:{ all -> 0x001b }
        r0 = r0 ^ r1;
        if (r0 == 0) goto L_0x0019;
    L_0x000f:
        r2.isCancelled = r1;	 Catch:{ all -> 0x001b }
        r2.clear();	 Catch:{ all -> 0x001b }
        r1 = r2.waiter;	 Catch:{ all -> 0x001b }
        r1.notifyAll(r2);	 Catch:{ all -> 0x001b }
    L_0x0019:
        monitor-exit(r2);
        return r0;
    L_0x001b:
        r3 = move-exception;
        monitor-exit(r2);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.request.RequestFutureTarget.cancel(boolean):boolean");
    }

    public synchronized boolean isCancelled() {
        return this.isCancelled;
    }

    public synchronized boolean isDone() {
        boolean z;
        z = this.isCancelled || this.resultReceived;
        return z;
    }

    public R get() throws InterruptedException, ExecutionException {
        try {
            return doGet(null);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    public R get(long time, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return doGet(Long.valueOf(timeUnit.toMillis(time)));
    }

    public void getSize(SizeReadyCallback cb) {
        cb.onSizeReady(this.width, this.height);
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Request getRequest() {
        return this.request;
    }

    public void onLoadCleared(Drawable placeholder) {
    }

    public void onLoadStarted(Drawable placeholder) {
    }

    public synchronized void onLoadFailed(Exception e, Drawable errorDrawable) {
        this.exceptionReceived = true;
        this.exception = e;
        this.waiter.notifyAll(this);
    }

    public synchronized void onResourceReady(R resource, GlideAnimation<? super R> glideAnimation) {
        this.resultReceived = true;
        this.resource = resource;
        this.waiter.notifyAll(this);
    }

    private synchronized R doGet(Long timeoutMillis) throws ExecutionException, InterruptedException, TimeoutException {
        if (this.assertBackgroundThread) {
            Util.assertBackgroundThread();
        }
        if (this.isCancelled) {
            throw new CancellationException();
        } else if (this.exceptionReceived) {
            throw new ExecutionException(this.exception);
        } else if (this.resultReceived) {
            return this.resource;
        } else {
            if (timeoutMillis == null) {
                this.waiter.waitForTimeout(this, 0);
            } else if (timeoutMillis.longValue() > 0) {
                this.waiter.waitForTimeout(this, timeoutMillis.longValue());
            }
            if (Thread.interrupted()) {
                throw new InterruptedException();
            } else if (this.exceptionReceived) {
                throw new ExecutionException(this.exception);
            } else if (this.isCancelled) {
                throw new CancellationException();
            } else if (this.resultReceived) {
                return this.resource;
            } else {
                throw new TimeoutException();
            }
        }
    }

    public void run() {
        this.request.clear();
    }

    public void clear() {
        this.mainHandler.post(this);
    }

    public void onStart() {
    }

    public void onStop() {
    }

    public void onDestroy() {
    }
}
