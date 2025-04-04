package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.installer.SessionInstaller;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApkInstallTask {

    public void execute(Context context, Listener listener, ArrayList<String> splitInstallData, int reqCode, InstallEventListener installEventListener) {
        onPreExecute(listener);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                Object result = doInBackground(context, splitInstallData, reqCode);
                handler.post(() -> onPostExecute(context, listener, result, installEventListener));
            }).start();
        });
    }

    void onPreExecute(@NonNull Listener listener) {
        listener.onShow();
    }

    void onPostExecute(Context context, Listener listener, Object result, InstallEventListener installEventListener) {
        if (result == null) {
            listener.onError(context.getString(R.string.installer_status_unknown_error));
            return;
        }

        if (result.equals(true)) {
            // InstallServiceで成功イベントを発生
            MyApplication myApplication = (MyApplication) context.getApplicationContext();
            myApplication.installEventListener = installEventListener;
            return;
        }

        if (result.equals(false)) {
            listener.onFailure("");
            return;
        }

        listener.onError(result.toString());
    }

    protected Object doInBackground(Context context, ArrayList<String> splitInstallData, int reqCode) {
        SessionInstaller sessionInstaller = new SessionInstaller();
        int sessionId;

        try {
            sessionId = sessionInstaller.splitCreateSession(context);

            if (sessionId < 0) {
                return false;
            }
        } catch (Exception e) {
            return e.getMessage();
        }

        /* インストールデータの長さ回数繰り返す */
        for (String str : splitInstallData) {
            /* 配列の中身を確認 */
            if (str != null) {
                try {
                    if (!sessionInstaller.splitWriteSession(context, new File(str), sessionId)) {
                        return false;
                    }
                } catch (Exception e) {
                    return e.getMessage();
                }
            } else {
                /* つぎの配列がnullなら終了 */
                break;
            }
        }

        try {
            return sessionInstaller.splitCommitSession(context, sessionId, reqCode);
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public interface Listener {
        void onShow();

        void onSuccess();

        void onFailure(String message);

        void onError(String message);
    }

}
