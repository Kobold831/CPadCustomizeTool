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

package com.saradabar.cpadcustomizetool.view.activity;

import static com.saradabar.cpadcustomizetool.util.Common.isDhizukuActive;

import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.saradabar.cpadcustomizetool.BuildConfig;
import com.saradabar.cpadcustomizetool.R;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.event.InstallEventListener;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.task.ApkInstallTask;
import com.saradabar.cpadcustomizetool.data.task.DchaInstallTask;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;

import org.json.JSONObject;
import org.zeroturnaround.zip.commons.FileUtils;

import java.io.File;
import java.util.ArrayList;

public class SelfUpdateActivity extends AppCompatActivity implements DownloadEventListener, InstallEventListener {

    AlertDialog progressDialog;
    TextView progressPercentText;
    TextView progressByteText;
    ProgressBar dialogProgressBar;

    String downloadFileUrl;
    ArrayList<String> installFileArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        showLoadingDialog("サーバーと通信しています…");
        new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(getExternalCacheDir(), "Check.json"), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK);
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK:
                try {
                    JSONObject jsonObj1 = Common.parseJson(new File(getExternalCacheDir(), "Check.json"));
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONObject jsonObj3 = jsonObj2.getJSONObject("update");
                    downloadFileUrl = jsonObj3.getString("url");

                    if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        cancelLoadingDialog();
                        showUpdateDialog(jsonObj3.getString("description"), downloadFileUrl);
                    } else {
                        cancelLoadingDialog();
                        showNoUpdateDialog();
                    }
                } catch (Exception ignored) {
                }
                break;
            case Constants.REQUEST_DOWNLOAD_APK:
                cancelLoadingDialog();
                switch (Preferences.load(this, Constants.KEY_FLAG_UPDATE_MODE, 1)) {
                    case 0:
                        startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(getExternalCacheDir(), "update.apk").getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case 1:
                        new AlertDialog.Builder(this)
                                .setCancelable(false)
                                .setTitle("インストール")
                                .setMessage("遷移先のページよりapkファイルをダウンロードしてadbでインストールしてください")
                                .setPositiveButton(R.string.dialog_common_ok, (dialog2, which2) -> {
                                    try {
                                        startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadFileUrl)), Constants.REQUEST_ACTIVITY_UPDATE);
                                    } catch (Exception ignored) {
                                        Toast.makeText(this, R.string.toast_unknown_activity, Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                })
                                .setNegativeButton("キャンセル", null)
                                .show();
                        break;
                    case 2:
                        new DchaInstallTask().execute(this, dchaInstallTaskListener(), new File(getExternalCacheDir(), "update.apk").getPath());
                        break;
                    case 3:
                        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                        if (!dpm.isDeviceOwnerApp(getPackageName())) {
                            Preferences.save(this, Constants.KEY_FLAG_UPDATE_MODE, 1);
                            new AlertDialog.Builder(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_update_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        installFileArrayList.set(0, new File(getExternalCacheDir(), "update.apk").getPath());
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                    case 4:
                        if (!isDhizukuActive(this)) {
                            Preferences.save(this, Constants.KEY_FLAG_UPDATE_MODE, 1);
                            new AlertDialog.Builder(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_update_mode))
                                    .setPositiveButton(R.string.dialog_common_ok, null)
                                    .show();
                            return;
                        }

                        installFileArrayList.set(0, new File(getExternalCacheDir(), "update.apk").getPath());
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), installFileArrayList, Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDownloadError(int reqCode) {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setMessage(R.string.dialog_error)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finish())
                .show();
    }

    @Override
    public void onConnectionError(int reqCode) {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setMessage(R.string.dialog_error_start_connection)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finish())
                .show();
    }

    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
        progressPercentText.setText(new StringBuilder(String.valueOf(progress)).append("%"));
        progressByteText.setText(new StringBuilder(String.valueOf(currentByte)).append(" MB").append("/").append(totalByte).append(" MB"));
        dialogProgressBar.setProgress(progress);
        progressDialog.setMessage(new StringBuilder("インストールファイルをサーバーからダウンロードしています…\nしばらくお待ち下さい…"));
    }

    private DchaInstallTask.Listener dchaInstallTaskListener() {
        return new DchaInstallTask.Listener() {

            /* プログレスバーの表示 */
            @Override
            public void onShow() {
                showLoadingDialog(getString(R.string.progress_state_installing));
            }

            /* 成功 */
            @Override
            public void onSuccess() {
                cancelLoadingDialog();
                new AlertDialog.Builder(SelfUpdateActivity.this)
                        .setMessage(R.string.dialog_info_success_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }

            /* 失敗 */
            @Override
            public void onFailure() {
                cancelLoadingDialog();
                new AlertDialog.Builder(SelfUpdateActivity.this)
                        .setMessage(R.string.dialog_info_failure_silent_install)
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                        .show();
            }
        };
    }

    private void showUpdateDialog(String str, String downloadFileUrl) {
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
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
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
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> finish())
                .show();
    }

    private void showNoUpdateDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setMessage(R.string.dialog_info_no_update)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                .show();
    }

    private void showLoadingDialog(String message) {
        View view = getLayoutInflater().inflate(R.layout.view_progress_spinner, null);
        TextView textView = view.findViewById(R.id.view_progress_spinner_text);
        textView.setText(message);
        progressDialog = new AlertDialog.Builder(this).setCancelable(false).setView(view).create();
        progressDialog.show();
    }

    private void cancelLoadingDialog() {
        if (progressDialog == null) {
            return;
        }

        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_ACTIVITY_UPDATE) {
            finish();
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
                AlertDialog alertDialog = new AlertDialog.Builder(SelfUpdateActivity.this)
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
                new AlertDialog.Builder(SelfUpdateActivity.this)
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
                new AlertDialog.Builder(SelfUpdateActivity.this)
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
