package com.saradabar.cpadcustomizetool.data.service;

/* 追加予定:すべての機能 */
interface IDeviceOwnerService {
    boolean isDeviceOwnerApp();
    void setUninstallBlocked(String str, boolean bl);
    boolean isUninstallBlocked(String str);
    boolean isInstallPackages(String str, in List<Uri> uriList);
}