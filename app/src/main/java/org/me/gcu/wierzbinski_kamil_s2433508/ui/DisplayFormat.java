//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.ui;

import org.me.gcu.wierzbinski_kamil_s2433508.model.FeedState;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Turns data values into short texts for the screen (UK date format, local time).
 */
public final class DisplayFormat {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.UK);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.UK);

    private DisplayFormat() {
    }

    /** "31 Aug 2026, 09:58" in the phone's time zone, or "-" if missing. */
    public static String dateTime(OffsetDateTime time) {
        if (time == null) {
            return "-";
        }
        return time.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_TIME);
    }

    /** Same as {@link #dateTime(OffsetDateTime)} for a time in milliseconds. */
    public static String dateTime(long millis) {
        if (millis <= 0) {
            return "-";
        }
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE_TIME);
    }

    /** "14:05:32" for a time in milliseconds. */
    public static String time(long millis) {
        if (millis <= 0) {
            return "-";
        }
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(TIME);
    }

    public static String source(FeedState.Source source) {
        switch (source) {
            case LIVE:
                return "Live data";
            case CACHE:
                return "Saved data";
            case SAMPLE:
                return "Example data";
            default:
                return "No data";
        }
    }
}
