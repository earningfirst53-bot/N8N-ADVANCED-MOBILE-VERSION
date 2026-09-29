package com.naten.advancedmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

public class AutomationTriggerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra("taskId");
        if (taskId == null || taskId.isEmpty()) return;
        JSONObject task = AutomationStore.findTask(context, taskId);
        if (task == null) return;
        AutomationStore.setActive(context, taskId);
        AutomationStore.log(context, "Triggered: " + task.optString("name", taskId));
    }
}
