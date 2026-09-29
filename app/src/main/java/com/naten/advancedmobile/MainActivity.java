package com.naten.advancedmobile;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private final ArrayList<JSONObject> tasks = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        seedIfEmpty();
        buildUi();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
        if (list != null) refreshList();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String s, float size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size);
        v.setTextColor(0xFF20242A);
        v.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        v.setPadding(dp(12), dp(7), dp(12), dp(7));
        return v;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s); b.setAllCaps(false);
        b.setMinHeight(dp(44));
        return b;
    }

    private void buildUi() {
        ScrollView sc = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(24));
        root.setBackgroundColor(0xFFF6F7F9);
        sc.addView(root);

        TextView title = text("N8N Advanced Mobile", 26, true);
        root.addView(title);
        root.addView(text("Phone-level workflow automation", 14, false));

        LinearLayout serviceCard = new LinearLayout(this);
        serviceCard.setOrientation(LinearLayout.VERTICAL);
        serviceCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        serviceCard.setBackgroundColor(0xFFFFFFFF);
        status = text("", 15, true);
        serviceCard.addView(status);
        serviceCard.addView(text("Accessibility access lets the automation engine read visible UI and perform configured actions.", 13, false));
        Button access = button("Open Accessibility Settings");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        serviceCard.addView(access);
        root.addView(serviceCard);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button add = button("+ New Automation");
        Button stop = button("Stop All");
        actions.addView(add, new LinearLayout.LayoutParams(0, dp(50), 1));
        actions.addView(stop, new LinearLayout.LayoutParams(0, dp(50), 1));
        add.setOnClickListener(v -> showEditor(null));
        stop.setOnClickListener(v -> {
            AutomationStore.clearActive(this);
            AutomationStore.log(this, "All automations stopped by user.");
            refreshList();
        });
        root.addView(actions);

        root.addView(text("Automations", 20, true));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);
        root.addView(text("Core actions: launch app, tap text, type text, swipe, back, home, read screen, check text, wait, notify. Scheduling survives reboot.", 12, false));
        setContentView(sc);
        updateStatus();
        refreshList();
    }

    private void seedIfEmpty() {
        if (!AutomationStore.getTasks(this).isEmpty()) return;
        try {
            JSONObject t = new JSONObject();
            t.put("id", UUID.randomUUID().toString());
            t.put("name", "Read current screen");
            t.put("scheduled", false);
            JSONArray a = new JSONArray();
            a.put(new JSONObject().put("type","READ_SCREEN").put("delayMs",0));
            a.put(new JSONObject().put("type","NOTIFY").put("title","N8N Result").put("text","Screen reading completed.").put("delayMs",1000));
            t.put("steps", a);
            AutomationStore.addTask(this, t);
        } catch (Exception ignored) {}
    }

    private void updateStatus() {
        boolean enabled = false;
        String id = new ComponentName(this, AutomationAccessibilityService.class).flattenToString();
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices != null) enabled = enabledServices.contains(id);
        status.setText(enabled ? "Accessibility service: ENABLED" : "Accessibility service: NOT ENABLED");
        status.setTextColor(enabled ? 0xFF167C3A : 0xFFC43C2C);
    }

    private void refreshList() {
        if (list == null) return;
        list.removeAllViews();
        tasks.clear(); tasks.addAll(AutomationStore.getTasks(this));
        for (JSONObject t : tasks) addTaskCard(t);
    }

    private void addTaskCard(JSONObject t) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackgroundColor(0xFFFFFFFF);
        String name = t.optString("name", "Automation");
        TextView head = text(name, 17, true);
        card.addView(head);
        card.addView(text((t.optBoolean("scheduled",false) ? "Scheduled • " + scheduleText(t) : "Manual trigger") +
            " • " + t.optJSONArray("steps").length() + " steps", 12, false));
        LinearLayout row = new LinearLayout(this);
        Button run = button("Run now");
        Button edit = button("Edit");
        row.addView(run, new LinearLayout.LayoutParams(0, dp(48), 1));
        row.addView(edit, new LinearLayout.LayoutParams(0, dp(48), 1));
        run.setOnClickListener(v -> {
            AutomationStore.setActive(this, t.optString("id"));
            AutomationStore.log(this, "Manual trigger: " + name);
            Toast.makeText(this, "Queued. Accessibility service will execute it.", Toast.LENGTH_SHORT).show();
        });
        edit.setOnClickListener(v -> showEditor(t));
        card.addView(row);
        list.addView(card, new LinearLayout.LayoutParams(-1, -2));
        Space gap = new Space(this); list.addView(gap, new LinearLayout.LayoutParams(1, dp(8)));
    }

    private String scheduleText(JSONObject t) {
        long ms = t.optLong("scheduleAt",0);
        return ms > 0 ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(ms)) : "time not set";
    }

    private void showEditor(JSONObject existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(4));

        EditText name = field("Automation name", existing == null ? "" : existing.optString("name"));
        EditText pkg = field("Target app package (optional, e.g. com.android.chrome)", existing == null ? "" : existing.optString("package"));
        EditText value = field("Text / value", "");
        Spinner action = new Spinner(this);
        String[] choices = {"READ_SCREEN","LAUNCH_APP","TAP_TEXT","TYPE_TEXT","SWIPE_UP","SWIPE_DOWN","BACK","HOME","CHECK_TEXT","NOTIFY","WAIT"};
        action.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, choices));
        form.addView(text("Action step",13,true)); form.addView(action);
        form.addView(value);

        CheckBox scheduled = new CheckBox(this); scheduled.setText("Schedule this automation");
        if (existing != null && existing.optBoolean("scheduled",false)) scheduled.setChecked(true);
        form.addView(scheduled);
        Button time = button("Choose schedule time");
        form.addView(time);
        final long[] scheduleAt = {existing == null ? 0 : existing.optLong("scheduleAt",0)};
        time.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (scheduleAt[0] > System.currentTimeMillis()) c.setTimeInMillis(scheduleAt[0]);
            TimePickerDialog d = new TimePickerDialog(this, (view,h,m) -> {
                Calendar chosen = Calendar.getInstance(); chosen.set(Calendar.HOUR_OF_DAY,h); chosen.set(Calendar.MINUTE,m); chosen.set(Calendar.SECOND,0);
                if (chosen.getTimeInMillis() <= System.currentTimeMillis()) chosen.add(Calendar.DAY_OF_YEAR,1);
                scheduleAt[0] = chosen.getTimeInMillis();
                time.setText("Scheduled: " + DateFormat.getTimeInstance(DateFormat.SHORT).format(chosen.getTime()));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
            d.show();
        });

        if (existing != null && existing.optLong("scheduleAt",0) > System.currentTimeMillis()) {
            time.setText("Scheduled: " + scheduleText(existing));
        }

        AlertDialogWrap dialog = new AlertDialogWrap(this, form, existing == null ? "New Automation" : "Edit Automation");
        dialog.show(() -> {
            try {
                JSONObject t = existing == null ? new JSONObject() : existing;
                if (existing == null) t.put("id", UUID.randomUUID().toString());
                String n = name.getText().toString().trim();
                if (n.isEmpty()) n = "Automation";
                t.put("name", n);
                t.put("package", pkg.getText().toString().trim());
                t.put("scheduled", scheduled.isChecked());
                if (scheduled.isChecked()) {
                    if (scheduleAt[0] == 0) throw new IllegalArgumentException("Choose a schedule time.");
                    t.put("scheduleAt", scheduleAt[0]);
                } else t.put("scheduleAt", 0);
                String at = (String)action.getSelectedItem();
                JSONObject step = new JSONObject().put("type",at).put("delayMs",0);
                if ("LAUNCH_APP".equals(at)) step.put("package",pkg.getText().toString().trim());
                else if ("NOTIFY".equals(at)) step.put("title",n).put("text",value.getText().toString());
                else if (!"READ_SCREEN".equals(at) && !"SWIPE_UP".equals(at) && !"SWIPE_DOWN".equals(at) && !"BACK".equals(at) && !"HOME".equals(at) && !"WAIT".equals(at)) step.put("text",value.getText().toString());
                if ("WAIT".equals(at)) step.put("delayMs", Math.max(0, parseInt(value.getText().toString(),1000)));
                JSONArray arr = new JSONArray(); arr.put(step);
                t.put("steps", arr);

                if (existing == null) AutomationStore.addTask(this,t);
                else AutomationStore.saveTasks(this, tasks);
                if (scheduled.isChecked()) AutomationStore.schedule(this,t); else AutomationStore.cancelSchedule(this,t.optString("id"));
                refreshList();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage() == null ? "Could not save." : e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setText(value); e.setTextSize(14);
        e.setTextColor(0xFF20242A); e.setHintTextColor(0xFF6F747D);
        e.setSingleLine(true); e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setPadding(dp(10),dp(8),dp(10),dp(8));
        return e;
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
    }

    private static class AlertDialogWrap {
        final Activity a; final LinearLayout form; final String title;
        AlertDialogWrap(Activity a, LinearLayout form, String title) { this.a=a; this.form=form; this.title=title; }
        void show(Runnable save) {
            new android.app.AlertDialog.Builder(a).setTitle(title).setView(form)
                .setNegativeButton("Cancel", null).setPositiveButton("Save", (d,w)->save.run()).show();
        }
    }
}
