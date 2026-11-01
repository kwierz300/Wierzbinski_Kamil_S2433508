/*  Mobile Platform Development - coursework 1st diet 26/27
    Traffic Scotland incidents and roadworks app.
*/

//
// Name                 Kamil Wierzbinski
// Student ID           S2433508
// Programme of Study   BSc (Hons) Computing
//

package org.me.gcu.wierzbinski_kamil_s2433508;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.me.gcu.wierzbinski_kamil_s2433508.model.FeedState;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;
import org.me.gcu.wierzbinski_kamil_s2433508.ui.DisplayFormat;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel;

/**
 * The single Activity of the app (the "View" part of MVVM).
 * It only shows data from the ViewModel; it never downloads or parses anything itself.
 * Stage 2: shows a status screen to check downloading, auto-update and offline mode.
 */
public class MainActivity extends AppCompatActivity {

    private TrafficViewModel viewModel;
    private TextView statusText;
    private TextView offlineBanner;
    private LinearProgressIndicator progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        statusText = findViewById(R.id.statusText);
        offlineBanner = findViewById(R.id.offlineBanner);
        progress = findViewById(R.id.progress);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);
        viewModel.getData().observe(this, this::showData);
        viewModel.getLoading().observe(this, isLoading ->
                progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));
        viewModel.getOnline().observe(this, isOnline ->
                offlineBanner.setVisibility(Boolean.FALSE.equals(isOnline)
                        && !viewModel.isUsingSampleData() ? View.VISIBLE : View.GONE));
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.setAutoUpdate(true);    // regular updates only while the app is visible
    }

    @Override
    protected void onStop() {
        viewModel.setAutoUpdate(false);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showData(TrafficData data) {
        if (data == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Last check: ").append(DisplayFormat.time(data.getCheckedAtMillis()))
                .append("\nAuto update every ").append(viewModel.getRefreshMinutes()).append(" min\n\n");
        appendFeed(sb, "INCIDENTS", data.getIncidents());
        appendFeed(sb, "ROADWORKS", data.getRoadworks());
        statusText.setText(sb.toString());
    }

    private static void appendFeed(StringBuilder sb, String title, FeedState feed) {
        sb.append(title).append('\n')
                .append("Source: ").append(DisplayFormat.source(feed.getSource())).append('\n')
                .append("Situations: ").append(feed.getSituations().size()).append('\n')
                .append("Published: ").append(DisplayFormat.dateTime(feed.getPublicationTime())).append('\n')
                .append("Downloaded: ").append(DisplayFormat.dateTime(feed.getSavedAtMillis())).append('\n');
        if (feed.hasError()) {
            sb.append("Problem: ").append(feed.getErrorMessage()).append('\n');
        }
        int shown = Math.min(3, feed.getSituations().size());
        for (int i = 0; i < shown; i++) {
            sb.append(" • ").append(feed.getSituations().get(i).getDescription()).append('\n');
        }
        sb.append('\n');
    }
}
