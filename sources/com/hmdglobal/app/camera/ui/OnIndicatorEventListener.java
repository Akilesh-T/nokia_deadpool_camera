package com.hmdglobal.app.camera.ui;

public interface OnIndicatorEventListener {
    public static final int EVENT_ENTER_SECOND_LEVEL_INDICATOR_BAR = 0;
    public static final int EVENT_ENTER_ZOOM_CONTROL = 2;
    public static final int EVENT_LEAVE_SECOND_LEVEL_INDICATOR_BAR = 1;
    public static final int EVENT_LEAVE_ZOOM_CONTROL = 3;

    void onIndicatorEvent(int i);
}
