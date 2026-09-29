package com.naten.advancedmobile;

import android.accessibilityservice.AccessibilityService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class AutomationAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean busy = false;
    private static AutomationAccessibilityService instance;

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AutomationStore.log(this, "Accessibility service connected.");
        handler.postDelayed(runner, 300);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && !busy) handler.post(runner);
    }

    @Override public void onDestroy() {
        instance = null;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onInterrupt() {
        AutomationStore.log(this, "Accessibility service interrupted.");
        busy = false;
    }

    public static void kick() {
        if (instance != null) instance.handler.post(instance.runner);
    }

    private final Runnable runner = new Runnable() {
        @Override public void run() {
            if (busy) return;
            String id = AutomationStore.getActive(AutomationAccessibilityService.this);
            if (id == null || id.isEmpty()) {
                handler.postDelayed(this, 1000);
                return;
            }

            JSONObject task = AutomationStore.findTask(AutomationAccessibilityService.this, id);
            if (task == null) {
                AutomationStore.clearActive(AutomationAccessibilityService.this);
                handler.postDelayed(this, 1000);
                return;
            }

            busy = true;
            executeNext(task);
        }
    };

    private void executeNext(JSONObject task) {
        try {
            JSONArray steps = task.optJSONArray("steps");
            int index = AutomationStore.getStep(this);

            if (steps == null || index >= steps.length()) {
                AutomationStore.log(this, "Completed: " + task.optString("name", "Automation"));
                AutomationStore.clearActive(this);
                busy = false;
                handler.postDelayed(runner, 1000);
                return;
            }

            JSONObject step = steps.getJSONObject(index);
            String type = step.optString("type");
            long delay = Math.max(0, step.optLong("delayMs", 0));

            Runnable action = () -> {
                boolean ok = performStep(type, step);
                if (!ok) {
                    AutomationStore.log(this, "Stopped: step " + (index + 1) + " (" + type + ") could not be completed.");
                    AutomationStore.clearActive(this);
                    busy = false;
                    handler.postDelayed(runner, 1000);
                    return;
                }
                AutomationStore.setStep(this, index + 1);
                busy = false;
                handler.postDelayed(runner, 250);
            };

            handler.postDelayed(action, delay);
        } catch (Exception e) {
            AutomationStore.log(this, "Error: " + e.getClass().getSimpleName());
            AutomationStore.clearActive(this);
            busy = false;
            handler.postDelayed(runner, 1000);
        }
    }

    private boolean performStep(String type, JSONObject step) {
        switch (type) {
            case "LAUNCH_APP":
                return launchPackage(step.optString("package"));
            case "TAP_TEXT":
                return tapText(step.optString("text"));
            case "TYPE_TEXT":
                return typeText(step.optString("text"));
            case "SWIPE_UP":
                return performSwipe(false);
            case "SWIPE_DOWN":
                return performSwipe(true);
            case "BACK":
                return performGlobalAction(GLOBAL_ACTION_BACK);
            case "HOME":
                return performGlobalAction(GLOBAL_ACTION_HOME);
            case "READ_SCREEN":
                String screen = readScreenText();
                String preview = screen.isEmpty() ? "No accessible text found." : screen.substring(0, Math.min(400, screen.length()));
                notifyUser("Screen text captured", preview);
                AutomationStore.log(this, "Read screen: " + (screen.isEmpty() ? "no text" : screen.substring(0, Math.min(160, screen.length()))));
                return true;
            case "CHECK_TEXT":
                boolean found = findText(step.optString("text")) != null;
                AutomationStore.log(this, found ? "Condition true: " + step.optString("text") : "Condition false: " + step.optString("text"));
                return found;
            case "NOTIFY":
                notifyUser(step.optString("title", "N8N Automation"), step.optString("text", "Automation reached this step."));
                return true;
            case "WAIT":
                return true;
            default:
                AutomationStore.log(this, "Unknown action: " + type);
                return false;
        }
    }

    private boolean launchPackage(String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) return false;
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg.trim());
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception e) {
            AutomationStore.log(this, "Launch error: " + e.getClass().getSimpleName());
            return false;
        }
    }

    private AccessibilityNodeInfo findText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || text == null || text.trim().isEmpty()) return null;
        String needle = text.trim();

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(needle);
        if (nodes != null) {
            for (AccessibilityNodeInfo n : nodes) if (n != null) return n;
        }
        return findTextRecursive(root, needle.toLowerCase());
    }

    private AccessibilityNodeInfo findTextRecursive(AccessibilityNodeInfo node, String needle) {
        if (node == null) return null;
        CharSequence a = node.getText();
        CharSequence d = node.getContentDescription();
        if ((a != null && a.toString().toLowerCase().contains(needle)) ||
            (d != null && d.toString().toLowerCase().contains(needle))) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findTextRecursive(node.getChild(i), needle);
            if (found != null) return found;
        }
        return null;
    }

    private boolean tapText(String text) {
        AccessibilityNodeInfo node = findText(text);
        if (node == null) return false;

        AccessibilityNodeInfo clickable = node;
        while (clickable != null && !clickable.isClickable()) clickable = clickable.getParent();
        if (clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;

        Rect r = new Rect();
        node.getBoundsInScreen(r);
        if (Build.VERSION.SDK_INT >= 24 && r.width() > 0 && r.height() > 0) {
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(r.centerX(), r.centerY());
            android.accessibilityservice.GestureDescription gesture =
                new android.accessibilityservice.GestureDescription.Builder()
                    .addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 80))
                    .build();
            return dispatchGesture(gesture, null, null);
        }
        return false;
    }

    private boolean typeText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo field = findEditable(root);
        if (field == null) return false;

        android.os.Bundle args = new android.os.Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text == null ? "" : text);
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findEditable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private String readScreenText() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        StringBuilder out = new StringBuilder();
        collectText(root, out);
        return out.toString().trim();
    }

    private void collectText(AccessibilityNodeInfo node, StringBuilder out) {
        if (node == null) return;
        CharSequence t = node.getText();
        if (t != null && t.length() > 0) {
            if (out.length() > 0) out.append(" | ");
            out.append(t);
        }
        for (int i = 0; i < node.getChildCount(); i++) collectText(node.getChild(i), out);
    }

    private boolean performSwipe(boolean down) {
        if (Build.VERSION.SDK_INT < 24) return false;
        android.graphics.Path path = new android.graphics.Path();
        float x = getResources().getDisplayMetrics().widthPixels * 0.5f;
        float h = getResources().getDisplayMetrics().heightPixels;
        float startY = down ? h * 0.35f : h * 0.75f;
        float endY = down ? h * 0.75f : h * 0.35f;
        path.moveTo(x, startY);
        path.lineTo(x, endY);

        android.accessibilityservice.GestureDescription gesture =
            new android.accessibilityservice.GestureDescription.Builder()
                .addStroke(new android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 400))
                .build();
        return dispatchGesture(gesture, null, null);
    }

    private void notifyUser(String title, String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                "naten", "N8N Automation", NotificationManager.IMPORTANCE_DEFAULT));
        }

        Intent launch = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(
            this, 7, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
            ? new android.app.Notification.Builder(this, "naten")
            : new android.app.Notification.Builder(this);

        b.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi);

        nm.notify((int) (System.currentTimeMillis() & 0x7fffffff), b.build());
    }
}
