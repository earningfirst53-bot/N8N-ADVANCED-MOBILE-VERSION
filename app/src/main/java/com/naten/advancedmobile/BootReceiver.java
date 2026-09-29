package com.naten.advancedmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        for (JSONObject task : AutomationStore.getTasks(context)) {
            if (task.optBoolean("scheduled", false)) AutomationStore.schedule(context, task);
        }
    }
}
