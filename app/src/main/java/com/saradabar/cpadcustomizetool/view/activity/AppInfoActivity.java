package com.saradabar.cpadcustomizetool.view.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Toast;

public class AppInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app_info);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        initialize();

        findViewById(R.id.info_button).setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_WIKI_DISCORD)));
            } catch (ActivityNotFoundException ignored) {
                Toast.toast(this, R.string.toast_unknown_activity);
            }
        });

        findViewById(R.id.download_button).setOnClickListener(view -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_GITHUB)));
            } catch (ActivityNotFoundException ignored) {
                Toast.toast(this, R.string.toast_unknown_activity);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void initialize() {
        TextView text1 = findViewById(R.id.menu_text_1),
                text2 = findViewById(R.id.menu_text_2),
                text3 = findViewById(R.id.menu_text_3),
                text4 = findViewById(R.id.menu_text_4),
                text5 = findViewById(R.id.menu_text_5),
                text6 = findViewById(R.id.menu_text_organization);

        text1.setText("アプリ名：" + getApplicationInfo().loadLabel(getPackageManager()));
        text2.setText("パッケージ名：" + BuildConfig.APPLICATION_ID);
        text3.setText("バージョン：" + BuildConfig.VERSION_NAME);
        text4.setText("バージョンコード：" + BuildConfig.VERSION_CODE);
        text5.setText(getString(R.string.info_app_state, BuildConfig.BUILD_TYPE));
        text6.setText(getString(R.string.info_organization_label, getString(R.string.info_organization)));
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