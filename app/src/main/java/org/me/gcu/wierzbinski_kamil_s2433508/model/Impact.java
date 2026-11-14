//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The effect of a situation on traffic ("impact" in the feed).
 * Fields that are not known by the app are kept in {@link #getOtherValues()},
 * so new data in the feed is still shown and never causes a crash.
 */
public class Impact {

    private Integer operationalLanes;   // numberOfOperationalLanes
    private Integer lanesRestricted;    // numberOfLanesRestricted
    private String delaysType;          // delays, longDelays, veryLongDelays, ...
    private Integer delaySeconds;       // delayTimeValue (not always present)
    private final Map<String, String> otherValues = new LinkedHashMap<>();

    /** Number of lanes still open (numberOfOperationalLanes), or null if not given. */
    public Integer getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(Integer operationalLanes) {
        this.operationalLanes = operationalLanes;
    }

    /** Number of restricted lanes (numberOfLanesRestricted), or null if not given. */
    public Integer getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(Integer lanesRestricted) {
        this.lanesRestricted = lanesRestricted;
    }

    /** Delay level from the feed (delays, longDelays, ...), or null. */
    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType;
    }

    /** Expected delay in seconds (delayTimeValue), or null. */
    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    /** Keeps an impact value the app does not know. */
    public void putOtherValue(String name, String value) {
        otherValues.put(name, value);
    }

    /** Impact values the app does not know (read-only). */
    public Map<String, String> getOtherValues() {
        return Collections.unmodifiableMap(otherValues);
    }

    /** True when the feed gave no impact values at all. */
    public boolean isEmpty() {
        return operationalLanes == null && lanesRestricted == null && delaysType == null
                && delaySeconds == null && otherValues.isEmpty();
    }
}
