//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Downloads one Traffic Scotland feed into a String.
 * Based on the starter code, but with time-outs, a StringBuilder instead of
 * joining Strings in a loop, a check of the HTTP status code and clear errors.
 * Must be called on a background thread (never on the UI thread).
 */
public class FeedDownloader {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    public String download(String address, String clientId, String clientKey) throws FeedException {
        if (address == null || address.isEmpty()) {
            throw new FeedException(FeedException.Reason.NOT_CONFIGURED, "no feed address");
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            // Authorisation header as in the starter code: Base64 of "clientId:clientKey"
            String credentials = clientId + ":" + clientKey;
            connection.setRequestProperty("Authorization",
                    Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new FeedException(FeedException.Reason.ACCESS_DENIED, "HTTP " + status);
            }
            if (status != HttpURLConnection.HTTP_OK) {
                throw new FeedException(FeedException.Reason.SERVER_ERROR, "HTTP " + status);
            }

            StringBuilder result = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line).append('\n');
                }
            }
            return result.toString();
        } catch (IOException e) {
            throw new FeedException(FeedException.Reason.NO_CONNECTION, String.valueOf(e.getMessage()));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
