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
import org.me.gcu.wierzbinski_kamil_s2433508.model.Validity;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the Traffic Scotland DATEX II XML (already downloaded into a String)
 * with the XmlPullParser and builds a list of {@link Situation} objects.
 *
 * The same element name (for example "value") is used in many places in the
 * feed, so the parser keeps a stack of open element names (the "path") and
 * decides what a text value means by looking at its parent elements.
 * Unknown elements are ignored, so new data in the feed does not crash the app.
 */
public class TrafficXmlParser {

    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    // Parser state (reset for every parse call)
    private final List<String> path = new ArrayList<>();
    private final StringBuilder text = new StringBuilder();
    private Situation current;
    private String currentSituationId;
    private LocationPoint currentPoint;
    private String pendingDescriptorValue;
    private String pendingDescriptorType;
    private OffsetDateTime publicationTime;
    // <headerInformation> comes before <situationRecord>, so its values are kept here first
    private String headerConfidentiality;
    private String headerInformationStatus;

    /**
     * Parses one feed.
     *
     * @param xml      the downloaded XML text
     * @param category which feed the text came from
     * @return list of situations, never null (empty if the text has no data)
     */
    public synchronized List<Situation> parse(String xml, SituationCategory category)
            throws XmlPullParserException, IOException {
        List<Situation> result = new ArrayList<>();
        resetState();
        String cleanXml = cleanUp(xml);
        if (cleanXml == null) {
            return result;
        }

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(cleanXml));

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                path.add(xpp.getName());
                text.setLength(0);
                handleStartTag(xpp, category);
            } else if (eventType == XmlPullParser.TEXT) {
                text.append(xpp.getText());
            } else if (eventType == XmlPullParser.END_TAG) {
                handleEndTag(xpp.getName(), text.toString().trim(), result);
                text.setLength(0);
                if (!path.isEmpty()) {
                    path.remove(path.size() - 1);
                }
            }
            eventType = xpp.next();
        }
        return result;
    }

    /**
     * The publicationTime of the feed read by the last call to {@link #parse},
     * or null if the feed did not have one.
     */
    public synchronized OffsetDateTime getLastPublicationTime() {
        return publicationTime;
    }

    /** Removes anything before the first tag (as the starter code did) and handles empty input. */
    static String cleanUp(String xml) {
        if (xml == null) {
            return null;
        }
        int start = xml.indexOf("<?");
        if (start < 0) {
            start = xml.indexOf('<');
        }
        if (start < 0) {
            return null;   // no XML at all (for example an empty or error response)
        }
        return xml.substring(start);
    }

    /** Clears everything left from the previous parse. */
    private void resetState() {
        path.clear();
        text.setLength(0);
        current = null;
        currentSituationId = null;
        currentPoint = null;
        pendingDescriptorValue = null;
        pendingDescriptorType = null;
        publicationTime = null;
        headerConfidentiality = null;
        headerInformationStatus = null;
    }

    // ------------------------------------------------------------------ start tags

    /** Reacts to an opening tag: starts a new record or point and reads attributes (id, version, xsi:type). */
    private void handleStartTag(XmlPullParser xpp, SituationCategory category) {
        String name = xpp.getName();
        switch (name) {
            case "situation":
                currentSituationId = xpp.getAttributeValue(null, "id");
                headerConfidentiality = null;
                headerInformationStatus = null;
                break;
            case "situationRecord":
                current = new Situation(category);
                current.setSituationId(currentSituationId);
                current.setRecordId(xpp.getAttributeValue(null, "id"));
                current.setRecordType(getXsiType(xpp));
                current.setRecordVersion(xpp.getAttributeValue(null, "version"));
                current.setConfidentiality(headerConfidentiality);
                current.setInformationStatus(headerInformationStatus);
                currentPoint = null;
                break;
            case "groupOfLocations":
                if (current != null) {
                    current.getLocation().setLocationType(getXsiType(xpp));
                }
                break;
            case "from":
            case "to":
                if (current != null && inside("groupOfLocations")) {
                    currentPoint = new LocationPoint(name, getXsiType(xpp));
                }
                break;
            case "name":
            case "ilc":
            case "otherName":
                if (currentPoint != null) {
                    pendingDescriptorValue = null;
                    pendingDescriptorType = null;
                }
                break;
            default:
                break;
        }
    }

    /** Reads the xsi:type attribute (for example "MaintenanceWorks"). */
    private static String getXsiType(XmlPullParser xpp) {
        String type = xpp.getAttributeValue(XSI_NAMESPACE, "type");
        if (type == null) {
            // Fallback: look for any attribute called "type"
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                if ("type".equals(xpp.getAttributeName(i))) {
                    type = xpp.getAttributeValue(i);
                }
            }
        }
        return type;
    }

    // ------------------------------------------------------------------ end tags

    /** Reacts to a closing tag: finishes records and points, or stores the text value. */
    private void handleEndTag(String name, String value, List<Situation> result) {
        if (current == null) {
            // outside a situation record (headers, envelope, ...)
            if ("publicationTime".equals(name) && !value.isEmpty()) {
                publicationTime = parseDate(value);
            } else if (inside("headerInformation") && !value.isEmpty()) {
                if ("confidentiality".equals(name)) {
                    headerConfidentiality = value;
                } else if ("informationStatus".equals(name)) {
                    headerInformationStatus = value;
                }
            }
            return;
        }
        switch (name) {
            case "situationRecord":
                TextBreakdown.enrich(current);
                result.add(current);
                current = null;
                return;
            case "groupOfLocations":
                currentPoint = null;
                return;
            case "from":
            case "to":
                if (currentPoint != null && inside("groupOfLocations")) {
                    current.getLocation().addPoint(currentPoint);
                    currentPoint = null;
                }
                return;
            case "name":
            case "ilc":
            case "otherName":
                if (currentPoint != null) {
                    currentPoint.addDescriptor(pendingDescriptorType, pendingDescriptorValue);
                }
                return;
            default:
                break;
        }
        if (value.isEmpty()) {
            return;
        }
        handleValue(name, value);
    }

    /** Stores one text value in the right place, based on the element name and its parents. */
    private void handleValue(String name, String value) {
        // --- location data
        if (inside("groupOfLocations")) {
            handleLocationValue(name, value);
            return;
        }
        // --- comment text and source name both use <value>, so check the parent
        if ("value".equals(name)) {
            if (inside("generalPublicComment")) {
                current.appendDescription(value);
            } else if (inside("sourceName")) {
                current.setSourceName(value);
            }
            return;
        }
        // --- validity
        if (inside("validity")) {
            Validity validity = current.getValidity();
            if ("validityStatus".equals(name)) {
                validity.setStatus(value);
            } else if ("overallStartTime".equals(name)) {
                validity.setStartTime(parseDate(value));
            } else if ("overallEndTime".equals(name)) {
                validity.setEndTime(parseDate(value));
            }
            return;
        }
        // --- impact
        if (inside("impact")) {
            handleImpactValue(name, value);
            return;
        }
        // --- source
        if (inside("source")) {
            if ("sourceType".equals(name)) {
                current.setSourceType(value);
            } else if ("sourceIdentification".equals(name)) {
                current.setSourceIdentification(value);
            }
            return;
        }
        // --- simple elements directly inside <situationRecord>
        if ("situationRecord".equals(parentName())) {
            switch (name) {
                case "situationRecordCreationTime":
                    current.setCreationTime(parseDate(value));
                    break;
                case "situationRecordVersionTime":
                    current.setVersionTime(parseDate(value));
                    break;
                case "probabilityOfOccurrence":
                    current.setProbability(value);
                    break;
                case "complianceOption":
                    current.setComplianceOption(value);
                    break;
                case "urgentRoadworks":
                    current.setUrgent(Boolean.parseBoolean(value));
                    break;
                case "situationRecordCreationReference":
                    current.setCreationReference(value);
                    break;
                case "situationRecordFirstSupplierVersionTime":
                    current.setFirstSupplierVersionTime(parseDate(value));
                    break;
                default:
                    if (name.endsWith("Type") && current.getSubType() == null) {
                        // roadOrCarriagewayOrLaneManagementType, abnormalTrafficType,
                        // roadMaintenanceType, accidentType, ... (also unknown future ones)
                        current.setSubType(name, value);
                    } else {
                        current.putOtherDetail(name, value);
                    }
                    break;
            }
        }
    }

    /** Stores values from groupOfLocations: direction, coordinates and point names. */
    private void handleLocationValue(String name, String value) {
        if ("tpegDirection".equals(name)) {
            current.getLocation().setDirection(value);
            return;
        }
        if ("tpegLinearLocationType".equals(name)) {
            current.getLocation().setLinearType(value);
            return;
        }
        if ("latitude".equals(name) || "longitude".equals(name)) {
            double number = parseDouble(value);
            boolean pointIsFull = currentPoint != null
                    && LocationPoint.ROLE_POINT.equals(currentPoint.getRole())
                    && currentPoint.hasCoordinates();
            if (currentPoint == null || pointIsFull) {
                // A single point location (not from/to) - keep it as a "point"
                currentPoint = new LocationPoint(LocationPoint.ROLE_POINT, null);
                current.getLocation().addPoint(currentPoint);
            }
            if ("latitude".equals(name)) {
                currentPoint.setLatitude(number);
            } else {
                currentPoint.setLongitude(number);
            }
            return;
        }
        if (currentPoint != null) {
            if ("value".equals(name)) {
                pendingDescriptorValue = value;
            } else if (name.endsWith("DescriptorType")) {
                pendingDescriptorType = value;
            }
        }
    }

    /** Stores values from impact; values the app does not know are kept as "other values". */
    private void handleImpactValue(String name, String value) {
        Impact impact = current.getImpact();
        switch (name) {
            case "numberOfOperationalLanes":
                impact.setOperationalLanes(parseInteger(value));
                break;
            case "numberOfLanesRestricted":
                impact.setLanesRestricted(parseInteger(value));
                break;
            case "delaysType":
                impact.setDelaysType(value);
                break;
            case "delayTimeValue":
                impact.setDelaySeconds(parseInteger(value));
                break;
            default:
                impact.putOtherValue(name, value);
                break;
        }
    }

    // ------------------------------------------------------------------ helpers

    /** True if the current element is inside an element with this name. */
    private boolean inside(String elementName) {
        // the last item of the path is the current element itself
        for (int i = 0; i < path.size() - 1; i++) {
            if (path.get(i).equals(elementName)) {
                return true;
            }
        }
        return false;
    }

    /** Name of the parent of the current element. */
    private String parentName() {
        return path.size() >= 2 ? path.get(path.size() - 2) : null;
    }

    /** ISO date-time text to OffsetDateTime, or null if it cannot be read. */
    static OffsetDateTime parseDate(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Text to Integer, or null if it is not a number. */
    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Text to double, or NaN if it is not a number. */
    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
