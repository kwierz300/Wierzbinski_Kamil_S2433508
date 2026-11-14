//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//
package org.me.gcu.wierzbinski_kamil_s2433508.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.wierzbinski_kamil_s2433508.model.FeedState;
import org.me.gcu.wierzbinski_kamil_s2433508.model.Situation;
import org.me.gcu.wierzbinski_kamil_s2433508.model.SituationCategory;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The single source of traffic data for the app (the "Model" side of MVVM).
 *
 * Threads: all network, file and parsing work runs on a background
 * ExecutorService. Results reach the UI only through LiveData.postValue(),
 * so no code here touches views and runOnUiThread is not needed.
 * A ScheduledExecutorService repeats the update at a fixed interval.
 */
public class TrafficRepository {

    private static final String TAG = "TrafficRepository";
    private static volatile TrafficRepository instance;

    private final Context appContext;
    private final AppConfig config;
    private final FeedDownloader downloader = new FeedDownloader();
    private final FeedCache cache;
    private final NetworkMonitor networkMonitor;

    /** One background thread: updates run one after another, never at the same time. */
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoUpdate;

    private final AtomicBoolean updating = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);

    private final MutableLiveData<TrafficData> data = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    /** Creates the helpers. Private, because the app uses one repository only (singleton). */
    private TrafficRepository(Context context) {
        appContext = context.getApplicationContext();
        config = AppConfig.fromBuild();
        cache = new FeedCache(appContext);
        networkMonitor = new NetworkMonitor(appContext);
    }

    /** Returns the single repository and creates it the first time. */
    public static TrafficRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (TrafficRepository.class) {
                if (instance == null) {
                    instance = new TrafficRepository(context);
                }
            }
        }
        return instance;
    }

    // ------------------------------------------------------------------ public API

    /** Latest data of both feeds. */
    public LiveData<TrafficData> getData() {
        return data;
    }

    /** True while an update is running. */
    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** Internet connection state. */
    public LiveData<Boolean> getOnline() {
        return networkMonitor.getOnline();
    }

    /** App settings (keys, example data, update interval). */
    public AppConfig getConfig() {
        return config;
    }

    /**
     * Called once when the app starts: shows the saved data straight away (if any),
     * then downloads fresh data. Calling it again does nothing.
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        networkMonitor.start(this::onReconnected);
        worker.execute(this::showSavedData);
        update();
    }

    /** Gets new data in the background. Ignored if an update is already running. */
    public void update() {
        if (!updating.compareAndSet(false, true)) {
            return;
        }
        loading.postValue(true);
        worker.execute(() -> {
            try {
                FeedState incidents = loadFeed(SituationCategory.INCIDENT);
                FeedState roadworks = loadFeed(SituationCategory.ROADWORKS);
                data.postValue(new TrafficData(incidents, roadworks, System.currentTimeMillis()));
            } catch (RuntimeException e) {
                // Never let an unexpected problem stop the app
                Log.e(TAG, "Update failed", e);
            } finally {
                updating.set(false);
                loading.postValue(false);
            }
        });
    }

    /**
     * Turns the automatic update on (while the app is on screen) or off.
     * The first automatic update waits for the rest of the interval since the last one.
     */
    public synchronized void setAutoUpdate(boolean enabled) {
        if (autoUpdate != null) {
            autoUpdate.cancel(false);
            autoUpdate = null;
        }
        if (!enabled) {
            return;
        }
        long intervalMs = TimeUnit.MINUTES.toMillis(config.getRefreshMinutes());
        long firstDelayMs = intervalMs;
        TrafficData current = data.getValue();
        if (current != null) {
            long age = System.currentTimeMillis() - current.getCheckedAtMillis();
            firstDelayMs = Math.max(0, intervalMs - age);
        }
        autoUpdate = scheduler.scheduleWithFixedDelay(this::update,
                firstDelayMs, intervalMs, TimeUnit.MILLISECONDS);
        Log.d(TAG, "Auto update every " + config.getRefreshMinutes() + " min, first in "
                + (firstDelayMs / 1000) + " s");
    }

    // ------------------------------------------------------------------ background work

    /** Called when the connection comes back: update straight away. */
    private void onReconnected() {
        Log.d(TAG, "Connection is back - updating");
        update();
    }

    /** Posts the saved data (if there is any) before the first download finishes. */
    private void showSavedData() {
        if (config.useSampleData() || data.getValue() != null) {
            return;
        }
        FeedState incidents = fromCache(SituationCategory.INCIDENT, null);
        FeedState roadworks = fromCache(SituationCategory.ROADWORKS, null);
        if (incidents.getSource() != FeedState.Source.NONE || roadworks.getSource() != FeedState.Source.NONE) {
            data.postValue(new TrafficData(incidents, roadworks, 0));
        }
    }

    /** Gets one feed: example file, fresh download, or the saved copy if the download fails. */
    private FeedState loadFeed(SituationCategory category) {
        if (config.useSampleData()) {
            return fromSample(category);
        }
        if (!networkMonitor.isOnlineNow()) {
            return fromCache(category, FeedException.Reason.NO_CONNECTION.getUserMessage());
        }
        if (!config.hasCredentials()) {
            return fromCache(category, FeedException.Reason.NOT_CONFIGURED.getUserMessage());
        }
        try {
            String xml = downloader.download(config.urlFor(category),
                    config.getClientId(), config.getClientKey());
            TrafficXmlParser parser = new TrafficXmlParser();
            List<Situation> list = parser.parse(xml, category);
            if (list.isEmpty() && !xml.contains("payloadPublication")) {
                // Not a DATEX II document (for example an error page sent for a wrong key): keep the old data.
                // A real feed with no situations at the moment is fine and is shown as empty.
                throw new FeedException(FeedException.Reason.NOT_TRAFFIC_DATA, "no DATEX II publication in response");
            }
            cache.save(category, xml);
            return new FeedState(category, FeedState.Source.LIVE, list,
                    parser.getLastPublicationTime(), System.currentTimeMillis(), null);
        } catch (FeedException e) {
            Log.w(TAG, category + ": " + e.getMessage());
            return fromCache(category, e.getReason().getUserMessage());
        } catch (XmlPullParserException | IOException e) {
            Log.w(TAG, category + ": parsing failed", e);
            return fromCache(category, FeedException.Reason.BAD_DATA.getUserMessage());
        }
    }

    /** The saved copy of a feed, marked with the error that made us use it. */
    private FeedState fromCache(SituationCategory category, String errorMessage) {
        String xml = cache.load(category);
        if (xml == null) {
            return FeedState.empty(category, errorMessage);
        }
        try {
            TrafficXmlParser parser = new TrafficXmlParser();
            List<Situation> list = parser.parse(xml, category);
            return new FeedState(category, FeedState.Source.CACHE, list,
                    parser.getLastPublicationTime(), cache.savedAt(category), errorMessage);
        } catch (XmlPullParserException | IOException e) {
            Log.w(TAG, category + ": saved data could not be read", e);
            return FeedState.empty(category, errorMessage);
        }
    }

    /** Example XML files packed in debug builds only (src/debug/assets). */
    private FeedState fromSample(SituationCategory category) {
        try {
            String xml = readAsset(config.sampleFileFor(category));
            TrafficXmlParser parser = new TrafficXmlParser();
            List<Situation> list = parser.parse(xml, category);
            return new FeedState(category, FeedState.Source.SAMPLE, list,
                    parser.getLastPublicationTime(), System.currentTimeMillis(), null);
        } catch (IOException | XmlPullParserException e) {
            Log.w(TAG, "Sample data not available", e);
            return FeedState.empty(category, "Example data not available in this build");
        }
    }

    /** Reads an example file from the debug assets into a String. */
    private String readAsset(String name) throws IOException {
        AssetManager assets = appContext.getAssets();
        try (InputStream in = assets.open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
