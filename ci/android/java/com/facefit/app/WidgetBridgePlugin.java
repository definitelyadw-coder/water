package com.facefit.app;

import android.content.SharedPreferences;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "WidgetBridge")
public class WidgetBridgePlugin extends Plugin {

    @PluginMethod
    public void update(PluginCall call) {
        SharedPreferences.Editor e = WidgetData.prefs(getContext()).edit();
        e.putInt("streak", call.getInt("streak", 0));
        e.putInt("water", call.getInt("water", 0));
        e.putInt("goal", call.getInt("goal", 8));
        e.putBoolean("done", call.getBoolean("done", false));
        e.putString("date", call.getString("date", ""));
        e.apply();
        WidgetData.refreshAll(getContext());
        call.resolve();
    }

    @PluginMethod
    public void takePending(PluginCall call) {
        SharedPreferences p = WidgetData.prefs(getContext());
        int n = p.getInt("pending", 0);
        p.edit().putInt("pending", 0).apply();
        JSObject r = new JSObject();
        r.put("n", n);
        call.resolve(r);
    }
}
