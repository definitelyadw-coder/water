package com.facefit.app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.RemoteViews;

// Картинка уже содержит текст: не всё сделано -> стакан «Полуксмаксим?»,
// всё сделано -> календарь «Жду вас завтра!». Число дней рисуем справа от огонька.
public class StreakWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        render(c, m, ids);
    }

    static void render(Context c, AppWidgetManager m, int[] ids) {
        WidgetData d = WidgetData.load(c);
        Bitmap bmp = compose(c, d.streak, d.done);
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_streak);
            v.setImageViewBitmap(R.id.w_img, bmp);
            v.setOnClickPendingIntent(R.id.w_root, WidgetData.openApp(c));
            m.updateAppWidget(id, v);
        }
    }

    static Bitmap compose(Context c, int streak, boolean done) {
        Resources r = c.getResources();
        Bitmap src = BitmapFactory.decodeResource(r, done ? R.drawable.w_bg_done : R.drawable.w_bg_glass);
        Bitmap out = src.copy(Bitmap.Config.ARGB_8888, true);
        float W = out.getWidth(), H = out.getHeight();

        String s = String.valueOf(Math.max(0, Math.min(streak, 9999)));
        Bitmap[] ds = new Bitmap[s.length()];
        float[] w = new float[s.length()];
        float dh = H * 0.15f, total = 0;
        for (int i = 0; i < s.length(); i++) {
            int rid = r.getIdentifier("w_d" + s.charAt(i), "drawable", c.getPackageName());
            ds[i] = BitmapFactory.decodeResource(r, rid);
            w[i] = ds[i].getWidth() * dh / ds[i].getHeight() * 0.92f;
            total += w[i];
        }
        float maxW = W * 0.40f;
        if (total > maxW) {
            float k = maxW / total;
            dh *= k;
            total *= k;
            for (int i = 0; i < w.length; i++) w[i] *= k;
        }

        // Огонёк: правый край и вертикальный центр (доли картинки)
        float flameRight = done ? 0.447f : 0.430f;
        float cy = H * (done ? 0.137f : 0.135f);
        float x = W * (flameRight + 0.028f);

        Canvas cv = new Canvas(out);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < ds.length; i++) {
            float full = w[i] / 0.92f;
            float off = (full - w[i]) / 2f;
            cv.drawBitmap(ds[i], null, new RectF(x - off, cy - dh / 2, x - off + full, cy + dh / 2), p);
            x += w[i];
        }
        return out;
    }
}
