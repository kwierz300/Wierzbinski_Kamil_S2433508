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
 * One point of a situation location ("from", "to", or a single point).
 * Holds the coordinates and all the names given for the point in the feed
 * (road number, junction name, link name, ...), keyed by descriptor type.
 */
public class LocationPoint {

    public static final String ROLE_FROM = "from";
    public static final String ROLE_TO = "to";
    public static final String ROLE_POINT = "point";

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final String role;
    private final String pointType;          // e.g. TpegJunction, TpegNonJunctionPoint
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    // descriptor type (e.g. tpegIlcName1, junctionName, linkName) -> value
    private final Map<String, String> descriptors = new LinkedHashMap<>();

    public LocationPoint(String role, String pointType) {
        this.role = role;
        this.pointType = pointType;
    }

    public String getRole() {
        return role;
    }

    public String getPointType() {
        return pointType;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    /** Adds a name for this point. If the same type appears twice, the first one is kept. */
    public void addDescriptor(String type, String value) {
        if (type == null || value == null || value.isEmpty()) {
            return;
        }
        if (!descriptors.containsKey(type)) {
            descriptors.put(type, value);
        }
    }

    public String getDescriptor(String type) {
        return descriptors.get(type);
    }

    public Map<String, String> getDescriptors() {
        return Collections.unmodifiableMap(descriptors);
    }

    /** Main road number of this point, e.g. "A74(M)" or "M8", or null if not given. */
    public String getRoadNumber() {
        String road = descriptors.get("tpegIlcName1");   // unplanned events
        if (road == null) {
            road = descriptors.get("linkName");          // roadworks
        }
        return road;
    }

    /** The most useful human readable place name for this point. */
    public String getPlaceName() {
        String[] preferred = {"intersectionName", "nonLinkedPointName", "junctionName", "tpegIlcName2"};
        for (String key : preferred) {
            String value = descriptors.get(key);
            if (value != null) {
                return value;
            }
        }
        return getRoadNumber();
    }

    /** Great circle (haversine) distance in kilometres from this point to the given coordinates. */
    public double distanceKmTo(double lat, double lon) {
        if (!hasCoordinates()) {
            return Double.MAX_VALUE;
        }
        double dLat = Math.toRadians(lat - latitude);
        double dLon = Math.toRadians(lon - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
