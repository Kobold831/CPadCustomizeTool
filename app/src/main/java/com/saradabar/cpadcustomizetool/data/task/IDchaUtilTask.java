package com.saradabar.cpadcustomizetool.data.task;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.saradabar.cpadcustomizetool.util.Constants;

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class IDchaUtilTask {

    public void execute(Context context, Listener listener) {
        doInBackground(context, listener);
    }

    protected void doInBackground(Context context, Listener listener) {
        if (!tryBindDchaUtilService(context, listener)) {
            listener.onFailure();
        }
    }

    public interface Listener {
        void onSuccess(IDchaUtilService iDchaUtilService);

        void onFailure();
    }

    public boolean tryBindDchaUtilService(Context context, Listener listener) {
        return context.bindService(Constants.DCHA_UTIL_SERVICE, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                IDchaUtilService iDchaUtilService = IDchaUtilService.Stub.asInterface(iBinder);
                listener.onSuccess(iDchaUtilService);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
