package com.saradabar.cpadcustomizetool.util;

import android.content.Context;

public class Path {
    public static String getTemporaryPath(Context context) {
        if (context != null) {
            return context.getExternalCacheDir().getPath() + "/tmp";
        } else return null;
    }
}