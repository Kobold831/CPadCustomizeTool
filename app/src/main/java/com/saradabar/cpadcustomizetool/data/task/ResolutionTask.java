package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.Display;
import android.view.IWindowManager;

import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaUtilServiceUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResolutionTask {

    public void execute(Context context, Listener listener, int i, int i1) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() ->
                new Thread(() ->
                        doInBackground(context, listener, i, i1)).start());
    }

    private void onPostExecute(Listener listener, Object result) {
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

    private void doInBackground(Context context, Listener listener, int width, int height) {
        if (Common.isCTX() || Common.isCTZ()) {
            if (width == 1024 && height == 768 || width == 1280 && height == 800 || width == 1920 && height == 1200) {
                doListener().onPost(listener, BenesseExtension.putInt(Constants.BC_COMPATSCREEN,
                        width == 1024 ? 1 : width == 1280 ? 2 : 0 // 1920x1200
                ));
            } else {
                try {
                    IWindowManager.Stub.asInterface(ServiceManager.getService("window")).setForcedDisplaySize(Display.DEFAULT_DISPLAY, width, height);
                    doListener().onPost(listener, true);
                } catch (RemoteException e) {
                    doListener().onPost(listener, e.getMessage());
                }
            }
        } else {
            new DchaUtilServiceUtil(context).setForcedDisplaySize(width, height, object ->
                    doListener().onPost(listener, object));
        }
    }

    public interface Listener {
        void onSuccess();

        void onFailure();

        void onError(String message);
    }

    private doListener doListener() {
        return (listener, result) -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() ->
                    handler.post(() ->
                            onPostExecute(listener, result))).start();
        };
    }

    private interface doListener {
        void onPost(Listener listener, Object result);
    }
}
