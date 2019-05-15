package com.hmdglobal.app.camera.app;

import android.content.Context;
import android.location.Location;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class LocationManager {
    private static final Tag TAG = new Tag("LocationManager");
    LocationProvider mLocationProvider;
    private boolean mRecordLocation;

    public LocationManager(Context context) {
        Log.d(TAG, "Using legacy location provider.");
        this.mLocationProvider = new LegacyLocationProvider(context);
    }

    public void recordLocation(boolean recordLocation) {
        this.mRecordLocation = recordLocation;
        this.mLocationProvider.recordLocation(this.mRecordLocation);
    }

    public Location getCurrentLocation() {
        return this.mLocationProvider.getCurrentLocation();
    }

    public void disconnect() {
        this.mLocationProvider.disconnect();
    }
}
