package com.saradabar.cpadcustomizetool.data.task;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuAllActive;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageInstaller;
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
import com.saradabar.cpadcustomizetool.util.Constants;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApkInstallTask {

    IDhizukuService mDhizukuService;
    DhizukuUserServiceArgs dhizukuUserServiceArgs;
    ServiceConnection dServiceConnection;

    public void execute(Context context, Listener listener, ArrayList<String> splitInstallData, int reqCode, InstallEventListener installEventListener) {
        onPreExecute(listener);
        AppExecutors.io(() ->
                doInBackground(context, listener, installEventListener, splitInstallData, reqCode));
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
            myApplication.setInstallEventListener(installEventListener);
            return;
        }

        if (result.equals(false)) {
            listener.onFailure("");
            return;
        }
        listener.onError(result.toString());
    }

    private void doInBackground(Context context, Listener listener, InstallEventListener installEventListener, ArrayList<String> splitInstallData, int reqCode) {
        if (isDhizukuAllActive(context)) {
            // Dhizukuでセッションインストール
            doDhizukuInstall(context, listener, installEventListener, splitInstallData, reqCode);
        } else {
            // このアプリでセッションインストール
            doSessionInstall(context, listener, installEventListener, splitInstallData, reqCode);
        }
    }

    private void doDhizukuInstall(Context context, Listener listener, InstallEventListener installEventListener, ArrayList<String> splitInstallData, int reqCode) {
        AtomicBoolean connected = new AtomicBoolean(false);
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutTask = () -> {
            if (connected.compareAndSet(false, true)) {
                doListener().onPost(context, listener, context.getString(R.string.installer_status_dhizuku_timeout), installEventListener);
            }
        };
        try {
            dhizukuUserServiceArgs = new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class));
            timeoutHandler.postDelayed(timeoutTask, Constants.DHIZUKU_SERVICE_TIMEOUT);
            dServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder iBinder) {
                    timeoutHandler.removeCallbacks(timeoutTask);
                    if (!connected.compareAndSet(false, true)) {
                        return;
                    }
                    new Thread(() -> {
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
                    }).start();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };

            // サービスに接続
            if (!Dhizuku.bindUserService(dhizukuUserServiceArgs, dServiceConnection)) {
                timeoutHandler.removeCallbacks(timeoutTask);
                if (connected.compareAndSet(false, true)) {
                    // 失敗
                    doListener().onPost(context, listener, false, installEventListener);
                }
            }
        } catch (Exception e) {
            timeoutHandler.removeCallbacks(timeoutTask);
            if (connected.compareAndSet(false, true)) {
                // 例外処理
                doListener().onPost(context, listener, e.getMessage(), installEventListener);
            }
        }
    }

    private void doSessionInstall(@NonNull Context context, Listener listener, InstallEventListener installEventListener, ArrayList<String> splitInstallData, int reqCode) {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        int sessionId;

        try {
            sessionId = SessionInstaller.createSession(packageInstaller);

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

        /* インストールデータの書き込み */
        Object writeResult = SessionInstaller.writeSessions(packageInstaller, sessionId, splitInstallData);

        if (!Boolean.TRUE.equals(writeResult)) {
            // 失敗
            doListener().onPost(context, listener, writeResult, installEventListener);
            return;
        }

        if (installEventListener != null) {
            ((MyApplication) context.getApplicationContext()).setInstallEventListener(installEventListener);
        }

        try {
            if (!SessionInstaller.commitSession(packageInstaller, sessionId, context,
                    SessionInstaller.installServiceIntent(context, sessionId, reqCode))) {
                // 失敗
                doListener().onPost(context, listener, false, installEventListener);
                return;
            }
                doListener().onPost(context, listener, true, installEventListener);
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

    @NonNull
    private doListener doListener() {
        return (context, listener, result, installEventListener) -> {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() ->
                    onPostExecute(context, listener, result, installEventListener));
        };
    }

    private interface doListener {
        void onPost(Context context, Listener listener, Object result, InstallEventListener installEventListener);
    }
}
