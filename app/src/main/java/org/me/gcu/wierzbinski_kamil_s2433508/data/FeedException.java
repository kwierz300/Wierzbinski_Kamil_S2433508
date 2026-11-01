//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.data;

/**
 * A problem while getting a feed, with a reason the app can explain to the user.
 */
public class FeedException extends Exception {

    public enum Reason {
        NO_CONNECTION("No internet connection"),
        ACCESS_DENIED("Access denied - check the client ID and key"),
        SERVER_ERROR("Traffic Scotland server error"),
        BAD_DATA("The data could not be read"),
        NOT_CONFIGURED("Feed address or keys not set up");

        private final String userMessage;

        Reason(String userMessage) {
            this.userMessage = userMessage;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    private final Reason reason;

    public FeedException(Reason reason, String detail) {
        super(reason.name() + ": " + detail);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
