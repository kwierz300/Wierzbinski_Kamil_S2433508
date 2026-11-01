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
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Gets the phone's approximate ("coarse") location with the standard Android
 * LocationManager (no extra libraries). Uses the last known location if there is
 * one, otherwise waits for one new location for up to 10 seconds.
 */
public class DeviceLocation {

    /** Result of a location request (always called on the main thread). */
    public interface Callback {
        void onLocation(Location location);

        void onNoLocation(String reason);
    }

    private static final long TIMEOUT_MS = 10_000;

    private final Context context;
    private final LocationManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public DeviceLocation(Context context) {
        this.context = context.getApplicationContext();
        this.manager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

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
            if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                callback.onNoLocation("Location is turned off on the phone");
                return;
            }
            waitForOneLocation(callback);
        } catch (SecurityException e) {
            callback.onNoLocation("Location permission not given");
        }
    }

    private void waitForOneLocation(Callback callback) throws SecurityException {
        final boolean[] finished = {false};
        LocationListener listener = new LocationListener() {
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

            @Override
            public void onProviderEnabled(@NonNull String provider) {
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
            }
        };
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener, Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (!finished[0]) {
                finished[0] = true;
                manager.removeUpdates(listener);
                callback.onNoLocation("Location not found in time");
            }
        }, TIMEOUT_MS);
    }
}
