package com.hmdglobal.app.camera.app;

import android.location.Location;

public interface LocationProvider {

    public interface OnConnectionFailedListener {
        void onConnectionFailed();
    }

    void disconnect();

    Location getCurrentLocation();

    void recordLocation(boolean z);
}
