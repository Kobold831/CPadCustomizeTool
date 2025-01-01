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
import android.app.AlertDialog;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class MainFragment extends PreferenceFragmentCompat implements DownloadEventListener, InstallEventListener {

    final Object objLock = new Object();

    AlertDialog progressDialog;
    TextView progressPercentText;
    TextView progressByteText;
    ProgressBar dialogProgressBar;

    String downloadFileUrl;

    IDchaService mDchaService;

    boolean isObsDchaState = false,
            isObsNavigation = false,
            isObsUnkSrc = false,
            isObsAdb = false;

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

    public Preference preEnableDchaService,
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pre_main);

        swDchaState = (SwitchPreferenceCompat) findPreference("pre_dcha_state");
        swKeepDchaState = (SwitchPreferenceCompat) findPreference("pre_keep_dcha_state");
        swNavigation = (SwitchPreferenceCompat) findPreference("pre_navigation");
        swKeepNavigation = (SwitchPreferenceCompat) findPreference("pre_keep_navigation");
        swUnkSrc = (SwitchPreferenceCompat) findPreference("pre_unk_src");
        swKeepUnkSrc = (SwitchPreferenceCompat) findPreference("pre_keep_unk_src");
        swAdb = (SwitchPreferenceCompat) findPreference("pre_adb");
        swKeepAdb = (SwitchPreferenceCompat) findPreference("pre_keep_adb");
        swBypassAdbDisable = (SwitchPreferenceCompat) findPreference("pre_app_adb");
        preLauncher = findPreference("pre_launcher");
        swKeepLauncher = (SwitchPreferenceCompat) findPreference("pre_keep_launcher");
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
        swDeviceAdmin = (SwitchPreferenceCompat) findPreference("pre_device_admin");
        preGetApp = findPreference("pre_get_app");
        preNotice = findPreference("pre_notice");

        /* 初期化 */
        initialize();
    }

    /* アクティビティ破棄 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isObsDchaState) {
            isObsDchaState = false;
            requireActivity().getContentResolver().unregisterContentObserver(dchaStateObserver);
        }

        if (isObsNavigation) {
            isObsNavigation = false;
            requireActivity().getContentResolver().unregisterContentObserver(navigationBarObserver);
        }

        if (isObsUnkSrc) {
            isObsUnkSrc = false;
            requireActivity().getContentResolver().unregisterContentObserver(marketObserver);
        }

        if (isObsAdb) {
            isObsAdb = false;
            requireActivity().getContentResolver().unregisterContentObserver(usbDebugObserver);
        }
    }

    /* 再表示 */
    @Override
    public void onResume() {
        super.onResume();
        if (Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
            TextView textView = view.findViewById(R.id.view_progress_spinner_text);
            textView.setText("サービスへの接続を待機しています...");
            AlertDialog waitForServiceDialog = new AlertDialog.Builder(requireActivity()).setCancelable(false).setView(view).create();
            waitForServiceDialog.show();
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                try {
                    new IDchaTask().execute(requireActivity(), iDchaTaskListener());
                    synchronized (objLock) {
                        objLock.wait();
                    }
                } catch (Exception ignored) {
                }

                if (waitForServiceDialog.isShowing()) {
                    waitForServiceDialog.cancel();
                }

                if (mDchaService == null) {
                    new AlertDialog.Builder(requireActivity())
                            .setCancelable(false)
                            .setMessage("DchaServiceとの通信に失敗しました")
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                requireActivity().finish();
                                requireActivity().overridePendingTransition(0, 0);
                                startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                            })
                            .show();
                } else {
                    setListener();
                }
            });
        } else {
            setListener();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

            if ((boolean) o) {
                new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(3);
            } else {
                new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
            }
            return false;
        });

        swKeepDchaState.setOnPreferenceChangeListener((preference, o) -> {
            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            Preferences.save(requireActivity(), Constants.KEY_ENABLED_KEEP_DCHA_STATE, (boolean) o);

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
            Preferences.save(requireActivity(), Constants.KEY_ENABLED_KEEP_SERVICE, (boolean) o);

            if ((boolean) o) {
                new DchaServiceUtil(requireActivity(), mDchaService).hideNavigationBar(false);
            }

            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            try {
                if ((boolean) o) {
                    chgSetting(Constants.FLAG_MARKET_APP_TRUE);
                } else {
                    chgSetting(Constants.FLAG_MARKET_APP_FALSE);
                }
            } catch (Exception ignored) {
                Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        swKeepUnkSrc.setOnPreferenceChangeListener((preference, o) -> {
            try {
                if ((boolean) o) {
                    chgSetting(Constants.FLAG_MARKET_APP_TRUE);
                }
            } catch (Exception ignored) {
                Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();
                return false;
            }

            Preferences.save(requireActivity(), Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                try {
                    if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(3);
                        Thread.sleep(100);
                    }

                    chgSetting(Constants.FLAG_USB_DEBUG_TRUE);

                    if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                    }
                } catch (Exception ignored) {
                    Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();

                    if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                    }
                }
            } else {
                try {
                    chgSetting(Constants.FLAG_USB_DEBUG_FALSE);
                } catch (Exception ignored) {
                    Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();
                }
            }
            return false;
        });

        swKeepAdb.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            if ((boolean) o) {
                try {
                    if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(3);
                        Thread.sleep(100);
                    }

                    chgSetting(Constants.FLAG_USB_DEBUG_TRUE);

                    if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                    }
                } catch (Exception ignored) {
                    Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();

                    if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                            Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                        new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                    }
                    return false;
                }
            }

            Preferences.save(requireActivity(), Constants.KEY_ENABLED_KEEP_USB_DEBUG, (boolean) o);
            requireActivity().startService(new Intent(requireActivity(), KeepService.class));
            requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));
            return true;
        });

        swBypassAdbDisable.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            if (!Common.isCfmDialog(requireActivity())) {
                cfmDialog();
                return false;
            }

            try {
                if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                        Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                    new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(3);
                    Thread.sleep(100);
                }

                chgSetting(Constants.FLAG_USB_DEBUG_TRUE);

                if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                        Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                    new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                }
            } catch (Exception ignored) {
                Toast.makeText(requireActivity(), R.string.toast_not_change, Toast.LENGTH_SHORT).show();

                if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTX ||
                        Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CTZ) {
                    new DchaServiceUtil(requireActivity(), mDchaService).setSetupStatus(0);
                }
                return false;
            }

            Preferences.save(requireActivity(), Constants.KEY_ENABLED_AUTO_USB_DEBUG, (boolean) o);
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
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        swKeepLauncher.setOnPreferenceChangeListener((preference, o) -> {
            Preferences.save(requireActivity(), Constants.KEY_ENABLED_KEEP_HOME, (boolean) o);

            if ((boolean) o) {
                Preferences.save(requireActivity(), Constants.KEY_SAVE_KEEP_HOME, getLauncherPackage(requireActivity()));
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
                    .setMessage(R.string.dialog_question_dcha_service)
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        if (!Preferences.load(requireActivity(), "debug_restriction", false) && !tryBindDchaService()) {
                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(R.string.dialog_error_not_work_dcha)
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        } else {
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_DCHA_SERVICE, true);
                            requireActivity().finish();
                            requireActivity().overridePendingTransition(0, 0);
                            startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        preEmgManual.setOnPreferenceClickListener(preference -> {
            TextView textView = new TextView(requireActivity());
            textView.setText(R.string.dialog_emergency_manual_red);
            textView.setTextSize(16);
            textView.setTextColor(Color.RED);
            textView.setPadding(35, 0, 35, 0);
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.dialog_title_emergency_manual)
                    .setMessage(R.string.dialog_emergency_manual)
                    .setView(textView)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preEmgExecute.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage("緊急モードを起動してもよろしいですか？\nよろしければ\"はい\"を押下してください")
                    .setNeutralButton(R.string.dialog_common_yes, (dialogInterface, i) -> startActivity(new Intent(requireActivity(), EmergencyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                    .setPositiveButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        preEmgShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.activity_emergency))
                        .setShortLabel(getString(R.string.activity_emergency))
                        .setIcon(Icon.createWithResource(requireActivity(), R.drawable.alert))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), "com.saradabar.cpadcustomizetool.view.activity.EmergencyActivity"))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent("com.android.launcher.action.INSTALL_SHORTCUT").putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName("com.saradabar.cpadcustomizetool", "com.saradabar.cpadcustomizetool.view.activity.EmergencyActivity"))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), R.drawable.alert))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_emergency));
                Toast.makeText(requireActivity(), R.string.toast_common_success, Toast.LENGTH_SHORT).show();
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
                Preferences.save(requireActivity(), Constants.KEY_NORMAL_LAUNCHER, Uri.fromParts("package", installedAppList.get(position).activityInfo.packageName, null).toString().replace("package:", ""));
                /* listviewの更新 */
                listView.invalidateViews();
                initialize();
            });

            new AlertDialog.Builder(requireActivity())
                    .setView(view)
                    .setTitle(R.string.dialog_title_launcher)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preNorManual.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.dialog_title_normal_manual)
                    .setMessage(R.string.dialog_normal_manual)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return false;
        });

        preNorExecute.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage("通常モードを起動してもよろしいですか？\nよろしければ\"はい\"を押下してください")
                    .setNeutralButton(R.string.dialog_common_yes, (dialogInterface, i) -> startActivity(new Intent(requireActivity(), NormalActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                    .setPositiveButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        preNorShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.activity_normal))
                        .setShortLabel(getString(R.string.activity_normal))
                        .setIcon(Icon.createWithResource(requireActivity(), R.drawable.reboot))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), "com.saradabar.cpadcustomizetool.view.activity.NormalActivity"))
                        .build(), null);
            } else {
                requireActivity().sendBroadcast(new Intent("com.android.launcher.action.INSTALL_SHORTCUT").putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName("com.saradabar.cpadcustomizetool", "com.saradabar.cpadcustomizetool.view.activity.NormalActivity"))
                        .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), R.drawable.reboot))
                        .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_normal));
                Toast.makeText(requireActivity(), R.string.toast_common_success, Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        preReboot.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.dialog_question_reboot)
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        try {
                            mDchaService.rebootPad(0, "");
                        } catch (Exception ignored) {
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        preRebootShortcut.setOnPreferenceClickListener(preference -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().getSystemService(ShortcutManager.class).requestPinShortcut(new ShortcutInfo.Builder(requireActivity(), getString(R.string.shortcut_reboot))
                        .setShortLabel(getString(R.string.shortcut_reboot))
                        .setIcon(Icon.createWithResource(requireActivity(), R.drawable.reboot))
                        .setIntent(new Intent(Intent.ACTION_MAIN).setClassName(requireActivity(), "com.saradabar.cpadcustomizetool.view.activity.RebootActivity"))
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
            if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT2 || Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT3) {
                if (!tryBindDchaUtilService()) {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(R.string.dialog_error_not_work_dcha_util)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                            .show();
                    return false;
                }
            }

            View view = requireActivity().getLayoutInflater().inflate(R.layout.view_resolution, null);

            new AlertDialog.Builder(requireActivity())
                    .setView(view)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_resolution)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
                        EditText editTextWidth = view.findViewById(R.id.edit_text_1);
                        EditText editTextHeight = view.findViewById(R.id.edit_text_2);

                        try {
                            int width = Integer.parseInt(editTextWidth.getText().toString());
                            int height = Integer.parseInt(editTextHeight.getText().toString());

                            if (width < 0 || height < 0) {
                                new AlertDialog.Builder(requireActivity())
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage(R.string.dialog_error_illegal_value)
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> dialog1.dismiss())
                                        .show();
                            } else {
                                StartActivity startActivity = (StartActivity) requireActivity();
                                new ResolutionTask().execute(requireActivity(), startActivity.resolutionTaskListener(), width, height);
                            }
                        } catch (NumberFormatException ignored) {
                            new AlertDialog.Builder(requireActivity())
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
            if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT2 || Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT3) {
                if (!tryBindDchaService()) {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(R.string.dialog_error_not_work_dcha_util)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
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
                        .setMessage(R.string.dialog_error_dhizuku_conn_failure)
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
                                        .setMessage(R.string.dialog_info_dhizuku_grant_permission)
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                        .show();
                            } else {
                                new AlertDialog.Builder(requireActivity())
                                        .setMessage(R.string.dialog_info_dhizuku_deny_permission)
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                        .show();
                            }
                        });
                    }
                });
            } else {
                new AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.dialog_info_dhizuku_already_grant_permission)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
            return false;
        });

        swDeviceAdmin.setOnPreferenceChangeListener((preference, o) -> {
            if ((boolean) o) {
                if (!((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isAdminActive(new ComponentName(requireActivity(), AdministratorReceiver.class))) {
                    startActivityForResult(new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(requireActivity(), AdministratorReceiver.class)), Constants.REQUEST_ACTIVITY_ADMIN);
                }
            } else {
                swDeviceAdmin.setChecked(true);
                new AlertDialog.Builder(requireActivity())
                        .setTitle("デバイス管理者を無効にしますか？")
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

        preGetApp.setOnPreferenceClickListener(preference -> {
            showLoadingDialog("サーバーと通信しています…");
            new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(requireActivity().getExternalCacheDir(), "Check.json"), Constants.REQUEST_DOWNLOAD_APP_CHECK);
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
        requireActivity().sendBroadcast(new Intent("com.android.launcher.action.INSTALL_SHORTCUT").putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(Intent.ACTION_MAIN).setClassName("com.saradabar.cpadcustomizetool", "com.saradabar.cpadcustomizetool.view.activity.RebootActivity"))
                .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(requireActivity(), R.drawable.reboot))
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, R.string.activity_reboot));
        Toast.makeText(requireActivity(), R.string.toast_common_success, Toast.LENGTH_SHORT).show();
    }

    /* 初期化 */
    public void initialize() {
        if (getPreferenceScreen() != null) {
            /* DchaServiceを使用するか */
            if (!Preferences.load(requireActivity(), Constants.KEY_FLAG_DCHA_SERVICE, false)) {
                for (Preference preference : Arrays.asList(
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
                        getPreferenceScreen().removePreference(preference);
                    }
                }
            } else {
                getPreferenceScreen().removePreference(preEnableDchaService);
            }
        }

        /* オブサーバーを有効化 */
        isObsDchaState = true;
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.DCHA_STATE), false, dchaStateObserver);
        isObsNavigation = true;
        requireActivity().getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.HIDE_NAVIGATION_BAR), false, navigationBarObserver);
        isObsUnkSrc = true;
        //noinspection deprecation
        requireActivity().getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.INSTALL_NON_MARKET_APPS), false, marketObserver);
        isObsAdb = true;
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
        swKeepNavigation.setChecked(Preferences.load(requireActivity(), Constants.KEY_ENABLED_KEEP_SERVICE, false));
        swKeepUnkSrc.setChecked(Preferences.load(requireActivity(), Constants.KEY_ENABLED_KEEP_MARKET_APP_SERVICE, false));
        swKeepDchaState.setChecked(Preferences.load(requireActivity(), Constants.KEY_ENABLED_KEEP_DCHA_STATE, false));
        swKeepAdb.setChecked(Preferences.load(requireActivity(), Constants.KEY_ENABLED_KEEP_USB_DEBUG, false));
        swKeepLauncher.setChecked(Preferences.load(requireActivity(), Constants.KEY_ENABLED_KEEP_HOME, false));
        preLauncher.setSummary(getLauncherName(requireActivity()));

        String normalLauncherName = null;

        try {
            normalLauncherName = (String) requireActivity().getPackageManager().getApplicationLabel(requireActivity().getPackageManager().getApplicationInfo(Preferences.load(requireActivity(), Constants.KEY_NORMAL_LAUNCHER, ""), 0));
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        if (normalLauncherName == null) {
            preSelNorLauncher.setSummary(getString(R.string.pre_main_sum_no_setting_launcher));
        } else {
            preSelNorLauncher.setSummary(getString(R.string.pre_main_sum_message_2, normalLauncherName));
        }

        /* 維持スイッチが有効のときサービスが停止していたら起動 */
        requireActivity().startService(new Intent(requireActivity(), KeepService.class));
        requireActivity().startService(new Intent(requireActivity(), ProtectKeepService.class));

        /* 端末ごとにPreferenceの状態を設定 */
        switch (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2)) {
            case Constants.MODEL_CT2:
                try {
                    if (requireActivity().getPackageManager().getPackageInfo(Constants.DCHA_SERVICE_PACKAGE, 0).versionCode < 5) {
                        preSilentInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                        preSilentInstall.setEnabled(false);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {
                }
                break;
            case Constants.MODEL_CT3:
                swDeviceAdmin.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                swDeviceAdmin.setEnabled(false);
                break;
            case Constants.MODEL_CTX:
            case Constants.MODEL_CTZ:
                break;
        }

        if (((UserManager) requireActivity().getSystemService(Context.USER_SERVICE)).hasUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)) {
            swKeepUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
            swKeepUnkSrc.setEnabled(false);
            swUnkSrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
            swUnkSrc.setEnabled(false);
        }

        if (((DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(requireActivity().getPackageName())) {
            swDeviceAdmin.setEnabled(false);
            swDeviceAdmin.setSummary(getString(R.string.pre_main_sum_already_device_owner));
            preDhizukuPermissionReq.setEnabled(false);
            preDhizukuPermissionReq.setSummary(getString(R.string.pre_main_sum_already_device_owner));
        }

        swBypassAdbDisable.setChecked(Preferences.load(requireActivity(), Constants.KEY_ENABLED_AUTO_USB_DEBUG, false));

        switch (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2)) {
            case Constants.MODEL_CT2:
            case Constants.MODEL_CT3:
                swBypassAdbDisable.setEnabled(false);
                swBypassAdbDisable.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                break;
        }

        new FileDownloadTask().execute(this, Constants.URL_NOTICE, new File(requireActivity().getExternalCacheDir(), "ct-notice.json"), Constants.REQUEST_DOWNLOAD_NOTICE);
        //new FileDownloadTask().execute(this, Constants.URL_TEST, new File(requireActivity().getExternalCacheDir(), "test.json"), 99);
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
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    private void chgSetting(int req) {
        switch (req) {
            case Constants.FLAG_USB_DEBUG_TRUE:
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
        TextView textView = view.findViewById(R.id.view_progress_spinner_text);
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
                .setTitle(getString(R.string.dialog_question_are_you_sure))
                .setMessage(getString(R.string.dialog_confirmation))
                .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> new AlertDialog.Builder(requireActivity())
                        .setCancelable(false)
                        .setTitle(getString(R.string.dialog_title_final_confirmation))
                        .setMessage(getString(R.string.dialog_final_confirmation))
                        .setPositiveButton(R.string.dialog_common_cancel, (dialog1, which1) -> dialog.dismiss())
                        .setNeutralButton(R.string.dialog_common_continue, (dialog1, which1) -> {
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_CONFIRMATION, true);
                            dialog1.dismiss();
                        })
                        .show())
                .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startDownload() {
        FileDownloadTask fileDownloadTask = new FileDownloadTask();
        fileDownloadTask.execute(this, downloadFileUrl, new File(requireActivity().getExternalCacheDir(), "update.apk"), Constants.REQUEST_DOWNLOAD_APK);
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
    private String getLauncherPackage(Context context) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        return null;
    }

    /* ランチャーのアプリ名を取得 */
    private String getLauncherName(Context context) {
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

        if (resolveInfo != null) {
            return resolveInfo.activityInfo.loadLabel(context.getPackageManager()).toString();
        }
        return null;
    }

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
                        .setMessage(R.string.dialog_info_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                cancelLoadingDialog();
                new AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.dialog_info_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
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
                    JSONObject jsonObj1 = Common.parseJson(new File(requireActivity().getExternalCacheDir(), "Check.json"));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONArray jsonArray = jsonObj2.getJSONArray("appList");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        GetAppListView.AppData data = new GetAppListView.AppData();
                        data.str = jsonArray.getJSONObject(i).getString("name");
                        appDataArrayList.add(data);
                    }
                } catch (Exception ignored) {
                }

                view = getLayoutInflater().inflate(R.layout.layout_app_list, null);
                ListView listView = view.findViewById(R.id.app_list);
                listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                listView.setAdapter(new GetAppListView.AppListAdapter(requireActivity(), appDataArrayList));
                listView.setOnItemClickListener((parent, view1, position, id) -> {
                    Preferences.save(requireActivity(), Constants.KEY_RADIO_TMP, (int) id);
                    listView.invalidateViews();
                });

                new AlertDialog.Builder(requireActivity())
                        .setView(view)
                        .setTitle("アプリを選択してください")
                        .setMessage("選択してOKを押下すると詳細な情報が表示されます")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            StringBuilder str = new StringBuilder();

                            for (int i = 0; i < listView.getCount(); i++) {
                                RadioButton radioButton = listView.getChildAt(i).findViewById(R.id.v_app_list_radio);
                                if (radioButton.isChecked()) {
                                    try {
                                        JSONObject jsonObj1 = Common.parseJson(new File(requireActivity().getExternalCacheDir(), "Check.json"));
                                        JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                                        JSONArray jsonArray = jsonObj2.getJSONArray("appList");
                                        str.append("アプリ名：").append(jsonArray.getJSONObject(i).getString("name")).append("\n\n").append("説明：").append(jsonArray.getJSONObject(i).getString("description")).append("\n");
                                        downloadFileUrl = jsonArray.getJSONObject(i).getString("url");
                                    } catch (Exception ignored) {
                                    }
                                }
                            }

                            if (str.toString().isEmpty()) {
                                return;
                            }

                            new AlertDialog.Builder(requireActivity())
                                    .setMessage(str + "\n" + "よろしければOKを押下してください")
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                        if (!Objects.equals(downloadFileUrl, "MYURL")) {
                                            startDownload();
                                            dialog.dismiss();
                                        } else {
                                            View view1 = getLayoutInflater().inflate(R.layout.view_app_url, null);
                                            EditText editText = view1.findViewById(R.id.edit_app_url);
                                            new AlertDialog.Builder(requireActivity())
                                                    .setMessage("http://またはhttps://を含むURLを指定してください")
                                                    .setView(view1)
                                                    .setCancelable(false)
                                                    .setPositiveButton(R.string.dialog_common_ok, (dialog3, which3) -> {
                                                        ((InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(), 0);
                                                        downloadFileUrl = editText.getText().toString();
                                                        startDownload();
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
                switch (Preferences.load(requireActivity(), Constants.KEY_FLAG_UPDATE_MODE, 1)) {
                    case 0:
                        requireActivity().startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(requireActivity().getExternalCacheDir(), "update.apk").getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case 1:
                        new AlertDialog.Builder(requireActivity())
                                .setCancelable(false)
                                .setTitle("インストール")
                                .setMessage("遷移先のページよりapkファイルをダウンロードしてadbでインストールしてください")
                                .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                    try {
                                        requireActivity().startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadFileUrl)), Constants.REQUEST_ACTIVITY_UPDATE);
                                    } catch (Exception ignored) {
                                        Toast.makeText(requireActivity(), R.string.toast_unknown_activity, Toast.LENGTH_SHORT).show();
                                        requireActivity().finish();
                                    }
                                })
                                .setNegativeButton("キャンセル", null)
                                .show();
                        break;
                    case 2:
                        new DchaInstallTask().execute(requireActivity(), dchaInstallTaskListener(), new File(requireActivity().getExternalCacheDir(), "update.apk").getPath());
                        break;
                    case 3:
                        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
                        if (!dpm.isDeviceOwnerApp(requireActivity().getPackageName())) {
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_UPDATE_MODE, 1);
                            new AlertDialog.Builder(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(requireActivity().getString(R.string.dialog_error_reset_update_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        installFileArrayList.add(0, new File(requireActivity().getExternalCacheDir(), "update.apk").getPath());
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_GET_APP, this);
                        break;
                    case 4:
                        if (!isDhizukuActive(requireActivity())) {
                            Preferences.save(requireActivity(), Constants.KEY_FLAG_UPDATE_MODE, 1);
                            new AlertDialog.Builder(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(requireActivity().getString(R.string.dialog_error_reset_update_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        installFileArrayList.add(0, new File(requireActivity().getExternalCacheDir(), "update.apk").getPath());
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_GET_APP, this);
                        break;
                }
                break;
            case Constants.REQUEST_DOWNLOAD_NOTICE:
                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(requireActivity().getExternalCacheDir(), "ct-notice.json"));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONArray jsonArray = jsonObj2.getJSONArray("noticeList");
                    if (jsonArray.length() == 0) {
                        preNotice.setTitle("＊＊アプリのお知らせはありません＊＊");
                    } else {
                        preNotice.setTitle("＊＊アプリのお知らせが" + jsonArray.length() + "件あります＊＊");
                        preNotice.setSummary("タップして確認してください");
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
                        .setMessage("ダウンロードに失敗しました\nネットワークが安定しているか確認してください")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
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
                        .setMessage("データ取得に失敗しました\nネットワークを確認してください")
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
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
        progressDialog.setMessage(new StringBuilder("インストールファイルをサーバーからダウンロードしています…\nしばらくお待ち下さい…"));
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
                        .setMessage(R.string.dialog_info_success_silent_install)
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
                        .setMessage(getString(R.string.dialog_info_failure_silent_install) + "\n" + message)
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
                        .setMessage(getString(R.string.dialog_error) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean tryBindDchaService() {
        return requireActivity().bindService(Constants.DCHA_SERVICE, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);
    }

    private boolean tryBindDchaUtilService() {
        return requireActivity().bindService(Constants.DCHA_UTIL_SERVICE, new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        }, Context.BIND_AUTO_CREATE);
    }

    private IDchaTask.Listener iDchaTaskListener() {
        return new IDchaTask.Listener() {
            @Override
            public void onSuccess(IDchaService iDchaService) {
                mDchaService = iDchaService;
                synchronized (objLock) {
                    objLock.notify();
                }
            }

            @Override
            public void onFailure() {
                synchronized (objLock) {
                    objLock.notify();
                }
            }
        };
    }
}
