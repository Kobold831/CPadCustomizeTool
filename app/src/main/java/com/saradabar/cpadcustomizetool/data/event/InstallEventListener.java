package com.saradabar.cpadcustomizetool.data.event;

import java.util.EventListener;

public interface InstallEventListener extends EventListener {
    void onInstallSuccess();
    void onInstallFailure(String str);
    void onInstallError(String str);
}