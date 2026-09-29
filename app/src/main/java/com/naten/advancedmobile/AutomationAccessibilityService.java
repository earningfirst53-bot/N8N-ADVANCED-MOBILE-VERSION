package com.naten.advancedmobile;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class AutomationAccessibilityService extends AccessibilityService {
    private static AutomationAccessibilityService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean emergencyStop;

    public static AutomationAccessibilityService getInstance() { return instance; }
    public static boolean isRunning() { return instance != null; }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
        AutomationStore.addLog(this, "Accessibility service connected");
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() { AutomationStore.addLog(this, "Accessibility service interrupted"); }
    @Override public void onDestroy() {
        if (instance == this) instance = null;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public void stopAll() {
        emergencyStop = true;
        handler.removeCallbacksAndMessages(null);
        AutomationStore.addLog(this, "EMERGENCY STOP");
    }

    public void resumeAutomation() {
        emergencyStop = false;
        AutomationStore.addLog(this, "Automation resumed");
    }

    public boolean globalBack() { return performGlobalAction(GLOBAL_ACTION_BACK); }
    public boolean globalHome() { return performGlobalAction(GLOBAL_ACTION_HOME); }

    public boolean scroll(boolean forward) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo node = findScrollable(root);
        if (node == null) return false;
        return node.performAction(forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                                          : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    public String dumpActiveWindow() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "No active accessibility window.";
        StringBuilder b = new StringBuilder();
        dump(root, b, 0);
        return b.toString();
    }

    private void dump(AccessibilityNodeInfo n, StringBuilder b, int depth) {
        if (n == null || depth > 24) return;
        Rect r = new Rect(); n.getBoundsInScreen(r);
        CharSequence t=n.getText(), d=n.getContentDescription();
        b.append(spaces(depth)).append(n.getClassName())
         .append(" | text=").append(t==null?"":t)
         .append(" | desc=").append(d==null?"":d)
         .append(" | id=").append(n.getViewIdResourceName()==null?"":n.getViewIdResourceName())
         .append(" | clickable=").append(n.isClickable())
         .append(" | editable=").append(n.isEditable())
         .append(" | bounds=").append(r).append("\n");
        for(int i=0;i<n.getChildCount();i++) dump(n.getChild(i),b,depth+1);
    }

    private String spaces(int n) { StringBuilder b=new StringBuilder(); for(int i=0;i<n;i++) b.append("  "); return b.toString(); }

    public boolean tapText(String query) {
        AccessibilityNodeInfo n=findText(query);
        if(n==null) return false;
        AccessibilityNodeInfo p=n;
        while(p!=null && !p.isClickable()) p=p.getParent();
        if(p!=null && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        Rect r=new Rect(); n.getBoundsInScreen(r);
        if(android.os.Build.VERSION.SDK_INT>=24 && r.width()>0 && r.height()>0) {
            Path path=new Path(); path.moveTo(r.centerX(),r.centerY());
            GestureDescription g=new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path,0,80)).build();
            return dispatchGesture(g,null,null);
        }
        return false;
    }

    public boolean setFocusedText(String text) {
        AccessibilityNodeInfo n=findEditable(getRootInActiveWindow());
        if(n==null) return false;
        Bundle a=new Bundle();
        a.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text==null?"":text);
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,a);
    }

    private AccessibilityNodeInfo findText(String q) {
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null || q==null || q.trim().isEmpty()) return null;
        List<AccessibilityNodeInfo> direct=root.findAccessibilityNodeInfosByText(q);
        if(direct!=null && !direct.isEmpty()) return direct.get(0);
        return findTextRecursive(root,q.toLowerCase());
    }

    private AccessibilityNodeInfo findTextRecursive(AccessibilityNodeInfo n,String q) {
        if(n==null) return null;
        CharSequence t=n.getText(), d=n.getContentDescription();
        if((t!=null && t.toString().toLowerCase().contains(q)) ||
           (d!=null && d.toString().toLowerCase().contains(q))) return n;
        for(int i=0;i<n.getChildCount();i++){ AccessibilityNodeInfo f=findTextRecursive(n.getChild(i),q); if(f!=null)return f; }
        return null;
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo n) {
        if(n==null)return null;
        if(n.isEditable() && n.isEnabled())return n;
        for(int i=0;i<n.getChildCount();i++){ AccessibilityNodeInfo f=findEditable(n.getChild(i)); if(f!=null)return f; }
        return null;
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo n) {
        if(n==null)return null;
        if(n.isScrollable() && n.isEnabled())return n;
        for(int i=0;i<n.getChildCount();i++){ AccessibilityNodeInfo f=findScrollable(n.getChild(i)); if(f!=null)return f; }
        return null;
    }

    public void runWorkflow(final List<AutomationStore.AutomationAction> actions) {
        if(actions==null || actions.isEmpty() || emergencyStop)return;
        runStep(actions,0);
    }

    private void runStep(final List<AutomationStore.AutomationAction> actions,final int i) {
        if(emergencyStop)return;
        if(i>=actions.size()){ AutomationStore.addLog(this,"Workflow complete"); return; }
        AutomationStore.AutomationAction a=actions.get(i);
        handler.postDelayed(() -> {
            if(emergencyStop)return;
            boolean ok=execute(a);
            AutomationStore.addLog(this,(ok?"OK: ":"FAIL: ")+a.type+(a.value.isEmpty()?"":" ["+a.value+"]"));
            if(ok) runStep(actions,i+1);
        },Math.max(0,a.delayMs));
    }

    private boolean execute(AutomationStore.AutomationAction a) {
        String t=a.type;
        if("launch".equals(t)) {
            try {
                android.content.Intent i=getPackageManager().getLaunchIntentForPackage(a.value);
                if(i==null)return false;
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i); return true;
            } catch(Exception e){return false;}
        }
        if("tap_text".equals(t))return tapText(a.value);
        if("type".equals(t))return setFocusedText(a.value);
        if("back".equals(t))return globalBack();
        if("home".equals(t))return globalHome();
        if("scroll_up".equals(t))return scroll(false);
        if("scroll_down".equals(t))return scroll(true);
        return "wait".equals(t);
    }
}
