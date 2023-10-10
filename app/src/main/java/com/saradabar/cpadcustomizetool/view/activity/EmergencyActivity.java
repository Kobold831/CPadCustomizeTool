package com.saradabar.cpadcustomizetool.view.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;

import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class EmergencyActivity extends Activity {

    IDchaService mDchaService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
        Runnable runnable = () -> {
            if (!startCheck()) {
                Toast.toast(this, R.string.toast_not_completed_settings);
                finishAndRemoveTask();
                return;
            }
            if (!setSystemSettings(true)) {
                Toast.toast(this, R.string.toast_not_change);
                finishAndRemoveTask();
                return;
            }
            switch (PreferenceManager.getDefaultSharedPreferences(this).getString("emergency_mode", "")) {
                case "1":
                    if (setDchaSettings("jp.co.benesse.touch.allgrade.b003.touchhomelauncher", "jp.co.benesse.touch.allgrade.b003.touchhomelauncher.HomeLauncherActivity")) {
                        Toast.toast(this, R.string.toast_execution);
                    }
                    finishAndRemoveTask();
                    break;
                case "2":
                    if (setDchaSettings("jp.co.benesse.touch.home", "jp.co.benesse.touch.home.LoadingActivity")) {
                        Toast.toast(this, R.string.toast_execution);
                    }
                    finishAndRemoveTask();
                    break;
            }
        };
        new Handler().postDelayed(runnable, 10);
    }

    private boolean startCheck() {
        return Preferences.GET_SETTINGS_FLAG(this);
    }

    private boolean setSystemSettings(boolean study) {
        ContentResolver resolver = getContentResolver();

        if (study) {
            SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);

            if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                SharedPreferences.Editor spe = sp.edit();
                spe.putBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false);
                spe.putBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false);
                spe.putBoolean(Constants.KEY_ENABLED_KEEP_HOME, false);
                spe.apply();
                ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

                for (ActivityManager.RunningServiceInfo serviceInfo : Objects.requireNonNull(manager).getRunningServices(Integer.MAX_VALUE)) {
                    if (KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        KeepService.getInstance().stopService(1);
                        KeepService.getInstance().stopService(2);
                        KeepService.getInstance().stopService(5);
                    }
                }
            }

            try {
                if (Preferences.isEmergencySettingsDchaState(this))
                    Settings.System.putInt(resolver, Constants.DCHA_STATE, 3);

                if (Preferences.isEmergencySettingsNavigationBar(this))
                    Settings.System.putInt(resolver, Constants.HIDE_NAVIGATION_BAR, 1);
                return true;
            } catch (SecurityException ignored) {
                return false;
            }
        } else {
            try {
                if (Preferences.isEmergencySettingsDchaState(this))
                    Settings.System.putInt(resolver, Constants.DCHA_STATE, 0);

                if (Preferences.isEmergencySettingsNavigationBar(this))
                    Settings.System.putInt(resolver, Constants.HIDE_NAVIGATION_BAR, 0);
                return true;
            } catch (SecurityException ignored) {
                return false;
            }
        }
    }

    private boolean setDchaSettings(String packageName, String className) {
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        try {
            startActivity(new Intent().setClassName(packageName, className));
        } catch (Exception e) {
            Toast.toast(this, R.string.toast_not_course);
            setSystemSettings(false);
            return false;
        }

        if (!Preferences.isEmergencySettingsLauncher(this) && !Preferences.isEmergencySettingsRemoveTask(this)) return true;

        if (!Preferences.GET_DCHASERVICE_FLAG(getApplicationContext())) {
            Toast.toast(getApplicationContext(), R.string.toast_use_not_dcha);
            setSystemSettings(false);
            return false;
        }

        ActivityInfo activityInfo = null;

        if (resolveInfo != null) activityInfo = resolveInfo.activityInfo;
        if (Preferences.isEmergencySettingsLauncher(getApplicationContext())) {
            try {
                if (activityInfo != null) {
                    mDchaService.clearDefaultPreferredApp(activityInfo.packageName);
                    mDchaService.setDefaultPreferredHomeApp(packageName);
                }
            } catch (RemoteException ignored) {
                Toast.toast(getApplicationContext(), R.string.toast_not_install_launcher);
                setSystemSettings(false);
                finishAndRemoveTask();
            }
        }
        if (Preferences.isEmergencySettingsRemoveTask(getApplicationContext())) {
            try {
                mDchaService.removeTask(null);
            } catch (RemoteException ignored) {
            }
        }
        return true;
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

    @Override
    public void onPause() {
        super.onPause();
        finishAndRemoveTask();
    }
}