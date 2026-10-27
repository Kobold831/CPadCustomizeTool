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

package com.saradabar.cpadcustomizetool.view.activity;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuAllActive;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.MainActivity;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.DialogUtil;
import com.saradabar.cpadcustomizetool.view.flagment.dialog.InstallerListDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CheckActivity extends AppCompatActivity implements DownloadEventListener, InstallEventListener {

    AlertDialog progressDialog;
    AppCompatTextView progressPercentText;
    AppCompatTextView progressByteText;
    ProgressBar dialogProgressBar;

    private static boolean updateChecked = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean updateCheckFinished = false;

    private final Runnable updateCheckTimeout = () -> {
        if (updateCheckFinished) {
            return;
        }
        finishUpdateCheck();
        cancelLoadingDialog();
        showWelcome();
    };

    private final Common.TaskUi installTaskUi = new Common.TaskUi() {
        @Override
        public Context context() {
            return CheckActivity.this;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onDestroy() {
        finishUpdateCheck();
        super.onDestroy();
    }

    private void init() {
        // クラッシュしていたか判定
        if (Common.isFlagEnabled(this, Constants.KEY_FLAG_ERROR_CRASH)) {
            showCrashScreen();
            return;
        }

        if (Common.isFlagEnabled(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE)) {
            // 初回起動完了
            supportModelCheck();
        } else {
            // 初回起動未完了
            updateCheck();
        }
    }

    // クラッシュ画面表示
    private void showCrashScreen() {
        startActivity(new Intent(this, CrashScreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        overridePendingTransition(0, 0);
        finish();
    }

    // Welcome画面表示
    private void showWelcome() {
        if (Common.isFlagEnabled(this, Constants.KEY_FLAG_APP_WELCOME_COMPLETE)) {
            // 初回起動完了
            supportModelCheck();
        } else {
            // 初回起動未完了
            startActivityForResult(new Intent(this, WelcomeAppActivity.class), Constants.REQUEST_ACTIVITY_WELCOME);
            overridePendingTransition(0, 0);
            finish();
        }
    }

    /* アップデートチェック */
    private void updateCheck() {
        if (updateChecked) {
            showWelcome();
            return;
        }
        updateChecked = true;
        updateCheckFinished = false;
        handler.removeCallbacks(updateCheckTimeout);
        showLoadingDialog(getString(R.string.progress_state_connecting));
        handler.postDelayed(updateCheckTimeout, Constants.JSON_TIMEOUT_MS);
        new FileDownloadTask().execute(this, Constants.URL_CORE, new File(getExternalCacheDir(), Constants.CORE_JSON), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK, Constants.JSON_TIMEOUT_MS, Constants.JSON_TIMEOUT_MS);
    }

    private void finishUpdateCheck() {
        updateCheckFinished = true;
        handler.removeCallbacks(updateCheckTimeout);
    }

    /* ダウンロード完了 */
    @Override
    public void onDownloadComplete(int reqCode) {
        if (reqCode == Constants.REQUEST_DOWNLOAD_UPDATE_CHECK) {
            if (updateCheckFinished) {
                return;
            }
            finishUpdateCheck();
        }

        cancelLoadingDialog();
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK:// アップデートチェック要求
                try {
                    JSONObject updateInfo = Common.getUpdateInfo(new File(getExternalCacheDir(), Constants.CORE_JSON));

                    if (updateInfo.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        showUpdateDialog(updateInfo.getString("url"));
                    } else {
                        showWelcome();
                    }
                } catch (JSONException | IOException ignored) {
                    showWelcome();
                }
                break;
            case Constants.REQUEST_DOWNLOAD_APK:// APKダウンロード要求
                switch (Common.getFlag(this, Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE)) {
                    case Constants.UPDATE_MODE_PACKAGE_INSTALLER:// パッケージインストーラー
                        startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case Constants.UPDATE_MODE_ADB:// Adb
                        try {
                            JSONObject updateInfo = Common.getUpdateInfo(new File(getExternalCacheDir(), Constants.CORE_JSON));

                            new DialogUtil(this)
                                    .setCancelable(false)
                                    .setTitle(getString(R.string.dialog_title_error))
                                    .setMessage(getString(R.string.dialog_no_installer, updateInfo.getString("url")))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                                    .show();
                        } catch (JSONException | IOException ignored) {
                            Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                            init();
                        }
                        break;
                    case Constants.UPDATE_MODE_DCHA:// Dcha
                        new DchaInstallTask().execute(this, dchaInstallTaskListener(), new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath());
                        break;
                    case Constants.UPDATE_MODE_DEVICE_OWNER:// デバイスオーナー
                        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

                        if (!dpm.isDeviceOwnerApp(getPackageName()) || Common.isCT2()) {
                            Common.setFlag(this, Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE);
                            new DialogUtil(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), new ArrayList<>(List.of(new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                    case Constants.UPDATE_MODE_DHIZUKU:// Dhizuku
                        if (!isDhizukuAllActive(this) || Common.isCT2()) {
                            Common.setFlag(this, Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE);
                            new DialogUtil(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), new ArrayList<>(List.of(new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                }
                break;
        }
    }

    @NonNull
    private DchaInstallTask.Listener dchaInstallTaskListener() {
        return Common.dchaInstallTaskListener(installTaskUi, this::init);
    }

    /* ダウンロードエラー */
    @Override
    public void onDownloadError(int reqCode) {
        if (reqCode == Constants.REQUEST_DOWNLOAD_UPDATE_CHECK) {
            if (updateCheckFinished) {
                return;
            }
            finishUpdateCheck();
        }

        cancelLoadingDialog();
        showWelcome();
    }

    /* サーバー接続エラー */
    @Override
    public void onConnectionError(int reqCode) {
        if (reqCode == Constants.REQUEST_DOWNLOAD_UPDATE_CHECK) {
            if (updateCheckFinished) {
                return;
            }
            finishUpdateCheck();
        }

        cancelLoadingDialog();
        showWelcome();
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
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

    /* アップデートダイアログ */
    private void showUpdateDialog(String downloadFileUrl) {
        View view = getLayoutInflater().inflate(R.layout.view_update, null);

        new DialogUtil(this)
                .setTitle(R.string.dialog_title_update)
                .setMessage(R.string.dialog_install_mode)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    FileDownloadTask fileDownloadTask = new FileDownloadTask();
                    fileDownloadTask.execute(this, downloadFileUrl, new File(getExternalCacheDir(), Constants.DOWNLOAD_APK), Constants.REQUEST_DOWNLOAD_APK);
                    ProgressHandler progressHandler = new ProgressHandler(Looper.getMainLooper());
                    progressHandler.fileDownloadTask = fileDownloadTask;
                    progressHandler.sendEmptyMessage(0);
                    View progressView = getLayoutInflater().inflate(R.layout.view_progress, null);
                    progressPercentText = progressView.findViewById(R.id.progress_percent);
                    progressByteText = progressView.findViewById(R.id.progress_byte);
                    dialogProgressBar = progressView.findViewById(R.id.progress);
                    progressPercentText.setText(getString(R.string.progress_percent, 0));
                    progressByteText.setText(R.string.progress_waiting);
                    dialogProgressBar.setIndeterminate(true);
                    dialogProgressBar.setMax(Constants.PERCENT_MAX);
                    progressDialog = new DialogUtil(this).setCancelable(false).setView(progressView).create();
                    progressDialog.setMessage("");
                    progressDialog.show();
                })
                .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> showWelcome())
                .setNeutralButton(R.string.activity_start, (dialog, which) -> new InstallerListDialogFragment(Constants.INSTALLER_LIST_FROM_MAIN, this::init).show(getSupportFragmentManager(), ""))
                .show();
    }


    /* ローディングダイアログを表示する */
    private void showLoadingDialog(String message) {
        progressDialog = Common.showProgressSpinner(this, message);
    }

    /* ローディングダイアログを非表示にする */
    private void cancelLoadingDialog() {
        Common.cancelDialog(progressDialog);
    }

    /* 端末チェック */
    private void supportModelCheck() {
        if (BuildConfig.DEBUG) {
            // デバッグビルド
            checkDchaService();
            return;
        }

        for (String model : Constants.LIST_MODEL) {
            if (Build.MODEL.equals(model)) {
                // サポートデバイス一致
                checkDchaService();
                return;
            }
        }
        // すべて一致しなかったので、デバイスサポート対象外エラー
        supportModelError();
    }

    /* 端末チェックエラー */
    private void supportModelError() {
        new DialogUtil(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_error)
                .setMessage(R.string.dialog_error_check_device)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAffinity())
                .show();
    }

    /* DchaService動作チェック */
    private void checkDchaService() {
        if (Common.isFlagEnabled(this, Constants.KEY_FLAG_DCHA_FUNCTION)) {
            // dchaを使用する設定
            if (!Common.isDchaActive(this)) {
                // Dchaが動作していない
                new DialogUtil(this)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_error_check_dcha)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            // Dchaを使用しない設定に変更
                            Common.setFlagEnabled(this, Constants.KEY_FLAG_DCHA_FUNCTION, false);
                            init();
                        })
                        .show();
                return;
            }
        }
        confCheck();
    }

    private void confCheck() {
        if (Common.isFlagEnabled(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE)) {
            // 初期設定完了
            if (isPermissionCheck()) {
                // 権限チェックOK
                checkNormalEnv();
            }
        } else {
            // 初期設定未完了
            WarningDialog();
        }
    }

    /* 初回起動お知らせ */
    private void WarningDialog() {
        new DialogUtil(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_notice_start)
                .setMessage(R.string.dialog_app_start_message)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    Common.setFlagEnabled(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, true);
                    checkNormalEnv();
                })
                .show();
    }

    private void checkNormalEnv() {
        if (!Common.isFlagEnabled(this, Constants.KEY_FLAG_ALREADY_DIALOG_NORMAL_ENV) && !isDchaInstalled(this) && !Common.isFlagEnabled(this, Constants.KEY_FLAG_NORMAL_ENV) ||
                !Common.isFlagEnabled(this, Constants.KEY_FLAG_ALREADY_DIALOG_NORMAL_ENV) && !isBenesseHomeInstalled() && !Common.isFlagEnabled(this, Constants.KEY_FLAG_NORMAL_ENV)) {
            // ダイアログ表示履歴なしかつDcha がインストールされていないかつ通常環境モードが無効、またはダイアログ表示履歴なしかつ勉強ホームがインストールされていないかつ通常環境モードが無効
            new DialogUtil(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_normal_env)
                    .setMessage(R.string.dialog_normal_env_message)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        Common.setFlagEnabled(this, Constants.KEY_FLAG_NORMAL_ENV, true);
                        showMainActivity();
                    })
                    .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> {
                        Common.setFlagEnabled(this, Constants.KEY_FLAG_ALREADY_DIALOG_NORMAL_ENV, true);
                        showMainActivity();
                    })
                    .show();
        } else {
            showMainActivity();
        }
    }

    private void showMainActivity() {
        // 通常環境モードに設定されている場合に、一部の機能を停止
        Common.setNormalEnv(this);
        //　メイン画面表示
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        overridePendingTransition(0, 0);
        finish();
    }

    /* システム設定変更権限か付与されているか確認 */
    private boolean isPermissionCheck() {
        if (!isWriteSystemPermissionCheck()) {
            new DialogUtil(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_grant_permission)
                    .setMessage(R.string.dialog_error_check_permission)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", getPackageName(), null)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), Constants.REQUEST_ACTIVITY_PERMISSION);
                        }
                    })
                    .setNeutralButton(R.string.dialog_common_cancel, (dialog, which) -> init())
                    .show();
            return false;
        }

        if (!isAccessExternalStoragePermissionCheck()) {
            return false;
        }
        return isAccessDchaServicePermissionCheck();
    }

    /* システム設定変更権限チェック */
    private boolean isWriteSystemPermissionCheck() {
        boolean canWrite = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canWrite = Settings.System.canWrite(this);
        }
        return canWrite;
    }

    /* ストレージアクセス権限チェック */
    private boolean isAccessExternalStoragePermissionCheck() {
        boolean canWrite = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canWrite = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!canWrite) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_PERMISSION_STORAGE);
            }
        }
        return canWrite;
    }

    /* DchaServiceアクセス権限チェック */
    private boolean isAccessDchaServicePermissionCheck() {
        boolean canWrite = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isDchaInstalled(this)) {
                canWrite = checkSelfPermission(Constants.DCHA_ACCESS_SYSTEM) == PackageManager.PERMISSION_GRANTED;
                if (!canWrite) {
                    requestPermissions(new String[]{Constants.DCHA_ACCESS_SYSTEM}, Constants.REQUEST_PERMISSION_DCHA_SYSTEM);
                }
            }
        }
        return canWrite;
    }

    private boolean isDchaInstalled(@NonNull Context context) {
        try {
            context.getPackageManager().getPackageInfo(Constants.PKG_DCHA_SERVICE, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private boolean isBenesseHomeInstalled() {
        try {
            getPackageManager().getPackageInfo(Constants.PKG_SHO_HOME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        try {
            getPackageManager().getPackageInfo(Constants.PKG_CHU_HOME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.REQUEST_PERMISSION_STORAGE && grantResults.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    new DialogUtil(this)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_request_storage_permission)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_PERMISSION_STORAGE);
                                }
                            })
                            .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> init())
                            .show();
                    return;
                }
            }
        }

        if (requestCode == Constants.REQUEST_PERMISSION_DCHA_SYSTEM && grantResults.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Constants.DCHA_ACCESS_SYSTEM) != PackageManager.PERMISSION_GRANTED) {
                    new DialogUtil(this)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_request_dcha_permission)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                if (checkSelfPermission(Constants.DCHA_ACCESS_SYSTEM) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Constants.DCHA_ACCESS_SYSTEM}, Constants.REQUEST_PERMISSION_DCHA_SYSTEM);
                                }
                            })
                            .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> init())
                            .show();
                    return;
                }
            }
        }

        if (isPermissionCheck()) {
            init();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ACTIVITY_UPDATE:
            case Constants.REQUEST_ACTIVITY_WELCOME:
            case Constants.REQUEST_ACTIVITY_PERMISSION:
                init();
                break;
        }
    }

    private ApkInstallTask.Listener apkInstallTaskListener() {
        return Common.apkInstallTaskListener(installTaskUi, this::init);
    }

    @Override
    public void onInstallSuccess(int reqCode) {
        apkInstallTaskListener().onSuccess();
    }

    @Override
    public void onInstallFailure(int reqCode, String str) {
        apkInstallTaskListener().onFailure(str);
    }

    @Override
    public void onInstallError(int reqCode, String str) {
        apkInstallTaskListener().onError(str);
    }
}
