//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.data;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import java.util.List;

/**
 * Gets the phone's approximate ("coarse") location with the standard Android
 * LocationManager (no extra libraries). Uses the last known location if there is
 * one, otherwise waits for one new location for up to 15 seconds.
 */
public class DeviceLocation {

    /** Result of a location request (always called on the main thread). */
    public interface Callback {
        void onLocation(Location location);

        void onNoLocation(String reason);
    }

    private static final long TIMEOUT_MS = 15_000;

    private final Context context;
    private final LocationManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    /** Uses the standard Android LocationManager. */
    public DeviceLocation(Context context) {
        this.context = context.getApplicationContext();
        this.manager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    /** True if the user has allowed approximate location. */
    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Gets the location (last known one, or waits for a new one) and passes it to the callback. */
    public void request(Callback callback) {
        if (!hasPermission()) {
            callback.onNoLocation("Location permission not given");
            return;
        }
        if (manager == null) {
            callback.onNoLocation("Location is not available on this device");
            return;
        }
        try {
            if (!LocationManagerCompat.isLocationEnabled(manager)) {
                callback.onNoLocation("Location is turned off on the phone");
                return;
            }
            Location best = null;
            for (String provider : manager.getProviders(true)) {
                Location last = manager.getLastKnownLocation(provider);
                if (last != null && (best == null || last.getTime() > best.getTime())) {
                    best = last;
                }
            }
            if (best != null) {
                callback.onLocation(best);
                return;
            }
            String provider = chooseProvider();
            if (provider == null) {
                callback.onNoLocation("Location is turned off on the phone");
                return;
            }
            waitForOneLocation(provider, callback);
        } catch (SecurityException e) {
            callback.onNoLocation("Location permission not given");
        }
    }

    /**
     * Picks a working location source. Phones differ: many newer ones (Android 12+) use
     * the "fused" source, others "network"; "gps" is the last choice (on Android 12+
     * it also works with approximate location and gives a rounded position).
     */
    private String chooseProvider() {
        List<String> enabled = manager.getProviders(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && enabled.contains(LocationManager.FUSED_PROVIDER)) {
            return LocationManager.FUSED_PROVIDER;
        }
        if (enabled.contains(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && enabled.contains(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        return null;
    }

    /** Asks the given source for one new location and gives up after 15 seconds. */
    private void waitForOneLocation(String provider, Callback callback) throws SecurityException {
        final boolean[] finished = {false};
        LocationListener listener = new LocationListener() {
            /** First new location: stop listening and return it. */
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!finished[0]) {
                    finished[0] = true;
                    manager.removeUpdates(this);
                    callback.onLocation(location);
                }
            }

            // The three methods below are needed on Android 8-10, where they are not optional
            @Override
            @SuppressWarnings("deprecation")
            public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
            }

            /** Not needed by the app. */
            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            /** Not needed by the app. */
            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };
        manager.requestLocationUpdates(provider, 0, 0, listener, Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (!finished[0]) {
                finished[0] = true;
                manager.removeUpdates(listener);
                callback.onNoLocation("Location not found in time");
            }
        }, TIMEOUT_MS);
    }
}
