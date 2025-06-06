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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.views.CrashLogListView;

import java.util.ArrayList;
import java.util.List;

public class CrashLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_log);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        List<CrashLogListView.AppData> appDataList = new ArrayList<>();
        ListView listView = findViewById(R.id.act_crash_log_list);
        ArrayList<String> stringArrayList = Preferences.load(this, Constants.KEY_LIST_CRASH_LOG);

        if (stringArrayList == null) {
            new DialogUtil(this)
                    .setCancelable(false)
                    .setMessage(getString(R.string.no_log))
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                    .show();
            return;
        }

        for (int i = 0; i < stringArrayList.size(); i++) {
            CrashLogListView.AppData appData = new CrashLogListView.AppData();
            appData.strMessage = stringArrayList.get(i);
            appDataList.add(appData);
        }
        CrashLogListView.AppListAdapter appListAdapter = new CrashLogListView.AppListAdapter(this, appDataList);
        listView.setAdapter(appListAdapter);
        listView.setSelection(stringArrayList.size() - 1);
        appListAdapter.setOnItemClickListener((view, position) -> {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText("", stringArrayList.get(position)));
            new DialogUtil(this)
                    .setMessage("対象データをコピーしました。")
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
        });
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
