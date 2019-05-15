package com.morphoinc.app.panoramagp3;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.panoramagp3.PanoramaGP3LocationListener.IPanoramaGP3LocationListener;

class PanoramaGP3LocationManager {
    private static final String LOG_TAG = "Camera2App";
    private static final int USE_GPS = 0;
    private static final int USE_NETWORK = 1;
    private final PanoramaGP3LocationListener[] mLocationListeners = new PanoramaGP3LocationListener[2];
    private final LocationManager mLocationManager;

    public PanoramaGP3LocationManager(LocationManager locationManager) {
        this.mLocationListeners[0] = new PanoramaGP3LocationListener();
        this.mLocationListeners[1] = new PanoramaGP3LocationListener();
        this.mLocationManager = locationManager;
    }

    public void clearLocation() {
        for (PanoramaGP3LocationListener listener : this.mLocationListeners) {
            listener.setLocation(null);
        }
    }

    public void removeUpdates() {
        PanoramaGP3LocationListener[] panoramaGP3LocationListenerArr = this.mLocationListeners;
        int length = panoramaGP3LocationListenerArr.length;
        int i = 0;
        while (i < length) {
            try {
                this.mLocationManager.removeUpdates(panoramaGP3LocationListenerArr[i]);
                i++;
            } catch (SecurityException e) {
                return;
            }
        }
    }

    public void requestLocation() {
        String provider = getLocationProviderName();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("GPS provider is ");
        stringBuilder.append(provider);
        LogFilter.v(str, stringBuilder.toString());
        if (!provider.isEmpty()) {
            this.mLocationManager.requestLocationUpdates(provider, 500, 0.1f, this.mLocationListeners[0]);
            if (!"network".equals(provider)) {
                this.mLocationManager.requestLocationUpdates("network", 1000, 0.0f, this.mLocationListeners[1]);
            }
        }
    }

    public void updateLocation(final IPanoramaGP3LocationListener locationListener) {
        this.mLocationListeners[0].start();
        this.mLocationListeners[0].setListener(new IPanoramaGP3LocationListener() {
            public void onLocationChanged() {
                PanoramaGP3LocationManager.this.mLocationListeners[0].setListener(null);
                locationListener.onLocationChanged();
            }
        });
    }

    public Location getLocation() {
        if (getLocationProviderName().isEmpty()) {
            clearLocation();
            return null;
        }
        for (int i = 0; i < 100; i++) {
            Location location = this.mLocationListeners[0].getTargetLocation();
            if (location != null) {
                return location;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LogFilter.w(LOG_TAG, "gps no location. use network location.");
        return this.mLocationListeners[1].getTargetLocation();
    }

    private String getLocationProviderName() {
        if (this.mLocationManager.isProviderEnabled("gps")) {
            return "gps";
        }
        Criteria criteria = new Criteria();
        criteria.setSpeedRequired(false);
        criteria.setAltitudeRequired(false);
        String provider = this.mLocationManager.getBestProvider(criteria, true);
        return provider == null ? "" : provider;
    }
}
