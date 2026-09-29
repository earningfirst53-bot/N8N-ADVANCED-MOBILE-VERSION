package com.naten.advancedmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.List;

public class AutomationTriggerReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        AutomationStore.addLog(context, "Scheduled trigger received");
        AutomationAccessibilityService service = AutomationAccessibilityService.getInstance();
        if (service == null) {
            AutomationStore.addLog(context, "Scheduled workflow skipped: accessibility service is OFF");
            return;
        }
        List<AutomationStore.AutomationAction> actions = AutomationStore.loadWorkflow(context);
        if (actions.isEmpty()) {
            AutomationStore.addLog(context, "Scheduled workflow skipped: no saved workflow");
            return;
        }
        service.runWorkflow(actions);
        AutomationStore.clearSchedule(context);
    }
}
