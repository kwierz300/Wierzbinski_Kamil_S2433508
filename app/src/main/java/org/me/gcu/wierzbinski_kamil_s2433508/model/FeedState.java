//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The current state of one feed (incidents or roadworks): the parsed situations,
 * where they came from, when the feed was published and the last error, if any.
 * Objects of this class never change after they are created, so they can be
 * passed safely from the background thread to the UI thread.
 */
public final class FeedState {

    /** Where the data shown to the user came from. */
    public enum Source {
        LIVE,       // downloaded just now
        CACHE,      // last saved download (no connection or download failed)
        SAMPLE,     // example files (debug builds only)
        NONE        // no data at all
    }

    private final SituationCategory category;
    private final Source source;
    private final List<Situation> situations;
    private final OffsetDateTime publicationTime;
    private final long savedAtMillis;
    private final String errorMessage;

    /** Creates an unchangeable state of one feed. */
    public FeedState(SituationCategory category, Source source, List<Situation> situations,
                     OffsetDateTime publicationTime, long savedAtMillis, String errorMessage) {
        this.category = category;
        this.source = source;
        this.situations = situations == null
                ? Collections.<Situation>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(situations));
        this.publicationTime = publicationTime;
        this.savedAtMillis = savedAtMillis;
        this.errorMessage = errorMessage;
    }

    /** State with no data, only an error message. */
    public static FeedState empty(SituationCategory category, String errorMessage) {
        return new FeedState(category, Source.NONE, null, null, 0, errorMessage);
    }

    public SituationCategory getCategory() {
        return category;
    }

    public Source getSource() {
        return source;
    }

    /** Situations of this feed (read-only list). */
    public List<Situation> getSituations() {
        return situations;
    }

    /** publicationTime from the feed itself. */
    public OffsetDateTime getPublicationTime() {
        return publicationTime;
    }

    /** When this data was downloaded (milliseconds since 1970), 0 if unknown. */
    public long getSavedAtMillis() {
        return savedAtMillis;
    }

    /** Message for the user when the last download failed, otherwise null. */
    public String getErrorMessage() {
        return errorMessage;
    }

    /** True when the last download of this feed failed. */
    public boolean hasError() {
        return errorMessage != null;
    }
}
