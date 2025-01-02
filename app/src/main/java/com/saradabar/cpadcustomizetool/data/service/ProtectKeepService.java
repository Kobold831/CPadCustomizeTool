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

package com.saradabar.cpadcustomizetool.data.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class ProtectKeepService extends Service {

    Binder binder = new Binder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Preferences.load(this, Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false) &&
                !Preferences.load(this, Constants.KEY_FLAG_KEEP_DCHA_STATE, false) &&
                !Preferences.load(this, Constants.KEY_FLAG_KEEP_MARKET_APP, false) &&
                !Preferences.load(this, Constants.KEY_FLAG_KEEP_USB_DEBUG, false) &&
                !Preferences.load(this, Constants.KEY_FLAG_KEEP_HOME, false)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        bindService(new Intent(getBaseContext(), KeepService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false) ||
                        Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_DCHA_STATE, false) ||
                        Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP, false) ||
                        Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false) ||
                        Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
                    startService(new Intent(getBaseContext(), KeepService.class));
                }
            }
        }, Context.BIND_AUTO_CREATE);
        return START_STICKY;
    }
}
