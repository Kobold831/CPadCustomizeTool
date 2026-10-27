package com.saradabar.cpadcustomizetool;

import android.app.ActivityManager;
import android.app.Application;
import android.os.Build;
import android.os.Process;

import androidx.annotation.Nullable;

import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceConnection;

import java.lang.ref.WeakReference;
import java.util.List;

public class MyApplication extends Application {

    private static MyApplication myApplication = null;

    public static MyApplication getContext() {
        return myApplication;
    }

    private WeakReference<InstallEventListener> installEventListenerRef;

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        if (isMainProcess() && Common.isFlagEnabled(this, Constants.KEY_FLAG_DCHA_FUNCTION)) {
            DchaServiceConnection.ensureBound(this);
        }
    }

    private boolean isMainProcess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return getPackageName().equals(getProcessName());
        }

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        if (activityManager == null) {
            return true;
        }

        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();

        if (processes == null) {
            return true;
        }

        int pid = Process.myPid();

        for (ActivityManager.RunningAppProcessInfo info : processes) {
            if (info.pid == pid) {
                return getPackageName().equals(info.processName);
            }
        }
        return true;
    }

    public synchronized void setInstallEventListener(@Nullable InstallEventListener listener) {
        installEventListenerRef = listener == null ? null : new WeakReference<>(listener);
    }

    @Nullable
    public synchronized InstallEventListener getInstallEventListener() {
        return installEventListenerRef == null ? null : installEventListenerRef.get();
    }
}
