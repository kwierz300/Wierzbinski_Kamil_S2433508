//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.data;

import org.me.gcu.wierzbinski_kamil_s2433508.model.Impact;
import org.me.gcu.wierzbinski_kamil_s2433508.model.LocationPoint;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Breaks the text values of a parsed situation into smaller parts, using
 * standard String methods and regular expressions:
 * - road name (from the location data, or from the comment as a fallback),
 * - the parts of a roadworks comment ("A9 Lothmore SB - TTL"),
 * - a readable summary of the works (abbreviations expanded),
 * - the detailed kind of situation (used to choose an icon).
 */
public final class TextBreakdown {

    /** Matches UK road numbers such as A9, M74, A74(M), A823(M), B957. */
    private static final Pattern ROAD_PATTERN =
            Pattern.compile("\\b([ABM]\\d{1,4})(\\(M\\)|M)?(?![\\w])", Pattern.CASE_INSENSITIVE);

    private TextBreakdown() {
    }

    /** Fills in all values worked out from the raw data. Called by the parser for each record. */
    public static void enrich(Situation s) {
        String road = findRoadName(s);
        s.setRoadName(road);
        s.setRoadKey(normaliseRoad(road));
        s.addRoadKey(normaliseRoad(road));
        for (String mentioned : findAllRoadsInText(s.getDescription())) {
            s.addRoadKey(normaliseRoad(mentioned));
        }
        s.setDescriptionParts(splitDescription(s.getDescription()));
        if (s.getCategory() == SituationCategory.ROADWORKS) {
            s.setKind(classifyRoadworks(s.getDescription()));
            s.setWorksSummary(worksSummary(s));
        } else {
            s.setKind(classifyIncident(s));
        }
    }

    // ------------------------------------------------------------------ road names

    /** Road number from the location data; if missing, the first road number in the comment. */
    static String findRoadName(Situation s) {
        for (LocationPoint p : s.getLocation().getPoints()) {
            // The feed sometimes joins the road with a place name ("M8MONKLANDS"),
            // so only a value that looks like a road number is used
            String road = findRoadInText(p.getRoadNumber());
            if (road != null) {
                return road;
            }
        }
        return findRoadInText(s.getDescription());
    }

    /** First road number found in free text, e.g. "M8 WB Woodside" gives "M8". Null if none. */
    public static String findRoadInText(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = ROAD_PATTERN.matcher(text);
        if (m.find()) {
            String suffix = m.group(2) == null ? "" : "(M)";
            return m.group(1).toUpperCase(Locale.UK) + suffix;
        }
        return null;
    }

    /** Every road number found in free text, without duplicates, in order of appearance. */
    public static List<String> findAllRoadsInText(String text) {
        List<String> roads = new ArrayList<>();
        if (text == null) {
            return roads;
        }
        Matcher m = ROAD_PATTERN.matcher(text);
        while (m.find()) {
            String suffix = m.group(2) == null ? "" : "(M)";
            String road = m.group(1).toUpperCase(Locale.UK) + suffix;
            if (!roads.contains(road)) {
                roads.add(road);
            }
        }
        return roads;
    }

    /**
     * Normalised road name used for searching: upper case, letters and digits only.
     * "A74(M)", "a74m" and "A74 (M)" all become "A74M".
     */
    public static String normaliseRoad(String road) {
        if (road == null) {
            return null;
        }
        String key = road.toUpperCase(Locale.UK).replaceAll("[^A-Z0-9]", "");
        return key.isEmpty() ? null : key;
    }

    // ------------------------------------------------------------------ comment text

    /** Splits a comment like "A1 - SB - Old Craighall to Lamberton - Mobile Lane closure" into parts. */
    static List<String> splitDescription(String description) {
        List<String> parts = new ArrayList<>();
        if (description == null) {
            return parts;
        }
        for (String part : description.split("\\s+-\\s+")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    /** Readable summary of what the roadworks are, e.g. "TTL" becomes "Temporary traffic lights". */
    static String worksSummary(Situation s) {
        List<String> parts = s.getDescriptionParts();
        if (parts.size() > 1) {
            return expandAbbreviations(parts.get(parts.size() - 1));
        }
        return s.getKind().getLabel();
    }

    /** Expands short forms used by Traffic Scotland operators. */
    public static String expandAbbreviations(String text) {
        if (text == null) {
            return null;
        }
        String result = text;
        result = result.replaceAll("(?i)\\bTTL(?:'?s)?\\b", "Temporary traffic lights");
        result = result.replaceAll("(?i)\\bMLC(?:'?s)?\\b", "Mobile lane closure");
        result = result.replaceAll("(?i)\\bTemp\\b", "Temporary");
        result = result.replaceAll("(?i)\\bm?VMS\\b", "Mobile message sign");
        result = result.replaceAll("(?i)\\bN/?B\\b", "northbound");
        result = result.replaceAll("(?i)\\bS/?B\\b", "southbound");
        result = result.replaceAll("(?i)\\bE/?B\\b", "eastbound");
        result = result.replaceAll("(?i)\\bW/?B\\b", "westbound");
        result = result.replaceAll("(?i)\\bRbt\\b", "Roundabout");
        result = result.replaceAll("(?i)\\bJct\\b", "Junction");
        result = result.replaceAll("(?i)\\bRd\\b", "Road");
        return result;
    }

    /** "southBound" becomes "Southbound"; "roadClosed" becomes "Road closed". */
    public static String humanise(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        if (camelCase.endsWith("Bound")) {
            String direction = camelCase.substring(0, camelCase.length() - "Bound".length());
            return capitalise(direction.toLowerCase(Locale.UK)) + "bound";
        }
        String spaced = camelCase.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.UK);
        return capitalise(spaced);
    }

    private static String capitalise(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(Locale.UK) + text.substring(1);
    }

    // ------------------------------------------------------------------ classification

    /** Kind of roadworks, worked out from the words in the comment. */
    static SituationKind classifyRoadworks(String description) {
        String text = description == null ? "" : description.toLowerCase(Locale.UK);
        if (text.contains("diversion")) {
            return SituationKind.ROADWORKS_DIVERSION;
        }
        if (text.contains("lane clos") || text.contains("lane restriction")
                || text.matches("(?s).*\\bmlc.*")
                || text.contains("hard shoulder") || text.contains("contraflow")
                || text.contains("narrow lane")) {
            return SituationKind.ROADWORKS_LANE_CLOSURE;
        }
        if (text.matches("(?s).*\\bttls?\\b.*") || text.contains("traffic light")
                || text.contains("signals") || text.contains("convoy")
                || text.contains("temp lights") || text.contains("stop and go")) {
            return SituationKind.ROADWORKS_TRAFFIC_LIGHTS;
        }
        if (text.contains("closure") || text.contains("closed")) {
            return SituationKind.ROADWORKS_CLOSURE;
        }
        return SituationKind.ROADWORKS;
    }

    /** Kind of unplanned event, from the record type, sub-type, impact and comment. */
    static SituationKind classifyIncident(Situation s) {
        String type = s.getRecordType() == null ? "" : s.getRecordType();
        String sub = s.getSubType() == null ? "" : s.getSubType().toLowerCase(Locale.UK);
        String text = s.getDescription().toLowerCase(Locale.UK);
        Impact impact = s.getImpact();

        if (type.equals("Accident") || text.contains("accident") || text.contains("collision")
                || text.contains("crash")) {
            return SituationKind.ACCIDENT;
        }
        if (type.endsWith("Obstruction") || text.contains("broken down") || text.contains("breakdown")
                || text.contains("vehicle fire") || text.contains("obstruction")) {
            return SituationKind.VEHICLE_OBSTRUCTION;
        }
        if (type.equals("PoorEnvironmentConditions") || type.equals("WeatherRelatedRoadConditions")
                || text.contains("flood") || text.contains("snow") || text.contains(" ice")
                || text.contains("high wind") || text.contains("fog")) {
            return SituationKind.WEATHER;
        }
        if (type.equals("AbnormalTraffic") || text.contains("queu") || text.contains("congestion")) {
            return SituationKind.QUEUING_TRAFFIC;
        }
        if (sub.contains("closed") || sub.contains("closure") && !sub.contains("lane")
                || (impact.getOperationalLanes() != null && impact.getOperationalLanes() == 0)
                || text.contains("closed")) {
            return SituationKind.ROAD_CLOSED;
        }
        if (sub.contains("lane") || sub.contains("contraflow")
                || (impact.getLanesRestricted() != null && impact.getLanesRestricted() > 0)
                || text.contains("lane")) {
            return SituationKind.LANE_RESTRICTION;
        }
        return SituationKind.OTHER_INCIDENT;
    }
}
