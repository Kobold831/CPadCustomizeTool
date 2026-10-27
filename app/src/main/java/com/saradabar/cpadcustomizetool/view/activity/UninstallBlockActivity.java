/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.task.AppExecutors;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.view.views.UninstallBlockAppListView;

import java.util.ArrayList;
import java.util.List;

public class UninstallBlockActivity extends AppCompatActivity {

    private AppCompatButton unDisableButton;
    private AppCompatButton unEnableButton;
    private AlertDialog progressDialog;

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
        ListView listView = findViewById(R.id.un_list);
        unDisableButton = findViewById(R.id.un_button_disable);
        unEnableButton = findViewById(R.id.un_button_enable);
        setButtonsEnabled(false);

        DevicePolicyManager dpm = Common.getDevicePolicyManager(this);
        progressDialog = Common.showProgressSpinner(this, getString(R.string.progress_state_check_file));

        AppExecutors.io(() -> {
            List<UninstallBlockAppListView.AppData> dataList = loadAppList();
            AppExecutors.main(() -> {
                Common.cancelDialog(progressDialog);

                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showAppList(dataList, listView, dpm);
            });
        });
    }

    @NonNull
    private List<UninstallBlockAppListView.AppData> loadAppList() {
        PackageManager packageManager = getPackageManager();
        List<UninstallBlockAppListView.AppData> dataList = new ArrayList<>();

        for (ApplicationInfo app : Common.getUserApplications(this)) {
            UninstallBlockAppListView.AppData data = new UninstallBlockAppListView.AppData();
            data.label = app.loadLabel(packageManager).toString();
            data.icon = app.loadIcon(packageManager);
            data.packName = app.packageName;
            dataList.add(data);
        }
        return dataList;
    }

    private void showAppList(@NonNull List<UninstallBlockAppListView.AppData> dataList, @NonNull ListView listView,
                             @NonNull DevicePolicyManager dpm) {
        UninstallBlockAppListView.AppListAdapter appListAdapter = new UninstallBlockAppListView.AppListAdapter(this, dataList);
        listView.setAdapter(appListAdapter);
        appListAdapter.setOnItemClickListener((view, position) -> {
            String selectPackage = Uri.fromParts("package", dataList.get(position).packName, null).toString();
            dpm.setUninstallBlocked(Common.getDeviceAdminComponent(this), selectPackage.replace("package:", ""),
                    !dpm.isUninstallBlocked(Common.getDeviceAdminComponent(this), selectPackage.replace("package:", "")));
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* ボタンが押されたならスイッチ一括変更 */
        /* 無効 */
        unDisableButton.setOnClickListener(v -> setAllUninstallBlocked(false, dataList, listView, dpm));
        /* 有効 */
        unEnableButton.setOnClickListener(v -> setAllUninstallBlocked(true, dataList, listView, dpm));
        setButtonsEnabled(true);
    }

    @Override
    protected void onDestroy() {
        Common.cancelDialog(progressDialog);
        super.onDestroy();
    }

    private void setAllUninstallBlocked(boolean blocked, @NonNull List<UninstallBlockAppListView.AppData> dataList,
                                        @NonNull ListView listView, @NonNull DevicePolicyManager dpm) {
        setButtonsEnabled(false);
        AlertDialog dialog = Common.showProgressSpinner(this, getString(R.string.progress_state_processing));
        progressDialog = dialog;
        TextView progressText = dialog.findViewById(R.id.view_progress_spinner_text);

        AppExecutors.io(() -> {
            ComponentName admin = Common.getDeviceAdminComponent(this);
            int total = dataList.size();
            int current = 0;
            boolean failed = false;

            for (UninstallBlockAppListView.AppData appData : dataList) {
                current++;
                int progress = current;
                String packageName = appData.packName;

                try {
                    if (dpm.isUninstallBlocked(admin, packageName) != blocked) {
                        dpm.setUninstallBlocked(admin, packageName, blocked);
                    }
                } catch (RuntimeException ignored) {
                    failed = true;
                }

                AppExecutors.main(() -> {
                    if (progressText != null) {
                        progressText.setText(String.format("%s\n%s",
                                getString(R.string.progress_state_processing),
                                getString(R.string.progress_uninstall_count, progress, total, packageName)));
                    }
                });
            }

            final boolean hasError = failed;
            AppExecutors.main(() -> {
                Common.cancelDialog(dialog);

                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setButtonsEnabled(true);
                listView.invalidateViews();

                if (hasError) {
                    Toast.makeText(this, R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        unDisableButton.setEnabled(enabled);
        unEnableButton.setEnabled(enabled);
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
