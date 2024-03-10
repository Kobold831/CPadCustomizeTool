/*
 * CPad Customize Tool
 * Copyright © 2021-2024 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

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