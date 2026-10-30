package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;

public class DchaInstallTask {

    public void execute(Context context, Listener listener, String installData) {
        onPreExecute(listener);
        AppExecutors.io(() -> doInBackground(context, listener, installData));
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
        new DchaServiceUtil(context).installApp(installData, object ->
                onPostExecute(listener, Boolean.TRUE.equals(object)));
    }

    public interface Listener {
        void onShow();

        void onSuccess();

        void onFailure();
    }
}
