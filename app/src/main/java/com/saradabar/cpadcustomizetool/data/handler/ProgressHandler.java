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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;

public class ProgressHandler extends Handler {

    public FileDownloadTask fileDownloadTask;

    public ProgressHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);

        if (fileDownloadTask.getLoadedBytePercent() >= 100) {
            fileDownloadTask.onProgressUpdate(100);
            return;
        }

        if (fileDownloadTask.isFinish()) {
            fileDownloadTask.onProgressUpdate(100);
            return;
        }

        fileDownloadTask.onProgressUpdate(fileDownloadTask.getLoadedBytePercent());

        sendEmptyMessageDelayed(0, 100);
    }
}
