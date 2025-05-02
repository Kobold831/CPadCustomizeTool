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

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.task.IDchaUtilTask;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DchaUtilServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class NormalActivity extends AppCompatActivity {

    IDchaUtilService mUtilService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, false)) {
            Toast.makeText(this, R.string.toast_no_setting_app, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
            return;
        }

        if (!Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
            Toast.makeText(this, R.string.toast_enable_dcha, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
            return;
        }
        ActivityManager activityManager = (ActivityManager) NormalActivity.this.getSystemService(ACTIVITY_SERVICE);
        switch (PreferenceManager.getDefaultSharedPreferences(NormalActivity.this).getString("emergency_mode", "")) {
            case "1":
                if (run()) {
                    // 成功
                    activityManager.killBackgroundProcesses(Constants.PKG_SHO_HOME);
                    Toast.makeText(NormalActivity.this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                }
                finishAndRemoveTask();
                break;
            case "2":
                if (run()) {
                    // 成功
                    activityManager.killBackgroundProcesses(Constants.PKG_CHU_HOME);
                    Toast.makeText(NormalActivity.this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                }
                finishAndRemoveTask();
                break;
        }

        new IDchaUtilTask().execute(this, new IDchaUtilTask.Listener() {
            @Override
            public void onSuccess(IDchaUtilService mDchaUtilService) {
                mUtilService = mDchaUtilService;

                if (mUtilService == null) {
                    Toast.makeText(NormalActivity.this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                    finishAndRemoveTask();
                }
            }

            @Override
            public void onFailure() {
                Toast.makeText(NormalActivity.this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
            }
        });
    }

    private boolean run() {
        // ホームを起動
        if (Preferences.loadMultiList(this, Constants.KEY_NORMAL_SETTINGS, 4)) {
            // 変更するホームが設定されているかチェック
            if (Preferences.load(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, "").isEmpty()) {
                Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
                return false;
            }

            try {
                startActivity(getPackageManager().getLaunchIntentForPackage(Preferences.load(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, "")));
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // DchaState 変更
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 1)) {
            try {
                BenesseExtension.setDchaState(0);
            } catch (Exception ignored) {
                new DchaServiceUtil(this).setSetupStatus(0);
            }
        }

        // ナビバー表示設定
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 2)) {
            new DchaServiceUtil(this).hideNavigationBar(false);
        }

        // 解像度の修正
        try {
            //noinspection ResultOfMethodCallIgnored
            BenesseExtension.putInt(Constants.BC_COMPATSCREEN, 0);
        } catch (NoSuchMethodError | NoClassDefFoundError | Exception ignored) {
            new DchaUtilServiceUtil(mUtilService).setForcedDisplaySize(1280, 800);
        }

        // ホームを変更
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 3)) {
            if (resolveInfo != null) {
                if (!setPreferredHomeApp(resolveInfo.activityInfo.packageName,
                        Preferences.load(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, ""))) {
                    // 失敗
                    Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private boolean setPreferredHomeApp(String s, String s1) {
        try {
            if (new DchaServiceUtil(this).clearDefaultPreferredApp(s)) {
                return new DchaServiceUtil(this).setDefaultPreferredHomeApp(s1);
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        finishAndRemoveTask();
    }
}
