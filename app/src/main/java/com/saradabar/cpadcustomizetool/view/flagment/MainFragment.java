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

package com.saradabar.cpadcustomizetool.view.flagment;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuAllActive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.receiver.DeviceAdminReceiver;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.EditAdminActivity;
import com.saradabar.cpadcustomizetool.view.activity.NoticeActivity;
import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.view.flagment.dialog.BypassPermissionDialogFragment;
import com.saradabar.cpadcustomizetool.view.views.GetAppListView;
import com.saradabar.cpadcustomizetool.view.views.LaunchAppListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @noinspection deprecation*/
public class MainFragment extends PreferenceFragmentCompat implements DownloadEventListener, InstallEventListener {

    AlertDialog progressDialog;
    AppCompatTextView progressPercentText;
    AppCompatTextView progressByteText;
    ProgressBar dialogProgressBar;

    String downloadFileUrl;

    SwitchPreferenceCompat swDchaState,
            swKeepDchaState,
            swNavigation,
            swKeepNavigation,
            swUnkSrc,
            swKeepUnkSrc,
            swAdb,
            swKeepAdb,
            swDeviceAdmin,
            swEnableDchaService,
            swPreInstallUnknownSource;

    Preference preUtil,
            preOtherSettings,
            preDeviceOwnerFn,
            preEditAdmin,
            preGetApp,
            preNotice,
            preRequestInstallPackages,
            preDchaFunction,
            preInstallUnknownSourceInfo,
            preAppFunction;

    /* アクティビティ破棄 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().getContentResolver().unregisterContentObserver(dchaStateObserver);
        requireActivity().getContentResolver().unregisterContentObserver(navigationBarObserver);
        requireActivity().getContentResolver().unregisterContentObserver(marketObserver);
        requireActivity().getContentResolver().unregisterContentObserver(usbDebugObserver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ACTIVITY_ADMIN) {
            initPreference();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_main, rootKey);

        swDchaState = findPreference("pre_dcha_state");
        swKeepDchaState = findPreference("pre_keep_dcha_state");
        swNavigation = findPreference("pre_navigation");
        swKeepNavigation = findPreference("pre_keep_navigation");
        swUnkSrc = findPreference("pre_unk_src");
        swKeepUnkSrc = findPreference("pre_keep_unk_src");
        swAdb = findPreference("pre_adb");
        swKeepAdb = findPreference("pre_keep_adb");
        preUtil = findPreference("pre_util");
        preOtherSettings = findPreference("pre_other_settings");
        swEnableDchaService = findPreference("pre_enable_dcha_service");
        preDeviceOwnerFn = findPreference("pre_device_owner_fn");
        preEditAdmin = findPreference("pre_edit_admin");
        swDeviceAdmin = findPreference("pre_device_admin");
        preGetApp = findPreference("pre_get_app");
        preNotice = findPreference("pre_notice");
        swPreInstallUnknownSource = findPreference("pre_owner_install_unknown_source");
        preRequestInstallPackages = findPreference("pre_main_request_install_packages");
        preDchaFunction = findPreference("pre_dcha_function");
        preInstallUnknownSourceInfo = findPreference("pre_install_unknown_source_info");
        preAppFunction = findPreference("pre_app_function");

        swDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }
            new DchaServiceUtil(requireActivity()).setSetupStatus((boolean) o ? 3 : 0, object -> {
            });
            return false;
        });

        swKeepDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                new DchaServiceUtil(requireActivity()).setSetupStatus(0, object -> {
                });
            }
            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_DCHA_STATE, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swNavigation.setOnPreferenceChangeListener((preference, o) -> {
            new DchaServiceUtil(requireActivity()).hideNavigationBar((boolean) o, object -> {
            });
            return false;
        });

        swKeepNavigation.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                new DchaServiceUtil(requireActivity()).hideNavigationBar(false, object -> {
                });
            }
            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            try {
                Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, (boolean) o ? 1 : 0);
            } catch (RuntimeException ignored) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        swKeepUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            try {
                Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, (boolean) o ? 1 : 0);
            } catch (RuntimeException ignored) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                return false;
            }

            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_MARKET_APP, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                if (Common.getDchaCompletedPast()) {
                    // COUNT_DCHA_COMPLETED 存在
                    new DchaServiceUtil(requireActivity()).setSetupStatus(3, object -> {
                        if (object.equals(true)) {
                            try {
                                Thread.sleep(100);
                                if (!adbEnabled(true)) {
                                    // 設定変更失敗
                                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                                }
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                });
                            } catch (InterruptedException ignored) {
                                // スレッド中断
                                Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                });
                            }
                        } else {
                            // 失敗
                            Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // COUNT_DCHA_COMPLETED 存在しない
                    if (!adbEnabled(true)) {
                        // 設定変更失敗
                        Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (!adbEnabled(false)) {
                    // 設定変更失敗
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });

        swKeepAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                if (Common.getDchaCompletedPast()) {
                    // COUNT_DCHA_COMPLETED 存在
                    new DchaServiceUtil(requireActivity()).setSetupStatus(3, object -> {
                        if (object.equals(true)) {
                            try {
                                Thread.sleep(100);
                                if (adbEnabled(true)) {
                                    // setupStatusを戻す
                                    new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                        if (object1.equals(true)) {
                                            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, true);
                                            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
                                            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
                                        } else {
                                            Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    // 設定変更失敗
                                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                                    // setupStatusを戻す
                                    new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                    });
                                }
                            } catch (InterruptedException ignored) {
                                // スレッド中断
                                Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(0, object1 -> {
                                });
                            }
                        } else {
                            // 失敗
                            Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // COUNT_DCHA_COMPLETED 存在しない
                    if (adbEnabled(true)) {
                        Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, true);
                        requireActivity().startService(new Intent(requireActivity(), KeepService.class));
                        requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
                    } else {
                        // 設定変更失敗
                        Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            } else {
                Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                requireActivity().startService(new Intent(requireActivity(), KeepService.class));
                requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            }
            return true;
        });

        preUtil.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new UtilFragment(), true, "ユーティリティー");
            return false;
        });

        preOtherSettings.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new DeviceSettingsFragment(), true, "デバイスの設定");
            return false;
        });

        swEnableDchaService.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                if (Common.isShowCfmDialog(requireActivity())) {
                    // 確認ダイアログが必要
                    cfmDialog();
                    return false;
                }

                if (!Preferences.load(requireActivity(), "debug_restriction", false) &&
                        !Common.isDchaActive(requireActivity())) {
                    // デバッグモードが無効かつdcha接続失敗
                    new DialogUtil(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
                Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, true);
                initPreference();
            } else {
                new DialogUtil(requireActivity())
                        .setMessage("DchaSerivce 機能を無効にしますか？")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false);
                            initPreference();
                        })
                        .setNegativeButton(R.string.dialog_common_cancel, null)
                        .show();
            }
            return false;
        });

        preDchaFunction.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new DchaFunctionFragment(), true, "Dcha アプリの機能");
            return false;
        });

        preDeviceOwnerFn.setOnPreferenceClickListener(preference -> {
            requireActivity().runOnUiThread(() ->
                    ((MainActivity) requireActivity()).transitionFragment(new DeviceOwnerFunctionFragment(), true, "デバイスオーナーの機能"));
            return false;
        });

        preEditAdmin.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), EditAdminActivity.class));
            return false;
        });

        swDeviceAdmin.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                if (!((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), DeviceAdminReceiver.class))) {
                    startActivityForResult(
                            new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(requireActivity(), DeviceAdminReceiver.class))
                                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getResources().getString(R.string.device_admin_detail))
                            , Constants.REQUEST_ACTIVITY_ADMIN);
                }
            } else {
                swDeviceAdmin.setChecked(true);
                new DialogUtil(requireActivity())
                        .setTitle(R.string.dialog_disable_device_admin)
                        .setMessage(R.string.dialog_question_admin)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            ((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(new ComponentName(requireActivity(), DeviceAdminReceiver.class));
                            swDeviceAdmin.setChecked(false);
                        })
                        .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> swDeviceAdmin.setChecked(true))
                        .show();
            }
            return false;
        });

        preGetApp.setOnPreferenceClickListener(preference -> {
            showLoadingDialog(getString(R.string.progress_state_connecting));
            new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(requireActivity().getExternalCacheDir(), Constants.CHECK_JSON), Constants.REQUEST_DOWNLOAD_APP_CHECK);
            return false;
        });

        preNotice.setOnPreferenceClickListener(preference -> {
            requireActivity().startActivity(new Intent(requireActivity(), NoticeActivity.class));
            requireActivity().overridePendingTransition(0, 0);
            return false;
        });

        swPreInstallUnknownSource.setOnPreferenceChangeListener((preference, o) -> {
            UserManager userManager = (UserManager) requireActivity().getSystemService(Context.USER_SERVICE);

            if (userManager != null && Common.isDchaActive(requireActivity())) {
                try {
                    if ((boolean) o) {
                        if (userManager.hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
                            userManager.setUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, false);
                            swPreInstallUnknownSource.setSummary("提供元不明のアプリは許可されています。");
                        }
                    } else {
                        userManager.setUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, true);
                        swPreInstallUnknownSource.setSummary("提供元不明のアプリは許可されていません。");
                    }
                    initPreference();
                    return true;
                } catch (SecurityException ignored) {
                    new BypassPermissionDialogFragment().show(requireActivity().getSupportFragmentManager(), "");
                }
            } else {
                new DialogUtil(requireActivity())
                        .setMessage(R.string.dialog_error + "UserManager の取得に失敗したか、DchaService が動作していません。")
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            return false;
        });

        preRequestInstallPackages.setOnPreferenceClickListener(preference -> {
            @SuppressLint("InflateParams") View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_launch_app_list, null);
            List<ApplicationInfo> installedAppList = requireActivity().getPackageManager().getInstalledApplications(0);
            List<LaunchAppListView.AppData> dataList = new ArrayList<>();

            for (ApplicationInfo app : installedAppList) {
                if(requireActivity().getPackageManager().getLaunchIntentForPackage(app.packageName) != null) {
                    LaunchAppListView.AppData data = new LaunchAppListView.AppData();
                    data.icon = app.loadIcon(requireActivity().getPackageManager());
                    data.label = app.loadLabel(requireActivity().getPackageManager()).toString();
                    data.packName = app.packageName;
                    dataList.add(data);
                }
            }

            ListView listView = view.findViewById(R.id.launch_app_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new LaunchAppListView.LaunchAppAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                ArrayList<String> stringArrayList = Common.exec("appops set " + dataList.get(position).packName + " REQUEST_INSTALL_PACKAGES allow");
                StringBuilder stringBuilder = new StringBuilder();

                for (int i = 0; i < stringArrayList.size(); i++) {
                    stringBuilder.append(stringArrayList.get(i));
                }

                if (String.valueOf(stringBuilder).contains("Security exception")) {
                    // 権限なし
                    new BypassPermissionDialogFragment().show(requireActivity().getSupportFragmentManager(), "");
                } else {
                    new DialogUtil(requireActivity())
                            .setMessage("このアプリで、不明なアプリのインストールは許可されました。")
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                }
            });

            new DialogUtil(requireActivity())
                    .setView(view)
                    .setTitle("アプリの選択")
                    .setPositiveButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preAppFunction.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new AppFunctionFragment(), true, "アプリの機能");
            return false;
        });
        /* 一括変更 */
        initPreference();
    }

    /* 初期化 */
    private void initPreference() {
        swDeviceAdmin.setChecked(((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), DeviceAdminReceiver.class)));
        swKeepNavigation.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false));
        swKeepUnkSrc.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_MARKET_APP, false));
        swKeepDchaState.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_DCHA_STATE, false));
        swKeepAdb.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false));

        // サービス起動(必要ないときは自動終了)
        requireActivity().startService(new Intent(requireActivity(), KeepService.class));
        requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));

        /* オブサーバーを有効化 */
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, dchaStateObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, navigationBarObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, marketObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, usbDebugObserver);

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

        if (((UserManager) requireActivity().getSystemService(Context.USER_SERVICE)).hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            swKeepUnkSrc.setVisible(false);
            swUnkSrc.setVisible(false);
            swPreInstallUnknownSource.setChecked(false);
            swPreInstallUnknownSource.setSummary("不明なアプリは許可されていません。");
            preRequestInstallPackages.setEnabled(false);
            preRequestInstallPackages.setSummary("この機能を使用するには、”不明なアプリのユーザー制限を解除”から許可してください。");
            preInstallUnknownSourceInfo.setEnabled(false);
        } else {
            swKeepUnkSrc.setVisible(true);
            swUnkSrc.setVisible(true);
            swPreInstallUnknownSource.setChecked(true);
            swPreInstallUnknownSource.setSummary("不明なアプリは許可されています。");
            preRequestInstallPackages.setEnabled(true);
            preRequestInstallPackages.setSummary("シェルコマンドを実行して、選択したアプリで不明なアプリのインストール権限を許可します。");
            preInstallUnknownSourceInfo.setEnabled(true);
        }

        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
            swDeviceAdmin.setEnabled(false);
            swDeviceAdmin.setSummary(getString(R.string.pre_main_sum_already_device_owner));
        }

        if (Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, Constants.DEF_BOOL)) {
            swEnableDchaService.setChecked(true);
            swEnableDchaService.setSummary("DchaService 機能は、有効です。");
            preDchaFunction.setEnabled(true);
            preDchaFunction.setSummary(R.string.pre_main_sum_dcha_function);
            preAppFunction.setEnabled(true);
            preAppFunction.setSummary(R.string.pre_main_sum_app_function);
        } else {
            swEnableDchaService.setChecked(false);
            swEnableDchaService.setSummary("DchaService 機能は、無効です。");
            preDchaFunction.setEnabled(false);
            preDchaFunction.setSummary(getString(R.string.pre_main_sum_check_enabled, getString(R.string.pre_main_title_use_dcha)));
            preAppFunction.setEnabled(false);
            preAppFunction.setSummary(getString(R.string.pre_main_sum_check_enabled, getString(R.string.pre_main_title_use_dcha)));
        }

        if (!requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            preDeviceOwnerFn.setEnabled(false);
            preDeviceOwnerFn.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
            swDeviceAdmin.setVisible(false);
        }

        if (Common.isCTZ()) {
            swUnkSrc.setVisible(false);
            swKeepUnkSrc.setVisible(false);
        } else {
            swPreInstallUnknownSource.setEnabled(false);
            swPreInstallUnknownSource.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
            preRequestInstallPackages.setEnabled(false);
            preRequestInstallPackages.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
            preInstallUnknownSourceInfo.setVisible(false);
        }
        new FileDownloadTask().execute(this, Constants.URL_NOTICE, new File(requireActivity().getExternalCacheDir(), Constants.NOTICE_JSON), Constants.REQUEST_DOWNLOAD_NOTICE);
    }

    /* システムUIオブザーバー */
    final ContentObserver dchaStateObserver = new ContentObserver(new Handler()) {

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
    final ContentObserver navigationBarObserver = new ContentObserver(new Handler()) {

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
    final ContentObserver marketObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* USBデバッグオブザーバー */
    final ContentObserver usbDebugObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    private boolean adbEnabled(boolean bool) {
        try {
            if (bool) {
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                if (Common.isBenesseExtensionExist("getDchaState")) {
                    Settings.System.putInt(requireActivity().getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 1);
                }
            } else {
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 0);
                if (Common.isBenesseExtensionExist("getDchaState")) {
                    Settings.System.putInt(requireActivity().getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 0);
                }
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void showLoadingDialog(String message) {
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(message);
        progressDialog = new DialogUtil(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.show();
    }

    private void cancelLoadingDialog() {
        if (progressDialog == null) {
            return;
        }

        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    private void cfmDialog() {
        new DialogUtil(requireActivity())
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_dcha_warning))
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) ->
                        Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION_CONFIRMATION, true))
                .setNegativeButton(R.string.dialog_common_cancel, null)
                .show();
    }

    private void startDownload(String str) {
        FileDownloadTask fileDownloadTask = new FileDownloadTask();
        fileDownloadTask.execute(this, str, new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK), Constants.REQUEST_DOWNLOAD_APK);
        ProgressHandler progressHandler = new ProgressHandler(Looper.getMainLooper());
        progressHandler.fileDownloadTask = fileDownloadTask;
        progressHandler.sendEmptyMessage(0);
        View view = getLayoutInflater().inflate(R.layout.view_progress, null);
        progressPercentText = view.findViewById(R.id.progress_percent);
        progressPercentText.setText("");
        progressByteText = view.findViewById(R.id.progress_byte);
        progressByteText.setText("");
        dialogProgressBar = view.findViewById(R.id.progress);
        dialogProgressBar.setProgress(0);
        progressDialog = new DialogUtil(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.setMessage("");
        progressDialog.show();
    }

    @NonNull
    private DchaInstallTask.Listener dchaInstallTaskListener() {
        return new DchaInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                showLoadingDialog(requireActivity().getResources().getString(R.string.progress_state_installing));
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                cancelLoadingDialog();
                new DialogUtil(MainFragment.this.requireActivity())
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                cancelLoadingDialog();
                new DialogUtil(MainFragment.this.requireActivity())
                        .setMessage(R.string.dialog_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }

    @SuppressLint("InflateParams")
    @Override
    public void onDownloadComplete(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APP_CHECK:
                cancelLoadingDialog();
                ArrayList<GetAppListView.AppData> appDataArrayList = new ArrayList<>();

                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(requireActivity().getExternalCacheDir(), Constants.CHECK_JSON));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONArray jsonArray = jsonObj2.getJSONArray("appList");
                    String model = "";

                    if (Common.isCT2()) {
                        model = "CT2";
                    } else if (Common.isCT3()) {
                        model = "CT3";
                    } else if (Common.isCTX()) {
                        model = "CTX";
                    } else if (Common.isCTZ()) {
                        model = "CTZ";
                    }

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONArray jsonArray1 = jsonArray.getJSONObject(i).getJSONArray("targetModel");
                        for (int s = 0; s < jsonArray1.length(); s++) {
                            if (Objects.equals(jsonArray1.getString(s), model) || jsonArray1.getString(s).isEmpty()) {
                                GetAppListView.AppData data = new GetAppListView.AppData();
                                data.name = jsonArray.getJSONObject(i).getString("name");
                                data.description = jsonArray.getJSONObject(i).getString("description");
                                data.url = jsonArray.getJSONObject(i).getString("url");
                                appDataArrayList.add(data);
                            }
                        }
                    }
                } catch (JSONException | IOException ignored) {
                }
                View view = getLayoutInflater().inflate(R.layout.layout_app_list, null);
                ListView listView = view.findViewById(R.id.app_list);
                listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                listView.setAdapter(new GetAppListView.AppListAdapter(requireActivity(), appDataArrayList));
                listView.setOnItemClickListener((parent, view1, position, id) -> {
                    // 選択位置を保存
                    Preferences.save(requireActivity(), Constants.KEY_INT_GET_APP_TMP, position);
                    listView.invalidateViews();
                });
                new DialogUtil(requireActivity())
                        .setView(view)
                        .setTitle("アプリを選択")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            StringBuilder str = new StringBuilder();
                            // 選択位置参照
                            int i = Preferences.load(requireActivity(), Constants.KEY_INT_GET_APP_TMP, 0);
                            str.append("アプリ名：")
                                    .append("\n")
                                    .append(appDataArrayList.get(i).name)
                                    .append("\n\n")
                                    .append("説明：")
                                    .append("\n")
                                    .append(appDataArrayList.get(i).description)
                                    .append("\n");
                            downloadFileUrl = appDataArrayList.get(i).url;

                            if (str.toString().isEmpty()) {
                                return;
                            }
                            new DialogUtil(requireActivity())
                                    .setMessage(str + "\n" + "よろしければ OK を押下してください。")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                        if (!Objects.equals(appDataArrayList.get(i).url, "MYURL")) {
                                            startDownload(appDataArrayList.get(i).url);
                                            dialog.dismiss();
                                        } else {
                                            View view1 = getLayoutInflater().inflate(R.layout.view_app_url, null);
                                            AppCompatEditText editText = view1.findViewById(R.id.edit_app_url);
                                            new DialogUtil(requireActivity())
                                                    .setMessage("http:// または https:// を含む URL を指定してください。")
                                                    .setView(view1)
                                                    .setCancelable(false)
                                                    .setPositiveButton(R.string.dialog_common_ok, (dialog3, which3) -> {
                                                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                                                        //noinspection DataFlowIssue
                                                        startDownload(editText.getText().toString());
                                                    })
                                                    .setNegativeButton(R.string.dialog_common_cancel, null)
                                                    .show();
                                        }
                                    })
                                    .show();
                        })
                        .show();
                break;
            /* APKダウンロード要求の場合 */
            case Constants.REQUEST_DOWNLOAD_APK:
                cancelLoadingDialog();
                switch (Preferences.load(requireActivity(), Constants.KEY_INT_UPDATE_MODE, 1)) {
                    case 0:
                        requireActivity().startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(requireActivity().getExternalCacheDir(), "update.apk").getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case 1:
                        new DialogUtil(requireActivity())
                                .setCancelable(false)
                                .setTitle(getString(R.string.dialog_title_error))
                                .setMessage(getString(R.string.dialog_no_installer, downloadFileUrl))
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                        break;
                    case 2:
                        new DchaInstallTask().execute(requireActivity(), dchaInstallTaskListener(), new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath());
                        break;
                    case 3:
                        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

                        if (!dpm.isDeviceOwnerApp(requireActivity().getPackageName())) {
                            Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, 1);
                            new DialogUtil(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(requireActivity().getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), new ArrayList<>(List.of(new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_GET_APP, this);
                        break;
                    case 4:
                        if (!isDhizukuAllActive(requireActivity())) {
                            Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, 1);
                            new DialogUtil(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(requireActivity().getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), new ArrayList<>(List.of(new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_GET_APP, this);
                        break;
                }
                break;
            case Constants.REQUEST_DOWNLOAD_NOTICE:
                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(requireActivity().getExternalCacheDir(), Constants.NOTICE_JSON));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONArray jsonArray = jsonObj2.getJSONArray("noticeList");

                    if (jsonArray.length() == 0) {
                        // 表示しない
                        preNotice.setVisible(false);
                    } else {
                        preNotice.setTitle("アプリのお知らせ");
                        preNotice.setSummary("お知らせは " + jsonArray.length() + " 件あります。タップして確認できます。");
                    }
                } catch (JSONException | IOException | IllegalStateException ignored) {
                }
                break;
        }
    }

    @Override
    public void onDownloadError(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APK,
                 Constants.REQUEST_DOWNLOAD_APP_CHECK -> {
                cancelLoadingDialog();
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_download))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            case Constants.REQUEST_DOWNLOAD_NOTICE -> preNotice.setVisible(false);
        }
    }

    @Override
    public void onConnectionError(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APK,
                 Constants.REQUEST_DOWNLOAD_APP_CHECK -> {
                cancelLoadingDialog();
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_connection))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            case Constants.REQUEST_DOWNLOAD_NOTICE -> preNotice.setVisible(false);
        }
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
        progressPercentText.setText(new StringBuilder(String.valueOf(progress)).append("%"));
        progressByteText.setText(new StringBuilder(String.valueOf(currentByte)).append(" MB").append("/").append(totalByte).append(" MB"));
        dialogProgressBar.setProgress(progress);
        progressDialog.setMessage(new StringBuilder(getString(R.string.progress_state_download_file)));
    }

    @Override
    public void onInstallSuccess(int reqCode) {
        apkInstallTaskListener().onSuccess();
    }

    @Override
    public void onInstallFailure(int reqCode, String message) {
        apkInstallTaskListener().onFailure(message);
    }

    @Override
    public void onInstallError(int reqCode, String message) {
        apkInstallTaskListener().onError(message);
    }

    private ApkInstallTask.Listener apkInstallTaskListener() {
        return new ApkInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                showLoadingDialog(getString(R.string.progress_state_installing));
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                Common.deleteDirectory(requireActivity().getExternalCacheDir());
                cancelLoadingDialog();
                AlertDialog alertDialog = new DialogUtil(MainFragment.this.requireActivity())
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .create();

                if (!alertDialog.isShowing()) {
                    alertDialog.show();
                }
            }

            /* 失敗 */
            @Override
            public void onFailure(String message) {
                Common.deleteDirectory(requireActivity().getExternalCacheDir());
                cancelLoadingDialog();
                new DialogUtil(MainFragment.this.requireActivity())
                        .setMessage(getString(R.string.dialog_failure_silent_install) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                Common.deleteDirectory(requireActivity().getExternalCacheDir());
                cancelLoadingDialog();
                new DialogUtil(MainFragment.this.requireActivity())
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }
}
