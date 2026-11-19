package lt.lifeschedule.app;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;

// Programėlė ↔ Laisvės valdiklis: update (vaizdas, dienos, mygtukai), pull (+1 eilė), pin (pridėti į ekraną).
@CapacitorPlugin(name = "LsWidget")
public class LsWidgetPlugin extends Plugin {

    @PluginMethod
    public void update(PluginCall call) {
        Context c = getContext();
        JSObject d = call.getData();
        try {
            String img = d.getString("img");
            if (img != null) {
                byte[] bytes = Base64.decode(img, Base64.DEFAULT);
                try (FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), "ls_widget.png"))) { out.write(bytes); }
                d.remove("img");
            }
            c.getSharedPreferences(LsWidget.PREFS, Context.MODE_PRIVATE).edit().putString("data", d.toString()).apply();
            LsWidget.updateAll(c);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }

    @PluginMethod
    public void pull(PluginCall call) {
        SharedPreferences sp = getContext().getSharedPreferences(LsWidget.PREFS, Context.MODE_PRIVATE);
        JSObject r = new JSObject();
        try {
            r.put("queue", new JSArray(sp.getString("queue", "[]")));
        } catch (Exception e) {
            r.put("queue", new JSArray());
        }
        sp.edit().putString("queue", "[]").apply();
        call.resolve(r);
    }

    @PluginMethod
    public void pin(PluginCall call) {
        JSObject r = new JSObject();
        boolean ok = false;
        if (Build.VERSION.SDK_INT >= 26) {
            AppWidgetManager m = getContext().getSystemService(AppWidgetManager.class);
            if (m != null && m.isRequestPinAppWidgetSupported()) ok = m.requestPinAppWidget(new ComponentName(getContext(), LsWidget.class), null, null);
        }
        r.put("ok", ok);
        call.resolve(r);
    }
}
