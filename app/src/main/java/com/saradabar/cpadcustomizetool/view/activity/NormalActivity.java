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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;

import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class NormalActivity extends Activity {

    IDchaService mDchaService;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);

        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

        Runnable runnable = () -> {
            if (!startCheck()) {
                Toast.toast(this, R.string.toast_not_completed_settings);
                finishAndRemoveTask();
                return;
            }

            if (!setSystemSettings()) {
                Toast.toast(this, R.string.toast_not_install_launcher);
                finishAndRemoveTask();
                return;
            }

            if (setDchaSettings()) {
                switch (Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(this).getString("emergency_mode", ""))) {
                    case "1":
                        activityManager.killBackgroundProcesses("jp.co.benesse.touch.allgrade.b003.touchhomelauncher");
                    case "2":
                        activityManager.killBackgroundProcesses("jp.co.benesse.touch.home");
                }
                Toast.toast(this, R.string.toast_execution);
            }
            finishAndRemoveTask();
        };

        new Handler().postDelayed(runnable, 10);
    }

    private boolean startCheck() {
        return Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false);
    }

    private boolean setSystemSettings() {
        ContentResolver resolver = getContentResolver();

        if (Preferences.isNormalModeSettingsDchaState(this)) {
            if (isCfmDialog(this)) {
                Settings.System.putInt(resolver, Constants.DCHA_STATE, 0);
            }
        }

        if (Preferences.isNormalModeSettingsNavigationBar(this))
            Settings.System.putInt(resolver, Constants.HIDE_NAVIGATION_BAR, 0);

        if (Objects.equals(Preferences.load(this, Constants.KEY_NORMAL_LAUNCHER, ""), ""))
            return false;

        if (Preferences.isNormalModeSettingsActivity(this)) {
            try {
                PackageManager pm = getPackageManager();
                Intent intent = pm.getLaunchIntentForPackage(Preferences.load(this, Constants.KEY_NORMAL_LAUNCHER, ""));
                startActivity(intent);
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    private boolean setDchaSettings() {
        if (!Preferences.load(getApplicationContext(), Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            Toast.toast(getApplicationContext(), R.string.toast_use_not_dcha);
            return false;
        }

        ActivityInfo activityInfo = null;
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) activityInfo = resolveInfo.activityInfo;

        if (Preferences.isNormalModeSettingsLauncher(getApplicationContext())) {
            try {
                if (activityInfo != null) {
                    mDchaService.clearDefaultPreferredApp(activityInfo.packageName);
                    mDchaService.setDefaultPreferredHomeApp(Preferences.load(getApplicationContext(), Constants.KEY_NORMAL_LAUNCHER, ""));
                }
            } catch (RemoteException ignored) {
                Toast.toast(getApplicationContext(), R.string.toast_not_install_launcher);
                finishAndRemoveTask();
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
