/*
 * CPad Customize Tool
 * Copyright © 2021-2024 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.view.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;

public class AppInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initialize();
        findViewById(R.id.download_button).setOnClickListener(view -> {
            try {
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_GITHUB).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.act_app_info_button_feedback).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_FEEDBACK).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initialize() {
        TextView text1 = findViewById(R.id.menu_text_1),
                text2 = findViewById(R.id.menu_text_2),
                text3 = findViewById(R.id.menu_text_3),
                text4 = findViewById(R.id.menu_text_4),
                text5 = findViewById(R.id.menu_text_5);

        text1.setText(new StringBuilder("アプリ名：").append(getApplicationInfo().loadLabel(getPackageManager())));
        text2.setText(new StringBuilder("パッケージ名：").append(BuildConfig.APPLICATION_ID));
        text3.setText(new StringBuilder("バージョン：").append(BuildConfig.VERSION_NAME));
        text4.setText(new StringBuilder("バージョンコード：").append(BuildConfig.VERSION_CODE));
        text5.setText(getString(R.string.info_app_state, BuildConfig.BUILD_TYPE));
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
