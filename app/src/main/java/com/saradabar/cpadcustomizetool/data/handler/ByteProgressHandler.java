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

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.saradabar.cpadcustomizetool.data.task.ApkMCopyTask;
import com.saradabar.cpadcustomizetool.data.task.ApkSCopyTask;
import com.saradabar.cpadcustomizetool.data.task.XApkCopyTask;

public class ByteProgressHandler extends Handler {

    public XApkCopyTask xApkCopyTask;
    public ApkMCopyTask apkMCopyTask;
    public ApkSCopyTask apkSCopyTask;
    public ProgressBar progressBar;
    public TextView textPercent, textByte;

    public ByteProgressHandler(Looper looper) {
        super(looper);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);

        if (xApkCopyTask != null) {
            if (xApkCopyTask.isFinish()) {
                progressBar.setProgress(0);
                return;
            }

            progressBar.setProgress(xApkCopyTask.getLoadedBytePercent(progressBar.getContext()));
            textPercent.setText(new StringBuilder(String.valueOf(progressBar.getProgress())).append("%"));
            textByte.setText(new StringBuilder(String.valueOf(xApkCopyTask.getLoadedCurrentByte(textByte.getContext()))).append(" / ").append(xApkCopyTask.getLoadedTotalByte()).append(" MB"));

            sendEmptyMessageDelayed(0, 100);
        }

        if (apkMCopyTask != null) {
            if (apkMCopyTask.isFinish()) {
                progressBar.setProgress(0);
                return;
            }

            progressBar.setProgress(apkMCopyTask.getLoadedBytePercent(progressBar.getContext()));
            textPercent.setText(new StringBuilder(String.valueOf(progressBar.getProgress())).append("%"));
            textByte.setText(new StringBuilder(String.valueOf(apkMCopyTask.getLoadedCurrentByte(textByte.getContext()))).append(" / ").append(apkMCopyTask.getLoadedTotalByte()).append(" MB"));

            sendEmptyMessageDelayed(0, 100);
        }

        if (apkSCopyTask != null) {
            if (apkSCopyTask.isFinish()) {
                progressBar.setProgress(0);
                return;
            }

            progressBar.setProgress(apkSCopyTask.getLoadedBytePercent(progressBar.getContext()));
            textPercent.setText(new StringBuilder(String.valueOf(progressBar.getProgress())).append("%"));
            textByte.setText(new StringBuilder(String.valueOf(apkSCopyTask.getLoadedCurrentByte(textByte.getContext()))).append(" / ").append(apkSCopyTask.getLoadedTotalByte()).append(" MB"));

            sendEmptyMessageDelayed(0, 100);
        }
    }
}
