/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
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
import android.content.ActivityNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class NormalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Common.isFlagEnabled(this, Constants.KEY_FLAG_NORMAL_ENV)) {
            finishAfterToast(R.string.toast_normal_env_finish);
            return;
        }

        // 初期設定が完了していない場合は終了
        if (!Common.isFlagEnabled(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE)) {
            finishAfterToast(R.string.toast_no_setting_app);
            return;
        }

        if (!Common.isFlagEnabled(this, Constants.KEY_FLAG_DCHA_FUNCTION)) {
            finishAfterToast(R.string.toast_enable_dcha);
            return;
        }

        // メイン処理
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        switch (Common.getPref(this, Constants.KEY_EMERGENCY_MODE)) {
            // 小学講座
            case Constants.COURSE_MODE_ELEMENTARY:
                run();
                activityManager.killBackgroundProcesses(Constants.PKG_SHO_HOME);
                break;
            // 中学講座
            case Constants.COURSE_MODE_JUNIOR:
                run();
                activityManager.killBackgroundProcesses(Constants.PKG_CHU_HOME);
                break;
            default:
                finishAfterToast(R.string.toast_set_alternative_launcher);
                break;
        }
    }

    private boolean isFlagContains(int val) {
        return Preferences.loadMultiList(this, Constants.KEY_NORMAL_SETTINGS, val);
    }

    private void run() {
        Common.resetResolution(this);
        if (isFlagContains(Constants.NORMAL_SETTING_HOME_NORMAL)) {
            setHomeApp(this::continueRun);
        } else {
            continueRun();
        }
    }

    private void continueRun() {
        if (isFlagContains(Constants.NORMAL_SETTING_START_HOME)) startHomeApp();
        if (isFlagContains(Constants.NORMAL_SETTING_SYSTEM_UI_NORMAL)) setSetupStatus();
        if (isFlagContains(Constants.NORMAL_SETTING_SHOW_NAVIGATION_BAR)) showNavigationBar();
        finishAfterToast(R.string.toast_execution);
    }

    private void finishAfterToast(int resId) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show();
        new Handler(Looper.getMainLooper()).postDelayed(this::finishAndRemoveTask, getResources().getInteger(R.integer.toast_wait_ms));
    }

    // ホームを起動
    private void startHomeApp() {
        // 変更するホームが設定されているかチェック
        if (Common.getName(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE).isEmpty()) {
            Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            startActivity(getPackageManager().getLaunchIntentForPackage(Preferences.load(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, Constants.DEF_STR)));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
        }

    }

    // DchaState 変更
    private void setSetupStatus() {
        if (Common.isCTX() || Common.isCTZ()) {
            BenesseExtension.setDchaState(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE);
        } else {
            Common.setDchaState(this, Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, () ->
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show());
        }
    }

    // ナビバー表示設定
    private void showNavigationBar() {
        Common.setNavigationBar(this, false, () ->
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show());
    }

    // ホームを変更
    private void setHomeApp(Runnable onComplete) {
        // 変更するホームが設定されているかチェック
        if (Common.getName(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE).isEmpty()) {
            finishAfterToast(R.string.toast_no_home);
            return;
        }
        ResolveInfo resolveInfo = Common.resolveHomeActivity(this);

        if (resolveInfo == null) {
            finishAfterToast(R.string.dialog_error);
            return;
        }
        new DchaServiceUtil(this).setPreferredHomeApp(resolveInfo.activityInfo.packageName,
                Common.getName(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE), object -> {
                    if (!Boolean.TRUE.equals(object)) {
                        // 失敗
                        Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
                    }
                    onComplete.run();
                });
    }
}
