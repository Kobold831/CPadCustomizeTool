/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.data.task;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.util.Common;

import java.util.List;

public class PermissionGrantTask {

    public void execute(Context context, boolean grant, @NonNull Listener listener) {
        listener.onShow();
        Handler handler = new Handler(Looper.getMainLooper());
        AppExecutors.io(() -> {
            if (Common.isCT2()) {
                handler.post(() -> listener.onError(context.getString(R.string.pre_main_sum_message_1, Build.MODEL)));
                return;
            }

            try {
                doInBackground(context, grant, listener, handler);
            } catch (Exception e) {
                String message = e.getMessage() == null ? context.getString(R.string.dialog_failure) : e.getMessage();
                handler.post(() -> listener.onError(message));
            }
        });
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void doInBackground(Context context, boolean grant, Listener listener, Handler handler) {
        DevicePolicyManager dpm = Common.getDevicePolicyManager(context);
        ComponentName adminComponent = Common.getDeviceAdminComponent(context);

        if (grant) {
            dpm.setPermissionPolicy(adminComponent, DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
        } else {
            dpm.setPermissionPolicy(adminComponent, DevicePolicyManager.PERMISSION_POLICY_PROMPT);
        }

        int grantState = grant
                ? DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                : DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT;

        List<ApplicationInfo> userApps = Common.getUserApplications(context);
        final int totalApps = userApps.size();
        int appIndex = 0;

        for (ApplicationInfo app : userApps) {
            String[] permissions = Common.getRequiredPermissions(context, app.packageName);
            int appNumber = appIndex + 1;
            int totalPermissions = permissions.length;

            for (int i = 0; i < totalPermissions; i++) {
                dpm.setPermissionGrantState(adminComponent, app.packageName, permissions[i], grantState);
                int done = i + 1;
                handler.post(() -> listener.onProgress(appNumber, totalApps, app.packageName, done, totalPermissions));
            }

            if (totalPermissions == 0) {
                handler.post(() -> listener.onProgress(appNumber, totalApps, app.packageName, 0, 0));
            }
            appIndex++;
        }

        handler.post(listener::onSuccess);
    }

    public interface Listener {
        void onShow();

        void onProgress(int currentApp, int totalApp, String packageName, int currentPermission, int totalPermission);

        void onSuccess();

        void onError(String message);
    }
}
