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

package com.saradabar.cpadcustomizetool.data.handler;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.view.flagment.DeviceOwnerFragment;

public class ByteProgressHandler extends Handler {

    int reqCode;
    public ProgressBar progressBar;
    public TextView textPercent, textByte;
    public DeviceOwnerFragment.TryXApkTask tryXApkTask;
    public DeviceOwnerFragment.TryApkMTask tryApkMTask;
    public boolean isCompleted = false;

    public ByteProgressHandler(Looper looper, int i) {
        super(looper);
        reqCode = i;
    }

    @TargetApi(Build.VERSION_CODES.O)
    @SuppressWarnings("deprecation")
    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);

        switch (reqCode) {
            case 0:
                if (isCompleted) {
                    progressBar.setProgress(0);
                    return;
                }

                if (!tryXApkTask.isCancelled() || tryXApkTask.getStatus() != AsyncTask.Status.FINISHED) {
                    progressBar.setProgress(tryXApkTask.getLoadedBytePercent());
                    textPercent.setText(new StringBuilder(String.valueOf(progressBar.getProgress())).append("%"));
                    textByte.setText(new StringBuilder(String.valueOf(tryXApkTask.getLoadedCurrentByte())).append(" / ").append(tryXApkTask.getLoadedTotalByte()).append(" MB"));

                    sendEmptyMessageDelayed(0, 100);
                }
                break;
            case 1:
                if (isCompleted) {
                    progressBar.setProgress(0);
                    return;
                }

                if (!tryApkMTask.isCancelled() || tryApkMTask.getStatus() != AsyncTask.Status.FINISHED) {
                    progressBar.setProgress(tryApkMTask.getLoadedBytePercent());
                    textPercent.setText(new StringBuilder(String.valueOf(progressBar.getProgress())).append("%"));
                    textByte.setText(new StringBuilder(String.valueOf(tryApkMTask.getLoadedCurrentByte())).append(" / ").append(tryApkMTask.getLoadedTotalByte()).append(" MB"));

                    sendEmptyMessageDelayed(0, 100);
                }
                break;
        }
    }
}
