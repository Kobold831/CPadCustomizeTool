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
import static com.saradabar.cpadcustomizetool.util.Common.mDhizukuService;
import static com.saradabar.cpadcustomizetool.util.Common.tryBindDhizukuService;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ByteProgressHandler;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.ApkMCopyTask;
import com.saradabar.cpadcustomizetool.data.task.XApkCopyTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Variables;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;
import com.saradabar.cpadcustomizetool.view.activity.UninstallBlockActivity;

import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.IOException;

public class DeviceOwnerFragment extends PreferenceFragmentCompat implements InstallEventListener {

    String[] splitInstallData = new String[128];

    public Preference preUninstallBlock,
            preSessionInstall,
            preAbandonSession,
            preClrDevOwn,
            preNowSetOwnPkg;

    SwitchPreferenceCompat swPrePermissionFrc;

    @SuppressLint("StaticFieldLeak")
    private static DeviceOwnerFragment instance = null;

    public static DeviceOwnerFragment getInstance() {
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_owner, rootKey);

        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        preUninstallBlock = findPreference("pre_owner_uninstall_block");
        swPrePermissionFrc = findPreference("pre_owner_permission_frc");
        preSessionInstall = findPreference("pre_owner_session_install");
        preAbandonSession = findPreference("pre_owner_abandon_session");
        preClrDevOwn = findPreference("pre_owner_clr_dev_own");
        preNowSetOwnPkg = findPreference("pre_owner_now_set_own_pkg");

        preUninstallBlock.setOnPreferenceClickListener(preference -> {
            try {
                startActivity(new Intent(requireActivity(), UninstallBlockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }

            return false;
        });

        swPrePermissionFrc.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if ((boolean) o) {
                    swPrePermissionFrc.setChecked(true);
                    swPrePermissionFrc.setSummary(getString(R.string.pre_owner_sum_permission_forced));

                    if (isDhizukuActive(requireActivity())) {
                        if (tryBindDhizukuService(requireActivity())) {
                            try {
                                mDhizukuService.setPermissionPolicy(DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
                            } catch (RemoteException ignored) {
                            }
                        } else return false;
                    } else {
                        dpm.setPermissionPolicy(new ComponentName(requireActivity(), AdministratorReceiver.class), DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
                    }

                    for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
                        /* ユーザーアプリか確認 */
                        if (app.sourceDir.startsWith("/data/app/")) {
                            Common.setPermissionGrantState(requireActivity(), app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                        }
                    }
                } else {
                    swPrePermissionFrc.setChecked(false);
                    swPrePermissionFrc.setSummary(getString(R.string.pre_owner_sum_permission_default));

                    if (isDhizukuActive(requireActivity())) {
                        if (tryBindDhizukuService(requireActivity())) {
                            try {
                                mDhizukuService.setPermissionPolicy(DevicePolicyManager.PERMISSION_POLICY_PROMPT);
                            } catch (RemoteException ignored) {
                            }
                        } else {
                            return false;
                        }
                    } else {
                        dpm.setPermissionPolicy(new ComponentName(requireActivity(), AdministratorReceiver.class), DevicePolicyManager.PERMISSION_POLICY_PROMPT);
                    }

                    for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
                        /* ユーザーアプリか確認 */
                        if (app.sourceDir.startsWith("/data/app/")) {
                            Common.setPermissionGrantState(requireActivity(), app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
                        }
                    }
                }
            }

            return true;
        });

        preSessionInstall.setOnPreferenceClickListener(preference -> {
            preSessionInstall.setEnabled(false);
            Variables.isPreferenceLock = true;

            try {
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/*"}).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), ""), Constants.REQUEST_ACTIVITY_INSTALL);
            } catch (ActivityNotFoundException ignored) {
                preSessionInstall.setEnabled(true);
                new MaterialAlertDialogBuilder(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            return false;
        });

        preAbandonSession.setOnPreferenceClickListener(preference -> {
            for (PackageInstaller.SessionInfo sessionInfo : requireActivity().getPackageManager().getPackageInstaller().getMySessions()) {
                try {
                    requireActivity().getPackageManager().getPackageInstaller().abandonSession(sessionInfo.getSessionId());
                } catch (Exception ignored) {
                }
            }

            new MaterialAlertDialogBuilder(requireActivity())
                    .setMessage("セッションを破棄しました")
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        preClrDevOwn.setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(requireActivity())
                    .setMessage(R.string.dialog_question_device_owner)
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        if (isDhizukuActive(requireActivity())) {
                            if (tryBindDhizukuService(requireActivity())) {
                                try {
                                    mDhizukuService.clearDeviceOwnerApp("com.rosan.dhizuku");
                                } catch (RemoteException ignored) {
                                }
                            }
                        } else {
                            dpm.clearDeviceOwnerApp(requireActivity().getPackageName());
                        }

                        requireActivity().finish();
                        requireActivity().overridePendingTransition(0, 0);
                        startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    })
                    .setNegativeButton(R.string.dialog_common_no, null)
                    .show();
            return false;
        });

        /* 初期化 */
        initialize();
    }

    /* 初期化 */
    private void initialize() {
        switch (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2)) {
            /* チャレンジパッド２ */
            case Constants.MODEL_CT2:
                swPrePermissionFrc.setEnabled(false);
                swPrePermissionFrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preSessionInstall.setEnabled(false);
                preSessionInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preAbandonSession.setEnabled(false);
                preAbandonSession.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                break;
            /* チャレンジパッド３ */
            case Constants.MODEL_CT3:
                swPrePermissionFrc.setEnabled(false);
                swPrePermissionFrc.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preUninstallBlock.setEnabled(false);
                preUninstallBlock.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preClrDevOwn.setEnabled(false);
                preClrDevOwn.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preSessionInstall.setEnabled(false);
                preSessionInstall.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                preAbandonSession.setEnabled(false);
                preAbandonSession.setSummary(Build.MODEL + getString(R.string.pre_main_sum_message_1));
                break;
            /* チャレンジパッドNEO・NEXT */
            case Constants.MODEL_CTX:
            case Constants.MODEL_CTZ:
                break;
        }

        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (dpm.isDeviceOwnerApp(requireActivity().getPackageName())) {
            if (Preferences.load(requireActivity(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    switch (dpm.getPermissionPolicy(new ComponentName(requireActivity(), AdministratorReceiver.class))) {
                        case DevicePolicyManager.PERMISSION_POLICY_PROMPT:
                            swPrePermissionFrc.setChecked(false);
                            swPrePermissionFrc.setSummary(getString(R.string.pre_owner_sum_permission_default));
                            break;
                        case DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT:
                            swPrePermissionFrc.setChecked(true);
                            swPrePermissionFrc.setSummary(getString(R.string.pre_owner_sum_permission_forced));
                            break;
                    }
                }
            }
        } else {
            if (!isDhizukuActive(requireActivity())) {
                preUninstallBlock.setEnabled(false);
                preUninstallBlock.setSummary(getString(R.string.pre_owner_sum_not_use_function));
                preClrDevOwn.setEnabled(false);
                preClrDevOwn.setSummary(getString(R.string.pre_owner_sum_not_use_function));
                swPrePermissionFrc.setEnabled(false);
                swPrePermissionFrc.setSummary(getString(R.string.pre_owner_sum_not_use_function));
                preSessionInstall.setEnabled(false);
                preSessionInstall.setSummary(getString(R.string.pre_owner_sum_not_use_function));
                preAbandonSession.setEnabled(false);
                preAbandonSession.setSummary(getString(R.string.pre_owner_sum_not_use_function));
            }
        }

        if (getDeviceOwnerPackage() != null) {
            preNowSetOwnPkg.setSummary(getString(R.string.pre_owner_sum_message_1) + getDeviceOwnerPackage() + getString(R.string.pre_owner_sum_message_2));
        } else {
            preNowSetOwnPkg.setSummary(getString(R.string.pre_owner_sum_no_device_owner));
        }

        if (Variables.isPreferenceLock) {
            preSessionInstall.setEnabled(false);
            preSessionInstall.setSummary(getString(R.string.progress_state_installing));
        }
    }

    private String getDeviceOwnerPackage() {
        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
            /* ユーザーアプリか確認 */
            if (app.sourceDir.startsWith("/data/app/")) {
                if (dpm.isDeviceOwnerApp(app.packageName)) {
                    return app.loadLabel(requireActivity().getPackageManager()).toString();
                }
            }
        }

        return null;
    }

    /* 再表示 */
    @Override
    public void onResume() {
        super.onResume();
        initialize();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_ACTIVITY_INSTALL) {
            if (trySetInstallData(data)) {
                String installFileName = new File(splitInstallData[0]).getName();

                /* ファイルの拡張子 */
                if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apk")) {
                    new ApkInstallTask().execute(requireActivity(), apkListener(), splitInstallData);
                    return;
                } else if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".xapk")) {
                    new XApkCopyTask().execute(requireActivity(), xApkListener(), splitInstallData);
                    return;
                } else if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apkm")) {
                    new ApkMCopyTask().execute(requireActivity(), apkMListener(), splitInstallData);
                    return;
                }
            }

            Variables.isPreferenceLock = false;
            preSessionInstall.setEnabled(true);

            new MaterialAlertDialogBuilder(requireActivity())
                    .setMessage(getString(R.string.dialog_error_no_file_data))
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    /* splitInstallDataにファイルパスを格納 */
    private boolean trySetInstallData(Intent intent) {
        try {
            /* 一時ファイルを消去 */
            File tmpFile = requireActivity().getExternalCacheDir();

            if (tmpFile != null) {
                FileUtils.deleteDirectory(tmpFile);
            }
        } catch (IOException ignored) {
        }

        try {
            ClipData clipData = intent.getClipData();

            if (clipData == null) {
                /* シングルApk */
                splitInstallData[0] = Common.getFilePath(requireActivity(), intent.getData());

                if (splitInstallData[0] == null) {
                    return false;
                }

                String installFileName = new File(splitInstallData[0]).getName();

                /* ファイルの拡張子 */
                if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apk") || installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".xapk") || installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apkm")) {
                    return splitInstallData != null;
                } else {
                    /* 未対応またはインストールファイルでないなら終了 */
                    return false;
                }
            } else {
                /* マルチApk */
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    /* 処理 */
                    splitInstallData[i] = Common.getFilePath(requireActivity(), clipData.getItemAt(i).getUri());
                }
            }

            return splitInstallData != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    public ApkInstallTask.Listener apkListener() {
        return new ApkInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                preSessionInstall.setSummary(getString(R.string.progress_state_installing));
                LinearProgressIndicator linearProgressIndicator = requireActivity().findViewById(R.id.act_progress_main);
                linearProgressIndicator.show();
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                Variables.isPreferenceLock = false;
                preSessionInstall.setEnabled(true);
                preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);

                LinearProgressIndicator linearProgressIndicator = StartActivity.getInstance().findViewById(R.id.act_progress_main);

                if (linearProgressIndicator.isShown()) {
                    linearProgressIndicator.hide();
                }

                if (MainFragment.getInstance() != null) {
                    if (MainFragment.getInstance().isAdded()) {
                        MainFragment.getInstance().initialize();
                    }
                }

                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = StartActivity.getInstance().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                AlertDialog alertDialog = new MaterialAlertDialogBuilder(StartActivity.getInstance())
                        .setMessage(R.string.dialog_info_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .create();

                if (!alertDialog.isShowing()) {
                    alertDialog.show();
                }
            }

            /* 失敗 */
            @Override
            public void onFailure(String message) {
                Variables.isPreferenceLock = false;
                preSessionInstall.setEnabled(true);
                preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);

                LinearProgressIndicator linearProgressIndicator = StartActivity.getInstance().findViewById(R.id.act_progress_main);

                if (linearProgressIndicator.isShown()) {
                    linearProgressIndicator.hide();
                }

                if (MainFragment.getInstance() != null) {
                    if (MainFragment.getInstance().isAdded()) {
                        MainFragment.getInstance().initialize();
                    }
                }

                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = StartActivity.getInstance().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.getInstance())
                        .setMessage(getString(R.string.dialog_info_failure_silent_install) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String message) {
                Variables.isPreferenceLock = false;
                preSessionInstall.setEnabled(true);
                preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);

                LinearProgressIndicator linearProgressIndicator = StartActivity.getInstance().findViewById(R.id.act_progress_main);

                if (linearProgressIndicator.isShown()) {
                    linearProgressIndicator.hide();
                }

                if (MainFragment.getInstance() != null) {
                    if (MainFragment.getInstance().isAdded()) {
                        MainFragment.getInstance().initialize();
                    }
                }

                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = StartActivity.getInstance().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.getInstance())
                        .setMessage(getString(R.string.dialog_error) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        };
    }

    public XApkCopyTask.Listener xApkListener() {
        return new XApkCopyTask.Listener() {

            AlertDialog alertDialog;
            ByteProgressHandler progressHandler;

            @Override
            public void onShow() {
                View view = getLayoutInflater().inflate(R.layout.view_progress, null);
                ProgressBar progressBar = view.findViewById(R.id.progress);
                TextView textPercent = view.findViewById(R.id.progress_percent);
                TextView textByte = view.findViewById(R.id.progress_byte);

                progressBar.setProgress(0);
                textPercent.setText(new StringBuilder(progressBar.getProgress()).append(getString(R.string.percent)));

                alertDialog = new MaterialAlertDialogBuilder(StartActivity.getInstance())
                        .setView(view)
                        .setMessage("")
                        .setCancelable(false)
                        .create();

                if (!alertDialog.isShowing()) {
                    alertDialog.show();
                }

                progressHandler = new ByteProgressHandler(Looper.getMainLooper());
                progressHandler.progressBar = progressBar;
                progressHandler.textPercent = textPercent;
                progressHandler.textByte = textByte;
                progressHandler.xApkCopyTask = XApkCopyTask.xApkCopyTask;
                progressHandler.sendEmptyMessage(0);
            }

            @Override
            public void onSuccess() {
                alertDialog.dismiss();
                new ApkInstallTask().execute(requireActivity(), apkListener(), splitInstallData);
            }

            @Override
            public void onFailure() {
                alertDialog.dismiss();
                Variables.isPreferenceLock = false;
                preSessionInstall.setEnabled(true);
                preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);

                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.getInstance())
                        .setMessage(getString(R.string.dialog_info_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String message) {
                alertDialog.dismiss();
                Variables.isPreferenceLock = false;
                preSessionInstall.setEnabled(true);
                preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);

                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.getInstance())
                        .setMessage(getString(R.string.dialog_error) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onProgressUpdate(String message) {
                alertDialog.setMessage(message);
            }
        };
    }

    public ApkMCopyTask.Listener apkMListener() {
        return new ApkMCopyTask.Listener() {

            AlertDialog alertDialog;
            ByteProgressHandler progressHandler;

            @Override
            public void onShow() {
                View view = getLayoutInflater().inflate(R.layout.view_progress, null);
                ProgressBar progressBar = view.findViewById(R.id.progress);
                TextView textPercent = view.findViewById(R.id.progress_percent);
                TextView textByte = view.findViewById(R.id.progress_byte);

                progressBar.setProgress(0);
                textPercent.setText(new StringBuilder(progressBar.getProgress()).append(getString(R.string.percent)));

                alertDialog = new MaterialAlertDialogBuilder(StartActivity.getInstance())
                        .setView(view)
                        .setMessage("")
                        .setCancelable(false)
                        .create();

                if (!alertDialog.isShowing()) {
                    alertDialog.show();
                }

                progressHandler = new ByteProgressHandler(Looper.getMainLooper());
                progressHandler.progressBar = progressBar;
                progressHandler.textPercent = textPercent;
                progressHandler.textByte = textByte;
                progressHandler.apkMCopyTask = ApkMCopyTask.apkMCopyTask;
                progressHandler.sendEmptyMessage(0);
            }

            @Override
            public void onSuccess() {
                alertDialog.dismiss();
                new ApkInstallTask().execute(requireActivity(), apkListener(), splitInstallData);
            }

            @Override
            public void onFailure() {
                alertDialog.dismiss();
                Variables.isPreferenceLock = false;
                preSessionInstall.setEnabled(true);
                preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);

                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new MaterialAlertDialogBuilder(StartActivity.getInstance())
                        .setMessage(getString(R.string.dialog_info_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String message) {
                alertDialog.dismiss();
                Variables.isPreferenceLock = false;
                preSessionInstall.setEnabled(true);
                preSessionInstall.setSummary(R.string.pre_owner_sum_silent_install);

                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new AlertDialog.Builder(StartActivity.getInstance())
                        .setMessage(getString(R.string.dialog_error) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onProgressUpdate(String message) {
                alertDialog.setMessage(message);
            }
        };
    }

    @Override
    public void onInstallSuccess(int reqCode) {
        if (reqCode == Constants.REQUEST_INSTALL_SILENT) {
            apkListener().onSuccess();
        }
    }

    @Override
    public void onInstallFailure(int reqCode, String message) {
        if (reqCode == Constants.REQUEST_INSTALL_SILENT) {
            apkListener().onFailure(message);
        }
    }

    @Override
    public void onInstallError(int reqCode, String message) {
        if (reqCode == Constants.REQUEST_INSTALL_SILENT) {
            apkListener().onError(message);
        }
    }
}
