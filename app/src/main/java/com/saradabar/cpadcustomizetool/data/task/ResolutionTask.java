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

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class ResolutionTask {

    IDchaUtilService mDchaUtilService;

    public void execute(Context context, Listener listener, int i, int i1) {
        onPreExecute();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                Boolean result = doInBackground(context, i, i1);
                handler.post(() -> onPostExecute(listener, result));
            }).start();
        });
    }

    void onPreExecute() {
    }

    void onPostExecute(Listener listener, Boolean result) {
        Runnable runnable = () -> {
            if (result) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        };

        new Handler().postDelayed(runnable, 1000);
    }

    protected Boolean doInBackground(Context context, int i, int i1) {
        return Common.tryBindDchaService(context, null, mDchaUtilService, mDchaUtilServiceConnection, false, Constants.FLAG_RESOLUTION,i, i1, "", "");
    }

    public interface Listener {

        void onSuccess();

        void onFailure();
    }

    ServiceConnection mDchaUtilServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaUtilService = IDchaUtilService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
}
