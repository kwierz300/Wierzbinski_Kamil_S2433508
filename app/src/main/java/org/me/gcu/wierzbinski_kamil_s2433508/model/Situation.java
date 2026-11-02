//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One traffic situation (an unplanned event or a roadworks record).
 * Built by the XML parser from one &lt;situationRecord&gt; element.
 */
public class Situation {

    private final SituationCategory category;

    // Raw values from the XML
    private String situationId;          // id of the parent <situation>
    private String recordId;             // id of the <situationRecord>
    private String recordVersion;        // version attribute of the <situationRecord>
    private String recordType;           // xsi:type, e.g. RoadOrCarriagewayOrLaneManagement
    private String subType;              // e.g. roadClosed, queuingTraffic, roadworks
    private String subTypeElement;       // name of the element that held subType
    private String description = "";     // generalPublicComment
    private OffsetDateTime creationTime;
    private OffsetDateTime versionTime;
    private OffsetDateTime firstSupplierVersionTime;
    private String creationReference;    // situationRecordCreationReference
    private String confidentiality;      // headerInformation/confidentiality
    private String informationStatus;    // headerInformation/informationStatus (real, test, ...)
    private String sourceIdentification; // source/sourceIdentification
    private String probability;          // probabilityOfOccurrence
    private String sourceName;
    private String sourceType;
    private String complianceOption;
    private Boolean urgent;              // urgentRoadworks
    private final Validity validity = new Validity();
    private final Impact impact = new Impact();
    private final RoadLocation location = new RoadLocation();
    private final Map<String, String> otherDetails = new LinkedHashMap<>();

    // Values worked out from the text by TextBreakdown
    private String roadName;             // display form, e.g. "A74(M)"
    private String roadKey;              // normalised form for searching, e.g. "A74M"
    private final Set<String> roadKeys = new LinkedHashSet<>(); // all roads in location + comment
    private SituationKind kind;
    private List<String> descriptionParts = new ArrayList<>();
    private String worksSummary;         // e.g. "Temporary traffic lights"

    /** Creates an empty situation of the given category; the parser fills it in. */
    public Situation(SituationCategory category) {
        this.category = category;
    }

    /** Unique key in the app: record id plus category. */
    public String getKey() {
        return category.name() + ":" + recordId;
    }

    /** Incident or roadworks. */
    public SituationCategory getCategory() {
        return category;
    }

    /** id of the parent situation element. */
    public String getSituationId() {
        return situationId;
    }

    /** Sets the value returned by getSituationId(). */
    public void setSituationId(String situationId) {
        this.situationId = situationId;
    }

    /** id of the situationRecord. */
    public String getRecordId() {
        return recordId;
    }

    /** Sets the value returned by getRecordId(). */
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    /** version attribute of the situationRecord. */
    public String getRecordVersion() {
        return recordVersion;
    }

    /** Sets the value returned by getRecordVersion(). */
    public void setRecordVersion(String recordVersion) {
        this.recordVersion = recordVersion;
    }

    /** situationRecordFirstSupplierVersionTime. */
    public OffsetDateTime getFirstSupplierVersionTime() {
        return firstSupplierVersionTime;
    }

    /** Sets the value returned by getFirstSupplierVersionTime(). */
    public void setFirstSupplierVersionTime(OffsetDateTime firstSupplierVersionTime) {
        this.firstSupplierVersionTime = firstSupplierVersionTime;
    }

    /** situationRecordCreationReference. */
    public String getCreationReference() {
        return creationReference;
    }

    /** Sets the value returned by getCreationReference(). */
    public void setCreationReference(String creationReference) {
        this.creationReference = creationReference;
    }

    /** confidentiality from headerInformation. */
    public String getConfidentiality() {
        return confidentiality;
    }

    /** Sets the value returned by getConfidentiality(). */
    public void setConfidentiality(String confidentiality) {
        this.confidentiality = confidentiality;
    }

    /** informationStatus from headerInformation (e.g. real). */
    public String getInformationStatus() {
        return informationStatus;
    }

    /** Sets the value returned by getInformationStatus(). */
    public void setInformationStatus(String informationStatus) {
        this.informationStatus = informationStatus;
    }

    /** sourceIdentification of the source. */
    public String getSourceIdentification() {
        return sourceIdentification;
    }

    /** Sets the value returned by getSourceIdentification(). */
    public void setSourceIdentification(String sourceIdentification) {
        this.sourceIdentification = sourceIdentification;
    }

    /** xsi:type of the record, e.g. MaintenanceWorks. */
    public String getRecordType() {
        return recordType;
    }

    /** Sets the value returned by getRecordType(). */
    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    /** Value of the sub-type element, e.g. roadClosed. */
    public String getSubType() {
        return subType;
    }

    /** Sets the sub-type and the name of the element it came from. */
    public void setSubType(String subTypeElement, String subType) {
        this.subTypeElement = subTypeElement;
        this.subType = subType;
    }

    /** Name of the element that held the sub-type. */
    public String getSubTypeElement() {
        return subTypeElement;
    }

    /** generalPublicComment text (all comments joined). */
    public String getDescription() {
        return description;
    }

    /** Adds comment text; a situation can have more than one comment. */
    public void appendDescription(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        description = description.isEmpty() ? text : description + "\n" + text;
    }

    /** situationRecordCreationTime. */
    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    /** Sets the value returned by getCreationTime(). */
    public void setCreationTime(OffsetDateTime creationTime) {
        this.creationTime = creationTime;
    }

    /** situationRecordVersionTime (time of the last change). */
    public OffsetDateTime getVersionTime() {
        return versionTime;
    }

    /** Sets the value returned by getVersionTime(). */
    public void setVersionTime(OffsetDateTime versionTime) {
        this.versionTime = versionTime;
    }

    /** probabilityOfOccurrence, e.g. certain. */
    public String getProbability() {
        return probability;
    }

    /** Sets the value returned by getProbability(). */
    public void setProbability(String probability) {
        this.probability = probability;
    }

    /** Source name, e.g. TrafficScotland. */
    public String getSourceName() {
        return sourceName;
    }

    /** Sets the value returned by getSourceName(). */
    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /** Source type, e.g. roadAuthorities. */
    public String getSourceType() {
        return sourceType;
    }

    /** Sets the value returned by getSourceType(). */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /** complianceOption, e.g. mandatory. */
    public String getComplianceOption() {
        return complianceOption;
    }

    /** Sets the value returned by getComplianceOption(). */
    public void setComplianceOption(String complianceOption) {
        this.complianceOption = complianceOption;
    }

    /** urgentRoadworks, or null for incidents. */
    public Boolean getUrgent() {
        return urgent;
    }

    /** Sets the value returned by getUrgent(). */
    public void setUrgent(Boolean urgent) {
        this.urgent = urgent;
    }

    /** When the situation is valid. */
    public Validity getValidity() {
        return validity;
    }

    /** Effect on traffic. */
    public Impact getImpact() {
        return impact;
    }

    /** Where the situation is. */
    public RoadLocation getLocation() {
        return location;
    }

    /** Keeps a simple value the app does not know. */
    public void putOtherDetail(String name, String value) {
        otherDetails.put(name, value);
    }

    /** Simple values the app does not know (read-only). */
    public Map<String, String> getOtherDetails() {
        return Collections.unmodifiableMap(otherDetails);
    }

    /** Road number for display, e.g. A74(M). */
    public String getRoadName() {
        return roadName;
    }

    /** Sets the value returned by getRoadName(). */
    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    /** Road number in search form, e.g. A74M. */
    public String getRoadKey() {
        return roadKey;
    }

    /** Sets the value returned by getRoadKey(). */
    public void setRoadKey(String roadKey) {
        this.roadKey = roadKey;
    }

    /** Adds a normalised road name that this situation is about (null is ignored). */
    public void addRoadKey(String key) {
        if (key != null) {
            roadKeys.add(key);
        }
    }

    /** All normalised road names linked to this situation (location and comment). */
    public Set<String> getRoadKeys() {
        return Collections.unmodifiableSet(roadKeys);
    }

    /** True if this situation is on the given road; "a74m", "A74(M)" both match "A74(M)". */
    public boolean isOnRoad(String road) {
        if (road == null) {
            return false;
        }
        String key = road.toUpperCase(java.util.Locale.UK).replaceAll("[^A-Z0-9]", "");
        return !key.isEmpty() && roadKeys.contains(key);
    }

    /** Detailed kind of situation (chooses the icon). */
    public SituationKind getKind() {
        return kind;
    }

    /** Sets the value returned by getKind(). */
    public void setKind(SituationKind kind) {
        this.kind = kind;
    }

    /** Comment split into parts at " - ". */
    public List<String> getDescriptionParts() {
        return Collections.unmodifiableList(descriptionParts);
    }

    /** Sets the value returned by getDescriptionParts(). */
    public void setDescriptionParts(List<String> descriptionParts) {
        this.descriptionParts = new ArrayList<>(descriptionParts);
    }

    /** Readable summary of the works (roadworks only). */
    public String getWorksSummary() {
        return worksSummary;
    }

    /** Sets the value returned by getWorksSummary(). */
    public void setWorksSummary(String worksSummary) {
        this.worksSummary = worksSummary;
    }

    /** Shortest distance in km from the given coordinates to this situation. */
    public double distanceKmTo(double lat, double lon) {
        return location.distanceKmTo(lat, lon);
    }
}
