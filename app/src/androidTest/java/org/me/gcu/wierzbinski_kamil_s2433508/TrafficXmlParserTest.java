//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.me.gcu.wierzbinski_kamil_s2433508.data.TrafficXmlParser;
import org.me.gcu.wierzbinski_kamil_s2433508.model.LocationPoint;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationKind;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Validity;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Tests the PullParser on the example files supplied with the coursework.
 * Runs on the phone/emulator because XmlPullParser is part of Android.
 * The example files are in src/debug/assets (packed in debug builds only).
 * The expected values were copied by hand from the example XML files.
 */
@RunWith(AndroidJUnit4.class)
public class TrafficXmlParserTest {

    private String readAsset(String name) throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (InputStream in = context.getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Test
    public void unplannedEventsAreParsed() throws Exception {
        TrafficXmlParser parser = new TrafficXmlParser();
        List<Situation> list = parser.parse(readAsset("unplannedContentExample.xml"), SituationCategory.INCIDENT);
        assertEquals(4, list.size());
        assertEquals(OffsetDateTime.parse("2026-08-31T09:58:26+01:00"), parser.getLastPublicationTime());

        Situation first = list.get(0);
        assertEquals("I_20260810_248", first.getRecordId());
        assertEquals("RoadOrCarriagewayOrLaneManagement", first.getRecordType());
        assertEquals("roadClosed", first.getSubType());
        assertEquals(SituationKind.ROAD_CLOSED, first.getKind());
        assertEquals("The A74M(S) junction 19 (Ecclefechan) off slip is closed to allow for essential roadworks.",
                first.getDescription());
        assertEquals("TrafficScotland", first.getSourceName());   // not mixed up with the comment
        assertEquals("roadAuthorities", first.getSourceType());
        assertEquals("real", first.getInformationStatus());          // from <headerInformation>
        assertEquals("noRestriction", first.getConfidentiality());
        assertEquals("10", first.getRecordVersion());                 // version attribute
        assertEquals("I_20260810_248", first.getCreationReference());
        assertEquals(OffsetDateTime.parse("2026-08-24T18:25:33+01:00"), first.getVersionTime());
        assertEquals("A74(M)", first.getRoadName());
        assertEquals("southBound", first.getLocation().getDirection());
        assertEquals(Integer.valueOf(0), first.getImpact().getOperationalLanes());
        assertNull(first.getValidity().getEndTime());

        LocationPoint from = first.getLocation().getFrom();
        assertEquals(55.103477, from.getLatitude(), 0.000001);
        assertEquals(-3.349595, from.getLongitude(), 0.000001);
        assertEquals("A74(M) J18 Ecclefechan", from.getPlaceName());
        assertTrue(first.getLocation().isLine());

        Situation queue = list.get(2);
        assertEquals("AbnormalTraffic", queue.getRecordType());
        assertEquals(Integer.valueOf(2), queue.getImpact().getLanesRestricted());
    }

    @Test
    public void roadworksAreParsed() throws Exception {
        List<Situation> list = new TrafficXmlParser()
                .parse(readAsset("roadworksContentExample.xml"), SituationCategory.ROADWORKS);
        assertEquals(405, list.size());

        Situation first = list.get(0);
        assertEquals("D_159301", first.getRecordId());
        assertEquals("MaintenanceWorks", first.getRecordType());
        assertEquals("M8 WB Woodside - A804 Phoenix Rd - Closure", first.getDescription());
        assertEquals("longDelays", first.getImpact().getDelaysType());
        assertEquals("M8", first.getRoadName());
        assertEquals(Validity.DurationBand.LONG, first.getValidity().getDurationBand());
        assertEquals(Boolean.FALSE, first.getUrgent());
        for (Situation s : list) {
            assertTrue(s.getLocation().isLine());
            assertTrue(s.getDescription().length() > 0);
        }
    }

    @Test
    public void emptyOrBrokenInputDoesNotCrash() throws Exception {
        TrafficXmlParser parser = new TrafficXmlParser();
        assertEquals(0, parser.parse(null, SituationCategory.INCIDENT).size());
        assertEquals(0, parser.parse("", SituationCategory.INCIDENT).size());
        assertEquals(0, parser.parse("Forbidden", SituationCategory.INCIDENT).size());
        try {
            parser.parse("<a><b>", SituationCategory.INCIDENT);
        } catch (XmlPullParserException expected) {
            // a cut off download is reported as an exception that the repository handles
        }
    }

    @Test
    public void unknownElementsAreIgnored() throws Exception {
        String xml = "<?xml version=\"1.0\"?><d2LogicalModel xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<situation id=\"S1\"><situationRecord xsi:type=\"Accident\" id=\"R1\">"
                + "<someNewElement><deep>x</deep></someNewElement>"
                + "<accidentType>collision</accidentType>"
                + "<generalPublicComment><comment><values><value>Crash on the A9</value></values></comment></generalPublicComment>"
                + "</situationRecord></situation></d2LogicalModel>";
        List<Situation> list = new TrafficXmlParser().parse(xml, SituationCategory.INCIDENT);
        assertEquals(1, list.size());
        assertEquals(SituationKind.ACCIDENT, list.get(0).getKind());
        assertEquals("A9", list.get(0).getRoadName());
    }
}
