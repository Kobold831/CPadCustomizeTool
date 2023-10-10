package com.saradabar.cpadcustomizetool.view.activity;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class CrashLogActivity extends Activity {

    TextView textView;
    ScrollView scrollView;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_log);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        textView = findViewById(R.id.textView);
        scrollView = findViewById(R.id.scrollView);
        if (Preferences.GET_CRASH_LOG(this).length() != 0) {
            addText(Preferences.GET_CRASH_LOG(this));
        } else {
            addText(getString(R.string.logger_empty));
        }
    }

    private void addText(String status)
    {
        textView.append(status);
        int bottom = textView.getBottom() + scrollView.getPaddingBottom();
        int sy = scrollView.getScrollY();
        int sh = scrollView.getHeight();
        int delta = bottom - (sy + sh);
        scrollView.smoothScrollBy(0, delta);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}