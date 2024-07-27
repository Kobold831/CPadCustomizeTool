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

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        /* UsbDebugを有効にするか確認 */
        if (Preferences.load(context, Constants.KEY_ENABLED_AUTO_USB_DEBUG, false)) {
            bypassAdbDisabled(context);
        }

        /* 維持スイッチが有効のときサービスを起動 */
        if (Preferences.load(context, Constants.KEY_ENABLED_KEEP_SERVICE, false) ||
                Preferences.load(context, Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) ||
                Preferences.load(context, Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) ||
                Preferences.load(context, Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) ||
                Preferences.load(context, Constants.KEY_ENABLED_KEEP_HOME, false)) {
            startService(context);
        }
    }

    private void bypassAdbDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                Preferences.save(context, Constants.KEY_ENABLED_AUTO_USB_DEBUG, false);
                return;
            }
        }

        if (!Common.isCfmDialog(context)) {
            Preferences.save(context, Constants.KEY_ENABLED_AUTO_USB_DEBUG, false);
            return;
        }

        try {
            if (Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                Settings.System.putInt(context.getContentResolver(), Constants.DCHA_STATE, 3);
                Thread.sleep(100);
            }

            Settings.Global.putInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 1);

            if (Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                Settings.System.putInt(context.getContentResolver(), Constants.DCHA_STATE, 0);
            }
        } catch (Exception ignored) {
            Preferences.save(context, Constants.KEY_ENABLED_AUTO_USB_DEBUG, false);
            if (Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                Settings.System.putInt(context.getContentResolver(), Constants.DCHA_STATE, 0);
            }
        }
    }

    private void startService(Context context) {
        try {
            Settings.System.putInt(context.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
            context.startService(new Intent(context, KeepService.class));
            context.startService(new Intent(context, ProtectKeepService.class));
        } catch (Exception ignored) {
        }
    }
}
