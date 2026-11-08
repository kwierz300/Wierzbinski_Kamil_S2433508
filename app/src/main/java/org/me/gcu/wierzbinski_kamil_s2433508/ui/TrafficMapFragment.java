//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.ChipGroup;

import org.me.gcu.wierzbinski_kamil_s2433508.BuildConfig;
import org.me.gcu.wierzbinski_kamil_s2433508.R;
import org.me.gcu.wierzbinski_kamil_s2433508.model.LocationPoint;
import org.me.gcu.wierzbinski_kamil_s2433508.model.RoadLocation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationKind;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel.MapFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google map with the situations: one marker per situation (icon of its kind on a
 * circle in the category colour) and, for the selected "from-to" situation, a line between its ends
 * (roadworks lines use the duration colour). The chips choose what is shown:
 * all, incidents, roadworks, or the results of the last search. Tapping a marker
 * opens the details of that situation.
 */
public class TrafficMapFragment extends Fragment implements OnMapReadyCallback {

    /** Default view: the whole of Scotland. */
    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);
    private static final float SCOTLAND_ZOOM = 5.8f;

    private TrafficViewModel viewModel;
    private GoogleMap map;
    private ChipGroup filterChips;
    private TextView mapInfo;
    private final Map<String, BitmapDescriptor> iconCache = new HashMap<>();
    private final Map<String, Marker> markersByKey = new HashMap<>();
    private List<Situation> shown = new ArrayList<>();
    private Polyline selectedLine;

    /** Creates the screen from fragment_map.xml. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    /** Sets up the filter chips and asks for the Google map. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        filterChips = view.findViewById(R.id.filterChips);
        mapInfo = view.findViewById(R.id.mapInfo);

        if (!BuildConfig.HAS_MAPS_KEY) {
            view.findViewById(R.id.noKeyText).setVisibility(View.VISIBLE);
        }

        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                viewModel.setMapFilter(filterFor(checkedIds.get(0)));
            }
        });

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /** Map ready: set up taps and the camera, then draw whenever the data or the filters change. */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setOnMarkerClickListener(marker -> {
            Object key = marker.getTag();
            selectByKey(key);
            return false;   // false: also show the info window and centre the marker
        });
        // Remember where the user has moved the map (kept over updates and rotation)
        map.setOnCameraIdleListener(() -> {
            if (map != null) {
                viewModel.setSavedCamera(map.getCameraPosition());
            }
        });

        // Draw again whenever the data, the filter, the search results or the focus change
        viewModel.getData().observe(getViewLifecycleOwner(), d -> redraw(false));
        viewModel.getMapFilter().observe(getViewLifecycleOwner(), f -> {
            checkChipFor(f);
            redraw(true);
        });
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), r -> {
            if (viewModel.getMapFilter().getValue() == MapFilter.SEARCH_RESULTS) {
                redraw(true);
            }
        });
        viewModel.getMapFocusKey().observe(getViewLifecycleOwner(), this::focusOn);
        viewModel.getSelectedKey().observe(getViewLifecycleOwner(), k -> drawSelectedLine());
    }

    /** Saves the camera so the map looks the same after rotation. */
    @Override
    public void onDestroyView() {
        if (map != null) {
            viewModel.setSavedCamera(map.getCameraPosition());   // keep the view when rotating
        }
        map = null;
        markersByKey.clear();
        super.onDestroyView();
    }

    // ------------------------------------------------------------------ filter

    /** Map filter for a chip. */
    private static MapFilter filterFor(int chipId) {
        if (chipId == R.id.chipIncidents) {
            return MapFilter.INCIDENTS;
        } else if (chipId == R.id.chipRoadworks) {
            return MapFilter.ROADWORKS;
        } else if (chipId == R.id.chipResults) {
            return MapFilter.SEARCH_RESULTS;
        }
        return MapFilter.ALL;
    }

    /** Checks the chip of the current filter. */
    private void checkChipFor(MapFilter filter) {
        int id;
        switch (filter) {
            case INCIDENTS:
                id = R.id.chipIncidents;
                break;
            case ROADWORKS:
                id = R.id.chipRoadworks;
                break;
            case SEARCH_RESULTS:
                id = R.id.chipResults;
                break;
            default:
                id = R.id.chipAll;
                break;
        }
        if (filterChips.getCheckedChipId() != id) {
            filterChips.check(id);
        }
    }

    /** Situations shown for a filter. */
    private List<Situation> situationsFor(MapFilter filter) {
        TrafficData data = viewModel.getData().getValue();
        if (data == null) {
            return new ArrayList<>();
        }
        switch (filter) {
            case INCIDENTS:
                return data.getIncidents().getSituations();
            case ROADWORKS:
                return data.getRoadworks().getSituations();
            case SEARCH_RESULTS:
                List<Situation> results = viewModel.getSearchResults().getValue();
                return results == null ? new ArrayList<>() : results;
            default:
                return data.getAll();
        }
    }

    // ------------------------------------------------------------------ drawing

    /**
     * Draws the situations for the current filter. The camera is fitted to them when the
     * filter has just changed (the ViewModel then forgets the saved camera); otherwise the
     * user's own view is kept, so automatic updates do not move the map.
     */
    private void redraw(boolean filterChanged) {
        if (map == null) {
            return;
        }
        MapFilter filter = viewModel.getMapFilter().getValue();
        shown = situationsFor(filter == null ? MapFilter.ALL : filter);
        map.clear();
        markersByKey.clear();
        selectedLine = null;

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int points = 0;
        for (Situation s : shown) {
            RoadLocation location = s.getLocation();
            LocationPoint main = location.getMainPoint();
            if (main == null) {
                continue;
            }
            LatLng position = new LatLng(main.getLatitude(), main.getLongitude());
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(SituationStyle.title(s))
                    .snippet(s.getDescription())
                    .icon(iconFor(s))
                    .anchor(0.5f, 0.5f));
            if (marker != null) {
                marker.setTag(s.getKey());
                markersByKey.put(s.getKey(), marker);
            }
            bounds.include(position);
            points++;
        }
        drawSelectedLine();
        mapInfo.setText(getString(R.string.map_count, points));

        if (viewModel.getMapFocusKey().getValue() != null) {
            return;   // the focus will move the camera
        }
        if (viewModel.getSavedCamera() != null) {
            // Same set of situations as before (new data, or after rotation): keep the user's view
            map.moveCamera(CameraUpdateFactory.newCameraPosition(viewModel.getSavedCamera()));
        } else if (points > 0) {
            fitTo(bounds.build());
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, SCOTLAND_ZOOM));
        }
    }

    /**
     * Draws the "from - to" stretch of the selected situation as a line (roadworks in the
     * colour of their length). Only the selected one is drawn: the feed gives just the two
     * end points, so many straight lines across the whole map would be hard to read.
     */
    private void drawSelectedLine() {
        if (map == null) {
            return;
        }
        if (selectedLine != null) {
            selectedLine.remove();
            selectedLine = null;
        }
        String key = viewModel.getSelectedKey().getValue();
        TrafficData data = viewModel.getData().getValue();
        Situation s = key == null || data == null ? null : data.findByKey(key);
        if (s == null || !markersByKey.containsKey(key) || !s.getLocation().isLine()) {
            return;
        }
        LocationPoint from = s.getLocation().getFrom();
        LocationPoint to = s.getLocation().getTo();
        int colour = s.getCategory() == SituationCategory.ROADWORKS
                ? SituationStyle.durationColour(requireContext(), s.getValidity().getDurationBand())
                : SituationStyle.categoryColour(requireContext(), s.getCategory());
        selectedLine = map.addPolyline(new PolylineOptions()
                .add(new LatLng(from.getLatitude(), from.getLongitude()),
                        new LatLng(to.getLatitude(), to.getLongitude()))
                .width(12f)
                .color(colour));
    }

    /** Moves the camera so all shown situations can be seen. */
    private void fitTo(LatLngBounds bounds) {
        View view = getView();
        int padding = Math.round(getResources().getDisplayMetrics().density * 48);
        if (view != null && view.getWidth() > 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            map.moveCamera(CameraUpdateFactory.zoomTo(Math.min(map.getCameraPosition().zoom, 14f)));
        } else {
            // The map has no size yet: wait until it is laid out
            map.setOnMapLoadedCallback(() -> {
                if (map != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
                    map.moveCamera(CameraUpdateFactory.zoomTo(Math.min(map.getCameraPosition().zoom, 14f)));
                }
            });
        }
    }

    /** Zooms to one situation (from "Show on map" on the details screen). */
    private void focusOn(String key) {
        if (map == null || key == null) {
            return;
        }
        Marker marker = markersByKey.get(key);
        if (marker == null) {
            // Not in the current filter: show everything so it can be found
            viewModel.setMapFilter(MapFilter.ALL);
            redraw(false);
            marker = markersByKey.get(key);
        }
        if (marker != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 13f));
            marker.showInfoWindow();
        }
        viewModel.clearMapFocus();
    }

    /** Selects the situation of a tapped marker. */
    private void selectByKey(Object key) {
        TrafficData data = viewModel.getData().getValue();
        if (data != null && key instanceof String) {
            viewModel.select(data.findByKey((String) key));
        }
    }

    // ------------------------------------------------------------------ marker icons

    /** Round marker in the category colour with the white icon of the situation kind (cached). */
    private BitmapDescriptor iconFor(Situation s) {
        SituationKind kind = s.getKind();
        String cacheKey = s.getCategory() + "/" + kind;
        BitmapDescriptor cached = iconCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Context context = requireContext();
        float density = context.getResources().getDisplayMetrics().density;
        int size = Math.round(34 * density);
        int iconSize = Math.round(18 * density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);           // white border
        paint.setColor(SituationStyle.categoryColour(context, s.getCategory()));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2 * density, paint);

        Drawable icon = AppCompatResources.getDrawable(context, SituationStyle.iconFor(kind));
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(Color.WHITE);
            int offset = (size - iconSize) / 2;
            icon.setBounds(offset, offset, offset + iconSize, offset + iconSize);
            icon.draw(canvas);
        }
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        iconCache.put(cacheKey, descriptor);
        return descriptor;
    }
}
