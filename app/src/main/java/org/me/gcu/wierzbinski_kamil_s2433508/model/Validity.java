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

    /** Roadworks up to this many days (about 1 month) are "short". */
    public static final long SHORT_MAX_DAYS = 31;
    /** Roadworks up to this many days (12 months) are "medium"; longer ones are "long". */
    public static final long MEDIUM_MAX_DAYS = 365;

    /** Duration band used for the colour coding of roadworks. */
    public enum DurationBand { SHORT, MEDIUM, LONG, UNKNOWN }

    /** Whether the situation has started yet, is happening now, or is already over. */
    public enum Timing { PLANNED, ACTIVE, ENDED }

    private String status;              // e.g. definedByValidityTimeSpec
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    /** validityStatus, e.g. definedByValidityTimeSpec. */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** overallStartTime, or null. */
    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    /** overallEndTime, or null (incidents usually have no end time). */
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

    /** Short (up to 1 month), medium (up to 12 months), long, or unknown. Used for the colour coding. */
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

    /** Planned (not started yet), active (now) or ended, at the given moment. */
    public Timing getTiming(OffsetDateTime moment) {
        if (startTime != null && moment.isBefore(startTime)) {
            return Timing.PLANNED;
        }
        if (endTime != null && moment.isAfter(endTime)) {
            return Timing.ENDED;
        }
        return Timing.ACTIVE;
    }

    /** True if the situation is valid at the given moment. */
    public boolean isActiveAt(OffsetDateTime moment) {
        if (startTime != null && moment.isBefore(startTime)) {
            return false;
        }
        return endTime == null || !moment.isAfter(endTime);
    }
}
