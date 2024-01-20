package com.saradabar.cpadcustomizetool.data.service;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.RemoteException;

import androidx.annotation.Keep;

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
}