package com.saadtopup.smsgateway;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppPrefs {
    private static final String PREF = "saad_gateway_prefs";
    private static final String KEY_ONLINE = "is_online";
    private static final String KEY_LAST_SCAN = "last_auto_scan";
    private static final String KEY_LAST_FOUND = "last_auto_found";

    public static boolean isOnline(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONLINE, true);
    }

    public static void setOnline(Context context, boolean online) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONLINE, online)
                .apply();
    }

    public static void setLastScan(Context context, long timeMs, int foundCount) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SCAN, timeMs)
                .putInt(KEY_LAST_FOUND, foundCount)
                .apply();
    }

    public static long getLastScanTime(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SCAN, 0);
    }

    public static int getLastFound(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getInt(KEY_LAST_FOUND, 0);
    }

    public static String getLastScanText(Context context) {
        long t = getLastScanTime(context);
        if (t <= 0) return "Never";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a", Locale.US);
        return sdf.format(new Date(t));
    }
}
