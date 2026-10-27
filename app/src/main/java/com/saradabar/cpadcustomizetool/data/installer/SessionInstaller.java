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

package com.saradabar.cpadcustomizetool.data.installer;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.saradabar.cpadcustomizetool.data.service.InstallService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class SessionInstaller {

    private SessionInstaller() {
    }

    public static int createSession(@NonNull PackageInstaller packageInstaller) throws IOException {
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        return packageInstaller.createSession(params);
    }

    public static boolean writeSession(@NonNull PackageInstaller packageInstaller, int sessionId, @NonNull File apkFile) throws IOException {
        long sizeBytes = Constants.SIZE_UNKNOWN;
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
            byte[] buffer = new byte[Constants.BUFFER_SIZE_LARGE];
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

    @Nullable
    public static Object writeSessions(@NonNull PackageInstaller packageInstaller, int sessionId, @NonNull List<String> installData) {
        for (String str : installData) {
            if (str == null) {
                break;
            }

            try {
                if (!writeSession(packageInstaller, sessionId, new File(str))) {
                    return false;
                }
            } catch (Exception e) {
                return e.getMessage();
            }
        }
        return true;
    }

    public static boolean commitSession(@NonNull PackageInstaller packageInstaller, int sessionId, @NonNull Context context, @NonNull Intent intent) throws IOException {
        PackageInstaller.Session session = null;

        try {
            session = packageInstaller.openSession(sessionId);
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

    @NonNull
    public static Intent installServiceIntent(@NonNull Context context, int sessionId, int requestCode) {
        return new Intent(context, InstallService.class)
                .putExtra("REQUEST_CODE", requestCode)
                .putExtra("REQUEST_SESSION", sessionId);
    }
}
