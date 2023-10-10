package com.saradabar.cpadcustomizetool.data.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.IBinder;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.connection.Updater;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListenerList;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;

import java.io.IOException;

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
            case 0:
                installEventListener.addEventListener(StartActivity.getInstance());
                break;
            case 1:
                installEventListener.addEventListener(Updater.getInstance());
        }
        switch (status) {
            case PackageInstaller.STATUS_SUCCESS:
                try {
                    getPackageManager().getPackageInstaller().openSession(sessionId).close();
                } catch (Exception ignored) {
                }
                installEventListener.installSuccessNotify();
                break;
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                try {
                    getPackageManager().getPackageInstaller().openSession(sessionId).abandon();
                } catch (Exception ignored) {
                }
                installEventListener.installFailureNotify(getErrorMessage(this, status) + "\n" + extra);
                break;
            default:
                try {
                    getPackageManager().getPackageInstaller().openSession(sessionId).abandon();
                } catch (Exception ignored) {
                }
                installEventListener.installErrorNotify(getErrorMessage(this, status) + "\n" + extra);
                break;
        }
    }

    private String getErrorMessage(Context context, int status) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                return context.getString(R.string.installer_status_user_action);
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                return context.getString(R.string.installer_status_failure_blocked);
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return context.getString(R.string.installer_status_failure_conflict);
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return context.getString(R.string.installer_status_failure_incompatible);
            case PackageInstaller.STATUS_FAILURE_INVALID:
                return context.getString(R.string.installer_status_failure_invalid);
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return context.getString(R.string.installer_status_failure_storage);
            default:
                return context.getString(R.string.installer_status_failure);
        }
    }
}