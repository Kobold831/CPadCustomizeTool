package com.saradabar.cpadcustomizetool.data.service;

interface IDhizukuService {
    void setUninstallBlocked(String packageName, boolean uninstallBlocked) = 21;
    boolean isUninstallBlocked(String packageName) = 22;
    void setPermissionPolicy(int policy) = 23;
    void setPermissionGrantState(String packageName, String permission, int grantState) = 24;
    boolean tryInstallPackages(String packageName, in String[] installData) = 25;
    void clearDeviceOwnerApp(String packageName) = 26;
}