//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.me.gcu.wierzbinski_kamil_s2433508.R;
import org.me.gcu.wierzbinski_kamil_s2433508.data.DeviceLocation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SearchOrigin;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel.SearchMode;

import java.util.List;
import java.util.Locale;

/**
 * Search screen with two options:
 * - by road name (with suggestions of all roads in the current data),
 * - by distance from the phone's approximate location (or a default point),
 * with a slider for the distance. Results can be shown together on the map.
 */
public class SearchFragment extends Fragment {

    private TrafficViewModel viewModel;
    private DeviceLocation deviceLocation;
    private MaterialButtonToggleGroup modeToggle;
    private View roadPanel;
    private View distancePanel;
    private MaterialAutoCompleteTextView roadInput;
    private TextView originText;
    private TextView distanceLabel;
    private Slider distanceSlider;
    private TextView resultCount;
    private MaterialButton showOnMapButton;
    private SituationAdapter adapter;

    /** Asks the user for the location permission; if refused, the default point is used. */
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    findMyLocation();
                } else {
                    viewModel.setOrigin(SearchOrigin.DEFAULT);
                    toast(getString(R.string.location_denied));
                }
            });

    /** Creates the screen from fragment_search.xml. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    /** Connects the views to the ViewModel: mode buttons, road field, slider, location button and results. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        deviceLocation = new DeviceLocation(requireContext());

        modeToggle = view.findViewById(R.id.modeToggle);
        roadPanel = view.findViewById(R.id.roadPanel);
        distancePanel = view.findViewById(R.id.distancePanel);
        roadInput = view.findViewById(R.id.roadInput);
        originText = view.findViewById(R.id.originText);
        distanceLabel = view.findViewById(R.id.distanceLabel);
        distanceSlider = view.findViewById(R.id.distanceSlider);
        resultCount = view.findViewById(R.id.resultCount);
        showOnMapButton = view.findViewById(R.id.showOnMapButton);
        RecyclerView results = view.findViewById(R.id.results);

        adapter = new SituationAdapter(viewModel::select);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        results.setAdapter(adapter);

        // Mode (road / distance)
        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.setSearchMode(checkedId == R.id.modeDistance ? SearchMode.DISTANCE : SearchMode.ROAD);
            }
        });

        // Search by road
        roadInput.setText(viewModel.getRoadQuery().getValue(), false);   // false: do not open suggestions
        roadInput.setOnItemClickListener((parent, v, position, id) -> runRoadSearch());
        roadInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_SEARCH || enter) {
                runRoadSearch();
                return true;
            }
            return false;
        });
        view.findViewById(R.id.roadSearchButton).setOnClickListener(v -> runRoadSearch());

        // Search by distance
        Integer km = viewModel.getDistanceKm().getValue();
        distanceSlider.setValue(km == null ? TrafficViewModel.DEFAULT_DISTANCE_KM : km);
        distanceSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                viewModel.setDistanceKm(Math.round(value));
            }
        });
        view.findViewById(R.id.locationButton).setOnClickListener(v -> askForLocation());

        showOnMapButton.setOnClickListener(v -> viewModel.showSearchResultsOnMap());

        viewModel.getData().observe(getViewLifecycleOwner(), d -> updateSuggestions());
        viewModel.getSearchMode().observe(getViewLifecycleOwner(), this::showMode);
        viewModel.getDistanceKm().observe(getViewLifecycleOwner(), k -> updateDistanceTexts());
        viewModel.getOrigin().observe(getViewLifecycleOwner(), o -> updateDistanceTexts());
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), this::showResults);
        viewModel.getSelectedKey().observe(getViewLifecycleOwner(), adapter::setSelectedKey);
    }

    /** Shows the road controls or the distance controls. */
    private void showMode(SearchMode mode) {
        boolean distance = mode == SearchMode.DISTANCE;
        int buttonId = distance ? R.id.modeDistance : R.id.modeRoad;
        if (modeToggle.getCheckedButtonId() != buttonId) {
            modeToggle.check(buttonId);
        }
        roadPanel.setVisibility(distance ? View.GONE : View.VISIBLE);
        distancePanel.setVisibility(distance ? View.VISIBLE : View.GONE);
        if (distance) {
            hideKeyboard();
        }
    }

    /** Fills the road suggestions from the current data. */
    private void updateSuggestions() {
        List<String> roads = viewModel.getRoadNames();
        roadInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, roads));
    }

    /** Starts the road search with the typed road. */
    private void runRoadSearch() {
        viewModel.searchRoad(roadInput.getText() == null ? "" : roadInput.getText().toString());
        roadInput.dismissDropDown();
        hideKeyboard();
    }

    /** Shows the distance and the starting point. */
    private void updateDistanceTexts() {
        Integer km = viewModel.getDistanceKm().getValue();
        SearchOrigin origin = viewModel.getOrigin().getValue();
        if (km != null) {
            distanceLabel.setText(getString(R.string.distance_label, km));
        }
        if (origin != null) {
            originText.setText(getString(R.string.origin_label, origin.getLabel()));
        }
    }

    /** Shows the results list and the count text. */
    private void showResults(List<Situation> list) {
        SearchMode mode = viewModel.getSearchMode().getValue();
        adapter.setDistanceFrom(mode == SearchMode.DISTANCE ? viewModel.getOrigin().getValue() : null);
        adapter.submitList(list);
        int count = list == null ? 0 : list.size();
        if (mode == SearchMode.DISTANCE) {
            Integer km = viewModel.getDistanceKm().getValue();
            resultCount.setText(getString(R.string.results_distance, count, km == null ? 0 : km));
        } else {
            String road = viewModel.getRoadQuery().getValue();
            if (road == null || road.isEmpty()) {
                resultCount.setText(R.string.results_type_road);
            } else if (count == 0) {
                resultCount.setText(getString(R.string.results_none_road, road.toUpperCase(Locale.UK)));
            } else {
                resultCount.setText(getString(R.string.results_road, count, road.toUpperCase(Locale.UK)));
            }
        }
        showOnMapButton.setEnabled(count > 0);
    }

    // ------------------------------------------------------------------ location

    /** Asks for the location permission if needed, then finds the location. */
    private void askForLocation() {
        if (deviceLocation.hasPermission()) {
            findMyLocation();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    /**
     * Gets the phone's location and uses it as the search point. If location is switched
     * off in the phone, asks the user to turn it on (the app cannot do it itself).
     */
    private void findMyLocation() {
        if (!deviceLocation.isLocationOn()) {
            askToTurnOnLocation();
            return;
        }
        originText.setText(R.string.location_searching);
        deviceLocation.request(new DeviceLocation.Callback() {
            /** Location found: use it as the search point. */
            @Override
            public void onLocation(Location location) {
                if (!isAdded()) {
                    return;
                }
                viewModel.setOrigin(new SearchOrigin(location.getLatitude(), location.getLongitude(),
                        getString(R.string.origin_device), true));
            }

            /** No location: use the default point and tell the user why. */
            @Override
            public void onNoLocation(String reason) {
                if (!isAdded()) {
                    return;
                }
                viewModel.setOrigin(SearchOrigin.DEFAULT);
                toast(getString(R.string.location_failed, reason));
            }
        });
    }

    /** Dialog: open the phone's location settings, or keep using the default point. */
    private void askToTurnOnLocation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.location_off_title)
                .setMessage(R.string.location_off_text)
                .setPositiveButton(R.string.location_open_settings, (d, w) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton(R.string.location_use_default, (d, w) ->
                        viewModel.setOrigin(SearchOrigin.DEFAULT))
                .show();
    }

    /** Shows a short message. */
    private void toast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    /** Hides the on-screen keyboard. */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }
}
