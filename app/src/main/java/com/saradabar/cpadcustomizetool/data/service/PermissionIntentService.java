package com.saradabar.cpadcustomizetool.data.service;

import android.app.IntentService;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.saradabar.cpadcustomizetool.util.Common;

import java.util.Objects;

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
            if (Common.isUserApp(packageInfo.applicationInfo)) {
                Common.setPermissionGrantState(getBaseContext(), packageInfo.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }
}
