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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;
import com.rosan.dhizuku.shared.DhizukuVariables;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ByteProgressHandler;
import com.saradabar.cpadcustomizetool.data.service.DhizukuService;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.ApkMCopyTask;
import com.saradabar.cpadcustomizetool.data.task.IDhizukuTask;
import com.saradabar.cpadcustomizetool.data.task.XApkCopyTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.UninstallBlockActivity;

import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceOwnerFragment extends PreferenceFragmentCompat implements InstallEventListener {

    final Object objLock = new Object();

    AlertDialog progressDialog;

    XApkCopyTask xApkCopyTask;
    ApkMCopyTask apkMCopyTask;

    public Preference preUninstallBlock,
            preSessionInstall,
            preAbandonSession,
            preClrDevOwn,
            preNowSetOwnPkg,
            preSessionInstallNotice;

    SwitchPreferenceCompat swPrePermissionFrc;

    IDhizukuService mDhizukuService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pre_owner);

        preUninstallBlock = findPreference("pre_owner_uninstall_block");
        swPrePermissionFrc = (SwitchPreferenceCompat) findPreference("pre_owner_permission_frc");
        preSessionInstall = findPreference("pre_owner_session_install");
        preAbandonSession = findPreference("pre_owner_abandon_session");
        preClrDevOwn = findPreference("pre_owner_clr_dev_own");
        preNowSetOwnPkg = findPreference("pre_owner_now_set_own_pkg");
        preSessionInstallNotice = findPreference("pre_owner_session_install_notice");

        /* 初期化 */
        initialize();
    }

    private void setListener() {
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

                    if (Common.isDhizukuActive(requireActivity())) {
                        if (tryBindDhizukuService()) {
                            try {
                                mDhizukuService.setPermissionPolicy(DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
                            } catch (RemoteException ignored) {
                            }
                        } else return false;
                    } else {
                        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
                        dpm.setPermissionPolicy(new ComponentName(requireActivity(), AdministratorReceiver.class), DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
                    }

                    for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
                        /* ユーザーアプリか確認 */
                        if (app.sourceDir.startsWith("/data/app/")) {
                            setPermissionGrantState(app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                        }
                    }
                } else {
                    swPrePermissionFrc.setChecked(false);
                    swPrePermissionFrc.setSummary(getString(R.string.pre_owner_sum_permission_default));

                    if (Common.isDhizukuActive(requireActivity())) {
                        if (tryBindDhizukuService()) {
                            try {
                                mDhizukuService.setPermissionPolicy(DevicePolicyManager.PERMISSION_POLICY_PROMPT);
                            } catch (RemoteException ignored) {
                            }
                        } else {
                            return false;
                        }
                    } else {
                        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
                        dpm.setPermissionPolicy(new ComponentName(requireActivity(), AdministratorReceiver.class), DevicePolicyManager.PERMISSION_POLICY_PROMPT);
                    }

                    for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
                        /* ユーザーアプリか確認 */
                        if (app.sourceDir.startsWith("/data/app/")) {
                            setPermissionGrantState(app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
                        }
                    }
                }
            }
            return true;
        });

        preSessionInstall.setOnPreferenceClickListener(preference -> {
            try {
                preSessionInstall.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/*"}).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), ""), Constants.REQUEST_ACTIVITY_INSTALL);
            } catch (Exception ignored) {
                preSessionInstall.setEnabled(true);
                new AlertDialog.Builder(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, null)
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

            new AlertDialog.Builder(requireActivity())
                    .setMessage("セッションを破棄しました。")
                    .setPositiveButton(R.string.dialog_common_ok, null)
                    .show();
            return false;
        });

        preClrDevOwn.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.dialog_question_device_owner)
                    .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                        if (Common.isDhizukuActive(requireActivity())) {
                            if (tryBindDhizukuService()) {
                                try {
                                    mDhizukuService.clearDeviceOwnerApp("com.rosan.dhizuku");
                                } catch (RemoteException ignored) {
                                }
                            }
                        } else {
                            DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
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

        preSessionInstallNotice.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setView(R.layout.view_owner_session_notice)
                    .setPositiveButton("OK", null)
                    .show();
            return false;
        });

        /* 初期化 */
        initialize();
    }

    /* 初期化 */
    private void initialize() {
        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        switch (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2)) {
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

        if (dpm.isDeviceOwnerApp(requireActivity().getPackageName())) {
            if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
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
            if (!Common.isDhizukuActive(requireActivity())) {
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
            } else {
                if (Preferences.load(requireActivity(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        switch (dpm.getPermissionPolicy(DhizukuVariables.COMPONENT_NAME)) {
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
            }
        }

        if (getDeviceOwnerPackage() != null) {
            preNowSetOwnPkg.setSummary(getString(R.string.pre_owner_sum_message_1, getDeviceOwnerPackage()));
        } else {
            preNowSetOwnPkg.setSummary(getString(R.string.pre_owner_sum_no_device_owner));
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
        if (Common.isDhizukuActive(requireActivity())) {
            try {
                if (requireActivity().getPackageManager().getPackageInfo("com.rosan.dhizuku", 0).versionCode > 11) {
                    new AlertDialog.Builder(requireActivity())
                            .setCancelable(false)
                            .setMessage("Dhizuku の互換性がありません。バージョン 2.8 の Dhizuku をインストールしてください。")
                            .setPositiveButton("OK", (dialog, which) -> {
                                requireActivity().finish();
                                requireActivity().overridePendingTransition(0, 0);
                                startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                            })
                            .show();
                    return;
                }
            } catch (Exception ignored) {
            }

            View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
            TextView textView = view.findViewById(R.id.view_progress_spinner_text);
            textView.setText("サービスへの接続を待機しています...");
            AlertDialog waitForServiceDialog = new AlertDialog.Builder(requireActivity()).setCancelable(false).setView(view).create();
            waitForServiceDialog.show();
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
                try {
                    new IDhizukuTask().execute(requireActivity(), iDhizukuTaskListener());
                    synchronized (objLock) {
                        objLock.wait();
                    }
                } catch (Exception ignored) {
                }

                if (waitForServiceDialog.isShowing()) {
                    waitForServiceDialog.cancel();
                }

                if (mDhizukuService == null) {
                    new AlertDialog.Builder(requireActivity())
                            .setCancelable(false)
                            .setMessage(R.string.dialog_error_dhizuku_conn_failure)
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
        ArrayList<String> installFileArrayList = new ArrayList<>();

        if (requestCode == Constants.REQUEST_ACTIVITY_INSTALL) {
            preSessionInstall.setEnabled(true);
            try {
                if (data == null) {
                    return;
                }

                if (trySetInstallData(data, installFileArrayList)) {
                    String installFileName = new File(installFileArrayList.get(0)).getName();

                    /* ファイルの拡張子 */
                    if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apk")) {
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_SILENT, this);
                    } else if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".xapk")) {
                        xApkCopyTask = new XApkCopyTask();
                        xApkCopyTask.execute(requireActivity(), xApkListener(), installFileArrayList);
                    } else if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apkm")) {
                        apkMCopyTask = new ApkMCopyTask();
                        apkMCopyTask.execute(requireActivity(), apkMListener(), installFileArrayList);
                    }
                } else {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, null)
                            .show();
                }
            } catch (Exception ignored) {
                new AlertDialog.Builder(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_data))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        }
    }

    /* splitInstallDataにファイルパスを格納 */
    private boolean trySetInstallData(Intent intent, ArrayList<String> stringArrayList) {
        try {
            ClipData clipData = intent.getClipData();

            if (clipData == null) {
                /* シングルApk */
                stringArrayList.add(0, Common.getFilePath(requireActivity(), intent.getData()));

                if (stringArrayList.get(0) == null) {
                    return false;
                }

                String installFileName = new File(stringArrayList.get(0)).getName();

                /* ファイルの拡張子 */
                /* 未対応またはインストールファイルでないなら終了 */
                return installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apk") || installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".xapk") || installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apkm");
            } else {
                /* マルチApk */
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    /* 処理 */
                    stringArrayList.add(i, Common.getFilePath(requireActivity(), clipData.getItemAt(i).getUri()));
                }
                return stringArrayList != null;
            }
        } catch (Exception ignored) {
            return false;
        }
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
                AlertDialog alertDialog = new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
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
                new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
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
                new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
                        .setMessage(getString(R.string.dialog_error) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
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

                alertDialog = new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
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
                progressHandler.xApkCopyTask = xApkCopyTask;
                progressHandler.sendEmptyMessage(0);
            }

            @Override
            public void onSuccess(ArrayList<String> stringArrayList) {
                alertDialog.dismiss();
                new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), stringArrayList, Constants.REQUEST_INSTALL_SILENT, DeviceOwnerFragment.this);
            }

            @Override
            public void onFailure() {
                alertDialog.dismiss();
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
                        .setMessage(getString(R.string.dialog_info_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String message) {
                alertDialog.dismiss();
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
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

                alertDialog = new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
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
                progressHandler.apkMCopyTask = apkMCopyTask;
                progressHandler.sendEmptyMessage(0);
            }

            @Override
            public void onSuccess(ArrayList<String> stringArrayList) {
                alertDialog.dismiss();
                new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), stringArrayList, Constants.REQUEST_INSTALL_SILENT, DeviceOwnerFragment.this);
            }

            @Override
            public void onFailure() {
                alertDialog.dismiss();
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
                        .setMessage(getString(R.string.dialog_info_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onError(String message) {
                alertDialog.dismiss();
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = requireActivity().getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }

                new AlertDialog.Builder(DeviceOwnerFragment.this.requireActivity())
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
            apkInstallTaskListener().onSuccess();
        }
    }

    @Override
    public void onInstallFailure(int reqCode, String message) {
        if (reqCode == Constants.REQUEST_INSTALL_SILENT) {
            apkInstallTaskListener().onFailure(message);
        }
    }

    @Override
    public void onInstallError(int reqCode, String message) {
        if (reqCode == Constants.REQUEST_INSTALL_SILENT) {
            apkInstallTaskListener().onError(message);
        }
    }

    private boolean tryBindDhizukuService() {
        DhizukuUserServiceArgs args = new DhizukuUserServiceArgs(new ComponentName(requireActivity(), DhizukuService.class));
        return Dhizuku.bindUserService(args, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder iBinder) {
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setPermissionGrantState(String packageName, int grantState) {
        if (Common.isDhizukuActive(requireActivity())) {
            if (tryBindDhizukuService()) {
                try {
                    for (String permission : getRuntimePermissions(packageName)) {
                        mDhizukuService.setPermissionGrantState(packageName, permission, grantState);
                    }
                } catch (RemoteException ignored) {
                }
            }
        } else {
            DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
            for (String permission : getRuntimePermissions(packageName)) {
                dpm.setPermissionGrantState(new ComponentName(requireActivity(), AdministratorReceiver.class), packageName, permission, grantState);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private String[] getRuntimePermissions(String packageName) {
        return new ArrayList<>(Arrays.asList(getRequiredPermissions(packageName))).toArray(new String[0]);
    }

    private String[] getRequiredPermissions(String packageName) {
        try {
            String[] str = requireActivity().getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            if (str != null && str.length > 0) {
                return str;
            } else {
                return new String[0];
            }
        } catch (Exception ignored) {
            return new String[0];
        }
    }

    /* ローディングダイアログを表示する */
    private void showLoadingDialog(String message) {
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        TextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(message);
        progressDialog = new AlertDialog.Builder(requireActivity()).setCancelable(false).setView(view).create();
        progressDialog.show();
    }

    /* ローディングダイアログを非表示にする */
    private void cancelLoadingDialog() {
        if (progressDialog == null) {
            return;
        }

        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    private IDhizukuTask.Listener iDhizukuTaskListener() {
        return new IDhizukuTask.Listener() {
            @Override
            public void onSuccess(IDhizukuService iDhizukuService) {
                mDhizukuService = iDhizukuService;
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
