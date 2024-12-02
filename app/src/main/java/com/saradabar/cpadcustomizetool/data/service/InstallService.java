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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.IBinder;

import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListenerList;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.view.activity.SelfUpdateActivity;
import com.saradabar.cpadcustomizetool.view.flagment.DeviceOwnerFragment;
import com.saradabar.cpadcustomizetool.view.flagment.MainFragment;

public class InstallService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        postStatus(intent.getIntExtra("REQUEST_SESSION", -1), intent.getIntExtra("REQUEST_CODE", 0), intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1), intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
        stopSelf();
        return START_NOT_STICKY;
    }

    private void postStatus(int sessionId, int code, int status, String extra) {
        InstallEventListenerList installEventListener = new InstallEventListenerList();

        switch (code) {
            case Constants.REQUEST_INSTALL_SILENT:
                if (DeviceOwnerFragment.getInstance() != null) {
                    installEventListener.addEventListener(DeviceOwnerFragment.getInstance());
                }
                break;
            case Constants.REQUEST_INSTALL_GET_APP:
                if (MainFragment.getInstance() != null) {
                    installEventListener.addEventListener(MainFragment.getInstance());
                }
                break;
            case Constants.REQUEST_INSTALL_SELF_UPDATE:
                if (MainActivity.getInstance() != null) {
                    installEventListener.addEventListener(MainActivity.getInstance());
                } else if (SelfUpdateActivity.getInstance() != null) {
                    installEventListener.addEventListener(SelfUpdateActivity.getInstance());
                }
                break;
        }

        switch (status) {
            case PackageInstaller.STATUS_SUCCESS:
                try {
                    getPackageManager().getPackageInstaller().openSession(sessionId).close();
                } catch (Exception ignored) {
                }
                installEventListener.installSuccessNotify(code);
                break;
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                getPackageManager().getPackageInstaller().abandonSession(sessionId);
                installEventListener.installFailureNotify(code, getErrorMessage(this, status) + "\n" + extra);
                break;
            default:
                getPackageManager().getPackageInstaller().abandonSession(sessionId);
                installEventListener.installErrorNotify(code, getErrorMessage(this, status) + "\n" + extra);
                break;
        }
    }

    private String getErrorMessage(Context context, int status) {
        return switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED ->
                    context.getString(R.string.installer_status_user_action);
            case PackageInstaller.STATUS_FAILURE_BLOCKED ->
                    context.getString(R.string.installer_status_failure_blocked);
            case PackageInstaller.STATUS_FAILURE_CONFLICT ->
                    context.getString(R.string.installer_status_failure_conflict);
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                    context.getString(R.string.installer_status_failure_incompatible);
            case PackageInstaller.STATUS_FAILURE_INVALID ->
                    context.getString(R.string.installer_status_failure_invalid);
            case PackageInstaller.STATUS_FAILURE_STORAGE ->
                    context.getString(R.string.installer_status_failure_storage);
            default -> context.getString(R.string.installer_status_failure);
        };
    }
}
