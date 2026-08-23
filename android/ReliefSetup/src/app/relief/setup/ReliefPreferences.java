package app.relief.setup;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Single source of truth for user-selected Relief apps. */
public final class ReliefPreferences {
    private static final String PREFS = "relief_preferences";
    private static final String KEY_MAP = "primary_map";
    private static final String KEY_MUSIC = "primary_music";
    private static final String KEY_WEATHER = "primary_weather";
    private static final String KEY_LOCATION = "location_sharing";
    private static final String KEY_MESSENGERS = "messengers";
    private static final String KEY_RCS_CONFIRMED = "rcs_confirmed";
    private static final String NO_APP = "__relief_none__";

    private final SharedPreferences preferences;

    public ReliefPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getPrimaryMap() {
        return getOptionalSelection(KEY_MAP, AppCatalog.MAPS[0].packageName);
    }

    public String getPrimaryMusic() {
        return getOptionalSelection(KEY_MUSIC, AppCatalog.MUSIC[0].packageName);
    }

    public String getPrimaryWeather() {
        return getOptionalSelection(KEY_WEATHER, AppCatalog.WEATHER[0].packageName);
    }

    public String getLocationSharing() {
        return getOptionalSelection(KEY_LOCATION, AppCatalog.LOCATION[0].packageName);
    }

    public Set<String> getMessengers() {
        Set<String> stored = preferences.getStringSet(KEY_MESSENGERS, Collections.emptySet());
        return new HashSet<>(stored);
    }

    public boolean isRcsConfirmed() {
        return preferences.getBoolean(KEY_RCS_CONFIRMED, false);
    }

    public void save(String mapPackage,
                     String musicPackage,
                     String weatherPackage,
                     String locationPackage,
                     Set<String> messengerPackages,
                     boolean rcsConfirmed) {
        preferences.edit()
                .putString(KEY_MAP, encodeOptionalSelection(mapPackage))
                .putString(KEY_MUSIC, encodeOptionalSelection(musicPackage))
                .putString(KEY_WEATHER, encodeOptionalSelection(weatherPackage))
                .putString(KEY_LOCATION, encodeOptionalSelection(locationPackage))
                .putStringSet(KEY_MESSENGERS, new HashSet<>(messengerPackages))
                .putBoolean(KEY_RCS_CONFIRMED, rcsConfirmed)
                .apply();
    }

    private String getOptionalSelection(String key, String defaultPackage) {
        String value = preferences.getString(key, defaultPackage);
        return NO_APP.equals(value) ? null : value;
    }

    private String encodeOptionalSelection(String packageName) {
        return packageName == null ? NO_APP : packageName;
    }
}
