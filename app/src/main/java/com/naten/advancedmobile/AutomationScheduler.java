package com.naten.advancedmobile;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class AutomationScheduler {
    private static final int REQUEST_CODE = 78125;
    private AutomationScheduler(){}

    public static void schedule(Context c, long at){
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(a==null)return;
        Intent i=new Intent(c,AutomationTriggerReceiver.class);
        PendingIntent p=PendingIntent.getBroadcast(
            c,REQUEST_CODE,i,
            PendingIntent.FLAG_UPDATE_CURRENT |
            (Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0)
        );
        if(Build.VERSION.SDK_INT>=23){
            a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);
        }else{
            a.set(AlarmManager.RTC_WAKEUP,at,p);
        }
    }

    public static void cancel(Context c){
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(a!=null){
            Intent i=new Intent(c,AutomationTriggerReceiver.class);
            PendingIntent p=PendingIntent.getBroadcast(
                c,REQUEST_CODE,i,
                PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0)
            );
            a.cancel(p);
        }
        AutomationStore.clearSchedule(c);
    }
}
