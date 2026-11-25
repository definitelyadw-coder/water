package com.facefit.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class WidgetData {
    static final String PREFS = "facefit_widget";
    int streak, water, pending, goal = 8;
    boolean done;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String day(int back) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -back);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    // Состояние с учётом смены дня (приложение могло не открываться)
    static WidgetData load(Context c) {
        SharedPreferences p = prefs(c);
        WidgetData d = new WidgetData();
        d.goal = Math.max(1, p.getInt("goal", 8));
        String date = p.getString("date", "");
        int streak = p.getInt("streak", 0);
        boolean done = p.getBoolean("done", false);
        if (date.equals(day(0))) {
            d.streak = streak;
            d.done = done;
            d.water = p.getInt("water", 0);
            d.pending = p.getInt("pending", 0);
        } else if (date.equals(day(1))) {
            d.streak = done ? streak : 0;
        }
        return d;
    }

    static void addGlass(Context c) {
        WidgetData d = load(c);
        prefs(c).edit()
            .putString("date", day(0))
            .putInt("streak", d.streak)
            .putBoolean("done", d.done)
            .putInt("water", d.water)
            .putInt("goal", d.goal)
            .putInt("pending", d.pending + 1)
            .apply();
    }

    static PendingIntent openApp(Context c) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        return PendingIntent.getActivity(c, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] a = m.getAppWidgetIds(new ComponentName(c, StreakWidget.class));
        if (a.length > 0) StreakWidget.render(c, m, a);
        int[] b = m.getAppWidgetIds(new ComponentName(c, WaterWidget.class));
        if (b.length > 0) WaterWidget.render(c, m, b);
    }
}
