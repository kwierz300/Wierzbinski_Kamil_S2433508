//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.data;

import android.content.Context;
import android.util.Log;

import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Keeps the last downloaded XML of each feed in the app's private storage,
 * so the app can show the most recent data when there is no connection.
 * The raw XML is saved (not the parsed objects), so it is parsed in exactly
 * the same way as fresh data.
 */
public class FeedCache {

    private static final String TAG = "FeedCache";

    private final File folder;

    /** The files are kept in the app's private folder. */
    public FeedCache(Context context) {
        this.folder = context.getFilesDir();
    }

    /** File used for one feed (feed_incident.xml or feed_roadworks.xml). */
    private File fileFor(SituationCategory category) {
        return new File(folder, "feed_" + category.name().toLowerCase(Locale.UK) + ".xml");
    }

    /** Saves the XML; writes to a temporary file first so a crash never leaves half a file. */
    public synchronized void save(SituationCategory category, String xml) {
        File target = fileFor(category);
        File temp = new File(folder, target.getName() + ".tmp");
        try (OutputStream out = new FileOutputStream(temp)) {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Could not save " + category, e);
            return;
        }
        if (!temp.renameTo(target)) {
            Log.e(TAG, "Could not replace " + target.getName());
        }
    }

    /** The saved XML, or null if nothing has been saved yet. */
    public synchronized String load(SituationCategory category) {
        File file = fileFor(category);
        if (!file.exists()) {
            return null;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int offset = 0;
            while (offset < bytes.length) {
                int read = in.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
            return new String(bytes, 0, offset, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Could not read " + category, e);
            return null;
        }
    }

    /** When the saved XML was written (milliseconds since 1970), 0 if there is none. */
    public synchronized long savedAt(SituationCategory category) {
        File file = fileFor(category);
        return file.exists() ? file.lastModified() : 0;
    }
}
