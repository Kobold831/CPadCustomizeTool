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

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

public class RebootActivity extends AppCompatActivity {

    @Override
    public final void onCreate(Bundle savedInstanceState) {
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
        reboot();
    }

    private void reboot() {
        new DialogUtil(this)
                .setMessage(R.string.dialog_question_reboot)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) ->
                        new DchaServiceUtil(this).rebootPad(0, null, object -> {
                    if (!object.equals(true)) {
                        Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                    }
                    finishAndRemoveTask();
                }))
                .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> finishAndRemoveTask())
                .setOnDismissListener(dialogInterface -> finishAndRemoveTask())
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        finishAndRemoveTask();
    }
}
