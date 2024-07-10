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

import static android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.Context;

import java.util.Set;

public class Preferences {

    /* データ管理 */
    public static void save(Context context, String key, int value) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    public static void save(Context context, String key, boolean value) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    public static void save(Context context, String key, String value) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static int load(Context context, String key, int value) {
        return context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).getInt(key, value);
    }

    public static boolean load(Context context, String key, boolean value) {
        return context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).getBoolean(key, value);
    }

    public static String load(Context context, String key, String value) {
        return context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).getString(key, value);
    }

    public static boolean delete(Context context, String key) {
        context.getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().remove(key).apply();
        return true;
    }

    /* マルチリストのデータ取得 */
    public static Set<String> getEmergencySettings(Context context) {
        return getDefaultSharedPreferences(context).getStringSet(Constants.KEY_EMERGENCY_SETTINGS, null);
    }

    public static boolean isEmergencySettingsDchaState(Context context) {
        Set<String> set = getEmergencySettings(context);

        if (set != null) {
            return set.contains(Integer.toString(1));
        }
        return false;
    }

    public static boolean isEmergencySettingsNavigationBar(Context context) {
        Set<String> set = getEmergencySettings(context);

        if (set != null) {
            return set.contains(Integer.toString(2));
        }
        return false;
    }

    public static boolean isEmergencySettingsLauncher(Context context) {
        Set<String> set = getEmergencySettings(context);

        if (set != null) {
            return set.contains(Integer.toString(3));
        }
        return false;
    }

    public static boolean isEmergencySettingsRemoveTask(Context context) {
        Set<String> set = getEmergencySettings(context);

        if (set != null) {
            return set.contains(Integer.toString(4));
        }
        return false;
    }

    private static Set<String> getNormalModeSettings(Context context) {
        return getDefaultSharedPreferences(context).getStringSet(Constants.KEY_NORMAL_SETTINGS, null);
    }

    public static boolean isNormalModeSettingsDchaState(Context context) {
        Set<String> set = getNormalModeSettings(context);

        if (set != null) {
            return set.contains(Integer.toString(1));
        }
        return false;
    }

    public static boolean isNormalModeSettingsNavigationBar(Context context) {
        Set<String> set = getNormalModeSettings(context);

        if (set != null) {
            return set.contains(Integer.toString(2));
        }
        return false;
    }

    public static boolean isNormalModeSettingsLauncher(Context context) {
        Set<String> set = getNormalModeSettings(context);

        if (set != null) {
            return set.contains(Integer.toString(3));
        }
        return false;
    }

    public static boolean isNormalModeSettingsActivity(Context context) {
        Set<String> set = getNormalModeSettings(context);

        if (set != null) {
            return set.contains(Integer.toString(4));
        }
        return false;
    }
}