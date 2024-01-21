package com.saradabar.cpadcustomizetool.data.handler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    Context mContext;
    Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    public CrashHandler(Context context) {
        mContext = context;
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        Common.LogOverWrite(mContext, throwable);
        Preferences.save(mContext, Constants.KEY_FLAG_CRASH_LOG, true);
        mDefaultUncaughtExceptionHandler.uncaughtException(thread, throwable);
    }
}