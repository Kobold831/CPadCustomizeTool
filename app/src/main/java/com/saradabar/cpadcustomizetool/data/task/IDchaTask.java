package com.saradabar.cpadcustomizetool.data.task;

import android.content.Context;

import androidx.annotation.NonNull;

import com.saradabar.cpadcustomizetool.util.DchaServiceConnection;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class IDchaTask {

    public void execute(@NonNull Context context, @NonNull Listener listener) {
        DchaServiceConnection.execute(context, listener::onDo);
    }

    public interface Listener {
        void onDo(IDchaService iDchaService);
    }
}
