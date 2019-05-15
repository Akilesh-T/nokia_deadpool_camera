package android.support.v7.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import com.hmdglobal.app.camera.util.PermissionsUtil;
import java.util.Calendar;

class TwilightManager {
    private static final int SUNRISE = 6;
    private static final int SUNSET = 22;
    private static final String TAG = "TwilightManager";
    private static TwilightManager sInstance;
    private final Context mContext;
    private final LocationManager mLocationManager;
    private final TwilightState mTwilightState = new TwilightState();

    private static class TwilightState {
        boolean isNight;
        long nextUpdate;
        long todaySunrise;
        long todaySunset;
        long tomorrowSunrise;
        long yesterdaySunset;

        TwilightState() {
        }
    }

    static TwilightManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            context = context.getApplicationContext();
            sInstance = new TwilightManager(context, (LocationManager) context.getSystemService("location"));
        }
        return sInstance;
    }

    @VisibleForTesting
    static void setInstance(TwilightManager twilightManager) {
        sInstance = twilightManager;
    }

    @VisibleForTesting
    TwilightManager(@NonNull Context context, @NonNull LocationManager locationManager) {
        this.mContext = context;
        this.mLocationManager = locationManager;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isNight() {
        TwilightState state = this.mTwilightState;
        if (isStateValid()) {
            return state.isNight;
        }
        Location location = getLastKnownLocation();
        if (location != null) {
            updateState(location);
            return state.isNight;
        }
        Log.i(TAG, "Could not get last known location. This is probably because the app does not have any location permissions. Falling back to hardcoded sunrise/sunset values.");
        int hour = Calendar.getInstance().get(11);
        boolean z = hour < 6 || hour >= 22;
        return z;
    }

    @SuppressLint({"MissingPermission"})
    private Location getLastKnownLocation() {
        Location coarseLoc = null;
        Location fineLoc = null;
        if (PermissionChecker.checkSelfPermission(this.mContext, PermissionsUtil.PERMS_ACCESS_COARSE_LOCATION) == 0) {
            coarseLoc = getLastKnownLocationForProvider("network");
        }
        if (PermissionChecker.checkSelfPermission(this.mContext, PermissionsUtil.PERMS_ACCESS_FINE_LOCATION) == 0) {
            fineLoc = getLastKnownLocationForProvider("gps");
        }
        if (fineLoc == null || coarseLoc == null) {
            return fineLoc != null ? fineLoc : coarseLoc;
        }
        return fineLoc.getTime() > coarseLoc.getTime() ? fineLoc : coarseLoc;
    }

    @RequiresPermission(anyOf = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"})
    private Location getLastKnownLocationForProvider(String provider) {
        try {
            if (this.mLocationManager.isProviderEnabled(provider)) {
                return this.mLocationManager.getLastKnownLocation(provider);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to get last known location", e);
        }
        return null;
    }

    private boolean isStateValid() {
        return this.mTwilightState.nextUpdate > System.currentTimeMillis();
    }

    private void updateState(@NonNull Location location) {
        long nextUpdate;
        TwilightState state = this.mTwilightState;
        long now = System.currentTimeMillis();
        TwilightCalculator calculator = TwilightCalculator.getInstance();
        TwilightCalculator twilightCalculator = calculator;
        twilightCalculator.calculateTwilight(now - 86400000, location.getLatitude(), location.getLongitude());
        long yesterdaySunset = calculator.sunset;
        twilightCalculator.calculateTwilight(now, location.getLatitude(), location.getLongitude());
        boolean z = true;
        if (calculator.state != 1) {
            z = false;
        }
        boolean isNight = z;
        long todaySunrise = calculator.sunrise;
        long todaySunset = calculator.sunset;
        long j = 86400000 + now;
        long yesterdaySunset2 = yesterdaySunset;
        yesterdaySunset = todaySunset;
        todaySunset = j;
        j = todaySunrise;
        boolean isNight2 = isNight;
        calculator.calculateTwilight(todaySunset, location.getLatitude(), location.getLongitude());
        long tomorrowSunrise = calculator.sunrise;
        if (j == -1 || yesterdaySunset == -1) {
            nextUpdate = now + 43200000;
        } else {
            if (now > yesterdaySunset) {
                nextUpdate = 0 + tomorrowSunrise;
            } else if (now > j) {
                nextUpdate = 0 + yesterdaySunset;
            } else {
                nextUpdate = 0 + j;
            }
            nextUpdate += 60000;
        }
        state.isNight = isNight2;
        state.yesterdaySunset = yesterdaySunset2;
        state.todaySunrise = j;
        state.todaySunset = yesterdaySunset;
        state.tomorrowSunrise = tomorrowSunrise;
        state.nextUpdate = nextUpdate;
    }
}
