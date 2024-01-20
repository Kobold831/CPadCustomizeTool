package com.saradabar.cpadcustomizetool.data.service;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build;
import android.os.RemoteException;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;

import com.rosan.dhizuku.shared.DhizukuVariables;

public class DhizukuService extends IDhizukuService.Stub {

    private Context context;
    private DevicePolicyManager dpm;

    @Keep
    public DhizukuService(Context context) {
        this.context = context;
        dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    @Override
    public void setUninstallBlocked(String packageName, boolean uninstallBlocked) throws RemoteException {
        dpm.setUninstallBlocked(DhizukuVariables.COMPONENT_NAME, packageName, uninstallBlocked);
    }

    @Override
    public boolean isUninstallBlocked(String packageName) throws RemoteException {
        return dpm.isUninstallBlocked(DhizukuVariables.COMPONENT_NAME, packageName);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void setPermissionPolicy(int policy) {
        dpm.setPermissionPolicy(DhizukuVariables.COMPONENT_NAME, policy);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void setPermissionGrantState(String packageName, String permission, int grantState) {
        dpm.setPermissionGrantState(DhizukuVariables.COMPONENT_NAME, packageName, permission, grantState);
    }
}