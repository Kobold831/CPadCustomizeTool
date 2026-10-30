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

package com.saradabar.cpadcustomizetool.data.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.task.CopyProgressTask;
import com.saradabar.cpadcustomizetool.util.Constants;

public class ByteProgressHandler extends Handler {

    public CopyProgressTask task;
    public ProgressBar progressBar;
    public AppCompatTextView textPercent, textByte;

    private String lastPercent = "";

    public ByteProgressHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);

        if (task == null) {
            return;
        }

        if (msg.what == 1) {
            if (msg.arg2 > 0) {
                if (lastPercent.isEmpty()) {
                    lastPercent = textPercent.getContext().getString(R.string.progress_percent, progressBar.getProgress());
                }
                textPercent.setText(lastPercent + textPercent.getContext().getString(R.string.progress_file_count, msg.arg1, msg.arg2));
            }
            return;
        }

        if (task.isFinish()) {
            progressBar.setProgress(0);
            return;
        }

        int progress = task.getLoadedBytePercent(progressBar.getContext());
        progressBar.setProgress(progress);
        lastPercent = textPercent.getContext().getString(R.string.progress_percent, progress);

        int totalFiles = task.getTotalFileCount();
        String percent = lastPercent;

        if (totalFiles > 0) {
            percent = percent + textPercent.getContext().getString(R.string.progress_file_count, task.getCurrentFileCount(), totalFiles);
        }

        textPercent.setText(percent);
        textByte.setText(textByte.getContext().getString(R.string.progress_byte, task.getLoadedCurrentByte(textByte.getContext()), task.getLoadedTotalByte()));

        sendEmptyMessageDelayed(0, Constants.PROGRESS_INTERVAL_MS);
    }
}
