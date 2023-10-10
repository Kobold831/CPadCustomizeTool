package com.saradabar.cpadcustomizetool.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

public class Preferences {
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

    /* データ管理 */
    public static void SET_UPDATE_FLAG(boolean FLAG, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("update", FLAG).apply();
    }

    public static boolean GET_UPDATE_FLAG(Context context) {
        boolean bl;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        bl = sp.getBoolean("update", true);
        return bl;
    }

    public static void SET_SETTINGS_FLAG(boolean FLAG, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("settings", FLAG).apply();
    }

    public static boolean GET_SETTINGS_FLAG(Context context) {
        boolean bl;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        bl = sp.getBoolean("settings", false);
        return bl;
    }

    public static void SET_MODEL_ID(int MODEL_ID, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("model_name", MODEL_ID).apply();
    }

    public static int GET_MODEL_ID(Context context) {
        int id;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        id = sp.getInt("model_name", 0);
        return id;
    }

    public static void SET_DCHASERVICE_FLAG(boolean FLAG, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("dcha_service", FLAG).apply();
    }

    public static boolean GET_DCHASERVICE_FLAG(Context context) {
        boolean bl;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        bl = sp.getBoolean("dcha_service", false);
        return bl;
    }

    public static void SET_CHANGE_SETTINGS_DCHA_FLAG(boolean FLAG, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("settings_dcha", FLAG).apply();
    }

    public static boolean GET_CHANGE_SETTINGS_DCHA_FLAG(Context context) {
        boolean bl;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        bl = sp.getBoolean("settings_dcha", false);
        return bl;
    }

    public static void SET_NORMAL_LAUNCHER(String string, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString("normal_launcher", string).apply();
    }

    public static String GET_NORMAL_LAUNCHER(Context context) {
        String string;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        string = sp.getString("normal_launcher", null);
        return string;
    }

    public static void SET_UPDATE_MODE(Context context, int i) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("update_mode", i).apply();
    }

    public static int GET_UPDATE_MODE(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt("update_mode", 1);
    }

    public static void SET_CONFIRMATION(boolean bl, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("confirmation", bl).apply();
    }

    public static boolean GET_CONFIRMATION(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return !sp.getBoolean("confirmation", false);
    }

    public static void SAVE_CRASH_LOG(Context context, String str){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString("crash_log", str).apply();
    }

    public static String GET_CRASH_LOG(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString("crash_log","");
    }

    public static boolean REMOVE_CRASH_LOG(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().remove("crash_log").apply();
        return true;
    }

    public static void SET_CRASH(Context context, boolean bl) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("crash", bl).apply();
    }

    public static boolean GET_CRASH(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean("crash", false);
    }
}