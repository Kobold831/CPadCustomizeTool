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

package com.saradabar.cpadcustomizetool;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;

import com.rosan.dhizuku.shared.DhizukuVariables;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.CrashScreenActivity;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;
import com.saradabar.cpadcustomizetool.view.activity.WebViewActivity;
import com.saradabar.cpadcustomizetool.view.activity.WelAppActivity;
import com.saradabar.cpadcustomizetool.view.views.UpdateModeListView;

import com.stephentuso.welcome.WelcomeHelper;

import org.json.JSONException;
import org.json.JSONObject;

import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @noinspection deprecation*/
public class MainActivity extends AppCompatActivity implements DownloadEventListener, InstallEventListener {

    AlertDialog progressDialog;
    AppCompatTextView progressPercentText;
    AppCompatTextView progressByteText;
    ProgressBar dialogProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        /* 前回クラッシュしているかどうか */
        if (Preferences.load(this, Constants.KEY_FLAG_ERROR_CRASH, Constants.DEF_BOOL)) {
            // クラッシュ画面表示
            showCrashScreen();
            return;
        }

        /* 初回起動が完了しているかどうか */
        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, Constants.DEF_BOOL)) {
            /* サポート端末か確認 */
            if (supportModelCheck()) {
                /* DchaServiceを確認 */
                checkDchaService();
            } else {
                supportModelError();
            }
        } else {
            // 初回起動
            updateCheck();
        }
    }

    private void showCrashScreen() {
        startActivity(new Intent(this, CrashScreenActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        overridePendingTransition(0, 0);
        finish();
    }

    private void showWelcome() {
        if (Preferences.load(this, Constants.KEY_FLAG_APP_WELCOME_COMPLETE, Constants.DEF_BOOL)) {
            /* サポート端末か確認 */
            if (supportModelCheck()) {
                /* DchaServiceを確認 */
                checkDchaService();
            } else {
                supportModelError();
            }
        } else {
            new WelcomeHelper(this, WelAppActivity.class).forceShow();
            overridePendingTransition(0, 0);
            finish();
        }
    }

    /* アップデートチェック */
    private void updateCheck() {
        showLoadingDialog(getString(R.string.progress_state_connecting));
        new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(getExternalCacheDir(), Constants.CHECK_JSON), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK);
    }

    /* ダウンロード完了 */
    @Override
    public void onDownloadComplete(int reqCode) {
        cancelLoadingDialog();
        switch (reqCode) {
            /* アップデートチェック要求の場合 */
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK:
                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(getExternalCacheDir(), Constants.CHECK_JSON));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONObject jsonObj3 = jsonObj2.getJSONObject("update");

                    if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        showUpdateDialog(jsonObj3.getString("description"), jsonObj3.getString("url"));
                    } else {
                        showWelcome();
                    }
                } catch (JSONException | IOException ignored) {
                    onDownloadError(reqCode);
                }
                break;
            /* APKダウンロード要求の場合 */
            case Constants.REQUEST_DOWNLOAD_APK:
                switch (Preferences.load(this, Constants.KEY_INT_UPDATE_MODE, 1)) {
                    case 0:
                        startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case 1:
                        try {
                            JSONObject jsonObj1 = Common.parseJson(new File(getExternalCacheDir(), Constants.CHECK_JSON));
                            JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                            JSONObject jsonObj3 = jsonObj2.getJSONObject("update");

                            new AlertDialog.Builder(this)
                                    .setCancelable(false)
                                    .setTitle(getString(R.string.dialog_title_error))
                                    .setMessage(getString(R.string.dialog_no_installer, jsonObj3.getString("url")))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                                    .show();
                        } catch (JSONException | IOException ignored) {
                            Toast.makeText(this, R.string.dialog_error, Toast.LENGTH_SHORT).show();
                            init();
                        }
                        break;
                    case 2:
                        new DchaInstallTask().execute(this, dchaInstallTaskListener(), new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath());
                        break;
                    case 3:
                        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

                        if (!dpm.isDeviceOwnerApp(getPackageName())) {
                            Preferences.save(this, Constants.KEY_INT_UPDATE_MODE, 1);
                            new AlertDialog.Builder(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), new ArrayList<>(List.of(new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                    case 4:
                        if (!isDhizukuActive(this) || Common.isCT2()) {
                            Preferences.save(this, Constants.KEY_INT_UPDATE_MODE, 1);
                            new AlertDialog.Builder(this)
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
        return new DchaInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                showLoadingDialog(getResources().getString(R.string.progress_state_installing));
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                cancelLoadingDialog();
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                cancelLoadingDialog();
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.dialog_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                        .show();
            }
        };
    }

    /* ダウンロードエラー */
    @Override
    public void onDownloadError(int reqCode) {
        cancelLoadingDialog();
        showWelcome();
    }

    /* サーバー接続エラー */
    @Override
    public void onConnectionError(int reqCode) {
        cancelLoadingDialog();
        showWelcome();
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
        progressPercentText.setText(new StringBuilder(String.valueOf(progress)).append("%"));
        progressByteText.setText(new StringBuilder(String.valueOf(currentByte)).append(" MB").append("/").append(totalByte).append(" MB"));
        dialogProgressBar.setProgress(progress);
        progressDialog.setMessage(new StringBuilder(getString(R.string.progress_state_download_file)));
    }

    /* アップデートダイアログ */
    private void showUpdateDialog(String str, String downloadFileUrl) {
        View view = getLayoutInflater().inflate(R.layout.view_update, null);
        AppCompatTextView tv = view.findViewById(R.id.update_information);
        tv.setText(str);
        view.findViewById(R.id.update_info_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_UPDATE_INFO).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
            }
        });

        new AlertDialog.Builder(this)
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
                    progressPercentText.setText("");
                    progressByteText = progressView.findViewById(R.id.progress_byte);
                    progressByteText.setText("");
                    dialogProgressBar = progressView.findViewById(R.id.progress);
                    dialogProgressBar.setProgress(0);
                    progressDialog = new AlertDialog.Builder(this).setCancelable(false).setView(progressView).create();
                    progressDialog.setMessage("");
                    progressDialog.show();
                })
                .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> showWelcome())
                .setNeutralButton("設定", (dialog, which) -> {
                    View v = getLayoutInflater().inflate(R.layout.layout_update_list, null);
                    List<UpdateModeListView.AppData> dataList = new ArrayList<>();
                    int i = 0;

                    for (String str1 : Constants.LIST_UPDATE_MODE) {
                        UpdateModeListView.AppData data = new UpdateModeListView.AppData();
                        data.label = str1;
                        data.updateMode = i;
                        dataList.add(data);
                        i++;
                    }
                    ListView listView = v.findViewById(R.id.update_list);
                    listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                    listView.setAdapter(new UpdateModeListView.AppListAdapter(v.getContext(), dataList));
                    listView.setOnItemClickListener((parent, mView, position, id) -> {
                        switch (position) {
                            case 0:
                                if (Common.isCT2() || Common.isCT3()) {
                                    Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                } else {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_no_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, null)
                                            .show();
                                }
                                break;
                            case 1:
                                Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                listView.invalidateViews();
                                break;
                            case 2:
                                try {
                                    if (Common.isDchaActive(this) &&
                                            getPackageManager().getPackageInfo(Constants.PKG_DCHA_SERVICE, 0).versionCode > 4) {
                                        Preferences.save(this, Constants.KEY_INT_UPDATE_MODE, (int) id);
                                        listView.invalidateViews();
                                    } else {
                                        new AlertDialog.Builder(this)
                                                .setMessage(getString(R.string.dialog_error_no_mode))
                                                .setPositiveButton(R.string.dialog_common_ok, null)
                                                .show();
                                    }
                                } catch (PackageManager.NameNotFoundException ignored) {
                                    new AlertDialog.Builder(this)
                                            .setMessage(getString(R.string.dialog_error_no_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, null)
                                            .show();
                                }
                                break;
                            case 3:
                                if (((DevicePolicyManager) v.getContext().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(v.getContext().getPackageName()) &&
                                        !Common.isCT2()) {
                                    Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                } else {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_no_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, null)
                                            .show();
                                }
                                break;
                            case 4:
                                if (Common.isCT2()) {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_no_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, null)
                                            .show();
                                    return;
                                }

                                if (Common.isDhizukuActive(v.getContext())) {
                                    try {
                                        if (getPackageManager().getPackageInfo(DhizukuVariables.OFFICIAL_PACKAGE_NAME, 0).versionCode < 12) {
                                            new AlertDialog.Builder(v.getContext())
                                                    .setCancelable(false)
                                                    .setMessage(getString(R.string.dialog_dhizuku_require_12))
                                                    .setPositiveButton(getString(R.string.dialog_common_ok), null)
                                                    .show();
                                        }
                                        Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                        listView.invalidateViews();
                                    } catch (PackageManager.NameNotFoundException ignored) {
                                        new AlertDialog.Builder(v.getContext())
                                                .setMessage(getString(R.string.dialog_error_no_mode))
                                                .setPositiveButton(R.string.dialog_common_ok, null)
                                                .show();
                                    }
                                } else {
                                    if (!Dhizuku.init(v.getContext())) {
                                        new AlertDialog.Builder(v.getContext())
                                                .setMessage(getString(R.string.dialog_error_no_mode))
                                                .setPositiveButton(R.string.dialog_common_ok, null)
                                                .show();
                                        return;
                                    }
                                    Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                                        @Override
                                        public void onRequestPermission(int grantResult) {
                                            runOnUiThread(() -> {
                                                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                                    Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                                    listView.invalidateViews();
                                                } else {
                                                    new AlertDialog.Builder(v.getContext())
                                                            .setMessage(R.string.dialog_dhizuku_deny_permission)
                                                            .setPositiveButton(R.string.dialog_common_ok, null)
                                                            .show();
                                                }
                                            });
                                        }
                                    });
                                }
                                break;
                        }
                    });

                    new AlertDialog.Builder(v.getContext())
                            .setCancelable(false)
                            .setView(v)
                            .setTitle(getString(R.string.dialog_title_select_mode))
                            .setPositiveButton(R.string.dialog_common_ok, (dialog1, which1) -> init())
                            .show();
                })
                .show();
    }

    /* ローディングダイアログを表示する */
    private void showLoadingDialog(String message) {
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        AppCompatTextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(message);
        progressDialog = new AlertDialog.Builder(this).setCancelable(false).setView(view).create();
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

    /* 端末チェック */
    private boolean supportModelCheck() {
        for (String string : Constants.LIST_MODEL) {
            if (Objects.equals(string, Build.MODEL)) {
                return true;
            }
        }
        /* debuggable の時は確認しない */
        return BuildConfig.DEBUG;
    }

    /* 端末チェックエラー */
    private void supportModelError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_error)
                .setMessage(R.string.dialog_error_check_device)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAffinity())
                .show();
    }

    /* DchaService動作チェック */
    private void checkDchaService() {
        /* DchaServiceを使用する設定かどうか */
        if (Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, Constants.DEF_BOOL)) {
            // dcha接続不可かどうか
            if (!Common.isDchaActive(this)) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_title_error)
                        .setMessage(R.string.dialog_error_check_dcha)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                            Preferences.save(this, Constants.KEY_FLAG_DCHA_FUNCTION, false);
                            init();
                        })
                        .show();
                return;
            }
        }
        confCheck();
    }

    private void confCheck() {
        switch (Build.MODEL) {
            case Constants.PRODUCT_CT3:
                confCheckCT3();
                break;
            case Constants.PRODUCT_CTX:
                confCheckCTX();
                break;
            case Constants.PRODUCT_CTZ:
                confCheckCTZ();
                break;
            default:
                confCheckCT2();
                break;
        }
    }

    /* Pad2起動設定チェック */
    private void confCheckCT2() {
        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, Constants.DEF_BOOL)) {
            startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(0, 0);
            finish();
        } else {
            WarningDialog();
        }
    }

    /* Pad3起動設定チェック */
    private void confCheckCT3() {
        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, Constants.DEF_BOOL)) {
            if (isPermissionCheck()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            }
        } else {
            WarningDialog();
        }
    }

    /* NEO起動設定チェック */
    private void confCheckCTX() {
        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, Constants.DEF_BOOL)) {
            if (isPermissionCheck()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            }
        } else {
            WarningDialog();
        }
    }

    /* NEXT起動設定チェック */
    private void confCheckCTZ() {
        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, Constants.DEF_BOOL)) {
            if (isPermissionCheck()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                overridePendingTransition(0, 0);
                finish();
            }
        } else {
            WarningDialog();
        }
    }

    /* 初回起動お知らせ */
    private void WarningDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_notice_start)
                .setMessage(R.string.dialog_app_start_message)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    Preferences.save(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, true);
                    finish();
                    startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    overridePendingTransition(0, 0);
                })
                .show();
    }

    /* システム設定変更権限か付与されているか確認 */
    private boolean isPermissionCheck() {
        if (!isWriteSystemPermissionCheck()) {
            new AlertDialog.Builder(this)
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
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
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
                    requestPermissions(new String[]{Constants.DCHA_ACCESS_SYSTEM}, 1);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0 && grantResults.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage(R.string.dialog_request_storage_permission)
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                                }
                            })
                            .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> init())
                            .show();
                    return;
                }
            }
        }

        if (requestCode == 1 && grantResults.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission("jp.co.benesse.dcha.permission.ACCESS_SYSTEM") != PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage("jp.co.benesse.dcha.permission.ACCESS_SYSTEM 権限を付与してください。権限を付与される場合は OK を押下してください。")
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                if (checkSelfPermission("jp.co.benesse.dcha.permission.ACCESS_SYSTEM") != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{"jp.co.benesse.dcha.permission.ACCESS_SYSTEM"}, 1);
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
            case WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST:
            case Constants.REQUEST_ACTIVITY_PERMISSION:
                init();
                break;
        }
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
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }
                cancelLoadingDialog();
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.dialog_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
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
                    File tmpFile = getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }
                cancelLoadingDialog();
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getString(R.string.dialog_failure_silent_install) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                        .show();
            }

            @Override
            public void onError(String message) {
                try {
                    /* 一時ファイルを消去 */
                    File tmpFile = getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (IOException ignored) {
                }
                cancelLoadingDialog();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.dialog_title_error))
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> init())
                        .show();
            }
        };
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
