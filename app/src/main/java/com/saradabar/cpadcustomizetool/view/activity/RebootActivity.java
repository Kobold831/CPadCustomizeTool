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

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class RebootActivity extends AppCompatActivity {

    IDchaService mDchaService;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        if (Preferences.load(this, Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            startReboot();
        } else {
            Toast.toast(this, R.string.toast_use_not_dcha);
            finishAndRemoveTask();
        }
    }

    private void startReboot() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.dialog_question_reboot)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
                    Runnable runnable = () -> {
                        try {
                            mDchaService.rebootPad(0, null);
                        } catch (RemoteException ignored) {
                        }
                    };
                    new Handler().postDelayed(runnable, 10);
                })
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> finishAndRemoveTask())
                .setOnDismissListener(dialogInterface -> finishAndRemoveTask())
                .show();
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
