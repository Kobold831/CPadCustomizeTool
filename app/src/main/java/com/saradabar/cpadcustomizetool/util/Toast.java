package com.saradabar.cpadcustomizetool.util;

import android.content.Context;

public class Toast {
    public static void toast(Context context, int resId) {
        android.widget.Toast.makeText(context, resId, android.widget.Toast.LENGTH_SHORT).show();
    }

    public static void toast(Context context, CharSequence text) {
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show();
    }
}