package com.hmdglobal.app.camera.app;

import android.graphics.Rect;
import com.android.ex.camera2.portability.Size;

public class Camera2FaceConverter {
    public static Rect getRightSize(Rect param, Size capture, float zoombar, float ratio) {
        zoombar = zoombar == 0.0f ? 1.0f : zoombar;
        int sizeH = (int) ((((float) capture.height()) * ratio) * zoombar);
        int sizeW = (int) ((((float) capture.width()) * ratio) * zoombar);
        param.left = (param.left - sizeW) / 2;
        param.right = (param.right - sizeW) / 2;
        param.top = (param.top - sizeH) / 3;
        param.bottom = (param.bottom - sizeH) / 3;
        return param;
    }
}
