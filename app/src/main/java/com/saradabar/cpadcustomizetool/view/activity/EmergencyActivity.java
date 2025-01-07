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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class EmergencyActivity extends Activity {

    IDchaService mDchaService;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初期設定が完了していない場合は終了
        if (!Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, false)) {
            Toast.makeText(this, R.string.toast_not_completed_settings, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
            return;
        }

        if (!Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
            Toast.makeText(this, R.string.toast_use_not_dcha, Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
            return;
        }

        if (!bindService(Constants.DCHA_SERVICE, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mDchaService = IDchaService.Stub.asInterface(iBinder);

                if (mDchaService == null) {
                    Toast.makeText(EmergencyActivity.this, "エラーが発生しました", Toast.LENGTH_SHORT).show();
                    finishAndRemoveTask();
                }

                // メイン処理
                switch (PreferenceManager.getDefaultSharedPreferences(EmergencyActivity.this).getString("emergency_mode", "")) {
                    case "1":
                        if (run("jp.co.benesse.touch.allgrade.b003.touchhomelauncher", "jp.co.benesse.touch.allgrade.b003.touchhomelauncher.HomeLauncherActivity")) {
                            // 成功
                            Toast.makeText(EmergencyActivity.this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                        }
                        finishAndRemoveTask();
                        break;
                    case "2":
                        if (run("jp.co.benesse.touch.home", "jp.co.benesse.touch.home.LoadingActivity")) {
                            // 成功
                            Toast.makeText(EmergencyActivity.this, R.string.toast_execution, Toast.LENGTH_SHORT).show();
                        }
                        finishAndRemoveTask();
                        break;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE)) {
            Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_SHORT).show();
            finishAndRemoveTask();
        }
    }

    private boolean run(String packageName, String className) {
        // 維持サービスの無効化
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

        // 勉強ホームを起動
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 5)) {
            try {
                startActivity(new Intent().setClassName(packageName, className));
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.toast_not_course, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // 設定変更
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 1)) {
            new DchaServiceUtil(this, mDchaService).setSetupStatus(3);
        }

        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 2)) {
            new DchaServiceUtil(this, mDchaService).hideNavigationBar(true);
        }

        // ホームを変更
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 3)) {
            if (resolveInfo != null) {
                if (!new DchaServiceUtil(this, mDchaService).setPreferredHomeApp(resolveInfo.activityInfo.packageName, packageName)) {
                    // 失敗
                    Toast.makeText(this, R.string.toast_not_install_launcher, Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // タスクの消去
        if (Preferences.loadMultiList(this, Constants.KEY_EMERGENCY_SETTINGS, 4)) {
            try {
                mDchaService.removeTask(null);
            } catch (Exception ignored) {
                Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_SHORT).show();
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
