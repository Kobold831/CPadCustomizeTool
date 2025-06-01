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

import android.app.admin.DevicePolicyManager;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.view.views.UninstallBlockAppListView;

import java.util.ArrayList;
import java.util.List;

public class UninstallBlockActivity extends AppCompatActivity {

    public UninstallBlockActivity() {
        super(R.layout.layout_uninstall_list);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        List<UninstallBlockAppListView.AppData> dataList = new ArrayList<>();

        for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
            /* ユーザーアプリか確認 */
            if (app.sourceDir.startsWith("/data/app/")) {
                UninstallBlockAppListView.AppData data = new UninstallBlockAppListView.AppData();
                data.label = app.loadLabel(getPackageManager()).toString();
                data.icon = app.loadIcon(getPackageManager());
                data.packName = app.packageName;
                dataList.add(data);
            }
        }
        DevicePolicyManager dpm = Common.getDevicePolicyManager(this);
        UninstallBlockAppListView.AppListAdapter appListAdapter = new UninstallBlockAppListView.AppListAdapter(this, dataList);
        ListView listView = findViewById(R.id.un_list);
        listView.setAdapter(appListAdapter);
        appListAdapter.setOnItemClickListener((view, position) -> {
            String selectPackage = Uri.fromParts("package", dataList.get(position).packName, null).toString();
            dpm.setUninstallBlocked(Common.getDeviceAdminComponent(this), selectPackage.replace("package:", ""),
                    !dpm.isUninstallBlocked(Common.getDeviceAdminComponent(this), selectPackage.replace("package:", "")));
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* ボタンが押されたならスイッチ一括変更 */
        AppCompatButton unDisableButton = findViewById(R.id.un_button_disable);
        AppCompatButton unEnableButton = findViewById(R.id.un_button_enable);
        /* 無効 */
        unDisableButton.setOnClickListener(v -> {
            for (UninstallBlockAppListView.AppData appData : dataList) {
                dpm.setUninstallBlocked(Common.getDeviceAdminComponent(this), appData.packName, false);
            }
            /* listviewの更新 */
            listView.invalidateViews();
        });
        /* 有効 */
        unEnableButton.setOnClickListener(v -> {
            for (UninstallBlockAppListView.AppData appData : dataList) {
                dpm.setUninstallBlocked(Common.getDeviceAdminComponent(this), appData.packName, true);
            }
            /* listviewの更新 */
            listView.invalidateViews();
        });
    }

    /* メニュー選択 */
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
