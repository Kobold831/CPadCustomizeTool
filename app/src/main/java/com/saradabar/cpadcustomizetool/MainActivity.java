package com.saradabar.cpadcustomizetool;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.saradabar.cpadcustomizetool.Receiver.PackageAddedReceiver;
import com.saradabar.cpadcustomizetool.data.connection.AsyncFileDownload;
import com.saradabar.cpadcustomizetool.data.connection.Checker;
import com.saradabar.cpadcustomizetool.data.connection.Updater;
import com.saradabar.cpadcustomizetool.data.event.UpdateEventListener;
import com.saradabar.cpadcustomizetool.data.handler.CrashHandler;
import com.saradabar.cpadcustomizetool.data.handler.ProgressHandler;
import com.saradabar.cpadcustomizetool.data.service.KeepService;
import com.saradabar.cpadcustomizetool.util.Common;
import com.saradabar.cpadcustomizetool.util.Constants;
import com.saradabar.cpadcustomizetool.util.Preferences;
import com.saradabar.cpadcustomizetool.util.Toast;
import com.saradabar.cpadcustomizetool.util.Variables;
import com.saradabar.cpadcustomizetool.view.activity.CrashLogActivity;
import com.saradabar.cpadcustomizetool.view.activity.StartActivity;
import com.saradabar.cpadcustomizetool.view.activity.WelAppActivity;
import com.stephentuso.welcome.WelcomeHelper;

import java.io.File;
import java.util.Objects;

import jp.co.benesse.dcha.dchaservice.IDchaService;

public class MainActivity extends Activity implements UpdateEventListener {
    IDchaService mDchaService;
    ProgressDialog loadingDialog;
    boolean result = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        if (Preferences.GET_CRASH(this)) {
            errorCrash();
            return;
        }
        FirstCheck();
    }

    private void FirstCheck() {
        registerReceiver();
        /* ネットワークチェック */
        if (!isNetWork()) {
            netWorkError();
            return;
        }
        /* アップデートチェックの可否を確認 */
        if (Preferences.GET_UPDATE_FLAG(this)) updateCheck();
        else supportCheck();
    }

    private void errorCrash() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("前回" + getApplicationInfo().loadLabel(getPackageManager()).toString() + "が予期せずに終了しました\n繰り返し発生する場合は\"ログを見る\"を押してクラッシュログを開発者に送信してください")
                .setPositiveButton(R.string.dialog_common_continue, (dialog, which) -> {
                    Preferences.SET_CRASH(this, false);
                    FirstCheck();
                })
                .setNeutralButton("ログを見る", (dialog, which) -> startActivity(new Intent(this, CrashLogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)))
                .show();
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addDataScheme("package");
        PackageAddedReceiver packageReceiver = new PackageAddedReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sp = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
                if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
                    if (intent.getData().toString().replace("package:", "").equals(context.getPackageName())) {
                        context.startService(new Intent(context, KeepService.class));
                    }
                    if (sp.getBoolean("permission_forced", false)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
                                /* ユーザーアプリか確認 */
                                if (app.sourceDir.startsWith("/data/app/")) {
                                    Common.setPermissionGrantState(context, app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                                }
                            }
                        }
                    }
                }
                if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
                    if (intent.getData().toString().replace("package:", "").equals(context.getPackageName())) {
                        context.startService(new Intent(context, KeepService.class));
                    }
                    if (sp.getBoolean("permission_forced", false)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
                                /* ユーザーアプリか確認 */
                                if (app.sourceDir.startsWith("/data/app/")) {
                                    Common.setPermissionGrantState(context, app.packageName, DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                                }
                            }
                        }
                    }
                }
            }
        };
        registerReceiver(packageReceiver, intentFilter);
    }

    /* ネットワークの接続を確認 */
    private boolean isNetWork() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /* ネットワークエラー */
    private void netWorkError() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error_start_wifi)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .setNeutralButton(R.string.dialog_common_continue, (dialog, which) -> {
                    result = false;
                    if (Preferences.GET_SETTINGS_FLAG(this)) {
                        if (checkModel()) checkDcha();
                        else errorNotTab2Or3();
                    } else new WelcomeHelper(this, WelAppActivity.class).forceShow();
                })
                .show();
    }

    private void updateCheck() {
        showLoadingDialog();
        new Updater(this, Constants.URL_UPDATE_CHECK, 1).updateCheck();
    }

    private void supportCheck() {
        if (!Preferences.GET_UPDATE_FLAG(this)) showLoadingDialog();
        new Checker(this, Constants.URL_SUPPORT_CHECK).supportCheck();
    }

    @Override
    public void onUpdateApkDownloadComplete() {
        new Handler().post(() -> new Updater(this, Constants.URL_UPDATE_CHECK, 1).installApk(this));
    }

    @Override
    public void onUpdateAvailable(String string) {
    }

    @Override
    public void onUpdateUnavailable() {
    }

    public void onSupportAvailable() {
        cancelLoadingDialog();
        showSupportDialog();
    }

    public void onSupportUnavailable() {
        cancelLoadingDialog();
        if (Preferences.GET_SETTINGS_FLAG(this)) {
            if (checkModel()) checkDcha();
            else errorNotTab2Or3();
        } else {
            new WelcomeHelper(this, WelAppActivity.class).forceShow();
        }
    }

    @Override
    public void onUpdateAvailable1(String string) {
        cancelLoadingDialog();
        showUpdateDialog(string);
    }

    @Override
    public void onUpdateUnavailable1() {
        supportCheck();
    }

    @Override
    public void onDownloadError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    @Override
    public void onConnectionError() {
        cancelLoadingDialog();
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error_start_connection)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    private void showUpdateDialog(String string) {
        if (!Preferences.GET_SETTINGS_FLAG(this)) {
            switch (Build.MODEL) {
                case "TAB-A05-BD":
                case "TAB-A05-BA1":
                    Preferences.SET_MODEL_ID(2, this);
                    break;
            }
        }
        View view = getLayoutInflater().inflate(R.layout.view_update, null);
        TextView mTextView = view.findViewById(R.id.update_information);
        mTextView.setText(string);
        view.findViewById(R.id.update_info_button).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.URL_UPDATE_INFO)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
            } catch (ActivityNotFoundException ignored) {
                Toast.toast(this, R.string.toast_unknown_activity);
            }
        });

        new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_update)
                .setPositiveButton(R.string.dialog_common_yes, (dialog, which) -> {
                    AsyncFileDownload asyncFileDownload = initFileLoader();
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setTitle(R.string.dialog_title_update);
                    progressDialog.setMessage(getString(R.string.progress_state_downloading_update_file));
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgress(0);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_common_cancel), (dialog2, which2) -> {
                        asyncFileDownload.cancel(true);
                        if (isNetWork()) {
                            showLoadingDialog();
                            supportCheck();
                        } else netWorkError();
                    });
                    progressDialog.show();
                    ProgressHandler progressHandler = new ProgressHandler();
                    progressHandler.progressDialog = progressDialog;
                    progressHandler.asyncfiledownload = asyncFileDownload;
                    progressHandler.sendEmptyMessage(0);
                })
                .setNegativeButton(R.string.dialog_common_no, (dialog, which) -> {
                    if (isNetWork()) {
                        showLoadingDialog();
                        supportCheck();
                    } else netWorkError();
                })
                .show();
    }

    private AsyncFileDownload initFileLoader() {
        AsyncFileDownload asyncfiledownload = new AsyncFileDownload(this, Variables.DOWNLOAD_FILE_URL, new File(new File(getExternalCacheDir(), "update.apk").getPath()));
        asyncfiledownload.execute();
        return asyncfiledownload;
    }

    private void showSupportDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setIcon(R.drawable.alert)
                .setMessage(R.string.dialog_error_start_use)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .setNeutralButton(R.string.dialog_common_continue, (dialog, which) -> {
                    result = false;
                    if (Preferences.GET_SETTINGS_FLAG(this)) {
                        if (checkModel()) checkDcha();
                        else errorNotTab2Or3();
                    } else new WelcomeHelper(this, WelAppActivity.class).forceShow();
                })
                .show();
    }

    private void showLoadingDialog() {
        loadingDialog = ProgressDialog.show(this, "", getString(R.string.progress_state_connecting), true);
        loadingDialog.show();
    }

    private void cancelLoadingDialog() {
        try {
            if (loadingDialog != null) loadingDialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    /* 端末チェック */
    private boolean checkModel() {
        String[] modelName = {"TAB-A03-BS", "TAB-A03-BR", "TAB-A03-BR2", "TAB-A03-BR3", "TAB-A05-BD", "TAB-A05-BA1"};
        for (String string : modelName) if (Objects.equals(string, Build.MODEL)) return true;
        return false;
    }

    /* 端末チェックエラー */
    private void errorNotTab2Or3() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_common_error)
                .setMessage(R.string.dialog_error_start_cpad)
                .setIcon(R.drawable.alert)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                .show();
    }

    /* DchaService動作チェック */
    private void checkDcha() {
        if (!Preferences.GET_DCHASERVICE_FLAG(this)) {
            switch (Build.MODEL) {
                case "TAB-A03-BR3":
                    checkSettingsTab3();
                    break;
                case "TAB-A05-BD":
                case "TAB-A05-BA1":
                    checkSettingsTabNeo();
                    break;
                default:
                    checkSettingsTab2();
                    break;
            }
            return;
        }

        /* DchaServiceの使用可否を確認 */
        if (!bindDchaService()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_common_error)
                    .setMessage(R.string.dialog_error_start_dcha_service)
                    .setIcon(R.drawable.alert)
                    .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> finishAndRemoveTask())
                    .setNeutralButton(R.string.dialog_common_continue, (dialogInterface, i) -> {
                        Preferences.SET_DCHASERVICE_FLAG(false, this);
                        switch (Build.MODEL) {
                            case "TAB-A03-BR3":
                                checkSettingsTab3();
                                break;
                            case "TAB-A05-BD":
                            case "TAB-A05-BA1":
                                checkSettingsTabNeo();
                                break;
                            default:
                                checkSettingsTab2();
                                break;
                        }
                    })
                    .show();
            return;
        }

        switch (Build.MODEL) {
            case "TAB-A03-BR3":
                checkSettingsTab3();
                break;
            case "TAB-A05-BD":
            case "TAB-A05-BA1":
                checkSettingsTabNeo();
                break;
            default:
                checkSettingsTab2();
                break;
        }
    }

    /* Pad2起動設定チェック */
    private void checkSettingsTab2() {
        Preferences.SET_MODEL_ID(0, this);
        if (Preferences.GET_SETTINGS_FLAG(this)) {
            startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", result));
            overridePendingTransition(0, 0);
            finish();
        } else startCheck();
    }

    /* Pad3起動設定チェック */
    private void checkSettingsTab3() {
        Preferences.SET_MODEL_ID(1, this);
        if (Preferences.GET_SETTINGS_FLAG(this)) {
            if (permissionSettings()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", result));
                overridePendingTransition(0, 0);
                finish();
            }
        } else startCheck();
    }

    /* PadNeo起動設定チェック */
    private void checkSettingsTabNeo() {
        Preferences.SET_MODEL_ID(2, this);
        if (Preferences.GET_SETTINGS_FLAG(this)) {
            if (permissionSettings()) {
                startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", result));
                overridePendingTransition(0, 0);
                finish();
            }
        } else startCheck();
    }

    /* 初回起動お知らせ */
    public void startCheck() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.dialog_title_notice_start)
                .setMessage(R.string.dialog_notice_start)
                .setPositiveButton(R.string.dialog_common_ok, (dialog, which) -> {
                    if (permissionSettings()) {
                        Preferences.SET_SETTINGS_FLAG(true, this);
                        startActivity(new Intent(this, StartActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).putExtra("result", result));
                        overridePendingTransition(0, 0);
                        finish();
                    }
                })
                .show();
    }

    /* 権限設定 */
    private boolean permissionSettings() {
        if (checkWriteSystemSettings()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.dialog_title_grant_permission)
                    .setMessage(R.string.dialog_error_start_permission)
                    .setIcon(R.drawable.alert)
                    .setPositiveButton(R.string.dialog_common_settings, (dialog, which) -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.fromParts("package", getPackageName(), null)).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION), Constants.REQUEST_PERMISSION);
                    })
                    .setNeutralButton(R.string.dialog_common_exit, (dialogInterface, i) -> finishAndRemoveTask())
                    .show();
            return false;
        } else {
            return true;
        }
    }

    /* システム設定変更権限チェック */
    private boolean checkWriteSystemSettings() {
        boolean canWrite = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            canWrite = Settings.System.canWrite(this);
        return !canWrite;
    }

    ServiceConnection mDchaServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mDchaService = IDchaService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mDchaService = null;
        }
    };

    public boolean bindDchaService() {
        return bindService(Constants.DCHA_SERVICE, mDchaServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.REQUEST_UPDATE:
                if (isNetWork()) {
                    showLoadingDialog();
                    supportCheck();
                } else netWorkError();
                break;
            case WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST:
            case Constants.REQUEST_PERMISSION:
                if (checkModel()) checkDcha();
                else errorNotTab2Or3();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDchaService != null) unbindService(mDchaServiceConnection);
    }
}