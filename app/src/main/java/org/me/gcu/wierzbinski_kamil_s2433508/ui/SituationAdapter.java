//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import org.me.gcu.wierzbinski_kamil_s2433508.R;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;

import java.util.Objects;

/**
 * Shows situations in a RecyclerView: icon, type, road, the generalPublicComment
 * and, for roadworks, a colour-coded duration. The selected item is highlighted.
 */
public class SituationAdapter extends ListAdapter<Situation, SituationAdapter.Holder> {

    /** Called when the user taps a situation. */
    public interface OnSituationClick {
        void onClick(Situation situation);
    }

    private static final DiffUtil.ItemCallback<Situation> DIFF = new DiffUtil.ItemCallback<Situation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Situation a, @NonNull Situation b) {
            return a.getKey().equals(b.getKey());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Situation a, @NonNull Situation b) {
            return Objects.equals(a.getDescription(), b.getDescription())
                    && Objects.equals(a.getVersionTime(), b.getVersionTime());
        }
    };

    private final OnSituationClick listener;
    private String selectedKey;

    public SituationAdapter(OnSituationClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    /** Highlights the selected situation (useful in landscape, where details are shown next to the list). */
    public void setSelectedKey(String key) {
        if (!Objects.equals(key, selectedKey)) {
            selectedKey = key;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), getItem(position).getKey().equals(selectedKey), listener);
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final View durationStrip;
        private final ImageView icon;
        private final TextView typeText;
        private final TextView roadText;
        private final TextView descriptionText;
        private final TextView durationText;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            durationStrip = itemView.findViewById(R.id.durationStrip);
            icon = itemView.findViewById(R.id.icon);
            typeText = itemView.findViewById(R.id.typeText);
            roadText = itemView.findViewById(R.id.roadText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            durationText = itemView.findViewById(R.id.durationText);
        }

        void bind(Situation s, boolean selected, OnSituationClick listener) {
            Context context = itemView.getContext();
            SituationStyle.applyIcon(icon, s);
            typeText.setText(SituationStyle.title(s));
            String road = SituationStyle.roadLine(s);
            roadText.setText(road);
            roadText.setVisibility(road.isEmpty() ? View.GONE : View.VISIBLE);
            descriptionText.setText(s.getDescription());

            if (s.getCategory() == SituationCategory.ROADWORKS) {
                int colour = SituationStyle.durationColour(context, s.getValidity().getDurationBand());
                durationStrip.setBackgroundColor(colour);
                durationText.setVisibility(View.VISIBLE);
                durationText.setText(SituationStyle.durationBadge(s));
                durationText.setBackgroundTintList(ColorStateList.valueOf(colour));
            } else {
                durationStrip.setBackgroundColor(SituationStyle.categoryColour(context, s.getCategory()));
                durationText.setVisibility(View.GONE);
            }

            int strokeColour = selected
                    ? MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary)
                    : MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY);
            card.setStrokeColor(strokeColour);
            card.setStrokeWidth(Math.round(context.getResources().getDisplayMetrics().density * (selected ? 2 : 1)));
            card.setOnClickListener(v -> listener.onClick(s));
            card.setContentDescription(SituationStyle.title(s) + ". " + road + ". " + s.getDescription());
        }
    }
}
