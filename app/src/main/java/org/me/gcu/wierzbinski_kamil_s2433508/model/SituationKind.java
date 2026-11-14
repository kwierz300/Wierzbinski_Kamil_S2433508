//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.model;

/**
 * A more detailed type of situation. It is worked out from the record type
 * (xsi:type), the sub-type element and the words in the public comment.
 * Each kind has its own icon in the user interface.
 */
public enum SituationKind {
    // Unplanned events (incidents)
    ROAD_CLOSED("Road closed"),
    LANE_RESTRICTION("Lane restriction"),
    QUEUING_TRAFFIC("Queuing traffic"),
    ACCIDENT("Accident"),
    OBSTRUCTION("Obstruction"),
    WEATHER("Weather conditions"),
    OTHER_INCIDENT("Incident"),
    // Roadworks
    ROADWORKS_CLOSURE("Roadworks - closure"),
    ROADWORKS_LANE_CLOSURE("Roadworks - lane closure"),
    ROADWORKS_TRAFFIC_LIGHTS("Roadworks - traffic lights"),
    ROADWORKS_DIVERSION("Roadworks - diversion"),
    ROADWORKS("Roadworks");

    private final String label;

    /** Each kind has a label for the screen. */
    SituationKind(String label) {
        this.label = label;
    }

    /** Name shown on screen, e.g. "Road closed". */
    public String getLabel() {
        return label;
    }
}
