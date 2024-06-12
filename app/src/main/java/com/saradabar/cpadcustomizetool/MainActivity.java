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
import static com.saradabar.cpadcustomizetool.util.Common.parseJson;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.saradabar.cpadcustomizetool.data.event.DownloadEventListener;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.installer.Updater;
import com.saradabar.cpadcustomizetool.data.task.FileDownloadTask;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.util.Variables;
import com.saradabar.cpadcustomizetool.view.activity.CrashLogActivity;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;
import com.saradabar.cpadcustomizetool.view.activity.WebViewActivity;
import com.saradabar.cpadcustomizetool.view.activity.WelAppActivity;
import com.saradabar.cpadcustomizetool.view.views.SingleListView;
import com.stephentuso.welcome.WelcomeHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class MainActivity extends Activity implements DownloadEventListener {

    ProgressDialog progressDialog;
    IDchaService mDchaService;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("");

        /* 前回クラッシュしているかどうか */
        if (Preferences.load(this, Constants.KEY_FLAG_CRASH_LOG, false)) {
            /* クラッシュダイアログ表示 */
            crashError();
        } else {
            /* 起動処理 */
            firstCheck();
        }
    }

    private void crashError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(getString(R.string.dialog_error_crash, getApplicationInfo().loadLabel(getPackageManager())))
                .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> {
                    Preferences.save(this, Constants.KEY_FLAG_CRASH_LOG, false);
                    firstCheck();
                })
                .setNeutralButton(R.string.dialog_common_check, (dialog, which) -> {
                    startActivity(new Intent(this, CrashLogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    finish();
                })
                .show();
    }

    private void firstCheck() {
        /* 初回起動か確認 */
        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
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

    /* アップデートチェック */
    private void updateCheck() {
        showLoadingDialog();
        new FileDownloadTask().execute(this, Constants.URL_CHECK, new File(getExternalCacheDir(), "Check.json"), Constants.REQUEST_DOWNLOAD_UPDATE_CHECK);
    }

    /* ダウンロード完了 */
    @Override
    public void onDownloadComplete(int reqCode) {
        switch (reqCode) {
            /* アップデートチェック要求の場合 */
            case Constants.REQUEST_DOWNLOAD_UPDATE_CHECK:
                try {
                    JSONObject jsonObj1 = parseJson(this);
                    JSONObject jsonObj2 = jsonObj1.getJSONObject("ct");
                    JSONObject jsonObj3 = jsonObj2.getJSONObject("update");
                    Variables.DOWNLOAD_FILE_URL = jsonObj3.getString("url");

                    if (jsonObj3.getInt("versionCode") > BuildConfig.VERSION_CODE) {
                        cancelLoadingDialog();
                        showUpdateDialog(jsonObj3.getString("description"));
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
                new Handler().post(() -> new Updater(this, progressDialog).installApk(this, 0));
                break;
            default:
                break;
        }
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

    @SuppressWarnings("deprecation")
    @Override
    public void onProgressUpdate(int progress, int currentByte, int totalByte) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(progress);
        progressDialog.show();
    }

    /* アップデートダイアログ */
    @SuppressWarnings("deprecation")
    private void showUpdateDialog(String str) {
        /* モデルIDをセット */
        switch (Build.MODEL) {
            case "TAB-A03-BR3":
                Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CT3);
                break;
            case "TAB-A05-BD":
                Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTX);
                break;
            case "TAB-A05-BA1":
                Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTZ);
                break;
            default:
                Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2);
                break;
        }

        View view = getLayoutInflater().inflate(R.layout.view_update, null);
        TextView tv = view.findViewById(R.id.update_information);
        tv.setText(str);
        view.findViewById(R.id.update_info_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, WebViewActivity.class).putExtra("URL", Constants.URL_UPDATE_INFO).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
                Toast.toast(this, R.string.toast_unknown_activity);
            }
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_update)
                .setMessage("アップデートモードを変更するには”設定”を押下してください")
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.setMessage("お待ち下さい・・・");
                    progressDialog.show();
                    FileDownloadTask fileDownloadTask = new FileDownloadTask();
                    fileDownloadTask.execute(this, Variables.DOWNLOAD_FILE_URL, new File(getExternalCacheDir(), "update.apk"), Constants.REQUEST_DOWNLOAD_APK);
                    ProgressHandler progressHandler = new ProgressHandler(Looper.getMainLooper());
                    progressHandler.fileDownloadTask = fileDownloadTask;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> new WelcomeHelper(this, WelAppActivity.class).forceShow())
                .setNeutralButton(R.string.dialog_common_settings, (dialog2, which2) -> {
                    View v = getLayoutInflater().inflate(R.layout.layout_update_list, null);
                    List<SingleListView.AppData> dataList = new ArrayList<>();
                    int i = 0;

                    for (String str1 : Constants.list) {
                        SingleListView.AppData data = new SingleListView.AppData();
                        data.label = str1;
                        data.updateMode = i;
                        dataList.add(data);
                        i++;
                    }

                    ListView listView = v.findViewById(R.id.update_list);
                    listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                    listView.setAdapter(new SingleListView.AppListAdapter(v.getContext(), dataList));
                    listView.setOnItemClickListener((parent, mView, position, id) -> {
                        switch (position) {
                            case 0:
                                if (Preferences.load(v.getContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT2 || Preferences.load(v.getContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) == Constants.MODEL_CT3) {
                                    Preferences.save(v.getContext(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                } else {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_not_work_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                            .show();
                                }
                                break;
                            case 1:
                                Preferences.save(v.getContext(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
                                listView.invalidateViews();
                                break;
                            case 2:
                                if (bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE) && Preferences.load(v.getContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                                    Preferences.save(v.getContext(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                } else {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_not_work_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                            .show();
                                }
                                break;
                            case 3:
                                if (((DevicePolicyManager) v.getContext().getSystemService(Context.DEVICE_POLICY_SERVICE)).isDeviceOwnerApp(v.getContext().getPackageName()) && Preferences.load(v.getContext(), Constants.KEY_MODEL_NAME, Constants.MODEL_CT2) != Constants.MODEL_CT2) {
                                    Preferences.save(v.getContext(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
                                } else {
                                    new AlertDialog.Builder(v.getContext())
                                            .setMessage(getString(R.string.dialog_error_not_work_mode))
                                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> dialog.dismiss())
                                            .show();
                                }
                                break;
                            case 4:
                                if (isDhizukuActive(v.getContext())) {
                                    Preferences.save(v.getContext(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
                                    listView.invalidateViews();
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
                                                    Preferences.save(v.getContext(), Constants.KEY_FLAG_UPDATE_MODE, (int) id);
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
                            .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> firstCheck())
                            .show();
                })
                .show();
    }

    /* ローディングダイアログを表示する */
    @SuppressWarnings("deprecation")
    private void showLoadingDialog() {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("サーバーと通信しています...");
        progressDialog.show();
    }

    /* ローディングダイアログを非表示にする */
    private void cancelLoadingDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    /* 端末チェック */
    private boolean supportModelCheck() {
        for (String string : Constants.modelName) {
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
                .setIcon(R.drawable.alert)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    /* DchaService動作チェック */
    private void checkDchaService() {
        /* DchaServiceを使用するか確認 */
        if (Preferences.load(this, Constants.KEY_FLAG_DCHA_SERVICE, false)) {
            if (!bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE)) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.dialog_title_common_error)
                        .setMessage(R.string.dialog_error_start_dcha_service)
                        .setIcon(R.drawable.alert)
                        .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                        .setNeutralButton(R.string.dialog_common_continue, (dialogInterface, i) -> {
                            Preferences.save(this, Constants.KEY_FLAG_DCHA_SERVICE, false);
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
        Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CT2);

        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
            startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            overridePendingTransition(0, 0);
            finish();
        } else {
            WarningDialog();
        }
    }

    /* Pad3起動設定チェック */
    private void confCheckCT3() {
        Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CT3);

        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
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
        Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTX);

        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
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
        Preferences.save(this, Constants.KEY_MODEL_NAME, Constants.MODEL_CTZ);

        if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
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
                        Preferences.save(this, Constants.KEY_FLAG_SETTINGS, true);
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
                    .setIcon(R.drawable.alert)
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
            if (Common.isAppInstalled(this, "jp.co.benesse.dcha.dchaservice")) {
                canWrite = checkSelfPermission("jp.co.benesse.dcha.permission.ACCESS_SYSTEM") == PackageManager.PERMISSION_GRANTED;
                if (!canWrite) {
                    requestPermissions(new String[]{"jp.co.benesse.dcha.permission.ACCESS_SYSTEM"}, 1);
                }
            }
        }

        return canWrite;
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
                            .setMessage("ストレージ権限を付与してください\n付与されない場合はアプリをご利用できません\n権限を付与される場合は\"OK\"を押下してください\n同意いただけない場合は\"キャンセル\"を押下してください")
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
                            .setMessage("jp.co.benesse.dcha.permission.ACCESS_SYSTEM権限を付与してください\n付与されない場合はアプリをご利用できません\n権限を付与される場合は\"OK\"を押下してください\n同意いただけない場合は\"キャンセル\"を押下してください")
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
            Preferences.save(this, Constants.KEY_FLAG_SETTINGS, true);
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
                if (Preferences.load(this, Constants.KEY_FLAG_SETTINGS, false)) {
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
}
