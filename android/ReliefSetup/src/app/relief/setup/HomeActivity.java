package app.relief.setup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Minimal HOME activity. It exposes only the functions Relief is meant to provide. */
public final class HomeActivity extends Activity {
    private ReliefPreferences preferences;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = new ReliefPreferences(this);
        setContentView(buildHome());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rebuild so labels and selections immediately reflect setup changes.
        if (preferences != null) setContentView(buildHome());
    }

    private View buildHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(28), dp(18), dp(20));

        TextView title = new TextView(this);
        title.setText("Relief");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setRowCount(3);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        gp.topMargin = dp(22);
        root.addView(grid, gp);

        addTile(grid, "PHONE", v -> startActivity(new Intent(Intent.ACTION_DIAL)));
        addTile(grid, "MESSAGES", v -> openMessages());
        addTile(grid, label("MAPS", AppCatalog.MAPS, preferences.getPrimaryMap()),
                v -> launchPreferred(preferences.getPrimaryMap(), AppCatalog.MAPS, "No navigation app is installed."));
        addTile(grid, label("MUSIC", AppCatalog.MUSIC, preferences.getPrimaryMusic()),
                v -> launchPreferred(preferences.getPrimaryMusic(), AppCatalog.MUSIC, "No music app is installed."));
        addTile(grid, label("WEATHER", AppCatalog.WEATHER, preferences.getPrimaryWeather()),
                v -> launchPreferred(preferences.getPrimaryWeather(), AppCatalog.WEATHER, "No weather app is installed."));
        addTile(grid, "APPS", v -> startActivity(new Intent(this, AppsActivity.class)));

        Button settings = new Button(this);
        settings.setText("Settings");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
        root.addView(settings, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private void addTile(GridLayout grid, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(17);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setOnClickListener(listener);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(6), dp(6), dp(6), dp(6));
        grid.addView(button, params);
    }

    private void openMessages() {
        String packageName = Telephony.Sms.getDefaultSmsPackage(this);
        if (packageName != null && AppUtils.launch(this, packageName)) return;

        try {
            startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")));
        } catch (Exception e) {
            Toast.makeText(this, "No SMS app is available.", Toast.LENGTH_LONG).show();
        }
    }

    private void launchPreferred(String preferred,
                                 AppCatalog.AppChoice[] candidates,
                                 String error) {
        if (preferred != null && AppUtils.launch(this, preferred)) return;
        for (AppCatalog.AppChoice app : candidates) {
            if (AppUtils.launch(this, app.packageName)) return;
        }
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MainActivity.class));
    }

    private String label(String category, AppCatalog.AppChoice[] apps, String packageName) {
        if (packageName == null) return category;
        for (AppCatalog.AppChoice app : apps) {
            if (packageName.equals(app.packageName)) return category + "\n" + app.name;
        }
        return category;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
