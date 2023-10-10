package com.saradabar.cpadcustomizetool.data.event;

import java.util.HashSet;
import java.util.Set;

public class InstallEventListenerList {

    private final Set<InstallEventListener> listeners = new HashSet<>();

    public void addEventListener(InstallEventListener installEventListener) {
        listeners.add(installEventListener);
    }

    public void installSuccessNotify() {
        for (InstallEventListener listener : listeners) listener.onInstallSuccess();
    }

    public void installFailureNotify(String str) {
        for (InstallEventListener listener : listeners) listener.onInstallFailure(str);
    }

    public void installErrorNotify(String str) {
        for (InstallEventListener listener : listeners) listener.onInstallError(str);
    }
}