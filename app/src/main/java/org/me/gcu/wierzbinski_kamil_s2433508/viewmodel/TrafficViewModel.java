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
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.wierzbinski_kamil_s2433508.data.TrafficRepository;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;

import java.util.Objects;

/**
 * ViewModel shared by the activity and all fragments (the "ViewModel" part of MVVM).
 * It holds the data and also the state of the screens (which screen, which tab,
 * which situation is selected). It survives screen rotation, so after turning the
 * phone the user sees exactly the same things as before.
 */
public class TrafficViewModel extends AndroidViewModel {

    /** The main screens of the app (one per navigation item). */
    public enum Screen { LIST, SEARCH, MAP }

    private final TrafficRepository repository;

    private final MutableLiveData<Screen> screen = new MutableLiveData<>(Screen.LIST);
    private final MutableLiveData<SituationCategory> listCategory =
            new MutableLiveData<>(SituationCategory.INCIDENT);
    private final MutableLiveData<String> selectedKey = new MutableLiveData<>();
    private final MediatorLiveData<Situation> selectedSituation = new MediatorLiveData<>();

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = TrafficRepository.getInstance(application);
        repository.start();
        // The selected situation is worked out again when the data or the selection changes
        selectedSituation.addSource(repository.getData(), data -> updateSelectedSituation());
        selectedSituation.addSource(selectedKey, key -> updateSelectedSituation());
    }

    private void updateSelectedSituation() {
        TrafficData data = repository.getData().getValue();
        String key = selectedKey.getValue();
        Situation found = (data == null || key == null) ? null : data.findByKey(key);
        if (!Objects.equals(found, selectedSituation.getValue())) {
            selectedSituation.setValue(found);
        }
    }

    // ------------------------------------------------------------------ data

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

    // ------------------------------------------------------------------ screen state

    public LiveData<Screen> getScreen() {
        return screen;
    }

    public void setScreen(Screen newScreen) {
        if (newScreen != screen.getValue()) {
            screen.setValue(newScreen);
        }
    }

    public LiveData<SituationCategory> getListCategory() {
        return listCategory;
    }

    public void setListCategory(SituationCategory category) {
        if (category != listCategory.getValue()) {
            listCategory.setValue(category);
        }
    }

    public LiveData<String> getSelectedKey() {
        return selectedKey;
    }

    public LiveData<Situation> getSelectedSituation() {
        return selectedSituation;
    }

    public void select(Situation situation) {
        String key = situation == null ? null : situation.getKey();
        if (!Objects.equals(key, selectedKey.getValue())) {
            selectedKey.setValue(key);
        }
    }

    public void clearSelection() {
        select(null);
    }
}
