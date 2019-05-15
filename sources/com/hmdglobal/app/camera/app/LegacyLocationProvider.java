package com.hmdglobal.app.camera.app;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;

public class LegacyLocationProvider implements LocationProvider {
    private static final Tag TAG = new Tag("LcyLocProvider");
    private Context mContext;
    LocationListener[] mLocationListeners = new LocationListener[]{new LocationListener("gps"), new LocationListener("network")};
    private LocationManager mLocationManager;
    private boolean mRecordLocation;

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;
        String mProvider;
        boolean mValid = false;

        public LocationListener(String provider) {
            this.mProvider = provider;
            this.mLastLocation = new Location(this.mProvider);
        }

        public void onLocationChanged(Location newLocation) {
            if (newLocation.getLatitude() != Camera2ParamsFragment.TARGET_EV || newLocation.getLongitude() != Camera2ParamsFragment.TARGET_EV) {
                if (!this.mValid) {
                    Log.d(LegacyLocationProvider.TAG, "Got first location.");
                }
                this.mLastLocation.set(newLocation);
                this.mValid = true;
            }
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
            this.mValid = false;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case 0:
                case 1:
                    this.mValid = false;
                    return;
                default:
                    return;
            }
        }

        public Location current() {
            return this.mValid ? this.mLastLocation : null;
        }
    }

    public LegacyLocationProvider(Context context) {
        this.mContext = context;
    }

    public Location getCurrentLocation() {
        if (!this.mRecordLocation) {
            return null;
        }
        for (Location l : this.mLocationListeners) {
            Location l2 = l2.current();
            if (l2 != null) {
                return l2;
            }
        }
        Log.d(TAG, "No location received yet.");
        return null;
    }

    public void recordLocation(boolean recordLocation) {
        if (this.mRecordLocation != recordLocation) {
            this.mRecordLocation = recordLocation;
            if (recordLocation) {
                startReceivingLocationUpdates();
            } else {
                stopReceivingLocationUpdates();
            }
        }
    }

    public void disconnect() {
        Log.d(TAG, "disconnect");
    }

    private void startReceivingLocationUpdates() {
        Tag tag;
        StringBuilder stringBuilder;
        if (this.mLocationManager == null) {
            this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        }
        if (this.mLocationManager != null) {
            try {
                this.mLocationManager.requestLocationUpdates("network", 1000, 0.0f, this.mLocationListeners[1]);
            } catch (SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex2) {
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("provider does not exist ");
                stringBuilder.append(ex2.getMessage());
                Log.d(tag, stringBuilder.toString());
            }
            try {
                this.mLocationManager.requestLocationUpdates("gps", 1000, 0.0f, this.mLocationListeners[0]);
            } catch (SecurityException ex3) {
                Log.i(TAG, "fail to request location update, ignore", ex3);
            } catch (IllegalArgumentException ex22) {
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("provider does not exist ");
                stringBuilder.append(ex22.getMessage());
                Log.d(tag, stringBuilder.toString());
            }
            Log.d(TAG, "startReceivingLocationUpdates");
        }
    }

    private void stopReceivingLocationUpdates() {
        if (this.mLocationManager != null) {
            for (android.location.LocationListener removeUpdates : this.mLocationListeners) {
                try {
                    this.mLocationManager.removeUpdates(removeUpdates);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
            Log.d(TAG, "stopReceivingLocationUpdates");
        }
    }
}
