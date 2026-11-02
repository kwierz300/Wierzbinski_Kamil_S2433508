//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.ui;

import org.me.gcu.wierzbinski_kamil_s2433508.model.FeedState;

import java.time.Duration;
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

    /** Only static methods: no objects of this class are created. */
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

    /** "3 days", "2 months 5 days", "1 year 4 months", "5 hours". Null-safe. */
    public static String duration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return "-";
        }
        long days = duration.toDays();
        if (days < 1) {
            long hours = Math.max(1, duration.toHours());
            return plural(hours, "hour");
        }
        long years = days / 365;
        long months = (days % 365) / 30;
        long restDays = (days % 365) % 30;
        if (years > 0) {
            return months > 0 ? plural(years, "year") + " " + plural(months, "month") : plural(years, "year");
        }
        if (months > 0) {
            return restDays > 0 ? plural(months, "month") + " " + plural(restDays, "day") : plural(months, "month");
        }
        return plural(days, "day");
    }

    /** "1 day" or "3 days". */
    private static String plural(long n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    /** "850 m" or "12.3 km". */
    public static String distance(double km) {
        if (km < 1) {
            return Math.round(km * 1000) + " m";
        }
        return String.format(Locale.UK, "%.1f km", km);
    }

    /** Latitude/longitude with 5 decimals (about 1 metre). */
    public static String coordinates(double lat, double lon) {
        return String.format(Locale.UK, "%.5f, %.5f", lat, lon);
    }

    /** Readable name of where the data came from. */
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
