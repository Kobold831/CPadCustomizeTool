package com.saradabar.cpadcustomizetool.data.service;

import android.app.IntentService;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.saradabar.cpadcustomizetool.util.Common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/** @noinspection deprecation*/
public class PermissionIntentService extends IntentService {

    public PermissionIntentService() {
        super("PermissionIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        if (intent.getStringExtra("packageName") == null) {
            return;
        }

        try {
            PackageInfo packageInfo = getBaseContext().getPackageManager().getPackageInfo(Objects.requireNonNull(intent.getStringExtra("packageName")), 0);

            if (packageInfo.applicationInfo == null) {
                return;
            }
            /* ユーザーアプリか確認 */
            if (packageInfo.applicationInfo.sourceDir.startsWith("/data/app/")) {
                setPermissionGrantState(getBaseContext(), packageInfo.packageName);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void setPermissionGrantState(Context context, String packageName) {
        DevicePolicyManager dpm = Common.getDevicePolicyManager(context);
        for (String permission : getRuntimePermissions(context, packageName)) {
            dpm.setPermissionGrantState(Common.getDeviceAdminComponent(context), packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
        }
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.M)
    private String[] getRuntimePermissions(Context context, String packageName) {
        return new ArrayList<>(Arrays.asList(getRequiredPermissions(context, packageName))).toArray(new String[0]);
    }

    @NonNull
    private String[] getRequiredPermissions(Context context, String packageName) {
        try {
            String[] str = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            if (str != null && str.length > 0) {
                return str;
            } else {
                return new String[0];
            }
        } catch (Exception ignored) {
            return new String[0];
        }
    }
}
