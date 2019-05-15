package com.morphoinc.app.panoramagp3;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import com.morphoinc.app.LogFilter;
import java.util.Date;

class PanoramaGP3LocationListener implements LocationListener {
    private static final String LOG_TAG = "Camera2App";
    private static final IPanoramaGP3LocationListener nullListener = new IPanoramaGP3LocationListener() {
        public void onLocationChanged() {
        }
    };
    private volatile IPanoramaGP3LocationListener mListener = nullListener;
    private Location mLocation = null;
    private long mStartTime = 0;

    public interface IPanoramaGP3LocationListener {
        void onLocationChanged();
    }

    PanoramaGP3LocationListener() {
    }

    public void setListener(IPanoramaGP3LocationListener listener) {
        if (listener == null) {
            this.mListener = nullListener;
        } else {
            this.mListener = listener;
        }
    }

    public Location getLocation() {
        return this.mLocation;
    }

    public void setLocation(Location location) {
        this.mLocation = location;
    }

    public void start() {
        this.mStartTime = new Date().getTime();
    }

    public Location getTargetLocation() {
        if (this.mLocation != null && this.mStartTime <= this.mLocation.getTime()) {
            return this.mLocation;
        }
        return null;
    }

    public void onLocationChanged(Location location) {
        LogFilter.v(LOG_TAG, "onLocationChanged");
        setLocation(location);
        this.mListener.onLocationChanged();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        String str;
        StringBuilder stringBuilder;
        switch (status) {
            case 0:
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LocationListener(");
                stringBuilder.append(provider);
                stringBuilder.append(").onStatusChanged OUT_OF_SERVICE");
                LogFilter.v(str, stringBuilder.toString());
                return;
            case 1:
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LocationListener(");
                stringBuilder.append(provider);
                stringBuilder.append(").onStatusChanged TEMPORARILY_UNAVAILABLE");
                LogFilter.v(str, stringBuilder.toString());
                return;
            case 2:
                str = LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LocationListener(");
                stringBuilder.append(provider);
                stringBuilder.append(").onStatusChanged AVAILABLE");
                LogFilter.v(str, stringBuilder.toString());
                return;
            default:
                return;
        }
    }

    public void onProviderEnabled(String provider) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LocationListener(");
        stringBuilder.append(provider);
        stringBuilder.append(").onProviderEnabled");
        LogFilter.v(str, stringBuilder.toString());
    }

    public void onProviderDisabled(String provider) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LocationListener(");
        stringBuilder.append(provider);
        stringBuilder.append(").onProviderDisabled");
        LogFilter.v(str, stringBuilder.toString());
    }
}
