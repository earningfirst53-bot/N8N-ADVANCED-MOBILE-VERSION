package com.naten.advancedmobile;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class AutomationStore {
    private static final String PREFS = "automation_store";
    private static final String WORKFLOW = "workflow";
    private static final String LOGS = "logs";
    private static final int MAX_LOGS = 80;
    private AutomationStore() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void saveWorkflow(Context c, List<AutomationAction> actions) {
        JSONArray a = new JSONArray();
        try {
            for (AutomationAction action : actions) {
                JSONObject o = new JSONObject();
                o.put("type", action.type);
                o.put("value", action.value == null ? "" : action.value);
                o.put("delay", action.delayMs);
                a.put(o);
            }
        } catch (Exception ignored) {}
        prefs(c).edit().putString(WORKFLOW, a.toString()).apply();
    }

    public static List<AutomationAction> loadWorkflow(Context c) {
        List<AutomationAction> result = new ArrayList<>();
        String raw = prefs(c).getString(WORKFLOW, "");
        if (raw.isEmpty()) return result;
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                result.add(new AutomationAction(o.optString("type"), o.optString("value"), o.optLong("delay", 0)));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static void addLog(Context c, String message) {
        String old = prefs(c).getString(LOGS, "");
        String next = (System.currentTimeMillis() + " | " + message.replace("\n", " ")) + "\n" + old;
        String[] lines = next.split("\n");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_LOGS, lines.length); i++) b.append(lines[i]).append("\n");
        prefs(c).edit().putString(LOGS, b.toString()).apply();
    }

    public static String getLogs(Context c) { return prefs(c).getString(LOGS, ""); }
    public static void clearLogs(Context c) { prefs(c).edit().remove(LOGS).apply(); }
    public static void saveSchedule(Context c, long at) { prefs(c).edit().putLong("schedule", at).apply(); }
    public static long getSchedule(Context c) { return prefs(c).getLong("schedule", 0); }
    public static void clearSchedule(Context c) { prefs(c).edit().remove("schedule").apply(); }

    public static final class AutomationAction {
        public final String type, value;
        public final long delayMs;
        public AutomationAction(String type, String value, long delayMs) {
            this.type = type; this.value = value; this.delayMs = delayMs;
        }
    }
}
