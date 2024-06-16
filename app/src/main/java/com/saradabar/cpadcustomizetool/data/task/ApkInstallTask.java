package com.saradabar.cpadcustomizetool.data.task;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;
import static com.saradabar.cpadcustomizetool.util.Common.tryBindDhizukuService;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.installer.SessionInstaller;
import com.saradabar.cpadcustomizetool.util.Constants;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApkInstallTask {

    public static ApkInstallTask apkInstallTask;

    public void execute(Context context, Listener listener, String[] splitInstallData) {
        onPreExecute(listener);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() -> {
                Object result = doInBackground(context, splitInstallData);
                handler.post(() -> onPostExecute(context, listener, result));
            }).start();
        });
    }

    void onPreExecute(Listener listener) {
        apkInstallTask = this;
        listener.onShow();
    }

    void onPostExecute(Context context, Listener listener, Object result) {
        if (result == null) {
            listener.onError(context.getString(R.string.installer_status_unknown_error));
            return;
        }

        if (result.equals(true)) {
            return;
        }

        if (result.equals(false)) {
            listener.onFailure("");
            return;
        }

        listener.onError(result.toString());
    }

    protected Object doInBackground(Context context, String[] splitInstallData) {
        if (isDhizukuActive(context)) {
            if (tryBindDhizukuService(context)) {
                try {
                    return ((MyApplication) context.getApplicationContext()).mDhizukuService.tryInstallPackages(splitInstallData, Constants.REQUEST_INSTALL_SILENT);
                } catch (RemoteException ignored) {
                }
            }

            return false;
        } else {
            SessionInstaller sessionInstaller = new SessionInstaller();
            int sessionId;

            try {
                sessionId = sessionInstaller.splitCreateSession(context).i;

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
                        if (!sessionInstaller.splitWriteSession(context, new File(str), sessionId).bl) {
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
                return sessionInstaller.splitCommitSession(context, sessionId, 0).bl;
            } catch (Exception e) {
                return e.getMessage();
            }
        }
    }

    public interface Listener {

        void onShow();

        void onSuccess();

        void onFailure(String message);

        void onError(String message);
    }
}
