package com.android.ex.camera2.portability;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class DispatchThread extends Thread {
    private static final long MAX_MESSAGE_QUEUE_LENGTH = 1024;
    private static final Tag TAG = new Tag("DispatchThread");
    private Handler mCameraHandler;
    private HandlerThread mCameraHandlerThread;
    private Boolean mIsEnded = new Boolean(false);
    private final Map<Integer, DispatchRunnable> mJobInstanceHash = new HashMap();
    private final Queue<DispatchRunnable> mJobQueue = new LinkedList();

    public DispatchThread(Handler cameraHandler, HandlerThread cameraHandlerThread) {
        super("Camera Job Dispatch Thread");
        this.mCameraHandler = cameraHandler;
        this.mCameraHandlerThread = cameraHandlerThread;
    }

    public void runJob(final Runnable job) {
        if (isEnded()) {
            throw new IllegalStateException("Trying to run job on interrupted dispatcher thread");
        }
        synchronized (this.mJobQueue) {
            if (((long) this.mJobQueue.size()) != 1024) {
                Log.w(TAG, " add to job queue");
                this.mJobQueue.add(new DispatchRunnable(null) {
                    public void run() {
                        job.run();
                    }
                });
                this.mJobQueue.notifyAll();
            } else {
                throw new RuntimeException("Camera master thread job queue full");
            }
        }
    }

    public void runJobInstance(final Runnable job, int instanceAction) {
        if (isEnded()) {
            throw new IllegalStateException("Trying to run job on interrupted dispatcher thread");
        }
        synchronized (this.mJobQueue) {
            if (((long) this.mJobQueue.size()) != 1024) {
                Log.w(TAG, " add to job queue");
                DispatchRunnable runnable = new DispatchRunnable(Integer.valueOf(instanceAction)) {
                    public void run() {
                        job.run();
                    }
                };
                synchronized (this.mJobInstanceHash) {
                    DispatchRunnable instanceRunnable = (DispatchRunnable) this.mJobInstanceHash.get(Integer.valueOf(instanceAction));
                    if (instanceRunnable != null) {
                        this.mJobQueue.remove(instanceRunnable);
                    }
                    this.mJobInstanceHash.put(Integer.valueOf(instanceAction), runnable);
                }
                this.mJobQueue.add(runnable);
                this.mJobQueue.notifyAll();
            } else {
                throw new RuntimeException("Camera master thread job queue full");
            }
        }
    }

    public void runJobSync(Runnable job, Object waitLock, long timeoutMs, String jobMsg) {
        String timeoutMsg = new StringBuilder();
        timeoutMsg.append("Timeout waiting ");
        timeoutMsg.append(timeoutMs);
        timeoutMsg.append("ms for ");
        timeoutMsg.append(jobMsg);
        timeoutMsg = timeoutMsg.toString();
        synchronized (waitLock) {
            long timeoutBound = SystemClock.uptimeMillis() + timeoutMs;
            try {
                runJob(job);
                waitLock.wait(timeoutMs);
                if (SystemClock.uptimeMillis() <= timeoutBound) {
                } else {
                    throw new IllegalStateException(timeoutMsg);
                }
            } catch (InterruptedException e) {
                if (SystemClock.uptimeMillis() > timeoutBound) {
                    throw new IllegalStateException(timeoutMsg);
                }
            }
        }
    }

    public void end() {
        synchronized (this.mIsEnded) {
            this.mIsEnded = Boolean.valueOf(true);
        }
        synchronized (this.mJobQueue) {
            this.mJobQueue.notifyAll();
        }
    }

    private boolean isEnded() {
        boolean booleanValue;
        synchronized (this.mIsEnded) {
            booleanValue = this.mIsEnded.booleanValue();
        }
        return booleanValue;
    }

    public void run() {
        Log.v(TAG, "thread start running");
        while (true) {
            DispatchRunnable job;
            synchronized (this.mJobQueue) {
                while (this.mJobQueue.size() == 0 && !isEnded()) {
                    try {
                        Log.w(TAG, " wait for next job");
                        this.mJobQueue.wait();
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Dispatcher thread wait() interrupted, exiting");
                    }
                }
                Log.w(TAG, " poll job");
                job = (DispatchRunnable) this.mJobQueue.poll();
            }
            if (job == null) {
                Log.w(TAG, " null job polled");
                if (isEnded()) {
                    this.mCameraHandlerThread.quitSafely();
                    return;
                }
            } else {
                Integer jobAction = job.getIntanceAction();
                if (jobAction != null) {
                    synchronized (this.mJobInstanceHash) {
                        this.mJobInstanceHash.remove(jobAction);
                    }
                }
                job.run();
                Log.w(TAG, " job run");
                synchronized (this) {
                    this.mCameraHandler.post(new Runnable() {
                        public void run() {
                            synchronized (DispatchThread.this) {
                                DispatchThread.this.notifyAll();
                                Log.w(DispatchThread.TAG, " camera handle notifyAll");
                            }
                        }
                    });
                    try {
                        Log.w(TAG, " camera handle waitDone");
                        wait();
                    } catch (InterruptedException e2) {
                    }
                }
            }
        }
        while (true) {
        }
    }
}
