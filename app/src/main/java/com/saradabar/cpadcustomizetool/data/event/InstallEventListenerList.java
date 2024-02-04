package com.saradabar.cpadcustomizetool.data.event;

import java.util.HashSet;
import java.util.Set;

public class InstallEventListenerList {

    private final Set<InstallEventListener> listeners = new HashSet<>();

    public void addEventListener(InstallEventListener installEventListener) {
        listeners.add(installEventListener);
    }

    public void installSuccessNotify(int reqCode) {
        for (InstallEventListener listener : listeners) listener.onInstallSuccess(reqCode);
    }

    public void installFailureNotify(int reqCode, String str) {
        for (InstallEventListener listener : listeners) listener.onInstallFailure(reqCode, str);
    }

    public void installErrorNotify(int reqCode, String str) {
        for (InstallEventListener listener : listeners) listener.onInstallError(reqCode, str);
    }
}