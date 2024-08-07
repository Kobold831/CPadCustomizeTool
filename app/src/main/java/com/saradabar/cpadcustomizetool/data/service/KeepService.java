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

import android.annotation.SuppressLint;
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
import android.support.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class KeepService extends Service {

    @SuppressLint("StaticFieldLeak")
    public static KeepService instance = null;

    public Binder binder = new Binder();

    private boolean isNavigationObserverEnable = false;
    private boolean isUiObserverEnable = false;
    private boolean isUnknownObserverEnable = false;
    private boolean isUsbObserverEnable = false;
    private boolean isHomeObserverEnable = false;

    public static KeepService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* オブザーバーを有効化 */
        if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_SERVICE, false)) {
            isNavigationObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, NavigationObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_DCHA_STATE, false)) {
            isUiObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, DchaStateObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false)) {
            isUnknownObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, MarketObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_USB_DEBUG, false)) {
            isUsbObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, UsbDebugObserver);
        }

        if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_HOME, false)) {
            isHomeObserverEnable = true;
            runKeepDefaultLauncher();
        }

        if (!Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_SERVICE, false) && !Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_HOME, false)) {
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
                if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_SERVICE, false) || Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_HOME, false)) {
                    startService(new Intent(getBaseContext(), ProtectKeepService.class));
                }
            }
        }, Context.BIND_AUTO_CREATE);

        return START_STICKY;
    }

    @SuppressWarnings("deprecation")
    public void startService() {
        /* オブザーバーを有効化 */
        if (!isNavigationObserverEnable) {
            if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_SERVICE, false)) {
                isNavigationObserverEnable = true;
                getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, NavigationObserver);
            }
        }

        if (!isUiObserverEnable) {
            if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_DCHA_STATE, false)) {
                isUiObserverEnable = true;
                getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, DchaStateObserver);
            }
        }

        if (!isUnknownObserverEnable) {
            if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false)) {
                isUnknownObserverEnable = true;
                getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, MarketObserver);
            }
        }

        if (!isUsbObserverEnable) {
            if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_USB_DEBUG, false)) {
                isUsbObserverEnable = true;
                getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, UsbDebugObserver);
            }
        }

        if (!isHomeObserverEnable) {
            if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_HOME, false)) {
                isHomeObserverEnable = true;
                runKeepDefaultLauncher();
            }
        }
    }

    public void stopService(int stopCode) {
        /* オブサーバーを無効化 */
        switch (stopCode) {
            /* 全停止 */
            case 0:
                if (isNavigationObserverEnable) {
                    getContentResolver().unregisterContentObserver(NavigationObserver);
                    isNavigationObserverEnable = false;
                }
                if (isUiObserverEnable) {
                    getContentResolver().unregisterContentObserver(DchaStateObserver);
                    isUiObserverEnable = false;
                }
                if (isUnknownObserverEnable) {
                    getContentResolver().unregisterContentObserver(MarketObserver);
                    isUnknownObserverEnable = false;
                }
                if (isUsbObserverEnable) {
                    getContentResolver().unregisterContentObserver(UsbDebugObserver);
                    isUsbObserverEnable = false;
                }
                if (isHomeObserverEnable) {
                    isHomeObserverEnable = false;
                }
                break;
            /* システムUI */
            case 1:
                if (isUiObserverEnable) {
                    getContentResolver().unregisterContentObserver(DchaStateObserver);
                    isUiObserverEnable = false;
                }
                break;
            case 2:
                if (isNavigationObserverEnable) {
                    getContentResolver().unregisterContentObserver(NavigationObserver);
                    isNavigationObserverEnable = false;
                }
                break;
            case 3:
                if (isUnknownObserverEnable) {
                    getContentResolver().unregisterContentObserver(MarketObserver);
                    isUnknownObserverEnable = false;
                }
                break;
            case 4:
                if (isUsbObserverEnable) {
                    Settings.System.putInt(getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 0);
                    getContentResolver().unregisterContentObserver(UsbDebugObserver);
                    isUsbObserverEnable = false;
                }
                break;
            case 5:
                if (isHomeObserverEnable) {
                    isHomeObserverEnable = false;
                }
                break;
        }

        /* 全機能が無効ならサービス停止 */
        if (!Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_SERVICE, false) && !Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_HOME, false)) {
            stopService(new Intent(getBaseContext(), ProtectKeepService.class));
            stopSelf();
        }
    }

    private void runKeepDefaultLauncher() {
        Handler handler = new Handler(getMainLooper()) {

            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                if (!isHomeObserverEnable) {
                    return;
                }

                if (getDefaultLauncherPackageName() != null) {
                    if (Preferences.load(getBaseContext(), Constants.KEY_ENABLED_KEEP_HOME, false) && !getDefaultLauncherPackageName().equals(Preferences.load(getBaseContext(), Constants.KEY_SAVE_KEEP_HOME, null))) {
                        bindService(Constants.DCHA_SERVICE, new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                                try {
                                    IDchaService mDchaService = IDchaService.Stub.asInterface(iBinder);
                                    mDchaService.clearDefaultPreferredApp(getDefaultLauncherPackageName());
                                    mDchaService.setDefaultPreferredHomeApp(Preferences.load(getBaseContext(), Constants.KEY_SAVE_KEEP_HOME, null));
                                } catch (Exception e) {
                                    Common.LogOverWrite(getBaseContext(), e);
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

        handler.sendEmptyMessageDelayed(0, 0);
    }

    private String getDefaultLauncherPackageName() {
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        } else {
            return null;
        }
    }

    /* DchaStateオブサーバー */
    ContentObserver DchaStateObserver = new ContentObserver(new Handler()) {

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
    ContentObserver NavigationObserver = new ContentObserver(new Handler()) {

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
    @SuppressWarnings("deprecation")
    ContentObserver MarketObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            try {
                if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) == 0) {
                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);
                }
            } catch (Exception ignored) {
                Preferences.save(getBaseContext(), Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false);
            }
        }
    };

    /* UDBデバッグオブサーバー */
    ContentObserver UsbDebugObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            try {
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED) == 0) {
                    if (Preferences.load(getBaseContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(getBaseContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        Settings.System.putInt(getBaseContext().getContentResolver(), Constants.DCHA_STATE, 3);
                        Thread.sleep(100);
                    }

                    Settings.Global.putInt(getBaseContext().getContentResolver(), Settings.Global.ADB_ENABLED, 1);

                    if (Preferences.load(getBaseContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(getBaseContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        Settings.System.putInt(getBaseContext().getContentResolver(), Constants.DCHA_STATE, 0);
                    }
                }

                Settings.System.putInt(getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 1);
            } catch (Exception ignored) {
                Preferences.save(getBaseContext(), Constants.KEY_ENABLED_AUTO_USB_DEBUG, false);
                if (Preferences.load(getBaseContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX || Preferences.load(getBaseContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                    Settings.System.putInt(getBaseContext().getContentResolver(), Constants.DCHA_STATE, 0);
                }
            }
        }
    };
}
