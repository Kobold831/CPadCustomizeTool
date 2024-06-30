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

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.data.service.DhizukuService;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.view.views.UninstallBlockView;

import java.util.ArrayList;
import java.util.List;

public class UninstallBlockActivity extends AppCompatActivity {

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_uninstall_list);

        DevicePolicyManager dpm = (DevicePolicyManager) this.getSystemService("device_policy");
        List<UninstallBlockView.AppData> dataList = new ArrayList<>();
        ListView listView = findViewById(R.id.un_list);
        Button unDisableButton = findViewById(R.id.un_button_disable);
        Button unEnableButton = findViewById(R.id.un_button_enable);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
            /* ユーザーアプリか確認 */
            if (app.sourceDir.startsWith("/data/app/")) {
                UninstallBlockView.AppData data = new UninstallBlockView.AppData();
                data.label = app.loadLabel(getPackageManager()).toString();
                data.icon = app.loadIcon(getPackageManager());
                data.packName = app.packageName;
                dataList.add(data);
            }
        }

        UninstallBlockView.AppListAdapter appListAdapter = new UninstallBlockView.AppListAdapter(this, dataList);

        listView.setAdapter(appListAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            UninstallBlockView.AppData item = dataList.get(position);
            String selectPackage = Uri.fromParts("package", item.packName, null).toString();
            if (Common.isDhizukuActive(this)) {
                if (tryBindDhizukuService(this)) {
                    try {
                        ((MyApplication) getApplicationContext()).mDhizukuService.setUninstallBlocked(selectPackage.replace("package:", ""), !((MyApplication) getApplicationContext()).mDhizukuService.isUninstallBlocked(selectPackage.replace("package:", "")));
                    } catch (RemoteException ignored) {
                    }
                }
            } else {
                dpm.setUninstallBlocked(new ComponentName(this, AdministratorReceiver.class), selectPackage.replace("package:", ""), !dpm.isUninstallBlocked(new ComponentName(this, AdministratorReceiver.class), selectPackage.replace("package:", "")));
            }
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* ボタンが押されたならスイッチ一括変更 */
        /* 無効 */
        unDisableButton.setOnClickListener(v -> {
            if (Common.isDhizukuActive(this)) {
                if (tryBindDhizukuService(this)) {
                    try {
                        for (UninstallBlockView.AppData appData : dataList) {
                            ((MyApplication) getApplicationContext()).mDhizukuService.setUninstallBlocked(appData.packName, false);
                        }
                        ((Switch) appListAdapter.view.findViewById(R.id.un_switch)).setChecked(false);
                    } catch (Exception ignored) {
                    }
                }
            } else {
                for (UninstallBlockView.AppData appData : dataList) {
                    dpm.setUninstallBlocked(new ComponentName(this, AdministratorReceiver.class), appData.packName, false);
                }
                ((Switch) appListAdapter.view.findViewById(R.id.un_switch)).setChecked(false);
            }
            /* listviewの更新 */
            listView.invalidateViews();
        });

        /* 有効 */
        unEnableButton.setOnClickListener(v -> {
            if (Common.isDhizukuActive(this)) {
                if (tryBindDhizukuService(this)) {
                    try {
                        for (UninstallBlockView.AppData appData : dataList) {
                            ((MyApplication) getApplicationContext()).mDhizukuService.setUninstallBlocked(appData.packName, true);
                        }
                        ((Switch) appListAdapter.view.findViewById(R.id.un_switch)).setChecked(true);
                    } catch (Exception ignored) {
                    }
                }
            } else {
                for (UninstallBlockView.AppData appData : dataList) {
                    dpm.setUninstallBlocked(new ComponentName(this, AdministratorReceiver.class), appData.packName, true);
                }
                ((Switch) appListAdapter.view.findViewById(R.id.un_switch)).setChecked(true);
            }
            /* listviewの更新 */
            listView.invalidateViews();
        });
    }

    /* メニュー選択 */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean tryBindDhizukuService(Context context) {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class));
        return Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                ((MyApplication) context.getApplicationContext()).mDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        });
    }
}