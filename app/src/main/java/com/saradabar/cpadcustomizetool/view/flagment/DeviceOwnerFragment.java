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
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.rosan.dhizuku.api.DhizukuUserServiceArgs;

import com.rosan.dhizuku.shared.DhizukuVariables;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ByteProgressHandler;
import com.saradabar.cpadcustomizetool.data.receiver.DeviceAdminReceiver;
import com.saradabar.cpadcustomizetool.data.service.DhizukuService;
import com.saradabar.cpadcustomizetool.data.service.IDhizukuService;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.ApkSCopyTask;
import com.saradabar.cpadcustomizetool.data.task.ApkMCopyTask;
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
import java.util.concurrent.Executors;

/** @noinspection deprecation*/
public class DeviceOwnerFragment extends PreferenceFragmentCompat implements InstallEventListener {

    AlertDialog progressDialog, waitForServiceDialog;

    XApkCopyTask xApkCopyTask;
    ApkMCopyTask apkMCopyTask;
    ApkSCopyTask apkSCopyTask;

    public Preference preUninstallBlock,
            preSessionInstall,
            preAbandonSession,
            preClrDevOwn,
            preNowSetOwnPkg,
            preSessionInstallNotice;

    SwitchPreferenceCompat swPrePermissionFrc;

    IDhizukuService mDhizukuService;
    DhizukuUserServiceArgs dhizukuUserServiceArgs;
    ServiceConnection dServiceConnection;

    Listener listener;

    boolean isActiveInstallTask = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(R.string.dialog_service_connecting);
        waitForServiceDialog = new AlertDialog.Builder(requireActivity()).setCancelable(false).setView(view).create();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pre_owner);

        preUninstallBlock = findPreference("pre_owner_uninstall_block");
        swPrePermissionFrc = findPreference("pre_owner_permission_frc");
        preSessionInstall = findPreference("pre_owner_session_install");
        preAbandonSession = findPreference("pre_owner_abandon_session");
        preClrDevOwn = findPreference("pre_owner_clr_dev_own");
        preNowSetOwnPkg = findPreference("pre_owner_now_set_own_pkg");
        preSessionInstallNotice = findPreference("pre_owner_session_install_notice");

        /* 初期化 */
        initialize();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Common.debugLog("onDestroy");

        if (dhizukuUserServiceArgs != null) {
            try {
                Dhizuku.stopUserService(dhizukuUserServiceArgs);
            } catch (IllegalStateException ignored) {
            }
        }

        if (dServiceConnection != null) {
            try {
                Dhizuku.unbindUserService(dServiceConnection);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Common.debugLog("onPause");

        if (dhizukuUserServiceArgs != null) {
            try {
                Dhizuku.stopUserService(dhizukuUserServiceArgs);
            } catch (IllegalStateException ignored) {
            }
        }

        if (dServiceConnection != null) {
            try {
                Dhizuku.unbindUserService(dServiceConnection);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private void finish() {
        requireActivity().finish();
        requireActivity().overridePendingTransition(0, 0);
        startActivity(requireActivity().getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    private void setListener() {
        preUninstallBlock.setOnPreferenceClickListener(preference -> {
            // 遷移前にdhizukuから切断
            if (dhizukuUserServiceArgs != null) {
                try {
                    Dhizuku.stopUserService(dhizukuUserServiceArgs);
                } catch (IllegalStateException ignored) {
                }
            }

            if (dServiceConnection != null) {
                try {
                    Dhizuku.unbindUserService(dServiceConnection);
                } catch (IllegalStateException ignored) {
                }
            }

            try {
                startActivity(new Intent(requireActivity(), UninstallBlockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
            }
            return false;
        });

        swPrePermissionFrc.setOnPreferenceChangeListener((preference, o) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if ((boolean) o) {
                    if (Common.isDhizukuActive(requireActivity())) {
                        if (tryBindDhizukuService()) {
                            try {
                                mDhizukuService.setPermissionPolicy(DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
                            } catch (DeadObjectException ignored) {
                                new AlertDialog.Builder(requireActivity())
                                        .setCancelable(false)
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage("Dhizuku と正常に通信できませんでした。Dhizuku のメイン画面から右上の︙を押して、”停止”をしてから再度やり直してください。")
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                            try {
                                                finish();
                                                startActivity(requireActivity().getPackageManager().getLaunchIntentForPackage(DhizukuVariables.OFFICIAL_PACKAGE_NAME));
                                                android.os.Process.killProcess(android.os.Process.myPid());
                                            } catch (ActivityNotFoundException ignored1) {
                                            }
                                        })
                                        .show();
                                return false;
                            } catch (Exception ignored) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
                        dpm.setPermissionPolicy(new ComponentName(requireActivity(), DeviceAdminReceiver.class), DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT);
                    }

                    for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
                        /* ユーザーアプリか確認 */
                        if (app.sourceDir.startsWith("/data/app/")) {
                            setPermissionGrantState(app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                        }
                    }
                    swPrePermissionFrc.setSummary(getString(R.string.pre_owner_sum_permission_forced));
                } else {
                    if (Common.isDhizukuActive(requireActivity())) {
                        if (tryBindDhizukuService()) {
                            try {
                                mDhizukuService.setPermissionPolicy(DevicePolicyManager.PERMISSION_POLICY_PROMPT);
                            } catch (DeadObjectException ignored) {
                                new AlertDialog.Builder(requireActivity())
                                        .setCancelable(false)
                                        .setTitle(R.string.dialog_title_error)
                                        .setMessage("Dhizuku と正常に通信できませんでした。Dhizuku のメイン画面から右上の︙を押して、”停止”をしてから再度やり直してください。")
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                            try {
                                                finish();
                                                startActivity(requireActivity().getPackageManager().getLaunchIntentForPackage(DhizukuVariables.OFFICIAL_PACKAGE_NAME));
                                                android.os.Process.killProcess(android.os.Process.myPid());
                                            } catch (ActivityNotFoundException ignored1) {
                                            }
                                        })
                                        .show();
                                return false;
                            } catch (Exception ignored) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
                        dpm.setPermissionPolicy(new ComponentName(requireActivity(), DeviceAdminReceiver.class), DevicePolicyManager.PERMISSION_POLICY_PROMPT);
                    }

                    for (ApplicationInfo app : requireActivity().getPackageManager().getInstalledApplications(0)) {
                        /* ユーザーアプリか確認 */
                        if (app.sourceDir.startsWith("/data/app/")) {
                            setPermissionGrantState(app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_DEFAULT);
                        }
                    }
                    swPrePermissionFrc.setSummary(getString(R.string.pre_owner_sum_permission_default));
                }
            }
            return true;
        });

        preSessionInstall.setOnPreferenceClickListener(preference -> {
            try {
                preSessionInstall.setEnabled(false);
                startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/*"}).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true), ""), Constants.REQUEST_ACTIVITY_INSTALL);

                // インストール中状態を設定
                isActiveInstallTask = true;

                // Dhizuku 切断
                if (dhizukuUserServiceArgs != null) {
                    try {
                        Dhizuku.stopUserService(dhizukuUserServiceArgs);
                    } catch (IllegalStateException ignored) {
                    }
                }

                if (dServiceConnection != null) {
                    try {
                        Dhizuku.unbindUserService(dServiceConnection);
                    } catch (IllegalStateException ignored) {
                    }
                }
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

            Toast.makeText(requireActivity(), R.string.toast_session_dest, Toast.LENGTH_SHORT).show();
            return false;
        });

        preClrDevOwn.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.dialog_question_owner)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        if (Common.isDhizukuActive(requireActivity())) {
                            if (tryBindDhizukuService()) {
                                try {
                                    mDhizukuService.clearDeviceOwnerApp(DhizukuVariables.OFFICIAL_PACKAGE_NAME);
                                    finish();
                                } catch (DeadObjectException ignored) {
                                    new AlertDialog.Builder(requireActivity())
                                            .setCancelable(false)
                                            .setTitle(R.string.dialog_title_error)
                                            .setMessage("Dhizuku と正常に通信できませんでした。Dhizuku のメイン画面から右上の︙を押して、”停止”をしてから再度やり直してください。")
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> {
                                                try {
                                                    finish();
                                                    startActivity(requireActivity().getPackageManager().getLaunchIntentForPackage(DhizukuVariables.OFFICIAL_PACKAGE_NAME));
                                                    android.os.Process.killProcess(android.os.Process.myPid());
                                                } catch (ActivityNotFoundException ignored1) {
                                                }
                                            })
                                            .show();
                                } catch (Exception ignored) {
                                }
                            }
                        } else {
                            DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
                            dpm.clearDeviceOwnerApp(requireActivity().getPackageName());
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, null)
                    .show();
            return false;
        });

        preSessionInstallNotice.setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireActivity())
                    .setView(R.layout.view_owner_session_notice)
                    .setPositiveButton(getString(R.string.dialog_common_ok), null)
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
                        switch (dpm.getPermissionPolicy(Constants.DHIZUKU_COMPONENT)) {
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

    @Nullable
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

    /* 表示 */
    @Override
    public void onStart() {
        super.onStart();
        Common.debugLog("onStart");

        //インストール中なら終了
        if (isActiveInstallTask) {
            return;
        }

        if (waitForServiceDialog.isShowing()) {
            return;
        }

        // Admin Component を変えるための措置
        DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (dpm.isDeviceOwnerApp(requireActivity().getPackageName())
                && !dpm.isAdminActive(new ComponentName(requireActivity(), DeviceAdminReceiver.class))
                && !dpm.isAdminActive(new ComponentName(requireActivity(), "com.saradabar.cpadcustomizetool.Receiver.AdministratorReceiver"))) {
            new AlertDialog.Builder(requireActivity())
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_error)
                    .setMessage("新しいバージョンは Device Owner の再設定が必要になりました。\nDevice Owner はこのデバイスに付与されていますが、Device Owner を ADB で再度有効にする必要があります。\n\n”OK” を押すと、Device Owner は解除されます。\n詳細は、メイン画面から ”アプリのお知らせ” を開いてください。")
                    .setPositiveButton("OK", (dialog, which) -> {
                        dpm.clearDeviceOwnerApp(requireActivity().getPackageName());
                        finish();
                    })
                    .show();
            return;
        }
        restart();
    }

    private void restart() {
        // インストール中状態を解除
        isActiveInstallTask = false;

        if (Dhizuku.init(requireActivity())) {
            if (!Dhizuku.isPermissionGranted()) {
                Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                    @Override
                    public void onRequestPermission(int grantResult) {
                        requireActivity().runOnUiThread(() -> {
                            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                new AlertDialog.Builder(requireActivity())
                                        .setCancelable(false)
                                        .setMessage(R.string.dialog_dhizuku_grant_permission)
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                                        .show();
                            } else {
                                new AlertDialog.Builder(requireActivity())
                                        .setCancelable(false)
                                        .setMessage(R.string.dialog_dhizuku_deny_permission)
                                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> setListener())
                                        .show();
                            }
                        });
                    }
                });
                return;
            }
        }

        if (Common.isDhizukuActive(requireActivity())) {
            try {
                if (requireActivity().getPackageManager().getPackageInfo(DhizukuVariables.OFFICIAL_PACKAGE_NAME, 0).versionCode < 12) {
                    new AlertDialog.Builder(requireActivity())
                            .setCancelable(false)
                            .setMessage(getString(R.string.dialog_dhizuku_require_12))
                            .setPositiveButton(getString(R.string.dialog_common_ok), null)
                            .show();
                }
            } catch (Exception ignored) {
            }

            waitForServiceDialog.show();
            Common.debugLog("waitForServiceDialog.show");

            listener = new Listener() {
                @Override
                public void onSuccess() {
                    Common.debugLog("onSuccess");
                    if (waitForServiceDialog.isShowing()) {
                        waitForServiceDialog.cancel();
                    }

                    if (mDhizukuService == null) {
                        new AlertDialog.Builder(requireActivity())
                                .setCancelable(false)
                                .setMessage(R.string.dialog_error_no_dhizuku)
                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                                .show();
                    }
                    setListener();
                }

                @Override
                public void onFailure() {
                    Common.debugLog("onFailure");
                    if (waitForServiceDialog.isShowing()) {
                        waitForServiceDialog.cancel();
                    }

                    new AlertDialog.Builder(requireActivity())
                            .setCancelable(false)
                            .setMessage(R.string.dialog_error_no_dhizuku)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                            .show();
                }
            };

            // サービスに接続したら発火させる
            dhizukuUserServiceArgs = new DhizukuUserServiceArgs(new ComponentName(requireActivity(), DhizukuService.class));
            dServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder iBinder) {
                    mDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
                    // サービス接続完了
                    listener.onSuccess();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };

            Executors.newSingleThreadExecutor().submit(() -> new Thread(() -> {
                if (!Dhizuku.bindUserService(dhizukuUserServiceArgs, dServiceConnection)) {
                    // サービス接続失敗
                    listener.onFailure();
                }
            }).start());
        } else {
            setListener();
        }
    }

    public interface Listener {
        void onSuccess();
        void onFailure();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ArrayList<String> installFileArrayList = new ArrayList<>();

        if (requestCode == Constants.REQUEST_ACTIVITY_INSTALL) {
            preSessionInstall.setEnabled(true);
            try {
                if (data == null) {
                    Common.debugLog("data == null");
                    restart();
                    return;
                }

                if (trySetInstallData(data, installFileArrayList)) {
                    //noinspection SequencedCollectionMethodCanBeUsed
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
                    } else if (installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apks")) {
                        apkSCopyTask = new ApkSCopyTask();
                        apkSCopyTask.execute(requireActivity(), apkSListener(), installFileArrayList);
                    }
                } else {
                    new AlertDialog.Builder(requireActivity())
                            .setMessage(getString(R.string.dialog_error_no_file_data))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> restart())
                            .show();
                }
            } catch (Exception ignored) {
                new AlertDialog.Builder(requireActivity())
                        .setMessage(getString(R.string.dialog_error_no_file_data))
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> restart())
                        .show();
            }
        }
    }

    /** @noinspection SequencedCollectionMethodCanBeUsed*/
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
                        || installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".xapk")
                        || installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apkm")
                        || installFileName.substring(installFileName.lastIndexOf(".")).equalsIgnoreCase(".apks");
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
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
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
                        .setMessage(getString(R.string.dialog_failure_silent_install) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
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
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
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
                AppCompatTextView textPercent = view.findViewById(R.id.progress_percent);
                AppCompatTextView textByte = view.findViewById(R.id.progress_byte);

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
                // インストール処理でdhizukuに接続するのでここでは再接続しない
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
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(getString(R.string.dialog_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
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
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
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
                AppCompatTextView textPercent = view.findViewById(R.id.progress_percent);
                AppCompatTextView textByte = view.findViewById(R.id.progress_byte);

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
                // インストール処理でdhizukuに接続するのでここでは再接続しない
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
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(getString(R.string.dialog_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
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
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
                        .show();
            }

            @Override
            public void onProgressUpdate(String message) {
                alertDialog.setMessage(message);
            }
        };
    }

        public ApkSCopyTask.Listener apkSListener() {
        return new ApkSCopyTask.Listener() {

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
                // インストール処理でdhizukuに接続するのでここでは再接続しない
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
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(getString(R.string.dialog_failure))
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
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
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // ok 押下後に再接続する
                            restart();
                        })
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
                mDhizukuService = IDhizukuService.Stub.asInterface(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        });
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void setPermissionGrantState(String packageName, int grantState) {
        if (Common.isDhizukuActive(requireActivity())) {
            if (tryBindDhizukuService()) {
                try {
                    for (String permission : getRuntimePermissions(packageName)) {
                        mDhizukuService.setPermissionGrantState(packageName, permission, grantState);
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            DevicePolicyManager dpm = (DevicePolicyManager) requireActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
            for (String permission : getRuntimePermissions(packageName)) {
                dpm.setPermissionGrantState(new ComponentName(requireActivity(), DeviceAdminReceiver.class), packageName, permission, grantState);
            }
        }
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.M)
    private String[] getRuntimePermissions(String packageName) {
        return new ArrayList<>(Arrays.asList(getRequiredPermissions(packageName))).toArray(new String[0]);
    }

    @NonNull
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
        AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
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
}
