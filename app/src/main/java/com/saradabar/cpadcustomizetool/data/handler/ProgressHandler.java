package com.saradabar.cpadcustomizetool.data.handler;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.connection.AsyncFileDownload;
import com.saradabar.cpadcustomizetool.view.flagment.MainFragment;

public class ProgressHandler extends Handler {

    public LinearProgressIndicator linearProgressIndicator;
    public TextView textView;
    public AsyncFileDownload asyncfiledownload;

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);

        if (asyncfiledownload.isCancelled()) {
            linearProgressIndicator.setProgress(0);
            linearProgressIndicator.setIndeterminate(true);
            linearProgressIndicator.hide();
        } else if (asyncfiledownload.getStatus() == AsyncTask.Status.FINISHED) {
            linearProgressIndicator.setProgress(0);
            linearProgressIndicator.setIndeterminate(true);
        } else {
            linearProgressIndicator.setIndeterminate(false);
            linearProgressIndicator.setProgress(asyncfiledownload.getLoadedBytePercent());
            try {
                MainFragment.getInstance().preGetApp.setSummary("インストールファイルをサーバーからダウンロードしています...しばらくお待ち下さい...\n進行状況：" + asyncfiledownload.getLoadedBytePercent() + "%");
            } catch (Exception ignored) {
            }

            try {
                textView.setText("インストールファイルをサーバーからダウンロードしています...しばらくお待ち下さい...\n進行状況：" + asyncfiledownload.getLoadedBytePercent() + "%");
            } catch (Exception ignored) {
            }
            sendEmptyMessageDelayed(0, 100);
        }
    }
}