package app.relief.setup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Central catalog of apps Relief knows how to install and launch. */
public final class AppCatalog {
    private AppCatalog() {}

    public static final String GRAPHENE_APPS = "app.grapheneos.apps";
    public static final String PLAY_STORE = "com.android.vending";
    public static final String PLAY_SERVICES = "com.google.android.gms";
    public static final String GOOGLE_MESSAGES = "com.google.android.apps.messaging";

    public static final AppChoice[] MESSENGERS = {
            new AppChoice("Signal", "org.thoughtcrime.securesms"),
            new AppChoice("WhatsApp", "com.whatsapp"),
            new AppChoice("Telegram", "org.telegram.messenger"),
            new AppChoice("Messenger", "com.facebook.orca")
    };

    public static final AppChoice[] MAPS = {
            new AppChoice("Google Maps", "com.google.android.apps.maps"),
            new AppChoice("Waze", "com.waze"),
            new AppChoice("Organic Maps", "app.organicmaps"),
            new AppChoice("HERE WeGo", "com.here.app.maps"),
            new AppChoice("None", null)
    };

    public static final AppChoice[] MUSIC = {
            new AppChoice("Amazon Music", "com.amazon.mp3"),
            new AppChoice("Spotify", "com.spotify.music"),
            new AppChoice("YouTube Music", "com.google.android.apps.youtube.music"),
            new AppChoice("Pandora", "com.pandora.android"),
            new AppChoice("VLC / local music", "org.videolan.vlc"),
            new AppChoice("None", null)
    };

    public static final AppChoice[] WEATHER = {
            new AppChoice("MyRadar", "com.acmeaom.android.myradar"),
            new AppChoice("Weather & Radar", "de.wetteronline.wetterapp"),
            new AppChoice("The Weather Channel", "com.weather.Weather"),
            new AppChoice("None", null)
    };

    public static final AppChoice[] LOCATION = {
            new AppChoice("Google Maps", "com.google.android.apps.maps"),
            new AppChoice("OwnTracks", "org.owntracks.android"),
            new AppChoice("None", null)
    };

    public static final class AppChoice {
        public final String name;
        public final String packageName;

        public AppChoice(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }
    }

    public static List<AppChoice> asList(AppChoice[] values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }
}
