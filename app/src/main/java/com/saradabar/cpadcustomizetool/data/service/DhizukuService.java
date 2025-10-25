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

package com.saradabar.cpadcustomizetool.data.service;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.Common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class DhizukuService extends IDhizukuService.Stub {

    private final Context mContext;

    public DhizukuService(Context context) {
        mContext = context;
    }

    @Override
    public boolean tryInstallPackages(List<String> installData, int reqCode) {
        int sessionId;

        try {
            sessionId = createSession(mContext.getPackageManager().getPackageInstaller());
            if (sessionId < 0) {
                mContext.getPackageManager().getPackageInstaller().abandonSession(sessionId);
                return false;
            }
        } catch (IOException ignored) {
            return false;
        }

        /* インストールデータの長さ回数繰り返す */
        for (String str : installData) {
            /* 配列の中身を確認 */
            if (str != null) {
                try {
                    if (!writeSession(mContext.getPackageManager().getPackageInstaller(), sessionId, new File(str))) {
                        mContext.getPackageManager().getPackageInstaller().abandonSession(sessionId);
                        return false;
                    }
                } catch (Exception e) {
                    mContext.getPackageManager().getPackageInstaller().abandonSession(sessionId);
                    return false;
                }
            } else {
                /* つぎの配列がnullなら終了 */
                break;
            }
        }

        try {
            if (commitSession(mContext.getPackageManager().getPackageInstaller(), sessionId, mContext, reqCode)) {
                return true;
            } else {
                mContext.getPackageManager().getPackageInstaller().abandonSession(sessionId);
                return false;
            }
        } catch (IOException ignored) {
            mContext.getPackageManager().getPackageInstaller().abandonSession(sessionId);
            return false;
        }
    }

    private int createSession(@NonNull PackageInstaller packageInstaller) throws IOException {
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setInstallLocation(PackageInfo.INSTALL_LOCATION_PREFER_EXTERNAL);
        return packageInstaller.createSession(params);
    }

    private boolean writeSession(PackageInstaller packageInstaller, int sessionId, @NonNull File apkFile) throws IOException {
        long sizeBytes = -1;
        String apkPath = apkFile.getAbsolutePath();
        File file = new File(apkPath);

        if (file.isFile()) {
            sizeBytes = file.length();
        }

        PackageInstaller.Session session = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            session = packageInstaller.openSession(sessionId);
            in = new FileInputStream(apkPath);
            out = session.openWrite(Common.getRandomString(), 0, sizeBytes);
            byte[] buffer = new byte[65536];
            int c;

            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }

            session.fsync(out);
            return true;
        } catch (Exception ignored) {
            if (session != null) session.abandon();
            return false;
        } finally {
            if (out != null) out.close();
            if (in != null) in.close();
        }
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private boolean commitSession(PackageInstaller packageInstaller, int sessionId, Context context, int reqCode) throws IOException {
        PackageInstaller.Session session = null;

        try {
            session = packageInstaller.openSession(sessionId);
            Intent intent = new Intent("com.saradabar.cpadcustomizetool.data.service.InstallService")
                    .setPackage("com.saradabar.cpadcustomizetool")
                    .putExtra("REQUEST_CODE", reqCode)
                    .putExtra("REQUEST_SESSION", sessionId);
            PendingIntent pendingIntent = PendingIntent.getService(
                    context,
                    sessionId,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT
            );

            session.commit(pendingIntent.getIntentSender());
            return true;
        } catch (Exception ignored) {
            if (session != null) session.abandon();
            return false;
        } finally {
            if (session != null) session.close();
        }
    }
}
