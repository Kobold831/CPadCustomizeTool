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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

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
import com.saradabar.cpadcustomizetool.util.DialogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SelfUpdateActivity extends AppCompatActivity implements DownloadEventListener, InstallEventListener {

    AlertDialog progressDialog;
    AppCompatTextView progressPercentText;
    AppCompatTextView progressByteText;
    ProgressBar dialogProgressBar;

    private final Common.TaskUi installTaskUi = new Common.TaskUi() {
        @Override
        public Context context() {
            return SelfUpdateActivity.this;
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        showLoadingDialog(getString(R.string.progress_state_connecting));
        new FileDownloadTask().execute(this, Constants.URL_CORE, new File(getExternalCacheDir(), Constants.CORE_JSON), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK, Constants.JSON_TIMEOUT_MS, Constants.JSON_TIMEOUT_MS);
    }

    @Override
    public void onDownloadComplete(int reqCode) {
        cancelLoadingDialog();
        switch (reqCode) {
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK:
                try {
                    JSONObject jsonObj3 = Common.getUpdateInfo(new File(getExternalCacheDir(), Constants.CORE_JSON));

                    if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        showUpdateDialog(jsonObj3.getString("url"));
                    } else {
                        showNoUpdateDialog();
                    }
                } catch (JSONException | IOException ignored) {
                    onDownloadError(reqCode);
                }
                break;
            case Constants.REQUEST_DOWNLOAD_APK:
                switch (Common.getFlag(this, Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE)) {
                    case Constants.UPDATE_MODE_PACKAGE_INSTALLER:
                        startActivityForResult(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), Constants.REQUEST_ACTIVITY_UPDATE);
                        break;
                    case Constants.UPDATE_MODE_ADB:
                        try {
                            JSONObject jsonObj1 = Common.parseJson(new File(getExternalCacheDir(), Constants.CORE_JSON));
                            JSONObject jsonObj3 = jsonObj1.getJSONObject("update");

                            new DialogUtil(this)
                                    .setCancelable(false)
                                    .setTitle(getString(R.string.dialog_title_error))
                                    .setMessage(getString(R.string.dialog_no_installer, jsonObj3.getString("url")))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                                    .show();
                        } catch (JSONException | IOException ignored) {
                            onDownloadError(reqCode);
                        }
                        break;
                    case Constants.UPDATE_MODE_DCHA:
                        new DchaInstallTask().execute(this, dchaInstallTaskListener(), new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath());
                        break;
                    case Constants.UPDATE_MODE_DEVICE_OWNER:
                        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

                        if (!dpm.isDeviceOwnerApp(getPackageName()) || Common.isCT2()) {
                            Common.setFlag(this, Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE);
                            new DialogUtil(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), new ArrayList<>(List.of(new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                    case Constants.UPDATE_MODE_DHIZUKU:
                        if (!isDhizukuAllActive(this) || Common.isCT2()) {
                            Common.setFlag(this, Constants.KEY_INT_UPDATE_MODE, Constants.DEF_UPDATE_MODE);
                            new DialogUtil(this)
                                    .setCancelable(false)
                                    .setMessage(getString(R.string.dialog_error_reset_installer))
                                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                                    .show();
                            return;
                        }
                        new ApkInstallTask().execute(this, apkInstallTaskListener(), new ArrayList<>(List.of(new File(getExternalCacheDir(), Constants.DOWNLOAD_APK).getPath())), Constants.REQUEST_INSTALL_SELF_UPDATE, this);
                        break;
                }
                break;
        }
    }

    @Override
    public void onDownloadError(int reqCode) {
        cancelLoadingDialog();
        new DialogUtil(this)
                .setCancelable(false)
                .setTitle(getString(R.string.dialog_title_error))
                .setMessage(getString(R.string.dialog_error_download))
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                .show();
    }

    @Override
    public void onConnectionError(int reqCode) {
        cancelLoadingDialog();
        new DialogUtil(this)
                .setCancelable(false)
                .setTitle(getString(R.string.dialog_title_error))
                .setMessage(R.string.dialog_error_connection)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                .show();
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

    @NonNull
    private DchaInstallTask.Listener dchaInstallTaskListener() {
        return Common.dchaInstallTaskListener(installTaskUi, this::finish);
    }

    private void showUpdateDialog(String downloadFileUrl) {
        View view = getLayoutInflater().inflate(R.layout.view_update, null);

        new DialogUtil(this)
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    FileDownloadTask fileDownloadTask = new FileDownloadTask();
                    fileDownloadTask.execute(this, downloadFileUrl, new File(getExternalCacheDir(), "update.apk"), Constants.REQUEST_DOWNLOAD_APK);
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
                .setNegativeButton(R.string.dialog_common_cancel, (dialog, which) -> finish())
                .show();
    }

    private void showNoUpdateDialog() {
        new DialogUtil(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setMessage(R.string.dialog_no_update)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finish())
                .show();
    }

    private void showLoadingDialog(String message) {
        progressDialog = Common.showProgressSpinner(this, message);
    }

    private void cancelLoadingDialog() {
        Common.cancelDialog(progressDialog);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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

    @NonNull
    private ApkInstallTask.Listener apkInstallTaskListener() {
        return Common.apkInstallTaskListener(installTaskUi, this::finish);
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
