package app.relief.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/** Shows only the extra apps the user deliberately selected in Relief. */
public final class AppsActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Relief apps");
        setContentView(buildUi());
    }

    private View buildUi() {
        ReliefPreferences prefs = new ReliefPreferences(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Apps");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        Set<String> selected = prefs.getMessengers();
        for (AppCatalog.AppChoice app : AppCatalog.MESSENGERS) {
            if (selected.contains(app.packageName)) {
                root.addView(appButton(app.name, app.packageName));
            }
        }

        String location = prefs.getLocationSharing();
        if (location != null) {
            String name = nameForPackage(AppCatalog.LOCATION, location, "Location sharing");
            root.addView(appButton(name, location));
        }

        if (selected.isEmpty() && location == null) {
            TextView empty = new TextView(this);
            empty.setText("No extra apps selected.");
            empty.setTextSize(16);
            empty.setPadding(0, dp(18), 0, dp(18));
            root.addView(empty);
        }

        Button setup = new Button(this);
        setup.setText("Relief setup");
        setup.setAllCaps(false);
        setup.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        root.addView(setup);

        Button settings = new Button(this);
        settings.setText("Android settings");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
        root.addView(settings);

        return scroll;
    }

    private Button appButton(String label, String packageName) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> {
            if (!AppUtils.launch(this, packageName)) {
                Toast.makeText(this, label + " is not installed.", Toast.LENGTH_LONG).show();
                AppUtils.openStore(this, packageName);
            }
        });
        return button;
    }

    private String nameForPackage(AppCatalog.AppChoice[] apps, String packageName, String fallback) {
        for (AppCatalog.AppChoice app : apps) {
            if (packageName.equals(app.packageName)) return app.name;
        }
        return fallback;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
