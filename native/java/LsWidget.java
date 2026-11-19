package lt.lifeschedule.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

// Laisvės valdiklis: tako vaizdas su Daigeliu, dienos ir iki 2 mygtukų +1.
// +1 įrašomas į eilę (SharedPreferences "queue"); programėlė ją pasiima per LsWidgetPlugin.pull().
public class LsWidget extends AppWidgetProvider {
    static final String PREFS = "ls_widget";
    static final String PLUS = "lt.lifeschedule.app.WIDGET_PLUS";

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, views(c));
    }

    @Override
    public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        if (!PLUS.equals(i.getAction())) return;
        String id = i.getStringExtra("id");
        int step = i.getIntExtra("step", 1);
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray q = new JSONArray(sp.getString("queue", "[]"));
            JSONObject e = new JSONObject();
            e.put("id", id);
            e.put("t", System.currentTimeMillis());
            e.put("step", step);
            q.put(e);
            JSONObject d = new JSONObject(sp.getString("data", "{}"));
            JSONArray b = d.optJSONArray("btns");
            if (b != null) for (int k = 0; k < b.length(); k++) {
                JSONObject o = b.getJSONObject(k);
                if (o.optString("id").equals(id)) o.put("added", o.optInt("added") + step);
            }
            sp.edit().putString("queue", q.toString()).putString("data", d.toString()).apply();
        } catch (Exception ignored) {
        }
        updateAll(c);
    }

    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, LsWidget.class));
        for (int id : ids) m.updateAppWidget(id, views(c));
    }

    static RemoteViews views(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.ls_widget);
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (open != null) {
            open.putExtra("lsView", "free");
            v.setOnClickPendingIntent(R.id.lsw_root, PendingIntent.getActivity(c, 1, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        }
        int[] bid = {R.id.lsw_b1, R.id.lsw_b2};
        try {
            JSONObject d = new JSONObject(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("data", "{}"));
            if (!d.optBoolean("on")) {
                v.setTextViewText(R.id.lsw_days, d.optString("days", "Laisvė"));
                v.setTextViewText(R.id.lsw_sub, d.optString("sub", "Atidaryk programėlę"));
                v.setImageViewResource(R.id.lsw_img, R.drawable.ls_widget_bg);
                for (int b : bid) v.setViewVisibility(b, View.GONE);
                return v;
            }
            v.setTextViewText(R.id.lsw_days, d.optString("days"));
            v.setTextViewText(R.id.lsw_sub, d.optString("sub"));
            File f = new File(c.getFilesDir(), "ls_widget.png");
            if (f.exists()) {
                Bitmap bm = BitmapFactory.decodeFile(f.getPath());
                if (bm != null) v.setImageViewBitmap(R.id.lsw_img, bm);
            }
            JSONArray b = d.optJSONArray("btns");
            for (int k = 0; k < bid.length; k++) {
                if (b == null || k >= b.length()) { v.setViewVisibility(bid[k], View.GONE); continue; }
                JSONObject o = b.getJSONObject(k);
                int add = o.optInt("added");
                v.setViewVisibility(bid[k], View.VISIBLE);
                v.setTextViewText(bid[k], "+1  " + o.optString("label") + (add > 0 ? "  (+" + add + ")" : ""));
                Intent pi = new Intent(c, LsWidget.class).setAction(PLUS).putExtra("id", o.optString("id")).putExtra("step", o.optInt("step", 1));
                v.setOnClickPendingIntent(bid[k], PendingIntent.getBroadcast(c, 10 + k, pi, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
            }
        } catch (Exception ignored) {
        }
        return v;
    }
}
