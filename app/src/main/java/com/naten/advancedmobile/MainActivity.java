package com.naten.advancedmobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private TextView status, inspector;
    private EditText pkg, tap, type, delay;
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}

    @Override public void onCreate(Bundle b){super.onCreate(b);build();}
    @Override protected void onResume(){super.onResume();if(status!=null)refresh();}

    private void build(){
        ScrollView sv=new ScrollView(this);
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(dp(18),dp(24),dp(18),dp(30));r.setBackgroundColor(Color.rgb(246,247,249));sv.addView(r);

        TextView t=txt("N8N Advanced Mobile",26,true);r.addView(t);
        TextView s=txt("Basic automation engine — deterministic user-defined actions",14,false);
        s.setPadding(0,dp(5),0,dp(14));r.addView(s);

        TextView disclosure=txt("Accessibility disclosure: this app can read the active app's accessibility UI tree and perform only actions you explicitly configure. It does not silently grant permissions or bypass Android security controls. You can disable the service in Android Settings.",13,false);
        disclosure.setBackgroundColor(Color.WHITE);disclosure.setPadding(dp(14),dp(14),dp(14),dp(14));r.addView(disclosure);

        Button settings=btn("Open Accessibility Settings");
        settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        r.addView(settings,mp(50,12));

        status=txt("",16,true);status.setBackgroundColor(Color.WHITE);status.setPadding(dp(12),dp(12),dp(12),dp(12));r.addView(status);

        TextView q=txt("Quick automation",20,true);q.setPadding(0,dp(22),0,dp(8));r.addView(q);
        pkg=field("Target package (optional)","com.android.settings");r.addView(pkg);
        tap=field("Text to tap (optional)","");r.addView(tap);
        type=field("Text to type (optional)","");r.addView(type);
        delay=field("Delay / schedule seconds","1");r.addView(delay);

        Button run=btn("Run Quick Workflow");run.setOnClickListener(v->runWorkflow());r.addView(run,mp(52,10));
        Button inspect=btn("Inspect Current Screen");inspect.setOnClickListener(v->inspect());r.addView(inspect,mp(52,8));
        inspector=txt("Inspector output will appear here.",12,false);inspector.setTypeface(Typeface.MONOSPACE);inspector.setTextIsSelectable(true);inspector.setBackgroundColor(Color.WHITE);inspector.setPadding(dp(10),dp(10),dp(10),dp(10));r.addView(inspector,new LinearLayout.LayoutParams(-1,dp(230)));

        LinearLayout nav=new LinearLayout(this);
        Button back=btn("Back");back.setOnClickListener(v->with(s->s.globalBack()));
        Button home=btn("Home");home.setOnClickListener(v->with(s->s.globalHome()));
        nav.addView(back,wp(1));nav.addView(home,wp(1));r.addView(nav,mp(52,8));

        LinearLayout scr=new LinearLayout(this);
        Button up=btn("Scroll Up");up.setOnClickListener(v->with(s->s.scroll(false)));
        Button down=btn("Scroll Down");down.setOnClickListener(v->with(s->s.scroll(true)));
        scr.addView(up,wp(1));scr.addView(down,wp(1));r.addView(scr,mp(52,5));

        Button schedule=btn("Save & Schedule Quick Workflow");schedule.setOnClickListener(v->schedule());r.addView(schedule,mp(52,12));
        Button cancel=btn("Cancel Scheduled Workflow");cancel.setOnClickListener(v->{AutomationScheduler.cancel(this);toast("Schedule cancelled");});r.addView(cancel,mp(52,5));

        Button stop=btn("EMERGENCY STOP");stop.setOnClickListener(v->with(s->{s.stopAll();toast("Automation stopped");}));r.addView(stop,mp(52,12));
        Button resume=btn("Resume Automation");resume.setOnClickListener(v->with(s->s.resumeAutomation()));r.addView(resume,mp(52,5));
        Button logs=btn("Show Logs");logs.setOnClickListener(v->showLogs());r.addView(logs,mp(52,5));
        setContentView(sv);refresh();
    }

    private List<AutomationStore.AutomationAction> actions(){
        List<AutomationStore.AutomationAction>a=new ArrayList<>();
        String p=pkg.getText().toString().trim(),x=tap.getText().toString().trim(),y=type.getText().toString();
        long d=1000;try{d=Math.max(0,Long.parseLong(delay.getText().toString().trim())*1000);}catch(Exception ignored){}
        if(!p.isEmpty())a.add(new AutomationStore.AutomationAction("launch",p,d));
        if(!x.isEmpty())a.add(new AutomationStore.AutomationAction("tap_text",x,p.isEmpty()?d:1000));
        if(!y.isEmpty())a.add(new AutomationStore.AutomationAction("type",y,x.isEmpty()&&p.isEmpty()?d:300));
        return a;
    }

    private void runWorkflow(){
        AutomationAccessibilityService s=AutomationAccessibilityService.getInstance();
        List<AutomationStore.AutomationAction>a=actions();
        if(s==null){toast("Enable the accessibility service first.");return;}
        if(a.isEmpty()){toast("Add at least one action.");return;}
        AutomationStore.saveWorkflow(this,a);s.resumeAutomation();s.runWorkflow(a);toast("Workflow started");
    }

    private void schedule(){
        List<AutomationStore.AutomationAction>a=actions();
        if(a.isEmpty()){toast("Add at least one action.");return;}
        AutomationStore.saveWorkflow(this,a);
        long sec=5;try{sec=Math.max(5,Long.parseLong(delay.getText().toString().trim()));}catch(Exception ignored){}
        long at=System.currentTimeMillis()+sec*1000L;AutomationStore.saveSchedule(this,at);AutomationScheduler.schedule(this,at);
        toast("Workflow scheduled in "+sec+" seconds");
    }

    private void inspect(){
        AutomationAccessibilityService s=AutomationAccessibilityService.getInstance();
        if(s==null){toast("Enable the accessibility service first.");return;}
        String x=s.dumpActiveWindow();if(x.length()>10000)x=x.substring(0,10000)+"\n… truncated …";inspector.setText(x);
    }

    private void showLogs(){
        new android.app.AlertDialog.Builder(this).setTitle("Automation logs").setMessage(AutomationStore.getLogs(this))
            .setPositiveButton("OK",null).setNeutralButton("Clear",(d,w)->AutomationStore.clearLogs(this)).show();
    }

    private void refresh(){if(status==null)return;boolean on=AutomationAccessibilityService.isRunning();status.setText(on?"Service: ON — ready":"Service: OFF — enable it in Accessibility Settings");status.setTextColor(on?Color.rgb(20,125,60):Color.rgb(175,75,20));}
    private interface SA{void run(AutomationAccessibilityService s);}
    private void with(SA a){AutomationAccessibilityService s=AutomationAccessibilityService.getInstance();if(s==null){toast("Enable the accessibility service first.");return;}a.run(s);}
    private TextView txt(String x,int z,boolean b){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(Color.rgb(30,35,42));if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private EditText field(String h,String v){EditText e=new EditText(this);e.setHint(h);e.setText(v);e.setSingleLine(true);e.setTextColor(Color.rgb(25,30,36));e.setHintTextColor(Color.rgb(110,115,122));e.setBackgroundColor(Color.WHITE);e.setPadding(dp(12),0,dp(12),0);return e;}
    private Button btn(String x){Button b=new Button(this);b.setText(x);b.setAllCaps(false);return b;}
    private LinearLayout.LayoutParams mp(int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(h));p.topMargin=dp(top);return p;}
    private LinearLayout.LayoutParams wp(float w){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(52),w);p.setMargins(dp(3),0,dp(3),0);return p;}
    private void toast(String x){Toast.makeText(this,x,Toast.LENGTH_SHORT).show();}
}
