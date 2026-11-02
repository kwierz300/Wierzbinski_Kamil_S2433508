//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Watches the phone's internet connection and publishes it as LiveData.
 * Calls a listener when the connection comes back, so the data can be refreshed.
 */
public class NetworkMonitor {

    /** Called (on a background thread) when the connection comes back after being lost. */
    public interface ReconnectListener {
        void onReconnected();
    }

    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> online = new MutableLiveData<>();
    private ReconnectListener listener;
    private boolean registered;
    private volatile boolean lastOnline;

    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        /** Called by Android when the network changes. */
        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
            update(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        }

        /** Called by Android when the network is lost. */
        @Override
        public void onLost(@NonNull Network network) {
            update(false);
        }
    };

    /** Reads the connection state when the app starts. */
    public NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        lastOnline = isOnlineNow();
        online.setValue(lastOnline);
    }

    /** Starts watching. Safe to call more than once. */
    public synchronized void start(ReconnectListener reconnectListener) {
        this.listener = reconnectListener;
        if (!registered && connectivityManager != null) {
            connectivityManager.registerDefaultNetworkCallback(callback);
            registered = true;
        }
    }

    /** LiveData: true when there is an internet connection. */
    public LiveData<Boolean> getOnline() {
        return online;
    }

    /** Checks the connection right now. */
    public boolean isOnlineNow() {
        if (connectivityManager == null) {
            return false;
        }
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /** Publishes the new state and calls the listener when the connection comes back. */
    private void update(boolean nowOnline) {
        boolean wasOnline = lastOnline;
        lastOnline = nowOnline;
        online.postValue(nowOnline);
        if (nowOnline && !wasOnline && listener != null) {
            listener.onReconnected();
        }
    }
}
