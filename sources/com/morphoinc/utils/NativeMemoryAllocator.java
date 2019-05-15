package com.morphoinc.utils;

import android.util.Log;
import java.nio.ByteBuffer;

public class NativeMemoryAllocator {
    private static final String TAG = "NativeMemoryAllocator";

    public static native ByteBuffer allocateBuffer(int i);

    public static native void freeBuffer(ByteBuffer byteBuffer);

    public static native long getAddress(ByteBuffer byteBuffer);

    static {
        try {
            System.loadLibrary("morpho_memory_allocator");
            Log.d(TAG, "load libmorpho_memory_allocator.so");
        } catch (UnsatisfiedLinkError e) {
            Log.d(TAG, e.getMessage());
            Log.d(TAG, "can't loadLibrary");
        }
    }
}
