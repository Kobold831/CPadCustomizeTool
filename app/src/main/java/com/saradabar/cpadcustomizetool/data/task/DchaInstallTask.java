package com.saradabar.cpadcustomizetool.data.task;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class DchaInstallTask {

    IDchaService mDchaService;

    public void execute(Context context, Listener listener, String installData) {
        onPreExecute(listener);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                Common.tryBindDchaService(context, mDchaService, null, mDchaServiceConnection, true, Constants.FLAG_CHECK, 0, 0, "", "");

                Runnable runnable = () -> {
                    Boolean result = doInBackground(context, installData);
                    handler.post(() -> onPostExecute(listener, result));
                };

                new Handler(Looper.getMainLooper()).postDelayed(runnable, 1000);
            }).start();
        });
    }

    void onPreExecute(Listener listener) {
        listener.onShow();
    }

    void onPostExecute(Listener listener, Boolean result) {
        if (result) {
            listener.onSuccess();
        } else {
            listener.onFailure();
        }
    }

    protected Boolean doInBackground(Context context, String installData) {
        return Common.tryBindDchaService(context, mDchaService, null, mDchaServiceConnection, true, Constants.FLAG_INSTALL_PACKAGE,0, 0, installData, "");
    }

    public interface Listener {

        void onShow();

        void onSuccess();

        void onFailure();
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
}
