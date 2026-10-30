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

import android.content.ActivityNotFoundException;
import android.content.Intent;
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

public class EmergencyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Common.isFlagEnabled(this, Constants.KEY_FLAG_NORMAL_ENV)) {
            Toast.makeText(this, R.string.toast_normal_env_finish, Toast.LENGTH_SHORT).show();
            finishAfterToast();
            return;
        }

        // 初期設定が完了していない場合は終了
        if (!Common.isFlagEnabled(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE)) {
            Toast.makeText(this, R.string.toast_no_setting_app, Toast.LENGTH_SHORT).show();
            finishAfterToast();
            return;
        }

        if (!Common.isFlagEnabled(this, Constants.KEY_FLAG_DCHA_FUNCTION)) {
            Toast.makeText(this, R.string.toast_enable_dcha, Toast.LENGTH_SHORT).show();
            finishAfterToast();
            return;
        }
        // メイン処理
        switch (Common.getPref(this, Constants.KEY_EMERGENCY_MODE)) {
            // 小学講座
            case Constants.COURSE_MODE_ELEMENTARY:
                run(Constants.PKG_SHO_HOME, Constants.HOME_SHO);
                break;
            // 中学講座
            case Constants.COURSE_MODE_JUNIOR:
                run(Constants.PKG_CHU_HOME, Constants.HOME_CHU);
                break;
            default:
                Toast.makeText(this, R.string.toast_select_course, Toast.LENGTH_SHORT).show();
                finishAfterToast();
                break;
        }
    }

    private boolean isFlagContains(int val) {
        return Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, val);
    }

    private void run(String packageName, String className) {
        disableKeepService();
        if (isFlagContains(Constants.EMERGENCY_SETTING_SYSTEM_UI_LEARNING)) setSetupStatus();
        if (isFlagContains(Constants.EMERGENCY_SETTING_HIDE_NAVIGATION_BAR)) hideNavigationBar();
        if (isFlagContains(Constants.EMERGENCY_SETTING_CLEAR_HISTORY)) removeTask();

        if (isFlagContains(Constants.EMERGENCY_SETTING_HOME_LEARNING)) {
            setHomeApp(packageName, () -> continueRun(packageName, className));
        } else {
            continueRun(packageName, className);
        }
    }

    private void continueRun(String packageName, String className) {
        Common.resetResolution(this);
        if (isFlagContains(Constants.EMERGENCY_SETTING_START_HOME)) startHomeApp(packageName, className);
        Toast.makeText(this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
        finishAfterToast();
    }

    private void finishAfterToast() {
        new Handler(Looper.getMainLooper()).postDelayed(this::finishAndRemoveTask, getResources().getInteger(R.integer.toast_wait_ms));
    }

    // 維持サービスの無効化
    private void disableKeepService() {
        if (isFlagContains(Constants.EMERGENCY_SETTING_SYSTEM_UI_LEARNING)) {
            Common.setFlagEnabled(this, Constants.KEY_FLAG_KEEP_DCHA_STATE, false);
        }

        if (isFlagContains(Constants.EMERGENCY_SETTING_HIDE_NAVIGATION_BAR)) {
            Common.setFlagEnabled(this, Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false);
        }

        if (isFlagContains(Constants.EMERGENCY_SETTING_HOME_LEARNING)) {
            Common.setFlagEnabled(this, Constants.KEY_FLAG_KEEP_HOME, false);
        }
        Common.startKeepService(this);
    }

    // 勉強ホームを起動
    private void startHomeApp(String packageName, String className) {
        try {
            startActivity(new Intent().setClassName(packageName, className));
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, R.string.toast_no_course, Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
            Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
        }
    }

    // DchaState 変更
    private void setSetupStatus() {
        if (Common.isCTX() || Common.isCTZ()) {
            BenesseExtension.setDchaState(Constants.DIGICHALIZE_STATUS_DIGICHALIZED);
        } else {
            Common.setDchaState(this, Constants.DIGICHALIZE_STATUS_DIGICHALIZED, () ->
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show());
        }
    }

    // ナビバー表示設定
    private void hideNavigationBar() {
        Common.setNavigationBar(this, true, () ->
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show());
    }

    // ホームを変更
    private void setHomeApp(String packageName, Runnable onComplete) {
        ResolveInfo resolveInfo = Common.resolveHomeActivity(this);

        if (resolveInfo == null) {
            Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
            onComplete.run();
            return;
        }
        new DchaServiceUtil(this).setPreferredHomeApp(resolveInfo.activityInfo.packageName, packageName, object -> {
            if (!Boolean.TRUE.equals(object)) {
                // 失敗
                Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
            }
            onComplete.run();
        });
    }

    // アプリ履歴の消去
    private void removeTask() {
        new DchaServiceUtil(this).removeTask(null, object -> {
            if (!object.equals(true)) {
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
