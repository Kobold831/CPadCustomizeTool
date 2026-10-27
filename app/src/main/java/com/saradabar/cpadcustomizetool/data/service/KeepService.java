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

package com.saradabar.cpadcustomizetool.data.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;

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

        if (Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME)) {
            // ホームアプリ維持
            runKeepHomeApp();
        }

        if (!Common.isAnyKeepFlagEnabled(getBaseContext())) {
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
                if (Common.isAnyKeepFlagEnabled(getBaseContext())) {
                    // いずれかの機能が有効
                    startService(new Intent(getBaseContext(), ProtectKeepService.class));
                }
            }
        }, Context.BIND_AUTO_CREATE);
        return START_STICKY;
    }

    private void initRegisterContentObserver() {
        if (Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR)) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, navigationBarObserver);
        } else {
            getContentResolver().unregisterContentObserver(navigationBarObserver);
        }

        if (Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_DCHA_STATE)) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, dchaStateObserver);
        } else {
            getContentResolver().unregisterContentObserver(dchaStateObserver);
        }

        if (Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP)) {
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, marketObserver);
        } else {
            getContentResolver().unregisterContentObserver(marketObserver);
        }

        if (Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG)) {
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
                if (!Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME)) {
                    // 機能無効
                    return;
                }

                if (Common.getLauncherPackage(getBaseContext()) != null) {
                    if (!Common.getLauncherPackage(getBaseContext()).equals(Common.getName(getBaseContext(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE))) {
                        // ホームアプリ不一致
                        new DchaServiceUtil(getBaseContext()).setPreferredHomeApp(Common.getLauncherPackage(getBaseContext()),
                                Common.getName(getBaseContext(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE), object -> {
                        });
                    }
                }
                sendEmptyMessageDelayed(0, getResources().getInteger(R.integer.keep_service_interval_ms));
            }
        };

        if (Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_HOME)) {
            // 機能有効
            handler.sendEmptyMessageDelayed(0, 0);
        }
    }

    /* DchaStateオブサーバー */
    final ContentObserver dchaStateObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // 維持機能が無効になっている場合は何もしない
            // (オブザーバーの解除は非同期のため、無効化直後にも呼ばれてしまう)
            if (!Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_DCHA_STATE)) {
                return;
            }
            try {
                if (BenesseExtension.getDchaState() != Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE) {
                    // DCHA_STATE が0でない
                    new DchaServiceUtil(getBaseContext()).setSetupStatus(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object -> {
                    });
                }
            } catch (SecurityException ignored) {
            }
        }
    };

    /* ナビゲーションバーオブサーバー */
    final ContentObserver navigationBarObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR)) {
                return;
            }
            try {
                if (Settings.System.getInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != Constants.SETTING_OFF) {
                    // HIDE_NAVIGATION_BAR が0でない
                    new DchaServiceUtil(getBaseContext()).hideNavigationBar(false, object -> {
                    });
                }
            } catch (Settings.SettingNotFoundException | SecurityException ignored) {
            }
        }
    };

    /* 提供元不明オブサーバー */
    final ContentObserver marketObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP)) {
                return;
            }
            try {
                if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != Constants.SETTING_ON) {
                    // INSTALL_NON_MARKET_APPS が1でない
                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, Constants.SETTING_ON);
                }
            } catch (Settings.SettingNotFoundException | SecurityException ignored) {
                Common.setFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_MARKET_APP, false);
            }
        }
    };

    /* UDBデバッグオブサーバー */
    final ContentObserver usbDebugObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!Common.isFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG)) {
                return;
            }
            try {
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED) != Constants.SETTING_ON) {
                    // ADB_ENABLED が1でない
                    if (!Common.isWriteSecureSettingsGranted(getBaseContext())) {
                        Common.setFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                        return;
                    }
                    Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, Constants.SETTING_ON);

                    if (Common.getDchaCompletedPast()) {
                        // COUNT_DCHA_COMPLETED 存在
                        if (BenesseExtension.getDchaState() == Constants.DIGICHALIZE_STATUS_DIGICHALIZED) {
                            if (!adbEnabled()) {
                                Common.setFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                            }
                            return;
                        }

                        final int previousStatus = BenesseExtension.getDchaState();
                        new DchaServiceUtil(getBaseContext()).setSetupStatus(Constants.DIGICHALIZE_STATUS_DIGICHALIZED, object -> {
                            if (object.equals(true)) {
                                try {
                                    Thread.sleep(getResources().getInteger(R.integer.retry_wait_ms));
                                    if (!adbEnabled()) {
                                        // 設定変更失敗
                                        Common.setFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                                    }
                                    // setupStatusを戻す
                                    new DchaServiceUtil(getBaseContext()).setSetupStatus(previousStatus, object1 -> {
                                    });
                                } catch (InterruptedException ignored) {
                                    // スレッド中断
                                    // setupStatusを戻す
                                    new DchaServiceUtil(getBaseContext()).setSetupStatus(previousStatus, object1 -> {
                                    });
                                }
                            } else {
                                // 失敗
                                Common.setFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                            }
                        });
                    } else {
                        // COUNT_DCHA_COMPLETED 存在しない
                        if (!adbEnabled()) {
                            // 設定変更失敗
                            Common.setFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                        }
                    }
                }
            } catch (Settings.SettingNotFoundException | SecurityException ignored) {
                Common.setFlagEnabled(getBaseContext(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
            }
        }
    };

    private boolean adbEnabled() {
        try {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, Constants.SETTING_ON);
            Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, Constants.SETTING_ON);
            if (Common.isDchaLockExists()) {
                try {
                    BenesseExtension.putInt(Constants.BC_PASSWORD_HIT_FLAG, Constants.SETTING_ON);
                } catch (NoSuchMethodError ignored) {
                    Settings.System.putInt(getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, Constants.SETTING_ON);
                }
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
