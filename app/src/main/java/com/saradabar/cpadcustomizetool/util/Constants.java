package com.saradabar.cpadcustomizetool.util;

import android.content.Intent;

import java.io.File;

public class Constants {
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

    public static final int REQUEST_UPDATE = 0;
    public static final int REQUEST_ADMIN = 1;
    public static final int REQUEST_INSTALL = 2;
    public static final int REQUEST_PERMISSION = 3;
    public static final int REQUEST_SYSTEM_UPDATE = 4;

    public static final String URL_UPDATE_CHECK = "https://github.com/Kobold831/Server/raw/main/CPadCustomizeTool_Update.xml";
    public static final String URL_SUPPORT_CHECK = "https://github.com/Kobold831/Server/raw/main/CPadCustomizeTool_Support.xml";
    public static final String URL_UPDATE_INFO = "https://docs.google.com/document/d/1uh-FrHM5o84uh7zXw3W_FRIDuzJo8NcVnUD8Rrw4CMQ/";
    public static final String URL_UPDATE = "https://is.gd/W5XR2Z";
    public static final String URL_WIKI_DISCORD = "https://ctabwiki.nerrog.net/?Discord";
    public static final String URL_WIKI_MAIN = "https://ctabwiki.nerrog.net/";
    public static final String URL_GITHUB = "https://github.com/Kobold831/CPadCustomizeTool";

    public static final String DCHA_STATE = "dcha_state";
    public static final String HIDE_NAVIGATION_BAR = "hide_navigation_bar";
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

    public static final Intent DCHA_SERVICE = new Intent("jp.co.benesse.dcha.dchaservice.DchaService").setPackage("jp.co.benesse.dcha.dchaservice");
    public static final Intent DCHA_UTIL_SERVICE = new Intent("jp.co.benesse.dcha.dchautilservice.DchaUtilService").setPackage("jp.co.benesse.dcha.dchautilservice");
    public static final Intent AURORA_SERVICE = new Intent("com.aurora.store.data.service.ResultService").setPackage("com.aurora.store");
    public static final Intent KEEP_SERVICE = new Intent("com.saradabar.cpadcustomizetool.data.service.KeepService").setPackage("com.saradabar.cpadcustomizetool");
    public static final Intent PROTECT_KEEP_SERVICE = new Intent("com.saradabar.cpadcustomizetool.data.service.ProtectKeepService").setPackage("com.saradabar.cpadcustomizetool");

    public static final File IGNORE_DCHA_COMPLETED_FILE = new File("/factory/ignore_dcha_completed");
    public static final File COUNT_DCHA_COMPLETED_FILE = new File("/factory/count_dcha_completed");
}