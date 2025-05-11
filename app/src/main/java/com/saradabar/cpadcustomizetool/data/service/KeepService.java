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
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class KeepService extends Service {

    Binder binder = new Binder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* オブザーバーを有効化 */
        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false)) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, navigationBarObserver);
        } else {
            getContentResolver().unregisterContentObserver(navigationBarObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_DCHA_STATE, false)) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, dchaStateObserver);
        } else {
            getContentResolver().unregisterContentObserver(dchaStateObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP, false)) {
            //noinspection deprecation
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, marketObserver);
        } else {
            getContentResolver().unregisterContentObserver(marketObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false)) {
            getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, usbDebugObserver);
        } else {
            getContentResolver().unregisterContentObserver(usbDebugObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
            runKeepDefaultLauncher();
        }

        if (!Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false) &&
                !Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_DCHA_STATE, false) &&
                !Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP, false) &&
                !Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false) &&
                !Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        /* ProtectKeepServiceにバインド */
        bindService(new Intent(getBaseContext(), ProtectKeepService.class), new ServiceConnection() {
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
                    startService(new Intent(getBaseContext(), ProtectKeepService.class));
                }
            }
        }, Context.BIND_AUTO_CREATE);
        return START_STICKY;
    }

    private void runKeepDefaultLauncher() {
        Handler handler = new Handler(getMainLooper()) {

            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                if (!Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
                    return;
                }

                if (getDefaultLauncherPackageName() != null) {
                    if (!getDefaultLauncherPackageName().equals(Preferences.load(getBaseContext(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE, null))) {
                        bindService(Constants.ACTION_DCHA_SERVICE, new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                                try {
                                    IDchaService iDchaService = IDchaService.Stub.asInterface(iBinder);
                                    iDchaService.clearDefaultPreferredApp(getDefaultLauncherPackageName());
                                    iDchaService.setDefaultPreferredHomeApp(Preferences.load(getBaseContext(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE, null));
                                } catch (Exception ignored) {
                                }
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName componentName) {
                            }
                        }, Context.BIND_AUTO_CREATE);
                    }
                }
                sendEmptyMessageDelayed(0, 10000);
            }
        };

        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
            handler.sendEmptyMessageDelayed(0, 0);
        }
    }

    @Nullable
    private String getDefaultLauncherPackageName() {
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        } else {
            return null;
        }
    }

    /* DchaStateオブサーバー */
    ContentObserver dchaStateObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.System.getInt(getContentResolver(), Constants.DCHA_STATE) == 3) {
                    Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 0);
                }
            } catch (Exception ignored) {
            }
        }
    };

    /* ナビゲーションバーオブサーバー */
    ContentObserver navigationBarObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.System.getInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR) == 1) {
                    Settings.System.putInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
                }
            } catch (Exception ignored) {
            }
        }
    };

    /* 提供元不明オブサーバー */
    ContentObserver marketObserver = new ContentObserver(new Handler()) {

        /** @noinspection deprecation*/
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) == 0) {
                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);
                }
            } catch (Exception ignored) {
                Preferences.save(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP, false);
            }
        }
    };

    /* UDBデバッグオブサーバー */
    ContentObserver usbDebugObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED) == 0) {
                    if (Common.getDchaCompletedPast()) {
                        Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 3);
                        Thread.sleep(100);
                    }

                    Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                    Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                    Settings.System.putInt(getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 1);

                    if (Common.getDchaCompletedPast()) {
                        Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 0);
                    }
                }
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 0)
                    Settings.Global.putInt(getBaseContext().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
            } catch (Exception ignored) {
                Preferences.save(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                if (Common.isCTX() || Common.isCTZ()) {
                    Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 0);
                }
            }
        }
    };
}
