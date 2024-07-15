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

    public static final int MODEL_CT2 = 0;
    public static final int MODEL_CT3 = 1;
    public static final int MODEL_CTX = 2;
    public static final int MODEL_CTZ = 3;

    public static final int FLAG_TEST = -1;
    public static final int FLAG_CHECK = 0;
    public static final int FLAG_SET_DCHA_STATE_0 = 1;
    public static final int FLAG_SET_DCHA_STATE_3 = 2;
    public static final int FLAG_HIDE_NAVIGATION_BAR = 3;
    public static final int FLAG_VIEW_NAVIGATION_BAR = 4;
    public static final int FLAG_REBOOT = 5;
    public static final int FLAG_USB_DEBUG_TRUE = 6;
    public static final int FLAG_USB_DEBUG_FALSE = 7;
    public static final int FLAG_MARKET_APP_TRUE = 8;
    public static final int FLAG_MARKET_APP_FALSE = 9;
    public static final int FLAG_SET_LAUNCHER = 10;
    public static final int FLAG_SYSTEM_UPDATE= 11;
    public static final int FLAG_INSTALL_PACKAGE = 12;
    public static final int FLAG_COPY_UPDATE_IMAGE = 13;
    public static final int FLAG_RESOLUTION = 20;

    public static final int REQUEST_ACTIVITY_UPDATE = 0;
    public static final int REQUEST_ACTIVITY_ADMIN = 1;
    public static final int REQUEST_ACTIVITY_INSTALL = 2;
    public static final int REQUEST_ACTIVITY_PERMISSION = 3;
    public static final int REQUEST_ACTIVITY_SYSTEM_UPDATE = 4;

    public static final int REQUEST_DOWNLOAD_UPDATE_CHECK = 0;
    public static final int REQUEST_DOWNLOAD_APK = 1;
    public static final int REQUEST_DOWNLOAD_APP_CHECK = 2;

    public static final int REQUEST_INSTALL_SILENT = 0;
    public static final int REQUEST_INSTALL_SELF_UPDATE = 1;
    public static final int REQUEST_INSTALL_GET_APP = 2;

    public static final String URL_CHECK = "https://raw.githubusercontent.com/Kobold831/Server/main/production/json/Check.json";
    public static final String URL_UPDATE_INFO = "https://docs.google.com/document/d/1uh-FrHM5o84uh7zXw3W_FRIDuzJo8NcVnUD8Rrw4CMQ/";
    public static final String URL_GITHUB = "https://github.com/Kobold831/CPadCustomizeTool";
    public static final String URL_FEEDBACK = "https://forms.gle/LnGuEc4GdRmwzf3GA";

    public static final String DCHA_STATE = "dcha_state";
    public static final String HIDE_NAVIGATION_BAR = "hide_navigation_bar";
    public static final String BC_PASSWORD_HIT_FLAG = "bc_password_hit";

    public static final String KEY_FLAG_SETTINGS = "settings";
    public static final String KEY_FLAG_UPDATE = "update";
    public static final String KEY_MODEL_NAME = "model_name";
    public static final String KEY_FLAG_DCHA_SERVICE = "dcha_service";
    public static final String KEY_FLAG_SETTINGS_DCHA = "settings_dcha";
    public static final String KEY_NORMAL_LAUNCHER = "normal_launcher";
    public static final String KEY_FLAG_UPDATE_MODE = "update_mode";
    public static final String KEY_FLAG_CONFIRMATION = "confirmation";
    public static final String KEY_CRASH_LOG = "crash_log";
    public static final String KEY_FLAG_CRASH_LOG = "crash";
    public static final String KEY_RADIO_TMP = "key_radio_tmp";

    public static final String KEY_EMERGENCY_SETTINGS = "emergency_settings";
    public static final String KEY_NORMAL_SETTINGS = "normal_settings";
    public static final String SHARED_PREFERENCE_KEY = "CustomizeTool";
    public static final String KEY_ENABLED_KEEP_SERVICE = "enabled_keep_service";
    public static final String KEY_ENABLED_KEEP_MARKET_APP_SERVICE = "enabled_keep_market_app_service";
    public static final String KEY_ENABLED_KEEP_DCHA_STATE = "enabled_keep_dcha_state";
    public static final String KEY_ENABLED_KEEP_USB_DEBUG = "enabled_keep_usb_debug";
    public static final String KEY_ENABLED_KEEP_HOME = "enabled_keep_home";
    public static final String KEY_SAVE_KEEP_HOME = "save_keep_home";
    public static final String KEY_ENABLED_AUTO_USB_DEBUG = "enabled_auto_usb_debug";

    public static final String DCHA_SERVICE_PACKAGE = "jp.co.benesse.dcha.dchaservice";
    public static final Intent DCHA_SERVICE = new Intent(DCHA_SERVICE_PACKAGE + ".DchaService").setPackage(DCHA_SERVICE_PACKAGE);
    public static final Intent DCHA_UTIL_SERVICE = new Intent("jp.co.benesse.dcha.dchautilservice.DchaUtilService").setPackage("jp.co.benesse.dcha.dchautilservice");

    public static final File IGNORE_DCHA_COMPLETED_FILE = new File("/factory/ignore_dcha_completed");
    public static final File COUNT_DCHA_COMPLETED_FILE = new File("/factory/count_dcha_completed");

    public static final List<String> list = Arrays.asList("パッケージインストーラ", "ADB", "DchaService", "デバイスオーナー", "Dhizuku");

    public static final String[] modelName = {"TAB-A03-BS", "TAB-A03-BR", "TAB-A03-BR2", "TAB-A03-BR3", "TAB-A05-BD", "TAB-A05-BA1"};
}