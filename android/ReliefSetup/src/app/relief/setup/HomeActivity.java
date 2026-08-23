package app.relief.setup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class HomeActivity extends Activity {
    private static final String[] MUSIC = {
            "com.amazon.mp3",
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.pandora.android",
            "org.videolan.vlc"
    };

    private static final String[] WEATHER = {
            "com.acmeaom.android.myradar",
            "de.wetteronline.wetterapp",
            "com.weather.Weather"
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildHome());
    }

    private View buildHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(28), dp(18), dp(20));

        TextView title = new TextView(this);
        title.setText("Relief");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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
        addTile(grid, "MAPS", v -> openMaps());
        addTile(grid, "MUSIC", v -> launchFirst(MUSIC, "No selected music app is installed."));
        addTile(grid, "WEATHER", v -> launchFirst(WEATHER, "No weather app is installed."));
        addTile(grid, "SETUP", v -> startActivity(new Intent(this, MainActivity.class)));

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
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = 0;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        p.setMargins(dp(6), dp(6), dp(6), dp(6));
        grid.addView(b, p);
    }

    private void openMessages() {
        String pkg = Telephony.Sms.getDefaultSmsPackage(this);
        if (pkg != null && launch(pkg)) return;
        Intent sms = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
        startActivity(sms);
    }

    private void openMaps() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="));
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No navigation app is installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void launchFirst(String[] packages, String error) {
        for (String pkg : packages) {
            if (launch(pkg)) return;
        }
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MainActivity.class));
    }

    private boolean launch(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        startActivity(i);
        return true;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
