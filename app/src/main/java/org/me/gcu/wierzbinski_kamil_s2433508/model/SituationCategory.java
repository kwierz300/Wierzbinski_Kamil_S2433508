//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.model;

/**
 * Which of the two Traffic Scotland feeds a situation came from.
 */
public enum SituationCategory {
    INCIDENT("Incident"),
    ROADWORKS("Roadworks");

    private final String label;

    SituationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
