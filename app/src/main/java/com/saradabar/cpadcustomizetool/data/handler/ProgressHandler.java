package com.saradabar.cpadcustomizetool.data.handler;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.data.connection.AsyncFileDownload;

public class ProgressHandler extends Handler {

    public ProgressDialog progressDialog;
    public AsyncFileDownload asyncfiledownload;

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        if (asyncfiledownload.isCancelled()) progressDialog.dismiss();
        else if (asyncfiledownload.getStatus() == AsyncTask.Status.FINISHED) progressDialog.dismiss();
        else {
            progressDialog.setProgress(asyncfiledownload.getLoadedBytePercent());
            sendEmptyMessageDelayed(0, 100);
        }
    }
}