package com.saradabar.cpadcustomizetool.data.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;

import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.util.Constants;

public class ProtectKeepService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            tryBind();
            return START_STICKY;
        } else {
            return START_NOT_STICKY;
        }
    }

    private void tryBind() {
        bindService(Constants.KEEP_SERVICE, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
            if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                startService(Constants.KEEP_SERVICE);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            startService(Constants.PROTECT_KEEP_SERVICE);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}