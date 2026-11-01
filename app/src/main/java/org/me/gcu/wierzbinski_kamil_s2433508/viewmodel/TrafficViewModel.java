//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.me.gcu.wierzbinski_kamil_s2433508.data.TrafficRepository;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;

/**
 * ViewModel shared by the screens of the app (the "ViewModel" part of MVVM).
 * It survives screen rotation, so the data is not downloaded again and
 * the user does not lose what is on screen when the phone is turned.
 */
public class TrafficViewModel extends AndroidViewModel {

    private final TrafficRepository repository;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = TrafficRepository.getInstance(application);
        repository.start();
    }

    public LiveData<TrafficData> getData() {
        return repository.getData();
    }

    public LiveData<Boolean> getLoading() {
        return repository.getLoading();
    }

    public LiveData<Boolean> getOnline() {
        return repository.getOnline();
    }

    /** Manual refresh from the toolbar. */
    public void refresh() {
        repository.update();
    }

    /** Called by the activity: automatic updates run only while the app is on screen. */
    public void setAutoUpdate(boolean enabled) {
        repository.setAutoUpdate(enabled);
    }

    public int getRefreshMinutes() {
        return repository.getConfig().getRefreshMinutes();
    }

    public boolean isUsingSampleData() {
        return repository.getConfig().useSampleData();
    }
}
