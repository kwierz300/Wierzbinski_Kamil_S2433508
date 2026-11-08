//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;

import org.me.gcu.wierzbinski_kamil_s2433508.R;
import org.me.gcu.wierzbinski_kamil_s2433508.model.FeedState;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Validity;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel;

import java.util.List;

/**
 * Screen with the list of all current situations, in two tabs:
 * incidents (unplanned events) and roadworks. Roadworks can be filtered by length
 * (the coloured dots are also the legend of the colour coding). A line shows when the data was updated and when the feed
 * was published. Tapping an item selects it; {@link SituationDetailFragment} shows it.
 */
public class SituationListFragment extends Fragment {

    private TrafficViewModel viewModel;
    private TabLayout tabs;
    private View roadworksFilters;
    private ChipGroup durationChips;
    private TextView updatedText;
    private TextView emptyText;
    private RecyclerView recycler;
    private SituationAdapter adapter;

    /** Creates the screen from fragment_list.xml. */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    /** Sets up the tabs, filters and list, and observes the data and the filters. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        tabs = view.findViewById(R.id.tabs);
        roadworksFilters = view.findViewById(R.id.roadworksFilters);
        durationChips = view.findViewById(R.id.durationChips);
        updatedText = view.findViewById(R.id.updatedText);
        emptyText = view.findViewById(R.id.emptyText);
        recycler = view.findViewById(R.id.recycler);

        setUpTabs();
        setUpFilters(view);

        adapter = new SituationAdapter(viewModel::select);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        viewModel.getData().observe(getViewLifecycleOwner(), data -> render());
        viewModel.getListCategory().observe(getViewLifecycleOwner(), category -> render());
        viewModel.getDurationFilter().observe(getViewLifecycleOwner(), band -> render());
        viewModel.getSelectedKey().observe(getViewLifecycleOwner(), adapter::setSelectedKey);
    }

    /** Adds the Incidents and Roadworks tabs. */
    private void setUpTabs() {
        tabs.addTab(tabs.newTab().setTag(SituationCategory.INCIDENT));
        tabs.addTab(tabs.newTab().setTag(SituationCategory.ROADWORKS));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            /** Tab chosen: show that category. */
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setListCategory((SituationCategory) tab.getTag());
            }

            /** Nothing to do. */
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            /** Same tab tapped again: scroll to the top. */
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                recycler.smoothScrollToPosition(0);
            }
        });
    }

    /** Colours the legend dots and connects the filter chips to the ViewModel. */
    private void setUpFilters(View view) {
        // Coloured dots = legend of the colour coding by length
        dot(view.findViewById(R.id.chipShort), Validity.DurationBand.SHORT);
        dot(view.findViewById(R.id.chipMedium), Validity.DurationBand.MEDIUM);
        dot(view.findViewById(R.id.chipLong), Validity.DurationBand.LONG);

        // Show the current filter value (kept in the ViewModel, so they survive rotation)
        durationChips.check(chipForBand(viewModel.getDurationFilter().getValue()));

        durationChips.setOnCheckedStateChangeListener((group, ids) ->
                viewModel.setDurationFilter(ids.isEmpty() ? null : bandForChip(ids.get(0))));
    }

    /** Gives a chip the colour of its length band. */
    private void dot(Chip chip, Validity.DurationBand band) {
        chip.setChipIconTint(ColorStateList.valueOf(SituationStyle.durationColour(requireContext(), band)));
    }

    /** Chip for a length band. */
    private static int chipForBand(Validity.DurationBand band) {
        if (band == Validity.DurationBand.SHORT) {
            return R.id.chipShort;
        } else if (band == Validity.DurationBand.MEDIUM) {
            return R.id.chipMedium;
        } else if (band == Validity.DurationBand.LONG) {
            return R.id.chipLong;
        }
        return R.id.chipAnyLength;
    }

    /** Length band for a chip (null = all lengths). */
    private static Validity.DurationBand bandForChip(int chipId) {
        if (chipId == R.id.chipShort) {
            return Validity.DurationBand.SHORT;
        } else if (chipId == R.id.chipMedium) {
            return Validity.DurationBand.MEDIUM;
        } else if (chipId == R.id.chipLong) {
            return Validity.DurationBand.LONG;
        }
        return null;
    }

    /** Shows the list for the selected tab and filters, with counts and the update time. */
    private void render() {
        SituationCategory category = viewModel.getListCategory().getValue();
        if (category == null) {
            category = SituationCategory.INCIDENT;
        }
        TabLayout.Tab tab = tabs.getTabAt(category == SituationCategory.INCIDENT ? 0 : 1);
        if (tab != null && !tab.isSelected()) {
            tab.select();
        }
        boolean roadworks = category == SituationCategory.ROADWORKS;
        roadworksFilters.setVisibility(roadworks ? View.VISIBLE : View.GONE);

        TrafficData data = viewModel.getData().getValue();
        if (data == null) {
            setTabCounts(0, 0);
            adapter.submitList(null);
            updatedText.setVisibility(View.GONE);
            showEmpty(getString(R.string.list_loading));
            return;
        }
        setTabCounts(data.getIncidents().getSituations().size(), data.getRoadworks().getSituations().size());

        FeedState feed = data.getFeed(category);
        List<Situation> all = feed.getSituations();
        List<Situation> shown = all;
        if (roadworks) {
            shown = TrafficViewModel.filterRoadworks(all, viewModel.getDurationFilter().getValue());
        }
        adapter.submitList(shown);

        // "Updated 19:45:12 · feed published 31 Aug 2026, 09:58 · 151 of 405 shown"
        long updated = data.getCheckedAtMillis() > 0 ? data.getCheckedAtMillis() : feed.getSavedAtMillis();
        String line = getString(R.string.updated_line, DisplayFormat.time(updated),
                DisplayFormat.dateTime(feed.getPublicationTime()));
        if (shown.size() != all.size()) {
            line += " · " + getString(R.string.shown_count, shown.size(), all.size());
        }
        updatedText.setText(line);
        updatedText.setVisibility(View.VISIBLE);

        if (shown.isEmpty()) {
            showEmpty(feed.hasError() && all.isEmpty() ? feed.getErrorMessage() : getString(R.string.list_empty));
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    /** Tab titles with the number of situations. */
    private void setTabCounts(int incidents, int roadworks) {
        TabLayout.Tab first = tabs.getTabAt(0);
        TabLayout.Tab second = tabs.getTabAt(1);
        if (first != null) {
            first.setText(getString(R.string.tab_incidents, incidents));
        }
        if (second != null) {
            second.setText(getString(R.string.tab_roadworks, roadworks));
        }
    }

    /** Shows a message instead of the list. */
    private void showEmpty(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
    }
}
