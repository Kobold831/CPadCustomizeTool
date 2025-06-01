package com.saradabar.cpadcustomizetool;

import android.app.Application;

import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;

public class MyApplication extends Application {

    private static MyApplication myApplication = null;

    public static MyApplication getContext() {
        return myApplication;
    }

    public InstallEventListener installEventListener;

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
    }
}
