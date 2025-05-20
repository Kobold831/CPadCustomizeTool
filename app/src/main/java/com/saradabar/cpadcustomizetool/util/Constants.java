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

import android.content.ComponentName;
import android.content.Intent;

import com.rosan.dhizuku.shared.DhizukuVariables;

import java.util.Arrays;
import java.util.List;

public class Constants {

    // デフォルト値
    /** @noinspection unused*/
    public static final int DEF_INT = 0;
    /** @noinspection unused*/
    public static final String DEF_STR = "";
    public static final boolean DEF_BOOL = false;

    // タブレットのモデル
    public static final String PRODUCT_CT2S = "TAB-A03-BS";
    public static final String PRODUCT_CT2K = "TAB-A03-BR";
    public static final String PRODUCT_CT2L = "TAB-A03-BR2";
    public static final List<String> PRODUCT_CT2 = Arrays.asList(PRODUCT_CT2S, PRODUCT_CT2K, PRODUCT_CT2L);
    public static final String PRODUCT_CT3 = "TAB-A03-BR3";
    public static final String PRODUCT_CTX = "TAB-A05-BD";
    public static final String PRODUCT_CTZ = "TAB-A05-BA1";

    // アクティビティコールバックの番号
    public static final int REQUEST_ACTIVITY_UPDATE = 0;
    public static final int REQUEST_ACTIVITY_ADMIN = 1;
    public static final int REQUEST_ACTIVITY_INSTALL = 2;
    public static final int REQUEST_ACTIVITY_PERMISSION = 3;
    public static final int REQUEST_ACTIVITY_SYSTEM_UPDATE = 4;

    // ダウンロード要求の番号
    public static final int REQUEST_DOWNLOAD_UPDATE_CHECK = 0;
    public static final int REQUEST_DOWNLOAD_APK = 1;
    public static final int REQUEST_DOWNLOAD_APP_CHECK = 2;
    public static final int REQUEST_DOWNLOAD_NOTICE = 3;

    // インストール要求の番号
    public static final int REQUEST_INSTALL_SILENT = 0;
    public static final int REQUEST_INSTALL_SELF_UPDATE = 1;
    public static final int REQUEST_INSTALL_GET_APP = 2;

    //　アプリで使用しているURL
    // TODO: Check.json 参照先変更機能
    public static final String CHECK_JSON = "Check.json";
    public static final String NOTICE_JSON = "ct-notice.json";
    public static final String URL_BASE = "https://raw.githubusercontent.com/Kobold831/Server/main/production/json/";
    public static final String URL_CHECK = URL_BASE + CHECK_JSON;
    public static final String URL_GITHUB = "https://github.com/Kobold831/CPadCustomizeTool";
    public static final String URL_UPDATE_INFO = URL_GITHUB + "/releases/latest";
    public static final String URL_FEEDBACK = "https://forms.gle/LnGuEc4GdRmwzf3GA";
    public static final String URL_NOTICE = URL_BASE + NOTICE_JSON;

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
    public static final String KEY_NORMAL_SETTINGS = "normal_settings";
    public static final String SHARED_PREFERENCE_KEY = "CustomizeTool";
    public static final String KEY_FLAG_KEEP_NAVIGATION_BAR = "enabled_keep_service";
    public static final String KEY_FLAG_KEEP_MARKET_APP = "enabled_keep_market_app_service";
    public static final String KEY_FLAG_KEEP_DCHA_STATE = "enabled_keep_dcha_state";
    public static final String KEY_FLAG_KEEP_USB_DEBUG = "enabled_keep_usb_debug";
    public static final String KEY_FLAG_KEEP_HOME = "enabled_keep_home";
    public static final String KEY_STRINGS_KEEP_HOME_APP_PACKAGE = "save_keep_home";

    public static final String PKG_COMMON_DCHA = "jp.co.benesse.dcha";
    public static final String PKG_DCHA_SERVICE = PKG_COMMON_DCHA + ".dchaservice";
    public static final Intent ACTION_DCHA_SERVICE = new Intent(PKG_DCHA_SERVICE + ".DchaService").setPackage(PKG_DCHA_SERVICE);
    public static final String PKG_UTIL_SERVICE = PKG_COMMON_DCHA + ".dchautilservice";
    public static final Intent ACTION_UTIL_SERVICE = new Intent(PKG_UTIL_SERVICE + ".DchaUtilService").setPackage(PKG_UTIL_SERVICE);
    public static final String PKG_SHO_HOME = "jp.co.benesse.touch.allgrade.b003.touchhomelauncher";
    public static final String HOME_SHO = PKG_SHO_HOME + ".HomeLauncherActivity";
    public static final String PKG_CHU_HOME = "jp.co.benesse.touch.home";
    public static final String HOME_CHU = PKG_CHU_HOME + ".LoadingActivity";

    public static final String DCHA_ACCESS_SYSTEM = PKG_COMMON_DCHA + ".permission.ACCESS_SYSTEM";

    public static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";
    public static final String DOWNLOAD_APK = "update.apk";

    public static final List<String> LIST_UPDATE_MODE = Arrays.asList("パッケージインストーラ", "ADB", "DchaService", "デバイスオーナー", "Dhizuku");

    public static final List<String> LIST_MODEL = Arrays.asList(String.valueOf(PRODUCT_CT2), PRODUCT_CT3, PRODUCT_CTX, PRODUCT_CTZ);

    // Don't use Dhizuku.getOwnerComponent()
    public static final ComponentName DHIZUKU_COMPONENT =
            new ComponentName(DhizukuVariables.OFFICIAL_PACKAGE_NAME, DhizukuVariables.OFFICIAL_PACKAGE_NAME + ".server.DhizukuDAReceiver");
}