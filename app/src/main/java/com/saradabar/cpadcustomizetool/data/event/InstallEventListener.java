package com.saradabar.cpadcustomizetool.data.event;

import java.util.EventListener;

public interface InstallEventListener extends EventListener {

    void onInstallSuccess(int reqCode);
    void onInstallFailure(int reqCode, String str);
    void onInstallError(int reqCode, String str);
}