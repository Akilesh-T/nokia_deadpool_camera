package com.hmdglobal.app.camera.util;

import android.graphics.Matrix;
import android.view.MotionEvent;

public final class MotionEventHelper {
    private MotionEventHelper() {
    }

    public static MotionEvent transformEvent(MotionEvent e, Matrix m) {
        return transformEventNew(e, m);
    }

    private static MotionEvent transformEventNew(MotionEvent e, Matrix m) {
        MotionEvent newEvent = MotionEvent.obtain(e);
        newEvent.transform(m);
        return newEvent;
    }
}
