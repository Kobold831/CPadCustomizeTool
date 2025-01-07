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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.view.activity.CrashLogActivity;
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

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class MainActivity extends Activity implements DownloadEventListener, InstallEventListener {

    AlertDialog progressDialog;
    TextView progressPercentText;
    TextView progressByteText;
    ProgressBar dialogProgressBar;

    IDchaService mDchaService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        /* 前回クラッシュしているかどうか */
        if (Preferences.load(this, Constants.KEY_FLAG_ERROR_CRASH, false)) {
            /* クラッシュダイアログ表示 */
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(R.string.dialog_error_crash)
                    .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> {
                        Preferences.save(this, Constants.KEY_FLAG_ERROR_CRASH, false);
                        init();
                    })
                    .setNeutralButton(R.string.dialog_common_check, (dialog, which) -> {
                        startActivity(new Intent(this, CrashLogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        finish();
                    })
                    .setNegativeButton("報告", (dialog, which) -> {
                        try {
                            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("", Preferences.load(this, Constants.KEY_STRINGS_CRASH_LOG, "")));
                            startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_FEEDBACK));
                            init();
                        } catch (Exception e) {
                            new AlertDialog.Builder(this)
                                    .setTitle("エラーの発生")
                                    .setMessage(e.getMessage())
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    })
                    .show();
        } else {
            /* 初回起動か確認 */
            if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, false)) {
                /* 初回起動ではないならサポート端末か確認 */
                if (supportModelCheck()) {
                    /* DchaServiceを確認 */
                    checkDchaService();
                } else {
                    supportModelError();
                }
            } else {
                /* 初回起動はアップデート確認後ウォークスルー起動 */
                /* アップデートチェック */
                updateCheck();
            }
        }
    }

    /* アップデートチェック */
    private void updateCheck() {
        showLoadingDialog("サーバーと通信しています…");
        new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(getExternalCacheDir(), "Check.json"), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK);
    }

    /* ダウンロード完了 */
    @Override
    public void onDownloadComplete(int reqCode) {
        ArrayList<String> installFileArrayList = new ArrayList<>();

        switch (reqCode) {
            /* アップデートチェック要求の場合 */
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK:
                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(getExternalCacheDir(), "Check.json"));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONObject jsonObj3 = jsonObj2.getJSONObject("update");

                    if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        cancelLoadingDialog();
                        showUpdateDialog(jsonObj3.getString("description"), jsonObj3.getString("url"));
                    } else {
                        cancelLoadingDialog();
                        new WelcomeHelper(this, WelAppActivity.class).forceShow();
                    }
                } catch (JSONException | IOException ignored) {
                    onDownloadError(0);
                }
                break;
            /* APKダウンロード要求の場合 */
            case Constants.REQUEST_DOWNLOAD_APK:
                cancelLoadingDialog();
                switch (Preferences.load(this, Constants.KEY_INT_UPDATE_MODE, 1)) {
                    case 0:
                        startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(getExternalCacheDir(), "update.apk").getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case 1:
                        try {
                            JSONObject jsonObj1 = Common.parseJson(new File(getExternalCacheDir(), "Check.json"));
                            JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                            JSONObject jsonObj3 = jsonObj2.getJSONObject("update");

                            new AlertDialog.Builder(this)
                                    .setCancelable(false)
                                    .setTitle(getString(R.string.dialog_title_common_error))
                                    .setMessage(getString(R.string.dialog_info_update_caution, jsonObj3.getString("url")))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                        } catch (Exception ignored) {
                            Toast.makeText(this, "エラーが発生しました", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        break;
                    case 2:
                        new DchaInstallTask().execute(this, dchaInstallTaskListener(), new File(getExternalCacheDir(), "update.apk").getPath());
                        break;
                    case 3:
                        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                        if (!dpm.isDeviceOwnerApp(getPackageName())) {
                            Preferences.save(this, Constants.KEY_INT_UPDATE_MODE, 1);
                            new AlertDialog.Builder(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_update_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        installFileArrayList.add(0, new File(getExternalCacheDir(), "update.apk").getPath());
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                    case 4:
                        if (!isDhizukuActive(this)) {
                            Preferences.save(this, Constants.KEY_INT_UPDATE_MODE, 1);
                            new AlertDialog.Builder(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_update_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        installFileArrayList.add(0, new File(getExternalCacheDir(), "update.apk").getPath());
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                }
                break;
            default:
                break;
        }
    }

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
                        .setMessage(R.string.dialog_info_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                cancelLoadingDialog();
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.dialog_info_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        };
    }

    /* ダウンロードエラー */
    @Override
    public void onDownloadError(int reqCode) {
        cancelLoadingDialog();
        new WelcomeHelper(this, WelAppActivity.class).forceShow();
    }


    /* サーバー接続エラー */
    @Override
    public void onConnectionError(int reqCode) {
        cancelLoadingDialog();
        new WelcomeHelper(this, WelAppActivity.class).forceShow();
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
        progressPercentText.setText(new StringBuilder(String.valueOf(progress)).append("%"));
        progressByteText.setText(new StringBuilder(String.valueOf(currentByte)).append(" MB").append("/").append(totalByte).append(" MB"));
        dialogProgressBar.setProgress(progress);
        progressDialog.setMessage(new StringBuilder("インストールファイルをサーバーからダウンロードしています…\n画面を切り替えないでください。"));
    }

    /* アップデートダイアログ */
    private void showUpdateDialog(String str, String downloadFileUrl) {
        /* モデルIDをセット */
        switch (Build.MODEL) {
            case "TAB-A03-BR3":
                Preferences.save(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT3);
                break;
            case "TAB-A05-BD":
                Preferences.save(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CTX);
                break;
            case "TAB-A05-BA1":
                Preferences.save(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CTZ);
                break;
            default:
                Preferences.save(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2);
                break;
        }

        View view = getLayoutInflater().inflate(R.layout.view_update, null);
        TextView tv = view.findViewById(R.id.update_information);
        tv.setText(str);
        view.findViewById(R.id.update_info_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_UPDATE_INFO).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (Exception ignored) {
                Toast.makeText(this, R.string.toast_unknown_activity, Toast.LENGTH_SHORT).show();
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_update)
                .setMessage("インストールモードを変更するには 設定 を押下してください。")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    FileDownloadTask fileDownloadTask = new FileDownloadTask();
                    fileDownloadTask.execute(this, downloadFileUrl, new File(getExternalCacheDir(), "update.apk"), Constants.REQUEST_DOWNLOAD_APK);
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
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> new WelcomeHelper(this, WelAppActivity.class).forceShow())
                .setNeutralButton(R.string.dialog_common_settings, (dialog2, which2) -> {
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
                                if (Preferences.load(v.getContext(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CT2 || Preferences.load(v.getContext(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) == Constants.MODEL_CT3) {
                                    Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                } else {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_not_work_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                            .show();
                                }
                                break;
                            case 1:
                                Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                listView.invalidateViews();
                                break;
                            case 2:
                                if (bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE) && Preferences.load(v.getContext(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                                    Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                } else {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_not_work_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                            .show();
                                }
                                break;
                            case 3:
                                if (((DevicePolicyManager) v.getContext().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(v.getContext().getPackageName()) && Preferences.load(v.getContext(), Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                                    Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                } else {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_not_work_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                            .show();
                                }
                                break;
                            case 4:
                                if (Common.isDhizukuActive(v.getContext())) {
                                    try {
                                        if (getPackageManager().getPackageInfo("com.rosan.dhizuku", 0).versionCode > 11) {
                                            new AlertDialog.Builder(v.getContext())
                                                    .setCancelable(false)
                                                    .setMessage("Dhizuku の互換性がありません。バージョン 2.8 の Dhizuku をインストールしてください。")
                                                    .setPositiveButton("OK", null)
                                                    .show();
                                            return;
                                        } else {
                                            Preferences.save(v.getContext(), Constants.KEY_INT_UPDATE_MODE, (int) id);
                                            listView.invalidateViews();
                                        }
                                    } catch (Exception ignored) {
                                    }
                                } else {
                                    if (!Dhizuku.init(v.getContext())) {
                                        new AlertDialog.Builder(v.getContext())
                                                .setMessage(getString(R.string.dialog_error_not_work_mode))
                                                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                                .show();
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
                                                            .setMessage(R.string.dialog_info_dhizuku_deny_permission)
                                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
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
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                                finish();
                                startActivity(new Intent(this, getClass()));
                            })
                            .show();
                })
                .show();
    }

    /* ローディングダイアログを表示する */
    private void showLoadingDialog(String message) {
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        TextView textView = view.findViewById(R.id.view_progress_spinner_text);
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
                .setTitle(R.string.dialog_title_common_error)
                .setMessage(R.string.dialog_error_start_cpad)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    /* DchaService動作チェック */
    private void checkDchaService() {
        /* DchaServiceを使用するか確認 */
        if (Preferences.load(this, Constants.KEY_FLAG_DCHA_FUNCTION, false)) {
            if (!bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE)) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_title_common_error)
                        .setMessage(R.string.dialog_error_start_dcha_service)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                        .setNeutralButton(R.string.dialog_common_continue, (dialogInterface, i) -> {
                            Preferences.save(this, Constants.KEY_FLAG_DCHA_FUNCTION, false);
                            confCheck();
                        })
                        .show();
                return;
            }
        }

        confCheck();
    }

    private void confCheck() {
        switch (Build.MODEL) {
            case "TAB-A03-BR3":
                confCheckCT3();
                break;
            case "TAB-A05-BD":
                confCheckCTX();
                break;
            case "TAB-A05-BA1":
                confCheckCTZ();
                break;
            default:
                confCheckCT2();
                break;
        }
    }

    /* Pad2起動設定チェック */
    private void confCheckCT2() {
        Preferences.save(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT2);

        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, false)) {
            startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(0, 0);
            finish();
        } else {
            WarningDialog();
        }
    }

    /* Pad3起動設定チェック */
    private void confCheckCT3() {
        Preferences.save(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CT3);

        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, false)) {
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
        Preferences.save(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CTX);

        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, false)) {
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
        Preferences.save(this, Constants.KEY_INT_MODEL_NUMBER, Constants.MODEL_CTZ);

        if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, false)) {
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
    public void WarningDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_notice_start)
                .setMessage(R.string.dialog_notice_start)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (isPermissionCheck()) {
                        Preferences.save(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, true);
                        startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                        overridePendingTransition(0, 0);
                        finish();
                    }
                })
                .show();
    }

    /* システム設定変更権限か付与されているか確認 */
    private boolean isPermissionCheck() {
        if (!isWriteSystemPermissionCheck()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_grant_permission)
                    .setMessage(R.string.dialog_error_start_permission)
                    .setPositiveButton(R.string.dialog_common_settings, (dialog, which) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", getPackageName(), null)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), Constants.REQUEST_ACTIVITY_PERMISSION);
                        }
                    })
                    .setNeutralButton(R.string.dialog_common_exit, (dialogInterface, i) -> finishAndRemoveTask())
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
            canWrite = checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
            if (!canWrite) {
                requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 0);
            }
        }

        return canWrite;
    }

    /* DchaServiceアクセス権限チェック */
    private boolean isAccessDchaServicePermissionCheck() {
        boolean canWrite = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isDchaInstalled(this)) {
                canWrite = checkSelfPermission("jp.co.benesse.dcha.permission.ACCESS_SYSTEM") == PackageManager.PERMISSION_GRANTED;
                if (!canWrite) {
                    requestPermissions(new String[]{"jp.co.benesse.dcha.permission.ACCESS_SYSTEM"}, 1);
                }
            }
        }

        return canWrite;
    }

    private boolean isDchaInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("jp.co.benesse.dcha.dchaservice", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaService = IDchaService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0 && grantResults.length > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                    new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setMessage("ストレージ権限を付与してください。付与されない場合はアプリをご利用できません。権限を付与される場合は OK を押下してください。同意いただけない場合は キャンセル を押下してください。")
                            .setPositiveButton("OK", (dialog, which) -> {
                                if (checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, 0);
                                }
                            })
                            .setNegativeButton("キャンセル", (dialog, which) -> finishAffinity())
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
                            .setMessage("jp.co.benesse.dcha.permission.ACCESS_SYSTEM 権限を付与してください。付与されない場合はアプリをご利用できません。権限を付与される場合は OK を押下してください。同意いただけない場合は キャンセル を押下してください。")
                            .setPositiveButton("OK", (dialog, which) -> {
                                if (checkSelfPermission("jp.co.benesse.dcha.permission.ACCESS_SYSTEM") != PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{"jp.co.benesse.dcha.permission.ACCESS_SYSTEM"}, 1);
                                }
                            })
                            .setNegativeButton("キャンセル", (dialog, which) -> finishAffinity())
                            .show();
                    return;
                }
            }
        }

        if (isPermissionCheck()) {
            Preferences.save(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, true);
            startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(0, 0);
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_ACTIVITY_UPDATE:
                if (Preferences.load(this, Constants.KEY_FLAG_APP_SETTINGS_COMPLETE, false)) {
                    if (supportModelCheck()) {
                        checkDchaService();
                    } else {
                        supportModelError();
                    }
                } else {
                    new WelcomeHelper(this, WelAppActivity.class).forceShow();
                }
                break;
            case WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST:
            case Constants.REQUEST_ACTIVITY_PERMISSION:
                if (supportModelCheck()) {
                    checkDchaService();
                } else {
                    supportModelError();
                }
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDchaService != null) {
            unbindService(mDchaServiceConnection);
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
                    File tmpFile = getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (Exception ignored) {
                }

                cancelLoadingDialog();
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this)
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
                    File tmpFile = getExternalCacheDir();

                    if (tmpFile != null) {
                        FileUtils.deleteDirectory(tmpFile);
                    }
                } catch (Exception ignored) {
                }

                cancelLoadingDialog();
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getString(R.string.dialog_info_failure_silent_install) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
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
                } catch (Exception ignored) {
                }

                cancelLoadingDialog();
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(getString(R.string.dialog_error) + "\n" + message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, null)
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
