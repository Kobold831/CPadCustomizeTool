package com.saradabar.cpadcustomizetool.data.task;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import com.saradabar.cpadcustomizetool.data.service.DhizukuService;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IDhizukuTask {

    IDhizukuService iDhizukuService;

    public void execute(Context context, Listener listener) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> handler.post(() -> doInBackground(context, listener))).start();
        });
    }

    protected void doInBackground(Context context, Listener listener) {
        if (!tryBindDhizukuService(context, listener)) {
            listener.onFailure();
        }
    }

    public interface Listener {
        void onSuccess(IDhizukuService iDhizukuService);

        void onFailure();
    }

    public boolean tryBindDhizukuService(Context context, Listener listener) {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class));
        return Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                iDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
                listener.onSuccess(iDhizukuService);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        });
    }
}
