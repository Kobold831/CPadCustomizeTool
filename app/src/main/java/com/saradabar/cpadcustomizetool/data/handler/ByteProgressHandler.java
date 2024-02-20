package com.saradabar.cpadcustomizetool.data.handler;

import android.os.AsyncTask;
import android.os.Handler;
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

    public ByteProgressHandler(int i) {
        reqCode = i;
    }

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
                    textPercent.setText(progressBar.getProgress() + "%");
                    textByte.setText(tryXApkTask.getLoadedCurrentByte() + " / " + tryXApkTask.getLoadedTotalByte() + " MB");

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
                    textPercent.setText(progressBar.getProgress() + "%");
                    textByte.setText(tryApkMTask.getLoadedCurrentByte() + " / " + tryApkMTask.getLoadedTotalByte() + " MB");

                    sendEmptyMessageDelayed(0, 100);
                }
                break;
        }
    }
}