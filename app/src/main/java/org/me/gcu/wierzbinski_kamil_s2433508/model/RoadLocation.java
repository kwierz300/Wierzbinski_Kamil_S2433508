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

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getLinearType() {
        return linearType;
    }

    public void setLinearType(String linearType) {
        this.linearType = linearType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void addPoint(LocationPoint point) {
        if (point != null) {
            points.add(point);
        }
    }

    public List<LocationPoint> getPoints() {
        return Collections.unmodifiableList(points);
    }

    public LocationPoint getFrom() {
        return findByRole(LocationPoint.ROLE_FROM);
    }

    public LocationPoint getTo() {
        return findByRole(LocationPoint.ROLE_TO);
    }

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
