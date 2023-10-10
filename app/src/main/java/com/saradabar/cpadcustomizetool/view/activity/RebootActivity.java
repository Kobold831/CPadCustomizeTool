package com.saradabar.cpadcustomizetool.view.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class RebootActivity extends Activity {

    IDchaService mDchaService;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        if (Preferences.GET_DCHASERVICE_FLAG(this)) {
            startReboot();
        } else {
            Toast.toast(this, R.string.toast_use_not_dcha);
            finishAndRemoveTask();
        }
    }

    private void startReboot() {
        new AlertDialog.Builder(this)
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
            mDchaService = null;
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        finishAndRemoveTask();
    }
}