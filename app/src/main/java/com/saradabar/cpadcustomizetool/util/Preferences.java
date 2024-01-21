package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

public class Preferences {

    /* データ管理 */
    public static void save(Context context, String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(key, value).apply();
    }

    public static void save(Context context, String key, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).apply();
    }

    public static void save(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    public static int load(Context context, String key, int value) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, value);
    }

    public static boolean load(Context context, String key, boolean value) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, value);
    }

    public static String load(Context context, String key, String value) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, value);
    }

    public static boolean delete(Context context, String key) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove(key).apply();
        return true;
    }

    /* マルチリストのデータ取得 */
    public static Set<String> getEmergencySettings(Context context) {
        SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getStringSet(Constants.KEY_EMERGENCY_SETTINGS, null);
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
        SharedPreferences preferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getStringSet(Constants.KEY_NORMAL_SETTINGS, null);
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