package com.hmdglobal.app.camera.beauty.util;

public class NoDoubleClickUtil {
    private static final int SPACE_TIME = 800;
    private static long lastChangedTime;
    private static long lastClickTime;
    private static long lastClickTime2;
    private static long lastClickTime3;
    private static long lastFastClickTime;

    public static void initLastClickTime() {
        lastClickTime = 0;
    }

    public static synchronized boolean isDoubleClick() {
        boolean isClick2;
        synchronized (NoDoubleClickUtil.class) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime > 800) {
                isClick2 = false;
            } else {
                isClick2 = true;
            }
            lastClickTime = currentTime;
        }
        return isClick2;
    }

    public static synchronized boolean isDoubleChanged() {
        boolean changed;
        synchronized (NoDoubleClickUtil.class) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastChangedTime > 500) {
                changed = false;
            } else {
                changed = true;
            }
            lastChangedTime = currentTime;
        }
        return changed;
    }

    public static boolean isFastTochClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastFastClickTime;
        if (0 < timeD && timeD < 200) {
            return true;
        }
        lastFastClickTime = time;
        return false;
    }

    public static boolean isFastDoubleClick() {
        long time = System.currentTimeMillis();
        long timeD = time - lastClickTime2;
        if (0 < timeD && timeD < 800) {
            return true;
        }
        lastClickTime2 = time;
        return false;
    }
}
