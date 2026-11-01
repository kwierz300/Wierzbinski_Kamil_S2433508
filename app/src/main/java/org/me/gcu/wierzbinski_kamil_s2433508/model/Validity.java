//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.model;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * When a situation is valid ("validity" in the feed).
 * Unplanned events usually only have a start time; roadworks have start and end.
 */
public class Validity {

    /** Roadworks up to this many days are "short". */
    public static final long SHORT_MAX_DAYS = 7;
    /** Roadworks up to this many days are "medium"; longer ones are "long". */
    public static final long MEDIUM_MAX_DAYS = 90;

    /** Duration band used for the colour coding of roadworks. */
    public enum DurationBand { SHORT, MEDIUM, LONG, UNKNOWN }

    private String status;              // e.g. definedByValidityTimeSpec
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    /** Planned length of the situation, or null when start or end is missing. */
    public Duration getDuration() {
        if (startTime == null || endTime == null) {
            return null;
        }
        return Duration.between(startTime, endTime);
    }

    public DurationBand getDurationBand() {
        Duration d = getDuration();
        if (d == null || d.isNegative()) {
            return DurationBand.UNKNOWN;
        }
        long days = d.toDays();
        if (days <= SHORT_MAX_DAYS) {
            return DurationBand.SHORT;
        }
        if (days <= MEDIUM_MAX_DAYS) {
            return DurationBand.MEDIUM;
        }
        return DurationBand.LONG;
    }

    /** True if the situation is valid at the given moment. */
    public boolean isActiveAt(OffsetDateTime moment) {
        if (startTime != null && moment.isBefore(startTime)) {
            return false;
        }
        return endTime == null || !moment.isAfter(endTime);
    }
}
