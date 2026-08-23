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
        return preferences.getString(KEY_MAP, AppCatalog.MAPS[0].packageName);
    }

    public String getPrimaryMusic() {
        return preferences.getString(KEY_MUSIC, AppCatalog.MUSIC[0].packageName);
    }

    public String getPrimaryWeather() {
        return preferences.getString(KEY_WEATHER, AppCatalog.WEATHER[0].packageName);
    }

    /** Returns null only when the user explicitly selected no location-sharing app. */
    public String getLocationSharing() {
        String value = preferences.getString(KEY_LOCATION, AppCatalog.LOCATION[0].packageName);
        return NO_APP.equals(value) ? null : value;
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
                .putString(KEY_MAP, mapPackage)
                .putString(KEY_MUSIC, musicPackage)
                .putString(KEY_WEATHER, weatherPackage)
                .putString(KEY_LOCATION, locationPackage == null ? NO_APP : locationPackage)
                .putStringSet(KEY_MESSENGERS, new HashSet<>(messengerPackages))
                .putBoolean(KEY_RCS_CONFIRMED, rcsConfirmed)
                .apply();
    }
}
