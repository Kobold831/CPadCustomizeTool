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

import android.content.Intent;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Constants {

    // タブレットモデルの番号
    public static final int MODEL_CT2 = 0;
    public static final int MODEL_CT3 = 1;
    public static final int MODEL_CTX = 2;
    public static final int MODEL_CTZ = 3;

    // 設定変更フラグの番号
    public static final int FLAG_USB_DEBUG_TRUE = 6;
    public static final int FLAG_USB_DEBUG_FALSE = 7;
    public static final int FLAG_MARKET_APP_TRUE = 8;
    public static final int FLAG_MARKET_APP_FALSE = 9;

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
    public static final String URL_CHECK = "https://raw.githubusercontent.com/Kobold831/Server/main/production/json/Check.json";
    public static final String URL_UPDATE_INFO = "https://docs.google.com/document/d/1uh-FrHM5o84uh7zXw3W_FRIDuzJo8NcVnUD8Rrw4CMQ/";
    public static final String URL_GITHUB = "https://github.com/Kobold831/CPadCustomizeTool";
    public static final String URL_FEEDBACK = "https://forms.gle/LnGuEc4GdRmwzf3GA";
    public static final String URL_NOTICE = "https://raw.githubusercontent.com/Kobold831/Server/refs/heads/main/production/json/ct-notice.json";

    public static final String DCHA_STATE = "dcha_state";
    public static final String HIDE_NAVIGATION_BAR = "hide_navigation_bar";
    public static final String BC_PASSWORD_HIT_FLAG = "bc_password_hit";

    // 設定キー
    public static final String KEY_FLAG_APP_SETTINGS_COMPLETE = "settings";
    public static final String KEY_FLAG_APP_START_UPDATE_CHECK = "update";
    public static final String KEY_FLAG_DCHA_FUNCTION = "dcha_service";
    public static final String KEY_FLAG_APP_SETTING_DCHA = "settings_dcha";
    public static final String KEY_FLAG_DCHA_FUNCTION_CONFIRMATION = "confirmation";
    public static final String KEY_FLAG_ERROR_CRASH = "crash";
    public static final String KEY_INT_MODEL_NUMBER = "model_name";
    public static final String KEY_INT_UPDATE_MODE = "update_mode";
    public static final String KEY_INT_GET_APP_TMP = "key_radio_tmp";
    public static final String KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE = "normal_launcher";
    public static final String KEY_STRINGS_CRASH_LOG = "crash_log";

    public static final String KEY_EMERGENCY_SETTINGS = "emergency_settings";
    public static final String KEY_NORMAL_SETTINGS = "normal_settings";
    public static final String SHARED_PREFERENCE_KEY = "CustomizeTool";
    public static final String KEY_FLAG_KEEP_NAVIGATION_BAR = "enabled_keep_service";
    public static final String KEY_FLAG_KEEP_MARKET_APP = "enabled_keep_market_app_service";
    public static final String KEY_FLAG_KEEP_DCHA_STATE = "enabled_keep_dcha_state";
    public static final String KEY_FLAG_KEEP_USB_DEBUG = "enabled_keep_usb_debug";
    public static final String KEY_FLAG_KEEP_HOME = "enabled_keep_home";
    public static final String KEY_STRINGS_KEEP_HOME_APP_PACKAGE = "save_keep_home";
    public static final String KEY_FLAG_AUTO_USB_DEBUG = "enabled_auto_usb_debug";

    public static final String DCHA_SERVICE_PACKAGE = "jp.co.benesse.dcha.dchaservice";
    public static final Intent DCHA_SERVICE = new Intent(DCHA_SERVICE_PACKAGE + ".DchaService").setPackage(DCHA_SERVICE_PACKAGE);
    public static final Intent DCHA_UTIL_SERVICE = new Intent("jp.co.benesse.dcha.dchautilservice.DchaUtilService").setPackage("jp.co.benesse.dcha.dchautilservice");

    public static final File IGNORE_DCHA_COMPLETED_FILE = new File("/factory/ignore_dcha_completed");
    public static final File COUNT_DCHA_COMPLETED_FILE = new File("/factory/count_dcha_completed");

    public static final List<String> LIST_UPDATE_MODE = Arrays.asList("パッケージインストーラ", "ADB", "DchaService", "デバイスオーナー", "Dhizuku");

    public static final String[] LIST_MODEL = {"TAB-A03-BS", "TAB-A03-BR", "TAB-A03-BR2", "TAB-A03-BR3", "TAB-A05-BD", "TAB-A05-BA1"};
}
