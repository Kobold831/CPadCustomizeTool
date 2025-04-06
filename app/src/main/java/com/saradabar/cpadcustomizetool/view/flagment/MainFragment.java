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

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;

import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.data.service.ProtectKeepService;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.data.task.IDchaTask;
import com.saradabar.cpadcustomizetool.data.task.ResolutionTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DchaServiceUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.EditAdminActivity;
import com.saradabar.cpadcustomizetool.view.activity.EmergencyActivity;
import com.saradabar.cpadcustomizetool.view.activity.NormalActivity;
import com.saradabar.cpadcustomizetool.view.activity.NoticeActivity;
import com.saradabar.cpadcustomizetool.view.activity.RebootActivity;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;
import com.saradabar.cpadcustomizetool.view.views.GetAppListView;
import com.saradabar.cpadcustomizetool.view.views.HomeAppListView;
import com.saradabar.cpadcustomizetool.view.views.NormalModeHomeAppListView;

import org.json.JSONArray;
import org.json.JSONObject;

import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

/** @noinspection deprecation*/
public class MainFragment extends PreferenceFragmentCompat implements DownloadEventListener, InstallEventListener {

    AlertDialog progressDialog;
    AppCompatTextView progressPercentText;
    AppCompatTextView progressByteText;
    ProgressBar dialogProgressBar;

    String downloadFileUrl;

    IDchaService mDchaService;

    SwitchPreferenceCompat swDchaState,
            swKeepDchaState,
            swNavigation,
            swKeepNavigation,
            swUnkSrc,
            swKeepUnkSrc,
            swAdb,
            swKeepAdb,
            swBypassAdbDisable,
            swKeepLauncher,
            swDeviceAdmin;

    private Preference preEnableDchaService,
            preEmgManual,
            preEmgExecute,
            preEmgShortcut,
            preSelNorLauncher,
            preNorManual,
            preNorExecute,
            preNorShortcut,
            preOtherSettings,
            preReboot,
            preRebootShortcut,
            preSilentInstall,
            preLauncher,
            preResolution,
            preResetResolution,
            preDeviceOwnerFn,
            preEditAdmin,
            preDhizukuPermissionReq,
            preSystemUpdate,
            preGetApp,
            preNotice;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pre_main);

        swDchaState = findPreference("pre_dcha_state");
        swKeepDchaState = findPreference("pre_keep_dcha_state");
        swNavigation = findPreference("pre_navigation");
        swKeepNavigation = findPreference("pre_keep_navigation");
        swUnkSrc = findPreference("pre_unk_src");
        swKeepUnkSrc = findPreference("pre_keep_unk_src");
        swAdb = findPreference("pre_adb");
        swKeepAdb = findPreference("pre_keep_adb");
        swBypassAdbDisable = findPreference("pre_app_adb");
        preLauncher = findPreference("pre_launcher");
        swKeepLauncher = findPreference("pre_keep_launcher");
        preOtherSettings = findPreference("pre_other_settings");
        preEnableDchaService = findPreference("pre_enable_dcha_service");
        preEmgManual = findPreference("pre_emg_manual");
        preEmgExecute = findPreference("pre_emg_execute");
        preEmgShortcut = findPreference("pre_emg_shortcut");
        preSelNorLauncher = findPreference("pre_sel_nor_launcher");
        preNorManual = findPreference("pre_nor_manual");
        preNorExecute = findPreference("pre_nor_execute");
        preNorShortcut = findPreference("pre_nor_shortcut");
        preReboot = findPreference("pre_reboot");
        preRebootShortcut = findPreference("pre_reboot_shortcut");
        preSilentInstall = findPreference("pre_silent_install");
        preResolution = findPreference("pre_resolution");
        preResetResolution = findPreference("pre_reset_resolution");
        preSystemUpdate = findPreference("pre_system_update");
        preDeviceOwnerFn = findPreference("pre_device_owner_fn");
        preEditAdmin = findPreference("pre_edit_admin");
        preDhizukuPermissionReq = findPreference("pre_dhizuku_permission_req");
        swDeviceAdmin = findPreference("pre_device_admin");
        preGetApp = findPreference("pre_get_app");
        preNotice = findPreference("pre_notice");

        /* 初期化 */
        initialize();
    }

    /* アクティビティ破棄 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dchaStateObserver != null) {
            requireActivity().getContentResolver().unregisterContentObserver(dchaStateObserver);
        }

        if (navigationBarObserver != null) {
            requireActivity().getContentResolver().unregisterContentObserver(navigationBarObserver);
        }

        if (marketObserver != null) {
            requireActivity().getContentResolver().unregisterContentObserver(marketObserver);
        }

        if (usbDebugObserver != null) {
            requireActivity().getContentResolver().unregisterContentObserver(usbDebugObserver);
        }
    }

    /* 再表示 */
    @Override
    public void onStart() {
        super.onStart();
        if (Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
            View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
            AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
            textView.setText(R.string.dialog_service_connecting);
            AlertDialog waitForServiceDialog = new AlertDialog.Builder(requireActivity()).setCancelable(false).setView(view).create();
            waitForServiceDialog.show();
            new IDchaTask().execute(requireActivity(), new IDchaTask.Listener() {
                @Override
                public void onSuccess(IDchaService iDchaService) {
                    mDchaService = iDchaService;

                    if (waitForServiceDialog.isShowing()) {
                        try {
                            waitForServiceDialog.cancel();
                        } catch (IllegalStateException | IllegalArgumentException ignored) {
                        }
                    }

                    if (mDchaService == null) {
                        if (!Preferences.load(requireActivity(), "debug_restriction", false)) {
                            new AlertDialog.Builder(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(R.string.dialog_dcha_failed)
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                        Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false);
                                        requireActivity().finish();
                                        requireActivity().overridePendingTransition(0, 0);
                                        startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                                    })
                                    .show();
                        }
                    }

                    try {
                        setListener();
                    } catch (IllegalStateException | IllegalArgumentException ignored) {
                    }
                }

                @Override
                public void onFailure() {
                    if (waitForServiceDialog.isShowing()) {
                        waitForServiceDialog.cancel();
                    }

                    if (!Preferences.load(requireActivity(), "debug_restriction", false)) {
                        new AlertDialog.Builder(requireActivity())
                                .setCancelable(false)
                                .setMessage(R.string.dialog_dcha_failed)
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                    Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false);
                                    requireActivity().finish();
                                    requireActivity().overridePendingTransition(0, 0);
                                    startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                                })
                                .show();
                    } else {
                        // debug mode
                        try {
                            setListener();
                        } catch (IllegalStateException | IllegalArgumentException ignored) {
                        }
                    }
                }
            });
        } else {
            setListener();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ACTIVITY_INSTALL:
                preSilentInstall.setEnabled(true);

                if (data == null) {
                    return;
                }

                String installData = Common.getFilePath(requireActivity(), data.getData());

                if (installData != null) {
                    new DchaInstallTask().execute(requireActivity(), dchaInstallTaskListener(), installData);
                    return;
                } else {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                }
                break;
            case Constants.REQUEST_ACTIVITY_SYSTEM_UPDATE:
                preSystemUpdate.setEnabled(true);

                if (data == null) {
                    return;
                }

                String updateData = Common.getFilePath(requireActivity(), data.getData());

                if (updateData != null) {
                    if (!new DchaServiceUtil(requireActivity(), mDchaService).execSystemUpdate(updateData, 0)) {
                        new AlertDialog.Builder(requireActivity())
                                .setMessage(R.string.dialog_error)
                                .setPositiveButton(R.string.dialog_common_ok, null)
                                .show();
                    }
                } else {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                            .show();
                }
                break;
        }
    }

    private void setListener() {
        swDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus((boolean) o ? 3 : 0);
            return false;
        });

        swKeepDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_DCHA_STATE, (boolean) o);

            if ((boolean) o) {
                new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
            }

            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swNavigation.setOnPreferenceChangeListener((preference, o) -> {
            new DchaServiceUtil(requireActivity(), mDchaService).hideNavigationBar((boolean) o);
            return false;
        });

        swKeepNavigation.setOnPreferenceChangeListener((preference, o) -> {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, (boolean) o);

            if ((boolean) o) {
                new DchaServiceUtil(requireActivity(), mDchaService).hideNavigationBar(false);
            }

            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            try {
                chgSetting((Boolean) o ? Constants.FLAG_MARKET_APP_TRUE : Constants.FLAG_MARKET_APP_FALSE);
            } catch (Exception ignored) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        swKeepUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            try {
                if ((boolean) o) {
                    chgSetting(Constants.FLAG_MARKET_APP_TRUE);
                }
            } catch (Exception ignored) {
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

            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                try {
                    if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(3);
                        Thread.sleep(100);
                    }

                    chgSetting(Constants.FLAG_USB_DEBUG_TRUE);

                    if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                    }
                } catch (Exception ignored) {
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();

                    if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                    }
                }
            } else {
                try {
                    chgSetting(Constants.FLAG_USB_DEBUG_FALSE);
                } catch (Exception ignored) {
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

            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                try {
                    if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(3);
                        Thread.sleep(100);
                    }

                    chgSetting(Constants.FLAG_USB_DEBUG_TRUE);

                    if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                    }
                } catch (Exception ignored) {
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();

                    if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                    }
                    return false;
                }
            }

            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swBypassAdbDisable.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            try {
                if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                        Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                    new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(3);
                    Thread.sleep(100);
                }

                chgSetting(Constants.FLAG_USB_DEBUG_TRUE);

                if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                        Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                    new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                }
            } catch (Exception ignored) {
                Toast.makeText(requireActivity(), R.string.toast_no_permission, Toast.LENGTH_SHORT).show();

                if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                        Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                    new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                }
                return false;
            }

            Preferences.save(requireActivity(), Constants.KEY_FLAG_AUTO_USB_DEBUG, (boolean) o);
            return true;
        });

        preLauncher.setOnPreferenceClickListener(preference -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_launcher_list, null);
            List<ResolveInfo> installedAppList = requireActivity().getPackageManager().queryIntentActivities(new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
            List<HomeAppListView.AppData> dataList = new ArrayList<>();

            for (ResolveInfo resolveInfo : installedAppList) {
                HomeAppListView.AppData data = new HomeAppListView.AppData();
                data.label = resolveInfo.loadLabel(requireActivity().getPackageManager()).toString();
                data.icon = resolveInfo.loadIcon(requireActivity().getPackageManager());
                data.packName = resolveInfo.activityInfo.packageName;
                dataList.add(data);
            }

            ListView listView = view.findViewById(R.id.launcher_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new HomeAppListView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                new DchaServiceUtil(requireActivity(), mDchaService)
                        .setPreferredHomeApp(getLauncherPackage(requireActivity()),
                                Uri.fromParts("package",
                                                installedAppList.get(position).activityInfo.packageName, null)
                                        .toString().replace("package:", ""));
                listView.invalidateViews();
                initialize();
            });

            new AlertDialog.Builder(requireActivity())
                    .setView(view)
                    .setTitle(R.string.dialog_title_launcher)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        swKeepLauncher.setOnPreferenceChangeListener((preference, o) -> {
            Preferences.save(requireActivity(), Constants.KEY_FLAG_KEEP_HOME, (boolean) o);

            if ((boolean) o) {
                Preferences.save(requireActivity(), Constants.KEY_STRINGS_KEEP_HOME_APP_PACKAGE, getLauncherPackage(requireActivity()));
            }

            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        preOtherSettings.setOnPreferenceClickListener(preference -> {
            StartActivity startActivity = (StartActivity) requireActivity();
            startActivity.transitionFragment(new OtherFragment(), true);
            return false;
        });

        preEnableDchaService.setOnPreferenceClickListener(preference -> {
            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.dialog_question_dcha)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        if (!Preferences.load(requireActivity(), "debug_restriction", false) && !tryBindDchaService()) {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(R.string.dialog_error_no_dcha)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        } else {
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, true);
                            requireActivity().finish();
                            requireActivity().overridePendingTransition(0, 0);
                            startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preEmgManual.setOnPreferenceClickListener(preference -> {
            AppCompatTextView textView = new AppCompatTextView(requireActivity());
            textView.setText(R.string.dialog_emergency_manual_red);
            textView.setTextSize(16);
            textView.setTextColor(Color.RED);
            textView.setPadding(32, 0, 32, 0);
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.dialog_title_emergency_manual)
                    .setMessage(R.string.dialog_emergency_manual)
                    .setView(textView)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        preEmgExecute.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.note_start_emergency_mode)
                    .setNeutralButton(R.string.dialog_common_yes, (dialog, which) -> startActivity(new Intent(requireActivity(), EmergencyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                    .setPositiveButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preEmgShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.activity_emergency))
                        .setShortLabel(getString(R.string.activity_emergency))
                        .setIcon(Icon.createWithResource(requireActivity(), android.R.drawable.ic_dialog_alert))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), EmergencyActivity.class.getName()))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent(Constants.ACTION_INSTALL_SHORTCUT).putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), EmergencyActivity.class.getName()))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), android.R.drawable.ic_dialog_alert))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_emergency));
                Toast.makeText(requireActivity(), R.string.toast_success, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preSelNorLauncher.setOnPreferenceClickListener(preference -> {
            View view = requireActivity().getLayoutInflater().inflate(R.layout.layout_normal_launcher_list, null);
            List<ResolveInfo> installedAppList = requireActivity().getPackageManager().queryIntentActivities(new Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
            List<NormalModeHomeAppListView.AppData> dataList = new ArrayList<>();

            for (ResolveInfo resolveInfo : installedAppList) {
                NormalModeHomeAppListView.AppData data = new NormalModeHomeAppListView.AppData();
                data.label = resolveInfo.loadLabel(requireActivity().getPackageManager()).toString();
                data.icon = resolveInfo.loadIcon(requireActivity().getPackageManager());
                data.packName = resolveInfo.activityInfo.packageName;
                dataList.add(data);
            }

            ListView listView = view.findViewById(R.id.normal_launcher_list);
            listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            listView.setAdapter(new NormalModeHomeAppListView.AppListAdapter(requireActivity(), dataList));
            listView.setOnItemClickListener((parent, mView, position, id) -> {
                Preferences.save(requireActivity(), Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, Uri.fromParts("package", installedAppList.get(position).activityInfo.packageName, null).toString().replace("package:", ""));
                /* listviewの更新 */
                listView.invalidateViews();
                initialize();
            });

            new AlertDialog.Builder(requireActivity())
                    .setView(view)
                    .setTitle(R.string.dialog_title_launcher)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        preNorManual.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.dialog_title_normal_manual)
                    .setMessage(R.string.dialog_normal_manual)
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        preNorExecute.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.note_start_normal_mode)
                    .setNeutralButton(R.string.dialog_common_yes, (dialog, which) -> startActivity(new Intent(requireActivity(), NormalActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                    .setPositiveButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preNorShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.activity_normal))
                        .setShortLabel(getString(R.string.activity_normal))
                        .setIcon(Icon.createWithResource(requireActivity(), android.R.drawable.ic_menu_revert))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), NormalActivity.class.getName()))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent(Constants.ACTION_INSTALL_SHORTCUT).putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), NormalActivity.class.getName()))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), android.R.drawable.ic_menu_revert))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_normal));
                Toast.makeText(requireActivity(), R.string.toast_success, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preReboot.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.dialog_question_reboot)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        try {
                            mDchaService.rebootPad(0, "");
                        } catch (Exception ignored) {
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preRebootShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.reboot))
                        .setShortLabel(getString(R.string.reboot))
                        .setIcon(Icon.createWithResource(requireActivity(), android.R.drawable.ic_popup_sync))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), RebootActivity.class.getName()))
                        .build(), null);
            } else {
                makeRebootShortcut();
            }
            return false;
        });

        preSilentInstall.setOnPreferenceClickListener(preference -> {
            try {
                preSilentInstall.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/vnd.android.package-archive").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), ""), Constants.REQUEST_ACTIVITY_INSTALL);
            } catch (Exception ignored) {
                preSilentInstall.setEnabled(true);
                new AlertDialog.Builder(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            return false;
        });

        preResolution.setOnPreferenceClickListener(preference -> {
            /* DchaUtilServiceが機能しているか */
            if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CT2 ||
                    Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CT3) {
                if (!tryBindDchaUtilService()) {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha_util)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
            }

            View view = requireActivity().getLayoutInflater().inflate(R.layout.view_resolution, null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    LinearLayout linearLayoutAny = view.findViewById(R.id.v_resolution_linearlayout_any);
                    LinearLayout linearLayoutBenesse = view.findViewById(R.id.v_resolution_linearlayout_benesse);
                    linearLayoutAny.setVisibility(View.GONE);
                    linearLayoutBenesse.setVisibility(View.VISIBLE);
                    new AlertDialog.Builder(requireActivity())
                            .setView(view)
                            .setCancelable(false)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                AppCompatRadioButton rb1024 = view.findViewById(R.id.v_resolution_radio_1024);
                                AppCompatRadioButton rb1280 = view.findViewById(R.id.v_resolution_radio_1280);
                                AppCompatRadioButton rb1920 = view.findViewById(R.id.v_resolution_radio_1920);

                                int width, height;

                                if (rb1024.isChecked()) {
                                    width = 1024;
                                    height = 768;
                                } else if (rb1280.isChecked()) {
                                    width = 1280;
                                    height = 800;
                                } else if (rb1920.isChecked()) {
                                    width = 1920;
                                    height = 1200;
                                } else {
                                    Toast.makeText(requireActivity(), R.string.toast_not_selected, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                StartActivity startActivity = (StartActivity) requireActivity();
                                new ResolutionTask().execute(requireActivity(), startActivity.resolutionTaskListener(), width, height);
                            })
                            .setNegativeButton(R.string.dialog_common_cancel, null)
                            .show();
                    return false;
                }
            }

            new AlertDialog.Builder(requireActivity())
                    .setView(view)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_resolution)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
                        AppCompatEditText editTextWidth = view.findViewById(R.id.edit_text_1);
                        AppCompatEditText editTextHeight = view.findViewById(R.id.edit_text_2);

                        try {
                            //noinspection DataFlowIssue
                            int width = Integer.parseInt(editTextWidth.getText().toString());
                            //noinspection DataFlowIssue
                            int height = Integer.parseInt(editTextHeight.getText().toString());

                            if (width < 0 || height < 0) {
                                new AlertDialog.Builder(requireActivity())
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_error_illegal_value)
                                        .setPositiveButton(R.string.dialog_common_ok, null)
                                        .show();
                            } else {
                                StartActivity startActivity = (StartActivity) requireActivity();
                                new ResolutionTask().execute(requireActivity(), startActivity.resolutionTaskListener(), width, height);
                            }
                        } catch (NumberFormatException ignored) {
                            new AlertDialog.Builder(requireActivity())
                                    .setTitle(R.string.dialog_title_error)
                                    .setMessage(R.string.dialog_error_illegal_value)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preResetResolution.setOnPreferenceClickListener(preference -> {
            /* DchaUtilService が機能しているか */
            if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CT2 || Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CT3) {
                if (!tryBindDchaUtilService()) {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(R.string.dialog_error_no_dcha_util)
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                    return false;
                }
            }

            StartActivity startActivity = (StartActivity) requireActivity();
            startActivity.resetResolution();
            return false;
        });

        preSystemUpdate.setOnPreferenceClickListener(preference -> {
            preSystemUpdate.setEnabled(false);

            try {
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false), ""), Constants.REQUEST_ACTIVITY_SYSTEM_UPDATE);
            } catch (ActivityNotFoundException ignored) {
                preSystemUpdate.setEnabled(true);
                new AlertDialog.Builder(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
            return false;
        });

        preDeviceOwnerFn.setOnPreferenceClickListener(preference -> {
            requireActivity().runOnUiThread(() -> {
                StartActivity startActivity = (StartActivity) requireActivity();
                startActivity.transitionFragment(new DeviceOwnerFragment(), true);
            });
            return false;
        });

        preEditAdmin.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), EditAdminActivity.class));
            return false;
        });

        preDhizukuPermissionReq.setOnPreferenceClickListener(preference -> {
            if (!Dhizuku.init(requireActivity())) {
                new AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.dialog_error_no_dhizuku)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
                return false;
            }

            if (!Dhizuku.isPermissionGranted()) {
                Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                    @Override
                    public void onRequestPermission(int grantResult) {
                        requireActivity().runOnUiThread(() -> {
                            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                new AlertDialog.Builder(requireActivity())
                                        .setMessage(R.string.dialog_dhizuku_grant_permission)
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                        .show();
                            } else {
                                new AlertDialog.Builder(requireActivity())
                                        .setMessage(R.string.dialog_dhizuku_deny_permission)
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                        .show();
                            }
                        });
                    }
                });
            } else {
                new AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.dialog_dhizuku_already_grant_permission)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
            return false;
        });

        swDeviceAdmin.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                if (!((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), AdministratorReceiver.class))) {
                    startActivityForResult(
                            new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(requireActivity(), AdministratorReceiver.class))
                                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.device_admin_detail)
                            , Constants.REQUEST_ACTIVITY_ADMIN);
                }
            } else {
                swDeviceAdmin.setChecked(true);
                new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.dialog_disable_device_admin)
                        .setMessage(R.string.dialog_question_admin)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            ((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).removeActiveAdmin(new ComponentName(requireActivity(), AdministratorReceiver.class));
                            swDeviceAdmin.setChecked(false);
                        })

                        .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> {
                            swDeviceAdmin.setChecked(true);
                            dialog.dismiss();
                        })
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
            startActivity(new Intent(requireActivity(), NoticeActivity.class));
            return false;
        });

        /* 一括変更 */
        initialize();
    }

    /* 再起動ショートカットを作成 */
    private void makeRebootShortcut() {
        requireActivity().sendBroadcast(new Intent(Constants.ACTION_INSTALL_SHORTCUT).putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), RebootActivity.class.getName()))
                .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), android.R.drawable.ic_popup_sync))
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_reboot));
        Toast.makeText(requireActivity(), R.string.toast_success, Toast.LENGTH_SHORT).show();
    }

    /* 初期化 */
    public void initialize() {
        if (getPreferenceScreen() != null) {
            /* DchaServiceを使用するか */
            if (!Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
                for (Object preference : Arrays.asList(
                        findPreference("pre_silent_install"),
                        findPreference("pre_launcher"),
                        findPreference("pre_keep_launcher"),
                        findPreference("category_emergency"),
                        findPreference("category_normal"),
                        findPreference("pre_reboot"),
                        findPreference("pre_reboot_shortcut"),
                        findPreference("pre_resolution"),
                        findPreference("pre_reset_resolution"),
                        findPreference("pre_system_update")
                )) {
                    if (preference != null) {
                        getPreferenceScreen().removePreference((Preference) preference);
                    }
                }
            } else {
                getPreferenceScreen().removePreference(preEnableDchaService);
            }
        }

        /* オブサーバーを有効化 */
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, dchaStateObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, navigationBarObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, marketObserver);
        requireActivity().getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, usbDebugObserver);

        try {
            swDchaState.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE) != 0);
        } catch (Exception ignored) {
        }

        try {
            swNavigation.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != 0);
        } catch (Exception ignored) {
        }

        try {
            //noinspection deprecation
            swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
        } catch (Exception ignored) {
        }

        try {
            swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
        } catch (Exception ignored) {
        }

        swDeviceAdmin.setChecked(((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), AdministratorReceiver.class)));
        swKeepNavigation.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_NAVIGATION_BAR, false));
        swKeepUnkSrc.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_MARKET_APP, false));
        swKeepDchaState.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_DCHA_STATE, false));
        swKeepAdb.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_USB_DEBUG, false));
        swKeepLauncher.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_KEEP_HOME, false));
        preLauncher.setSummary(getLauncherName(requireActivity()));

        //String normalLauncherName = "com.android.launcher" + (Build.VERSION.SDK_INT == 22 ? "2" : "3");
        String normalLauncherName = null;

        try {
            normalLauncherName = (String) requireActivity().getPackageManager().getApplicationLabel(requireActivity().getPackageManager().getApplicationInfo(Preferences.load(requireActivity(), Constants.KEY_STRINGS_NORMAL_LAUNCHER_APP_PACKAGE, ""), 0));
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        preSelNorLauncher.setSummary(getString(normalLauncherName == null ? R.string.pre_main_sum_no_setting_launcher : R.string.pre_main_sum_message_2, normalLauncherName));

        /* 維持スイッチが有効のときサービスが停止していたら起動 */
        requireActivity().startService(new Intent(requireActivity(), KeepService.class));
        requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));

        /* 端末ごとにPreferenceの状態を設定 */
        switch (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2)) {
            case Constants.MODEL_CT2:
                try {
                    if (requireActivity().getPackageManager().getPackageInfo(Constants.PKG_DCHA_SERVICE, 0).versionCode < 5) {
                        preSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                        preSilentInstall.setEnabled(false);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
                break;
            case Constants.MODEL_CT3:
                break;
            case Constants.MODEL_CTX:
            case Constants.MODEL_CTZ:
                swKeepUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swKeepUnkSrc.setEnabled(false);
                swUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swUnkSrc.setEnabled(false);
                break;
        }

        if (!requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            preDeviceOwnerFn.setEnabled(false);
            preDeviceOwnerFn.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
            preDhizukuPermissionReq.setVisible(false);
            swDeviceAdmin.setVisible(false);
        }

        if (((UserManager) requireActivity().getSystemService(Context.USER_SERVICE)).hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            swKeepUnkSrc.setVisible(false);
            swUnkSrc.setVisible(false);
        }

        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
            swDeviceAdmin.setEnabled(false);
            swDeviceAdmin.setSummary(getString(R.string.pre_main_sum_already_device_owner));
            preDhizukuPermissionReq.setEnabled(false);
            preDhizukuPermissionReq.setSummary(getString(R.string.pre_main_sum_already_device_owner));
        }

        swBypassAdbDisable.setChecked(Preferences.load(requireActivity(), Constants.KEY_FLAG_AUTO_USB_DEBUG, false));

        if (!Common.isBenesseExtensionExist()) {
            swBypassAdbDisable.setEnabled(false);
            swBypassAdbDisable.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
        }

        new FileDownloadTask().execute(this, Constants.URL_NOTICE, new File(requireActivity().getExternalCacheDir(), Constants.NOTICE_JSON), Constants.REQUEST_DOWNLOAD_NOTICE);
    }

    /* システムUIオブザーバー */
    ContentObserver dchaStateObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swDchaState.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.DCHA_STATE) != 0);
            } catch (Exception ignored) {
            }
        }
    };

    /* ナビゲーションバーオブザーバー */
    ContentObserver navigationBarObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swNavigation.setChecked(Settings.System.getInt(requireActivity().getContentResolver(), Constants.HIDE_NAVIGATION_BAR) != 0);
            } catch (Exception ignored) {
            }
        }
    };

    /* 提供元オブザーバー */
    ContentObserver marketObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swUnkSrc.setChecked(Settings.Secure.getInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 0);
            } catch (Exception ignored) {
            }
        }
    };

    /* USBデバッグオブザーバー */
    ContentObserver usbDebugObserver = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                swAdb.setChecked(Settings.Global.getInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED) != 0);
            } catch (Exception ignored) {
            }
        }
    };

    /* 設定変更 */
    private void chgSetting(int req) {
        switch (req) {
            case Constants.FLAG_USB_DEBUG_TRUE:
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                Settings.System.putInt(requireActivity().getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 1);
                break;
            case Constants.FLAG_USB_DEBUG_FALSE:
                Settings.Global.putInt(requireActivity().getContentResolver(), Settings.Global.ADB_ENABLED, 0);
                Settings.System.putInt(requireActivity().getContentResolver(), Constants.BC_PASSWORD_HIT_FLAG, 0);
                break;
            case Constants.FLAG_MARKET_APP_TRUE:
                Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);
                break;
            case Constants.FLAG_MARKET_APP_FALSE:
                Settings.Secure.putInt(requireActivity().getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0);
                break;
        }
    }

    public void showLoadingDialog(String message) {
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(message);
        progressDialog = new AlertDialog.Builder(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.show();
    }

    public void cancelLoadingDialog() {
        if (progressDialog == null) {
            return;
        }

        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    private void cfmDialog() {
        new AlertDialog.Builder(requireActivity())
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_dcha_warning))
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_FUNCTION_CONFIRMATION, true))
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
        progressDialog = new AlertDialog.Builder(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.setMessage("");
        progressDialog.show();
    }

    /* ランチャーのパッケージ名を取得 */
    @Nullable
    private String getLauncherPackage(@NonNull Context context) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return null;
    }

    /* ランチャーのアプリ名を取得 */
    @Nullable
    private String getLauncherName(@NonNull Context context) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.loadLabel(context.getPackageManager()).toString();
        }
        return null;
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
                new AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                cancelLoadingDialog();
                new AlertDialog.Builder(requireActivity())
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
        View view;
        ArrayList<String> installFileArrayList = new ArrayList<>();

        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_APP_CHECK:
                cancelLoadingDialog();
                ArrayList<GetAppListView.AppData> appDataArrayList = new ArrayList<>();

                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(requireActivity().getExternalCacheDir(), Constants.CHECK_JSON));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONArray jsonArray = jsonObj2.getJSONArray("appList");

                    String model;

                    switch (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, 0)) {
                        case Constants.MODEL_CT3 -> model = "CT3";
                        case Constants.MODEL_CTX -> model = "CTX";
                        case Constants.MODEL_CTZ -> model = "CTZ";
                        default -> model = "CT2";
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
                } catch (Exception ignored) {
                }

                view = getLayoutInflater().inflate(R.layout.layout_app_list, null);
                ListView listView = view.findViewById(R.id.app_list);
                listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                listView.setAdapter(new GetAppListView.AppListAdapter(requireActivity(), appDataArrayList));
                listView.setOnItemClickListener((parent, view1, position, id) -> {
                    // 選択位置を保存
                    Preferences.save(requireActivity(), Constants.KEY_INT_GET_APP_TMP, position);
                    listView.invalidateViews();
                });

                new AlertDialog.Builder(requireActivity())
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

                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(str + "\n" + "よろしければ OK を押下してください。")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                        if (!Objects.equals(appDataArrayList.get(i).url, "MYURL")) {
                                            startDownload(appDataArrayList.get(i).url);
                                            dialog.dismiss();
                                        } else {
                                            View view1 = getLayoutInflater().inflate(R.layout.view_app_url, null);
                                            AppCompatEditText editText = view1.findViewById(R.id.edit_app_url);
                                            new AlertDialog.Builder(requireActivity())
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
                        new AlertDialog.Builder(requireActivity())
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
                            new AlertDialog.Builder(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(requireActivity().getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        //noinspection SequencedCollectionMethodCanBeUsed
                        installFileArrayList.add(0, new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath());
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_GET_APP, this);
                        break;
                    case 4:
                        if (!isDhizukuActive(requireActivity())) {
                            Preferences.save(requireActivity(), Constants.KEY_INT_UPDATE_MODE, 1);
                            new AlertDialog.Builder(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(requireActivity().getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        //noinspection SequencedCollectionMethodCanBeUsed
                        installFileArrayList.add(0, new File(requireActivity().getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath());
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_GET_APP, this);
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
                        preNotice.setTitle("＜＜アプリのお知らせが " + jsonArray.length() + " 件あります＞＞");
                        preNotice.setSummary("タップして確認してください。");
                    }
                } catch (Exception ignored) {
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
                new AlertDialog.Builder(requireActivity())
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
                new AlertDialog.Builder(requireActivity())
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

    public ApkInstallTask.Listener apkInstallTaskListener() {
        return new ApkInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                showLoadingDialog(getString(R.string.progress_state_installing));
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (Exception ignored) {
                }

                cancelLoadingDialog();
                AlertDialog alertDialog = new AlertDialog.Builder(MainFragment.this.requireActivity())
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
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (Exception ignored) {
                }

                cancelLoadingDialog();
                new AlertDialog.Builder(MainFragment.this.requireActivity())
                        .setMessage(getString(R.string.dialog_failure_silent_install) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            @Override
            public void onError(String message) {
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (Exception ignored) {
                }

                cancelLoadingDialog();
                new AlertDialog.Builder(MainFragment.this.requireActivity())
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean tryBindDchaService() {
        return requireActivity().bindService(Constants.ACTION_DCHA_SERVICE, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean tryBindDchaUtilService() {
        return requireActivity().bindService(Constants.ACTION_UTIL_SERVICE, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            }
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);
    }
}
