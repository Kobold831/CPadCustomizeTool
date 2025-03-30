/*
 * CPad Customize Tool
 * Copyright © 2021-2024 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.util;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Set;

public class Preferences {

    /* データ管理 */
    public static void save(@NonNull Context context, String key, int value) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    public static void save(@NonNull Context context, String key, boolean value) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    public static void save(@NonNull Context context, String key, String value) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static void save(@NonNull Context context, String key, ArrayList<String> value) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putString(key, new JSONArray(value).toString()).apply();
    }

    public static int load(@NonNull Context context, String key, int value) {
        return context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).getInt(key, value);
    }

    public static boolean load(@NonNull Context context, String key, boolean value) {
        return context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).getBoolean(key, value);
    }

    public static String load(@NonNull Context context, String key, String value) {
        return context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).getString(key, value);
    }

    @Nullable
    public static ArrayList<String> load(@NonNull Context context, String key) {
        try {
            JSONArray jsonArray = new JSONArray(context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).getString(key, null));
            ArrayList<String> arrayList = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                arrayList.add(jsonArray.get(i).toString());
            }
            return arrayList;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void delete(@NonNull Context context, String key) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().remove(key).apply();
    }

    /* マルチリストのデータ取得 */
    public static boolean loadMultiList(Context context, String key, int item) {
        Set<String> strings = getDefaultSharedPreferences(context).getStringSet(key, null);

        if (strings == null) {
            return false;
        }
        return strings.contains(Integer.toString(item));
    }
}
