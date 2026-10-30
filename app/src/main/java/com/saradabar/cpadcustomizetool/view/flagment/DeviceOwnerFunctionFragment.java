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

import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;

import com.rosan.dhizuku.shared.DhizukuVariables;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ByteProgressHandler;
import com.saradabar.cpadcustomizetool.data.receiver.DeviceAdminReceiver;
import com.saradabar.cpadcustomizetool.data.task.ApkCopyTask;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.PermissionGrantTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.UninstallBlockActivity;

import java.io.File;
import java.util.ArrayList;

public class DeviceOwnerFunctionFragment extends PreferenceFragmentCompat implements InstallEventListener {

    AlertDialog progressDialog;
    AppCompatTextView progressSpinnerText;

    ApkCopyTask apkCopyTask;

    private Preference preUninstallBlock,
            preSessionInstall,
            preAbandonSession,
            preClrDevOwn,
            preNowSetOwnPkg;

    private SwitchPreferenceCompat swPrePermissionFrc;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pre_device_owner_function, rootKey);

        preUninstallBlock = findPreference("pre_owner_uninstall_block");
        swPrePermissionFrc = findPreference("pre_owner_permission_frc");
        preSessionInstall = findPreference("pre_owner_session_install");
        preAbandonSession = findPreference("pre_owner_abandon_session");
        preClrDevOwn = findPreference("pre_owner_clr_dev_own");
        preNowSetOwnPkg = findPreference("pre_owner_now_set_own_pkg");

        if (adminComponentMeasures()) {
            // Admin Component エラー
            return;
        }
        init();
    }

    // Admin Component を変えるための措置
    private boolean adminComponentMeasures() {
        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (dpm.isDeviceOwnerApp(requireActivity().getPackageName())
                && !dpm.isAdminActive(new ComponentName(requireActivity(), DeviceAdminReceiver.class))) {
            // このアプリがデバイスオーナーかつDeviceAdminReceiverのAdminコンポーネントがアクティブでない
            new DialogUtil(requireActivity())
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(R.string.dialog_device_owner_reconfigure)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        dpm.clearDeviceOwnerApp(requireActivity().getPackageName());
                        finish();
                    })
                    .show();
            return true;
        } else {
            return false;
        }
    }

    private void finish() {
        requireActivity().startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        requireActivity().overridePendingTransition(0, 0);
        requireActivity().finish();
    }

    private void setListener() {
        preUninstallBlock.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireActivity(), UninstallBlockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            requireActivity().overridePendingTransition(0, 0);
            return false;
        });

        swPrePermissionFrc.setOnPreferenceChangeListener((preference, o) -> {
            boolean grant = (boolean) o;
            swPrePermissionFrc.setEnabled(false);
            new PermissionGrantTask().execute(requireActivity(), grant, permissionGrantListener(grant));
            return true;
        });

        preSessionInstall.setOnPreferenceClickListener(preference -> {
            try {
                preSessionInstall.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES,
                                new String[]{"application/*"}).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), ""),
                        Constants.REQUEST_ACTIVITY_INSTALL);
            } catch (ActivityNotFoundException ignored) {
                preSessionInstall.setEnabled(true);
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_browse))
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
            return false;
        });

        preAbandonSession.setOnPreferenceClickListener(preference -> {
            for (PackageInstaller.SessionInfo sessionInfo : requireActivity().getPackageManager().getPackageInstaller().getMySessions()) {
                requireActivity().getPackageManager().getPackageInstaller().abandonSession(sessionInfo.getSessionId());
            }
            Toast.makeText(requireActivity(), R.string.toast_session_dest, Toast.LENGTH_SHORT).show();
            return false;
        });

        preClrDevOwn.setOnPreferenceClickListener(preference -> {
            new DialogUtil(requireActivity())
                    .setMessage(R.string.dialog_question_owner)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        DevicePolicyManager dpm = Common.getDevicePolicyManager(requireActivity());
                        if (Common.isDhizukuAllActive(requireActivity())) {
                            dpm.clearDeviceOwnerApp(DhizukuVariables.OFFICIAL_PACKAGE_NAME);
                        } else {
                            dpm.clearDeviceOwnerApp(requireActivity().getPackageName());
                        }
                        finish();
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });
        /* 初期化 */
        initPreference();
    }

    @NonNull
    private PermissionGrantTask.Listener permissionGrantListener(boolean grant) {
        return new PermissionGrantTask.Listener() {
            @Override
            public void onShow() {
                showLoadingDialog(getString(R.string.progress_state_permission));
            }

            @Override
            public void onProgress(int currentApp, int totalApp, String packageName, int currentPermission, int totalPermission) {
                if (!isAdded() || progressSpinnerText == null) {
                    return;
                }
                progressSpinnerText.setText(String.format("%s\n%s",
                        getString(R.string.progress_state_permission),
                        getString(R.string.progress_permission_count, currentApp, totalApp, packageName, currentPermission, totalPermission)));
            }

            @Override
            public void onSuccess() {
                cancelLoadingDialog();

                if (!isAdded()) {
                    return;
                }
                swPrePermissionFrc.setEnabled(true);
                swPrePermissionFrc.setSummary(getString(grant
                        ? R.string.pre_owner_sum_permission_forced
                        : R.string.pre_owner_sum_permission_default));
            }

            @Override
            public void onError(String message) {
                cancelLoadingDialog();

                if (!isAdded()) {
                    return;
                }
                swPrePermissionFrc.setEnabled(true);
                initPreference();
                new DialogUtil(requireActivity())
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
                        .show();
            }
        };
    }

    /* 初期化 */
    private void initPreference() {
        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (Common.isCT2()) {
            swPrePermissionFrc.setEnabled(false);
            swPrePermissionFrc.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
            preSessionInstall.setEnabled(false);
            preSessionInstall.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
            preAbandonSession.setEnabled(false);
            preAbandonSession.setSummary(getString(R.string.pre_main_sum_message_1, Build.MODEL));
        }

        if (dpm.isDeviceOwnerApp(requireActivity().getPackageName()) && !Common.isCT2() ||
                dpm.isDeviceOwnerApp(DhizukuVariables.OFFICIAL_PACKAGE_NAME) && !Common.isCT2()) {
            switch (dpm.getPermissionPolicy(new ComponentName(requireActivity(), DeviceAdminReceiver.class))) {
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

        if (!dpm.isDeviceOwnerApp(requireActivity().getPackageName()) &&
                !dpm.isDeviceOwnerApp(DhizukuVariables.OFFICIAL_PACKAGE_NAME)) {
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

        if (getDeviceOwnerPackage() != null) {
            preNowSetOwnPkg.setSummary(getString(R.string.pre_owner_sum_message_1, getDeviceOwnerPackage()));
        } else {
            preNowSetOwnPkg.setSummary(getString(R.string.pre_owner_sum_no_device_owner));
        }

        if (Preferences.load(requireActivity(), Constants.KEY_FLAG_NORMAL_ENV, Constants.DEF_BOOL)) {
            preUninstallBlock.setVisible(false);

        }
    }

    @Nullable
    private String getDeviceOwnerPackage() {
        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        for (ApplicationInfo app : Common.getUserApplications(requireActivity())) {
            if (dpm.isDeviceOwnerApp(app.packageName)) {
                return app.loadLabel(requireActivity().getPackageManager()).toString();
            }
        }
        return null;
    }

    private void init() {
        if (Common.isDhizukuActive(requireActivity()) && !Dhizuku.isPermissionGranted()) {
            // Dhizukuが動作かつDhizukuへの権限がない
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) {
                    requireActivity().runOnUiThread(() -> {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            new DialogUtil(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(R.string.dialog_dhizuku_grant_permission)
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                                    .show();
                        } else {
                            new DialogUtil(requireActivity())
                                    .setCancelable(false)
                                    .setMessage(R.string.dialog_dhizuku_deny_permission)
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                                    .show();
                        }
                    });
                }
            });
            return;
        }

        if (Common.isDhizukuActive(requireActivity())) {
            // Dhizukuが動作
            if (Common.isVersionBelow(requireActivity(), DhizukuVariables.OFFICIAL_PACKAGE_NAME, Constants.MIN_VERSION_CODE_DHIZUKU)) {
                // v2.13.0 未満は切り捨て
                new DialogUtil(requireActivity())
                        .setCancelable(false)
                        .setMessage(getString(R.string.dialog_dhizuku_require_20))
                        .setPositiveButton(getString(R.string.dialog_common_ok), null)
                        .show();
            }
        }
        setListener();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ACTIVITY_INSTALL) {
            preSessionInstall.setEnabled(true);

            try {
                if (data == null) {
                    init();
                    return;
                }
                ArrayList<String> installFileArrayList = new ArrayList<>();

                if (trySetInstallData(data, installFileArrayList)) {
                    String installFileName = new File(installFileArrayList.get(0)).getName();

                    /* ファイルの拡張子 */
                    if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apk")) {
                        new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_SILENT, this);
                    } else if (ApkCopyTask.isSupported(installFileName)) {
                        apkCopyTask = new ApkCopyTask(installFileName);
                        apkCopyTask.execute(requireActivity(), apkCopyTaskListener(), installFileArrayList);
                    }
                } else {
                    new DialogUtil(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                            .show();
                }
            } catch (Exception ignored) {
                new DialogUtil(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_data))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
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
                return installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apk")
                        || ApkCopyTask.isSupported(installFileName);
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

    @NonNull
    private ApkInstallTask.Listener apkInstallTaskListener() {
        return Common.apkInstallTaskListener(new Common.TaskUi() {
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
        }, this::init);
    }

    private void showSplitSelectionDialog(ArrayList<String> entries, ArrayList<String> defaults) {
        ArrayList<String> ordered = new ArrayList<>(defaults);

        for (String entry : entries) {
            if (!defaults.contains(entry)) {
                ordered.add(entry);
            }
        }

        String[] names = new String[ordered.size()];
        boolean[] checked = new boolean[ordered.size()];

        for (int i = 0; i < ordered.size(); i++) {
            names[i] = new File(ordered.get(i)).getName();
            checked[i] = defaults.contains(ordered.get(i));
        }

        new DialogUtil(requireActivity())
                .setTitle(R.string.dialog_title_select_split)
                .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    ArrayList<String> installData = new ArrayList<>();

                    for (int i = 0; i < ordered.size(); i++) {
                        if (checked[i]) {
                            installData.add(ordered.get(i));
                        }
                    }

                    if (installData.isEmpty()) {
                        Toast.makeText(requireActivity(), R.string.dialog_error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    apkCopyTask.extract(requireActivity(), apkCopyTaskListener(), installData);
                })
                .setNegativeButton(R.string.dialog_common_cancel, null)
                .show();
    }

    @NonNull
    private ApkCopyTask.Listener apkCopyTaskListener() {
        return new ApkCopyTask.Listener() {

            AlertDialog alertDialog;
            ByteProgressHandler progressHandler;

            @Override
            public void onShow() {
                View view = getLayoutInflater().inflate(R.layout.view_progress, null);
                ProgressBar progressBar = view.findViewById(R.id.progress);
                AppCompatTextView textPercent = view.findViewById(R.id.progress_percent);
                AppCompatTextView textByte = view.findViewById(R.id.progress_byte);
                progressBar.setProgress(0);
                textPercent.setText(new StringBuilder(progressBar.getProgress()).append(getString(R.string.percent)));
                alertDialog = new DialogUtil(DeviceOwnerFunctionFragment.this.requireActivity())
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
                progressHandler.task = apkCopyTask;
                progressHandler.sendEmptyMessage(0);
            }

            @Override
            public void onAnalyzed(ArrayList<String> entries, ArrayList<String> defaults, boolean notOptimized) {
                if (!notOptimized) {
                    showSplitSelectionDialog(entries, defaults);
                    return;
                }

                new DialogUtil(requireActivity())
                        .setMessage(R.string.dialog_split_not_optimized)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_next, (dialog, which) -> showSplitSelectionDialog(entries, defaults))
                        .setNegativeButton(R.string.dialog_common_cancel, null)
                        .show();
            }

            @Override
            public void onSuccess(ArrayList<String> paths) {
                if (alertDialog != null) {
                    alertDialog.dismiss();
                }
                new ApkInstallTask().execute(requireActivity(), apkInstallTaskListener(), paths, Constants.REQUEST_INSTALL_SILENT, DeviceOwnerFunctionFragment.this);
            }

            @Override
            public void onFailure() {
                if (alertDialog != null) {
                    alertDialog.dismiss();
                }
                Common.deleteDirectoryAsync(requireActivity().getExternalCacheDir());
                new DialogUtil(DeviceOwnerFunctionFragment.this.requireActivity())
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(getString(R.string.dialog_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            init();
                        })
                        .show();
            }

            @Override
            public void onError(String message) {
                if (alertDialog != null) {
                    alertDialog.dismiss();
                }
                Common.deleteDirectoryAsync(requireActivity().getExternalCacheDir());
                new DialogUtil(DeviceOwnerFunctionFragment.this.requireActivity())
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            init();
                        })
                        .show();
            }

            @Override
            public void onProgressUpdate(String message) {
                if (alertDialog != null) {
                    alertDialog.setMessage(message);
                }
            }

            @Override
            public void onFileProgress(int current, int total) {
                if (progressHandler != null) {
                    progressHandler.obtainMessage(1, current, total).sendToTarget();
                }
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

    /* ローディングダイアログを表示する */
    private void showLoadingDialog(String message) {
        progressDialog = Common.showProgressSpinner(requireActivity(), message);
        progressSpinnerText = progressDialog.findViewById(R.id.view_progress_spinner_text);
    }

    /* ローディングダイアログを非表示にする */
    private void cancelLoadingDialog() {
        progressSpinnerText = null;
        Common.cancelDialog(progressDialog);
    }
}
