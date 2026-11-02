//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.data;

import org.me.gcu.wierzbinski_kamil_s2433508.BuildConfig;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;

/**
 * App settings. The client ID and key are NOT written in the source code:
 * they are kept in local.properties (which is not uploaded to GitHub) and copied
 * into the generated BuildConfig class when the app is built (see app/build.gradle.kts).
 */
public final class AppConfig {

    public static final String UNPLANNED_EVENTS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    /** "Current Roadworks" publication (the brief uses only Unplanned Events and Current Roadworks). */
    public static final String ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    public static final String SAMPLE_UNPLANNED_FILE = "unplannedContentExample.xml";
    public static final String SAMPLE_ROADWORKS_FILE = "roadworksContentExample.xml";

    private final String clientId;
    private final String clientKey;
    private final boolean useSampleData;
    private final int refreshMinutes;

    /** Creates the settings. Empty keys are allowed (the app then shows a message instead of data). */
    public AppConfig(String clientId, String clientKey, boolean useSampleData, int refreshMinutes) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientKey = clientKey == null ? "" : clientKey.trim();
        this.useSampleData = useSampleData;
        this.refreshMinutes = refreshMinutes > 0 ? refreshMinutes : 60;
    }

    /** Settings from local.properties, as built into this version of the app. */
    public static AppConfig fromBuild() {
        return new AppConfig(BuildConfig.TS_CLIENT_ID, BuildConfig.TS_CLIENT_KEY,
                BuildConfig.USE_SAMPLE_DATA, BuildConfig.REFRESH_MINUTES);
    }

    /** Traffic Scotland client ID. */
    public String getClientId() {
        return clientId;
    }

    /** Traffic Scotland client key. */
    public String getClientKey() {
        return clientKey;
    }

    /** True when both the client ID and the key are set. */
    public boolean hasCredentials() {
        return !clientId.isEmpty() && !clientKey.isEmpty();
    }

    /** True only in debug builds when USE_SAMPLE_DATA=true is set in local.properties. */
    public boolean useSampleData() {
        return useSampleData;
    }

    /** Minutes between automatic updates (60 by default, shorter for the demo). */
    public int getRefreshMinutes() {
        return refreshMinutes;
    }

    /** Feed address for incidents or roadworks. */
    public String urlFor(SituationCategory category) {
        return category == SituationCategory.INCIDENT ? UNPLANNED_EVENTS_URL : ROADWORKS_URL;
    }

    /** Example XML file name for incidents or roadworks. */
    public String sampleFileFor(SituationCategory category) {
        return category == SituationCategory.INCIDENT ? SAMPLE_UNPLANNED_FILE : SAMPLE_ROADWORKS_FILE;
    }
}
