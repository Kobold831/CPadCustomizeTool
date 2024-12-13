package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.saradabar.cpadcustomizetool.util.DchaUtilServiceUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class ResolutionTask {

    final Object objLock = new Object();

    IDchaUtilService mDchaUtilService;

    public void execute(Context context, Listener listener, int i, int i1) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                Boolean result = doInBackground(context, i, i1);
                handler.post(() -> onPostExecute(listener, result));
            }).start();
        });
    }

    void onPostExecute(Listener listener, Boolean result) {
        new Handler().postDelayed(() -> {
            if (result) {
                listener.onSuccess();
            } else {
                listener.onFailure();
            }
        }, 1000);
    }

    protected Boolean doInBackground(Context context, int i, int i1) {
        try {
            new IDchaUtilTask().execute(context, iDchaUtilTaskListener());
            synchronized (objLock) {
                objLock.wait();
            }
            if (mDchaUtilService == null) return false;
            return new DchaUtilServiceUtil(mDchaUtilService).setForcedDisplaySize(i, i1);
        } catch (Exception ignored) {
            return false;
        }
    }

    public interface Listener {
        void onSuccess();

        void onFailure();
    }

    private IDchaUtilTask.Listener iDchaUtilTaskListener() {
        return new IDchaUtilTask.Listener() {
            @Override
            public void onSuccess(IDchaUtilService iDchaUtilService) {
                mDchaUtilService = iDchaUtilService;
                synchronized (objLock) {
                    objLock.notify();
                }
            }

            @Override
            public void onFailure() {
                synchronized (objLock) {
                    objLock.notify();
                }
            }
        };
    }
}
