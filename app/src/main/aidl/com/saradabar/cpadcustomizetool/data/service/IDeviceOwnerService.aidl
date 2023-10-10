package com.saradabar.cpadcustomizetool.data.service;

interface IDeviceOwnerService {
    boolean isDeviceOwnerApp();
    void setUninstallBlocked(String str, boolean bl);
    boolean isUninstallBlocked(String str);
    boolean installPackages(String str, in List<Uri> uriList);
}