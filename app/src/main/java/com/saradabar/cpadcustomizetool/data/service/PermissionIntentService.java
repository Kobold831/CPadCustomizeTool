package com.saradabar.cpadcustomizetool.data.service;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.saradabar.cpadcustomizetool.MyApplication;
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

        if (Common.isDhizukuActive(getBaseContext())) {
            if (tryBindDhizukuService(getBaseContext())) {
                new Handler().postDelayed(() -> {
                    try {
                        PackageInfo packageInfo = getBaseContext().getPackageManager().getPackageInfo(Objects.requireNonNull(intent.getStringExtra("packageName")), 0);
                        /* ユーザーアプリか確認 */
                        if (packageInfo.applicationInfo.sourceDir.startsWith("/data/app/")) {
                            setPermissionGrantState(getBaseContext(), packageInfo.packageName);
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
                    setPermissionGrantState(getBaseContext(), packageInfo.packageName);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
    }

    private boolean tryBindDhizukuService(Context context) {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(context, DhizukuService.class));
        return Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
                ((MyApplication) context.getApplicationContext()).mDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setPermissionGrantState(Context context, String packageName) {
        if (Common.isDhizukuActive(context)) {
            if (tryBindDhizukuService(context)) {
                try {
                    for (String permission : getRuntimePermissions(context, packageName)) {
                        ((MyApplication) context.getApplicationContext()).mDhizukuService.setPermissionGrantState(packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                    }
                } catch (RemoteException ignored) {
                }
            }
        } else {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            for (String permission : getRuntimePermissions(context, packageName)) {
                dpm.setPermissionGrantState(new ComponentName(context, AdministratorReceiver.class), packageName, permission, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private String[] getRuntimePermissions(Context context, String packageName) {
        return new ArrayList<>(Arrays.asList(getRequiredPermissions(context, packageName))).toArray(new String[0]);
    }

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
