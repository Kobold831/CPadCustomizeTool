package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DchaInstallTask {

    public void execute(Context context, Listener listener, String installData) {
        onPreExecute(listener);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() ->
                new Thread(() ->
                        doInBackground(context, listener, installData)).start());
    }

    private void onPreExecute(@NonNull Listener listener) {
        listener.onShow();
    }

    private void onPostExecute(Listener listener, boolean result) {
        if (result) {
            listener.onSuccess();
        } else {
            listener.onFailure();
        }
    }

    private void doInBackground(Context context, Listener listener, String installData) {
        new DchaServiceUtil(context).installApp(installData, 2, object ->
                doListener().onPost(listener, object.equals(true)));
    }

    public interface Listener {
        void onShow();

        void onSuccess();

        void onFailure();
    }

    @NonNull
    private doListener doListener() {
        return (listener, result) -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() ->
                    handler.post(() ->
                            onPostExecute(listener, result))).start();
        };
    }

    private interface doListener {
        void onPost(Listener listener, boolean result);
    }
}
