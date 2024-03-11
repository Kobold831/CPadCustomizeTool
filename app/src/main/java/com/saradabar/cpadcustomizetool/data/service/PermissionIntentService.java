package com.saradabar.cpadcustomizetool.data.service;

import android.app.IntentService;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;

import com.saradabar.cpadcustomizetool.util.Common;

@SuppressWarnings("deprecation")
public class PermissionIntentService extends IntentService {

    public PermissionIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Common.isDhizukuActive(getBaseContext())) {
                if (Common.tryBindDhizukuService(getBaseContext())) {
                    Runnable runnable = () -> {
                        for (ApplicationInfo app : getBaseContext().getPackageManager().getInstalledApplications(0)) {
                            /* ユーザーアプリか確認 */
                            if (app.sourceDir.startsWith("/data/app/")) {
                                Common.setPermissionGrantState(getBaseContext(), app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                            }
                        }
                    };

                    new Handler().postDelayed(runnable, 5000);
                }
            } else {
                for (ApplicationInfo app : getBaseContext().getPackageManager().getInstalledApplications(0)) {
                    /* ユーザーアプリか確認 */
                    if (app.sourceDir.startsWith("/data/app/")) {
                        Common.setPermissionGrantState(getBaseContext(), app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                    }
                }
            }
        }
    }
}
