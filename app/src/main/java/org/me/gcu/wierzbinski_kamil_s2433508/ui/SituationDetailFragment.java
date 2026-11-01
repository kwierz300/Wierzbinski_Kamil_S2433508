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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.wierzbinski_kamil_s2433508.R;
import org.me.gcu.wierzbinski_kamil_s2433508.data.TextBreakdown;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Impact;
import org.me.gcu.wierzbinski_kamil_s2433508.model.LocationPoint;
import org.me.gcu.wierzbinski_kamil_s2433508.model.RoadLocation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Validity;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Shows everything the feed says about the selected situation:
 * description, when (validity), where (road, direction, from/to points),
 * impact on traffic and other information. Only values that exist are shown.
 */
public class SituationDetailFragment extends Fragment {

    private TextView emptyHint;
    private ScrollView detailScroll;
    private ImageView icon;
    private TextView titleText;
    private TextView subtitleText;
    private TextView descriptionText;
    private TextView durationBadge;
    private LinearLayout sections;
    private String shownKey;
    private TrafficViewModel viewModel;
    private Situation shown;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        emptyHint = view.findViewById(R.id.emptyHint);
        detailScroll = view.findViewById(R.id.detailScroll);
        icon = view.findViewById(R.id.detailIcon);
        titleText = view.findViewById(R.id.titleText);
        subtitleText = view.findViewById(R.id.subtitleText);
        descriptionText = view.findViewById(R.id.descriptionText);
        durationBadge = view.findViewById(R.id.durationBadge);
        sections = view.findViewById(R.id.sections);

        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        view.findViewById(R.id.showOnMapButton).setOnClickListener(v -> {
            Situation s = shown;
            if (s == null) {
                return;
            }
            // The selection is kept: portrait shows the details below the map,
            // landscape keeps them in the right pane
            viewModel.showOnMap(s);
        });
        viewModel.getSelectedSituation().observe(getViewLifecycleOwner(), this::show);
    }

    private void show(Situation s) {
        shown = s;
        if (s == null) {
            shownKey = null;
            emptyHint.setVisibility(View.VISIBLE);
            detailScroll.setVisibility(View.GONE);
            return;
        }
        emptyHint.setVisibility(View.GONE);
        detailScroll.setVisibility(View.VISIBLE);
        if (!s.getKey().equals(shownKey)) {
            detailScroll.scrollTo(0, 0);   // new situation: start at the top
        }
        shownKey = s.getKey();

        SituationStyle.applyIcon(icon, s);
        titleText.setText(SituationStyle.title(s));
        String road = SituationStyle.roadLine(s);
        subtitleText.setText(road.isEmpty() ? s.getCategory().getLabel() : s.getCategory().getLabel() + " · " + road);
        descriptionText.setText(s.getDescription());

        Validity validity = s.getValidity();
        if (s.getCategory() == SituationCategory.ROADWORKS) {
            durationBadge.setVisibility(View.VISIBLE);
            durationBadge.setText(SituationStyle.durationLabel(validity.getDurationBand()) + " roadworks · "
                    + DisplayFormat.duration(validity.getDuration()));
            durationBadge.setBackgroundTintList(ColorStateList.valueOf(
                    SituationStyle.durationColour(requireContext(), validity.getDurationBand())));
        } else {
            durationBadge.setVisibility(View.GONE);
        }

        sections.removeAllViews();
        addWhenSection(s);
        addWhereSection(s);
        addImpactSection(s);
        addMoreSection(s);
    }

    private void addWhenSection(Situation s) {
        Validity v = s.getValidity();
        addHeader("When");
        addRow("Status", SituationStyle.timingText(s, OffsetDateTime.now()));
        addRow("Start", DisplayFormat.dateTime(v.getStartTime()));
        addRow("End", v.getEndTime() == null ? "Not known yet" : DisplayFormat.dateTime(v.getEndTime()));
        if (v.getDuration() != null) {
            addRow("Planned length", DisplayFormat.duration(v.getDuration()));
        }
    }

    private void addWhereSection(Situation s) {
        RoadLocation location = s.getLocation();
        addHeader("Where");
        addRow("Road", s.getRoadName());
        addRow("Direction", TextBreakdown.humanise(location.getDirection()));
        for (LocationPoint p : location.getPoints()) {
            String label = LocationPoint.ROLE_FROM.equals(p.getRole()) ? "From"
                    : LocationPoint.ROLE_TO.equals(p.getRole()) ? "To" : "Location";
            StringBuilder value = new StringBuilder();
            if (p.getPlaceName() != null) {
                value.append(p.getPlaceName());
            }
            String junction = p.getDescriptor("junctionName");
            if (junction != null && (p.getPlaceName() == null || !p.getPlaceName().contains(junction))) {
                value.append(" (junction ").append(junction).append(')');
            }
            if (p.hasCoordinates()) {
                if (value.length() > 0) {
                    value.append('\n');
                }
                value.append(DisplayFormat.coordinates(p.getLatitude(), p.getLongitude()));
            }
            addRow(label, value.toString());
        }
    }

    private void addImpactSection(Situation s) {
        Impact impact = s.getImpact();
        boolean hasWorks = s.getWorksSummary() != null;
        if (impact.isEmpty() && !hasWorks) {
            return;
        }
        addHeader("Impact");
        if (hasWorks) {
            addRow("Works", s.getWorksSummary());
        }
        if (impact.getOperationalLanes() != null) {
            addRow("Lanes open", impact.getOperationalLanes() == 0
                    ? "None (closed)" : String.valueOf(impact.getOperationalLanes()));
        }
        if (impact.getLanesRestricted() != null) {
            addRow("Lanes restricted", String.valueOf(impact.getLanesRestricted()));
        }
        addRow("Delays", TextBreakdown.humanise(impact.getDelaysType()));
        if (impact.getDelaySeconds() != null) {
            addRow("Expected delay", (impact.getDelaySeconds() / 60) + " min");
        }
        for (Map.Entry<String, String> e : impact.getOtherValues().entrySet()) {
            addRow(TextBreakdown.humanise(e.getKey()), TextBreakdown.humanise(e.getValue()));
        }
    }

    private void addMoreSection(Situation s) {
        addHeader("More information");
        addRow("Type", TextBreakdown.humanise(s.getRecordType()));
        addRow("Details", TextBreakdown.humanise(s.getSubType()));
        addRow("Probability", TextBreakdown.humanise(s.getProbability()));
        addRow("Compliance", TextBreakdown.humanise(s.getComplianceOption()));
        if (s.getUrgent() != null) {
            addRow("Urgent", s.getUrgent() ? "Yes" : "No");
        }
        for (Map.Entry<String, String> e : s.getOtherDetails().entrySet()) {
            addRow(TextBreakdown.humanise(e.getKey()), e.getValue());
        }
        addRow("Created", DisplayFormat.dateTime(s.getCreationTime()));
        addRow("Last changed", DisplayFormat.dateTime(s.getVersionTime()));
        addRow("Source", s.getSourceName());
        addRow("Source type", TextBreakdown.humanise(s.getSourceType()));
        addRow("Information status", TextBreakdown.humanise(s.getInformationStatus()));
        addRow("Confidentiality", TextBreakdown.humanise(s.getConfidentiality()));
        addRow("Reference", s.getRecordId()
                + (s.getRecordVersion() == null ? "" : " (version " + s.getRecordVersion() + ")"));
    }

    private void addHeader(String text) {
        TextView header = new TextView(requireContext());
        header.setText(text);
        header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        int top = Math.round(getResources().getDisplayMetrics().density * 20);
        int bottom = Math.round(getResources().getDisplayMetrics().density * 4);
        header.setPadding(0, top, 0, bottom);
        sections.addView(header);
    }

    /** Adds "label: value"; does nothing when the value is missing. */
    private void addRow(String label, String value) {
        if (value == null || value.isEmpty() || "-".equals(value)) {
            return;
        }
        View row = getLayoutInflater().inflate(R.layout.item_detail_row, sections, false);
        ((TextView) row.findViewById(R.id.label)).setText(label);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        sections.addView(row);
    }
}
