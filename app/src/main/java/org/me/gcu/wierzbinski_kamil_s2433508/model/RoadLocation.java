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
 * The location of a situation ("groupOfLocations" in the feed).
 * A linear location has two points (from and to) that define a stretch of road.
 * Other location types may have only one point.
 */
public class RoadLocation {

    private String locationType;      // xsi:type of groupOfLocations, e.g. "Linear"
    private String linearType;        // e.g. "segment"
    private String direction;         // e.g. "southBound"
    private final List<LocationPoint> points = new ArrayList<>();

    /** xsi:type of groupOfLocations, e.g. Linear. */
    public String getLocationType() {
        return locationType;
    }

    /** Sets the value returned by getLocationType(). */
    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    /** Type of linear location, e.g. segment. */
    public String getLinearType() {
        return linearType;
    }

    /** Sets the value returned by getLinearType(). */
    public void setLinearType(String linearType) {
        this.linearType = linearType;
    }

    /** Direction from the feed, e.g. southBound. */
    public String getDirection() {
        return direction;
    }

    /** Sets the value returned by getDirection(). */
    public void setDirection(String direction) {
        this.direction = direction;
    }

    /** Adds a point (null is ignored). */
    public void addPoint(LocationPoint point) {
        if (point != null) {
            points.add(point);
        }
    }

    /** All points (read-only). */
    public List<LocationPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    /** The "from" point, or null. */
    public LocationPoint getFrom() {
        return findByRole(LocationPoint.ROLE_FROM);
    }

    /** The "to" point, or null. */
    public LocationPoint getTo() {
        return findByRole(LocationPoint.ROLE_TO);
    }

    /** First point with the given role, or null. */
    private LocationPoint findByRole(String role) {
        for (LocationPoint p : points) {
            if (role.equals(p.getRole())) {
                return p;
            }
        }
        return null;
    }

    /** True when the location is a stretch of road with two points that have coordinates. */
    public boolean isLine() {
        LocationPoint from = getFrom();
        LocationPoint to = getTo();
        return from != null && to != null && from.hasCoordinates() && to.hasCoordinates();
    }

    /** First point with coordinates, used for a single map marker. Null if none. */
    public LocationPoint getMainPoint() {
        LocationPoint from = getFrom();
        if (from != null && from.hasCoordinates()) {
            return from;
        }
        for (LocationPoint p : points) {
            if (p.hasCoordinates()) {
                return p;
            }
        }
        return null;
    }

    /** Shortest distance in km from the given coordinates to any point of this location. */
    public double distanceKmTo(double lat, double lon) {
        double best = Double.MAX_VALUE;
        for (LocationPoint p : points) {
            best = Math.min(best, p.distanceKmTo(lat, lon));
        }
        return best;
    }
}
