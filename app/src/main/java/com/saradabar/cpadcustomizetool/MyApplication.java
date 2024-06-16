package com.saradabar.cpadcustomizetool;

import android.app.Application;

import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;

public class MyApplication extends Application {

    public IDhizukuService mDhizukuService;

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
    }
}
