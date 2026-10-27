/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.data.service;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.data.installer.SessionInstaller;

import java.io.IOException;
import java.util.List;

public class DhizukuService extends IDhizukuService.Stub {

    private final Context mContext;

    public DhizukuService(Context context) {
        mContext = context;
    }

    @Override
    public boolean tryInstallPackages(List<String> installData, int reqCode) {
        PackageInstaller packageInstaller = mContext.getPackageManager().getPackageInstaller();
        int sessionId;

        try {
            sessionId = SessionInstaller.createSession(packageInstaller);
            if (sessionId < 0) {
                packageInstaller.abandonSession(sessionId);
                return false;
            }
        } catch (IOException ignored) {
            return false;
        }

        if (!Boolean.TRUE.equals(SessionInstaller.writeSessions(packageInstaller, sessionId, installData))) {
            packageInstaller.abandonSession(sessionId);
            return false;
        }

        try {
            if (SessionInstaller.commitSession(packageInstaller, sessionId, mContext, installServiceIntent(sessionId, reqCode))) {
                return true;
            } else {
                packageInstaller.abandonSession(sessionId);
                return false;
            }
        } catch (IOException ignored) {
            packageInstaller.abandonSession(sessionId);
            return false;
        }
    }

    @NonNull
    private Intent installServiceIntent(int sessionId, int reqCode) {
        return new Intent("com.saradabar.cpadcustomizetool.data.service.InstallService")
                .setPackage(BuildConfig.APPLICATION_ID)
                .putExtra("REQUEST_CODE", reqCode)
                .putExtra("REQUEST_SESSION", sessionId);
    }
}
