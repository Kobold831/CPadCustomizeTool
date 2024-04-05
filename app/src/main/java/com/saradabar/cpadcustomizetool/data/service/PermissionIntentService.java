package com.saradabar.cpadcustomizetool.data.service;

import android.app.IntentService;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;

import com.saradabar.cpadcustomizetool.util.Common;

import java.util.Objects;

@SuppressWarnings("deprecation")
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

        if (Common.isDhizukuActive(getBaseContext())) {
            if (Common.tryBindDhizukuService(getBaseContext())) {
                new Handler().postDelayed(() -> {
                    try {
                        PackageInfo packageInfo = getBaseContext().getPackageManager().getPackageInfo(Objects.requireNonNull(intent.getStringExtra("packageName")), 0);
                        /* ユーザーアプリか確認 */
                        if (packageInfo.applicationInfo.sourceDir.startsWith("/data/app/")) {
                            Common.setPermissionGrantState(getBaseContext(), packageInfo.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                        }
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                }, 5000);
            }
        } else {
            try {
                PackageInfo packageInfo = getBaseContext().getPackageManager().getPackageInfo(Objects.requireNonNull(intent.getStringExtra("packageName")), 0);
                /* ユーザーアプリか確認 */
                if (packageInfo.applicationInfo.sourceDir.startsWith("/data/app/")) {
                    Common.setPermissionGrantState(getBaseContext(), packageInfo.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
    }
}
