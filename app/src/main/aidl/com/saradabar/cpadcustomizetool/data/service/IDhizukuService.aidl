package com.saradabar.cpadcustomizetool.data.service;

interface IDhizukuService {
    void setUninstallBlocked(String packageName, boolean uninstallBlocked);
    boolean isUninstallBlocked(String packageName);
}