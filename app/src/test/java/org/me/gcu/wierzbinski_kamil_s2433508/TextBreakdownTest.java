//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.me.gcu.wierzbinski_kamil_s2433508.data.TextBreakdown;
import org.me.gcu.wierzbinski_kamil_s2433508.model.LocationPoint;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationKind;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Validity;

import java.time.OffsetDateTime;

/**
 * Local unit tests for the text breakdown and model logic (no Android needed).
 */
public class TextBreakdownTest {

    @Test
    public void roadNamesAreNormalisedForSearch() {
        assertEquals("A74M", TextBreakdown.normaliseRoad("A74(M)"));
        assertEquals("A74M", TextBreakdown.normaliseRoad("a74 m"));
        assertEquals("M8", TextBreakdown.normaliseRoad(" m8 "));
        assertNull(TextBreakdown.normaliseRoad("()"));
    }

    @Test
    public void roadIsFoundInComment() {
        assertEquals("M8", TextBreakdown.findRoadInText("M8 WB Woodside - A804 Phoenix Rd - Closure"));
        assertEquals("A74(M)", TextBreakdown.findRoadInText("The A74M(S) junction 19 off slip is closed"));
        assertEquals(2, TextBreakdown.findAllRoadsInText("M8 WB Woodside - A804 Phoenix Rd").size());
        assertNull(TextBreakdown.findRoadInText("No road here"));
        assertNull(TextBreakdown.findRoadInText("M8MONKLANDS"));   // not a clean road number
    }

    @Test
    public void abbreviationsAreExpanded() {
        assertEquals("A9 Lothmore southbound - Temporary traffic lights",
                TextBreakdown.expandAbbreviations("A9 Lothmore SB - TTL"));
        assertEquals("Mobile lane closure", TextBreakdown.expandAbbreviations("MLC"));
        assertEquals("Southbound", TextBreakdown.humanise("southBound"));
        assertEquals("Road closed", TextBreakdown.humanise("roadClosed"));
    }

    @Test
    public void roadworksAreClassifiedFromComment() {
        assertEquals(SituationKind.ROADWORKS_TRAFFIC_LIGHTS, roadworks("A9 Golspie SB - TTL").getKind());
        assertEquals(SituationKind.ROADWORKS_LANE_CLOSURE, roadworks("M74 NB J21 - J20 - Lane Closure").getKind());
        assertEquals(SituationKind.ROADWORKS_DIVERSION, roadworks("A985 - WB - Kings Rd Rbt - Diversion only").getKind());
        assertEquals(SituationKind.ROADWORKS_CLOSURE, roadworks("M8 WB Woodside - A804 Phoenix Rd - Closure").getKind());
        assertEquals(SituationKind.ROADWORKS, roadworks("A830 Arisaig - Vergeworks").getKind());
    }

    @Test
    public void roadworksCommentIsSplitIntoParts() {
        Situation s = roadworks("A1 - SB - Old Craighall to Lamberton - Mobile Lane closure");
        assertEquals(4, s.getDescriptionParts().size());
        assertEquals("Mobile Lane closure", s.getWorksSummary());
        assertTrue(s.isOnRoad("a1"));
        assertFalse(s.isOnRoad("A90"));
    }

    @Test
    public void durationBandsMatchColourCoding() {
        Validity v = new Validity();
        OffsetDateTime start = OffsetDateTime.parse("2026-08-01T00:00:00+01:00");
        v.setStartTime(start);
        assertEquals(Validity.DurationBand.UNKNOWN, v.getDurationBand());
        v.setEndTime(start.plusDays(3));
        assertEquals(Validity.DurationBand.SHORT, v.getDurationBand());     // up to 1 month
        v.setEndTime(start.plusDays(100));
        assertEquals(Validity.DurationBand.MEDIUM, v.getDurationBand());    // 1-12 months
        v.setEndTime(start.plusDays(400));
        assertEquals(Validity.DurationBand.LONG, v.getDurationBand());      // over 12 months
    }

    @Test
    public void timingShowsPlannedActiveEnded() {
        Validity v = new Validity();
        OffsetDateTime start = OffsetDateTime.parse("2026-11-10T08:00:00Z");
        v.setStartTime(start);
        v.setEndTime(start.plusDays(5));
        assertEquals(Validity.Timing.PLANNED, v.getTiming(start.minusDays(1)));
        assertEquals(Validity.Timing.ACTIVE, v.getTiming(start.plusDays(2)));
        assertEquals(Validity.Timing.ENDED, v.getTiming(start.plusDays(6)));
        v.setEndTime(null);   // incidents have no end time: still active
        assertEquals(Validity.Timing.ACTIVE, v.getTiming(start.plusDays(100)));
    }

    @Test
    public void distanceUsesNearestPoint() {
        Situation s = roadworks("M8 test");
        LocationPoint from = new LocationPoint(LocationPoint.ROLE_FROM, null);
        from.setLatitude(55.8642);   // Glasgow
        from.setLongitude(-4.2518);
        LocationPoint to = new LocationPoint(LocationPoint.ROLE_TO, null);
        to.setLatitude(55.9533);     // Edinburgh
        to.setLongitude(-3.1883);
        s.getLocation().addPoint(from);
        s.getLocation().addPoint(to);
        // Glasgow to Edinburgh is about 67 km; distance from Edinburgh must use the "to" point
        assertEquals(67.0, from.distanceKmTo(55.9533, -3.1883), 2.0);
        assertEquals(0.0, s.distanceKmTo(55.9533, -3.1883), 0.01);
    }

    @Test
    public void joinedRoadNameFallsBackToComment() {
        Situation s = new Situation(SituationCategory.ROADWORKS);
        LocationPoint p = new LocationPoint(LocationPoint.ROLE_FROM, null);
        p.addDescriptor("linkName", "M8MONKLANDS");
        s.getLocation().addPoint(p);
        s.appendDescription("M8 WB JCT 15 On Slip Stirling Rd - Slip Closure");
        TextBreakdown.enrich(s);
        assertEquals("M8", s.getRoadName());
    }

    private static Situation roadworks(String comment) {
        Situation s = new Situation(SituationCategory.ROADWORKS);
        s.appendDescription(comment);
        TextBreakdown.enrich(s);
        return s;
    }
}
