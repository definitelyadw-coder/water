package com.facefit.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WaterWidget extends AppWidgetProvider {
    static final String ACTION_ADD = "com.facefit.app.ADD_WATER";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        render(c, m, ids);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        if (ACTION_ADD.equals(i.getAction())) {
            WidgetData.addGlass(c);
            WidgetData.refreshAll(c);
        } else {
            super.onReceive(c, i);
        }
    }

    static void render(Context c, AppWidgetManager m, int[] ids) {
        WidgetData d = WidgetData.load(c);
        int w = d.water + d.pending;
        Intent add = new Intent(c, WaterWidget.class).setAction(ACTION_ADD);
        PendingIntent addPi = PendingIntent.getBroadcast(c, 1, add,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_water);
            v.setTextViewText(R.id.w_count, w + "/" + d.goal);
            v.setProgressBar(R.id.w_bar, 100, Math.min(100, w * 100 / d.goal), false);
            v.setOnClickPendingIntent(R.id.w_root, WidgetData.openApp(c));
            v.setOnClickPendingIntent(R.id.w_add, addPi);
            m.updateAppWidget(id, v);
        }
    }
}
