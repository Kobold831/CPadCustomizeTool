package com.saradabar.cpadcustomizetool.data.task;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class IDchaTask {

    public void execute(Context context, Listener listener) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() ->
                    handler.post(() ->
                            doInBackground(context, listener))).start();
        });
    }

    protected void doInBackground(Context context, Listener listener) {
        if (!tryBindDchaService(context, listener)) {
            listener.onDo(null);
        }
    }

    public interface Listener {
        void onDo(IDchaService iDchaService);
    }

    public boolean tryBindDchaService(@NonNull Context context, Listener listener) {
        return context.bindService(Constants.ACTION_DCHA_SERVICE, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                IDchaService iDchaService = IDchaService.Stub.asInterface(iBinder);
                listener.onDo(iDchaService);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
