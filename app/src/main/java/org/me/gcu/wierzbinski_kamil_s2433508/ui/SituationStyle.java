//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import org.me.gcu.wierzbinski_kamil_s2433508.R;
import org.me.gcu.wierzbinski_kamil_s2433508.data.TextBreakdown;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationKind;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Validity;

/**
 * How a situation looks on screen: its icon, colours and short texts.
 * Used by the list, the details screen and (later) the map, so they always match.
 */
public final class SituationStyle {

    private SituationStyle() {
    }

    /** A different icon for each kind of situation (Font Awesome Free, CC BY 4.0). */
    @DrawableRes
    public static int iconFor(SituationKind kind) {
        if (kind == null) {
            return R.drawable.ic_kind_other_incident;
        }
        switch (kind) {
            case ROAD_CLOSED:
                return R.drawable.ic_kind_road_closed;
            case LANE_RESTRICTION:
                return R.drawable.ic_kind_lane_restriction;
            case QUEUING_TRAFFIC:
                return R.drawable.ic_kind_queuing_traffic;
            case ACCIDENT:
                return R.drawable.ic_kind_accident;
            case VEHICLE_OBSTRUCTION:
                return R.drawable.ic_kind_vehicle_obstruction;
            case WEATHER:
                return R.drawable.ic_kind_weather;
            case ROADWORKS_CLOSURE:
                return R.drawable.ic_kind_roadworks_closure;
            case ROADWORKS_LANE_CLOSURE:
                return R.drawable.ic_kind_roadworks_lane_closure;
            case ROADWORKS_TRAFFIC_LIGHTS:
                return R.drawable.ic_kind_roadworks_traffic_lights;
            case ROADWORKS_DIVERSION:
                return R.drawable.ic_kind_roadworks_diversion;
            case ROADWORKS:
                return R.drawable.ic_kind_roadworks;
            default:
                return R.drawable.ic_kind_other_incident;
        }
    }

    @ColorInt
    public static int categoryColour(Context context, SituationCategory category) {
        return ContextCompat.getColor(context, category == SituationCategory.ROADWORKS
                ? R.color.category_roadworks : R.color.category_incident);
    }

    /** Colour coding of roadworks by planned duration. */
    @ColorInt
    public static int durationColour(Context context, Validity.DurationBand band) {
        int res;
        switch (band) {
            case SHORT:
                res = R.color.duration_short;
                break;
            case MEDIUM:
                res = R.color.duration_medium;
                break;
            case LONG:
                res = R.color.duration_long;
                break;
            default:
                res = R.color.duration_unknown;
                break;
        }
        return ContextCompat.getColor(context, res);
    }

    public static String durationLabel(Validity.DurationBand band) {
        switch (band) {
            case SHORT:
                return "Short";
            case MEDIUM:
                return "Medium";
            case LONG:
                return "Long";
            default:
                return "Unknown length";
        }
    }

    /** "Long · 1 year 2 months" for the list badge. */
    public static String durationBadge(Situation s) {
        Validity v = s.getValidity();
        return durationLabel(v.getDurationBand()) + " · " + DisplayFormat.duration(v.getDuration());
    }

    /** Shows the white icon on a round background in the category colour. */
    public static void applyIcon(ImageView view, Situation s) {
        Context context = view.getContext();
        view.setImageResource(iconFor(s.getKind()));
        view.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white)));
        view.setBackgroundTintList(ColorStateList.valueOf(categoryColour(context, s.getCategory())));
        view.setContentDescription(s.getKind() == null ? null : s.getKind().getLabel());
    }

    /** Title of a situation: its kind ("Road closed", "Roadworks - traffic lights"). */
    public static String title(Situation s) {
        return s.getKind() == null ? s.getCategory().getLabel() : s.getKind().getLabel();
    }

    /** "A74(M) · Southbound" (road and direction, whichever are known). */
    public static String roadLine(Situation s) {
        String road = s.getRoadName();
        String direction = TextBreakdown.humanise(s.getLocation().getDirection());
        if (road != null && direction != null) {
            return road + " · " + direction;
        }
        return road != null ? road : (direction != null ? direction : "");
    }
}
