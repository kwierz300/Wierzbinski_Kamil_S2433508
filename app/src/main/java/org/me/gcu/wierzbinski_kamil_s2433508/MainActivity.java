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
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.me.gcu.wierzbinski_kamil_s2433508.model.FeedState;
import org.me.gcu.wierzbinski_kamil_s2433508.model.TrafficData;
import org.me.gcu.wierzbinski_kamil_s2433508.ui.DisplayFormat;
import org.me.gcu.wierzbinski_kamil_s2433508.ui.PlaceholderFragment;
import org.me.gcu.wierzbinski_kamil_s2433508.ui.SituationDetailFragment;
import org.me.gcu.wierzbinski_kamil_s2433508.ui.SituationListFragment;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel;
import org.me.gcu.wierzbinski_kamil_s2433508.viewmodel.TrafficViewModel.Screen;

/**
 * The single Activity of the app (View layer of MVVM). It hosts the fragments,
 * the toolbar and the navigation, and shows the status banner.
 *
 * Portrait: one pane - the list/search/map screen OR the details (bottom navigation).
 * Landscape: navigation rail, the list/search/map screen and the details side by side.
 * All screen state lives in the shared ViewModel, so rotating the phone keeps it.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG_DETAIL = "detail";

    private TrafficViewModel viewModel;
    private boolean twoPane;
    private View contentContainer;
    private View detailContainer;
    private TextView statusBanner;
    private LinearProgressIndicator progress;
    private NavigationBarView navigation;
    private OnBackPressedCallback backCallback;
    private boolean selectingNavItem;   // true while the code (not the user) changes the navigation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        twoPane = getResources().getBoolean(R.bool.two_pane);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        contentContainer = findViewById(R.id.content_container);
        detailContainer = findViewById(R.id.detail_container);
        statusBanner = findViewById(R.id.statusBanner);
        progress = findViewById(R.id.progress);
        navigation = findViewById(R.id.navigation);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        // The details fragment always lives in its own container (shown or hidden as needed)
        if (getSupportFragmentManager().findFragmentByTag(TAG_DETAIL) == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, new SituationDetailFragment(), TAG_DETAIL)
                    .commit();
        }

        navigation.setOnItemSelectedListener(item -> {
            if (selectingNavItem) {
                return true;
            }
            viewModel.setScreen(screenFor(item.getItemId()));
            if (!twoPane) {
                viewModel.clearSelection();   // portrait: leave the details screen
            }
            return true;
        });
        navigation.setOnItemReselectedListener(item -> {
            if (!twoPane) {
                viewModel.clearSelection();
            }
        });

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (!twoPane && viewModel.getSelectedKey().getValue() != null) {
                    viewModel.clearSelection();
                } else {
                    viewModel.setScreen(Screen.LIST);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);

        viewModel.getScreen().observe(this, this::showScreen);
        viewModel.getSelectedKey().observe(this, key -> updatePanes());
        viewModel.getData().observe(this, data -> updateBanner());
        viewModel.getOnline().observe(this, online -> updateBanner());
        viewModel.getLoading().observe(this, isLoading ->
                progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));
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

    // ------------------------------------------------------------------ menu (toolbar)

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            viewModel.clearSelection();
            return true;
        } else if (id == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        } else if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAbout() {
        TrafficData data = viewModel.getData().getValue();
        String lastCheck = data == null ? "-" : DisplayFormat.dateTime(data.getCheckedAtMillis());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_text, lastCheck, viewModel.getRefreshMinutes()))
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    // ------------------------------------------------------------------ screens and panes

    private static Screen screenFor(int menuId) {
        if (menuId == R.id.nav_search) {
            return Screen.SEARCH;
        } else if (menuId == R.id.nav_map) {
            return Screen.MAP;
        }
        return Screen.LIST;
    }

    private static int menuIdFor(Screen screen) {
        switch (screen) {
            case SEARCH:
                return R.id.nav_search;
            case MAP:
                return R.id.nav_map;
            default:
                return R.id.nav_list;
        }
    }

    /** Puts the fragment for the chosen screen in the main container (if it is not there already). */
    private void showScreen(Screen screen) {
        if (navigation.getSelectedItemId() != menuIdFor(screen)) {
            selectingNavItem = true;
            navigation.setSelectedItemId(menuIdFor(screen));
            selectingNavItem = false;
        }
        FragmentManager fm = getSupportFragmentManager();
        Fragment current = fm.findFragmentById(R.id.content_container);
        if (current == null || !screen.name().equals(current.getTag())) {
            fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.content_container, createFragment(screen), screen.name())
                    .commit();
        }
        updatePanes();
    }

    private static Fragment createFragment(Screen screen) {
        switch (screen) {
            case SEARCH:
                return PlaceholderFragment.newInstance(R.string.placeholder_search);
            case MAP:
                return PlaceholderFragment.newInstance(R.string.placeholder_map);
            default:
                return new SituationListFragment();
        }
    }

    /** Portrait: details cover the screen when something is selected. Landscape: both always visible. */
    private void updatePanes() {
        boolean detailOpen = viewModel.getSelectedKey().getValue() != null;
        if (twoPane) {
            contentContainer.setVisibility(View.VISIBLE);
            detailContainer.setVisibility(View.VISIBLE);
        } else {
            contentContainer.setVisibility(detailOpen ? View.GONE : View.VISIBLE);
            detailContainer.setVisibility(detailOpen ? View.VISIBLE : View.GONE);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(!twoPane && detailOpen);
        }
        backCallback.setEnabled((!twoPane && detailOpen) || viewModel.getScreen().getValue() != Screen.LIST);
    }

    /** Red banner when there is no connection or the last download failed. */
    private void updateBanner() {
        TrafficData data = viewModel.getData().getValue();
        boolean online = !Boolean.FALSE.equals(viewModel.getOnline().getValue());
        String message = null;
        if (viewModel.isUsingSampleData()) {
            message = getString(R.string.sample_banner);
        } else if (data == null) {
            message = online ? null : getString(R.string.offline_no_data);
        } else {
            FeedState failed = data.getIncidents().hasError() ? data.getIncidents()
                    : data.getRoadworks().hasError() ? data.getRoadworks() : null;
            if (!online) {
                long saved = savedTime(data);
                message = saved > 0 ? getString(R.string.offline_banner, DisplayFormat.dateTime(saved))
                        : getString(R.string.offline_no_data);
            } else if (failed != null) {
                message = failed.getSavedAtMillis() > 0
                        ? getString(R.string.problem_banner, failed.getErrorMessage(),
                                DisplayFormat.dateTime(failed.getSavedAtMillis()))
                        : failed.getErrorMessage();
            }
        }
        statusBanner.setText(message);
        statusBanner.setVisibility(message == null ? View.GONE : View.VISIBLE);
    }

    private static long savedTime(TrafficData data) {
        return Math.max(data.getIncidents().getSavedAtMillis(), data.getRoadworks().getSavedAtMillis());
    }
}
