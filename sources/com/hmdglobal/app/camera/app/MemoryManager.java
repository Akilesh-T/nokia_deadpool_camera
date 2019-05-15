package com.hmdglobal.app.camera.app;

import java.util.HashMap;

public interface MemoryManager {
    public static final int STATE_LOW_MEMORY = 1;
    public static final int STATE_OK = 0;

    public interface MemoryListener {
        void onLowMemory();

        void onMemoryStateChanged(int i);
    }

    void addListener(MemoryListener memoryListener);

    int getMaxAllowedNativeMemoryAllocation();

    HashMap queryMemory();

    void removeListener(MemoryListener memoryListener);
}
