package com.saradabar.cpadcustomizetool.data.handler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void uncaughtException(@NonNull Thread thread, Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();
        String message = getNowDate() +
                ">DEVICE INFO\n" +
                Build.FINGERPRINT + "\n\n" +
                ">UNHANDLED EXCEPTION\n" +
                ex.getCause() + "\n\n" +
                ">STACK BACKTRACE\n" +
                stackTrace + "\n";
        String str;
        if (Preferences.GET_CRASH_LOG(mContext) != null) {
            str = String.join(",", Preferences.GET_CRASH_LOG(mContext)).replace("    ", "") + message;
        } else str = message;
        Preferences.SAVE_CRASH_LOG(mContext, str);
        Preferences.SET_CRASH(mContext, true);
        mDefaultUncaughtExceptionHandler.uncaughtException(thread, ex);
    }

    @SuppressLint("NewApi")
    public static void LogOverWrite(Throwable throwable, Context context) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();
        String message = getNowDate() +
                ">DEVICE INFO\n" +
                Build.FINGERPRINT + "\n\n" +
                ">HANDLED EXCEPTION\n" +
                throwable.getCause() + "\n\n" +
                ">STACK BACKTRACE\n" +
                stackTrace + "\n";
        String str;
        if (Preferences.GET_CRASH_LOG(context) != null) {
            str = String.join(",", Preferences.GET_CRASH_LOG(context)).replace("    ", "") + message;
        } else str = message;
        Preferences.SAVE_CRASH_LOG(context, str);
    }

    public static String getNowDate() {
        DateFormat df = new SimpleDateFormat("MMM dd HH:mm:ss.SSS z yyyy :\n", Locale.ENGLISH);
        return df.format(System.currentTimeMillis());
    }
}