package app.relief.setup;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** First-run and reconfiguration screen for the Relief launcher. */
public final class MainActivity extends Activity {
    private final List<CheckChoice> messengerChoices = new ArrayList<>();
    private final List<RadioChoice> mapChoices = new ArrayList<>();
    private final List<RadioChoice> musicChoices = new ArrayList<>();
    private final List<RadioChoice> weatherChoices = new ArrayList<>();
    private final List<RadioChoice> locationChoices = new ArrayList<>();
    private final ArrayList<AppCatalog.AppChoice> installQueue = new ArrayList<>();

    private ReliefPreferences preferences;
    private TextView platformStatus;
    private TextView playStatus;
    private TextView messagesStatus;
    private TextView smsStatus;
    private TextView phonePermissionStatus;
    private CheckBox rcsConfirmed;
    private Button finishButton;
    private TextView installQueueStatus;
    private Button installNextButton;
    private int queueIndex;

    private static final class CheckChoice {
        final AppCatalog.AppChoice app;
        final CheckBox view;

        CheckChoice(AppCatalog.AppChoice app, CheckBox view) {
            this.app = app;
            this.view = view;
        }
    }

    private static final class RadioChoice {
        final AppCatalog.AppChoice app;
        final RadioButton view;

        RadioChoice(AppCatalog.AppChoice app, RadioButton view) {
            this.app = app;
            this.view = view;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = new ReliefPreferences(this);
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
        applySystemBarInsets(scroll);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(48));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        root.addView(text("Relief", 30, true));
        root.addView(text("A minimal launcher for calls, RCS/SMS, messaging, navigation, music, weather alerts and location sharing.", 16, false));

        section(root, "RCS — required");
        platformStatus = statusRow(root, "Android environment");
        root.addView(text("Relief uses Google Messages for RCS. Actual RCS registration is controlled by Google Messages, the carrier and the device; Android does not expose a public API for Relief to read that registration state.", 14, false));

        playStatus = statusRow(root, "Google Play services");
        Button grapheneApps = button("Open GrapheneOS Apps");
        grapheneApps.setOnClickListener(v -> launchGrapheneApps());
        root.addView(grapheneApps);

        messagesStatus = statusRow(root, "Google Messages");
        Button installMessages = button("Install / open Google Messages");
        installMessages.setOnClickListener(v -> installOrLaunch(AppCatalog.GOOGLE_MESSAGES));
        root.addView(installMessages);

        smsStatus = statusRow(root, "Default SMS app");
        Button makeDefault = button("Make Google Messages default SMS");
        makeDefault.setOnClickListener(v -> requestSmsRole());
        root.addView(makeDefault);

        phonePermissionStatus = statusRow(root, "GrapheneOS Play services Phone permission");
        Button playPermissions = button("Open Play services permissions");
        playPermissions.setOnClickListener(v -> AppUtils.openAppDetails(this, AppCatalog.PLAY_SERVICES));
        root.addView(playPermissions);

        Button openMessages = button("Open Messages and verify RCS");
        openMessages.setOnClickListener(v -> {
            if (!AppUtils.launch(this, AppCatalog.GOOGLE_MESSAGES)) {
                AppUtils.openStore(this, AppCatalog.GOOGLE_MESSAGES);
            }
        });
        root.addView(openMessages);

        rcsConfirmed = new CheckBox(this);
        rcsConfirmed.setText("I confirmed Google Messages → Settings → RCS chats shows Connected");
        rcsConfirmed.setTextSize(16);
        rcsConfirmed.setChecked(preferences.isRcsConfirmed());
        rcsConfirmed.setOnCheckedChangeListener((buttonView, isChecked) -> refreshRequiredStatus());
        root.addView(rcsConfirmed);

        section(root, "Messaging");
        Set<String> selectedMessengers = preferences.getMessengers();
        for (AppCatalog.AppChoice app : AppCatalog.MESSENGERS) {
            addCheck(root, app, selectedMessengers.contains(app.packageName));
        }

        section(root, "Navigation");
        RadioGroup maps = new RadioGroup(this);
        maps.setOrientation(RadioGroup.VERTICAL);
        addRadios(maps, mapChoices, AppCatalog.MAPS, preferences.getPrimaryMap());
        root.addView(maps);

        section(root, "Music");
        RadioGroup music = new RadioGroup(this);
        music.setOrientation(RadioGroup.VERTICAL);
        addRadios(music, musicChoices, AppCatalog.MUSIC, preferences.getPrimaryMusic());
        root.addView(music);

        section(root, "Weather alerts");
        RadioGroup weather = new RadioGroup(this);
        weather.setOrientation(RadioGroup.VERTICAL);
        addRadios(weather, weatherChoices, AppCatalog.WEATHER, preferences.getPrimaryWeather());
        root.addView(weather);

        section(root, "Location sharing");
        RadioGroup location = new RadioGroup(this);
        location.setOrientation(RadioGroup.VERTICAL);
        addRadios(location, locationChoices, AppCatalog.LOCATION, preferences.getLocationSharing());
        root.addView(location);

        section(root, "Install selected apps");
        Button prepare = button("Save choices and prepare install queue");
        prepare.setOnClickListener(v -> {
            saveSelections();
            prepareInstallQueue();
        });
        root.addView(prepare);

        installQueueStatus = text("No install queue prepared.", 14, false);
        root.addView(installQueueStatus);
        installNextButton = button("Install next selected app");
        installNextButton.setEnabled(false);
        installNextButton.setOnClickListener(v -> installNext());
        root.addView(installNextButton);

        section(root, "Launcher");
        Button homeSettings = button("Set Relief as default Home app");
        homeSettings.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Open Android Settings → Apps → Default apps → Home app.", Toast.LENGTH_LONG).show();
            }
        });
        root.addView(homeSettings);

        section(root, "Finish");
        root.addView(text("Finish stays locked until Google Messages and Play services are present, Google Messages is the default SMS app, and you confirm RCS is Connected. GrapheneOS additionally requires Play services Phone permission.", 14, false));
        finishButton = button("Finish Relief setup");
        finishButton.setEnabled(false);
        finishButton.setOnClickListener(v -> {
            saveSelections();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        root.addView(finishButton);

        return scroll;
    }

    private void saveSelections() {
        Set<String> messengers = new HashSet<>();
        for (CheckChoice choice : messengerChoices) {
            if (choice.view.isChecked()) messengers.add(choice.app.packageName);
        }
        preferences.save(
                selectedPackage(mapChoices),
                selectedPackage(musicChoices),
                selectedPackage(weatherChoices),
                selectedPackage(locationChoices),
                messengers,
                rcsConfirmed.isChecked());
    }

    private void prepareInstallQueue() {
        installQueue.clear();
        queueIndex = 0;

        addIfMissing(new AppCatalog.AppChoice("Google Messages", AppCatalog.GOOGLE_MESSAGES));
        for (CheckChoice choice : messengerChoices) {
            if (choice.view.isChecked()) addIfMissing(choice.app);
        }
        addSelectedRadio(mapChoices);
        addSelectedRadio(musicChoices);
        addSelectedRadio(weatherChoices);
        addSelectedRadio(locationChoices);
        refreshInstallQueue();
    }

    private void addSelectedRadio(List<RadioChoice> choices) {
        for (RadioChoice choice : choices) {
            if (choice.view.isChecked() && choice.app.packageName != null) {
                addIfMissing(choice.app);
                return;
            }
        }
    }

    private void addIfMissing(AppCatalog.AppChoice app) {
        if (app.packageName == null || AppUtils.isInstalled(this, app.packageName)) return;
        for (AppCatalog.AppChoice existing : installQueue) {
            if (app.packageName.equals(existing.packageName)) return;
        }
        installQueue.add(app);
    }

    private void installNext() {
        while (queueIndex < installQueue.size()
                && AppUtils.isInstalled(this, installQueue.get(queueIndex).packageName)) {
            queueIndex++;
        }
        if (queueIndex >= installQueue.size()) {
            refreshInstallQueue();
            return;
        }
        AppCatalog.AppChoice app = installQueue.get(queueIndex);
        AppUtils.openStore(this, app.packageName);
    }

    private void refreshInstallQueue() {
        if (installQueueStatus == null || installNextButton == null) return;
        while (queueIndex < installQueue.size()
                && AppUtils.isInstalled(this, installQueue.get(queueIndex).packageName)) {
            queueIndex++;
        }
        if (installQueue.isEmpty()) {
            installQueueStatus.setText("No apps waiting to install.");
            installNextButton.setEnabled(false);
        } else if (queueIndex >= installQueue.size()) {
            installQueueStatus.setText("Selected apps are installed.");
            installNextButton.setEnabled(false);
        } else {
            AppCatalog.AppChoice next = installQueue.get(queueIndex);
            installQueueStatus.setText("Next: " + next.name + " (" + (queueIndex + 1) + "/" + installQueue.size() + ")");
            installNextButton.setText("Install " + next.name);
            installNextButton.setEnabled(true);
        }
    }

    private void refreshRequiredStatus() {
        if (playStatus == null) return;

        boolean graphene = AppUtils.isInstalled(this, AppCatalog.GRAPHENE_APPS);
        boolean playServices = AppUtils.isInstalled(this, AppCatalog.PLAY_SERVICES);
        boolean messages = AppUtils.isInstalled(this, AppCatalog.GOOGLE_MESSAGES);
        boolean defaultSms = AppCatalog.GOOGLE_MESSAGES.equals(Telephony.Sms.getDefaultSmsPackage(this));
        boolean phonePermission = playServices && getPackageManager().checkPermission(
                "android.permission.READ_PHONE_STATE", AppCatalog.PLAY_SERVICES) == PackageManager.PERMISSION_GRANTED;

        platformStatus.setText("✓ " + (graphene ? "GrapheneOS-compatible environment detected" : "Standard Android environment"));
        playStatus.setText(status(playServices) + " Google Play services");
        messagesStatus.setText(status(messages) + " Google Messages");
        smsStatus.setText(status(defaultSms) + " Google Messages is default SMS");

        if (graphene) {
            phonePermissionStatus.setVisibility(View.VISIBLE);
            phonePermissionStatus.setText(status(phonePermission) + " Play services Phone permission");
        } else {
            phonePermissionStatus.setVisibility(View.GONE);
        }

        boolean required = playServices && messages && defaultSms && rcsConfirmed.isChecked();
        if (graphene) required = required && phonePermission;
        finishButton.setEnabled(required);
    }

    private String status(boolean ok) {
        return ok ? "✓" : "✗";
    }

    private void requestSmsRole() {
        if (!AppUtils.isInstalled(this, AppCatalog.GOOGLE_MESSAGES)) {
            Toast.makeText(this, "Install Google Messages first.", Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS));
                return;
            }
        }

        Intent legacy = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        legacy.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, AppCatalog.GOOGLE_MESSAGES);
        try {
            startActivity(legacy);
        } catch (Exception e) {
            Toast.makeText(this, "Open Android Settings → Apps → Default apps → SMS app.", Toast.LENGTH_LONG).show();
        }
    }

    private void installOrLaunch(String packageName) {
        if (!AppUtils.launch(this, packageName)) AppUtils.openStore(this, packageName);
    }

    private void launchGrapheneApps() {
        if (!AppUtils.launch(this, AppCatalog.GRAPHENE_APPS)) {
            Toast.makeText(this, "GrapheneOS Apps is only present on GrapheneOS. On standard Android, install Google Messages from your app store.", Toast.LENGTH_LONG).show();
        }
    }

    private void addCheck(LinearLayout root, AppCatalog.AppChoice app, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(app.name + installedSuffix(app.packageName));
        box.setTextSize(16);
        box.setChecked(checked);
        root.addView(box);
        messengerChoices.add(new CheckChoice(app, box));
    }

    /**
     * Adds a mutually-exclusive category. Radio buttons are attached to their RadioGroup
     * before the checked item is applied. Setting checked state before attachment can leave
     * multiple children visually checked because the group has not started tracking them yet.
     */
    private void addRadios(RadioGroup group,
                           List<RadioChoice> destination,
                           AppCatalog.AppChoice[] apps,
                           String selectedPackage) {
        int selectedId = View.NO_ID;

        for (AppCatalog.AppChoice app : apps) {
            RadioButton button = new RadioButton(this);
            button.setId(View.generateViewId());
            button.setText(app.name + installedSuffix(app.packageName));
            button.setTextSize(16);
            group.addView(button);
            destination.add(new RadioChoice(app, button));

            if (selectedId == View.NO_ID && packagesEqual(selectedPackage, app.packageName)) {
                selectedId = button.getId();
            }
        }

        if (selectedId == View.NO_ID && !destination.isEmpty()) {
            selectedId = destination.get(0).view.getId();
        }
        if (selectedId != View.NO_ID) {
            group.check(selectedId);
        }
    }

    private String selectedPackage(List<RadioChoice> choices) {
        for (RadioChoice choice : choices) {
            if (choice.view.isChecked()) return choice.app.packageName;
        }
        return null;
    }

    private boolean packagesEqual(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private String installedSuffix(String packageName) {
        return packageName != null && AppUtils.isInstalled(this, packageName) ? "  ✓ installed" : "";
    }

    private TextView statusRow(LinearLayout root, String label) {
        TextView view = text("… " + label, 16, true);
        root.addView(view);
        return view;
    }

    private void section(LinearLayout root, String label) {
        TextView view = text(label, 20, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(28);
        params.bottomMargin = dp(8);
        view.setLayoutParams(params);
        root.addView(view);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setLineSpacing(0, 1.12f);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        params.bottomMargin = dp(4);
        button.setLayoutParams(params);
        return button;
    }

    private void applySystemBarInsets(View view) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            int left;
            int top;
            int right;
            int bottom;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }

            v.setPadding(left, top, right, bottom);
            return insets;
        });
        view.requestApplyInsets();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
