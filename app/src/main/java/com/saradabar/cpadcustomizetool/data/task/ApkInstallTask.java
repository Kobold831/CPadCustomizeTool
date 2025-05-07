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

    IDhizukuService mDhizukuService;
    DhizukuUserServiceArgs dhizukuUserServiceArgs;
    ServiceConnection dServiceConnection;

    public void execute(Context context, Listener listener, ArrayList<String> splitInstallData, int reqCode, InstallEventListener installEventListener) {
        onPreExecute(listener);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() ->
                new Thread(() ->
                        doInBackground(context, listener, installEventListener, splitInstallData, reqCode)).start());
    }

    private void onPreExecute(@NonNull Listener listener) {
        listener.onShow();
    }

    private void onPostExecute(Context context, Listener listener, Object result, InstallEventListener installEventListener) {
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

    private void doInBackground(Context context, Listener listener, InstallEventListener installEventListener, ArrayList<String> splitInstallData, int reqCode) {
        if (isDhizukuActive(context)) {
            // Dhizukuでセッションインストール
            doDhizukuInstall(context, listener, installEventListener, splitInstallData, reqCode);
        } else {
            // このアプリでセッションインストール
            doSessionInstall(context, listener, installEventListener, splitInstallData, reqCode);
        }
    }

    private void doDhizukuInstall(Context context, Listener listener, InstallEventListener installEventListener, ArrayList<String> splitInstallData, int reqCode) {
        try {
            dhizukuUserServiceArgs = new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class));
            dServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder iBinder) {
                    mDhizukuService = IDhizukuService.Stub.asInterface(iBinder);

                    if (mDhizukuService == null) {
                        // 失敗
                        doListener().onPost(context, listener, false, installEventListener);
                        return;
                    }

                    try {
                        if (mDhizukuService.tryInstallPackages(splitInstallData, reqCode)) {
                            if (dhizukuUserServiceArgs != null) {
                                try {
                                    Dhizuku.stopUserService(dhizukuUserServiceArgs);
                                } catch (IllegalStateException ignored) {
                                }
                            }

                            if (dServiceConnection != null) {
                                try {
                                    Dhizuku.unbindUserService(dServiceConnection);
                                } catch (IllegalStateException ignored) {
                                }
                            }
                            // 成功
                            doListener().onPost(context, listener, true, installEventListener);
                        } else {
                            // 失敗
                            doListener().onPost(context, listener, false, installEventListener);
                        }
                    } catch (Exception e) {
                        // 例外処理
                        doListener().onPost(context, listener, e.getMessage(), installEventListener);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };

            // サービスに接続
            if (!Dhizuku.bindUserService(dhizukuUserServiceArgs, dServiceConnection)) {
                // 失敗
                doListener().onPost(context, listener, false, installEventListener);
            }
        } catch (Exception e) {
            // 例外処理
            doListener().onPost(context, listener, e.getMessage(), installEventListener);
        }
    }

    private void doSessionInstall(Context context, Listener listener, InstallEventListener installEventListener, ArrayList<String> splitInstallData, int reqCode) {
        SessionInstaller sessionInstaller = new SessionInstaller();
        int sessionId;

        try {
            sessionId = sessionInstaller.splitCreateSession(context);

            if (sessionId < 0) {
                // 失敗
                doListener().onPost(context, listener, false, installEventListener);
                return;
            }
        } catch (Exception e) {
            // 例外処理
            doListener().onPost(context, listener, e.getMessage(), installEventListener);
            return;
        }

        /* インストールデータの長さ回数繰り返す */
        for (String str : splitInstallData) {
            /* 配列の中身を確認 */
            if (str != null) {
                try {
                    if (!sessionInstaller.splitWriteSession(context, new File(str), sessionId)) {
                        // 失敗
                        doListener().onPost(context, listener, false, installEventListener);
                        return;
                    }
                } catch (Exception e) {
                    // 例外処理
                    doListener().onPost(context, listener, e.getMessage(), installEventListener);
                    return;
                }
            } else {
                /* つぎの配列の中身が空ならループ終了 */
                break;
            }
        }

        try {
            if (!sessionInstaller.splitCommitSession(context, sessionId, reqCode)) {
                // 失敗
                doListener().onPost(context, listener, false, installEventListener);
            }
        } catch (Exception e) {
            // 例外処理
            doListener().onPost(context, listener, e.getMessage(), installEventListener);
        }
    }

    public interface Listener {
        void onShow();

        void onSuccess();

        void onFailure(String message);

        void onError(String message);
    }

    private doListener doListener() {
        return (context, listener, result, installEventListener) -> {
            Handler handler = new Handler(Looper.getMainLooper());
            new Thread(() ->
                    handler.post(() ->
                            onPostExecute(context, listener, result, installEventListener))).start();
        };
    }

    private interface doListener {
        void onPost(Context context, Listener listener, Object result, InstallEventListener installEventListener);
    }
}
