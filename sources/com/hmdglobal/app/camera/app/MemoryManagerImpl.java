package com.hmdglobal.app.camera.app;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import com.hmdglobal.app.camera.app.MediaSaver.QueueListener;
import com.hmdglobal.app.camera.app.MemoryManager.MemoryListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.GservicesHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class MemoryManagerImpl implements MemoryManager, QueueListener, ComponentCallbacks2 {
    private static final float MAX_MEM_ALLOWED = 0.7f;
    private static final Tag TAG = new Tag("MemoryManagerImpl");
    private static final int[] sCriticalStates = new int[]{80, 15};
    private final LinkedList<MemoryListener> mListeners = new LinkedList();
    private final int mMaxAllowedNativeMemory;
    private final MemoryQuery mMemoryQuery;

    public static MemoryManagerImpl create(Context context, MediaSaver mediaSaver) {
        MemoryManagerImpl memoryManager = new MemoryManagerImpl(getMaxAllowedNativeMemory(context), new MemoryQuery((ActivityManager) context.getSystemService("activity")));
        context.registerComponentCallbacks(memoryManager);
        mediaSaver.setQueueListener(memoryManager);
        return memoryManager;
    }

    private MemoryManagerImpl(int maxAllowedNativeMemory, MemoryQuery memoryQuery) {
        this.mMaxAllowedNativeMemory = maxAllowedNativeMemory;
        this.mMemoryQuery = memoryQuery;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Max native memory: ");
        stringBuilder.append(this.mMaxAllowedNativeMemory);
        stringBuilder.append(" MB");
        Log.d(tag, stringBuilder.toString());
    }

    public void addListener(MemoryListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                Log.w(TAG, "Listener already added.");
            } else {
                this.mListeners.add(listener);
            }
        }
    }

    public void removeListener(MemoryListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                this.mListeners.remove(listener);
            } else {
                Log.w(TAG, "Cannot remove listener that was never added.");
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void onLowMemory() {
        notifyLowMemory();
    }

    public void onTrimMemory(int level) {
        for (int i : sCriticalStates) {
            if (level == i) {
                notifyLowMemory();
                return;
            }
        }
    }

    public void onQueueStatus(boolean full) {
        notifyCaptureStateUpdate(full);
    }

    public int getMaxAllowedNativeMemoryAllocation() {
        return this.mMaxAllowedNativeMemory;
    }

    public HashMap queryMemory() {
        return this.mMemoryQuery.queryMemory();
    }

    private static int getMaxAllowedNativeMemory(Context context) {
        int maxAllowedOverrideMb = GservicesHelper.getMaxAllowedNativeMemoryMb(context);
        if (maxAllowedOverrideMb > 0) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Max native memory overridden: ");
            stringBuilder.append(maxAllowedOverrideMb);
            Log.d(tag, stringBuilder.toString());
            return maxAllowedOverrideMb;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        return (int) (((float) Math.max(activityManager.getMemoryClass(), activityManager.getLargeMemoryClass())) * MAX_MEM_ALLOWED);
    }

    private void notifyLowMemory() {
        synchronized (this.mListeners) {
            Iterator it = this.mListeners.iterator();
            while (it.hasNext()) {
                ((MemoryListener) it.next()).onLowMemory();
            }
        }
    }

    private void notifyCaptureStateUpdate(int captureState) {
        synchronized (this.mListeners) {
            Iterator it = this.mListeners.iterator();
            while (it.hasNext()) {
                ((MemoryListener) it.next()).onMemoryStateChanged(captureState);
            }
        }
    }
}
