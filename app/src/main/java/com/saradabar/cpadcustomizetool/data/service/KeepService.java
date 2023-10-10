package com.saradabar.cpadcustomizetool.data.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class KeepService extends Service {

    IDchaService mDchaService;

    private boolean isNavigationObserverEnable = false;
    private boolean isUiObserverEnable = false;
    private boolean isUnknownObserverEnable = false;
    private boolean isUsbObserverEnable = false;
    private boolean isHomeObserverEnable = false;

    static KeepService instance = null;

    public static KeepService getInstance() {
        return instance;
    }

    private void KeepHome() {
        bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
        Runnable runnable = () -> {
            SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
            if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false) && !getHomePackageName().equals(sp.getString(Constants.KEY_SAVE_KEEP_HOME, null)) && mDchaService != null) {
                try {
                    mDchaService.clearDefaultPreferredApp(getHomePackageName());
                    mDchaService.setDefaultPreferredHomeApp(sp.getString(Constants.KEY_SAVE_KEEP_HOME, null));
                } catch (Exception ex) {
                    CrashHandler.LogOverWrite(ex, this);
                }
            }
        };
        new Handler().postDelayed(runnable, 1000);
    }

    private String getHomePackageName() {
        return getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0).activityInfo.packageName;
    }

    ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaService = IDchaService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDchaService = null;
        }
    };

    ContentObserver DchaStateObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.System.getInt(getContentResolver(), Constants.DCHA_STATE) == 3) {
                    Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 0);
                }
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    ContentObserver NavigationObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.System.getInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR) == 1) {
                    Settings.System.putInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
                }
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    ContentObserver MarketObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) == 0) {
                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);
                }
            } catch (Exception ignored) {
                getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false).apply();
                stopSelf();
            }
        }
    };

    ContentObserver UsbDebugObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                if (Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED) == 0) {
                    if (Preferences.GET_MODEL_ID(getApplicationContext()) == 2) {
                        Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 3);
                    }
                    Thread.sleep(100);
                    Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                    if (Preferences.GET_MODEL_ID(getApplicationContext()) == 2) {
                        Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 0);
                    }
                }
            } catch (Exception ignored) {
                if (Preferences.GET_MODEL_ID(getApplicationContext()) == 2) {
                    Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 0);
                }
                getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false).apply();
                stopSelf();
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));
        instance = this;
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        /* オブザーバーを有効化 */
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false)) {
            isNavigationObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, NavigationObserver);
        }
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false)) {
            isUiObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, DchaStateObserver);
        }
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false)) {
            isUnknownObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, MarketObserver);
        }
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false)) {
            isUsbObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, UsbDebugObserver);
        }
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            isHomeObserverEnable = true;
            KeepHome();
        }
        if (!sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            return START_NOT_STICKY;
        }
        tryBind();
        return START_STICKY;
    }

    private void tryBind() {
        bindService(Constants.PROTECT_KEEP_SERVICE, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
            if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                startService(Constants.PROTECT_KEEP_SERVICE);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            startService(Constants.KEEP_SERVICE);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startService() {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        /* オブザーバーを有効化 */
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false)) {
            isNavigationObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, NavigationObserver);
        }
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false)) {
            isUiObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, DchaStateObserver);
        }
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false)) {
            isUnknownObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, MarketObserver);
        }
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false)) {
            isUsbObserverEnable = true;
            getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, UsbDebugObserver);
        }
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            isHomeObserverEnable = true;
            KeepHome();
        }
    }

    public void stopService(int stopCode) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        /* オブサーバーを無効化 */
        switch (stopCode) {
            case 1:
                if (isNavigationObserverEnable) {
                    getContentResolver().unregisterContentObserver(NavigationObserver);
                    isNavigationObserverEnable = false;
                }
                break;
            case 2:
                if (isUiObserverEnable) {
                    getContentResolver().unregisterContentObserver(DchaStateObserver);
                    isUiObserverEnable = false;
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
                    getContentResolver().unregisterContentObserver(UsbDebugObserver);
                    isUsbObserverEnable = false;
                }
                break;
            case 5:
                if (isHomeObserverEnable) {
                    isHomeObserverEnable = false;
                }
                break;
            case 6:
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
                stopService(Constants.KEEP_SERVICE);
                stopService(Constants.PROTECT_KEEP_SERVICE);
                break;
        }
        if (!sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            stopService(Constants.KEEP_SERVICE);
            stopService(Constants.PROTECT_KEEP_SERVICE);
        }
    }
}