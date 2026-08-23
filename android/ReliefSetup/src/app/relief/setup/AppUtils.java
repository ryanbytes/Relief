package app.relief.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

/** Small, side-effect-focused helpers for package discovery and launching. */
public final class AppUtils {
    private AppUtils() {}

    public static boolean isInstalled(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean launch(Context context, String packageName) {
        if (packageName == null) return false;
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) return false;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return true;
    }

    public static void openStore(Activity activity, String packageName) {
        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
        if (isInstalled(activity, AppCatalog.PLAY_STORE)) {
            market.setPackage(AppCatalog.PLAY_STORE);
        }
        try {
            activity.startActivity(market);
        } catch (Exception ignored) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            } catch (Exception e) {
                Toast.makeText(activity, "No app store or browser is available.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void openAppDetails(Activity activity, String packageName) {
        if (!isInstalled(activity, packageName)) {
            Toast.makeText(activity, "Package is not installed.", Toast.LENGTH_LONG).show();
            return;
        }
        activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + packageName)));
    }
}
