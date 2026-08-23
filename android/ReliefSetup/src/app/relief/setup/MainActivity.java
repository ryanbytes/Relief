package app.relief.setup;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final String GRAPHENE_APPS = "app.grapheneos.apps";
    private static final String PLAY_STORE = "com.android.vending";
    private static final String PLAY_SERVICES = "com.google.android.gms";
    private static final String GOOGLE_MESSAGES = "com.google.android.apps.messaging";

    private final List<CheckChoice> optionalChecks = new ArrayList<>();
    private final List<RadioChoice> mapChoices = new ArrayList<>();
    private final List<RadioChoice> weatherChoices = new ArrayList<>();
    private final List<RadioChoice> locationChoices = new ArrayList<>();

    private TextView playStatus;
    private TextView messagesStatus;
    private TextView smsStatus;
    private TextView phonePermissionStatus;
    private CheckBox rcsConfirmed;
    private Button finishButton;
    private TextView installQueueStatus;
    private Button installNextButton;
    private final ArrayList<AppChoice> installQueue = new ArrayList<>();
    private int queueIndex = 0;

    private static final class AppChoice {
        final String name;
        final String packageName;

        AppChoice(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }
    }

    private static final class CheckChoice {
        final AppChoice app;
        final CheckBox view;

        CheckChoice(AppChoice app, CheckBox view) {
            this.app = app;
            this.view = view;
        }
    }

    private static final class RadioChoice {
        final AppChoice app;
        final RadioButton view;

        RadioChoice(AppChoice app, RadioButton view) {
            this.app = app;
            this.view = view;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Relief Setup");
        setContentView(buildUi());
        refreshRequiredStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRequiredStatus();
        refreshInstallQueue();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(48));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView title = text("Relief", 30, true);
        root.addView(title);
        root.addView(text("Set up only the communication, navigation, music, weather and location-sharing apps you want.", 16, false));

        section(root, "RCS — required");
        root.addView(text("Relief requires Google Messages RCS. GrapheneOS supports it through sandboxed Google Play in the Owner profile. Some carriers also require Play services ICC authentication.", 15, false));

        playStatus = statusRow(root, "Sandboxed Google Play");
        Button grapheneApps = button("Open GrapheneOS Apps");
        grapheneApps.setOnClickListener(v -> launchPackage(GRAPHENE_APPS));
        root.addView(grapheneApps);

        messagesStatus = statusRow(root, "Google Messages");
        Button installMessages = button("Install / open Google Messages");
        installMessages.setOnClickListener(v -> openStore(GOOGLE_MESSAGES));
        root.addView(installMessages);

        smsStatus = statusRow(root, "Default SMS app");
        Button makeDefault = button("Make Google Messages default SMS");
        makeDefault.setOnClickListener(v -> requestSmsRole());
        root.addView(makeDefault);

        phonePermissionStatus = statusRow(root, "Play services Phone permission");
        Button playPermissions = button("Open Play services permissions");
        playPermissions.setOnClickListener(v -> openAppDetails(PLAY_SERVICES));
        root.addView(playPermissions);

        root.addView(text("Carrier verification: Settings → Apps → Sandboxed Google Play → Play services special permissions → allow ICC authentication if your carrier requires it.", 14, false));

        Button openMessages = button("Open Messages and verify RCS");
        openMessages.setOnClickListener(v -> launchPackage(GOOGLE_MESSAGES));
        root.addView(openMessages);

        rcsConfirmed = new CheckBox(this);
        rcsConfirmed.setText("I confirmed Messages → Settings → RCS chats shows Connected");
        rcsConfirmed.setTextSize(16);
        rcsConfirmed.setOnCheckedChangeListener((buttonView, isChecked) -> refreshRequiredStatus());
        root.addView(rcsConfirmed);

        section(root, "Messaging");
        addCheck(root, "Signal", "org.thoughtcrime.securesms");
        addCheck(root, "WhatsApp", "com.whatsapp");
        addCheck(root, "Telegram", "org.telegram.messenger");
        addCheck(root, "Messenger", "com.facebook.orca");

        section(root, "Navigation");
        RadioGroup maps = new RadioGroup(this);
        maps.setOrientation(RadioGroup.VERTICAL);
        addRadio(maps, mapChoices, "Google Maps", "com.google.android.apps.maps", true);
        addRadio(maps, mapChoices, "Waze", "com.waze", false);
        addRadio(maps, mapChoices, "Organic Maps", "app.organicmaps", false);
        addRadio(maps, mapChoices, "HERE WeGo", "com.here.app.maps", false);
        root.addView(maps);

        section(root, "Music");
        addCheck(root, "Amazon Music", "com.amazon.mp3");
        addCheck(root, "Spotify", "com.spotify.music");
        addCheck(root, "YouTube Music", "com.google.android.apps.youtube.music");
        addCheck(root, "Pandora", "com.pandora.android");
        addCheck(root, "VLC / local music", "org.videolan.vlc");

        section(root, "Weather alerts");
        RadioGroup weather = new RadioGroup(this);
        weather.setOrientation(RadioGroup.VERTICAL);
        addRadio(weather, weatherChoices, "MyRadar", "com.acmeaom.android.myradar", true);
        addRadio(weather, weatherChoices, "Weather & Radar", "de.wetteronline.wetterapp", false);
        addRadio(weather, weatherChoices, "The Weather Channel", "com.weather.Weather", false);
        root.addView(weather);

        section(root, "Location sharing");
        RadioGroup location = new RadioGroup(this);
        location.setOrientation(RadioGroup.VERTICAL);
        addRadio(location, locationChoices, "Google Maps", "com.google.android.apps.maps", true);
        addRadio(location, locationChoices, "OwnTracks", "org.owntracks.android", false);
        addRadio(location, locationChoices, "None", null, false);
        root.addView(location);

        section(root, "Install selected apps");
        Button prepare = button("Prepare install queue");
        prepare.setOnClickListener(v -> prepareInstallQueue());
        root.addView(prepare);

        installQueueStatus = text("No install queue prepared.", 14, false);
        root.addView(installQueueStatus);
        installNextButton = button("Install next selected app");
        installNextButton.setEnabled(false);
        installNextButton.setOnClickListener(v -> installNext());
        root.addView(installNextButton);

        section(root, "Finish");
        root.addView(text("The Finish button stays locked until sandboxed Google Play and Google Messages are present, Google Messages is the default SMS app, Play services has Phone permission, and you manually confirm RCS is Connected.", 14, false));
        finishButton = button("Finish Relief setup");
        finishButton.setEnabled(false);
        finishButton.setOnClickListener(v -> {
            Toast.makeText(this, "Relief setup complete", Toast.LENGTH_LONG).show();
            finish();
        });
        root.addView(finishButton);

        return scroll;
    }

    private void prepareInstallQueue() {
        installQueue.clear();
        queueIndex = 0;

        addIfMissing(new AppChoice("Google Messages", GOOGLE_MESSAGES));
        for (CheckChoice c : optionalChecks) {
            if (c.view.isChecked()) addIfMissing(c.app);
        }
        addSelectedRadio(mapChoices);
        addSelectedRadio(weatherChoices);
        addSelectedRadio(locationChoices);

        while (queueIndex < installQueue.size() && isInstalled(installQueue.get(queueIndex).packageName)) {
            queueIndex++;
        }
        refreshInstallQueue();
    }

    private void addSelectedRadio(List<RadioChoice> choices) {
        for (RadioChoice c : choices) {
            if (c.view.isChecked() && c.app.packageName != null) {
                addIfMissing(c.app);
                return;
            }
        }
    }

    private void addIfMissing(AppChoice app) {
        if (app.packageName == null || isInstalled(app.packageName)) return;
        for (AppChoice existing : installQueue) {
            if (app.packageName.equals(existing.packageName)) return;
        }
        installQueue.add(app);
    }

    private void installNext() {
        if (!isInstalled(PLAY_STORE)) {
            Toast.makeText(this, "Install sandboxed Google Play from GrapheneOS Apps first.", Toast.LENGTH_LONG).show();
            launchPackage(GRAPHENE_APPS);
            return;
        }
        if (queueIndex >= installQueue.size()) return;
        AppChoice app = installQueue.get(queueIndex);
        openStore(app.packageName);
    }

    private void refreshInstallQueue() {
        if (installQueueStatus == null) return;
        while (queueIndex < installQueue.size() && isInstalled(installQueue.get(queueIndex).packageName)) {
            queueIndex++;
        }
        if (installQueue.isEmpty()) {
            installQueueStatus.setText("No apps waiting to install.");
            installNextButton.setEnabled(false);
        } else if (queueIndex >= installQueue.size()) {
            installQueueStatus.setText("Selected apps are installed.");
            installNextButton.setEnabled(false);
        } else {
            AppChoice next = installQueue.get(queueIndex);
            installQueueStatus.setText("Next: " + next.name + "  (" + (queueIndex + 1) + "/" + installQueue.size() + ")");
            installNextButton.setText("Install " + next.name);
            installNextButton.setEnabled(true);
        }
    }

    private void refreshRequiredStatus() {
        if (playStatus == null) return;
        boolean playStore = isInstalled(PLAY_STORE);
        boolean playServices = isInstalled(PLAY_SERVICES);
        boolean messages = isInstalled(GOOGLE_MESSAGES);
        boolean defaultSms = GOOGLE_MESSAGES.equals(Telephony.Sms.getDefaultSmsPackage(this));
        boolean phonePermission = getPackageManager().checkPermission(
                "android.permission.READ_PHONE_STATE", PLAY_SERVICES) == PackageManager.PERMISSION_GRANTED;

        playStatus.setText(status(playStore && playServices) + " Sandboxed Google Play");
        messagesStatus.setText(status(messages) + " Google Messages");
        smsStatus.setText(status(defaultSms) + " Google Messages is default SMS");
        phonePermissionStatus.setText(status(phonePermission) + " Play services Phone permission");

        finishButton.setEnabled(playStore && playServices && messages && defaultSms && phonePermission && rcsConfirmed.isChecked());
    }

    private String status(boolean ok) {
        return ok ? "✓" : "✗";
    }

    private void requestSmsRole() {
        if (!isInstalled(GOOGLE_MESSAGES)) {
            Toast.makeText(this, "Install Google Messages first.", Toast.LENGTH_LONG).show();
            return;
        }
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS)) {
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_SMS), 10);
        } else {
            Toast.makeText(this, "SMS role is not available.", Toast.LENGTH_LONG).show();
        }
    }

    private void openStore(String packageName) {
        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        market.setPackage(PLAY_STORE);
        try {
            startActivity(market);
        } catch (Exception e) {
            Intent web = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(web);
        }
    }

    private void openAppDetails(String packageName) {
        if (!isInstalled(packageName)) {
            Toast.makeText(this, "Package is not installed yet.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName));
        startActivity(i);
    }

    private void launchPackage(String packageName) {
        Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
        if (i == null) {
            Toast.makeText(this, packageName + " is not installed.", Toast.LENGTH_LONG).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private boolean isInstalled(String packageName) {
        if (packageName == null) return false;
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void addCheck(LinearLayout root, String name, String packageName) {
        CheckBox box = new CheckBox(this);
        box.setText(name + (isInstalled(packageName) ? "  ✓ installed" : ""));
        box.setTextSize(16);
        root.addView(box);
        optionalChecks.add(new CheckChoice(new AppChoice(name, packageName), box));
    }

    private void addRadio(RadioGroup group, List<RadioChoice> choices,
                          String name, String packageName, boolean checked) {
        RadioButton button = new RadioButton(this);
        button.setText(name + (packageName != null && isInstalled(packageName) ? "  ✓ installed" : ""));
        button.setTextSize(16);
        button.setChecked(checked);
        group.addView(button);
        choices.add(new RadioChoice(new AppChoice(name, packageName), button));
    }

    private TextView statusRow(LinearLayout root, String label) {
        TextView v = text("… " + label, 16, true);
        root.addView(v);
        return v;
    }

    private void section(LinearLayout root, String label) {
        TextView v = text(label, 20, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(28);
        p.bottomMargin = dp(8);
        v.setLayoutParams(p);
        root.addView(v);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setLineSpacing(0, 1.12f);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(6);
        p.bottomMargin = dp(4);
        b.setLayoutParams(p);
        return b;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
