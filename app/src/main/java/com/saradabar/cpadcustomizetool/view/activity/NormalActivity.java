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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DchaUtilServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class NormalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初期設定が完了していない場合は終了
        if (!Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, Constants.DEF_BOOL)) {
            Toast.makeText(this, R.string.toast_no_setting_app, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
            return;
        }

        if (!Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, Constants.DEF_BOOL)) {
            Toast.makeText(this, R.string.toast_enable_dcha, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
            return;
        }
        // メイン処理
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        switch (PreferenceManager.getDefaultSharedPreferences(this).getString("emergency_mode", "")) {
            // 小学講座
            case "1":
                run();
                activityManager.killBackgroundProcesses(Constants.PKG_SHO_HOME);
                break;
            // 中学講座
            case "2":
                run();
                activityManager.killBackgroundProcesses(Constants.PKG_CHU_HOME);
                break;
        }
    }

    private void run() {
        startHomeApp();
        setSetupStatus();
    }

    // ホームを起動
    private void startHomeApp() {
        if (Preferences.loadMultiList(this, Constants.KEY_NORMAL_SETTINGS, 4)) {
            // 変更するホームが設定されているかチェック
            if (Preferences.load(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, "").isEmpty()) {
                Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                startActivity(getPackageManager().getLaunchIntentForPackage(Preferences.load(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, "")));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // DchaState 変更
    private void setSetupStatus() {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 1)) {
            if (Common.isBenesseExtensionExist("setDchaState")) {
                BenesseExtension.setDchaState(0);
                showNavigationBar();
                return;
            }
            new DchaServiceUtil(this).setSetupStatus(0, object -> {
                if (!object.equals(true)) {
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                }
                showNavigationBar();
            });
        } else {
            showNavigationBar();
        }
    }

    // ナビバー表示設定
    private void showNavigationBar() {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 2)) {
            new DchaServiceUtil(this).hideNavigationBar(false, object -> {
                if (!object.equals(true)) {
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                }
                setDisplaySize();
            });
        } else {
            setDisplaySize();
        }
    }

    // 解像度の修正
    private void setDisplaySize() {
        if (Common.isCTX() || Common.isCTZ()) {
            // CTXとCTZ
            if (Common.isBenesseExtensionExist("putInt")) {
                //noinspection ResultOfMethodCallIgnored
                BenesseExtension.putInt(Constants.BC_COMPATSCREEN, 0);
            } else {
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
            }
            setHomeApp();
        } else {
            // CT2とCT3
            new DchaUtilServiceUtil(this).setForcedDisplaySize(1280, 800, object -> {
                if (!object.equals(true)) {
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                }
                setHomeApp();
            });
        }
    }

    // ホームを変更
    private void setHomeApp() {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 3)) {
            // 変更するホームが設定されているかチェック
            if (Preferences.load(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, "").isEmpty()) {
                Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
                return;
            }
            ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

            if (resolveInfo == null) {
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
                return;
            }
            new DchaServiceUtil(this).setPreferredHomeApp(resolveInfo.activityInfo.packageName,
                    Preferences.load(this, Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, ""), object -> {
                        if (!object.equals(true)) {
                            // 失敗
                            Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(NormalActivity.this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                        finishAndRemoveTask();
                    });
        } else {
            Toast.makeText(NormalActivity.this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finishAndRemoveTask();
    }
}
