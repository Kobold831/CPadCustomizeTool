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
import android.content.ComponentName;
import android.content.Context;
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
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.view.views.UninstallBlockAppListView;

import java.util.ArrayList;
import java.util.List;

public class UninstallBlockActivity extends AppCompatActivity {

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        Common.debugLog("UninstallBlockActivity onStart");
        restart();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_uninstall_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void restart() {
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

        UninstallBlockAppListView.AppListAdapter appListAdapter = new UninstallBlockAppListView.AppListAdapter(UninstallBlockActivity.this, dataList);
        listView.setAdapter(appListAdapter);
        setListener(dataList, listView, unDisableButton, unEnableButton, appListAdapter);

    }

    public interface Listener {
        void onSuccess();
        void onFailure();
    }

    private void setListener(List<UninstallBlockAppListView.AppData> dataList, @NonNull ListView listView, @NonNull AppCompatButton unDisableButton, @NonNull AppCompatButton unEnableButton, UninstallBlockAppListView.AppListAdapter appListAdapter) {
        listView.setOnItemClickListener((parent, view, position, id) -> {
            UninstallBlockAppListView.AppData item = dataList.get(position);
            String selectPackage = Uri.fromParts("package", item.packName, null).toString();

            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            dpm.setUninstallBlocked(new ComponentName(UninstallBlockActivity.this, AdministratorReceiver.class), selectPackage.replace("package:", ""), !dpm.isUninstallBlocked(new ComponentName(UninstallBlockActivity.this, AdministratorReceiver.class), selectPackage.replace("package:", "")));
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* ボタンが押されたならスイッチ一括変更 */
        /* 無効 */
        unDisableButton.setOnClickListener(v -> {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            for (UninstallBlockAppListView.AppData appData : dataList) {
                dpm.setUninstallBlocked(new ComponentName(UninstallBlockActivity.this, AdministratorReceiver.class), appData.packName, false);
            }
            ((SwitchCompat) appListAdapter.view.findViewById(R.id.un_switch)).setChecked(false);
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* 有効 */
        unEnableButton.setOnClickListener(v -> {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

            for (UninstallBlockAppListView.AppData appData : dataList) {
                dpm.setUninstallBlocked(new ComponentName(UninstallBlockActivity.this, AdministratorReceiver.class), appData.packName, true);
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
