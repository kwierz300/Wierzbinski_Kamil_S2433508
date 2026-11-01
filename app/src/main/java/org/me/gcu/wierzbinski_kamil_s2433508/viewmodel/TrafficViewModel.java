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
import com.google.android.gms.maps.model.CameraPosition;

import org.me.gcu.wierzbinski_kamil_s2433508.data.TextBreakdown;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SearchOrigin;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Validity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * ViewModel shared by the activity and all fragments (the "ViewModel" part of MVVM).
 * It holds the data and also the state of the screens (which screen, which tab,
 * which situation is selected). It survives screen rotation, so after turning the
 * phone the user sees exactly the same things as before.
 */
public class TrafficViewModel extends AndroidViewModel {

    /** The main screens of the app (one per navigation item). */
    public enum Screen { LIST, SEARCH, MAP }

    /** The two kinds of search. */
    public enum SearchMode { ROAD, DISTANCE }

    /** What the map shows. */
    public enum MapFilter { ALL, INCIDENTS, ROADWORKS, SEARCH_RESULTS }

    /** Filter of roadworks by time: all, only those happening now, or only planned ones. */
    public enum TimingFilter { ANY, ACTIVE, PLANNED }

    public static final int DEFAULT_DISTANCE_KM = 25;

    private final TrafficRepository repository;

    private final MutableLiveData<Screen> screen = new MutableLiveData<>(Screen.LIST);
    private final MutableLiveData<SituationCategory> listCategory =
            new MutableLiveData<>(SituationCategory.INCIDENT);
    private final MutableLiveData<String> selectedKey = new MutableLiveData<>();
    private final MediatorLiveData<Situation> selectedSituation = new MediatorLiveData<>();
    // Landscape only: details pane hidden so the list/search/map uses the whole width
    private final MutableLiveData<Boolean> detailsCollapsed = new MutableLiveData<>(false);

    // Roadworks list filters (null duration band = all lengths)
    private final MutableLiveData<Validity.DurationBand> durationFilter = new MutableLiveData<>();
    private final MutableLiveData<TimingFilter> timingFilter = new MutableLiveData<>(TimingFilter.ANY);

    // Search state
    private final MutableLiveData<SearchMode> searchMode = new MutableLiveData<>(SearchMode.ROAD);
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> distanceKm = new MutableLiveData<>(DEFAULT_DISTANCE_KM);
    private final MutableLiveData<SearchOrigin> origin = new MutableLiveData<>(SearchOrigin.DEFAULT);
    private final MediatorLiveData<List<Situation>> searchResults = new MediatorLiveData<>();

    // Map state
    private final MutableLiveData<MapFilter> mapFilter = new MutableLiveData<>(MapFilter.ALL);
    private final MutableLiveData<String> mapFocusKey = new MutableLiveData<>();
    private CameraPosition savedCamera;   // camera position of the map, kept over rotation

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = TrafficRepository.getInstance(application);
        repository.start();
        // The selected situation is worked out again when the data or the selection changes
        selectedSituation.addSource(repository.getData(), data -> updateSelectedSituation());
        selectedSituation.addSource(selectedKey, key -> updateSelectedSituation());
        // Search results are worked out again when the data or any search setting changes
        searchResults.addSource(repository.getData(), d -> updateSearchResults());
        searchResults.addSource(searchMode, m -> updateSearchResults());
        searchResults.addSource(roadQuery, q -> updateSearchResults());
        searchResults.addSource(distanceKm, k -> updateSearchResults());
        searchResults.addSource(origin, o -> updateSearchResults());
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

    public LiveData<Boolean> getDetailsCollapsed() {
        return detailsCollapsed;
    }

    public void setDetailsCollapsed(boolean collapsed) {
        if (!Boolean.valueOf(collapsed).equals(detailsCollapsed.getValue())) {
            detailsCollapsed.setValue(collapsed);
        }
    }

    public void select(Situation situation) {
        if (situation != null) {
            setDetailsCollapsed(false);   // a new choice: make sure its details can be seen
        }
        String key = situation == null ? null : situation.getKey();
        if (!Objects.equals(key, selectedKey.getValue())) {
            selectedKey.setValue(key);
        }
    }

    public void clearSelection() {
        select(null);
    }

    // ------------------------------------------------------------------ roadworks filters

    public LiveData<Validity.DurationBand> getDurationFilter() {
        return durationFilter;
    }

    /** Shows only roadworks of one length band; null shows all. */
    public void setDurationFilter(Validity.DurationBand band) {
        if (band != durationFilter.getValue()) {
            durationFilter.setValue(band);
        }
    }

    public LiveData<TimingFilter> getTimingFilter() {
        return timingFilter;
    }

    public void setTimingFilter(TimingFilter filter) {
        if (filter != timingFilter.getValue()) {
            timingFilter.setValue(filter);
        }
    }

    /** Roadworks that match the length band (null = any) and the timing filter at the given moment. */
    public static List<Situation> filterRoadworks(List<Situation> all, Validity.DurationBand band,
                                                  TimingFilter timing, OffsetDateTime now) {
        List<Situation> result = new ArrayList<>();
        for (Situation s : all) {
            Validity v = s.getValidity();
            if (band != null && v.getDurationBand() != band) {
                continue;
            }
            if (timing == TimingFilter.ACTIVE && v.getTiming(now) != Validity.Timing.ACTIVE) {
                continue;
            }
            if (timing == TimingFilter.PLANNED && v.getTiming(now) != Validity.Timing.PLANNED) {
                continue;
            }
            result.add(s);
        }
        return result;
    }

    // ------------------------------------------------------------------ search

    public LiveData<SearchMode> getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(SearchMode mode) {
        if (mode != searchMode.getValue()) {
            searchMode.setValue(mode);
        }
    }

    public LiveData<String> getRoadQuery() {
        return roadQuery;
    }

    public void searchRoad(String road) {
        roadQuery.setValue(road == null ? "" : road.trim());
    }

    public LiveData<Integer> getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(int km) {
        Integer current = distanceKm.getValue();
        if (current == null || current != km) {
            distanceKm.setValue(km);
        }
    }

    public LiveData<SearchOrigin> getOrigin() {
        return origin;
    }

    public void setOrigin(SearchOrigin newOrigin) {
        origin.setValue(newOrigin == null ? SearchOrigin.DEFAULT : newOrigin);
    }

    /** Results of the current search (road or distance), never null. */
    public LiveData<List<Situation>> getSearchResults() {
        return searchResults;
    }

    private void updateSearchResults() {
        TrafficData data = repository.getData().getValue();
        List<Situation> result = new ArrayList<>();
        if (data != null) {
            if (searchMode.getValue() == SearchMode.DISTANCE) {
                result = findNear(data.getAll(), origin.getValue(), distanceKm.getValue());
            } else {
                result = findOnRoad(data.getAll(), roadQuery.getValue());
            }
        }
        searchResults.setValue(Collections.unmodifiableList(result));
    }

    /** All situations on the given road ("m8", "A74(M)" ...). Empty for an empty query. */
    public static List<Situation> findOnRoad(List<Situation> all, String road) {
        List<Situation> result = new ArrayList<>();
        if (road == null || road.trim().isEmpty()) {
            return result;
        }
        for (Situation s : all) {
            if (s.isOnRoad(road)) {
                result.add(s);
            }
        }
        return result;
    }

    /** Situations within the distance of the origin, nearest first. */
    public static List<Situation> findNear(List<Situation> all, SearchOrigin from, Integer km) {
        List<Situation> result = new ArrayList<>();
        if (from == null || km == null) {
            return result;
        }
        for (Situation s : all) {
            if (s.distanceKmTo(from.getLatitude(), from.getLongitude()) <= km) {
                result.add(s);
            }
        }
        Collections.sort(result, Comparator.comparingDouble(
                s -> s.distanceKmTo(from.getLatitude(), from.getLongitude())));
        return result;
    }

    /** Road names in the current data, for the suggestions list (A roads, then B, then M, by number). */
    public List<String> getRoadNames() {
        TrafficData data = repository.getData().getValue();
        TreeSet<String> names = new TreeSet<>(TrafficViewModel::compareRoads);
        if (data != null) {
            for (Situation s : data.getAll()) {
                if (s.getRoadName() != null) {
                    names.add(s.getRoadName());
                }
            }
        }
        return new ArrayList<>(names);
    }

    private static int compareRoads(String a, String b) {
        int byLetter = Character.compare(a.charAt(0), b.charAt(0));
        if (byLetter != 0) {
            return byLetter;
        }
        int byNumber = Integer.compare(roadNumber(a), roadNumber(b));
        return byNumber != 0 ? byNumber : a.compareTo(b);
    }

    private static int roadNumber(String road) {
        String digits = road.replaceAll("[^0-9]", "");
        try {
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Normalised form of a road name typed by the user (same rules as the data). */
    public static String normaliseRoad(String road) {
        return TextBreakdown.normaliseRoad(road);
    }

    // ------------------------------------------------------------------ map

    public LiveData<MapFilter> getMapFilter() {
        return mapFilter;
    }

    public void setMapFilter(MapFilter filter) {
        if (filter != mapFilter.getValue()) {
            savedCamera = null;   // new set of situations: fit the camera to them
            mapFilter.setValue(filter);
        }
    }

    /** Opens the map showing the current search results. */
    public void showSearchResultsOnMap() {
        savedCamera = null;
        mapFilter.setValue(MapFilter.SEARCH_RESULTS);
        setScreen(Screen.MAP);
    }

    /** Opens the map zoomed to one situation (from the details screen). */
    public void showOnMap(Situation situation) {
        if (situation == null) {
            return;
        }
        mapFocusKey.setValue(situation.getKey());
        setScreen(Screen.MAP);
    }

    public LiveData<String> getMapFocusKey() {
        return mapFocusKey;
    }

    /** Called by the map once it has zoomed to the focused situation. */
    public void clearMapFocus() {
        mapFocusKey.setValue(null);
    }

    public CameraPosition getSavedCamera() {
        return savedCamera;
    }

    public void setSavedCamera(CameraPosition camera) {
        savedCamera = camera;
    }
}
