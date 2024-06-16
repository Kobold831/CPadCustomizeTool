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

package com.saradabar.cpadcustomizetool.view.activity;

import static com.saradabar.cpadcustomizetool.util.Common.isCfmDialog;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
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
import android.widget.Toast;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class EmergencyActivity extends Activity {

    IDchaService mDchaService;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
        new Handler().postDelayed(() -> {
            if (!startCheck()) {
                Toast.makeText(this, R.string.toast_not_completed_settings, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
                return;
            }

            if (!setSystemSettings(true)) {
                Toast.makeText(this, R.string.toast_not_change, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
                return;
            }

            switch (Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(this).getString("emergency_mode", ""))) {
                case "1":
                    if (setDchaSettings("jp.co.benesse.touch.allgrade.b003.touchhomelauncher", "jp.co.benesse.touch.allgrade.b003.touchhomelauncher.HomeLauncherActivity")) {
                        Toast.makeText(this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                    }
                    finishAndRemoveTask();
                    break;
                case "2":
                    if (setDchaSettings("jp.co.benesse.touch.home", "jp.co.benesse.touch.home.LoadingActivity")) {
                        Toast.makeText(this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                    }
                    finishAndRemoveTask();
                    break;
            }
        }, 10);
    }

    private boolean startCheck() {
        return Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false);
    }

    private boolean setSystemSettings(boolean study) {
        if (study) {
            SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);

            if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                sp.edit()
                        .putBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false)
                        .putBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false)
                        .putBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)
                        .apply();
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
                if (Preferences.isEmergencySettingsDchaState(this)) {
                    if (isCfmDialog(this)) {
                        Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 3);
                    }
                }

                if (Preferences.isEmergencySettingsNavigationBar(this))
                    Settings.System.putInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 1);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        } else {
            try {
                if (Preferences.isEmergencySettingsDchaState(this)) {
                    if (isCfmDialog(this)) {
                        Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 0);
                    }
                }

                if (Preferences.isEmergencySettingsNavigationBar(this))
                    Settings.System.putInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }

    private boolean setDchaSettings(String packageName, String className) {
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        ActivityInfo activityInfo = null;

        if (resolveInfo != null) {
            activityInfo = resolveInfo.activityInfo;
        }

        try {
            startActivity(new Intent().setClassName(packageName, className));
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.toast_not_course, Toast.LENGTH_SHORT).show();
        }

        if (!Preferences.isEmergencySettingsLauncher(this) && !Preferences.isEmergencySettingsRemoveTask(this))
            return true;

        if (!Preferences.load(this, Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            Toast.makeText(this, R.string.toast_use_not_dcha, Toast.LENGTH_SHORT).show();
            setSystemSettings(false);
            return false;
        }

        if (Preferences.isEmergencySettingsLauncher(this)) {
            try {
                if (activityInfo != null) {
                    mDchaService.clearDefaultPreferredApp(activityInfo.packageName);
                    mDchaService.setDefaultPreferredHomeApp(packageName);
                }
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.toast_not_install_launcher, Toast.LENGTH_SHORT).show();
                setSystemSettings(false);
                finishAndRemoveTask();
            }
        }

        if (Preferences.isEmergencySettingsRemoveTask(this)) {
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
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        finishAndRemoveTask();
    }
}
