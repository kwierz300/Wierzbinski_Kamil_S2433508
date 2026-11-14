//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.model;

/**
 * The point used for the search by distance: the phone's approximate location,
 * or a fixed default point when the location is not available.
 */
public final class SearchOrigin {

    /** Default point: Glasgow city centre (George Square). */
    public static final SearchOrigin DEFAULT =
            new SearchOrigin(55.8611, -4.2502, "Glasgow city centre (default)", false);

    private final double latitude;
    private final double longitude;
    private final String label;
    private final boolean fromDevice;

    /** Creates a search point. */
    public SearchOrigin(double latitude, double longitude, String label, boolean fromDevice) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.label = label;
        this.fromDevice = fromDevice;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /** Text shown to the user, e.g. "your location". */
    public String getLabel() {
        return label;
    }

    /** True when the point is the phone's own (coarse) location. */
    public boolean isFromDevice() {
        return fromDevice;
    }
}
