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

package com.saradabar.cpadcustomizetool.Receiver;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;
import static com.saradabar.cpadcustomizetool.util.Common.tryBindDhizukuService;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import androidx.preference.PreferenceManager;

import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;

import java.util.Objects;

public class PackageAddedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(context));
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (Objects.equals(intent.getAction(), Intent.ACTION_PACKAGE_ADDED)) {
            if (Objects.requireNonNull(intent.getData()).toString().replace("package:", "").equals(context.getPackageName())) {
                /* 維持スイッチが有効のときサービスを起動 */
                if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                    Settings.System.putInt(context.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);

                    if (!Common.isRunningService(context, KeepService.class.getName())) {
                        context.startService(new Intent(context, KeepService.class));
                    }

                    if (!Common.isRunningService(context, ProtectKeepService.class.getName())) {
                        context.startService(new Intent(context, ProtectKeepService.class));
                    }

                    Runnable runnable = () -> KeepService.getInstance().startService();

                    new Handler().postDelayed(runnable, 1000);
                }
            }

            if (sp.getBoolean("pre_owner_permission_frc", false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (isDhizukuActive(context)) {
                        if (tryBindDhizukuService(context)) {
                            Runnable runnable = () -> {
                                for (ApplicationInfo app : context.getPackageManager().getInstalledApplications(0)) {
                                    /* ユーザーアプリか確認 */
                                    if (app.sourceDir.startsWith("/data/app/")) {
                                        Common.setPermissionGrantState(context, app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                                    }
                                }
                            };

                            new Handler().postDelayed(runnable, 5000);
                        }
                    } else {
                        for (ApplicationInfo app : context.getPackageManager().getInstalledApplications(0)) {
                            /* ユーザーアプリか確認 */
                            if (app.sourceDir.startsWith("/data/app/")) {
                                Common.setPermissionGrantState(context, app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                            }
                        }
                    }
                }
            }
        }

        if (Objects.equals(intent.getAction(), Intent.ACTION_PACKAGE_REPLACED)) {
            if (Objects.requireNonNull(intent.getData()).toString().replace("package:", "").equals(context.getPackageName())) {
                /* 維持スイッチが有効のときサービスを起動 */
                if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                    Settings.System.putInt(context.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);

                    if (!Common.isRunningService(context, KeepService.class.getName())) {
                        context.startService(new Intent(context, KeepService.class));
                    }

                    if (!Common.isRunningService(context, ProtectKeepService.class.getName())) {
                        context.startService(new Intent(context, ProtectKeepService.class));
                    }

                    Runnable runnable = () -> KeepService.getInstance().startService();

                    new Handler().postDelayed(runnable, 1000);
                }
            }
        }
    }
}
