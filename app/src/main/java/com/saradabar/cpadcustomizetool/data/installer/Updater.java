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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.saradabar.cpadcustomizetool.MyApplication;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.service.DhizukuService;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import java.io.File;

public class Updater implements InstallEventListener {

    Activity activity;
    String DOWNLOAD_FILE_URL;

    @SuppressLint("StaticFieldLeak")
    static Updater instance = null;

    public static Updater getInstance() {
        return instance;
    }

    public Updater(Activity act, String url) {
        instance = this;
        activity = act;
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

    public int ownerInstallApk(int flag) {
        if (((DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(activity.getPackageName())) {
            if (trySessionInstall(flag)) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (Preferences.load(activity, Constants.KEY_MODEL_NAME, 0) == Constants.MODEL_CTX || Preferences.load(activity, Constants.KEY_MODEL_NAME, 0) == Constants.MODEL_CTZ) {
                Preferences.save(activity, Constants.KEY_FLAG_UPDATE_MODE, 1);
            } else {
                Preferences.save(activity, Constants.KEY_FLAG_UPDATE_MODE, 0);
            }
            return 2;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean dhizukuInstallApk(int flag) {
        if (Common.isDhizukuActive(activity)) {
            if (tryBindDhizukuService()) {
                try {
                    String[] installData = new String[1];
                    installData[0] = new File(activity.getExternalCacheDir(), "update.apk").getPath();
                    int reqCode;

                    if (flag == 0) {
                        reqCode = Constants.REQUEST_INSTALL_SELF_UPDATE;
                    } else {
                        reqCode = Constants.REQUEST_INSTALL_GET_APP;
                    }

                    return ((MyApplication) activity.getApplicationContext()).mDhizukuService.tryInstallPackages(installData, reqCode);
                } catch (Exception ignored) {
                }
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
