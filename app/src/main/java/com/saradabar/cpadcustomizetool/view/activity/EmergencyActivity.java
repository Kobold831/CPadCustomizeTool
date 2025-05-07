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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DchaUtilServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class EmergencyActivity extends AppCompatActivity {

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
        switch (PreferenceManager.getDefaultSharedPreferences(this).getString("emergency_mode", "")) {
            // 小学講座
            case "1":
                run(Constants.PKG_SHO_HOME, Constants.HOME_SHO);
                break;
            // 中学講座
            case "2":
                run(Constants.PKG_CHU_HOME, Constants.HOME_CHU);
                break;
        }
    }

    private void run(String packageName, String className) {
        disableKeepService();
        startHomeApp(packageName, className);
        setSetupStatus(packageName);
    }

    // 維持サービスの無効化
    private void disableKeepService() {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 1)) {
            Preferences.save(this, Constants.KEY_FLAG_KEEP_DCHA_STATE, false);
        }

        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 2)) {
            Preferences.save(this, Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false);
        }

        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 3)) {
            Preferences.save(this, Constants.KEY_FLAG_KEEP_HOME, false);
        }
        startService(new Intent(this, KeepService.class));
        startService(new Intent(this, ProtectKeepService.class));
    }

    // 勉強ホームを起動
    private void startHomeApp(String packageName, String className) {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 5)) {
            try {
                startActivity(new Intent().setClassName(packageName, className));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, R.string.toast_no_course, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // DchaState 変更
    private void setSetupStatus(String packageName) {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 1)) {
            new DchaServiceUtil(this).setSetupStatus(3, object -> {
                if (!object.equals(true)) {
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                }
                hideNavigationBar(packageName);
            });
        } else {
            hideNavigationBar(packageName);
        }
    }

    // ナビバー表示設定
    private void hideNavigationBar(String packageName) {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 2)) {
            new DchaServiceUtil(this).hideNavigationBar(true, object -> {
                if (!object.equals(true)) {
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                }
                setDisplaySize(packageName);
            });
        } else {
            setDisplaySize(packageName);
        }
    }

    // 解像度修正
    private void setDisplaySize(String packageName) {
        if (Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.DEF_INT) == Constants.MODEL_CTX ||
                Preferences.load(this, Constants.KEY_INT_MODEL_NUMBER, Constants.DEF_INT) == Constants.MODEL_CTZ) {
            // CTXとCTZ
            if (Common.isBenesseExtensionExist()) {
                //noinspection ResultOfMethodCallIgnored
                BenesseExtension.putInt(Constants.BC_COMPATSCREEN, 0);
            } else {
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
            }
            setHomeApp(packageName);
        } else {
            // CT2とCT3
            new DchaUtilServiceUtil(this).setForcedDisplaySize(1280, 800, object -> {
                if (!object.equals(true)) {
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                }
                setHomeApp(packageName);
            });
        }
    }

    // ホームを変更
    private void setHomeApp(String packageName) {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 3)) {
            ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

            if (resolveInfo == null) {
                Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                removeTask();
                return;
            }
            new DchaServiceUtil(this).setPreferredHomeApp(resolveInfo.activityInfo.packageName, packageName, object -> {
                if (!object.equals(true)) {
                    // 失敗
                    Toast.makeText(this, R.string.toast_no_home, Toast.LENGTH_SHORT).show();
                }
                removeTask();
            });
        } else {
            removeTask();
        }
    }

    // タスクの消去
    private void removeTask() {
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 4)) {
            new DchaServiceUtil(this).removeTask(null, object -> {
                if (!object.equals(true)) {
                    Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                finishAndRemoveTask();
            });
        } else {
            Toast.makeText(this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finishAndRemoveTask();
    }
}
