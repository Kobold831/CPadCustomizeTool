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

package com.saradabar.cpadcustomizetool.data.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        /* 維持スイッチが有効のときサービスを起動 */
        if (Preferences.load(context, Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false) ||
                Preferences.load(context, Constants.KEY_FLAG_KEEP_DCHA_STATE, false) ||
                Preferences.load(context, Constants.KEY_FLAG_KEEP_MARKET_APP, false) ||
                Preferences.load(context, Constants.KEY_FLAG_KEEP_USB_DEBUG, false) ||
                Preferences.load(context, Constants.KEY_FLAG_KEEP_HOME, false)) {
            Settings.System.putInt(context.getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
            context.startService(new Intent(context, KeepService.class));
            context.startService(new Intent(context, ProtectKeepService.class));
        }
    }
}
