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

    public Integer getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(Integer operationalLanes) {
        this.operationalLanes = operationalLanes;
    }

    public Integer getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(Integer lanesRestricted) {
        this.lanesRestricted = lanesRestricted;
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public void putOtherValue(String name, String value) {
        otherValues.put(name, value);
    }

    public Map<String, String> getOtherValues() {
        return Collections.unmodifiableMap(otherValues);
    }

    public boolean isEmpty() {
        return operationalLanes == null && lanesRestricted == null && delaysType == null
                && delaySeconds == null && otherValues.isEmpty();
    }
}
