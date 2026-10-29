package com.kiranrajagopal.abxtimeout;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** Schedules and shows time-out notifications. The page sends the full list each time data changes. */
public final class Alarms {
    static final String CHANNEL = "timeout";
    private static final String PREFS = "alarms";
    private static final String KEY = "list";

    private Alarms() {}

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(CHANNEL) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "Antibiotic time-out alerts", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Empirical antibiotic at 48 h, and time-outs overdue at 72 h");
                ch.enableVibration(true);
                ch.setVibrationPattern(new long[]{0, 400, 200, 400, 200, 400});
                ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                nm.createNotificationChannel(ch);
            }
        }
    }

    /** Replace all scheduled alarms with this JSON list: [{id, at(ms), title, text}]. */
    static void setAll(Context c, String json) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cancelAll(c, sp.getString(KEY, "[]"));
        sp.edit().putString(KEY, json).apply();
        scheduleAll(c, json);
    }

    static void rescheduleStored(Context c) {
        scheduleAll(c, c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
    }

    private static PendingIntent pi(Context c, String id, String title, String text) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction("com.kiranrajagopal.abxtimeout.ALARM." + id);
        i.putExtra("id", id);
        i.putExtra("title", title);
        i.putExtra("text", text);
        return PendingIntent.getBroadcast(c, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void cancelAll(Context c, String json) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            JSONArray a = new JSONArray(json);
            for (int k = 0; k < a.length(); k++) {
                JSONObject o = a.getJSONObject(k);
                am.cancel(pi(c, o.getString("id"), o.optString("title"), o.optString("text")));
            }
        } catch (Exception ignored) {}
    }

    private static void scheduleAll(Context c, String json) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            long now = System.currentTimeMillis();
            JSONArray a = new JSONArray(json);
            for (int k = 0; k < a.length(); k++) {
                JSONObject o = a.getJSONObject(k);
                long at = o.getLong("at");
                if (at < now - 60_000) continue;
                PendingIntent p = pi(c, o.getString("id"), o.optString("title"), o.optString("text"));
                boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p);
            }
        } catch (Exception ignored) {}
    }

    static void show(Context c, String id, String title, String text) {
        ensureChannel(c);
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        Notification pub = (Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c))
            .setSmallIcon(R.drawable.ic_stat_iv).setContentTitle("Antibiotic time-out").setContentText("A course needs review").build();
        b.setSmallIcon(R.drawable.ic_stat_iv)
            .setColor(0xFF7A2BB8)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(pub)
            .setAutoCancel(true)
            .setContentIntent(tap);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        try { nm.notify(id.hashCode(), b.build()); } catch (SecurityException ignored) {}
    }
}
