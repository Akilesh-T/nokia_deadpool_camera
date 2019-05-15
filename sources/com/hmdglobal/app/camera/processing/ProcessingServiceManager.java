package com.hmdglobal.app.camera.processing;

import android.content.Context;
import android.content.Intent;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.util.LinkedList;

public class ProcessingServiceManager {
    private static final Tag TAG = new Tag("ProcessingSvcMgr");
    private static ProcessingServiceManager sInstance;
    private final Context mAppContext;
    private boolean mHoldProcessing = false;
    private final LinkedList<ProcessingTask> mQueue = new LinkedList();
    private volatile boolean mServiceRunning = false;

    public static void initSingleton(Context appContext) {
        sInstance = new ProcessingServiceManager(appContext);
    }

    public static ProcessingServiceManager getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        throw new IllegalStateException("initSingleton() not yet called.");
    }

    private ProcessingServiceManager(Context context) {
        this.mAppContext = context;
    }

    public synchronized void enqueueTask(ProcessingTask task) {
        this.mQueue.add(task);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Task added. Queue size now: ");
        stringBuilder.append(this.mQueue.size());
        Log.d(tag, stringBuilder.toString());
        if (!(this.mServiceRunning || this.mHoldProcessing)) {
            startService();
        }
    }

    public synchronized ProcessingTask popNextSession() {
        Tag tag;
        StringBuilder stringBuilder;
        if (this.mQueue.isEmpty() || this.mHoldProcessing) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Popping null. On hold? ");
            stringBuilder.append(this.mHoldProcessing);
            Log.d(tag, stringBuilder.toString());
            this.mServiceRunning = false;
            return null;
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Popping a session. Remaining: ");
        stringBuilder.append(this.mQueue.size() - 1);
        Log.d(tag, stringBuilder.toString());
        return (ProcessingTask) this.mQueue.remove();
    }

    public synchronized boolean isRunningOrHasItems() {
        boolean z;
        z = this.mServiceRunning || !this.mQueue.isEmpty();
        return z;
    }

    public synchronized boolean suspendProcessing() {
        if (isRunningOrHasItems()) {
            Log.d(TAG, "Not able to suspend processing.");
            return false;
        }
        Log.d(TAG, "Suspend processing");
        this.mHoldProcessing = true;
        return true;
    }

    public synchronized void resumeProcessing() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Resume processing. Queue size: ");
        stringBuilder.append(this.mQueue.size());
        Log.d(tag, stringBuilder.toString());
        if (this.mHoldProcessing) {
            this.mHoldProcessing = false;
            if (!this.mQueue.isEmpty()) {
                startService();
            }
        }
    }

    private void startService() {
        this.mAppContext.startService(new Intent(this.mAppContext, ProcessingService.class));
        this.mServiceRunning = true;
    }
}
