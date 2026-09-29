package com.naten.advancedmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        long at = AutomationStore.getSchedule(context);
        if (at > System.currentTimeMillis()) {
            AutomationScheduler.schedule(context, at);
            AutomationStore.addLog(context, "Scheduled workflow restored after boot");
        }
    }
}
