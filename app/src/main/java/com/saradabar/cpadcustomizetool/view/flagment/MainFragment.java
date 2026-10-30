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

package com.saradabar.cpadcustomizetool.view.flagment;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuAllActive;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BenesseExtension;
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
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.receiver.DeviceAdminReceiver;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.AppExecutors;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.NoticeActivity;
import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.view.views.GetAppListView;
import com.saradabar.cpadcustomizetool.view.views.LaunchAppListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
            preGetApp,
            preNotice,
            preRequestInstallPackages,
            preDchaFunction,
            preInstallUnknownSourceInfo,
            preAppFunction;

    PreferenceCategory catDcha;

    private boolean observersRegistered = false;

    private boolean getAppRefreshing = false;

    private static final long APP_LIST_TIMEOUT_MS = 10000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean appListFinished = false;

    private final Runnable appListTimeout = () -> {
        if (appListFinished) {
            return;
        }
        appListFinished = true;
        cancelLoadingDialog();

        if (isAdded()) {
            showDownloadError(R.string.dialog_error_connection);
        }
    };

    /* アクティビティ破棄 */
    @Override
    public void onDestroy() {
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().unregisterContentObserver(dchaStateObserver);
            context.getContentResolver().unregisterContentObserver(navigationBarObserver);
            context.getContentResolver().unregisterContentObserver(marketObserver);
            context.getContentResolver().unregisterContentObserver(usbDebugObserver);
        }
        observersRegistered = false;
        handler.removeCallbacks(appListTimeout);
        cancelLoadingDialog();
        super.onDestroy();
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
        swDeviceAdmin = findPreference("pre_device_admin");
        preGetApp = findPreference("pre_get_app");
        preNotice = findPreference("pre_notice");
        swPreInstallUnknownSource = findPreference("pre_owner_install_unknown_source");
        preRequestInstallPackages = findPreference("pre_main_request_install_packages");
        preDchaFunction = findPreference("pre_dcha_function");
        preInstallUnknownSourceInfo = findPreference("pre_install_unknown_source_info");
        preAppFunction = findPreference("pre_app_function");
        catDcha = findPreference("category_other");

        swDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }
            new DchaServiceUtil(requireActivity()).setSetupStatus((boolean) o ? Constants.DIGICHALIZE_STATUS_DIGICHALIZED : Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object -> {
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
                new DchaServiceUtil(requireActivity()).setSetupStatus(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object -> {
                });
            }
            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_KEEP_DCHA_STATE, (boolean) o);
            Common.startKeepService(requireActivity());
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
            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, (boolean) o);
            Common.startKeepService(requireActivity());
            return true;
        });

        swUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            if (!Common.isWriteSecureSettingsGranted(requireActivity())) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                return false;
            }
            Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, (boolean) o ? Constants.SETTING_ON : Constants.SETTING_OFF);
            return true;
        });

        swKeepUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            if (!Common.isWriteSecureSettingsGranted(requireActivity())) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                return false;
            }
            Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, (boolean) o ? Constants.SETTING_ON : Constants.SETTING_OFF);

            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_KEEP_MARKET_APP, (boolean) o);
            Common.startKeepService(requireActivity());
            return true;
        });

        swAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (!Common.isWriteSecureSettingsGranted(requireActivity())) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                if (Common.getDchaCompletedPast()) {
                    // COUNT_DCHA_COMPLETED 存在
                    new DchaServiceUtil(requireActivity()).setSetupStatus(Constants.DIGICHALIZE_STATUS_DIGICHALIZED, object -> {
                        if (object.equals(true)) {
                            try {
                                Thread.sleep(getResources().getInteger(R.integer.retry_wait_ms));
                                if (!adbEnabled(true)) {
                                    // 設定変更失敗
                                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                                }
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object1 -> {
                                });
                            } catch (InterruptedException ignored) {
                                // スレッド中断
                                Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object1 -> {
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
            if (!Common.isWriteSecureSettingsGranted(requireActivity())) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                return false;
            }

            if (Common.isShowCfmDialog(requireActivity())) {
                // 確認ダイアログが必要
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                if (Common.getDchaCompletedPast()) {
                    // COUNT_DCHA_COMPLETED 存在
                    new DchaServiceUtil(requireActivity()).setSetupStatus(Constants.DIGICHALIZE_STATUS_DIGICHALIZED, object -> {
                        if (object.equals(true)) {
                            try {
                                Thread.sleep(getResources().getInteger(R.integer.retry_wait_ms));
                                if (adbEnabled(true)) {
                                    // setupStatusを戻す
                                    new DchaServiceUtil(requireActivity()).setSetupStatus(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object1 -> {
                                        if (object1.equals(true)) {
                                            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, true);
                                            Common.startKeepService(requireActivity());
                                        } else {
                                            Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    // 設定変更失敗
                                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                                    // setupStatusを戻す
                                    new DchaServiceUtil(requireActivity()).setSetupStatus(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object1 -> {
                                    });
                                }
                            } catch (InterruptedException ignored) {
                                // スレッド中断
                                Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                                // setupStatusを戻す
                                new DchaServiceUtil(requireActivity()).setSetupStatus(Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE, object1 -> {
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
                        Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, true);
                        Common.startKeepService(requireActivity());
                    } else {
                        // 設定変更失敗
                        Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            } else {
                Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false);
                Common.startKeepService(requireActivity());
            }
            return true;
        });

        preUtil.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new UtilFragment(), true, getString(R.string.pre_main_title_util));
            return false;
        });

        preOtherSettings.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new DeviceSettingsFragment(), true, getString(R.string.pre_main_title_other_settings));
            return false;
        });

        swEnableDchaService.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                if (Common.isShowCfmDialog(requireActivity())) {
                    // 確認ダイアログが必要
                    cfmDialog();
                    return false;
                }

                if (!Common.isFlagEnabled(requireActivity(), Constants.KEY_DEBUG_RESTRICTION) &&
                        !Common.isDchaActive(requireActivity())) {
                    // デバッグモードが無効かつdcha接続失敗
                    new DialogUtil(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
                Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, true);
                initPreference();
            } else {
                new DialogUtil(requireActivity())
                        .setMessage(R.string.dialog_disable_dcha_function)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            Common.setFlagEnabled(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false);
                            initPreference();
                        })
                        .setNegativeButton(R.string.dialog_common_cancel, null)
                        .show();
            }
            return false;
        });

        preDchaFunction.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new DchaFunctionFragment(), true, getString(R.string.pre_main_title_dcha_function));
            return false;
        });

        preDeviceOwnerFn.setOnPreferenceClickListener(preference -> {
            requireActivity().runOnUiThread(() ->
                    ((MainActivity) requireActivity()).transitionFragment(new DeviceOwnerFunctionFragment(), true, getString(R.string.pre_main_title_device_owner)));
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
            openGetAppList();
            return false;
        });

        preNotice.setOnPreferenceClickListener(preference -> {
            requireActivity().startActivity(new Intent(requireActivity(), NoticeActivity.class));
            requireActivity().overridePendingTransition(0, 0);
            return false;
        });

        swPreInstallUnknownSource.setOnPreferenceChangeListener((preference, o) -> {
            UserManager userManager = (UserManager) requireActivity().getSystemService(Context.USER_SERVICE);

            if (userManager == null || !Common.isDchaActive(requireActivity())) {
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error) + getString(R.string.dialog_error_user_manager))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
                return false;
            }

            boolean enabled = (boolean) o;

            if (setUnknownSourceRestriction(userManager, enabled)) {
                return true;
            }

            showLoadingDialog(getString(R.string.progress_state_processing));
            Common.acquireBypassPermission(requireActivity(), success -> {
                cancelLoadingDialog();

                if (!isAdded()) {
                    return;
                }

                if (success && setUnknownSourceRestriction(userManager, enabled)) {
                    return;
                }

                new DialogUtil(requireActivity())
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_error)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            });
            return false;
        });

        preRequestInstallPackages.setOnPreferenceClickListener(preference -> {
            showLoadingDialog(getString(R.string.progress_state_check_file));
            Context appContext = requireContext().getApplicationContext();
            AppExecutors.io(() -> {
                List<LaunchAppListView.AppData> dataList = loadRequestInstallPackagesApps(appContext);
                AppExecutors.main(() -> {
                    if (!isAdded()) {
                        cancelLoadingDialog();
                        return;
                    }
                    cancelLoadingDialog();
                    showRequestInstallPackagesDialog(dataList);
                });
            });
            return false;
        });

        preAppFunction.setOnPreferenceClickListener(preference -> {
            ((MainActivity) requireActivity()).transitionFragment(new AppFunctionFragment(), true, getString(R.string.pre_main_title_app_function_short));
            return false;
        });

        registerObservers();
        /* サービス起動(必要ないときは自動終了) */
        Common.startKeepService(requireActivity());
        /* 一括変更 */
        initPreference();
        requestNotice();
    }

    private void registerObservers() {
        if (observersRegistered) {
            return;
        }
        observersRegistered = true;

        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, dchaStateObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, navigationBarObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, marketObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, usbDebugObserver);
    }

    private void requestNotice() {
        Context context = getContext();
        File cacheDir = context == null ? null : context.getExternalCacheDir();

        if (cacheDir == null) {
            return;
        }
        new FileDownloadTask().execute(this, Constants.URL_CORE, new File(cacheDir, Constants.CORE_JSON), Constants.REQUEST_DOWNLOAD_NOTICE, Constants.JSON_TIMEOUT_MS, Constants.JSON_TIMEOUT_MS);
    }

    private void initPreference() {
        swDeviceAdmin.setChecked(((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), DeviceAdminReceiver.class)));
        swKeepNavigation.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false));
        swKeepUnkSrc.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_MARKET_APP, false));
        swKeepDchaState.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_DCHA_STATE, false));
        swKeepAdb.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false));

        // 各設定が未作成でも以降の項目へ影響しないよう、デフォルト値付きで個別に読む
        swDchaState.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE, Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE) != Constants.DIGICHALIZE_STATUS_UNDIGICHALIZE);
        swNavigation.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR, Constants.SETTING_OFF) != Constants.SETTING_OFF);
        swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, Constants.SETTING_OFF) != Constants.SETTING_OFF);
        swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, Constants.SETTING_OFF) != Constants.SETTING_OFF);

        if (((UserManager) requireActivity().getSystemService(Context.USER_SERVICE)).hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            swKeepUnkSrc.setVisible(false);
            swUnkSrc.setVisible(false);
            swPreInstallUnknownSource.setChecked(false);
            swPreInstallUnknownSource.setSummary(R.string.pre_main_sum_unknown_source_disallowed);
            preRequestInstallPackages.setEnabled(false);
            preRequestInstallPackages.setSummary(R.string.pre_main_sum_request_install_packages_disabled);
            preInstallUnknownSourceInfo.setEnabled(false);
        } else {
            swKeepUnkSrc.setVisible(true);
            swUnkSrc.setVisible(true);
            swPreInstallUnknownSource.setChecked(true);
            swPreInstallUnknownSource.setSummary(R.string.pre_main_sum_unknown_source_allowed);
            preRequestInstallPackages.setEnabled(true);
            preRequestInstallPackages.setSummary(R.string.pre_main_sum_request_install_packages);
            preInstallUnknownSourceInfo.setEnabled(true);
        }

        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
            swDeviceAdmin.setEnabled(false);
            swDeviceAdmin.setSummary(getString(R.string.pre_main_sum_already_device_owner));
        }

        if (Common.isFlagEnabled(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION)) {
            swEnableDchaService.setChecked(true);
            swEnableDchaService.setSummary(R.string.pre_main_sum_dcha_enabled);
            preDchaFunction.setEnabled(true);
            preDchaFunction.setSummary(R.string.pre_main_sum_dcha_function);
            preAppFunction.setEnabled(true);
            preAppFunction.setSummary(R.string.pre_main_sum_app_function);
        } else {
            swEnableDchaService.setChecked(false);
            swEnableDchaService.setSummary(R.string.pre_main_sum_dcha_disabled);
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

        if (Common.isCTX() || Common.isCTZ()) {
            swUnkSrc.setVisible(false);
            swKeepUnkSrc.setVisible(false);
        } else {
            swPreInstallUnknownSource.setVisible(false);
            preRequestInstallPackages.setVisible(false);
            preInstallUnknownSourceInfo.setVisible(false);
        }

        if (Common.isFlagEnabled(requireActivity(), Constants.KEY_FLAG_NORMAL_ENV)) {
            // 通常環境モード
            swKeepDchaState.setVisible(false);
            swKeepNavigation.setVisible(false);
            swKeepUnkSrc.setVisible(false);
            swKeepAdb.setVisible(false);
            catDcha.setVisible(false);
            swEnableDchaService.setVisible(false);
            preDchaFunction.setVisible(false);
            preAppFunction.setVisible(false);
        }
    }

    /* システムUIオブザーバー */
    final ContentObserver dchaStateObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!isAdded() || swDchaState == null) return;
            swDchaState.setChecked(BenesseExtension.getDchaState() != 0);
        }
    };

    /* ナビゲーションバーオブザーバー */
    final ContentObserver navigationBarObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!isAdded() || swNavigation == null) return;
            try {
                swNavigation.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* 提供元オブザーバー */
    final ContentObserver marketObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (!isAdded() || swUnkSrc == null) return;
            try {
                swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
            } catch (Settings.SettingNotFoundException ignored) {
            }
        }
    };

    /* USBデバッグオブザーバー */
    final ContentObserver usbDebugObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateAdbSwitch();
        }
    };

    /* USBデバッグの状態をUIへ反映 */
    private void updateAdbSwitch() {
        if (!isAdded() || swAdb == null) return;
        swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, Constants.SETTING_OFF) != Constants.SETTING_OFF);
    }

    private boolean adbEnabled(boolean bool) {
        try {
            if (bool) {
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, Constants.SETTING_ON);
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, Constants.SETTING_ON);
                if (Common.isDchaLockExists()) {
                    Settings.System.putInt(requireActivity().getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, Constants.SETTING_ON);
                }
            } else {
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, Constants.SETTING_OFF);
                if (Common.isDchaLockExists()) {
                    Settings.System.putInt(requireActivity().getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, Constants.SETTING_OFF);
                }
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        } finally {
            // 値が変わらない書き込みではContentObserverが発火しないため、UIはここで明示的に更新する
            updateAdbSwitch();
        }
    }

    private void showLoadingDialog(String message) {
        progressDialog = Common.showProgressSpinner(requireActivity(), message);
    }

    private void cancelLoadingDialog() {
        Common.cancelDialog(progressDialog);
    }

    @NonNull
    private Common.TaskUi installTaskUi() {
        return new Common.TaskUi() {
            @Override
            public Context context() {
                return isAdded() ? requireActivity() : null;
            }

            @Override
            public void showLoading() {
                showLoadingDialog(getString(R.string.progress_state_installing));
            }

            @Override
            public void cancelLoading() {
                cancelLoadingDialog();
            }
        };
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
        progressByteText = view.findViewById(R.id.progress_byte);
        dialogProgressBar = view.findViewById(R.id.progress);
        progressPercentText.setText(getString(R.string.progress_percent, 0));
        progressByteText.setText(R.string.progress_waiting);
        dialogProgressBar.setIndeterminate(true);
        dialogProgressBar.setMax(Constants.PERCENT_MAX);
        progressDialog = new DialogUtil(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.setMessage("");
        progressDialog.show();
    }

    /*
     * ダウンロード後にインストールできる状態かチェックする
     * 戻り値が false の場合、エラーダイアログを表示済みで、ダウンロードやインストールを続行できない
     */
    private boolean checkInstallAvailable(String downloadUrl) {
        switch (Common.getFlag(requireActivity(), Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE)) {
            case Constants.UPDATE_MODE_ADB:
                new DialogUtil(requireActivity())
                        .setCancelable(false)
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(getString(R.string.dialog_no_installer, downloadUrl))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
                return false;
            case Constants.UPDATE_MODE_DEVICE_OWNER:
                DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

                if (dpm == null || !dpm.isDeviceOwnerApp(requireActivity().getPackageName())) {
                    Common.setFlag(requireActivity(), Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE);
                    new DialogUtil(requireActivity())
                            .setCancelable(false)
                            .setMessage(requireActivity().getString(R.string.dialog_error_reset_installer))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
                return true;
            case Constants.UPDATE_MODE_DHIZUKU:
                if (!isDhizukuAllActive(requireActivity())) {
                    Common.setFlag(requireActivity(), Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE);
                    new DialogUtil(requireActivity())
                            .setCancelable(false)
                            .setMessage(requireActivity().getString(R.string.dialog_error_reset_installer))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
                return true;
            default:
                return true;
        }
    }

    @NonNull
    private DchaInstallTask.Listener dchaInstallTaskListener() {
        return Common.dchaInstallTaskListener(installTaskUi(), null);
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        if (reqCode == Constants.REQUEST_DOWNLOAD_APP_CHECK) {
            getAppRefreshing = false;
            return;
        }

        if (reqCode == Constants.REQUEST_DOWNLOAD_APP_LIST && !finishAppListDownload()) {
            return;
        }

        if (!isAdded() || getContext() == null) return;
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APP_LIST: {
                cancelLoadingDialog();
                ArrayList<GetAppListView.AppData> appDataArrayList = parseGetAppList(new File(requireActivity().getExternalCacheDir(), Constants.APP_LIST_JSON));

                if (appDataArrayList == null) {
                    appDataArrayList = new ArrayList<>();
                }
                showGetAppListDialog(appDataArrayList);
                break;
            }
            /* APKダウンロード要求の場合 */
            case Constants.REQUEST_DOWNLOAD_APK:
                cancelLoadingDialog();
                // ダウンロード完了後にインストールできる状態か再チェックする
                if (!checkInstallAvailable(downloadFileUrl)) {
                    return;
                }
                switch (Common.getFlag(requireActivity(), Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE)) {
                    case Constants.UPDATE_MODE_PACKAGE_INSTALLER:
                        requireActivity().startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(requireActivity().getExternalCacheDir(), "update.apk").getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case Constants.UPDATE_MODE_DCHA:
                        new DchaInstallTask().execute(requireActivity(), dchaInstallTaskListener(), new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath());
                        break;
                    case Constants.UPDATE_MODE_DEVICE_OWNER:
                    case Constants.UPDATE_MODE_DHIZUKU:
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), new ArrayList<>(List.of(new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_GET_APP, this);
                        break;
                }
                break;
            case Constants.REQUEST_DOWNLOAD_NOTICE:
                try {
                    JSONArray jsonArray = Common.getNotices(new File(requireActivity().getExternalCacheDir(), Constants.CORE_JSON));

                    if (jsonArray.length() == 0) {
                        // 表示しない
                        preNotice.setVisible(false);
                    } else {
                        preNotice.setTitle(R.string.activity_notice);
                        preNotice.setSummary(getString(R.string.pre_main_sum_notice, jsonArray.length()));
                    }
                } catch (JSONException | IOException | IllegalStateException ignored) {
                }
                break;
        }
    }

    private boolean setUnknownSourceRestriction(@NonNull UserManager userManager, boolean allowed) {
        try {
            userManager.setUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, !allowed);
            initPreference();
            return true;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    private void openGetAppList() {
        Context context = getContext();
        File cacheDir = context == null ? null : context.getExternalCacheDir();

        if (cacheDir == null) {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_error)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return;
        }

        File cacheFile = new File(cacheDir, Constants.APP_LIST_JSON);
        ArrayList<GetAppListView.AppData> appDataArrayList = parseGetAppList(cacheFile);

        if (appDataArrayList != null) {
            showGetAppListDialog(appDataArrayList);
            refreshGetAppList(cacheFile);
            return;
        }

        appListFinished = false;
        handler.removeCallbacks(appListTimeout);
        showLoadingDialog(getString(R.string.progress_state_connecting));
        handler.postDelayed(appListTimeout, APP_LIST_TIMEOUT_MS);
        new FileDownloadTask().execute(this, Constants.URL_APP_LIST, cacheFile, Constants.REQUEST_DOWNLOAD_APP_LIST, Constants.JSON_TIMEOUT_MS, Constants.JSON_TIMEOUT_MS);
    }

    private boolean finishAppListDownload() {
        if (appListFinished) {
            return false;
        }
        appListFinished = true;
        handler.removeCallbacks(appListTimeout);
        return true;
    }

    private void showDownloadError(int messageRes) {
        String message = getString(messageRes);
        String reason = FileDownloadTask.getLastError();

        if (!reason.isEmpty()) {
            message = message + "\n" + reason;
        }

        new DialogUtil(requireActivity())
                .setMessage(message)
                .setPositiveButton(R.string.dialog_common_ok, null)
                .show();
    }

    private void refreshGetAppList(@NonNull File cacheFile) {
        if (getAppRefreshing) {
            return;
        }
        getAppRefreshing = true;
        new FileDownloadTask().execute(this, Constants.URL_APP_LIST, cacheFile, Constants.REQUEST_DOWNLOAD_APP_CHECK, Constants.JSON_TIMEOUT_MS, Constants.JSON_TIMEOUT_MS);
    }

    @Nullable
    private ArrayList<GetAppListView.AppData> parseGetAppList(@NonNull File file) {
        if (!file.exists()) {
            return null;
        }

        try {
            JSONArray jsonArray = Common.parseJson(file).getJSONArray("appList");
            String model = getAppModelName();
            String ownPackage = requireContext().getPackageName();
            ArrayList<GetAppListView.AppData> appDataArrayList = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);

                if (ownPackage.equals(item.optString("packageName"))) {
                    continue;
                }

                JSONArray targetModels = item.optJSONArray("targetModel");

                if (targetModels == null) {
                    continue;
                }

                for (int s = 0; s < targetModels.length(); s++) {
                    String target = targetModels.optString(s, "");

                    if (target.isEmpty() || target.equals(model)) {
                        GetAppListView.AppData data = new GetAppListView.AppData();
                        data.name = item.optString("name");
                        data.description = item.optString("description");
                        data.url = item.optString("url");
                        appDataArrayList.add(data);
                        break;
                    }
                }
            }
            return appDataArrayList;
        } catch (JSONException | IOException ignored) {
            return null;
        }
    }

    @NonNull
    private String getAppModelName() {
        if (Common.isCT2()) {
            return "CT2";
        }

        if (Common.isCT3()) {
            return "CT3";
        }

        if (Common.isCTX()) {
            return "CTX";
        }

        if (Common.isCTZ()) {
            return "CTZ";
        }
        return "";
    }

    private void showGetAppListDialog(@NonNull ArrayList<GetAppListView.AppData> appDataArrayList) {
        View view = getLayoutInflater().inflate(R.layout.layout_app_list, null);
        ListView listView = view.findViewById(R.id.app_list);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(new GetAppListView.AppListAdapter(requireActivity(), appDataArrayList));
        listView.setOnItemClickListener((parent, view1, position, id) -> {
            Common.setFlag(requireActivity(), Constants.KEY_INT_GET_APP_TMP, position);
            listView.invalidateViews();
        });
        new DialogUtil(requireActivity())
                .setView(view)
                .setTitle(R.string.dialog_title_choose_app)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (appDataArrayList.isEmpty()) {
                        return;
                    }

                    int stored = Preferences.load(requireActivity(), Constants.KEY_INT_GET_APP_TMP, 0);
                    final int position = (stored >= 0 && stored < appDataArrayList.size()) ? stored : 0;
                    GetAppListView.AppData selected = appDataArrayList.get(position);
                    StringBuilder str = new StringBuilder();
                    str.append(getString(R.string.dialog_label_app_name))
                            .append("\n")
                            .append(selected.name)
                            .append("\n\n")
                            .append(getString(R.string.dialog_label_app_description))
                            .append("\n")
                            .append(selected.description)
                            .append("\n");
                    downloadFileUrl = selected.url;

                    if (str.toString().isEmpty()) {
                        return;
                    }

                    new DialogUtil(requireActivity())
                            .setMessage(str + "\n" + getString(R.string.dialog_confirm_ok))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                if (!Objects.equals(selected.url, "MYURL")) {
                                    if (checkInstallAvailable(selected.url)) {
                                        startDownload(selected.url);
                                        dialog.dismiss();
                                    }
                                } else {
                                    View view1 = getLayoutInflater().inflate(R.layout.view_app_url, null);
                                    AppCompatEditText editText = view1.findViewById(R.id.edit_app_url);
                                    new DialogUtil(requireActivity())
                                            .setMessage(R.string.dialog_error_invalid_url)
                                            .setView(view1)
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog3, which3) -> {
                                                ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                                                downloadFileUrl = editText.getText().toString();
                                                if (checkInstallAvailable(downloadFileUrl)) {
                                                    startDownload(downloadFileUrl);
                                                }
                                            })
                                            .setNegativeButton(R.string.dialog_common_cancel, null)
                                            .show();
                                }
                            })
                            .show();
                })
                .show();
    }

    @Override
    public void onDownloadError(int reqCode) {
        if (reqCode == Constants.REQUEST_DOWNLOAD_APP_CHECK) {
            getAppRefreshing = false;
            return;
        }

        if (reqCode == Constants.REQUEST_DOWNLOAD_APP_LIST && !finishAppListDownload()) {
            return;
        }

        if (!isAdded() || getContext() == null) return;
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APK,
                 Constants.REQUEST_DOWNLOAD_APP_LIST -> {
                cancelLoadingDialog();
                showDownloadError(R.string.dialog_error_download);
            }
            case Constants.REQUEST_DOWNLOAD_NOTICE -> preNotice.setVisible(false);
        }
    }

    @Override
    public void onConnectionError(int reqCode) {
        if (reqCode == Constants.REQUEST_DOWNLOAD_APP_CHECK) {
            getAppRefreshing = false;
            return;
        }

        if (reqCode == Constants.REQUEST_DOWNLOAD_APP_LIST && !finishAppListDownload()) {
            return;
        }

        if (!isAdded() || getContext() == null) return;
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APK,
                 Constants.REQUEST_DOWNLOAD_APP_LIST -> {
                cancelLoadingDialog();
                showDownloadError(R.string.dialog_error_connection);
            }
            case Constants.REQUEST_DOWNLOAD_NOTICE -> preNotice.setVisible(false);
        }
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
        if (!isAdded() || progressPercentText == null || progressByteText == null || dialogProgressBar == null || progressDialog == null) return;
        if (totalByte > 0 && dialogProgressBar.isIndeterminate()) {
            dialogProgressBar.setIndeterminate(false);
        }
        progressPercentText.setText(getString(R.string.progress_percent, progress));
        if (totalByte > 0) {
            progressByteText.setText(getString(R.string.progress_byte, currentByte, totalByte));
        }
        dialogProgressBar.setProgress(progress);
        progressDialog.setMessage(getString(R.string.progress_state_download_file));
    }

    @Override
    public void onInstallSuccess(int reqCode) {
        if (!isAdded() || getContext() == null) return;
        apkInstallTaskListener().onSuccess();
    }

    @Override
    public void onInstallFailure(int reqCode, String message) {
        if (!isAdded() || getContext() == null) return;
        apkInstallTaskListener().onFailure(message);
    }

    @Override
    public void onInstallError(int reqCode, String message) {
        if (!isAdded() || getContext() == null) return;
        apkInstallTaskListener().onError(message);
    }

    private void grantRequestInstallPackages(String packName) {
        showLoadingDialog(getString(R.string.progress_state_installing));
        AppExecutors.io(() -> {
            String result = execRequestInstallPackages(packName);
            AppExecutors.main(() -> {
                if (!isAdded()) {
                    cancelLoadingDialog();
                    return;
                }

                if (isSecurityError(result)) {
                    Common.acquireBypassPermission(requireActivity(), success -> {
                        cancelLoadingDialog();

                        if (!isAdded()) {
                            return;
                        }

                        if (!success) {
                            new DialogUtil(requireActivity())
                                    .setTitle(getString(R.string.dialog_title_error))
                                    .setMessage(R.string.dialog_error)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        showLoadingDialog(getString(R.string.progress_state_installing));
                        AppExecutors.io(() -> {
                            String retryResult = execRequestInstallPackages(packName);
                            AppExecutors.main(() -> {
                                if (!isAdded()) {
                                    cancelLoadingDialog();
                                    return;
                                }
                                cancelLoadingDialog();
                                showRequestInstallPackagesResult(retryResult);
                            });
                        });
                    });
                } else {
                    cancelLoadingDialog();
                    showRequestInstallPackagesResult(result);
                }
            });
        });
    }

    private boolean isSecurityError(@NonNull String result) {
        return result.contains("Security exception") || result.contains("SecurityException");
    }

    @NonNull
    private List<LaunchAppListView.AppData> loadRequestInstallPackagesApps(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<PackageInfo> installedAppList = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        List<LaunchAppListView.AppData> dataList = new ArrayList<>();

        for (PackageInfo info : installedAppList) {
            if (info.applicationInfo == null || info.requestedPermissions == null) {
                continue;
            }

            boolean isRequestInstallPackages = false;

            for (String permission : info.requestedPermissions) {
                if (Manifest.permission.REQUEST_INSTALL_PACKAGES.equals(permission)) {
                    isRequestInstallPackages = true;
                    break;
                }
            }

            if (!isRequestInstallPackages) {
                continue;
            }

            LaunchAppListView.AppData data = new LaunchAppListView.AppData();
            data.icon = info.applicationInfo.loadIcon(packageManager);
            data.label = info.applicationInfo.loadLabel(packageManager).toString();
            data.packName = info.packageName;
            dataList.add(data);
        }
        return dataList;
    }

    private void showRequestInstallPackagesDialog(@NonNull List<LaunchAppListView.AppData> dataList) {
        View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_launch_app_list, null);
        ListView listView = view.findViewById(R.id.launch_app_list);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(new LaunchAppListView.LaunchAppAdapter(requireActivity(), dataList));
        listView.setOnItemClickListener((parent, mView, position, id) ->
                grantRequestInstallPackages(dataList.get(position).packName));

        new DialogUtil(requireActivity())
                .setView(view)
                .setTitle(R.string.dialog_title_select_app)
                .setPositiveButton(R.string.dialog_common_cancel, null)
                .show();
    }

    private void showRequestInstallPackagesResult(@NonNull String result) {
        if (result.isEmpty()) {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_allow_install_unknown_source)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
        } else {
            new DialogUtil(requireActivity())
                    .setTitle(getString(R.string.dialog_title_error))
                    .setMessage(result)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
        }
    }

    @NonNull
    private String execRequestInstallPackages(String packName) {
        ArrayList<String> stringArrayList = Common.exec("appops set " + packName + " REQUEST_INSTALL_PACKAGES allow");
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < stringArrayList.size(); i++) {
            stringBuilder.append(stringArrayList.get(i));
        }

        return stringBuilder.toString();
    }

    @NonNull
    private ApkInstallTask.Listener apkInstallTaskListener() {
        return Common.apkInstallTaskListener(installTaskUi(), null);
    }
}
