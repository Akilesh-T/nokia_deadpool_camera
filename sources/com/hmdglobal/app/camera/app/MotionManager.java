package com.hmdglobal.app.camera.app;

import android.content.Context;
import java.util.LinkedList;

public class MotionManager {
    private final LinkedList<MotionListener> mListeners = new LinkedList();

    public interface MotionListener {
        void onMoving();
    }

    public MotionManager(Context context) {
    }

    public void addListener(MotionListener listener) {
    }

    public void removeListener(MotionListener listener) {
    }

    public void reset() {
    }

    public void start() {
    }

    public void stop() {
    }

    public boolean isEnabled() {
        return false;
    }

    public void onGyroUpdate(long t, float x, float y, float z) {
    }
}
