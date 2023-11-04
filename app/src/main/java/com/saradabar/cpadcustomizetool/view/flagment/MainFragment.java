package com.saradabar.cpadcustomizetool.view.flagment;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;
import com.saradabar.cpadcustomizetool.view.views.LauncherView;
import com.saradabar.cpadcustomizetool.view.views.NormalModeView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.benesse.dcha.dchaservice.IDchaService;
import jp.co.benesse.dcha.dchautilservice.IDchaUtilService;

public class MainFragment extends PreferenceFragmentCompat {

    @SuppressLint("StaticFieldLeak")
    static MainFragment instance = null;

    int width, height;

    boolean isObsDchaState = false,
            isObsNavigation = false,
            isObsUnkSrc = false,
            isObsAdb = false;

    ListView mListView;
    IDchaService mDchaService;
    IDchaUtilService mDchaUtilService;

    String setLauncherPackage,
            installData,
            systemUpdateFilePath;

    SwitchPreference swDchaState,
            swKeepDchaState,
            swNavigation,
            swKeepNavigation,
            swUnkSrc,
            swKeepUnkSrc,
            swAdb,
            swKeepAdb,
            swKeepLauncher,
            swDeviceAdmin;

    Preference preEnableDchaService,
            preEmgManual,
            preSelNorLauncher,
            preNorManual,
            preOtherSettings,
            preReboot,
            preRebootShortcut,
            preSilentInstall,
            preLauncher,
            preResolution,
            preResetResolution,
            preDeviceOwnerFn,
            preSystemUpdate;

    public static MainFragment getInstance() {//インスタンスを取得
        return instance;
    }

    /* システムUIオブザーバー */
    ContentObserver obsDchaState = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swDchaState.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* ナビゲーションバーオブザーバー */
    ContentObserver obsNavigation = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swNavigation.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* 提供元オブザーバー */
    ContentObserver obsUnkSrc = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                //noinspection deprecation
                swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* USBデバッグオブザーバー */
    ContentObserver obsAdb = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* Dchaサービスコネクション */
    ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaService = IDchaService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    /* DchaUtilサービスコネクション */
    ServiceConnection mDchaUtilServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaUtilService = IDchaUtilService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    /* Dcha・UtilServiceにバインド */
    public boolean bindDchaService(int req, boolean isDchaReq) {
        try {
            if (isDchaReq) {
                switch (req) {
                    case Constants.FLAG_SET_DCHA_STATE_0:
                        if (!isCfmDialog()) {
                            mDchaService.setSetupStatus(0);
                        }
                        break;
                    case Constants.FLAG_SET_DCHA_STATE_3:
                        if (!isCfmDialog()) {
                            mDchaService.setSetupStatus(3);
                        }
                        break;
                    case Constants.FLAG_HIDE_NAVIGATION_BAR:
                        mDchaService.hideNavigationBar(true);
                        break;
                    case Constants.FLAG_VIEW_NAVIGATION_BAR:
                        mDchaService.hideNavigationBar(false);
                        break;
                    case Constants.FLAG_REBOOT:
                        mDchaService.rebootPad(0, null);
                        break;
                    case Constants.FLAG_SET_LAUNCHER:
                        mDchaService.clearDefaultPreferredApp(getLauncherPackage());
                        mDchaService.setDefaultPreferredHomeApp(setLauncherPackage);
                        /* listviewの更新 */
                        mListView.invalidateViews();
                        initialize();
                        break;
                    case Constants.FLAG_SYSTEM_UPDATE:
                        if (mDchaService.copyUpdateImage(systemUpdateFilePath, "/cache/update.zip")) {
                            mDchaService.rebootPad(2, "/cache/update.zip");
                            return true;
                        } else return false;
                    case Constants.FLAG_INSTALL_PACKAGE:
                        return mDchaService.installApp(installData, 1);
                    case Constants.FLAG_COPY_UPDATE_IMAGE:
                        return mDchaService.copyUpdateImage("", "");
                    case Constants.FLAG_CHECK:
                        return requireActivity().getApplicationContext().bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
                    case Constants.FLAG_TEST:
                        break;
                }
            } else {
                switch (req) {
                    case Constants.FLAG_CHECK:
                        return requireActivity().getApplicationContext().bindService(Constants.DCHA_UTIL_SERVICE, mDchaUtilServiceConnection, Context.BIND_AUTO_CREATE);
                    case Constants.FLAG_RESOLUTION:
                        return mDchaUtilService.setForcedDisplaySize(width, height);
                }
            }
        } catch (RemoteException ignored) {
        }
        return true;
    }

    /* 設定変更 */
    @SuppressWarnings("deprecation")
    private void chgSetting(int req) {
        switch (req) {
            case Constants.FLAG_SET_DCHA_STATE_0:
                if (!isCfmDialog()) {
                    Settings.System.putInt(requireActivity().getContentResolver(), Constants.DCHA_STATE, 0);
                }
                break;
            case Constants.FLAG_SET_DCHA_STATE_3:
                if (!isCfmDialog()) {
                    Settings.System.putInt(requireActivity().getContentResolver(), Constants.DCHA_STATE, 3);
                }
                break;
            case Constants.FLAG_HIDE_NAVIGATION_BAR:
                Settings.System.putInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 1);
                break;
            case Constants.FLAG_VIEW_NAVIGATION_BAR:
                Settings.System.putInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR, 0);
                break;
            case Constants.FLAG_USB_DEBUG_TRUE:
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                break;
            case Constants.FLAG_USB_DEBUG_FALSE:
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 0);
                break;
            case Constants.FLAG_MARKET_APP_TRUE:
                Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);
                break;
            case Constants.FLAG_MARKET_APP_FALSE:
                Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0);
                break;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_main, rootKey);
        instance = this;
        /* サービスのインターフェースを取得 */
        bindDchaService(Constants.FLAG_CHECK, true);
        bindDchaService(Constants.FLAG_CHECK, false);

        swDchaState = findPreference("pre_dcha_state");
        swKeepDchaState = findPreference("pre_keep_dcha_state");
        swNavigation = findPreference("pre_navigation");
        swKeepNavigation = findPreference("pre_keep_navigation");
        swUnkSrc = findPreference("pre_unk_src");
        swKeepUnkSrc = findPreference("pre_keep_unk_src");
        swAdb = findPreference("pre_adb");
        swKeepAdb = findPreference("pre_keep_adb");
        swKeepLauncher = findPreference("pre_keep_launcher");
        swDeviceAdmin = findPreference("pre_device_admin");
        preLauncher = findPreference("pre_launcher");
        preEnableDchaService = findPreference("pre_enable_dcha_service");
        preOtherSettings = findPreference("pre_other_settings");
        preReboot = findPreference("pre_reboot");
        preRebootShortcut = findPreference("pre_reboot_shortcut");
        preEmgManual = findPreference("pre_emg_manual");
        preSelNorLauncher = findPreference("pre_sel_nor_launcher");
        preNorManual = findPreference("pre_nor_manual");
        preSilentInstall = findPreference("pre_silent_install");
        preResolution = findPreference("pre_resolution");
        preResetResolution = findPreference("pre_reset_resolution");
        preDeviceOwnerFn = findPreference("pre_device_owner_fn");
        preSystemUpdate = findPreference("pre_system_update");

        /* オブサーバーを有効化 */
        isObsDchaState = true;
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, obsDchaState);
        isObsNavigation = true;
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, obsNavigation);
        isObsUnkSrc = true;
        //noinspection deprecation
        requireActivity().getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, obsUnkSrc);
        isObsAdb = true;
        requireActivity().getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, obsAdb);

        /* 初期化 */
        initialize();

        /* リスナーを有効化 */
        swDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (!Preferences.GET_CHANGE_SETTINGS_DCHA_FLAG(getActivity())) {
                if ((boolean) o) {
                    chgSetting(Constants.FLAG_SET_DCHA_STATE_3);
                } else {
                    chgSetting(Constants.FLAG_SET_DCHA_STATE_0);
                }
            } else if (Preferences.GET_CHANGE_SETTINGS_DCHA_FLAG(getActivity())) {
                if ((boolean) o) {
                    bindDchaService(Constants.FLAG_SET_DCHA_STATE_3, true);
                } else {
                    bindDchaService(Constants.FLAG_SET_DCHA_STATE_0, true);
                }
            }
            return false;
        });

        swNavigation.setOnPreferenceChangeListener((preference, o) -> {
            if (!Preferences.GET_CHANGE_SETTINGS_DCHA_FLAG(getActivity())) {
                if ((boolean) o) {
                    chgSetting(Constants.FLAG_HIDE_NAVIGATION_BAR);
                } else {
                    chgSetting(Constants.FLAG_VIEW_NAVIGATION_BAR);
                }
            } else if (Preferences.GET_CHANGE_SETTINGS_DCHA_FLAG(getActivity())) {
                if ((boolean) o) {
                    bindDchaService(Constants.FLAG_HIDE_NAVIGATION_BAR, true);
                } else {
                    bindDchaService(Constants.FLAG_VIEW_NAVIGATION_BAR, true);
                }
            }
            return false;
        });

        swKeepNavigation.setOnPreferenceChangeListener((preference, o) -> {
            requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, (boolean) o).apply();
            if ((boolean) o) {
                chgSetting(Constants.FLAG_VIEW_NAVIGATION_BAR);
                requireActivity().startService(new Intent(getActivity(), KeepService.class));
                requireActivity().startService(Constants.PROTECT_KEEP_SERVICE);
                for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                    if (!KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        try {
                            KeepService.getInstance().startService();
                        } catch (NullPointerException ignored) {
                        }
                    }
                }
            } else {
                SharedPreferences sp = requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
                if (!sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                    requireActivity().stopService(Constants.PROTECT_KEEP_SERVICE);
                }
                for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                    if (KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        KeepService.getInstance().stopService(1);
                        return true;
                    }
                }
            }
            return true;
        });

        swKeepUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, (boolean) o).apply();
            if ((boolean) o) {
                try {
                    //noinspection deprecation
                    Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);
                    requireActivity().startService(new Intent(requireActivity(), KeepService.class));
                    requireActivity().startService(Constants.PROTECT_KEEP_SERVICE);
                    for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                        if (!KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                            try {
                                KeepService.getInstance().startService();
                            } catch (NullPointerException ignored) {
                            }
                        }
                    }
                } catch (SecurityException e) {
                    Toast.toast(getActivity(), R.string.toast_not_change);
                    requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false).apply();
                    swKeepUnkSrc.setChecked(false);
                    return false;
                }
            } else {
                SharedPreferences sp = requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
                if (!sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                    requireActivity().stopService(Constants.PROTECT_KEEP_SERVICE);
                }
                for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                    if (KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        KeepService.getInstance().stopService(3);
                        return true;
                    }
                }
            }
            return true;
        });

        swKeepAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (isCfmDialog()) {
                return false;
            }
            requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, (boolean) o).apply();
            if ((boolean) o) {
                try {
                    if (Preferences.GET_MODEL_ID(getActivity()) == 2) {
                        chgSetting(Constants.FLAG_SET_DCHA_STATE_3);
                    }
                    Thread.sleep(100);
                    Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                    if (Preferences.GET_MODEL_ID(getActivity()) == 2) {
                        chgSetting(Constants.FLAG_SET_DCHA_STATE_0);
                    }
                    requireActivity().startService(new Intent(getActivity(), KeepService.class));
                    requireActivity().startService(Constants.PROTECT_KEEP_SERVICE);
                    for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                        if (!KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                            try {
                                KeepService.getInstance().startService();
                            } catch (NullPointerException ignored) {
                            }
                        }
                    }
                } catch (Exception e) {
                    if (Preferences.GET_MODEL_ID(requireActivity()) == 2) {
                        chgSetting(Constants.FLAG_SET_DCHA_STATE_0);
                    }
                    Toast.toast(getActivity(), R.string.toast_not_change);
                    requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false).apply();
                    swKeepAdb.setChecked(false);
                    return false;
                }
            } else {
                SharedPreferences sp = requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
                if (!sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                    requireActivity().stopService(Constants.PROTECT_KEEP_SERVICE);
                }
                for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE))
                    if (KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        KeepService.getInstance().stopService(4);
                        return true;
                    }
            }
            return true;
        });

        swKeepDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (isCfmDialog()) {
                return false;
            }
            requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, (boolean) o).apply();
            if ((boolean) o) {
                chgSetting(Constants.FLAG_SET_DCHA_STATE_0);
                requireActivity().startService(new Intent(getActivity(), KeepService.class));
                requireActivity().startService(Constants.PROTECT_KEEP_SERVICE);
                for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                    if (!KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        try {
                            KeepService.getInstance().startService();
                        } catch (NullPointerException ignored) {
                        }
                    }
                }
            } else {
                SharedPreferences sp = requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
                if (!sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                    requireActivity().stopService(Constants.PROTECT_KEEP_SERVICE);
                }
                for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                    if (KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        KeepService.getInstance().stopService(2);
                        return true;
                    }
                }
            }
            return true;
        });

        swKeepLauncher.setOnPreferenceChangeListener((preference, o) -> {
            requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putBoolean(Constants.KEY_ENABLED_KEEP_HOME, (boolean) o).apply();
            if ((boolean) o) {
                requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE).edit().putString(Constants.KEY_SAVE_KEEP_HOME, getLauncherPackage()).apply();
                requireActivity().startService(new Intent(getActivity(), KeepService.class));
                requireActivity().startService(Constants.PROTECT_KEEP_SERVICE);
                for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                    if (!KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        try {
                            KeepService.getInstance().startService();
                        } catch (NullPointerException ignored) {
                        }
                    }
                }
            } else {
                SharedPreferences sp = requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
                if (!sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
                    requireActivity().stopService(Constants.PROTECT_KEEP_SERVICE);
                }
                for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                    if (KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                        KeepService.getInstance().stopService(5);
                        return true;
                    }
                }
            }
            return true;
        });

        swUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                try {
                    chgSetting(Constants.FLAG_MARKET_APP_TRUE);
                } catch (SecurityException ignored) {
                    Toast.toast(getActivity(), R.string.toast_not_change);
                    try {
                        //noinspection deprecation
                        swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
                    } catch (Settings.SettingNotFoundException ignored1) {
                    }
                }
            } else {
                try {
                    chgSetting(Constants.FLAG_MARKET_APP_FALSE);
                } catch (SecurityException ignored) {
                    Toast.toast(getActivity(), R.string.toast_not_change);
                    try {
                        //noinspection deprecation
                        swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
                    } catch (Settings.SettingNotFoundException ignored1) {
                    }
                }
            }
            return false;
        });

        swAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (isCfmDialog()) {
                return false;
            }
            if ((boolean) o) {
                try {
                    if (Preferences.GET_MODEL_ID(getActivity()) == 2) {
                        chgSetting(Constants.FLAG_SET_DCHA_STATE_3);
                        Thread.sleep(100);
                    }
                    chgSetting(Constants.FLAG_USB_DEBUG_TRUE);
                    if (Preferences.GET_MODEL_ID(getActivity()) == 2) {
                        chgSetting(Constants.FLAG_SET_DCHA_STATE_0);
                    }
                } catch (SecurityException | InterruptedException ignored) {
                    if (Preferences.GET_MODEL_ID(getActivity()) == 2) {
                        chgSetting(Constants.FLAG_SET_DCHA_STATE_0);
                    }
                    Toast.toast(getActivity(), R.string.toast_not_change);
                    try {
                        swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
                    } catch (Settings.SettingNotFoundException ignored1) {
                    }
                }
            } else {
                try {
                    chgSetting(Constants.FLAG_USB_DEBUG_FALSE);
                } catch (SecurityException ignored) {
                    Toast.toast(getActivity(), R.string.toast_not_change);
                    try {
                        swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
                    } catch (Settings.SettingNotFoundException ignored1) {
                    }
                }
            }
            return false;
        });

        swDeviceAdmin.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                if (!((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), AdministratorReceiver.class))) {
                    startActivityForResult(new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(requireActivity(), AdministratorReceiver.class)), Constants.REQUEST_ADMIN);
                }
            } else {
                swDeviceAdmin.setChecked(true);
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.dialog_title_dcha_service)
                        .setMessage(R.string.dialog_question_device_admin)
                        .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                            ((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(new ComponentName(requireActivity(), AdministratorReceiver.class));
                            swDeviceAdmin.setChecked(false);
                        })

                        .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> {
                            swDeviceAdmin.setChecked(true);
                            dialog.dismiss();
                        })
                        .show();
            }
            return false;
        });

        preEmgManual.setOnPreferenceClickListener(preference -> {
            TextView textView = new TextView(getActivity());
            textView.setText(R.string.dialog_emergency_manual_red);
            textView.setTextSize(16);
            textView.setTextColor(Color.RED);
            textView.setPadding(35, 0, 35, 0);
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dialog_title_emergency_manual)
                    .setMessage(R.string.dialog_emergency_manual)
                    .setView(textView)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preNorManual.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dialog_title_normal_manual)
                    .setMessage(R.string.dialog_normal_manual)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preSelNorLauncher.setOnPreferenceClickListener(preference -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_normal_launcher_list, null);
            List<ResolveInfo> installedAppList = requireActivity().getPackageManager().queryIntentActivities(new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
            List<NormalModeView.AppData> dataList = new ArrayList<>();
            for (ResolveInfo resolveInfo : installedAppList) {
                NormalModeView.AppData data = new NormalModeView.AppData();
                data.label = resolveInfo.loadLabel(requireActivity().getPackageManager()).toString();
                data.icon = resolveInfo.loadIcon(requireActivity().getPackageManager());
                data.packName = resolveInfo.activityInfo.packageName;
                dataList.add(data);
            }
            ListView listView = view.findViewById(R.id.normal_launcher_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new NormalModeView.AppListAdapter(getActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                Preferences.SET_NORMAL_LAUNCHER(Uri.fromParts("package", installedAppList.get(position).activityInfo.packageName, null).toString().replace("package:", ""), getActivity());
                /* listviewの更新 */
                listView.invalidateViews();
                initialize();
            });
            new AlertDialog.Builder(getActivity())
                    .setView(view)
                    .setTitle(R.string.dialog_title_launcher)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preReboot.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_question_reboot)
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> bindDchaService(Constants.FLAG_REBOOT, true))
                    .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preRebootShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(getActivity(), getString(R.string.shortcut_reboot))
                        .setShortLabel(getString(R.string.shortcut_reboot))
                        .setIcon(Icon.createWithResource(requireActivity(), R.drawable.reboot))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), "com.saradabar.cpadcustomizetool.view.activity.RebootActivity"))
                        .build(), null);
            } else {
                makeRebootShortcut();
            }
            return false;
        });

        preEnableDchaService.setOnPreferenceClickListener(preference -> {
            if (isCfmDialog()) {
                return false;
            }
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dialog_title_dcha_service)
                    .setMessage(R.string.dialog_question_dcha_service)
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        if (!bindDchaService(Constants.FLAG_CHECK, true)) {
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(R.string.dialog_error_not_work_dcha)
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                    .show();
                        } else {
                            Preferences.SET_DCHASERVICE_FLAG(true, getActivity());
                            requireActivity().finish();
                            requireActivity().overridePendingTransition(0, 0);
                            startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", true));
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preLauncher.setOnPreferenceClickListener(preference -> {
            View v = requireActivity().getLayoutInflater().inflate(R.layout.layout_launcher_list, null);
            List<ResolveInfo> installedAppList = requireActivity().getPackageManager().queryIntentActivities(new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
            List<LauncherView.AppData> dataList = new ArrayList<>();
            for (ResolveInfo resolveInfo : installedAppList) {
                LauncherView.AppData data = new LauncherView.AppData();
                data.label = resolveInfo.loadLabel(requireActivity().getPackageManager()).toString();
                data.icon = resolveInfo.loadIcon(requireActivity().getPackageManager());
                data.packName = resolveInfo.activityInfo.packageName;
                dataList.add(data);
            }
            mListView = v.findViewById(R.id.launcher_list);
            mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            mListView.setAdapter(new LauncherView.AppListAdapter(getActivity(), dataList));
            mListView.setOnItemClickListener((parent, mView, position, id) -> {
                setLauncherPackage = Uri.fromParts("package", installedAppList.get(position).activityInfo.packageName, null).toString().replace("package:", "");
                bindDchaService(Constants.FLAG_SET_LAUNCHER, true);
            });
            new AlertDialog.Builder(getActivity())
                    .setView(v)
                    .setTitle(R.string.dialog_title_launcher)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preOtherSettings.setOnPreferenceClickListener(preference -> {
            StartActivity.getInstance().transitionFragment(new MainOtherFragment(), true);
            return false;
        });

        preSilentInstall.setOnPreferenceClickListener(preference -> {
            preSilentInstall.setEnabled(false);
            try {
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/vnd.android.package-archive").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), ""), Constants.REQUEST_INSTALL);
            } catch (ActivityNotFoundException ignored) {
                preSilentInstall.setEnabled(true);
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
            return false;
        });

        preResolution.setOnPreferenceClickListener(preference -> {
            /* DchaUtilServiceが機能しているか */
            if (!bindDchaService(Constants.FLAG_CHECK, false)) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.dialog_error_not_work_dcha_util)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
                return false;
            }

            View view = requireActivity().getLayoutInflater().inflate(R.layout.view_resolution, null);
            new AlertDialog.Builder(getActivity())
                    .setView(view)
                    .setTitle(R.string.dialog_title_resolution)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
                        EditText editTextWidth = view.findViewById(R.id.edit_text_1);
                        EditText editTextHeight = view.findViewById(R.id.edit_text_2);
                        try {
                            width = Integer.parseInt(editTextWidth.getText().toString());
                            height = Integer.parseInt(editTextHeight.getText().toString());
                            if (width < 300 || height < 300) {
                                new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_error_illegal_value)
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                        .show();
                            } else {
                                resolutionTask resolutionTask = new resolutionTask();
                                resolutionTask.setListener(StartActivity.getInstance().mCreateListener());
                                resolutionTask.execute();
                            }
                        } catch (NumberFormatException ignored) {
                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(R.string.dialog_error_illegal_value)
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preResetResolution.setOnPreferenceClickListener(preference -> {
            /* DchaUtilServiceが機能しているか */
            if (!bindDchaService(Constants.FLAG_CHECK, false)) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.dialog_error_not_work_dcha_util)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
                return false;
            }
            resetResolution();
            return false;
        });

        preDeviceOwnerFn.setOnPreferenceClickListener(preference -> {
            StartActivity.getInstance().transitionFragment(new DeviceOwnerFragment(), true);
            return false;
        });

        preSystemUpdate.setOnPreferenceClickListener(preference -> {
            preSystemUpdate.setEnabled(false);
            try {
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), ""), Constants.REQUEST_SYSTEM_UPDATE);
            } catch (ActivityNotFoundException ignored) {
                preSystemUpdate.setEnabled(true);
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
            return false;
        });

        /* 端末ごとにPreferenceの状態を設定 */
        switch (Preferences.GET_MODEL_ID(getActivity())) {
            case 0:
                preSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preSilentInstall.setEnabled(false);
                break;
            case 1:
                if (!Build.ID.contains("01.")) {
                    swKeepUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                    swKeepUnkSrc.setEnabled(false);
                    swUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                    swUnkSrc.setEnabled(false);
                }
                swDeviceAdmin.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swDeviceAdmin.setEnabled(false);
                break;
            case 2:
                swUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swKeepUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swUnkSrc.setEnabled(false);
                swKeepUnkSrc.setEnabled(false);
                break;
        }

        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
            swDeviceAdmin.setEnabled(false);
            swDeviceAdmin.setSummary(getString(R.string.pre_main_sum_already_device_owner));
        }

        /* DchaServiceを使用するか */
        if (!Preferences.GET_DCHASERVICE_FLAG(requireActivity())) {
            for (Preference preference : Arrays.asList(
                    preSilentInstall,
                    preLauncher,
                    swKeepLauncher,
                    findPreference("category_emergency"),
                    findPreference("category_normal"),
                    preReboot,
                    preRebootShortcut,
                    preResolution,
                    preResetResolution,
                    preSystemUpdate
            )) {
                getPreferenceScreen().removePreference(preference);
            }
        } else {
            getPreferenceScreen().removePreference(preEnableDchaService);
        }
    }

    /* 確認ダイアログ */
    private boolean isCfmDialog() {
        if (!Constants.COUNT_DCHA_COMPLETED_FILE.exists() && Constants.IGNORE_DCHA_COMPLETED_FILE.exists()) {
            if (Preferences.GET_CONFIRMATION(getActivity())) {
                new AlertDialog.Builder(getActivity())
                        .setCancelable(false)
                        .setTitle(getString(R.string.dialog_question_are_you_sure))
                        .setMessage(getString(R.string.dialog_confirmation))
                        .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> new AlertDialog.Builder(getActivity())
                                .setCancelable(false)
                                .setTitle(getString(R.string.dialog_title_final_confirmation))
                                .setMessage(getString(R.string.dialog_final_confirmation))
                                .setPositiveButton(R.string.dialog_common_continue, (dialog1, which1) -> {
                                    Preferences.SET_CONFIRMATION(true, getActivity());
                                    dialog1.dismiss();
                                })
                                .setNegativeButton(R.string.dialog_common_cancel, (dialog1, which1) -> dialog.dismiss())
                                .show())
                        .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /* ランチャーのパッケージ名を取得 */
    private String getLauncherPackage() {
        return (requireActivity().getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)).activityInfo.packageName;
    }

    /* ランチャーの名前を取得 */
    private String getLauncherName() {
        return (requireActivity().getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)).activityInfo.loadLabel(requireActivity().getPackageManager()).toString();
    }

    /* 再起動ショートカットを作成 */
    private void makeRebootShortcut() {
        requireActivity().sendBroadcast(new Intent("com.android.launcher.action.INSTALL_SHORTCUT").putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN)
                .setClassName("com.saradabar.cpadcustomizetool", "com.saradabar.cpadcustomizetool.view.activity.RebootActivity"))
                .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getActivity(), R.drawable.reboot))
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_reboot));
        Toast.toast(getActivity(), R.string.toast_common_success);
    }

    /* 初期化 */
    private void initialize() {
        SharedPreferences sp = requireActivity().getSharedPreferences(Constants.SHARED_PREFERENCE_KEY, Context.MODE_PRIVATE);
        try {
            swDchaState.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE) != 0);
        } catch (Settings.SettingNotFoundException ignored) {
        }
        try {
            swNavigation.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != 0);
        } catch (Settings.SettingNotFoundException ignored) {
        }
        try {
            //noinspection deprecation
            swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
        } catch (Settings.SettingNotFoundException ignored) {
        }
        try {
            swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
        } catch (Settings.SettingNotFoundException ignored) {
        }
        swDeviceAdmin.setChecked(((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), AdministratorReceiver.class)));
        swKeepNavigation.setChecked(sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false));
        swKeepUnkSrc.setChecked(sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false));
        swKeepDchaState.setChecked(sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false));
        swKeepAdb.setChecked(sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false));
        swKeepLauncher.setChecked(sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false));
        preLauncher.setSummary(getLauncherName());
        String normalLauncherName = null;
        try {
            normalLauncherName = (String) requireActivity().getPackageManager().getApplicationLabel(requireActivity().getPackageManager().getApplicationInfo(Preferences.GET_NORMAL_LAUNCHER(getActivity()), 0));
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (normalLauncherName == null) {
            preSelNorLauncher.setSummary(getString(R.string.pre_main_sum_no_setting_launcher));
        } else {
            preSelNorLauncher.setSummary(getString(R.string.pre_main_sum_message_2) + normalLauncherName + getString(R.string.pre_main_sum_message_3));
        }

        /* 維持スイッチが有効のときサービスが停止していたら起動 */
        if (sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) || sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            requireActivity().startService(Constants.KEEP_SERVICE);
            requireActivity().startService(Constants.PROTECT_KEEP_SERVICE);
            for (ActivityManager.RunningServiceInfo serviceInfo : ((ActivityManager) requireActivity().getSystemService(Context.ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
                if (!KeepService.class.getName().equals(serviceInfo.service.getClassName())) {
                    try {
                        KeepService.getInstance().startService();
                    } catch (NullPointerException ignored) {
                    }
                }
            }
        } else if (!sp.getBoolean(Constants.KEY_ENABLED_KEEP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_DCHA_STATE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_USB_DEBUG, false) && !sp.getBoolean(Constants.KEY_ENABLED_KEEP_HOME, false)) {
            requireActivity().stopService(Constants.PROTECT_KEEP_SERVICE);
        }
    }

    /* アクティビティ破棄 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDchaService != null) requireActivity().getApplicationContext().unbindService(mDchaServiceConnection);
        if (mDchaUtilService != null) requireActivity().getApplicationContext().unbindService(mDchaUtilServiceConnection);
        if (isObsDchaState) {
            requireActivity().getContentResolver().unregisterContentObserver(obsDchaState);
            isObsDchaState = false;
        }
        if (isObsNavigation) {
            requireActivity().getContentResolver().unregisterContentObserver(obsNavigation);
            isObsNavigation = false;
        }
        if (isObsUnkSrc) {
            requireActivity().getContentResolver().unregisterContentObserver(obsUnkSrc);
            isObsUnkSrc = false;
        }
        if (isObsAdb) {
            requireActivity().getContentResolver().unregisterContentObserver(obsAdb);
            isObsAdb = false;
        }
    }

    /* 再表示 */
    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity().getActionBar() != null) requireActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
        if (!preLauncher.isEnabled()) preLauncher.setEnabled(true);

        /* オブザーバー有効 */
        isObsDchaState = true;
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, obsDchaState);
        isObsNavigation = true;
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, obsNavigation);
        isObsUnkSrc = true;
        //noinspection deprecation
        requireActivity().getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, obsUnkSrc);
        isObsAdb = true;
        requireActivity().getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, obsAdb);

        /* 一括変更 */
        initialize();

        switch (Preferences.GET_MODEL_ID(getActivity())) {
            case 0:
                preSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preSilentInstall.setEnabled(false);
                break;
            case 1:
                swDeviceAdmin.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swDeviceAdmin.setEnabled(false);
                break;
            case 2:
                swUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swKeepUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swUnkSrc.setEnabled(false);
                swKeepUnkSrc.setEnabled(false);
                break;
        }

        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
            swDeviceAdmin.setEnabled(false);
            swDeviceAdmin.setSummary(getString(R.string.pre_main_sum_already_device_owner));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_INSTALL:
                preSilentInstall.setEnabled(true);
                /* シングルApk */
                try {
                    installData = getFilePath(getActivity(), data.getData());
                } catch (NullPointerException ignored) {
                    installData = null;
                }
                if (installData != null) {
                    installTask silent = new installTask();
                    silent.setListener(StartActivity.getInstance().createListener());
                    silent.execute();
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                            .show();
                }
                break;
            case Constants.REQUEST_SYSTEM_UPDATE:
                preSystemUpdate.setEnabled(true);
                try {
                    systemUpdateFilePath = getFilePath(getActivity(), data.getData());
                } catch (Exception ignored) {
                    systemUpdateFilePath = null;
                }
                if (systemUpdateFilePath != null) {
                    if (!bindDchaService(Constants.FLAG_SYSTEM_UPDATE, true)) {
                        new AlertDialog.Builder(getActivity())
                                .setMessage(R.string.dialog_error)
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                } else {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                            .show();
                }
                break;
        }
    }

    /* 選択したファイルデータを取得 */
    private String getFilePath(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            String[] str = DocumentsContract.getDocumentId(uri).split(":");
            switch (uri.getAuthority()) {
                case "com.android.externalstorage.documents":
                    return Environment.getExternalStorageDirectory() + "/" + str[1];
                case "com.android.providers.downloads.documents":
                    return str[1];
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /* インストールタスク */
    public static class installTask extends AsyncTask<Boolean, Void, Boolean> {
        private Listener mListener;

        @Override
        protected void onPreExecute() {
            mListener.onShow();
        }

        @Override
        protected Boolean doInBackground(Boolean... value) {
            return MainFragment.getInstance().isInstallApp();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) mListener.onSuccess();
            else mListener.onFailure();
        }

        public void setListener(Listener listener) {
            mListener = listener;
        }

        /* StartActivity */
        public interface Listener {
            void onShow();

            void onSuccess();

            void onFailure();
        }
    }

    /* 解像度タスク */
    public static class resolutionTask extends AsyncTask<Boolean, Void, Boolean> {
        private Listener mListener;

        @Override
        protected Boolean doInBackground(Boolean... value) {
            return MainFragment.getInstance().isSetResolution();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Runnable runnable = () -> {
                if (result) mListener.onSuccess();
                else mListener.onFailure();
            };
            new Handler().postDelayed(runnable, 1000);
        }

        public void setListener(Listener listener) {
            mListener = listener;
        }

        /* StartActivity */
        public interface Listener {
            void onSuccess();

            void onFailure();
        }
    }

    /* サイレントインストール */
    public boolean isInstallApp() {
        return bindDchaService(Constants.FLAG_INSTALL_PACKAGE, true);
    }

    /* 解像度の変更 */
    public boolean isSetResolution() {
        return bindDchaService(Constants.FLAG_RESOLUTION, false);
    }

    /* 解像度のリセット */
    public void resetResolution() {
        switch (Preferences.GET_MODEL_ID(getActivity())) {
            case 0:
            case 1:
                width = 1280;
                height = 800;
                break;
            case 2:
                width = 1920;
                height = 1200;
                break;
        }
        if (!bindDchaService(Constants.FLAG_RESOLUTION, false)) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_error)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }
}