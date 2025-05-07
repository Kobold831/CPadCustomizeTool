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
import androidx.appcompat.widget.SwitchCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.view.views.UninstallBlockAppListView;

import java.util.ArrayList;
import java.util.List;

public class UninstallBlockActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_uninstall_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        List<UninstallBlockAppListView.AppData> dataList = new ArrayList<>();
        ListView listView = findViewById(R.id.un_list);
        AppCompatButton unDisableButton = findViewById(R.id.un_button_disable);
        AppCompatButton unEnableButton = findViewById(R.id.un_button_enable);

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
        UninstallBlockAppListView.AppListAdapter appListAdapter = new UninstallBlockAppListView.AppListAdapter(this, dataList);
        listView.setAdapter(appListAdapter);
        setListener(dataList, listView, unDisableButton, unEnableButton, appListAdapter);
    }

    private void setListener(List<UninstallBlockAppListView.AppData> dataList, @NonNull ListView listView, @NonNull AppCompatButton unDisableButton, @NonNull AppCompatButton unEnableButton, UninstallBlockAppListView.AppListAdapter appListAdapter) {
        DevicePolicyManager dpm = Common.getDevicePolicyManager(this);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectPackage = Uri.fromParts("package", dataList.get(position).packName, null).toString();

            dpm.setUninstallBlocked(Common.getDeviceAdminComponent(this), selectPackage.replace("package:", ""),
                    !dpm.isUninstallBlocked(Common.getDeviceAdminComponent(this), selectPackage.replace("package:", "")));
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* ボタンが押されたならスイッチ一括変更 */
        /* 無効 */
        unDisableButton.setOnClickListener(v -> {
            for (UninstallBlockAppListView.AppData appData : dataList) {
                dpm.setUninstallBlocked(Common.getDeviceAdminComponent(this), appData.packName, false);
            }

            ((SwitchCompat) appListAdapter.view.findViewById(R.id.un_switch)).setChecked(false);
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* 有効 */
        unEnableButton.setOnClickListener(v -> {
            for (UninstallBlockAppListView.AppData appData : dataList) {
                dpm.setUninstallBlocked(Common.getDeviceAdminComponent(this), appData.packName, true);
            }

            ((SwitchCompat) appListAdapter.view.findViewById(R.id.un_switch)).setChecked(true);
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
