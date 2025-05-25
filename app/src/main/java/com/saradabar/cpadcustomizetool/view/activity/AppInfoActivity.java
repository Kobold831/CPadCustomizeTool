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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;

public class AppInfoActivity extends AppCompatActivity {

    public AppInfoActivity() {
        super(R.layout.activity_app_info);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        initialize();
        MaterialButton buttonDownload = findViewById(R.id.download_button);
        MaterialButton buttonFeedBack = findViewById(R.id.act_app_info_button_feedback);

        buttonDownload.setOnClickListener(view ->
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_GITHUB).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)));
        buttonFeedBack.setOnClickListener(v ->
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_FEEDBACK).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)));
        MaterialCardView cardViewDev1 = findViewById(R.id.act_app_info_card_dev_1);
        MaterialCardView cardViewDev2 = findViewById(R.id.act_app_info_card_dev_2);

        cardViewDev1.setOnClickListener(v ->
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", "https://github.com/Kobold831").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)));
        cardViewDev2.setOnClickListener(v ->
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", "https://github.com/s1204IT").addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)));
    }

    private void initialize() {
        AppCompatTextView text1 = findViewById(R.id.menu_text_1),
                text2 = findViewById(R.id.menu_text_2),
                text3 = findViewById(R.id.menu_text_3),
                text4 = findViewById(R.id.menu_text_4),
                text5 = findViewById(R.id.menu_text_5);

        text1.setText(getApplicationInfo().loadLabel(getPackageManager()));
        text2.setText(BuildConfig.APPLICATION_ID);
        text3.setText(BuildConfig.VERSION_NAME);
        text4.setText(String.valueOf(BuildConfig.VERSION_CODE));
        text5.setText(BuildConfig.BUILD_TYPE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(0, 0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
