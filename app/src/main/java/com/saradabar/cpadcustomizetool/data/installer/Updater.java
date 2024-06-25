/*
 * CPad Customize Tool
 * Copyright © 2021-2024 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.data.installer;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.service.DhizukuService;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import java.io.File;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class Updater implements InstallEventListener {

    AlertDialog progressDialog;
    IDchaService mDchaService;
    Activity activity;
    String DOWNLOAD_FILE_URL;

    @SuppressLint("StaticFieldLeak")
    static Updater instance = null;

    public static Updater getInstance() {
        return instance;
    }

    public Updater(Activity act, String url, AlertDialog progressDialog) {
        instance = this;
        activity = act;
        this.progressDialog = progressDialog;
        DOWNLOAD_FILE_URL = url;
    }

    @Override
    public void onInstallSuccess(int reqCode) {

    }

    /* 失敗 */
    @Override
    public void onInstallFailure(int reqCode, String str) {
        new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.dialog_info_failure_silent_install) + "\n" + str)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                .show();
    }

    @Override
    public void onInstallError(int reqCode, String str) {
        new AlertDialog.Builder(activity)
                .setMessage(activity.getString(R.string.dialog_error) + "\n" + str)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                .show();
    }

    public void installApk(Context context, int flag) {
        View view = activity.getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        TextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText("");

        switch (Preferences.load(activity, Constants.KEY_FLAG_UPDATE_MODE, 1)) {
            case 0:
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }

                activity.startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(context.getExternalCacheDir(), "update.apk").getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                break;
            case 1:
                switch (flag) {
                    case 0:
                        new AlertDialog.Builder(activity)
                                .setCancelable(false)
                                .setTitle(R.string.dialog_title_update)
                                .setMessage(R.string.dialog_info_update_caution)
                                .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                    try {
                                        activity.startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_UPDATE)), Constants.REQUEST_ACTIVITY_UPDATE);
                                    } catch (Exception ignored) {
                                        Toast.makeText(activity, R.string.toast_unknown_activity, Toast.LENGTH_SHORT).show();
                                        activity.finish();
                                    }
                                })
                                .setNegativeButton("キャンセル", null)
                                .show();
                        break;
                    case 1:
                        if (progressDialog.isShowing()) {
                            progressDialog.cancel();
                        }

                        new AlertDialog.Builder(activity)
                                .setCancelable(false)
                                .setTitle("インストール")
                                .setMessage("遷移先のページよりapkファイルをダウンロードしてadbでインストールしてください")
                                .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                    try {
                                        activity.startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_FILE_URL)), Constants.REQUEST_ACTIVITY_UPDATE);
                                    } catch (Exception ignored) {
                                        Toast.makeText(activity, R.string.toast_unknown_activity, Toast.LENGTH_SHORT).show();
                                        activity.finish();
                                    }
                                })
                                .setNegativeButton("キャンセル", null)
                                .show();
                        break;
                }
                break;
            case 2:
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }

                textView.setText(activity.getString(R.string.progress_state_installing));
                progressDialog = new AlertDialog.Builder(activity).setCancelable(false).setView(view).create();
                progressDialog.show();

                if (tryBindDchaService()) {
                    new Handler().postDelayed(() -> {
                        if (!tryInstallPackage()) {
                            if (progressDialog.isShowing()) {
                                progressDialog.cancel();
                            }

                            new AlertDialog.Builder(activity)
                                    .setCancelable(false)
                                    .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                    .show();
                        } else {
                            if (progressDialog.isShowing()) {
                                progressDialog.cancel();
                            }
                        }
                    }, 10);
                } else {
                    if (progressDialog.isShowing()) {
                        progressDialog.cancel();
                    }

                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                            .show();
                }
                break;
            case 3:
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }

                textView.setText(activity.getString(R.string.progress_state_installing));
                progressDialog = new AlertDialog.Builder(activity).setCancelable(false).setView(view).create();
                progressDialog.show();

                if (((DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(activity.getPackageName())) {
                    if (!trySessionInstall(flag)) {
                        if (progressDialog.isShowing()) {
                            progressDialog.cancel();
                        }

                        new AlertDialog.Builder(activity)
                                .setCancelable(false)
                                .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                .show();
                    } else {
                        if (progressDialog.isShowing()) {
                            progressDialog.cancel();
                        }
                    }
                } else {
                    if (Preferences.load(activity, Constants.KEY_MODEL_NAME, 0) == Constants.MODEL_CTX || Preferences.load(activity, Constants.KEY_MODEL_NAME, 0) == Constants.MODEL_CTZ) {
                        Preferences.save(activity, Constants.KEY_FLAG_UPDATE_MODE, 1);
                    } else Preferences.save(activity, Constants.KEY_FLAG_UPDATE_MODE, 0);
                    if (progressDialog.isShowing()) {
                        progressDialog.cancel();
                    }

                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(activity.getString(R.string.dialog_error_reset_update_mode))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                            .show();
                }
                break;
            case 4:
                if (progressDialog.isShowing()) {
                    progressDialog.cancel();
                }

                textView.setText(activity.getString(R.string.progress_state_installing));
                progressDialog = new AlertDialog.Builder(activity).setCancelable(false).setView(view).create();
                progressDialog.show();

                if (isDhizukuActive(activity)) {
                    if (tryBindDhizukuService()) {
                        new Handler().postDelayed(() -> {
                            try {
                                String[] installData = new String[1];
                                installData[0] = new File(activity.getExternalCacheDir(), "update.apk").getPath();
                                int reqCode;

                                if (flag == 0) {
                                    reqCode = Constants.REQUEST_INSTALL_SELF_UPDATE;
                                } else {
                                    reqCode = Constants.REQUEST_INSTALL_GET_APP;
                                }

                                if (!((MyApplication) context.getApplicationContext()).mDhizukuService.tryInstallPackages(installData, reqCode)) {
                                    if (progressDialog.isShowing()) {
                                        progressDialog.cancel();
                                    }

                                    new AlertDialog.Builder(activity)
                                            .setCancelable(false)
                                            .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                            .show();
                                }
                            } catch (RemoteException ignored) {
                            }
                        }, 5000);
                        return;
                    } else {
                        if (progressDialog.isShowing()) {
                            progressDialog.cancel();
                        }

                        new AlertDialog.Builder(activity)
                                .setCancelable(false)
                                .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                                .show();
                    }
                } else {
                    if (progressDialog.isShowing()) {
                        progressDialog.cancel();
                    }

                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setMessage(context.getResources().getString(R.string.dialog_error) + "\n繰り返し発生する場合は”アプリ設定→アップデートモードを選択”が有効なモードに設定されているかをご確認ください")
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> activity.finish())
                            .show();
                }
                break;
        }
    }

    ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaService = IDchaService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    public boolean tryBindDchaService() {
        return activity.bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean tryInstallPackage() {
        if (mDchaService != null) {
            try {
                return mDchaService.installApp(new File(activity.getExternalCacheDir(), "update.apk").getPath(), 1);
            } catch (RemoteException ignored) {
            }
        }
        return false;
    }

    private boolean trySessionInstall(int reqCode) {
        SessionInstaller sessionInstaller = new SessionInstaller();
        int sessionId;

        try {
            sessionId = sessionInstaller.splitCreateSession(activity).i;
            if (sessionId < 0) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }

        try {
            if (!sessionInstaller.splitWriteSession(activity, new File(activity.getExternalCacheDir(), "update.apk"), sessionId).bl) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }

        try {
            if (reqCode == 0) {
                return sessionInstaller.splitCommitSession(activity, sessionId, Constants.REQUEST_INSTALL_SELF_UPDATE).bl;
            } else {
                return sessionInstaller.splitCommitSession(activity, sessionId, Constants.REQUEST_INSTALL_GET_APP).bl;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean tryBindDhizukuService() {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(activity, DhizukuService.class));
        return Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                ((MyApplication) activity.getApplicationContext()).mDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        });
    }
}
