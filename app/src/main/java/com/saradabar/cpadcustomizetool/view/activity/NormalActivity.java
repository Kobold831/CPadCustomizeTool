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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class NormalActivity extends Activity {

    IDchaService mDchaService;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(Constants.DCHA_SERVICE, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mDchaService = IDchaService.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);
        new Handler().postDelayed(() -> {
            if (!Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
                Toast.makeText(this, R.string.toast_not_completed_settings, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
                return;
            }

            if (!setSystemSettings()) {
                Toast.makeText(this, R.string.toast_not_install_launcher, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
                return;
            }

            if (setDchaSettings()) {
                ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
                switch (Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(this).getString("emergency_mode", ""))) {
                    case "1":
                        activityManager.killBackgroundProcesses("jp.co.benesse.touch.allgrade.b003.touchhomelauncher");
                        break;
                    case "2":
                        activityManager.killBackgroundProcesses("jp.co.benesse.touch.home");
                        break;
                }
                Toast.makeText(this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
            }
        }, 1000);
    }

    private boolean setSystemSettings() {
        if (Preferences.isNormalModeSettingsDchaState(this)) {
            Settings.System.putInt(getContentResolver(), Constants.DCHA_STATE, 0);
        }

        if (Preferences.isNormalModeSettingsNavigationBar(this)) {
            Settings.System.putInt(getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
        }

        if (Preferences.isNormalModeSettingsActivity(this)) {
            if (Objects.equals(Preferences.load(this, Constants.KEY_NORMAL_LAUNCHER, ""), "")) {
                return false;
            }
            try {
                startActivity(getPackageManager().getLaunchIntentForPackage(Preferences.load(this, Constants.KEY_NORMAL_LAUNCHER, "")));
            } catch (Exception ignored) {
                return false;
            }
        }
        return true;
    }

    private boolean setDchaSettings() {
        if (!Preferences.load(this, Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            Toast.makeText(this, R.string.toast_use_not_dcha, Toast.LENGTH_SHORT).show();
            return false;
        }

        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        ActivityInfo activityInfo;

        if (resolveInfo != null) {
            activityInfo = resolveInfo.activityInfo;
        } else {
            Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (Preferences.isNormalModeSettingsLauncher(this)) {
            try {
                if (activityInfo != null) {
                    mDchaService.clearDefaultPreferredApp(activityInfo.packageName);
                    mDchaService.setDefaultPreferredHomeApp(Preferences.load(this, Constants.KEY_NORMAL_LAUNCHER, ""));
                }
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.toast_not_install_launcher, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        finishAndRemoveTask();
    }
}
