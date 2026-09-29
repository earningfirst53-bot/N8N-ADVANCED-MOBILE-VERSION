package com.naten.advancedmobile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AutomationStore {
    private static final String PREFS = "naten_advanced";
    private static final String TASKS = "tasks";
    private static final String ACTIVE = "active_task";
    private static final String STEP = "active_step";
    private static final String LOG = "last_log";

    private AutomationStore() {}

    public static List<JSONObject> getTasks(Context context) {
        ArrayList<JSONObject> out = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TASKS, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) out.add(a.getJSONObject(i));
        } catch (Exception ignored) {}
        return out;
    }

    public static void saveTasks(Context context, List<JSONObject> tasks) {
        JSONArray a = new JSONArray();
        for (JSONObject t : tasks) a.put(t);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(TASKS, a.toString()).apply();
    }

    public static void addTask(Context context, JSONObject task) {
        List<JSONObject> tasks = getTasks(context);
        tasks.add(task);
        saveTasks(context, tasks);
    }

    public static void setActive(Context context, String taskId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ACTIVE, taskId).putInt(STEP, 0).apply();
    }

    public static String getActive(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ACTIVE, "");
    }

    public static int getStep(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(STEP, 0);
    }

    public static void setStep(Context context, int step) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(STEP, step).apply();
    }

    public static void clearActive(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ACTIVE, "").putInt(STEP, 0).apply();
    }

    public static void log(Context context, String message) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LOG, message).apply();
    }

    public static String getLastLog(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LOG, "No automation has run yet.");
    }

    public static JSONObject findTask(Context context, String id) {
        for (JSONObject t : getTasks(context)) {
            if (id.equals(t.optString("id"))) return t;
        }
        return null;
    }

    public static void schedule(Context context, JSONObject task) {
        long time = task.optLong("scheduleAt", 0);
        if (time <= System.currentTimeMillis()) return;
        Intent i = new Intent(context, AutomationTriggerReceiver.class).putExtra("taskId", task.optString("id"));
        int request = Math.abs(task.optString("id").hashCode());
        PendingIntent pi = PendingIntent.getBroadcast(context, request, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
    }

    public static void cancelSchedule(Context context, String taskId) {
        Intent i = new Intent(context, AutomationTriggerReceiver.class);
        int request = Math.abs(taskId.hashCode());
        PendingIntent pi = PendingIntent.getBroadcast(context, request, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }
}
