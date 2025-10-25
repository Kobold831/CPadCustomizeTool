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

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class KeepService extends Service {

    final Binder binder = new Binder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* オブザーバーの初期化 */
        initRegisterContentObserver();

        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
            // ホームアプリ維持
            runKeepHomeApp();
        }

        if (!Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false) &&
                !Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_DCHA_STATE, false) &&
                !Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP, false) &&
                !Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false) &&
                !Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
            // すべての機能が無効
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
                    // いずれかの機能が有効
                    startService(new Intent(getBaseContext(), ProtectKeepService.class));
                }
            }
        }, Context.BIND_AUTO_CREATE);
        return START_STICKY;
    }

    private void initRegisterContentObserver() {
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
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, marketObserver);
        } else {
            getContentResolver().unregisterContentObserver(marketObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false)) {
            getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, usbDebugObserver);
        } else {
            getContentResolver().unregisterContentObserver(usbDebugObserver);
        }
    }

    private void runKeepHomeApp() {
        Handler handler = new Handler(getMainLooper()) {

            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (!Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
                    // 機能無効
                    return;
                }

                if (getDefaultLauncherPackageName() != null) {
                    if (!getDefaultLauncherPackageName().equals(Preferences.load(getBaseContext(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE, Constants.DEF_STR))) {
                        // ホームアプリ不一致
                        new DchaServiceUtil(getBaseContext()).setPreferredHomeApp(getDefaultLauncherPackageName(),
                                Preferences.load(getBaseContext(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE, Constants.DEF_STR), object -> {
                                });
                    }
                }
                sendEmptyMessageDelayed(0, 10000);
            }
        };

        if (Preferences.load(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME, false)) {
            // 機能有効
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
    final ContentObserver dchaStateObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.System.getInt(getContentResolver(), Constants.DCHA_STATE) != 0) {
                    // DCHA_STATE が0でない
                    new DchaServiceUtil(getBaseContext()).setSetupStatus(0, object -> {
                    });
                }
            } catch (Settings.SettingNotFoundException | SecurityException ignored) {
            }
        }
    };

    /* ナビゲーションバーオブサーバー */
    final ContentObserver navigationBarObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.System.getInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != 0) {
                    // HIDE_NAVIGATION_BAR が0でない
                    new DchaServiceUtil(getBaseContext()).hideNavigationBar(false, object -> {
                    });
                }
            } catch (Settings.SettingNotFoundException | SecurityException ignored) {
            }
        }
    };

    /* 提供元不明オブサーバー */
    final ContentObserver marketObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 1) {
                    // INSTALL_NON_MARKET_APPS が1でない
                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);
                }
            } catch (Settings.SettingNotFoundException | SecurityException ignored) {
                Preferences.save(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP, false);
            }
        }
    };

    /* UDBデバッグオブサーバー */
    final ContentObserver usbDebugObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED) != 1) {
                    // ADB_ENABLED が1でない
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (getBaseContext().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                            Preferences.save(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                            return;
                        }
                    }
                    Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

                    if (Common.getDchaCompletedPast()) {
                        // COUNT_DCHA_COMPLETED 存在
                        new DchaServiceUtil(getBaseContext()).setSetupStatus(3, object -> {
                            if (object.equals(true)) {
                                try {
                                    Thread.sleep(100);
                                    if (!adbEnabled()) {
                                        // 設定変更失敗
                                        Preferences.save(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                                    }
                                    // setupStatusを戻す
                                    new DchaServiceUtil(getBaseContext()).setSetupStatus(0, object1 -> {
                                    });
                                } catch (InterruptedException ignored) {
                                    // スレッド中断
                                    // setupStatusを戻す
                                    new DchaServiceUtil(getBaseContext()).setSetupStatus(0, object1 -> {
                                    });
                                }
                            } else {
                                // 失敗
                                Preferences.save(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                            }
                        });
                    } else {
                        // COUNT_DCHA_COMPLETED 存在しない
                        if (!adbEnabled()) {
                            // 設定変更失敗
                            Preferences.save(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                        }
                    }
                }
            } catch (Settings.SettingNotFoundException | SecurityException ignored) {
                Preferences.save(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
            }
        }
    };

    /**
     * @noinspection BooleanMethodIsAlwaysInverted
     */
    private boolean adbEnabled() {
        try {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
            Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, 1);
            if (Common.isBenesseExtensionExist("getDchaState")) {
                Settings.System.putInt(getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 1);
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
