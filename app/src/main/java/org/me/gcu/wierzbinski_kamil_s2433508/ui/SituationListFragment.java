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

import com.google.android.material.tabs.TabLayout;

import org.me.gcu.wierzbinski_kamil_s2433508.R;
import org.me.gcu.wierzbinski_kamil_s2433508.model.FeedState;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Validity;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel;

/**
 * Screen with the list of all current situations, in two tabs:
 * incidents (unplanned events) and roadworks. Tapping an item selects it,
 * and the details are shown by {@link SituationDetailFragment}.
 */
public class SituationListFragment extends Fragment {

    private TrafficViewModel viewModel;
    private TabLayout tabs;
    private View legend;
    private TextView emptyText;
    private SituationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        tabs = view.findViewById(R.id.tabs);
        legend = view.findViewById(R.id.legend);
        emptyText = view.findViewById(R.id.emptyText);
        RecyclerView recycler = view.findViewById(R.id.recycler);

        setUpLegend(view);

        tabs.addTab(tabs.newTab().setTag(SituationCategory.INCIDENT));
        tabs.addTab(tabs.newTab().setTag(SituationCategory.ROADWORKS));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setListCategory((SituationCategory) tab.getTag());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                recycler.smoothScrollToPosition(0);
            }
        });

        adapter = new SituationAdapter(viewModel::select);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        viewModel.getData().observe(getViewLifecycleOwner(), data -> render());
        viewModel.getListCategory().observe(getViewLifecycleOwner(), category -> render());
        viewModel.getSelectedKey().observe(getViewLifecycleOwner(), adapter::setSelectedKey);
    }

    private void setUpLegend(View view) {
        tint(view.findViewById(R.id.legendShort), Validity.DurationBand.SHORT);
        tint(view.findViewById(R.id.legendMedium), Validity.DurationBand.MEDIUM);
        tint(view.findViewById(R.id.legendLong), Validity.DurationBand.LONG);
    }

    private void tint(View badge, Validity.DurationBand band) {
        badge.setBackgroundTintList(ColorStateList.valueOf(SituationStyle.durationColour(requireContext(), band)));
    }

    /** Shows the list for the selected tab, with counts in the tab titles. */
    private void render() {
        SituationCategory category = viewModel.getListCategory().getValue();
        if (category == null) {
            category = SituationCategory.INCIDENT;
        }
        TabLayout.Tab tab = tabs.getTabAt(category == SituationCategory.INCIDENT ? 0 : 1);
        if (tab != null && !tab.isSelected()) {
            tab.select();
        }
        legend.setVisibility(category == SituationCategory.ROADWORKS ? View.VISIBLE : View.GONE);

        TrafficData data = viewModel.getData().getValue();
        if (data == null) {
            setTabCounts(0, 0);
            adapter.submitList(null);
            showEmpty(getString(R.string.list_loading));
            return;
        }
        setTabCounts(data.getIncidents().getSituations().size(), data.getRoadworks().getSituations().size());

        FeedState feed = data.getFeed(category);
        adapter.submitList(feed.getSituations());
        if (feed.getSituations().isEmpty()) {
            showEmpty(feed.hasError() ? feed.getErrorMessage() : getString(R.string.list_empty));
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

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

    private void showEmpty(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
    }
}
