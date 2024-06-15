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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.PermissionIntentService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;

import java.util.Objects;

public class PackageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction()) && !Objects.requireNonNull(intent.getExtras()).getBoolean(Intent.EXTRA_REPLACING)) {
            run(context, intent);
        }

        if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
            if (Objects.requireNonNull(intent.getExtras()).getBoolean(Intent.EXTRA_DATA_REMOVED) && intent.getExtras().getBoolean(Intent.EXTRA_REPLACING)) {
                run(context, intent);
            }

            if (!intent.getExtras().getBoolean(Intent.EXTRA_DATA_REMOVED) && intent.getExtras().getBoolean(Intent.EXTRA_REPLACING)) {
                run(context, intent);
            }
        }

        if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
            run(context, intent);
        }
    }

    @SuppressWarnings("deprecation")
    private void run(Context context, Intent intent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (intent.getData() == null) {
            return;
        }

        /* サービス開始 */
        if (intent.getData().toString().replace("package:", "").equals(context.getPackageName())) {
            /* 維持スイッチが有効のときサービスを起動 */
            if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                Settings.System.putInt(context.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);

                if (!Common.isRunningService(context, KeepService.class.getName())) {
                    context.startService(new Intent(context, KeepService.class));
                }

                if (!Common.isRunningService(context, ProtectKeepService.class.getName())) {
                    context.startService(new Intent(context, ProtectKeepService.class));
                }

                new Handler().postDelayed(() -> KeepService.getInstance().startService(), 1000);
            }
        }

        /* ランタイム権限を強制付与が有効な場合 */
        if (sp.getBoolean("pre_owner_permission_frc", false)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return;
            }

            if (!Common.isRunningService(context, PermissionIntentService.class.getName())) {
                context.startService(new Intent(context, PermissionIntentService.class).putExtra("packageName", intent.getData().toString().replace("package:", "")));
            }
        }
    }
}
