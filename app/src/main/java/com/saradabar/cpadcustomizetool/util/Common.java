package com.saradabar.cpadcustomizetool.util;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;

import java.util.ArrayList;
import java.util.Arrays;

public class Common {
    public static ComponentName getAdministratorComponent(Context context) {
        return new ComponentName(context, com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver.class);
    }

    public static void setPermissionGrantState(Context context, String packageName, int i) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : getRuntimePermissions(context, packageName)) {
                devicePolicyManager.setPermissionGrantState(new ComponentName(context, AdministratorReceiver.class), packageName, permission, i);
            }
        }
    }

    public static String[] getRequiredPermissions(Context context, String packageName) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            String[] str = packageInfo.requestedPermissions;
            if (str != null && str.length > 0) {
                return str;
            } else {
                return new String[0];
            }
        } catch (Exception ignored) {
            return new String[0];
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String[] getRuntimePermissions(Context context, String packageName) {
        return new ArrayList<>(Arrays.asList(getRequiredPermissions(context, packageName))).toArray(new String[0]);
    }
}