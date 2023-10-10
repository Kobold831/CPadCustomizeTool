package com.saradabar.cpadcustomizetool.data.handler;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.view.flagment.DeviceOwnerFragment;

public class ByteProgressHandler extends Handler {

    public ProgressBar progressBar;
    public TextView textPercent, textByte;
    public DeviceOwnerFragment.TryXApkTask tryXApkTask;

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if (!tryXApkTask.isCancelled() || tryXApkTask.getStatus() != AsyncTask.Status.FINISHED) {
            progressBar.setProgress(tryXApkTask.getLoadedBytePercent());
            textPercent.setText(progressBar.getProgress() + "%");
            textByte.setText(tryXApkTask.getLoadedCurrentByte() + " / " + tryXApkTask.getLoadedTotalByte() + " MB");
            sendEmptyMessageDelayed(0, 100);
        }
    }
}