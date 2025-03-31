package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.ServiceManager;
import android.view.Display;
import android.view.IWindowManager;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaUtilServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;

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
                Object result = doInBackground(context, i, i1);
                handler.post(() -> onPostExecute(listener, result));
            }).start();
        });
    }

    void onPostExecute(Listener listener, Object result) {
        new Handler().postDelayed(() -> {
            if (result.equals(true)) {
                listener.onSuccess();
                return;
            }

            if (result.equals(false)) {
                listener.onFailure();
                return;
            }
            listener.onError(result.toString());
        }, 1500);
    }

    protected Object doInBackground(Context context, int width, int height) {
        try {
            if (Preferences.load(context, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                    Preferences.load(context, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                if (width == 1024 && height == 768 || width == 1280 && height == 800 || width == 1920 && height == 1200) {
                    //noinspection ResultOfMethodCallIgnored
                    BenesseExtension.putInt(Constants.BC_COMPATSCREEN,
                            width == 1024 ? 1 : width == 1280 ? 2 : 0 // 1920x1200
                    );
                    return true;
                }
                IWindowManager.Stub.asInterface(ServiceManager.getService("window")).setForcedDisplaySize(Display.DEFAULT_DISPLAY, width, height);
                return true;
            } else {
                new IDchaUtilTask().execute(context, iDchaUtilTaskListener());
                synchronized (objLock) {
                    objLock.wait();
                }

                if (mDchaUtilService == null) {
                    return false;
                }
                return new DchaUtilServiceUtil(mDchaUtilService).setForcedDisplaySize(i, i1);
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    public interface Listener {
        void onSuccess();

        void onFailure();

        void onError(String message);
    }

    @NonNull
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
