/*
 * CPad Customize Tool
 * Copyright © 2021-2026 Kobold831 <146227823+kobold831@users.noreply.github.com>
 *
 * CPad Customize Tool is Open Source Software.
 * It is licensed under the terms of the Apache License 2.0 issued by the Apache Software Foundation.
 *
 * Kobold831 own any copyright or moral rights in the copyrighted work as defined in the Copyright Act, and has not waived them.
 * Any use, reproduction, or distribution of this software beyond the scope of Apache License 2.0 is prohibited.
 *
 */

package com.saradabar.cpadcustomizetool.util;

import android.content.Intent;

import java.util.ArrayList;
import java.util.Arrays;

public class Constants {

    // デフォルト値
    public static final int DEF_INT = 0;
    public static final String DEF_STR = "";
    public static final boolean DEF_BOOL = false;

    // タブレットのモデル
    public static final String PRODUCT_CT2S = "TAB-A03-BS";
    public static final String PRODUCT_CT2K = "TAB-A03-BR";
    public static final String PRODUCT_CT2L = "TAB-A03-BR2";
    public static final ArrayList<String> PRODUCT_CT2 = new ArrayList<>(Arrays.asList(PRODUCT_CT2S, PRODUCT_CT2K, PRODUCT_CT2L));
    public static final String PRODUCT_CT3 = "TAB-A03-BR3";
    public static final String PRODUCT_CTX = "TAB-A05-BD";
    public static final String PRODUCT_CTZ = "TAB-A05-BA1";

    // DchaState
    public static final int DIGICHALIZE_STATUS_UNDIGICHALIZE = 0;
    public static final int DIGICHALIZE_STATUS_DIGICHALIZED = 3;
    // BenesseExtension
    public static final int XGA = 1;
    public static final int WXGA = 2;
    public static final int WUXGA = 0;
    public static final int XGA_WIDTH = 1024;
    public static final int XGA_HEIGHT = 768;
    public static final int WXGA_WIDTH = 1280;
    public static final int WXGA_HEIGHT = 800;
    public static final int WUXGA_WIDTH = 1920;
    public static final int WUXGA_HEIGHT = 1200;

    // アクティビティコールバックの番号
    public static final int REQUEST_ACTIVITY_UPDATE = 0;
    public static final int REQUEST_ACTIVITY_ADMIN = 1;
    public static final int REQUEST_ACTIVITY_INSTALL = 2;
    public static final int REQUEST_ACTIVITY_PERMISSION = 3;
    public static final int REQUEST_ACTIVITY_WELCOME = 5;

    // ダウンロード要求の番号
    public static final int REQUEST_DOWNLOAD_UPDATE_CHECK = 0;
    public static final int REQUEST_DOWNLOAD_APK = 1;
    public static final int REQUEST_DOWNLOAD_APP_CHECK = 2;
    public static final int REQUEST_DOWNLOAD_NOTICE = 3;
    public static final int REQUEST_DOWNLOAD_APP_LIST = 4;

    // インストール要求の番号
    public static final int REQUEST_INSTALL_SILENT = 0;
    public static final int REQUEST_INSTALL_SELF_UPDATE = 1;
    public static final int REQUEST_INSTALL_GET_APP = 2;

    public static final long DHIZUKU_SERVICE_TIMEOUT = 30 * 1000L;

    public static final int REQUEST_PERMISSION_STORAGE = 0;
    public static final int REQUEST_PERMISSION_DCHA_SYSTEM = 1;

    public static final int INSTALLER_LIST_FROM_MAIN = 0;
    public static final int INSTALLER_LIST_FROM_SETTINGS = 1;

    public static final int UPDATE_MODE_PACKAGE_INSTALLER = 0;
    public static final int UPDATE_MODE_ADB = 1;
    public static final int UPDATE_MODE_DCHA = 2;
    public static final int UPDATE_MODE_DEVICE_OWNER = 3;
    public static final int UPDATE_MODE_DHIZUKU = 4;
    public static final int DEF_UPDATE_MODE = UPDATE_MODE_ADB;

    public static final int DEBUG_DEVICE_CT2 = 1;
    public static final int DEBUG_DEVICE_CT3 = 2;
    public static final int DEBUG_DEVICE_CTX = 3;
    public static final int DEBUG_DEVICE_CTZ = 4;
    public static final int DEBUG_DEVICE_DIGIT_LENGTH = 1;

    public static final String COURSE_MODE_ELEMENTARY = "1";
    public static final String COURSE_MODE_JUNIOR = "2";

    public static final int EMERGENCY_SETTING_SYSTEM_UI_LEARNING = 1;
    public static final int EMERGENCY_SETTING_HIDE_NAVIGATION_BAR = 2;
    public static final int EMERGENCY_SETTING_HOME_LEARNING = 3;
    public static final int EMERGENCY_SETTING_CLEAR_HISTORY = 4;
    public static final int EMERGENCY_SETTING_START_HOME = 5;

    public static final int NORMAL_SETTING_SYSTEM_UI_NORMAL = 1;
    public static final int NORMAL_SETTING_SHOW_NAVIGATION_BAR = 2;
    public static final int NORMAL_SETTING_HOME_NORMAL = 3;
    public static final int NORMAL_SETTING_START_HOME = 4;

    public static final int MIN_VERSION_CODE_DHIZUKU = 20;
    public static final int MIN_VERSION_CODE_DCHA_SERVICE = 5;

    public static final int SETTING_ON = 1;
    public static final int SETTING_OFF = 0;

    public static final int PERCENT_MAX = 100;
    public static final int PERCENT_NOT_FINISHED = 99;

    public static final int SIZE_UNKNOWN = -1;

    public static final int BYTE_TO_MB = 1024 * 1024;
    public static final int BUFFER_SIZE = 8192;
    public static final int BUFFER_SIZE_LARGE = 65536;
    public static final int BUFFER_SIZE_SMALL = 1024;

    public static final int CONNECT_TIMEOUT_MS = 15000;
    public static final int READ_TIMEOUT_MS = 30000;
    public static final int JSON_TIMEOUT_MS = 3000;
    public static final int DOWNLOAD_THREAD_COUNT = 3;

    public static final int PROGRESS_INTERVAL_MS = 100;
    public static final int RESOLUTION_APPLY_DELAY_MS = 1500;

    public static final int SEC_PER_MIN = 60;
    public static final int SEC_PER_HOUR = 3600;
    public static final int SEC_PER_DAY = 86400;
    public static final int HOUR_PER_DAY = 24;
    public static final int DAY_PER_MONTH = 31;

    public static final int RANDOM_STRING_LENGTH = 5;
    public static final int WELCOME_PAGE_COUNT = 3;
    public static final int WELCOME_INDICATOR_SIZE_DP = 8;
    public static final int MAX_CAUSE_DEPTH = 8;

    public static final String MIME_ZIP = "application/zip";
    public static final String MIME_APK = "application/vnd.android.package-archive";

    // 分割APK
    public static final String[] SPLIT_ALWAYS_SELECT = {
            "UnityDataAssetPack.apk"
    };

    public static final int ABI_ARMEABI_V7A = 2;
    public static final int ABI_ARM64_V8A = 3;

    public static final String SPLIT_ABI_ARMEABI = "armeabi";
    public static final String SPLIT_ABI_ARMEABI_V7A = "armeabi_v7a";
    public static final String SPLIT_ABI_ARM64_V8A = "arm64_v8a";
    public static final String SPLIT_ABI_X86 = "x86";
    public static final String SPLIT_ABI_X86_64 = "x86_64";
    public static final String SPLIT_ABI_MIPS = "mips";
    public static final String SPLIT_ABI_MIPS64 = "mips64";
    public static final String SPLIT_ABI_RISCV64 = "riscv64";

    public static final String SPLIT_LANGUAGE_JA = "ja";

    public static final String SPLIT_DENSITY_LDPI = "ldpi";
    public static final String SPLIT_DENSITY_MDPI = "mdpi";
    public static final String SPLIT_DENSITY_TVDPI = "tvdpi";
    public static final String SPLIT_DENSITY_HDPI = "hdpi";
    public static final String SPLIT_DENSITY_XHDPI = "xhdpi";
    public static final String SPLIT_DENSITY_XXHDPI = "xxhdpi";
    public static final String SPLIT_DENSITY_XXXHDPI = "xxxhdpi";

    public static final int DENSITY_LDPI = 120;
    public static final int DENSITY_MDPI = 160;
    public static final int DENSITY_TVDPI = 213;
    public static final int DENSITY_HDPI = 240;
    public static final int DENSITY_XHDPI = 320;
    public static final int DENSITY_XXHDPI = 480;
    public static final int DENSITY_XXXHDPI = 640;

    //　アプリで使用しているURL
    // TODO: Core.json 参照先変更機能
    public static final String CORE_JSON = "Core.json";
    public static final String APP_LIST_JSON = "AppList.json";
    public static final String URL_BASE = "https://raw.githubusercontent.com/Kobold831/Server/refs/heads/main/production/";
    public static final String URL_CORE = URL_BASE + CORE_JSON;
    public static final String URL_APP_LIST = URL_BASE + APP_LIST_JSON;
    public static final String URL_GITHUB = "https://github.com/Kobold831/CPadCustomizeTool";
    public static final String URL_UPDATE_INFO = URL_GITHUB + "/releases/latest";
    public static final String URL_FEEDBACK = "https://forms.gle/LnGuEc4GdRmwzf3GA";
    public static final String URI_EXTRA_NAME = "uri";
    public static final String WEBVIEW_DEFAULT_URL = "https://www.google.co.jp/intl/ja/";

    public static final String DCHA_STATE = "dcha_state";
    public static final String HIDE_NAVIGATION_BAR = "hide_navigation_bar";
    public static final String BC_PASSWORD_HIT_FLAG = "bc_password_hit";
    public static final String BC_COMPATSCREEN = "bc:compatscreen";

    // 設定キー
    public static final String KEY_FLAG_APP_WELCOME_COMPLETE = "key_flag_app_welcome_complete";
    public static final String KEY_FLAG_APP_SETTINGS_COMPLETE = "settings";
    public static final String KEY_FLAG_APP_START_UPDATE_CHECK = "update";
    public static final String KEY_FLAG_DCHA_FUNCTION = "dcha_service";
    public static final String KEY_FLAG_APP_SETTING_DCHA = "settings_dcha";
    public static final String KEY_FLAG_DCHA_FUNCTION_CONFIRMATION = "confirmation";
    public static final String KEY_FLAG_ERROR_CRASH = "crash";
    public static final String KEY_INT_UPDATE_MODE = "update_mode";
    public static final String KEY_INT_GET_APP_TMP = "key_radio_tmp";
    public static final String KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE = "normal_launcher";
    public static final String KEY_LIST_CRASH_LOG = "LIST_CRASH_LOG";
    public static final String KEY_EMERGENCY_SETTINGS = "emergency_settings";
    public static final String KEY_EMERGENCY_MODE = "emergency_mode";
    public static final String KEY_NORMAL_SETTINGS = "normal_settings";
    public static final String SHARED_PREFERENCE_KEY = "CustomizeTool";
    public static final String KEY_FLAG_KEEP_NAVIGATION_BAR = "enabled_keep_service";
    public static final String KEY_FLAG_KEEP_MARKET_APP = "enabled_keep_market_app_service";
    public static final String KEY_FLAG_KEEP_DCHA_STATE = "enabled_keep_dcha_state";
    public static final String KEY_FLAG_KEEP_USB_DEBUG = "enabled_keep_usb_debug";
    public static final String KEY_FLAG_KEEP_HOME = "enabled_keep_home";
    public static final String KEY_STRINGS_KEEP_HOME_APP_PACKAGE = "save_keep_home";
    public static final String KEY_INT_DEBUG_DEVICE = "key_int_debug_device";
    public static final String KEY_FLAG_SIMPLE_MODE = "key_flag_simple_mode";
    public static final String KEY_FLAG_NORMAL_ENV = "key_flag_normal_env";
    public static final String KEY_FLAG_ALREADY_DIALOG_NORMAL_ENV = "key_flag_already_dialog_normal_env";
    public static final String KEY_DEBUG_RESTRICTION = "debug_restriction";

    // その他
    public static final String PKG_COMMON_DCHA = "jp.co.benesse.dcha";
    public static final String PKG_DCHA_SERVICE = PKG_COMMON_DCHA + ".dchaservice";
    public static final Intent ACTION_DCHA_SERVICE = new Intent(PKG_DCHA_SERVICE + ".DchaService").setPackage(PKG_DCHA_SERVICE);
    public static final String PKG_SHO_HOME = "jp.co.benesse.touch.allgrade.b003.touchhomelauncher";
    public static final String HOME_SHO = PKG_SHO_HOME + ".HomeLauncherActivity";
    public static final String PKG_CHU_HOME = "jp.co.benesse.touch.home";
    public static final String HOME_CHU = PKG_CHU_HOME + ".LoadingActivity";

    public static final String DCHA_ACCESS_SYSTEM = PKG_COMMON_DCHA + ".permission.ACCESS_SYSTEM";

    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    public static final String DOWNLOAD_APK = "update.apk";

    public static final ArrayList<String> LIST_MODEL = new ArrayList<>() {
        {
            addAll(PRODUCT_CT2);
            add(PRODUCT_CT3);
            add(PRODUCT_CTX);
            add(PRODUCT_CTZ);
        }
    };
}
