package com.saradabar.cpadcustomizetool;

import android.app.Application;

import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;

public class MyApplication extends Application {

    public InstallEventListener installEventListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
    }
}
