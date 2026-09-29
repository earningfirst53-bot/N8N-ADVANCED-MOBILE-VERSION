package com.naten.advancedmobile;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.TOP);
        root.setPadding(dp(20), dp(28), dp(20), dp(20));
        root.setBackgroundColor(Color.rgb(246,247,249));

        TextView title = new TextView(this);
        title.setText("N8N Advanced Mobile");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(25,30,36));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Basic Android automation foundation");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.rgb(80,86,94));
        subtitle.setPadding(0, dp(6), 0, dp(20));
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));

        TextView status = new TextView(this);
        status.setText("APP STATUS\nInstalled and running successfully.");
        status.setTextSize(17);
        status.setTextColor(Color.rgb(22,124,58));
        status.setBackgroundColor(Color.WHITE);
        status.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        Button test = new Button(this);
        test.setText("Test App");
        test.setAllCaps(false);
        test.setOnClickListener(v ->
            Toast.makeText(this, "N8N Advanced Mobile is running.", Toast.LENGTH_SHORT).show()
        );
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(52));
        bp.topMargin = dp(16);
        root.addView(test, bp);

        TextView note = new TextView(this);
        note.setText("This is the clean installable foundation build. Automation services will be added only after the base APK is verified.");
        note.setTextSize(13);
        note.setTextColor(Color.rgb(80,86,94));
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }
}

// final validation pass

// CI validation marker v0.4
