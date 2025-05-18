package com.saradabar.cpadcustomizetool.view.activity;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

import java.util.ArrayList;

public class CrashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AppCompatButton btnMain = findViewById(R.id.act_splash_btn_main);
        AppCompatButton btnClearAppData = findViewById(R.id.act_splash_btn_clear_app_data);
        AppCompatButton btnOpenWeb = findViewById(R.id.act_splash_btn_open_web);
        AppCompatButton btnSendCrash = findViewById(R.id.act_splash_btn_send_crash);
        AppCompatButton btnOpenCrash = findViewById(R.id.act_splash_btn_open_crash);

        btnMain.setOnClickListener(v -> {
            Preferences.save(this, Constants.KEY_FLAG_ERROR_CRASH, false);
            View contentView = getWindow().getDecorView().findViewById(android.R.id.content);
            contentView.setVisibility(View.GONE);
            startActivity(new Intent(this, CheckActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(0, 0);
            finish();
        });

        btnClearAppData.setOnClickListener(v -> new DialogUtil(this)
                .setMessage(R.string.dialog_confirm_delete)
                .setPositiveButton(getString(R.string.dialog_common_yes), (dialog, which) -> {
                    ActivityManager activityManager = (ActivityManager) getSystemService(Service.ACTIVITY_SERVICE);
                    activityManager.clearApplicationUserData();
                })
                .setNegativeButton(getString(R.string.dialog_common_cancel), null)
                .show());

        btnOpenWeb.setOnClickListener(v -> startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", "https://docs.google.com/document/d/1NcdovWYrOTPrwvzDrYF0hUSg3T7zyQxnef27rTKU_SY/edit?usp=drive_link")));

        btnSendCrash.setOnClickListener(v -> {
            try {
                ArrayList<String> arrayList = Preferences.load(this, Constants.KEY_LIST_CRASH_LOG);

                if (arrayList == null) {
                    new DialogUtil(this)
                            .setMessage(R.string.dialog_error)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return;
                }
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                //noinspection SequencedCollectionMethodCanBeUsed
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", arrayList.get(arrayList.size() - 1)));
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_FEEDBACK));
                new DialogUtil(this)
                        .setMessage("ご協力ありがとうございます。")
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            } catch (Exception e) {
                new DialogUtil(this)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(e.getMessage())
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        });

        btnOpenCrash.setOnClickListener(v -> startActivity(new Intent(this, CrashLogActivity.class)));
    }
}
