//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Everything the app knows at one moment: the state of both feeds.
 * A new object is created after each refresh (it is never changed).
 */
public final class TrafficData {

    private final FeedState incidents;
    private final FeedState roadworks;
    private final long checkedAtMillis;

    public TrafficData(FeedState incidents, FeedState roadworks, long checkedAtMillis) {
        this.incidents = incidents;
        this.roadworks = roadworks;
        this.checkedAtMillis = checkedAtMillis;
    }

    public FeedState getIncidents() {
        return incidents;
    }

    public FeedState getRoadworks() {
        return roadworks;
    }

    public FeedState getFeed(SituationCategory category) {
        return category == SituationCategory.INCIDENT ? incidents : roadworks;
    }

    /** When the app last tried to get new data (milliseconds since 1970). */
    public long getCheckedAtMillis() {
        return checkedAtMillis;
    }

    /** All situations from both feeds, incidents first. */
    public List<Situation> getAll() {
        List<Situation> all = new ArrayList<>(incidents.getSituations());
        all.addAll(roadworks.getSituations());
        return Collections.unmodifiableList(all);
    }

    /** Finds a situation by its key (see {@link Situation#getKey()}), or null. */
    public Situation findByKey(String key) {
        if (key == null) {
            return null;
        }
        for (Situation s : getAll()) {
            if (key.equals(s.getKey())) {
                return s;
            }
        }
        return null;
    }

    /** True when at least one feed shows saved (not fresh) data. */
    public boolean isShowingSavedData() {
        return incidents.getSource() == FeedState.Source.CACHE
                || roadworks.getSource() == FeedState.Source.CACHE;
    }
}
