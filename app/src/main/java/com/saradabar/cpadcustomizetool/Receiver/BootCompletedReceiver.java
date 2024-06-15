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

import static com.saradabar.cpadcustomizetool.util.Common.isCfmDialog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sp = context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            /* UsbDebugを有効にするか確認 */
            try {
                if (sp.getBoolean(Constants.KEY_ENABLED_AUTO_USB_DEBUG, false)) {
                    try {
                        if (isCfmDialog(context)) {
                            if (Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                                Settings.System.putInt(context.getContentResolver(), Constants.DCHA_STATE, 3);
                                Thread.sleep(100);
                            }
                        }

                        Settings.Global.putInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 1);

                        if (isCfmDialog(context)) {
                            if (Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                                Settings.System.putInt(context.getContentResolver(), Constants.DCHA_STATE, 0);
                            }
                        }
                    } catch (Exception ignored) {
                        /* 権限が付与されていないなら機能を無効 */
                        sp.edit().putBoolean(Constants.KEY_ENABLED_AUTO_USB_DEBUG, false).apply();

                        if (isCfmDialog(context)) {
                            if (Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(context, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                                Settings.System.putInt(context.getContentResolver(), Constants.DCHA_STATE, 0);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                sp.edit().putBoolean(Constants.KEY_ENABLED_AUTO_USB_DEBUG, false).apply();
            }

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
