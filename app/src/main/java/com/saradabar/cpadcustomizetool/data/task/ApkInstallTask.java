package com.saradabar.cpadcustomizetool.data.task;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.installer.SessionInstaller;
import com.saradabar.cpadcustomizetool.data.service.DhizukuService;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApkInstallTask {

    final Object objLock = new Object();

    IDhizukuService mDhizukuService;

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
        if (isDhizukuActive(context)) {
            if (tryBindDhizukuService(context)) {
                try {
                    new IDhizukuTask().execute(context, iDhizukuTaskListener());
                    synchronized (objLock) {
                        objLock.wait();
                    }
                    if (mDhizukuService == null) return false;
                    return mDhizukuService.tryInstallPackages(splitInstallData, reqCode);
                } catch (Exception e) {
                    return e.getMessage();
                }
            }
            return false;
        } else {
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
    }

    public interface Listener {
        void onShow();

        void onSuccess();

        void onFailure(String message);

        void onError(String message);
    }

    private boolean tryBindDhizukuService(Context context) {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class));
        return Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        });
    }

    @NonNull
    private IDhizukuTask.Listener iDhizukuTaskListener() {
        return new IDhizukuTask.Listener() {
            @Override
            public void onSuccess(IDhizukuService iDhizukuService) {
                mDhizukuService = iDhizukuService;
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
