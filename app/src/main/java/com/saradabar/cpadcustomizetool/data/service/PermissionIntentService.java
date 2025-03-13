package com.saradabar.cpadcustomizetool.data.service;

import android.app.IntentService;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.util.Common;

import java.util.ArrayList;
import java.util.Arrays;
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

        try {
            PackageInfo packageInfo = getBaseContext().getPackageManager().getPackageInfo(Objects.requireNonNull(intent.getStringExtra("packageName")), 0);
            assert packageInfo.applicationInfo != null;
            /* ユーザーアプリか確認 */
            if (packageInfo.applicationInfo.sourceDir.startsWith("/data/app/")) {
                setPermissionGrantState(getBaseContext(), packageInfo.packageName);
            }
        } catch (Exception ignored) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void setPermissionGrantState(Context context, String packageName) {
        if (Common.isDhizukuActive(context)) {
            Dhizuku.bindUserService(new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class)), new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder iBinder) {
                    IDhizukuService iDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
                    try {
                        for (String permission : getRuntimePermissions(context, packageName)) {
                            iDhizukuService.setPermissionGrantState(packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                        }
                    } catch (Exception ignored) {
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            });
        } else {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            for (String permission : getRuntimePermissions(context, packageName)) {
                dpm.setPermissionGrantState(new ComponentName(context, AdministratorReceiver.class), packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            }
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
