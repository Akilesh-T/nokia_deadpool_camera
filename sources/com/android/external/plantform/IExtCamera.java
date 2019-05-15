package com.android.external.plantform;

import android.hardware.Camera;

public interface IExtCamera {
    void create(Camera camera);

    void destroy();

    void setGestureCallback(IExtGestureCallback iExtGestureCallback);

    void setRamaCallback(IExtPanoramaCallback iExtPanoramaCallback);

    void setRamaMoveCallback(IExtPanoramaMoveCallback iExtPanoramaMoveCallback);

    void startGestureDetection();

    void startRama(int i);

    void stopGestureDetection();

    void stopRama(int i);
}
